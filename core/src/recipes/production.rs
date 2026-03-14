use std::collections::HashMap;

use chrono::Utc;
use sqlx::SqlitePool;
use uuid::Uuid;

use crate::error::{AppError, AppResult};
use crate::models::domain::{Pagination, ProductionLog, Recipe, RecipeAvailability, RecipeIngredient};
use crate::persistence::queries::raw_materials as rm_queries;
use crate::persistence::queries::recipes as recipe_queries;

/// Service for production execution (Req 5.1–5.5).
pub struct ProductionServiceImpl {
    pool: SqlitePool,
}

impl ProductionServiceImpl {
    pub fn new(pool: SqlitePool) -> Self {
        Self { pool }
    }

    /// Execute a production run: calculate requirements, validate stock,
    /// atomically deduct raw materials + increment finished good + log.
    ///
    /// Requirements: 5.1, 5.2, 5.3, 5.4
    pub async fn execute_production(
        &self,
        recipe_id: Uuid,
        quantity: i32,
        chef_id: Uuid,
    ) -> AppResult<ProductionLog> {
        if quantity <= 0 {
            return Err(AppError::Validation {
                field: "quantity".to_string(),
                message: "Production quantity must be greater than 0".to_string(),
            });
        }

        let recipe_id_str = recipe_id.to_string();
        let chef_id_str = chef_id.to_string();

        // Fetch recipe
        let recipe_row = recipe_queries::get_by_id(&self.pool, &recipe_id_str)
            .await?
            .ok_or_else(|| AppError::Validation {
                field: "recipe_id".to_string(),
                message: format!("Recipe {recipe_id} not found"),
            })?;

        // Fetch ingredients
        let ingredients = recipe_queries::get_ingredients(&self.pool, &recipe_id_str).await?;

        // Fetch chef name
        let chef_row = crate::persistence::queries::users::get_user_by_id(&self.pool, &chef_id_str)
            .await?
            .ok_or_else(|| AppError::Validation {
                field: "chef_id".to_string(),
                message: format!("User {chef_id} not found"),
            })?;

        // Req 5.1: Calculate total raw material requirements
        let requirements: Vec<(String, String, f64)> = ingredients
            .iter()
            .map(|ing| {
                let total = ing.required_quantity * quantity as f64;
                (ing.raw_material_id.clone(), ing.raw_material_name.clone(), total)
            })
            .collect();

        let now = Utc::now();
        let now_str = now.to_rfc3339();

        // Begin transaction for atomicity (Req 5.2)
        let mut tx = self.pool.begin().await?;

        // Req 5.3: Check ALL raw material stocks, collect all insufficient
        let mut insufficient: Vec<String> = Vec::new();
        for (rm_id, rm_name, required) in &requirements {
            let row = sqlx::query_as::<_, rm_queries::RawMaterialRow>(
                "SELECT id, name, unit, current_quantity, last_updated, sync_status, updated_at FROM raw_materials WHERE id = ?",
            )
            .bind(rm_id)
            .fetch_optional(&mut *tx)
            .await?
            .ok_or_else(|| AppError::Validation {
                field: "raw_material_id".to_string(),
                message: format!("Raw material {rm_id} not found"),
            })?;

            if row.current_quantity < *required {
                insufficient.push(format!(
                    "{} (available: {}, required: {})",
                    rm_name, row.current_quantity, required
                ));
            }
        }

        if !insufficient.is_empty() {
            return Err(AppError::Validation {
                field: "stock".to_string(),
                message: format!(
                    "Insufficient stock for production: {}",
                    insufficient.join("; ")
                ),
            });
        }

        // Deduct all raw materials
        let mut materials_consumed: HashMap<Uuid, f64> = HashMap::new();
        for (rm_id, _rm_name, required) in &requirements {
            sqlx::query(
                "UPDATE raw_materials SET current_quantity = current_quantity - ?, last_updated = ?, updated_at = ? WHERE id = ?",
            )
            .bind(required)
            .bind(&now_str)
            .bind(&now_str)
            .bind(rm_id)
            .execute(&mut *tx)
            .await?;

            let rm_uuid = crate::utils::parse_uuid("raw_material", rm_id)?;
            materials_consumed.insert(rm_uuid, *required);
        }

        // Increment finished good quantity (Req 5.2)
        let fg_id = &recipe_row.finished_good_id;
        sqlx::query(
            "UPDATE finished_goods SET current_quantity = current_quantity + ?, last_updated = ?, updated_at = ? WHERE id = ?",
        )
        .bind(quantity as f64)
        .bind(&now_str)
        .bind(&now_str)
        .bind(fg_id)
        .execute(&mut *tx)
        .await?;

        // Req 5.4: Insert production log
        let log_id = Uuid::new_v4();
        let materials_json = serde_json::to_string(&materials_consumed)
            .map_err(|e| AppError::Serialization(e.to_string()))?;

        sqlx::query(
            "INSERT INTO production_logs (id, recipe_id, chef_id, production_quantity, materials_consumed_json, finished_good_id, timestamp, sync_status, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, 'Synced', ?)",
        )
        .bind(log_id.to_string())
        .bind(&recipe_id_str)
        .bind(&chef_id_str)
        .bind(quantity)
        .bind(&materials_json)
        .bind(fg_id)
        .bind(&now_str)
        .bind(&now_str)
        .execute(&mut *tx)
        .await?;

        // Commit transaction
        tx.commit().await?;

        // Fetch finished good name for the log
        let _fg_name = recipe_queries::get_finished_good_name(&self.pool, fg_id)
            .await?
            .unwrap_or_default();

        Ok(ProductionLog {
            id: log_id,
            recipe_id,
            recipe_name: recipe_row.name,
            chef_id,
            chef_name: chef_row.full_name,
            production_quantity: quantity,
            materials_consumed,
            finished_good_id: crate::utils::parse_uuid("finished_good", fg_id)?,
            timestamp: now,
        })
    }

