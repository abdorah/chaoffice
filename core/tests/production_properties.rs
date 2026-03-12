// Property tests for Production Execution (Properties 8, 9, 10, 11)
//
// **Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5**

mod generators;

use proptest::prelude::*;
use tokio::runtime::Runtime;
use uuid::Uuid;

use sweet_lab_core::error::AppError;
use sweet_lab_core::persistence::db;
use sweet_lab_core::persistence::queries::finished_goods as fg_queries;
use sweet_lab_core::persistence::queries::raw_materials as rm_queries;
use sweet_lab_core::persistence::queries::recipes as recipe_queries;
use sweet_lab_core::recipes::production::ProductionServiceImpl;

// ── Helpers ────────────────────────────────────────────────────────────────

async fn setup() -> (sqlx::SqlitePool, ProductionServiceImpl) {
    let pool = db::init_db(":memory:").await.expect("DB init failed");
    let svc = ProductionServiceImpl::new(pool.clone());
    (pool, svc)
}

async fn seed_user(pool: &sqlx::SqlitePool, id: &str, name: &str) {
    let now = chrono::Utc::now().to_rfc3339();
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

async fn seed_raw_material(pool: &sqlx::SqlitePool, id: &str, name: &str, qty: f64) {
    let now = chrono::Utc::now().to_rfc3339();
    rm_queries::insert(pool, id, name, "kg", qty, &now)
        .await
        .expect("seed raw material failed");
}

async fn seed_finished_good(pool: &sqlx::SqlitePool, id: &str, name: &str, qty: f64) {
    let now = chrono::Utc::now().to_rfc3339();
    fg_queries::insert(pool, id, name, qty, 10.0, &now)
        .await
        .expect("seed finished good failed");
}

async fn seed_recipe(
    pool: &sqlx::SqlitePool,
    recipe_id: &str,
    name: &str,
    fg_id: &str,
    ingredients: &[(&str, &str, f64)], // (rm_id, rm_name, required_qty)
) {
    let now = chrono::Utc::now().to_rfc3339();
    let mut tx = pool.begin().await.unwrap();
    recipe_queries::insert_recipe(&mut *tx, recipe_id, name, fg_id, &now)
        .await
        .unwrap();
    for (rm_id, _rm_name, qty) in ingredients {
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

// ── Property 8: Production run inventory conservation ──────────────────────
//
// For any valid production run with recipe R and production quantity P,
// after execution: (a) each raw material's quantity SHALL decrease by exactly
// (ingredient.required_quantity × P), and (b) the finished good's quantity
// SHALL increase by exactly P.
//
// **Validates: Requirements 5.1, 5.2**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(30))]

    #[test]
    fn prop8_production_run_inventory_conservation(
        num_ingredients in 1usize..=4,
        production_qty in 1i32..=10,
        base_stock in 100.0f64..=500.0,
        initial_fg_qty in 0.0f64..=50.0,
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;

            let fg_id = Uuid::new_v4();
            let recipe_id = Uuid::new_v4();
            let chef_id = Uuid::new_v4();

            seed_finished_good(&pool, &fg_id.to_string(), "Product", initial_fg_qty).await;
            seed_user(&pool, &chef_id.to_string(), "Chef Test").await;

            // Create raw materials with enough stock
            let mut ingredients = Vec::new();
            let mut rm_ids = Vec::new();
            let mut required_qtys = Vec::new();
            for i in 0..num_ingredients {
                let rm_id = Uuid::new_v4();
                let name = format!("Material{i}");
                let req_qty = ((i as f64) + 1.0) * 0.5; // 0.5, 1.0, 1.5, 2.0
                seed_raw_material(&pool, &rm_id.to_string(), &name, base_stock).await;
                ingredients.push((rm_id.to_string(), name, req_qty));
                rm_ids.push(rm_id);
                required_qtys.push(req_qty);
            }

            let ing_refs: Vec<(&str, &str, f64)> = ingredients
                .iter()
                .map(|(id, name, qty)| (id.as_str(), name.as_str(), *qty))
                .collect();
            seed_recipe(&pool, &recipe_id.to_string(), "TestRecipe", &fg_id.to_string(), &ing_refs).await;

            // Execute production
            let log = svc.execute_production(recipe_id, production_qty, chef_id).await;
            prop_assert!(log.is_ok(), "Production should succeed, got: {:?}", log.unwrap_err());
            let log = log.unwrap();

            // (a) Each raw material decreased by (required_quantity × production_qty)
            for (idx, rm_id) in rm_ids.iter().enumerate() {
                let rm = rm_queries::get_by_id(&pool, &rm_id.to_string()).await.unwrap().unwrap();
                let expected = base_stock - (required_qtys[idx] * production_qty as f64);
                prop_assert!(
                    (rm.current_quantity - expected).abs() < 1e-9,
                    "Material {} expected qty {}, got {}",
                    idx, expected, rm.current_quantity
                );

                // Also verify materials_consumed in the log
                let consumed = log.materials_consumed.get(rm_id).copied().unwrap_or(0.0);
                let expected_consumed = required_qtys[idx] * production_qty as f64;
                prop_assert!(
                    (consumed - expected_consumed).abs() < 1e-9,
                    "Material {} consumed expected {}, got {}",
                    idx, expected_consumed, consumed
                );
            }

            // (b) Finished good increased by production_qty
            let fg = fg_queries::get_by_id(&pool, &fg_id.to_string()).await.unwrap().unwrap();
            let expected_fg = initial_fg_qty + production_qty as f64;
            prop_assert!(
                (fg.current_quantity - expected_fg).abs() < 1e-9,
                "Finished good expected qty {}, got {}",
                expected_fg, fg.current_quantity
            );

            Ok(())
        })?;
    }
}


