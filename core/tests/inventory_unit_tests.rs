//! Unit tests for inventory edge cases.
//!
//! Validates: Requirements 3.4, 6.4

use std::collections::HashMap;

use chrono::Utc;
use uuid::Uuid;

use sweet_lab_core::error::AppError;
use sweet_lab_core::inventory::finished_goods::FinishedGoodService;
use sweet_lab_core::inventory::raw_materials::RawMaterialService;
use sweet_lab_core::persistence::db;
use sweet_lab_core::persistence::queries::finished_goods as fg_queries;
use sweet_lab_core::persistence::queries::raw_materials as rm_queries;

// ── Helpers ────────────────────────────────────────────────────────────────

async fn setup_raw() -> (sqlx::SqlitePool, RawMaterialService) {
    let pool = db::init_db(":memory:").await.expect("DB init failed");
    let svc = RawMaterialService::new(pool.clone());
    (pool, svc)
}

async fn setup_fg() -> (sqlx::SqlitePool, FinishedGoodService) {
    let pool = db::init_db(":memory:").await.expect("DB init failed");
    let svc = FinishedGoodService::new(pool.clone());
    (pool, svc)
}

async fn seed_material(pool: &sqlx::SqlitePool, id: &str, name: &str, unit: &str, qty: f64) {
    let now = Utc::now().to_rfc3339();
    rm_queries::insert(pool, id, name, unit, qty, &now)
        .await
        .expect("seed material failed");
}

async fn seed_good(pool: &sqlx::SqlitePool, id: &str, name: &str, qty: f64, price: i64) {
    let now = Utc::now().to_rfc3339();
    fg_queries::insert(pool, id, name, qty, price, &now)
        .await
        .expect("seed good failed");
}

// ── Boundary: deduct exactly available quantity (Req 3.4) ──────────────────

#[tokio::test]
async fn raw_material_deduct_exact_quantity_leaves_zero() {
    let (pool, svc) = setup_raw().await;
    let id = Uuid::new_v4();
    seed_material(&pool, &id.to_string(), "Sugar", "kg", 42.5).await;

    let updated = svc.deduct(id, 42.5).await.unwrap();
    assert_eq!(updated.current_quantity, 0.0, "deducting exact stock should leave 0");
}

#[tokio::test]
async fn raw_material_deduct_exact_then_any_further_deduction_fails() {
    let (pool, svc) = setup_raw().await;
    let id = Uuid::new_v4();
    seed_material(&pool, &id.to_string(), "Milk", "L", 10.0).await;

    // Drain completely
    svc.deduct(id, 10.0).await.unwrap();

    // Any positive deduction should now fail
    let err = svc.deduct(id, 0.01).await.unwrap_err();
    assert!(
        matches!(err, AppError::InsufficientStock { .. }),
        "deducting from zero stock must return InsufficientStock"
    );
}

#[tokio::test]
async fn finished_good_deduct_exact_quantity_leaves_zero() {
    let (pool, svc) = setup_fg().await;
    let id = Uuid::new_v4();
    seed_good(&pool, &id.to_string(), "Sweet Box", 15.0, 2500).await;

    let updated = svc.deduct(id, 15.0).await.unwrap();
    assert_eq!(updated.current_quantity, 0.0, "deducting exact stock should leave 0");
}

#[tokio::test]
async fn finished_good_deduct_exact_then_any_further_deduction_fails() {
    let (pool, svc) = setup_fg().await;
    let id = Uuid::new_v4();
    seed_good(&pool, &id.to_string(), "Gift Box", 5.0, 3000).await;

    svc.deduct(id, 5.0).await.unwrap();

    let err = svc.deduct(id, 0.01).await.unwrap_err();
    assert!(
        matches!(err, AppError::InsufficientStock { .. }),
        "deducting from zero stock must return InsufficientStock"
    );
}

// ── Zero quantity deduction ────────────────────────────────────────────────

#[tokio::test]
async fn raw_material_deduct_zero_succeeds_quantity_unchanged() {
    let (pool, svc) = setup_raw().await;
    let id = Uuid::new_v4();
    seed_material(&pool, &id.to_string(), "Cream", "L", 20.0).await;

    let updated = svc.deduct(id, 0.0).await.unwrap();
    assert_eq!(
        updated.current_quantity, 20.0,
        "deducting zero should leave quantity unchanged"
    );
}

#[tokio::test]
async fn finished_good_deduct_zero_succeeds_quantity_unchanged() {
    let (pool, svc) = setup_fg().await;
    let id = Uuid::new_v4();
    seed_good(&pool, &id.to_string(), "Chocolate Box", 8.0, 4000).await;

    let updated = svc.deduct(id, 0.0).await.unwrap();
    assert_eq!(
        updated.current_quantity, 8.0,
        "deducting zero should leave quantity unchanged"
    );
}