    /// Compute recipe availability: max_producible per recipe (Req 5.5).
    ///
    /// max_producible = min(floor(stock / required)) across all ingredients.
    pub async fn get_recipe_availability(&self, pagination: Option<Pagination>) -> AppResult<Vec<RecipeAvailability>> {
        let pg = pagination.unwrap_or_default();
        let recipe_rows = recipe_queries::list_all(&self.pool, &pg).await?;
        let mut result = Vec::with_capacity(recipe_rows.len());

        for row in recipe_rows {
            let ingredients = recipe_queries::get_ingredients(&self.pool, &row.id).await?;
            let fg_name = recipe_queries::get_finished_good_name(&self.pool, &row.finished_good_id)
                .await?
                .unwrap_or_default();

            let mut max_producible = i32::MAX;
            let mut insufficient_materials = Vec::new();

            for ing in &ingredients {
                let rm_row = rm_queries::get_by_id(&self.pool, &ing.raw_material_id).await?;
                match rm_row {
                    Some(rm) => {
                        let producible = (rm.current_quantity / ing.required_quantity).floor() as i32;
                        max_producible = max_producible.min(producible);
                        if rm.current_quantity < ing.required_quantity {
                            insufficient_materials.push(ing.raw_material_name.clone());
                        }
                    }
                    None => {
                        max_producible = 0;
                        insufficient_materials.push(ing.raw_material_name.clone());
                    }
                }
            }

            // If no ingredients, max_producible should be 0
            if ingredients.is_empty() {
                max_producible = 0;
            }

            let domain_ingredients: Vec<RecipeIngredient> = ingredients
                .into_iter()
                .map(|ing| Ok(RecipeIngredient {
                    raw_material_id: crate::utils::parse_uuid("raw_material", &ing.raw_material_id)?,
                    raw_material_name: ing.raw_material_name,
                    required_quantity: ing.required_quantity,
                }))
                .collect::<AppResult<Vec<_>>>()?;

            result.push(RecipeAvailability {
                recipe: Recipe {
                    id: crate::utils::parse_uuid("recipe", &row.id)?,
                    name: row.name,
                    finished_good_id: crate::utils::parse_uuid("finished_good", &row.finished_good_id)?,
                    finished_good_name: fg_name,
                    ingredients: domain_ingredients,
                },
                max_producible,
                insufficient_materials,
            });
        }

        Ok(result)
    }

