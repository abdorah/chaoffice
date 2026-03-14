// Property tests for Sales Transactions (Properties 20, 21, 22)
//
// **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5**

mod generators;

use proptest::prelude::*;
use tokio::runtime::Runtime;

use sweet_lab_core::models::domain::SaleLineItem;
use sweet_lab_core::models::Money;
use sweet_lab_core::persistence::db;
use sweet_lab_core::persistence::queries::customers as customer_queries;
use sweet_lab_core::persistence::queries::finished_goods as fg_queries;
use sweet_lab_core::persistence::queries::wallets as wallet_queries;
use sweet_lab_core::sales::receipts;
use sweet_lab_core::sales::transactions::SalesServiceImpl;

// ── Helpers ────────────────────────────────────────────────────────────────

async fn setup() -> (sqlx::SqlitePool, SalesServiceImpl) {
    let pool = db::init_db(":memory:").await.expect("DB init failed");
    let svc = SalesServiceImpl::new(pool.clone());
    (pool, svc)
}

/// Seed a customer, wallet, and a set of finished goods.
/// Returns (customer_id, wallet_id).
async fn seed_fixtures(
    pool: &sqlx::SqlitePool,
    wallet_balance: i64,
    goods: &[(uuid::Uuid, &str, f64, i64)],
) -> (uuid::Uuid, uuid::Uuid) {
    let now = chrono::Utc::now().to_rfc3339();
    let cust_id = uuid::Uuid::new_v4();
    let wallet_id = uuid::Uuid::new_v4();

    customer_queries::insert(
        pool, &cust_id.to_string(), "Test Customer", "Test City",
        &format!("+963{}", uuid::Uuid::new_v4().as_u128() % 1_000_000_000), &now,
    ).await.expect("seed customer failed");

    wallet_queries::insert_wallet(
        pool, &wallet_id.to_string(), "Cash", "Cash", wallet_balance, &now,
    ).await.expect("seed wallet failed");

    for (id, name, qty, price) in goods {
        fg_queries::insert(pool, &id.to_string(), name, *qty, *price, &now)
            .await.expect("seed finished good failed");
    }
    (cust_id, wallet_id)
}