#[tokio::test]
async fn raw_material_deduct_zero_from_zero_stock_succeeds() {
    let (pool, svc) = setup_raw().await;
    let id = Uuid::new_v4();
    seed_material(&pool, &id.to_string(), "Butter", "kg", 0.0).await;

    let updated = svc.deduct(id, 0.0).await.unwrap();
    assert_eq!(
        updated.current_quantity, 0.0,
        "deducting zero from zero stock should succeed"
    );
}

// ── Concurrent deductions within a transaction (deduct_multiple) ───────────

#[tokio::test]
async fn deduct_multiple_exact_quantities_leaves_all_zero() {
    let (pool, svc) = setup_raw().await;
    let id1 = Uuid::new_v4();
    let id2 = Uuid::new_v4();
    let id3 = Uuid::new_v4();
    seed_material(&pool, &id1.to_string(), "Sugar", "kg", 10.0).await;
    seed_material(&pool, &id2.to_string(), "Milk", "L", 5.0).await;
    seed_material(&pool, &id3.to_string(), "Cream", "L", 3.0).await;

    let mut deductions = HashMap::new();
    deductions.insert(id1, 10.0);
    deductions.insert(id2, 5.0);
    deductions.insert(id3, 3.0);

    svc.deduct_multiple(deductions).await.unwrap();

    let materials = svc.get_all(None).await.unwrap();
    for m in &materials {
        assert_eq!(
            m.current_quantity, 0.0,
            "{} should be zero after exact deduction",
            m.name
        );
    }
}

#[tokio::test]
async fn deduct_multiple_with_one_insufficient_rolls_back_all() {
    let (pool, svc) = setup_raw().await;
    let id_ok = Uuid::new_v4();
    let id_fail = Uuid::new_v4();
    seed_material(&pool, &id_ok.to_string(), "Sugar", "kg", 100.0).await;
    seed_material(&pool, &id_fail.to_string(), "Milk", "L", 2.0).await;

    let mut deductions = HashMap::new();
    deductions.insert(id_ok, 50.0);
    deductions.insert(id_fail, 10.0); // exceeds available 2.0

    let err = svc.deduct_multiple(deductions).await.unwrap_err();
    assert!(matches!(err, AppError::InsufficientStock { .. }));

    // Both quantities must be unchanged — transaction rolled back
    let materials = svc.get_all(None).await.unwrap();
    let sugar = materials.iter().find(|m| m.name == "Sugar").unwrap();
    let milk = materials.iter().find(|m| m.name == "Milk").unwrap();
    assert_eq!(sugar.current_quantity, 100.0, "Sugar should be unchanged after rollback");
    assert_eq!(milk.current_quantity, 2.0, "Milk should be unchanged after rollback");
}

#[tokio::test]
async fn deduct_multiple_with_zero_amounts_succeeds() {
    let (pool, svc) = setup_raw().await;
    let id1 = Uuid::new_v4();
    let id2 = Uuid::new_v4();
    seed_material(&pool, &id1.to_string(), "Sugar", "kg", 50.0).await;
    seed_material(&pool, &id2.to_string(), "Milk", "L", 30.0).await;

    let mut deductions = HashMap::new();
    deductions.insert(id1, 0.0);
    deductions.insert(id2, 0.0);

    svc.deduct_multiple(deductions).await.unwrap();

    let materials = svc.get_all(None).await.unwrap();
    let sugar = materials.iter().find(|m| m.name == "Sugar").unwrap();
    let milk = materials.iter().find(|m| m.name == "Milk").unwrap();
    assert_eq!(sugar.current_quantity, 50.0);
    assert_eq!(milk.current_quantity, 30.0);
}

#[tokio::test]
async fn deduct_multiple_empty_map_succeeds() {
    let (_pool, svc) = setup_raw().await;

    let deductions = HashMap::new();
    svc.deduct_multiple(deductions).await.unwrap();
}

#[tokio::test]
async fn deduct_multiple_nonexistent_material_rolls_back() {
    let (pool, svc) = setup_raw().await;
    let id_real = Uuid::new_v4();
    seed_material(&pool, &id_real.to_string(), "Sugar", "kg", 100.0).await;

    let mut deductions = HashMap::new();
    deductions.insert(id_real, 10.0);
    deductions.insert(Uuid::new_v4(), 5.0); // doesn't exist

    let err = svc.deduct_multiple(deductions).await.unwrap_err();
    assert!(matches!(err, AppError::NotFound { .. }));

    // Sugar should be unchanged
    let materials = svc.get_all(None).await.unwrap();
    assert_eq!(materials[0].current_quantity, 100.0);
}