    /// Fetch production history with recipe and chef names (Req 5.4).
    pub async fn get_production_history(&self, pagination: Option<Pagination>) -> AppResult<Vec<ProductionLog>> {
        let pg = pagination.unwrap_or_default();
        let sql = format!(
            "SELECT pl.id, pl.recipe_id, r.name AS recipe_name, pl.chef_id, u.full_name AS chef_name,
                    pl.production_quantity, pl.materials_consumed_json, pl.finished_good_id, pl.timestamp
             FROM production_logs pl
             JOIN recipes r ON r.id = pl.recipe_id
             JOIN users u ON u.id = pl.chef_id
             ORDER BY pl.timestamp DESC
             LIMIT {} OFFSET {}",
            pg.limit, pg.offset
        );
        let rows = sqlx::query_as::<_, ProductionLogRow>(&sql)
            .fetch_all(&self.pool)
            .await?;

        rows.into_iter().map(log_row_to_domain).collect()
    }
}

/// Row type for production_logs joined with recipes and users.
#[derive(Debug, Clone, sqlx::FromRow)]
struct ProductionLogRow {
    id: String,
    recipe_id: String,
    recipe_name: String,
    chef_id: String,
    chef_name: String,
    production_quantity: i32,
    materials_consumed_json: String,
    finished_good_id: String,
    timestamp: String,
}

fn log_row_to_domain(row: ProductionLogRow) -> AppResult<ProductionLog> {
    let materials_consumed: HashMap<Uuid, f64> =
        serde_json::from_str(&row.materials_consumed_json)
            .map_err(|e| AppError::Serialization(e.to_string()))?;

    let timestamp = crate::utils::parse_timestamp(&row.timestamp)?;

    Ok(ProductionLog {
        id: crate::utils::parse_uuid("production_log", &row.id)?,
        recipe_id: crate::utils::parse_uuid("recipe", &row.recipe_id)?,
        recipe_name: row.recipe_name,
        chef_id: crate::utils::parse_uuid("user", &row.chef_id)?,
        chef_name: row.chef_name,
        production_quantity: row.production_quantity,
        materials_consumed,
        finished_good_id: crate::utils::parse_uuid("finished_good", &row.finished_good_id)?,
        timestamp,
    })
}


#[cfg(test)]
mod tests {
    use super::*;
    use crate::persistence::db;
    use crate::persistence::queries::finished_goods as fg_queries;
    use crate::persistence::queries::raw_materials as rm_queries;

    async fn setup() -> (SqlitePool, ProductionServiceImpl) {
        let pool = db::init_db(":memory:").await.expect("DB init failed");
        let svc = ProductionServiceImpl::new(pool.clone());
        (pool, svc)
    }

    async fn seed_user(pool: &SqlitePool, id: &str, name: &str) {
        let now = Utc::now().to_rfc3339();
        sqlx::query(
            "INSERT INTO users (id, username, full_name, role, password_hash, sync_status, updated_at, created_at)
             VALUES (?, ?, ?, 'Chef', 'hash', 'Synced', ?, ?)",
        )
        .bind(id)
        .bind(name)
        .bind(name)
        .bind(&now)
        .bind(&now)
        .execute(pool)
        .await
        .expect("seed user failed");
    }

    async fn seed_raw_material(pool: &SqlitePool, id: &str, name: &str, qty: f64) {
        let now = Utc::now().to_rfc3339();
        rm_queries::insert(pool, id, name, "kg", qty, &now)
            .await
            .expect("seed raw material failed");
    }

