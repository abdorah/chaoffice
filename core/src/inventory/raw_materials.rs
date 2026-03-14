use std::collections::HashMap;

use chrono::Utc;
use sqlx::SqlitePool;
use uuid::Uuid;

use crate::error::{AppError, AppResult};
use crate::models::domain::{Pagination, RawMaterial};
use crate::persistence::queries::raw_materials as queries;

/// Service for managing raw material inventory.
pub struct RawMaterialService {
    pool: SqlitePool,
}

impl RawMaterialService {
    pub fn new(pool: SqlitePool) -> Self {
        Self { pool }
    }

    /// Fetch all raw materials.
    pub async fn get_all(&self, pagination: Option<Pagination>) -> AppResult<Vec<RawMaterial>> {
        let pg = pagination.unwrap_or_default();
        let rows = queries::list_all(&self.pool, &pg).await?;
        rows.into_iter().map(row_to_domain).collect()
    }

    /// Increase a raw material's quantity by `quantity` (Req 3.2).
    pub async fn add_purchase(
        &self,
        material_id: Uuid,
        quantity: f64,
    ) -> AppResult<RawMaterial> {
        let now = Utc::now().to_rfc3339();
        let row = queries::add_quantity(&self.pool, &material_id.to_string(), quantity, &now).await?;
        row_to_domain(row)
    }

    /// Deduct `quantity` from a single raw material (Req 3.3, 3.4).
    pub async fn deduct(
        &self,
        material_id: Uuid,
        quantity: f64,
    ) -> AppResult<RawMaterial> {
        let now = Utc::now().to_rfc3339();
        let row = queries::deduct_quantity(&self.pool, &material_id.to_string(), quantity, &now).await?;
        row_to_domain(row)
    }

    /// Atomically deduct multiple raw materials in a single transaction.
    ///
    /// If any material has insufficient stock, the entire operation is rolled back
    /// and an `InsufficientStock` error is returned for the first failing material.
    pub async fn deduct_multiple(
        &self,
        deductions: HashMap<Uuid, f64>,
    ) -> AppResult<()> {
        let now = Utc::now().to_rfc3339();

        let mut tx = self.pool.begin().await?;

        // Phase 1: Check all stocks before deducting anything
        for (material_id, amount) in &deductions {
            let id_str = material_id.to_string();
            let row = sqlx::query_as::<_, queries::RawMaterialRow>(
                "SELECT id, name, unit, current_quantity, last_updated, sync_status, updated_at FROM raw_materials WHERE id = ?",
            )
            .bind(&id_str)
            .fetch_optional(&mut *tx)
            .await?
            .ok_or_else(|| AppError::NotFound {
                entity_type: "raw_material".to_string(),
                entity_id: material_id.to_string(),
            })?;

            if row.current_quantity < *amount {
                return Err(AppError::InsufficientStock {
                    material_name: row.name,
                    available: row.current_quantity as i64,
                    requested: *amount as i64,
                });
            }
        }

        // Phase 2: Deduct all materials
        for (material_id, amount) in &deductions {
            let id_str = material_id.to_string();
            sqlx::query(
                "UPDATE raw_materials SET current_quantity = current_quantity - ?, last_updated = ?, updated_at = ? WHERE id = ?",
            )
            .bind(amount)
            .bind(&now)
            .bind(&now)
            .bind(&id_str)
            .execute(&mut *tx)
            .await?;
        }

        tx.commit().await?;
        Ok(())
    }
}

