// Property tests for Debt Tracking (Properties 27, 28, 29, 30)
//
// **Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.5**

mod generators;

use proptest::prelude::*;
use tokio::runtime::Runtime;

use chrono::{Duration, Utc};
use sweet_lab_core::debt::tracker::DebtServiceImpl;
use sweet_lab_core::persistence::db;
use sweet_lab_core::persistence::queries::customers as customer_queries;
use sweet_lab_core::persistence::queries::debts as debt_queries;
use uuid::Uuid;

// ── Helpers ────────────────────────────────────────────────────────────────

async fn setup() -> (sqlx::SqlitePool, DebtServiceImpl) {
    let pool = db::init_db(":memory:").await.expect("DB init failed");
    let svc = DebtServiceImpl::new(pool.clone());
    (pool, svc)
}

async fn seed_customer(pool: &sqlx::SqlitePool, id: &str, name: &str, mobile: &str) {
    let now = Utc::now().to_rfc3339();
    customer_queries::insert(pool, id, name, "TestCity", mobile, &now)
        .await
        .expect("seed customer failed");
}

async fn seed_wallet(pool: &sqlx::SqlitePool, id: &str) {
    let now = Utc::now().to_rfc3339();
    sqlx::query(
        "INSERT INTO wallets (id, name, wallet_type, current_balance, sync_status, updated_at)
         VALUES (?, 'TestWallet', 'Cash', 100000.0, 'Synced', ?)",
    )
    .bind(id)
    .bind(&now)
    .execute(pool)
    .await
    .expect("seed wallet failed");
}

async fn seed_sale(pool: &sqlx::SqlitePool, sale_id: &str, customer_id: &str, wallet_id: &str) {
    let now = Utc::now().to_rfc3339();
    sqlx::query(
        "INSERT INTO sales (id, customer_id, total_amount, amount_paid, payment_wallet_id, timestamp, sync_status, updated_at)
         VALUES (?, ?, 100.0, 0.0, ?, ?, 'Synced', ?)",
    )
    .bind(sale_id)
    .bind(customer_id)
    .bind(wallet_id)
    .bind(&now)
    .bind(&now)
    .execute(pool)
    .await
    .expect("seed sale failed");
}

// ── Property 27: Debt overdue days calculation ─────────────────────────────
//
// For any unpaid debt record with sale_date SD and current date CD,
// the overdue days SHALL equal (CD - SD) in days.
//
// **Validates: Requirements 10.1**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(30))]

    #[test]
    fn prop27_overdue_days_equals_date_diff(
        days_ago in 0u32..365u32,
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;

            let cust_id = Uuid::new_v4();
            let sale_id = Uuid::new_v4();
            let debt_id = Uuid::new_v4();
            let wallet_id = Uuid::new_v4();
            let mobile = format!("05{:08}", days_ago);

            seed_customer(&pool, &cust_id.to_string(), "Customer", &mobile).await;
            seed_wallet(&pool, &wallet_id.to_string()).await;
            seed_sale(&pool, &sale_id.to_string(), &cust_id.to_string(), &wallet_id.to_string()).await;

            let sale_date = Utc::now() - Duration::days(days_ago as i64);
            debt_queries::insert_debt(
                &pool,
                &debt_id.to_string(),
                &cust_id.to_string(),
                &sale_id.to_string(),
                100.0,
                100.0,
                &sale_date.to_rfc3339(),
                &Utc::now().to_rfc3339(),
            )
            .await
            .expect("insert debt failed");

            let debts = svc.get_active_debts().await.unwrap();
            prop_assert_eq!(debts.len(), 1);

            // The overdue_days should equal the number of days since sale_date.
            // Allow ±1 day tolerance for clock boundary (test may run across midnight).
            let expected = days_ago as i32;
            let actual = debts[0].overdue_days;
            prop_assert!(
                (actual - expected).abs() <= 1,
                "Expected overdue_days ~{} but got {} for sale {} days ago",
                expected, actual, days_ago
            );

            Ok(())
        })?;
    }
}