    async fn seed_finished_good(pool: &SqlitePool, id: &str, name: &str, qty: f64) {
        let now = Utc::now().to_rfc3339();
        fg_queries::insert(pool, id, name, qty, 1000, &now)
            .await
            .expect("seed finished good failed");
    }

    async fn seed_recipe(pool: &SqlitePool, recipe_id: &str, name: &str, fg_id: &str, ingredients: &[(&str, f64)]) {
        let now = Utc::now().to_rfc3339();
        let mut tx = pool.begin().await.unwrap();
        recipe_queries::insert_recipe(&mut *tx, recipe_id, name, fg_id, &now)
            .await
            .unwrap();
        for (rm_id, qty) in ingredients {
            let ing_id = Uuid::new_v4().to_string();
            sqlx::query(
                "INSERT INTO recipe_ingredients (id, recipe_id, raw_material_id, required_quantity) VALUES (?, ?, ?, ?)",
            )
            .bind(&ing_id)
            .bind(recipe_id)
            .bind(rm_id)
            .bind(qty)
            .execute(&mut *tx)
            .await
            .expect("seed ingredient failed");
        }
        tx.commit().await.unwrap();
    }

    // ── execute_production ─────────────────────────────────────────────

    #[tokio::test]
    async fn execute_production_succeeds_and_updates_inventory() {
        let (pool, svc) = setup().await;
        let fg_id = Uuid::new_v4();
        let rm1 = Uuid::new_v4();
        let rm2 = Uuid::new_v4();
        let recipe_id = Uuid::new_v4();
        let chef_id = Uuid::new_v4();

        seed_finished_good(&pool, &fg_id.to_string(), "Sweet Box", 0.0).await;
        seed_raw_material(&pool, &rm1.to_string(), "Sugar", 100.0).await;
        seed_raw_material(&pool, &rm2.to_string(), "Milk", 50.0).await;
        seed_user(&pool, &chef_id.to_string(), "Chef Ali").await;
        seed_recipe(
            &pool,
            &recipe_id.to_string(),
            "Sweet Recipe",
            &fg_id.to_string(),
            &[(&rm1.to_string(), 2.0), (&rm2.to_string(), 1.5)],
        )
        .await;

        // Produce 3 units: Sugar needs 6.0, Milk needs 4.5
        let log = svc.execute_production(recipe_id, 3, chef_id).await.unwrap();

        assert_eq!(log.recipe_id, recipe_id);
        assert_eq!(log.recipe_name, "Sweet Recipe");
        assert_eq!(log.chef_id, chef_id);
        assert_eq!(log.chef_name, "Chef Ali");
        assert_eq!(log.production_quantity, 3);
        assert_eq!(log.finished_good_id, fg_id);
        assert_eq!(*log.materials_consumed.get(&rm1).unwrap(), 6.0);
        assert_eq!(*log.materials_consumed.get(&rm2).unwrap(), 4.5);

        // Verify inventory changes
        let sugar = rm_queries::get_by_id(&pool, &rm1.to_string()).await.unwrap().unwrap();
        assert_eq!(sugar.current_quantity, 94.0); // 100 - 6

        let milk = rm_queries::get_by_id(&pool, &rm2.to_string()).await.unwrap().unwrap();
        assert_eq!(milk.current_quantity, 45.5); // 50 - 4.5

        let fg = fg_queries::get_by_id(&pool, &fg_id.to_string()).await.unwrap().unwrap();
        assert_eq!(fg.current_quantity, 3.0); // 0 + 3
    }

