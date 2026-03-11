use chrono::Utc;
use sqlx::SqlitePool;
use uuid::Uuid;

use crate::error::{AppError, AppResult};
use crate::models::domain::{Recipe, RecipeIngredient};
use crate::persistence::queries::recipes as queries;

/// Service for recipe CRUD and validation (Req 4.1–4.5).
pub struct RecipeServiceImpl {
    pool: SqlitePool,
}

impl RecipeServiceImpl {
    pub fn new(pool: SqlitePool) -> Self {
        Self { pool }
    }

    /// Fetch all recipes with their ingredients.
    pub async fn get_recipes(&self) -> AppResult<Vec<Recipe>> {
        let rows = queries::list_all(&self.pool).await?;
        let mut recipes = Vec::with_capacity(rows.len());
        for row in rows {
            let ingredients = queries::get_ingredients(&self.pool, &row.id).await?;
            let finished_good_name = queries::get_finished_good_name(&self.pool, &row.finished_good_id)
                .await?
                .unwrap_or_default();
            recipes.push(Recipe {
                id: parse_uuid(&row.id)?,
                name: row.name,
                finished_good_id: parse_uuid(&row.finished_good_id)?,
                finished_good_name,
                ingredients: ingredients.into_iter().map(ingredient_row_to_domain).collect::<AppResult<Vec<_>>>()?,
            });
        }
        Ok(recipes)
    }

    /// Create a new recipe (Req 4.1, 4.2, 4.3, 4.4).
    pub async fn create_recipe(
        &self,
        name: &str,
        finished_good_id: Uuid,
        ingredients: Vec<RecipeIngredient>,
    ) -> AppResult<Recipe> {
        validate_ingredient_count(&ingredients)?;
        self.validate_finished_good_exists(finished_good_id).await?;
        self.validate_ingredients(&ingredients).await?;

        let recipe_id = Uuid::new_v4();
        let now = Utc::now().to_rfc3339();
        let fg_id_str = finished_good_id.to_string();

        let mut tx = self.pool.begin().await?;

        queries::insert_recipe(&mut *tx, &recipe_id.to_string(), name, &fg_id_str, &now).await?;

        for ing in &ingredients {
            let ing_id = Uuid::new_v4();
            queries::insert_ingredient(
                &mut *tx,
                &ing_id.to_string(),
                &recipe_id.to_string(),
                &ing.raw_material_id.to_string(),
                ing.required_quantity,
            )
            .await?;
        }

        tx.commit().await?;

        let finished_good_name = queries::get_finished_good_name(&self.pool, &fg_id_str)
            .await?
            .unwrap_or_default();

        Ok(Recipe {
            id: recipe_id,
            name: name.to_string(),
            finished_good_id,
            finished_good_name,
            ingredients,
        })
    }

    /// Update an existing recipe (Req 4.2, 4.3, 4.4).
    pub async fn update_recipe(&self, recipe: Recipe) -> AppResult<Recipe> {
        validate_ingredient_count(&recipe.ingredients)?;
        self.validate_finished_good_exists(recipe.finished_good_id).await?;
        self.validate_ingredients(&recipe.ingredients).await?;

        let now = Utc::now().to_rfc3339();
        let recipe_id_str = recipe.id.to_string();
        let fg_id_str = recipe.finished_good_id.to_string();

        let mut tx = self.pool.begin().await?;

        queries::update_recipe(&mut *tx, &recipe_id_str, &recipe.name, &fg_id_str, &now).await?;
        queries::delete_ingredients(&mut *tx, &recipe_id_str).await?;

        for ing in &recipe.ingredients {
            let ing_id = Uuid::new_v4();
            queries::insert_ingredient(
                &mut *tx,
                &ing_id.to_string(),
                &recipe_id_str,
                &ing.raw_material_id.to_string(),
                ing.required_quantity,
            )
            .await?;
        }

        tx.commit().await?;

        let finished_good_name = queries::get_finished_good_name(&self.pool, &fg_id_str)
            .await?
            .unwrap_or_default();

        Ok(Recipe {
            id: recipe.id,
            name: recipe.name,
            finished_good_id: recipe.finished_good_id,
            finished_good_name,
            ingredients: recipe.ingredients,
        })
    }