// ── Property 28: Debt aging report sort order ──────────────────────────────
//
// For any set of active debt records, the debt aging report SHALL return
// them sorted by overdue days in descending order.
//
// **Validates: Requirements 10.2**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(20))]

    #[test]
    fn prop28_aging_report_sorted_desc(
        // Generate 2-6 distinct day offsets for debts
        debt_days in prop::collection::vec(1u32..500u32, 2..=6),
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;

            let cust_id = Uuid::new_v4();
            let wallet_id = Uuid::new_v4();
            let mobile = format!("05{:08}", 99999);

            seed_customer(&pool, &cust_id.to_string(), "ReportCustomer", &mobile).await;
            seed_wallet(&pool, &wallet_id.to_string()).await;

            // Create a debt for each day offset
            for (i, &days) in debt_days.iter().enumerate() {
                let sale_id = Uuid::new_v4();
                let debt_id = Uuid::new_v4();
                seed_sale(&pool, &sale_id.to_string(), &cust_id.to_string(), &wallet_id.to_string()).await;

                let sale_date = Utc::now() - Duration::days(days as i64);
                debt_queries::insert_debt(
                    &pool,
                    &debt_id.to_string(),
                    &cust_id.to_string(),
                    &sale_id.to_string(),
                    (i as f64 + 1.0) * 50.0,
                    (i as f64 + 1.0) * 50.0,
                    &sale_date.to_rfc3339(),
                    &Utc::now().to_rfc3339(),
                )
                .await
                .expect("insert debt failed");
            }

            let report = svc.get_debt_aging_report().await.unwrap();
            prop_assert_eq!(report.len(), debt_days.len());

            // Verify descending order by overdue_days
            for window in report.windows(2) {
                prop_assert!(
                    window[0].overdue_days >= window[1].overdue_days,
                    "Aging report not sorted descending: {} should be >= {}",
                    window[0].overdue_days, window[1].overdue_days
                );
            }

            Ok(())
        })?;
    }
}

// ── Property 29: Debt payment FIFO allocation ──────────────────────────────
//
// For any customer with multiple active debts and for any payment amount,
// the payment SHALL be applied to debts in order of oldest sale_date first.
// Each debt's remaining_amount SHALL decrease by the allocated portion,
// and debts fully paid SHALL be marked as settled.
//
// **Validates: Requirements 10.3, 10.4**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(20))]

    #[test]
    fn prop29_payment_fifo_allocation(
        // Generate 2-4 debt amounts (in cents, converted to dollars)
        debt_amounts in prop::collection::vec(100u64..10_000u64, 2..=4),
        // Payment as a percentage (1-150%) of total debt
        payment_pct in 1u32..=150u32,
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;

            let cust_id = Uuid::new_v4();
            let wallet_id = Uuid::new_v4();
            let mobile = format!("05{:08}", 88888);

            seed_customer(&pool, &cust_id.to_string(), "FIFOCustomer", &mobile).await;
            seed_wallet(&pool, &wallet_id.to_string()).await;

            // Create debts with increasing sale_dates (oldest first)
            let mut debt_ids = Vec::new();
            let mut amounts_f64 = Vec::new();
            let num_debts = debt_amounts.len();

            for (i, &cents) in debt_amounts.iter().enumerate() {
                let sale_id = Uuid::new_v4();
                let debt_id = Uuid::new_v4();
                seed_sale(&pool, &sale_id.to_string(), &cust_id.to_string(), &wallet_id.to_string()).await;

                let amount = cents as f64 / 100.0;
                amounts_f64.push(amount);

                // Oldest debt is furthest in the past
                let days_ago = ((num_debts - i) * 10) as i64;
                let sale_date = Utc::now() - Duration::days(days_ago);

                debt_queries::insert_debt(
                    &pool,
                    &debt_id.to_string(),
                    &cust_id.to_string(),
                    &sale_id.to_string(),
                    amount,
                    amount,
                    &sale_date.to_rfc3339(),
                    &Utc::now().to_rfc3339(),
                )
                .await
                .expect("insert debt failed");

                debt_ids.push(debt_id);
            }

            let total_debt: f64 = amounts_f64.iter().sum();
            let payment_amount = (total_debt * payment_pct as f64 / 100.0 * 100.0).floor() / 100.0;

            // Skip zero payments
            if payment_amount <= 0.0 {
                return Ok(());
            }

            let payment = svc.record_payment(cust_id, payment_amount, wallet_id).await.unwrap();

            // Verify FIFO: allocations should be in order of oldest debt first
            // (debt_ids[0] is the oldest since we created them with decreasing days_ago)
            let mut remaining_payment = payment_amount;
            for (i, alloc) in payment.allocations.iter().enumerate() {
                // Allocation should match the i-th debt (oldest first)
                prop_assert_eq!(
                    alloc.debt_record_id, debt_ids[i],
                    "Allocation {} should target debt_ids[{}]", i, i
                );

                let expected_alloc = remaining_payment.min(amounts_f64[i]);
                prop_assert!(
                    (alloc.amount_applied - expected_alloc).abs() < 1e-9,
                    "Allocation {} should be {} but was {}",
                    i, expected_alloc, alloc.amount_applied
                );

                remaining_payment -= expected_alloc;
                if remaining_payment <= 0.0 {
                    break;
                }
            }

            // Verify settled debts are no longer active
            let active = svc.get_active_debts().await.unwrap();
            let mut paid_so_far = payment_amount;
            let mut expected_settled = 0;
            for &amt in &amounts_f64 {
                if paid_so_far >= amt {
                    expected_settled += 1;
                    paid_so_far -= amt;
                } else {
                    break;
                }
            }

            let expected_active = num_debts - expected_settled;
            // If payment covers partial of next debt, that debt is still active
            prop_assert_eq!(
                active.len(), expected_active,
                "Expected {} active debts after payment of {} against total {}",
                expected_active, payment_amount, total_debt
            );

            Ok(())
        })?;
    }
}