/// Convert a persistence row to a domain model.
fn row_to_domain(row: queries::RawMaterialRow) -> AppResult<RawMaterial> {
    let id = crate::utils::parse_uuid("raw_material", &row.id)?;
    let last_updated = crate::utils::parse_timestamp(&row.last_updated)?;

    Ok(RawMaterial {
        id,
        name: row.name,
        unit: row.unit,
        current_quantity: row.current_quantity,
        last_updated,
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::persistence::db;
    use crate::persistence::queries::raw_materials as queries;

    async fn setup() -> (SqlitePool, RawMaterialService) {
        let pool = db::init_db(":memory:").await.expect("DB init failed");
        let svc = RawMaterialService::new(pool.clone());
        (pool, svc)
    }

    async fn seed_material(pool: &SqlitePool, id: &str, name: &str, unit: &str, qty: f64) {
        let now = Utc::now().to_rfc3339();
        queries::insert(pool, id, name, unit, qty, &now)
            .await
            .expect("seed failed");
    }

    #[tokio::test]
    async fn get_all_returns_empty_when_no_materials() {
        let (_pool, svc) = setup().await;
        let materials = svc.get_all(None).await.unwrap();
        assert!(materials.is_empty());
    }

    #[tokio::test]
    async fn get_all_returns_seeded_materials() {
        let (pool, svc) = setup().await;
        let id = Uuid::new_v4().to_string();
        seed_material(&pool, &id, "Sugar", "kg", 100.0).await;

        let materials = svc.get_all(None).await.unwrap();
        assert_eq!(materials.len(), 1);
        assert_eq!(materials[0].name, "Sugar");
        assert_eq!(materials[0].current_quantity, 100.0);
    }

    #[tokio::test]
    async fn add_purchase_increases_quantity() {
        let (pool, svc) = setup().await;
        let id = Uuid::new_v4();
        seed_material(&pool, &id.to_string(), "Milk", "L", 50.0).await;

        let updated = svc.add_purchase(id, 25.0).await.unwrap();
        assert_eq!(updated.current_quantity, 75.0);
        assert_eq!(updated.name, "Milk");
    }

    #[tokio::test]
    async fn add_purchase_nonexistent_material_fails() {
        let (_pool, svc) = setup().await;
        let err = svc.add_purchase(Uuid::new_v4(), 10.0).await.unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }

    #[tokio::test]
    async fn deduct_succeeds_when_stock_sufficient() {
        let (pool, svc) = setup().await;
        let id = Uuid::new_v4();
        seed_material(&pool, &id.to_string(), "Cream", "L", 30.0).await;

        let updated = svc.deduct(id, 10.0).await.unwrap();
        assert_eq!(updated.current_quantity, 20.0);
    }

    #[tokio::test]
    async fn deduct_exact_quantity_succeeds() {
        let (pool, svc) = setup().await;
        let id = Uuid::new_v4();
        seed_material(&pool, &id.to_string(), "Cheese", "kg", 5.0).await;

        let updated = svc.deduct(id, 5.0).await.unwrap();
        assert_eq!(updated.current_quantity, 0.0);
    }

    #[tokio::test]
    async fn deduct_insufficient_stock_returns_error() {
        let (pool, svc) = setup().await;
        let id = Uuid::new_v4();
        seed_material(&pool, &id.to_string(), "Butter", "kg", 2.0).await;

        let err = svc.deduct(id, 5.0).await.unwrap_err();
        match err {
            AppError::InsufficientStock {
                material_name,
                available,
                requested,
            } => {
                assert_eq!(material_name, "Butter");
                assert_eq!(available, 2);
                assert_eq!(requested, 5);
            }
            other => panic!("Expected InsufficientStock, got: {other:?}"),
        }
    }

    #[tokio::test]
    async fn deduct_nonexistent_material_fails() {
        let (_pool, svc) = setup().await;
        let err = svc.deduct(Uuid::new_v4(), 1.0).await.unwrap_err();
        assert!(matches!(err, AppError::NotFound { .. }));
    }

    #[tokio::test]
    async fn deduct_multiple_succeeds_atomically() {
        let (pool, svc) = setup().await;
        let id1 = Uuid::new_v4();
        let id2 = Uuid::new_v4();
        seed_material(&pool, &id1.to_string(), "Sugar", "kg", 100.0).await;
        seed_material(&pool, &id2.to_string(), "Milk", "L", 50.0).await;

        let mut deductions = HashMap::new();
        deductions.insert(id1, 30.0);
        deductions.insert(id2, 20.0);

        svc.deduct_multiple(deductions).await.unwrap();

        let materials = svc.get_all(None).await.unwrap();
        let sugar = materials.iter().find(|m| m.name == "Sugar").unwrap();
        let milk = materials.iter().find(|m| m.name == "Milk").unwrap();
        assert_eq!(sugar.current_quantity, 70.0);
        assert_eq!(milk.current_quantity, 30.0);
    }

    #[tokio::test]
    async fn deduct_multiple_rolls_back_on_insufficient_stock() {
        let (pool, svc) = setup().await;
        let id1 = Uuid::new_v4();
        let id2 = Uuid::new_v4();
        seed_material(&pool, &id1.to_string(), "Sugar", "kg", 100.0).await;
        seed_material(&pool, &id2.to_string(), "Milk", "L", 5.0).await;

        let mut deductions = HashMap::new();
        deductions.insert(id1, 30.0);
        deductions.insert(id2, 20.0); // exceeds available 5.0

        let err = svc.deduct_multiple(deductions).await.unwrap_err();
        assert!(matches!(err, AppError::InsufficientStock { .. }));

        // Both quantities should be unchanged (transaction rolled back)
        let materials = svc.get_all(None).await.unwrap();
        let sugar = materials.iter().find(|m| m.name == "Sugar").unwrap();
        let milk = materials.iter().find(|m| m.name == "Milk").unwrap();
        assert_eq!(sugar.current_quantity, 100.0);
        assert_eq!(milk.current_quantity, 5.0);
    }

    #[tokio::test]
    async fn deduct_multiple_nonexistent_material_fails() {
        let (pool, svc) = setup().await;
        let id1 = Uuid::new_v4();
        seed_material(&pool, &id1.to_string(), "Sugar", "kg", 100.0).await;

        let mut deductions = HashMap::new();
        deductions.insert(id1, 10.0);
        deductions.insert(Uuid::new_v4(), 5.0); // doesn't exist

        let err = svc.deduct_multiple(deductions).await.unwrap_err();
        assert!(matches!(err, AppError::NotFound { .. }));

        // Sugar should be unchanged
        let materials = svc.get_all(None).await.unwrap();
        assert_eq!(materials[0].current_quantity, 100.0);
    }
}