    #[tokio::test]
    async fn execute_production_rejects_insufficient_stock_listing_all() {
        let (pool, svc) = setup().await;
        let fg_id = Uuid::new_v4();
        let rm1 = Uuid::new_v4();
        let rm2 = Uuid::new_v4();
        let recipe_id = Uuid::new_v4();
        let chef_id = Uuid::new_v4();

        seed_finished_good(&pool, &fg_id.to_string(), "Sweet Box", 0.0).await;
        seed_raw_material(&pool, &rm1.to_string(), "Sugar", 3.0).await;
        seed_raw_material(&pool, &rm2.to_string(), "Milk", 1.0).await;
        seed_user(&pool, &chef_id.to_string(), "Chef Ali").await;
        seed_recipe(
            &pool,
            &recipe_id.to_string(),
            "Sweet Recipe",
            &fg_id.to_string(),
            &[(&rm1.to_string(), 2.0), (&rm2.to_string(), 1.5)],
        )
        .await;

        // Produce 5 units: Sugar needs 10 (have 3), Milk needs 7.5 (have 1)
        let err = svc.execute_production(recipe_id, 5, chef_id).await.unwrap_err();
        match &err {
            AppError::Validation { field, message } => {
                assert_eq!(field, "stock");
                assert!(message.contains("Sugar"), "Should list Sugar: {message}");
                assert!(message.contains("Milk"), "Should list Milk: {message}");
            }
            other => panic!("Expected Validation, got: {other:?}"),
        }

        // Verify no inventory changes (transaction rolled back)
        let sugar = rm_queries::get_by_id(&pool, &rm1.to_string()).await.unwrap().unwrap();
        assert_eq!(sugar.current_quantity, 3.0);
        let milk = rm_queries::get_by_id(&pool, &rm2.to_string()).await.unwrap().unwrap();
        assert_eq!(milk.current_quantity, 1.0);
        let fg = fg_queries::get_by_id(&pool, &fg_id.to_string()).await.unwrap().unwrap();
        assert_eq!(fg.current_quantity, 0.0);
    }

    #[tokio::test]
    async fn execute_production_rejects_zero_quantity() {
        let (pool, svc) = setup().await;
        let chef_id = Uuid::new_v4();
        seed_user(&pool, &chef_id.to_string(), "Chef").await;

        let err = svc.execute_production(Uuid::new_v4(), 0, chef_id).await.unwrap_err();
        assert!(matches!(err, AppError::Validation { field, .. } if field == "quantity"));
    }

    #[tokio::test]
    async fn execute_production_rejects_negative_quantity() {
        let (pool, svc) = setup().await;
        let chef_id = Uuid::new_v4();
        seed_user(&pool, &chef_id.to_string(), "Chef").await;

        let err = svc.execute_production(Uuid::new_v4(), -1, chef_id).await.unwrap_err();
        assert!(matches!(err, AppError::Validation { field, .. } if field == "quantity"));
    }

    #[tokio::test]
    async fn execute_production_rejects_nonexistent_recipe() {
        let (pool, svc) = setup().await;
        let chef_id = Uuid::new_v4();
        seed_user(&pool, &chef_id.to_string(), "Chef").await;

        let err = svc.execute_production(Uuid::new_v4(), 1, chef_id).await.unwrap_err();
        assert!(matches!(err, AppError::Validation { field, .. } if field == "recipe_id"));
    }

    #[tokio::test]
    async fn execute_production_rejects_nonexistent_chef() {
        let (pool, svc) = setup().await;
        let fg_id = Uuid::new_v4();
        let rm_id = Uuid::new_v4();
        let recipe_id = Uuid::new_v4();

        seed_finished_good(&pool, &fg_id.to_string(), "Box", 0.0).await;
        seed_raw_material(&pool, &rm_id.to_string(), "Sugar", 100.0).await;
        seed_recipe(
            &pool,
            &recipe_id.to_string(),
            "Recipe",
            &fg_id.to_string(),
            &[(&rm_id.to_string(), 1.0)],
        )
        .await;

        let err = svc.execute_production(recipe_id, 1, Uuid::new_v4()).await.unwrap_err();
        assert!(matches!(err, AppError::Validation { field, .. } if field == "chef_id"));
    }