    /// Delete a recipe if it has no production log references (Req 4.5).
    pub async fn delete_recipe(&self, recipe_id: Uuid) -> AppResult<()> {
        let id_str = recipe_id.to_string();

        let count = queries::count_production_logs(&self.pool, &id_str).await?;
        if count > 0 {
            return Err(AppError::DeletionBlocked {
                entity: "Recipe".to_string(),
                reason: format!(
                    "Recipe has {count} production log entries and cannot be deleted"
                ),
            });
        }

        queries::delete_recipe(&self.pool, &id_str).await
    }

    /// Validate that all ingredient raw materials exist in inventory (Req 4.2, 4.4).
    pub async fn validate_ingredients(&self, ingredients: &[RecipeIngredient]) -> AppResult<()> {
        let mut missing = Vec::new();

        for ing in ingredients {
            let id_str = ing.raw_material_id.to_string();
            let row = crate::persistence::queries::raw_materials::get_by_id(&self.pool, &id_str).await?;
            if row.is_none() {
                missing.push(ing.raw_material_name.clone());
            }
        }

        if !missing.is_empty() {
            return Err(AppError::Validation {
                field: "ingredients".to_string(),
                message: format!("Raw materials not found: {}", missing.join(", ")),
            });
        }

        Ok(())
    }

    /// Validate that the finished good exists.
    async fn validate_finished_good_exists(&self, finished_good_id: Uuid) -> AppResult<()> {
        let id_str = finished_good_id.to_string();
        let row = crate::persistence::queries::finished_goods::get_by_id(&self.pool, &id_str).await?;
        if row.is_none() {
            return Err(AppError::Validation {
                field: "finished_good_id".to_string(),
                message: format!("Finished good {finished_good_id} not found"),
            });
        }
        Ok(())
    }
}

/// Validate ingredient count is between 1 and 10 inclusive (Req 4.1).
fn validate_ingredient_count(ingredients: &[RecipeIngredient]) -> AppResult<()> {
    let count = ingredients.len();
    if count < 1 || count > 10 {
        return Err(AppError::Validation {
            field: "ingredients".to_string(),
            message: format!("Recipe must have 1 to 10 ingredients, got {count}"),
        });
    }
    Ok(())
}

fn parse_uuid(s: &str) -> AppResult<Uuid> {
    Uuid::parse_str(s).map_err(|e| AppError::Unknown(format!("Invalid UUID: {e}")))
}

