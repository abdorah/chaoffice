use chrono::{DateTime, Utc};
use sqlx::SqlitePool;
use uuid::Uuid;

use crate::error::{AppError, AppResult};
use crate::models::domain::FinishedGood;
use crate::persistence::queries::finished_goods as queries;

/// Service for managing finished goods inventory.
pub struct FinishedGoodService {
    pool: SqlitePool,
}

impl FinishedGoodService {
    pub fn new(pool: SqlitePool) -> Self {
        Self { pool }
    }

    /// Fetch all finished goods.
    pub async fn get_all(&self) -> AppResult<Vec<FinishedGood>> {
        let rows = queries::list_all(&self.pool).await?;
        rows.into_iter().map(row_to_domain).collect()
    }

    /// Increase a finished good's quantity by `quantity` (Req 6.2).
    pub async fn add(
        &self,
        good_id: Uuid,
        quantity: f64,
    ) -> AppResult<FinishedGood> {
        let now = Utc::now().to_rfc3339();
        let row = queries::add_quantity(&self.pool, &good_id.to_string(), quantity, &now).await?;
        row_to_domain(row)
    }

    /// Deduct `quantity` from a finished good (Req 6.3, 6.4).
    pub async fn deduct(
        &self,
        good_id: Uuid,
        quantity: f64,
    ) -> AppResult<FinishedGood> {
        let now = Utc::now().to_rfc3339();
        let row = queries::deduct_quantity(&self.pool, &good_id.to_string(), quantity, &now).await?;
        row_to_domain(row)
    }
}

/// Convert a persistence row to a domain model.
fn row_to_domain(row: queries::FinishedGoodRow) -> AppResult<FinishedGood> {
    let id = Uuid::parse_str(&row.id)
        .map_err(|e| AppError::Unknown(format!("Invalid UUID: {e}")))?;
    let last_updated: DateTime<Utc> = row
        .last_updated
        .parse()
        .map_err(|e| AppError::Unknown(format!("Invalid timestamp: {e}")))?;

    Ok(FinishedGood {
        id,
        name: row.name,
        current_quantity: row.current_quantity,
        unit_price: row.unit_price,
        last_updated,
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::persistence::db;
    use crate::persistence::queries::finished_goods as queries;

    async fn setup() -> (SqlitePool, FinishedGoodService) {
        let pool = db::init_db(":memory:").await.expect("DB init failed");
        let svc = FinishedGoodService::new(pool.clone());
        (pool, svc)
    }

    async fn seed_good(pool: &SqlitePool, id: &str, name: &str, qty: f64, price: f64) {
        let now = Utc::now().to_rfc3339();
        queries::insert(pool, id, name, qty, price, &now)
            .await
            .expect("seed failed");
    }

    #[tokio::test]
    async fn get_all_returns_empty_when_no_goods() {
        let (_pool, svc) = setup().await;
        let goods = svc.get_all().await.unwrap();
        assert!(goods.is_empty());
    }

    #[tokio::test]
    async fn get_all_returns_seeded_goods() {
        let (pool, svc) = setup().await;
        let id = Uuid::new_v4().to_string();
        seed_good(&pool, &id, "Sweet Box", 20.0, 15.50).await;

        let goods = svc.get_all().await.unwrap();
        assert_eq!(goods.len(), 1);
        assert_eq!(goods[0].name, "Sweet Box");
        assert_eq!(goods[0].current_quantity, 20.0);
        assert_eq!(goods[0].unit_price, 15.50);
    }

    #[tokio::test]
    async fn add_increases_quantity() {
        let (pool, svc) = setup().await;
        let id = Uuid::new_v4();
        seed_good(&pool, &id.to_string(), "Sweet Box", 10.0, 15.0).await;

        let updated = svc.add(id, 5.0).await.unwrap();
        assert_eq!(updated.current_quantity, 15.0);
        assert_eq!(updated.name, "Sweet Box");
    }

    #[tokio::test]
    async fn add_nonexistent_good_fails() {
        let (_pool, svc) = setup().await;
        let err = svc.add(Uuid::new_v4(), 10.0).await.unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }

    #[tokio::test]
    async fn deduct_succeeds_when_stock_sufficient() {
        let (pool, svc) = setup().await;
        let id = Uuid::new_v4();
        seed_good(&pool, &id.to_string(), "Chocolate Box", 30.0, 20.0).await;

        let updated = svc.deduct(id, 10.0).await.unwrap();
        assert_eq!(updated.current_quantity, 20.0);
    }

    #[tokio::test]
    async fn deduct_exact_quantity_succeeds() {
        let (pool, svc) = setup().await;
        let id = Uuid::new_v4();
        seed_good(&pool, &id.to_string(), "Gift Box", 5.0, 25.0).await;

        let updated = svc.deduct(id, 5.0).await.unwrap();
        assert_eq!(updated.current_quantity, 0.0);
    }

    #[tokio::test]
    async fn deduct_insufficient_stock_returns_error() {
        let (pool, svc) = setup().await;
        let id = Uuid::new_v4();
        seed_good(&pool, &id.to_string(), "Premium Box", 3.0, 50.0).await;

        let err = svc.deduct(id, 10.0).await.unwrap_err();
        match err {
            AppError::InsufficientStock {
                material_name,
                available,
                requested,
            } => {
                assert_eq!(material_name, "Premium Box");
                assert_eq!(available, 3.0);
                assert_eq!(requested, 10.0);
            }
            other => panic!("Expected InsufficientStock, got: {other:?}"),
        }
    }

    #[tokio::test]
    async fn deduct_nonexistent_good_fails() {
        let (_pool, svc) = setup().await;
        let err = svc.deduct(Uuid::new_v4(), 1.0).await.unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }
}
