// Property tests for Recipe Management (Properties 12, 13, 14)
//
// **Validates: Requirements 4.1, 4.2, 4.4, 4.5**

mod generators;

use proptest::prelude::*;
use tokio::runtime::Runtime;
use uuid::Uuid;

use sweet_lab_core::error::AppError;
use sweet_lab_core::models::domain::RecipeIngredient;
use sweet_lab_core::persistence::db;
use sweet_lab_core::persistence::queries::finished_goods as fg_queries;
use sweet_lab_core::persistence::queries::raw_materials as rm_queries;
use sweet_lab_core::recipes::management::RecipeServiceImpl;

// ── Helpers ────────────────────────────────────────────────────────────────

async fn setup() -> (sqlx::SqlitePool, RecipeServiceImpl) {
    let pool = db::init_db(":memory:").await.expect("DB init failed");
    let svc = RecipeServiceImpl::new(pool.clone());
    (pool, svc)
}

async fn seed_raw_material(pool: &sqlx::SqlitePool, id: &str, name: &str) {
    let now = chrono::Utc::now().to_rfc3339();
    rm_queries::insert(pool, id, name, "kg", 100.0, &now)
        .await
        .expect("seed raw material failed");
}

async fn seed_finished_good(pool: &sqlx::SqlitePool, id: &str, name: &str) {
    let now = chrono::Utc::now().to_rfc3339();
    fg_queries::insert(pool, id, name, 0.0, 10.0, &now)
        .await
        .expect("seed finished good failed");
}

fn make_ingredients(materials: &[(Uuid, String, f64)]) -> Vec<RecipeIngredient> {
    materials
        .iter()
        .map(|(id, name, qty)| RecipeIngredient {
            raw_material_id: *id,
            raw_material_name: name.clone(),
            required_quantity: *qty,
        })
        .collect()
}


// ── Property 12: Recipe ingredient count bounds ────────────────────────────
//
// For any recipe creation or update, the Recipe_Engine SHALL accept recipes
// with 1 to 10 ingredients inclusive and reject recipes with 0 or more than
// 10 ingredients with AppError::Validation.
//
// **Validates: Requirements 4.1**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(50))]

    #[test]
    fn prop12_accept_valid_ingredient_count(count in 1usize..=10) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;
            let fg_id = Uuid::new_v4();
            seed_finished_good(&pool, &fg_id.to_string(), "TestGood").await;

            let mut ingredients = Vec::new();
            for i in 0..count {
                let rm_id = Uuid::new_v4();
                let name = format!("Material{i}");
                seed_raw_material(&pool, &rm_id.to_string(), &name).await;
                ingredients.push((rm_id, name, 1.0));
            }

            let result = svc
                .create_recipe("ValidRecipe", fg_id, make_ingredients(&ingredients))
                .await;

            prop_assert!(
                result.is_ok(),
                "Recipe with {} ingredients should be accepted, got: {:?}",
                count,
                result.unwrap_err()
            );
            let recipe = result.unwrap();
            prop_assert_eq!(recipe.ingredients.len(), count);

            Ok(())
        })?;
    }

    #[test]
    fn prop12_reject_too_many_ingredients(extra in 1usize..=10) {
        let count = 10 + extra; // 11..=20
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;
            let fg_id = Uuid::new_v4();
            seed_finished_good(&pool, &fg_id.to_string(), "TestGood").await;

            let mut ingredients = Vec::new();
            for i in 0..count {
                let rm_id = Uuid::new_v4();
                let name = format!("Material{i}");
                seed_raw_material(&pool, &rm_id.to_string(), &name).await;
                ingredients.push((rm_id, name, 1.0));
            }

            let result = svc
                .create_recipe("TooMany", fg_id, make_ingredients(&ingredients))
                .await;

            prop_assert!(result.is_err(), "Recipe with {} ingredients should be rejected", count);
            match result.unwrap_err() {
                AppError::Validation { field, message } => {
                    prop_assert_eq!(field, "ingredients");
                    prop_assert!(
                        message.contains("1 to 10"),
                        "Error message should mention bounds, got: {}",
                        message
                    );
                }
                other => prop_assert!(false, "Expected Validation error, got: {:?}", other),
            }

            Ok(())
        })?;
    }

    #[test]
    fn prop12_reject_zero_ingredients(_seed in 0u32..100) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;
            let fg_id = Uuid::new_v4();
            seed_finished_good(&pool, &fg_id.to_string(), "TestGood").await;

            let result = svc.create_recipe("Empty", fg_id, vec![]).await;

            prop_assert!(result.is_err(), "Recipe with 0 ingredients should be rejected");
            match result.unwrap_err() {
                AppError::Validation { field, .. } => {
                    prop_assert_eq!(field, "ingredients");
                }
                other => prop_assert!(false, "Expected Validation error, got: {:?}", other),
            }

            Ok(())
        })?;
    }
}