// ── Property 9: Production run rejection on insufficient stock ─────────────
//
// For any production run where at least one required raw material has
// insufficient stock, execute_production() SHALL return an error,
// no material quantities SHALL change, and the error SHALL list all
// materials with insufficient quantities.
//
// **Validates: Requirements 5.3**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(30))]

    #[test]
    fn prop9_production_rejected_on_insufficient_stock(
        production_qty in 2i32..=10,
        num_ingredients in 1usize..=4,
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;

            let fg_id = Uuid::new_v4();
            let recipe_id = Uuid::new_v4();
            let chef_id = Uuid::new_v4();

            seed_finished_good(&pool, &fg_id.to_string(), "Product", 5.0).await;
            seed_user(&pool, &chef_id.to_string(), "Chef Test").await;

            // Create materials where ALL have insufficient stock
            let mut ingredients = Vec::new();
            let mut rm_ids = Vec::new();
            let mut initial_qtys = Vec::new();
            for i in 0..num_ingredients {
                let rm_id = Uuid::new_v4();
                let name = format!("Material{i}");
                let req_qty = 5.0; // each ingredient needs 5 per unit
                let stock = 1.0;   // only 1.0 available, needs 5*production_qty
                seed_raw_material(&pool, &rm_id.to_string(), &name, stock).await;
                ingredients.push((rm_id.to_string(), name, req_qty));
                rm_ids.push(rm_id);
                initial_qtys.push(stock);
            }

            let ing_refs: Vec<(&str, &str, f64)> = ingredients
                .iter()
                .map(|(id, name, qty)| (id.as_str(), name.as_str(), *qty))
                .collect();
            seed_recipe(&pool, &recipe_id.to_string(), "TestRecipe", &fg_id.to_string(), &ing_refs).await;

            // Execute production — should fail
            let result = svc.execute_production(recipe_id, production_qty, chef_id).await;
            prop_assert!(result.is_err(), "Production should be rejected due to insufficient stock");

            // Verify error mentions stock
            match &result.unwrap_err() {
                AppError::Validation { field, message } => {
                    prop_assert_eq!(field, "stock");
                    // All materials should be listed as insufficient
                    for i in 0..num_ingredients {
                        prop_assert!(
                            message.contains(&format!("Material{i}")),
                            "Error should mention Material{}, got: {}",
                            i, message
                        );
                    }
                }
                other => prop_assert!(false, "Expected Validation error, got: {:?}", other),
            }

            // Verify NO quantities changed
            for (idx, rm_id) in rm_ids.iter().enumerate() {
                let rm = rm_queries::get_by_id(&pool, &rm_id.to_string()).await.unwrap().unwrap();
                prop_assert!(
                    (rm.current_quantity - initial_qtys[idx]).abs() < 1e-9,
                    "Material {} quantity should be unchanged (expected {}, got {})",
                    idx, initial_qtys[idx], rm.current_quantity
                );
            }

            // Finished good should also be unchanged
            let fg = fg_queries::get_by_id(&pool, &fg_id.to_string()).await.unwrap().unwrap();
            prop_assert!(
                (fg.current_quantity - 5.0).abs() < 1e-9,
                "Finished good quantity should be unchanged (expected 5.0, got {})",
                fg.current_quantity
            );

            Ok(())
        })?;
    }
}