    #[tokio::test]
    async fn execute_production_exact_stock_succeeds() {
        let (pool, svc) = setup().await;
        let fg_id = Uuid::new_v4();
        let rm_id = Uuid::new_v4();
        let recipe_id = Uuid::new_v4();
        let chef_id = Uuid::new_v4();

        seed_finished_good(&pool, &fg_id.to_string(), "Box", 5.0).await;
        seed_raw_material(&pool, &rm_id.to_string(), "Sugar", 10.0).await;
        seed_user(&pool, &chef_id.to_string(), "Chef").await;
        seed_recipe(
            &pool,
            &recipe_id.to_string(),
            "Recipe",
            &fg_id.to_string(),
            &[(&rm_id.to_string(), 5.0)],
        )
        .await;

        // Produce 2: needs exactly 10.0 Sugar
        let log = svc.execute_production(recipe_id, 2, chef_id).await.unwrap();
        assert_eq!(log.production_quantity, 2);

        let sugar = rm_queries::get_by_id(&pool, &rm_id.to_string()).await.unwrap().unwrap();
        assert_eq!(sugar.current_quantity, 0.0);

        let fg = fg_queries::get_by_id(&pool, &fg_id.to_string()).await.unwrap().unwrap();
        assert_eq!(fg.current_quantity, 7.0);
    }

    // ── get_recipe_availability ────────────────────────────────────────

    #[tokio::test]
    async fn get_recipe_availability_computes_max_producible() {
        let (pool, svc) = setup().await;
        let fg_id = Uuid::new_v4();
        let rm1 = Uuid::new_v4();
        let rm2 = Uuid::new_v4();
        let recipe_id = Uuid::new_v4();

        seed_finished_good(&pool, &fg_id.to_string(), "Box", 0.0).await;
        seed_raw_material(&pool, &rm1.to_string(), "Sugar", 10.0).await;
        seed_raw_material(&pool, &rm2.to_string(), "Milk", 7.0).await;
        seed_recipe(
            &pool,
            &recipe_id.to_string(),
            "Recipe",
            &fg_id.to_string(),
            &[(&rm1.to_string(), 2.0), (&rm2.to_string(), 3.0)],
        )
        .await;

        // Sugar: floor(10/2) = 5, Milk: floor(7/3) = 2 → min = 2
        let avail = svc.get_recipe_availability(None).await.unwrap();
        assert_eq!(avail.len(), 1);
        assert_eq!(avail[0].max_producible, 2);
        assert!(avail[0].insufficient_materials.is_empty());
    }

    #[tokio::test]
    async fn get_recipe_availability_lists_zero_stock_materials() {
        let (pool, svc) = setup().await;
        let fg_id = Uuid::new_v4();
        let rm1 = Uuid::new_v4();
        let rm2 = Uuid::new_v4();
        let recipe_id = Uuid::new_v4();

        seed_finished_good(&pool, &fg_id.to_string(), "Box", 0.0).await;
        seed_raw_material(&pool, &rm1.to_string(), "Sugar", 10.0).await;
        seed_raw_material(&pool, &rm2.to_string(), "Milk", 0.0).await;
        seed_recipe(
            &pool,
            &recipe_id.to_string(),
            "Recipe",
            &fg_id.to_string(),
            &[(&rm1.to_string(), 2.0), (&rm2.to_string(), 3.0)],
        )
        .await;

        let avail = svc.get_recipe_availability(None).await.unwrap();
        assert_eq!(avail[0].max_producible, 0);
        assert_eq!(avail[0].insufficient_materials, vec!["Milk"]);
    }

    #[tokio::test]
    async fn get_recipe_availability_lists_insufficient_partial_stock() {
        let (pool, svc) = setup().await;
        let fg_id = Uuid::new_v4();
        let rm1 = Uuid::new_v4();
        let rm2 = Uuid::new_v4();
        let recipe_id = Uuid::new_v4();

        seed_finished_good(&pool, &fg_id.to_string(), "Box", 0.0).await;
        // Sugar needs 5.0 per batch but only 3.0 available → insufficient
        seed_raw_material(&pool, &rm1.to_string(), "Sugar", 3.0).await;
        // Milk needs 2.0 per batch and has 10.0 → sufficient
        seed_raw_material(&pool, &rm2.to_string(), "Milk", 10.0).await;
        seed_recipe(
            &pool,
            &recipe_id.to_string(),
            "Recipe",
            &fg_id.to_string(),
            &[(&rm1.to_string(), 5.0), (&rm2.to_string(), 2.0)],
        )
        .await;

        let avail = svc.get_recipe_availability(None).await.unwrap();
        assert_eq!(avail[0].max_producible, 0);
        // Sugar has 3.0 but needs 5.0 → flagged as insufficient
        assert_eq!(avail[0].insufficient_materials, vec!["Sugar"]);
    }