// ── Property 13: Recipe material existence validation ──────────────────────
//
// For any recipe that references a raw material not present in the
// Inventory_Service, the Recipe_Engine SHALL return AppError::Validation
// and identify the missing material(s).
//
// **Validates: Requirements 4.2, 4.4**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(50))]

    #[test]
    fn prop13_reject_nonexistent_materials(
        num_fake in 1usize..=5,
        num_real in 0usize..=3,
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;
            let fg_id = Uuid::new_v4();
            seed_finished_good(&pool, &fg_id.to_string(), "TestGood").await;

            let mut ingredients = Vec::new();
            let mut fake_names = Vec::new();

            // Seed real materials
            for i in 0..num_real {
                let rm_id = Uuid::new_v4();
                let name = format!("RealMat{i}");
                seed_raw_material(&pool, &rm_id.to_string(), &name).await;
                ingredients.push((rm_id, name, 1.0));
            }

            // Add fake (non-existent) materials
            for i in 0..num_fake {
                let fake_id = Uuid::new_v4();
                let name = format!("FakeMat{i}");
                fake_names.push(name.clone());
                ingredients.push((fake_id, name, 1.0));
            }

            // Ensure total is within 1..=10
            let total = num_real + num_fake;
            if total < 1 || total > 10 {
                return Ok(());
            }

            let result = svc
                .create_recipe("BadRecipe", fg_id, make_ingredients(&ingredients))
                .await;

            prop_assert!(result.is_err(), "Recipe with non-existent materials should be rejected");
            match result.unwrap_err() {
                AppError::Validation { field, message } => {
                    prop_assert_eq!(field, "ingredients");
                    for fake_name in &fake_names {
                        prop_assert!(
                            message.contains(fake_name),
                            "Error should mention missing material '{}', got: {}",
                            fake_name,
                            message
                        );
                    }
                }
                other => prop_assert!(false, "Expected Validation error, got: {:?}", other),
            }

            Ok(())
        })?;
    }
}


// ── Property 14: Recipe deletion protection ────────────────────────────────
//
// For any recipe that has been referenced by at least one ProductionLog entry,
// deletion SHALL return AppError::DeletionBlocked. For any recipe with zero
// references, deletion SHALL succeed.
//
// **Validates: Requirements 4.5**

async fn seed_production_log(pool: &sqlx::SqlitePool, recipe_id: &str, fg_id: &str) {
    let now = chrono::Utc::now().to_rfc3339();
    let chef_id = Uuid::new_v4().to_string();
    let username = format!("chef_{}", &chef_id[..8]);

    // Seed a user for the chef FK (unique username per call)
    sqlx::query(
        "INSERT INTO users (id, username, full_name, role, password_hash, sync_status, updated_at, created_at)
         VALUES (?, ?, 'Chef', 'Chef', 'hash', 'Synced', ?, ?)",
    )
    .bind(&chef_id)
    .bind(&username)
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
    .bind(&chef_id)
    .bind(fg_id)
    .bind(&now)
    .bind(&now)
    .execute(pool)
    .await
    .expect("seed production log failed");
}

proptest! {
    #![proptest_config(ProptestConfig::with_cases(30))]

    #[test]
    fn prop14_deletion_blocked_with_production_logs(num_logs in 1usize..=3) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;
            let fg_id = Uuid::new_v4();
            let rm_id = Uuid::new_v4();
            seed_finished_good(&pool, &fg_id.to_string(), "TestGood").await;
            seed_raw_material(&pool, &rm_id.to_string(), "Sugar").await;

            let recipe = svc
                .create_recipe(
                    "UsedRecipe",
                    fg_id,
                    make_ingredients(&[(rm_id, "Sugar".to_string(), 1.0)]),
                )
                .await
                .expect("create recipe failed");

            // Seed production logs referencing this recipe
            for _ in 0..num_logs {
                seed_production_log(&pool, &recipe.id.to_string(), &fg_id.to_string()).await;
            }

            let result = svc.delete_recipe(recipe.id).await;

            prop_assert!(result.is_err(), "Deletion should be blocked with {} production logs", num_logs);
            match result.unwrap_err() {
                AppError::DeletionBlocked { entity, reason } => {
                    prop_assert_eq!(entity, "Recipe");
                    prop_assert!(
                        reason.contains("production log"),
                        "Reason should mention production logs, got: {}",
                        reason
                    );
                }
                other => prop_assert!(false, "Expected DeletionBlocked, got: {:?}", other),
            }

            Ok(())
        })?;
    }

    #[test]
    fn prop14_deletion_succeeds_without_production_logs(_seed in 0u32..50) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;
            let fg_id = Uuid::new_v4();
            let rm_id = Uuid::new_v4();
            seed_finished_good(&pool, &fg_id.to_string(), "TestGood").await;
            seed_raw_material(&pool, &rm_id.to_string(), "Sugar").await;

            let recipe = svc
                .create_recipe(
                    "UnusedRecipe",
                    fg_id,
                    make_ingredients(&[(rm_id, "Sugar".to_string(), 1.0)]),
                )
                .await
                .expect("create recipe failed");

            let result = svc.delete_recipe(recipe.id).await;
            prop_assert!(result.is_ok(), "Deletion should succeed with no production logs, got: {:?}", result.unwrap_err());

            // Verify recipe is gone
            let recipes = svc.get_recipes().await.unwrap();
            prop_assert!(
                recipes.iter().all(|r| r.id != recipe.id),
                "Deleted recipe should not appear in list"
            );

            Ok(())
        })?;
    }
}