// ── Property 10: Production log completeness ───────────────────────────────
//
// For any completed production run, the resulting ProductionLog SHALL contain
// the chef's identity, recipe used, production quantity, timestamp, and all
// material quantities consumed.
//
// **Validates: Requirements 5.4**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(30))]

    #[test]
    fn prop10_production_log_completeness(
        production_qty in 1i32..=10,
        num_ingredients in 1usize..=4,
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;

            let fg_id = Uuid::new_v4();
            let recipe_id = Uuid::new_v4();
            let chef_id = Uuid::new_v4();
            let chef_name = "Chef Ali";

            seed_finished_good(&pool, &fg_id.to_string(), "Product", 0.0).await;
            seed_user(&pool, &chef_id.to_string(), chef_name).await;

            let mut ingredients = Vec::new();
            let mut rm_ids = Vec::new();
            let mut required_qtys = Vec::new();
            for i in 0..num_ingredients {
                let rm_id = Uuid::new_v4();
                let name = format!("Material{i}");
                let req_qty = (i as f64 + 1.0) * 1.5;
                seed_raw_material(&pool, &rm_id.to_string(), &name, 1000.0).await;
                ingredients.push((rm_id.to_string(), name, req_qty));
                rm_ids.push(rm_id);
                required_qtys.push(req_qty);
            }

            let ing_refs: Vec<(&str, &str, f64)> = ingredients
                .iter()
                .map(|(id, name, qty)| (id.as_str(), name.as_str(), *qty))
                .collect();
            seed_recipe(&pool, &recipe_id.to_string(), "TestRecipe", &fg_id.to_string(), &ing_refs).await;

            let before = chrono::Utc::now();
            let log = svc.execute_production(recipe_id, production_qty, chef_id).await.unwrap();
            let after = chrono::Utc::now();

            // Chef identity
            prop_assert_eq!(log.chef_id, chef_id);
            prop_assert_eq!(&log.chef_name, chef_name);

            // Recipe used
            prop_assert_eq!(log.recipe_id, recipe_id);
            prop_assert_eq!(&log.recipe_name, "TestRecipe");

            // Production quantity
            prop_assert_eq!(log.production_quantity, production_qty);

            // Finished good id
            prop_assert_eq!(log.finished_good_id, fg_id);

            // Timestamp is within the execution window
            prop_assert!(
                log.timestamp >= before && log.timestamp <= after,
                "Timestamp {} should be between {} and {}",
                log.timestamp, before, after
            );

            // All material quantities consumed
            prop_assert_eq!(
                log.materials_consumed.len(),
                num_ingredients,
                "Should have consumed {} materials, got {}",
                num_ingredients, log.materials_consumed.len()
            );
            for (idx, rm_id) in rm_ids.iter().enumerate() {
                let consumed = log.materials_consumed.get(rm_id);
                prop_assert!(
                    consumed.is_some(),
                    "Material {} should be in materials_consumed",
                    idx
                );
                let expected = required_qtys[idx] * production_qty as f64;
                prop_assert!(
                    (consumed.unwrap() - expected).abs() < 1e-9,
                    "Material {} consumed expected {}, got {}",
                    idx, expected, consumed.unwrap()
                );
            }

            Ok(())
        })?;
    }
}