fn ingredient_row_to_domain(row: queries::RecipeIngredientRow) -> AppResult<RecipeIngredient> {
    Ok(RecipeIngredient {
        raw_material_id: parse_uuid(&row.raw_material_id)?,
        raw_material_name: row.raw_material_name,
        required_quantity: row.required_quantity,
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::persistence::db;
    use crate::persistence::queries::finished_goods as fg_queries;
    use crate::persistence::queries::raw_materials as rm_queries;

    async fn setup() -> (SqlitePool, RecipeServiceImpl) {
        let pool = db::init_db(":memory:").await.expect("DB init failed");
        let svc = RecipeServiceImpl::new(pool.clone());
        (pool, svc)
    }

    async fn seed_raw_material(pool: &SqlitePool, id: &str, name: &str) {
        let now = Utc::now().to_rfc3339();
        rm_queries::insert(pool, id, name, "kg", 100.0, &now)
            .await
            .expect("seed raw material failed");
    }

    async fn seed_finished_good(pool: &SqlitePool, id: &str, name: &str) {
        let now = Utc::now().to_rfc3339();
        fg_queries::insert(pool, id, name, 0.0, 10.0, &now)
            .await
            .expect("seed finished good failed");
    }

    async fn seed_production_log(pool: &SqlitePool, recipe_id: &str, chef_id: &str, fg_id: &str) {
        let now = Utc::now().to_rfc3339();
        // Seed a user for the chef reference
        sqlx::query(
            "INSERT OR IGNORE INTO users (id, username, full_name, role, password_hash, sync_status, updated_at, created_at)
             VALUES (?, 'chef1', 'Chef One', 'Chef', 'hash', 'Synced', ?, ?)",
        )
        .bind(chef_id)
        .bind(&now)
        .bind(&now)
        .execute(pool)
        .await
        .expect("seed user failed");

        let log_id = Uuid::new_v4().to_string();
        sqlx::query(
            "INSERT INTO production_logs (id, recipe_id, chef_id, production_quantity, materials_consumed_json, finished_good_id, timestamp, sync_status, updated_at)
             VALUES (?, ?, ?, 1, '{}', ?, ?, 'Synced', ?)",
        )
        .bind(&log_id)
        .bind(recipe_id)
        .bind(chef_id)
        .bind(fg_id)
        .bind(&now)
        .bind(&now)
        .execute(pool)
        .await
        .expect("seed production log failed");
    }

    fn make_ingredients(materials: &[(Uuid, &str, f64)]) -> Vec<RecipeIngredient> {
        materials
            .iter()
            .map(|(id, name, qty)| RecipeIngredient {
                raw_material_id: *id,
                raw_material_name: name.to_string(),
                required_quantity: *qty,
            })
            .collect()
    }

    // ── get_recipes ────────────────────────────────────────────────────

    #[tokio::test]
    async fn get_recipes_returns_empty_when_none() {
        let (_pool, svc) = setup().await;
        let recipes = svc.get_recipes().await.unwrap();
        assert!(recipes.is_empty());
    }

    #[tokio::test]
    async fn get_recipes_returns_created_recipe_with_ingredients() {
        let (pool, svc) = setup().await;
        let fg_id = Uuid::new_v4();
        let rm_id = Uuid::new_v4();
        seed_finished_good(&pool, &fg_id.to_string(), "Sweet Box").await;
        seed_raw_material(&pool, &rm_id.to_string(), "Sugar").await;

        let ingredients = make_ingredients(&[(rm_id, "Sugar", 2.5)]);
        svc.create_recipe("Test Recipe", fg_id, ingredients).await.unwrap();

        let recipes = svc.get_recipes().await.unwrap();
        assert_eq!(recipes.len(), 1);
        assert_eq!(recipes[0].name, "Test Recipe");
        assert_eq!(recipes[0].finished_good_name, "Sweet Box");
        assert_eq!(recipes[0].ingredients.len(), 1);
        assert_eq!(recipes[0].ingredients[0].raw_material_name, "Sugar");
        assert_eq!(recipes[0].ingredients[0].required_quantity, 2.5);
    }

    // ── create_recipe ──────────────────────────────────────────────────

    #[tokio::test]
    async fn create_recipe_succeeds_with_valid_inputs() {
        let (pool, svc) = setup().await;
        let fg_id = Uuid::new_v4();
        let rm1 = Uuid::new_v4();
        let rm2 = Uuid::new_v4();
        seed_finished_good(&pool, &fg_id.to_string(), "Chocolate Box").await;
        seed_raw_material(&pool, &rm1.to_string(), "Milk").await;
        seed_raw_material(&pool, &rm2.to_string(), "Cocoa").await;

        let ingredients = make_ingredients(&[(rm1, "Milk", 1.0), (rm2, "Cocoa", 0.5)]);
        let recipe = svc.create_recipe("Choco Recipe", fg_id, ingredients).await.unwrap();

        assert_eq!(recipe.name, "Choco Recipe");
        assert_eq!(recipe.finished_good_id, fg_id);
        assert_eq!(recipe.finished_good_name, "Chocolate Box");
        assert_eq!(recipe.ingredients.len(), 2);
    }

    #[tokio::test]
    async fn create_recipe_rejects_zero_ingredients() {
        let (pool, svc) = setup().await;
        let fg_id = Uuid::new_v4();
        seed_finished_good(&pool, &fg_id.to_string(), "Box").await;

        let err = svc.create_recipe("Empty", fg_id, vec![]).await.unwrap_err();
        match err {
            AppError::Validation { field, message } => {
                assert_eq!(field, "ingredients");
                assert!(message.contains("1 to 10"));
            }
            other => panic!("Expected Validation, got: {other:?}"),
        }
    }

    #[tokio::test]
    async fn create_recipe_rejects_eleven_ingredients() {
        let (pool, svc) = setup().await;
        let fg_id = Uuid::new_v4();
        seed_finished_good(&pool, &fg_id.to_string(), "Box").await;

        let mut materials = Vec::new();
        let mut ingredients = Vec::new();
        for i in 0..11 {
            let rm_id = Uuid::new_v4();
            let name = format!("Material{i}");
            seed_raw_material(&pool, &rm_id.to_string(), &name).await;
            materials.push((rm_id, name.clone()));
            ingredients.push(RecipeIngredient {
                raw_material_id: rm_id,
                raw_material_name: name,
                required_quantity: 1.0,
            });
        }

        let err = svc.create_recipe("Too Many", fg_id, ingredients).await.unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }

    #[tokio::test]
    async fn create_recipe_rejects_nonexistent_finished_good() {
        let (_pool, svc) = setup().await;
        let fake_fg = Uuid::new_v4();
        let rm_id = Uuid::new_v4();
        // Don't seed the finished good

        let ingredients = make_ingredients(&[(rm_id, "Sugar", 1.0)]);
        let err = svc.create_recipe("Bad FG", fake_fg, ingredients).await.unwrap_err();
        match err {
            AppError::Validation { field, .. } => assert_eq!(field, "finished_good_id"),
            other => panic!("Expected Validation, got: {other:?}"),
        }
    }

    #[tokio::test]
    async fn create_recipe_rejects_nonexistent_raw_material() {
        let (pool, svc) = setup().await;
        let fg_id = Uuid::new_v4();
        seed_finished_good(&pool, &fg_id.to_string(), "Box").await;

        let fake_rm = Uuid::new_v4();
        let ingredients = make_ingredients(&[(fake_rm, "Ghost Material", 1.0)]);
        let err = svc.create_recipe("Bad RM", fg_id, ingredients).await.unwrap_err();
        match err {
            AppError::Validation { field, message } => {
                assert_eq!(field, "ingredients");
                assert!(message.contains("Ghost Material"));
            }
            other => panic!("Expected Validation, got: {other:?}"),
        }
    }

    #[tokio::test]
    async fn create_recipe_with_max_ten_ingredients_succeeds() {
        let (pool, svc) = setup().await;
        let fg_id = Uuid::new_v4();
        seed_finished_good(&pool, &fg_id.to_string(), "Box").await;

        let mut ingredients = Vec::new();
        for i in 0..10 {
            let rm_id = Uuid::new_v4();
            let name = format!("Mat{i}");
            seed_raw_material(&pool, &rm_id.to_string(), &name).await;
            ingredients.push(RecipeIngredient {
                raw_material_id: rm_id,
                raw_material_name: name,
                required_quantity: 1.0,
            });
        }

        let recipe = svc.create_recipe("Max Recipe", fg_id, ingredients).await.unwrap();
        assert_eq!(recipe.ingredients.len(), 10);
    }

    // ── update_recipe ──────────────────────────────────────────────────

    #[tokio::test]
    async fn update_recipe_succeeds_with_new_ingredients() {
        let (pool, svc) = setup().await;
        let fg_id = Uuid::new_v4();
        let rm1 = Uuid::new_v4();
        let rm2 = Uuid::new_v4();
        seed_finished_good(&pool, &fg_id.to_string(), "Sweet Box").await;
        seed_raw_material(&pool, &rm1.to_string(), "Sugar").await;
        seed_raw_material(&pool, &rm2.to_string(), "Cream").await;

        let recipe = svc
            .create_recipe("Original", fg_id, make_ingredients(&[(rm1, "Sugar", 1.0)]))
            .await
            .unwrap();

        let updated = svc
            .update_recipe(Recipe {
                id: recipe.id,
                name: "Updated".to_string(),
                finished_good_id: fg_id,
                finished_good_name: String::new(),
                ingredients: make_ingredients(&[(rm1, "Sugar", 2.0), (rm2, "Cream", 0.5)]),
            })
            .await
            .unwrap();

        assert_eq!(updated.name, "Updated");
        assert_eq!(updated.ingredients.len(), 2);
    }

    #[tokio::test]
    async fn update_recipe_rejects_zero_ingredients() {
        let (pool, svc) = setup().await;
        let fg_id = Uuid::new_v4();
        let rm_id = Uuid::new_v4();
        seed_finished_good(&pool, &fg_id.to_string(), "Box").await;
        seed_raw_material(&pool, &rm_id.to_string(), "Sugar").await;

        let recipe = svc
            .create_recipe("R", fg_id, make_ingredients(&[(rm_id, "Sugar", 1.0)]))
            .await
            .unwrap();

        let err = svc
            .update_recipe(Recipe {
                id: recipe.id,
                name: "R".to_string(),
                finished_good_id: fg_id,
                finished_good_name: String::new(),
                ingredients: vec![],
            })
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }

    #[tokio::test]
    async fn update_recipe_rejects_nonexistent_recipe() {
        let (pool, svc) = setup().await;
        let fg_id = Uuid::new_v4();
        let rm_id = Uuid::new_v4();
        seed_finished_good(&pool, &fg_id.to_string(), "Box").await;
        seed_raw_material(&pool, &rm_id.to_string(), "Sugar").await;

        let err = svc
            .update_recipe(Recipe {
                id: Uuid::new_v4(),
                name: "Ghost".to_string(),
                finished_good_id: fg_id,
                finished_good_name: String::new(),
                ingredients: make_ingredients(&[(rm_id, "Sugar", 1.0)]),
            })
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }

    // ── delete_recipe ──────────────────────────────────────────────────

    #[tokio::test]
    async fn delete_recipe_succeeds_when_no_production_logs() {
        let (pool, svc) = setup().await;
        let fg_id = Uuid::new_v4();
        let rm_id = Uuid::new_v4();
        seed_finished_good(&pool, &fg_id.to_string(), "Box").await;
        seed_raw_material(&pool, &rm_id.to_string(), "Sugar").await;

        let recipe = svc
            .create_recipe("Deletable", fg_id, make_ingredients(&[(rm_id, "Sugar", 1.0)]))
            .await
            .unwrap();

        svc.delete_recipe(recipe.id).await.unwrap();

        let recipes = svc.get_recipes().await.unwrap();
        assert!(recipes.is_empty());
    }

    #[tokio::test]
    async fn delete_recipe_blocked_when_production_logs_exist() {
        let (pool, svc) = setup().await;
        let fg_id = Uuid::new_v4();
        let rm_id = Uuid::new_v4();
        let chef_id = Uuid::new_v4();
        seed_finished_good(&pool, &fg_id.to_string(), "Box").await;
        seed_raw_material(&pool, &rm_id.to_string(), "Sugar").await;

        let recipe = svc
            .create_recipe("Used Recipe", fg_id, make_ingredients(&[(rm_id, "Sugar", 1.0)]))
            .await
            .unwrap();

        seed_production_log(&pool, &recipe.id.to_string(), &chef_id.to_string(), &fg_id.to_string()).await;

        let err = svc.delete_recipe(recipe.id).await.unwrap_err();
        match err {
            AppError::DeletionBlocked { entity, reason } => {
                assert_eq!(entity, "Recipe");
                assert!(reason.contains("production log"));
            }
            other => panic!("Expected DeletionBlocked, got: {other:?}"),
        }
    }

    #[tokio::test]
    async fn delete_nonexistent_recipe_fails() {
        let (_pool, svc) = setup().await;
        let err = svc.delete_recipe(Uuid::new_v4()).await.unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }

    // ── validate_ingredients ───────────────────────────────────────────

    #[tokio::test]
    async fn validate_ingredients_passes_for_existing_materials() {
        let (pool, svc) = setup().await;
        let rm_id = Uuid::new_v4();
        seed_raw_material(&pool, &rm_id.to_string(), "Sugar").await;

        let ingredients = make_ingredients(&[(rm_id, "Sugar", 1.0)]);
        svc.validate_ingredients(&ingredients).await.unwrap();
    }

    #[tokio::test]
    async fn validate_ingredients_fails_listing_all_missing() {
        let (_pool, svc) = setup().await;
        let rm1 = Uuid::new_v4();
        let rm2 = Uuid::new_v4();

        let ingredients = make_ingredients(&[(rm1, "Ghost1", 1.0), (rm2, "Ghost2", 2.0)]);
        let err = svc.validate_ingredients(&ingredients).await.unwrap_err();
        match err {
            AppError::Validation { field, message } => {
                assert_eq!(field, "ingredients");
                assert!(message.contains("Ghost1"));
                assert!(message.contains("Ghost2"));
            }
            other => panic!("Expected Validation, got: {other:?}"),
        }
    }
}