// ── Property 30: Debt critical flag threshold ──────────────────────────────
//
// For any debt record, is_critical SHALL be true if and only if
// overdue_days > 30.
//
// **Validates: Requirements 10.5**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(30))]

    #[test]
    fn prop30_critical_flag_iff_over_30_days(
        days_ago in 0u32..365u32,
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;

            let cust_id = Uuid::new_v4();
            let sale_id = Uuid::new_v4();
            let debt_id = Uuid::new_v4();
            let wallet_id = Uuid::new_v4();
            let mobile = format!("05{:08}", days_ago + 50000);

            seed_customer(&pool, &cust_id.to_string(), "CriticalCustomer", &mobile).await;
            seed_wallet(&pool, &wallet_id.to_string()).await;
            seed_sale(&pool, &sale_id.to_string(), &cust_id.to_string(), &wallet_id.to_string()).await;

            let sale_date = Utc::now() - Duration::days(days_ago as i64);
            debt_queries::insert_debt(
                &pool,
                &debt_id.to_string(),
                &cust_id.to_string(),
                &sale_id.to_string(),
                100.0,
                100.0,
                &sale_date.to_rfc3339(),
                &Utc::now().to_rfc3339(),
            )
            .await
            .expect("insert debt failed");

            let debts = svc.get_active_debts().await.unwrap();
            prop_assert_eq!(debts.len(), 1);

            let debt = &debts[0];
            let expected_critical = debt.overdue_days > 30;

            prop_assert_eq!(
                debt.is_critical, expected_critical,
                "For overdue_days={}, is_critical should be {} but was {}",
                debt.overdue_days, expected_critical, debt.is_critical
            );

            // Explicit boundary checks:
            // - At exactly 30 days: NOT critical
            // - At 31+ days: critical
            if debt.overdue_days <= 30 {
                prop_assert!(!debt.is_critical, "Debt with {} overdue days should NOT be critical", debt.overdue_days);
            } else {
                prop_assert!(debt.is_critical, "Debt with {} overdue days SHOULD be critical", debt.overdue_days);
            }

            Ok(())
        })?;
    }
}