    #[tokio::test]
    async fn get_recipe_availability_empty_when_no_recipes() {
        let (_pool, svc) = setup().await;
        let avail = svc.get_recipe_availability(None).await.unwrap();
        assert!(avail.is_empty());
    }

    // ── get_production_history ─────────────────────────────────────────

    #[tokio::test]
    async fn get_production_history_returns_logs_after_production() {
        let (pool, svc) = setup().await;
        let fg_id = Uuid::new_v4();
        let rm_id = Uuid::new_v4();
        let recipe_id = Uuid::new_v4();
        let chef_id = Uuid::new_v4();

        seed_finished_good(&pool, &fg_id.to_string(), "Box", 0.0).await;
        seed_raw_material(&pool, &rm_id.to_string(), "Sugar", 100.0).await;
        seed_user(&pool, &chef_id.to_string(), "Chef Ali").await;
        seed_recipe(
            &pool,
            &recipe_id.to_string(),
            "Recipe",
            &fg_id.to_string(),
            &[(&rm_id.to_string(), 1.0)],
        )
        .await;

        svc.execute_production(recipe_id, 2, chef_id).await.unwrap();
        svc.execute_production(recipe_id, 3, chef_id).await.unwrap();

        let history = svc.get_production_history(None).await.unwrap();
        assert_eq!(history.len(), 2);
        // Most recent first
        assert_eq!(history[0].production_quantity, 3);
        assert_eq!(history[1].production_quantity, 2);
        assert_eq!(history[0].chef_name, "Chef Ali");
    }

    #[tokio::test]
    async fn get_production_history_empty_when_no_logs() {
        let (_pool, svc) = setup().await;
        let history = svc.get_production_history(None).await.unwrap();
        assert!(history.is_empty());
    }

    #[tokio::test]
    async fn production_log_contains_all_required_fields() {
        let (pool, svc) = setup().await;
        let fg_id = Uuid::new_v4();
        let rm_id = Uuid::new_v4();
        let recipe_id = Uuid::new_v4();
        let chef_id = Uuid::new_v4();

        seed_finished_good(&pool, &fg_id.to_string(), "Sweet Box", 0.0).await;
        seed_raw_material(&pool, &rm_id.to_string(), "Sugar", 50.0).await;
        seed_user(&pool, &chef_id.to_string(), "Chef Ali").await;
        seed_recipe(
            &pool,
            &recipe_id.to_string(),
            "Sweet Recipe",
            &fg_id.to_string(),
            &[(&rm_id.to_string(), 2.5)],
        )
        .await;

        let log = svc.execute_production(recipe_id, 4, chef_id).await.unwrap();

        // Req 5.4: chef identity, recipe, quantity, timestamp, materials consumed
        assert_eq!(log.chef_id, chef_id);
        assert_eq!(log.chef_name, "Chef Ali");
        assert_eq!(log.recipe_id, recipe_id);
        assert_eq!(log.recipe_name, "Sweet Recipe");
        assert_eq!(log.production_quantity, 4);
        assert_eq!(log.finished_good_id, fg_id);
        assert!(!log.materials_consumed.is_empty());
        assert_eq!(*log.materials_consumed.get(&rm_id).unwrap(), 10.0); // 2.5 * 4
        // Timestamp should be recent
        let diff = Utc::now() - log.timestamp;
        assert!(diff.num_seconds() < 5);
    }
}
