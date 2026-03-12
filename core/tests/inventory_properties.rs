// Property tests for Inventory Management (Properties 6, 7, 15)
//
// **Validates: Requirements 3.2, 3.4, 6.4**

mod generators;

use proptest::prelude::*;
use tokio::runtime::Runtime;
use uuid::Uuid;

use sweet_lab_core::error::AppError;
use sweet_lab_core::inventory::finished_goods::FinishedGoodService;
use sweet_lab_core::inventory::raw_materials::RawMaterialService;
use sweet_lab_core::persistence::db;
use sweet_lab_core::persistence::queries::finished_goods as fg_queries;
use sweet_lab_core::persistence::queries::raw_materials as rm_queries;

// ── Helpers ────────────────────────────────────────────────────────────────

async fn setup_raw_material_svc() -> (sqlx::SqlitePool, RawMaterialService) {
    let pool = db::init_db(":memory:").await.expect("DB init failed");
    let svc = RawMaterialService::new(pool.clone());
    (pool, svc)
}

async fn setup_finished_good_svc() -> (sqlx::SqlitePool, FinishedGoodService) {
    let pool = db::init_db(":memory:").await.expect("DB init failed");
    let svc = FinishedGoodService::new(pool.clone());
    (pool, svc)
}

// ── Property 6: Raw material purchase increases quantity ───────────────────
//
// For any raw material with current quantity Q and for any positive purchase
// amount A, after recording the purchase the material's quantity SHALL equal
// Q + A.
//
// **Validates: Requirements 3.2**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(100))]

    #[test]
    fn prop6_raw_material_purchase_increases_quantity(
        initial_qty in 0u64..50_000u64,
        purchase_amt in 1u64..50_000u64,
    ) {
        let initial_qty = initial_qty as f64 / 100.0;
        let purchase_amt = purchase_amt as f64 / 100.0;

        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup_raw_material_svc().await;
            let id = Uuid::new_v4();
            let now = chrono::Utc::now().to_rfc3339();
            rm_queries::insert(&pool, &id.to_string(), "TestMaterial", "kg", initial_qty, &now)
                .await
                .expect("seed failed");

            let updated = svc.add_purchase(id, purchase_amt).await.unwrap();

            let expected = initial_qty + purchase_amt;
            prop_assert!(
                (updated.current_quantity - expected).abs() < 1e-9,
                "Expected quantity {}, got {}",
                expected,
                updated.current_quantity
            );

            Ok(())
        })?;
    }
}

// ── Property 7: Insufficient raw material stock rejection ──────────────────
//
// For any raw material with current quantity Q and for any deduction amount D
// where D > Q, the Inventory_Service SHALL return AppError::InsufficientStock
// and the material's quantity SHALL remain Q.
//
// **Validates: Requirements 3.4**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(100))]

    #[test]
    fn prop7_insufficient_raw_material_stock_rejection(
        current_qty in 0u64..50_000u64,
        excess in 1u64..50_000u64,
    ) {
        let current_qty = current_qty as f64 / 100.0;
        let deduction = current_qty + (excess as f64 / 100.0);

        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup_raw_material_svc().await;
            let id = Uuid::new_v4();
            let now = chrono::Utc::now().to_rfc3339();
            rm_queries::insert(&pool, &id.to_string(), "TestMaterial", "kg", current_qty, &now)
                .await
                .expect("seed failed");

            let err = svc.deduct(id, deduction).await;
            prop_assert!(err.is_err(), "Deduction exceeding stock should fail");

            match err.unwrap_err() {
                AppError::InsufficientStock { material_name, available, requested } => {
                    prop_assert_eq!(material_name, "TestMaterial");
                    prop_assert!(
                        (available - current_qty).abs() < 1e-9,
                        "Available should be {}, got {}", current_qty, available
                    );
                    prop_assert!(
                        (requested - deduction).abs() < 1e-9,
                        "Requested should be {}, got {}", deduction, requested
                    );
                }
                other => prop_assert!(false, "Expected InsufficientStock, got: {:?}", other),
            }

            // Verify quantity unchanged
            let materials = svc.get_all().await.unwrap();
            prop_assert_eq!(materials.len(), 1);
            prop_assert!(
                (materials[0].current_quantity - current_qty).abs() < 1e-9,
                "Quantity should remain {}, got {}",
                current_qty,
                materials[0].current_quantity
            );

            Ok(())
        })?;
    }
}

// ── Property 15: Finished good insufficient stock rejection ────────────────
//
// For any finished good with current quantity Q and for any sale quantity S
// where S > Q, the Inventory_Service SHALL return AppError::InsufficientStock
// and the quantity SHALL remain Q.
//
// **Validates: Requirements 6.4**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(100))]

    #[test]
    fn prop15_finished_good_insufficient_stock_rejection(
        current_qty in 0u64..50_000u64,
        excess in 1u64..50_000u64,
    ) {
        let current_qty = current_qty as f64 / 100.0;
        let deduction = current_qty + (excess as f64 / 100.0);

        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup_finished_good_svc().await;
            let id = Uuid::new_v4();
            let now = chrono::Utc::now().to_rfc3339();
            fg_queries::insert(&pool, &id.to_string(), "TestGood", current_qty, 10.0, &now)
                .await
                .expect("seed failed");

            let err = svc.deduct(id, deduction).await;
            prop_assert!(err.is_err(), "Deduction exceeding stock should fail");

            match err.unwrap_err() {
                AppError::InsufficientStock { material_name, available, requested } => {
                    prop_assert_eq!(material_name, "TestGood");
                    prop_assert!(
                        (available - current_qty).abs() < 1e-9,
                        "Available should be {}, got {}", current_qty, available
                    );
                    prop_assert!(
                        (requested - deduction).abs() < 1e-9,
                        "Requested should be {}, got {}", deduction, requested
                    );
                }
                other => prop_assert!(false, "Expected InsufficientStock, got: {:?}", other),
            }

            // Verify quantity unchanged
            let goods = svc.get_all().await.unwrap();
            prop_assert_eq!(goods.len(), 1);
            prop_assert!(
                (goods[0].current_quantity - current_qty).abs() < 1e-9,
                "Quantity should remain {}, got {}",
                current_qty,
                goods[0].current_quantity
            );

            Ok(())
        })?;
    }
}