// ── Property 11: Recipe availability calculation ───────────────────────────
//
// For any recipe, the max_producible value SHALL equal the minimum of
// (material.current_quantity / ingredient.required_quantity) across all
// ingredients, floored to an integer.
//
// **Validates: Requirements 5.5**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(30))]

    #[test]
    fn prop11_recipe_availability_calculation(
        num_ingredients in 1usize..=4,
        // Generate stock levels and required quantities per ingredient
        stocks in prop::collection::vec(1.0f64..=200.0, 1..=4),
        req_qtys in prop::collection::vec(0.5f64..=10.0, 1..=4),
    ) {
        // Ensure we have enough entries for num_ingredients
        let num = num_ingredients.min(stocks.len()).min(req_qtys.len());
        if num == 0 {
            return Ok(());
        }

        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;

            let fg_id = Uuid::new_v4();
            let recipe_id = Uuid::new_v4();

            seed_finished_good(&pool, &fg_id.to_string(), "Product", 0.0).await;

            let mut ingredients = Vec::new();
            let mut expected_min = i32::MAX;
            for i in 0..num {
                let rm_id = Uuid::new_v4();
                let name = format!("Material{i}");
                let stock = stocks[i];
                let req = req_qtys[i];
                seed_raw_material(&pool, &rm_id.to_string(), &name, stock).await;
                ingredients.push((rm_id.to_string(), name, req));

                let producible = (stock / req).floor() as i32;
                expected_min = expected_min.min(producible);
            }

            let ing_refs: Vec<(&str, &str, f64)> = ingredients
                .iter()
                .map(|(id, name, qty)| (id.as_str(), name.as_str(), *qty))
                .collect();
            seed_recipe(&pool, &recipe_id.to_string(), "TestRecipe", &fg_id.to_string(), &ing_refs).await;

            let availability = svc.get_recipe_availability().await.unwrap();
            prop_assert_eq!(availability.len(), 1, "Should have exactly 1 recipe");

            let avail = &availability[0];
            prop_assert_eq!(
                avail.max_producible, expected_min,
                "max_producible should be min(floor(stock/required)) = {}, got {}",
                expected_min, avail.max_producible
            );

            Ok(())
        })?;
    }

    #[test]
    fn prop11_recipe_availability_zero_when_no_stock(
        num_ingredients in 1usize..=3,
        req_qty in 1.0f64..=10.0,
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;

            let fg_id = Uuid::new_v4();
            let recipe_id = Uuid::new_v4();

            seed_finished_good(&pool, &fg_id.to_string(), "Product", 0.0).await;

            let mut ingredients = Vec::new();
            for i in 0..num_ingredients {
                let rm_id = Uuid::new_v4();
                let name = format!("Material{i}");
                // All materials have zero stock
                seed_raw_material(&pool, &rm_id.to_string(), &name, 0.0).await;
                ingredients.push((rm_id.to_string(), name, req_qty));
            }

            let ing_refs: Vec<(&str, &str, f64)> = ingredients
                .iter()
                .map(|(id, name, qty)| (id.as_str(), name.as_str(), *qty))
                .collect();
            seed_recipe(&pool, &recipe_id.to_string(), "TestRecipe", &fg_id.to_string(), &ing_refs).await;

            let availability = svc.get_recipe_availability().await.unwrap();
            prop_assert_eq!(availability.len(), 1);
            prop_assert_eq!(
                availability[0].max_producible, 0,
                "max_producible should be 0 when all materials have zero stock"
            );

            Ok(())
        })?;
    }
}
