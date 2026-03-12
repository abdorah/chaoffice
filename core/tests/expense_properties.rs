// Property tests for Expense Management (Properties 25, 31)
//
// **Validates: Requirements 9.4, 11.1, 11.2, 11.4**

mod generators;

use proptest::prelude::*;
use tokio::runtime::Runtime;

use chrono::{Duration, Utc};
use sweet_lab_core::expenses::service::ExpenseServiceImpl;
use sweet_lab_core::models::domain::ExpenseCategory;
use sweet_lab_core::persistence::db;
use sweet_lab_core::persistence::queries::wallets as wallet_queries;
use uuid::Uuid;

// ── Helpers ────────────────────────────────────────────────────────────────

async fn setup() -> (sqlx::SqlitePool, ExpenseServiceImpl) {
    let pool = db::init_db(":memory:").await.expect("DB init failed");
    let svc = ExpenseServiceImpl::new(pool.clone());
    (pool, svc)
}

async fn seed_wallet(pool: &sqlx::SqlitePool, id: &str, name: &str, balance: f64) {
    let now = Utc::now().to_rfc3339();
    wallet_queries::insert_wallet(pool, id, name, "Cash", balance, &now)
        .await
        .expect("seed wallet failed");
}

async fn seed_user(pool: &sqlx::SqlitePool, id: &str) {
    let now = Utc::now().to_rfc3339();
    sqlx::query(
        "INSERT INTO users (id, username, full_name, role, password_hash, sync_status, updated_at, created_at)
         VALUES (?, 'testuser', 'Test User', 'Representative', 'hash', 'Synced', ?, ?)",
    )
    .bind(id)
    .bind(&now)
    .bind(&now)
    .execute(pool)
    .await
    .expect("seed user failed");
}

// ── Property 25: Expense wallet debit ──────────────────────────────────────
//
// For any expense with amount A recorded against a wallet with sufficient
// balance B, the wallet balance SHALL decrease by A and the expense record
// SHALL contain description, amount, category, wallet source, and timestamp.
//
// **Validates: Requirements 9.4, 11.1, 11.2**

fn arb_category() -> impl Strategy<Value = ExpenseCategory> {
    prop_oneof![
        Just(ExpenseCategory::Purchase),
        Just(ExpenseCategory::OperatingCost),
    ]
}

proptest! {
    #![proptest_config(ProptestConfig::with_cases(30))]

    #[test]
    fn prop25_expense_debits_wallet_and_contains_all_fields(
        // Wallet initial balance (ensure it's always >= amount)
        balance_cents in 1000u64..1_000_000u64,
        // Expense amount as a fraction of balance (1-100%)
        amount_pct in 1u32..=100u32,
        category in arb_category(),
        desc_idx in 0usize..5usize,
    ) {
        let descriptions = ["Office supplies", "Milk purchase", "Electricity bill", "Sugar", "Packaging"];
        let description = descriptions[desc_idx];

        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;

            let wallet_id = Uuid::new_v4();
            let user_id = Uuid::new_v4();
            let initial_balance = balance_cents as f64 / 100.0;
            let amount = (initial_balance * amount_pct as f64 / 100.0 * 100.0).floor() / 100.0;

            // Skip if amount rounds to zero
            if amount <= 0.0 {
                return Ok(());
            }

            seed_wallet(&pool, &wallet_id.to_string(), "Test Wallet", initial_balance).await;
            seed_user(&pool, &user_id.to_string()).await;

            let expense = svc
                .record_expense(description, amount, category.clone(), wallet_id, user_id)
                .await
                .unwrap();

            // Verify wallet balance decreased by exactly the expense amount
            let wallet = wallet_queries::get_by_id(&pool, &wallet_id.to_string())
                .await
                .unwrap()
                .unwrap();
            let expected_balance = initial_balance - amount;
            prop_assert!(
                (wallet.current_balance - expected_balance).abs() < 1e-9,
                "Wallet balance should be {} but was {} (initial={}, amount={})",
                expected_balance, wallet.current_balance, initial_balance, amount
            );

            // Verify expense record contains all required fields
            prop_assert_eq!(&expense.description, description);
            prop_assert!(
                (expense.amount - amount).abs() < 1e-9,
                "Expense amount should be {} but was {}",
                amount, expense.amount
            );
            prop_assert_eq!(expense.category, category);
            prop_assert_eq!(expense.wallet_id, wallet_id);
            prop_assert_eq!(&expense.wallet_name, "Test Wallet");
            prop_assert_eq!(expense.recorded_by, user_id);
            // Timestamp should be recent (within last 5 seconds)
            let elapsed = Utc::now() - expense.timestamp;
            prop_assert!(
                elapsed.num_seconds() < 5,
                "Expense timestamp should be recent but was {} seconds ago",
                elapsed.num_seconds()
            );

            Ok(())
        })?;
    }
}

// ── Property 31: Expense report grouping and totals ────────────────────────
//
// For any set of expenses in a date range, the expense report SHALL group
// expenses by category, each category subtotal SHALL equal the sum of
// expense amounts in that category, and the grand total SHALL equal the
// sum of all subtotals.
//
// **Validates: Requirements 11.4**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(20))]

    #[test]
    fn prop31_expense_report_grouping_and_totals(
        // Generate 2-8 expenses with random categories and amounts
        expense_specs in prop::collection::vec(
            (1u64..50_000u64, prop_oneof![Just(0u8), Just(1u8)]),
            2..=8
        ),
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;

            let wallet_id = Uuid::new_v4();
            let user_id = Uuid::new_v4();

            // Seed wallet with enough balance for all expenses
            let total_cents: u64 = expense_specs.iter().map(|(c, _)| c).sum();
            let total_needed = total_cents as f64 / 100.0 + 1.0;
            seed_wallet(&pool, &wallet_id.to_string(), "Big Wallet", total_needed).await;
            seed_user(&pool, &user_id.to_string()).await;

            // Track expected amounts per category
            let mut expected_purchase_total = 0.0f64;
            let mut expected_operating_total = 0.0f64;
            let mut expected_purchase_count = 0usize;
            let mut expected_operating_count = 0usize;

            for (i, (cents, cat_flag)) in expense_specs.iter().enumerate() {
                let amount = *cents as f64 / 100.0;
                let category = if *cat_flag == 0 {
                    ExpenseCategory::Purchase
                } else {
                    ExpenseCategory::OperatingCost
                };

                match &category {
                    ExpenseCategory::Purchase => {
                        expected_purchase_total += amount;
                        expected_purchase_count += 1;
                    }
                    ExpenseCategory::OperatingCost => {
                        expected_operating_total += amount;
                        expected_operating_count += 1;
                    }
                }

                svc.record_expense(
                    &format!("Expense {i}"),
                    amount,
                    category,
                    wallet_id,
                    user_id,
                )
                .await
                .unwrap();
            }

            // Query grouped expenses over a wide date range
            let start = Utc::now() - Duration::hours(1);
            let end = Utc::now() + Duration::hours(1);
            let grouped = svc.get_expenses_by_category(start, end).await.unwrap();

            // Verify grouping: each category should have the correct count
            let purchase_expenses = grouped.get(&ExpenseCategory::Purchase);
            let operating_expenses = grouped.get(&ExpenseCategory::OperatingCost);

            let actual_purchase_count = purchase_expenses.map_or(0, |v| v.len());
            let actual_operating_count = operating_expenses.map_or(0, |v| v.len());

            prop_assert_eq!(
                actual_purchase_count, expected_purchase_count,
                "Purchase count: expected {} got {}",
                expected_purchase_count, actual_purchase_count
            );
            prop_assert_eq!(
                actual_operating_count, expected_operating_count,
                "OperatingCost count: expected {} got {}",
                expected_operating_count, actual_operating_count
            );

            // Verify subtotals: sum of amounts in each category
            let actual_purchase_total: f64 = purchase_expenses
                .map_or(0.0, |v| v.iter().map(|e| e.amount).sum());
            let actual_operating_total: f64 = operating_expenses
                .map_or(0.0, |v| v.iter().map(|e| e.amount).sum());

            prop_assert!(
                (actual_purchase_total - expected_purchase_total).abs() < 1e-6,
                "Purchase subtotal: expected {} got {}",
                expected_purchase_total, actual_purchase_total
            );
            prop_assert!(
                (actual_operating_total - expected_operating_total).abs() < 1e-6,
                "OperatingCost subtotal: expected {} got {}",
                expected_operating_total, actual_operating_total
            );

            // Verify grand total = sum of subtotals
            let grand_total = actual_purchase_total + actual_operating_total;
            let expected_grand = expected_purchase_total + expected_operating_total;
            prop_assert!(
                (grand_total - expected_grand).abs() < 1e-6,
                "Grand total: expected {} got {}",
                expected_grand, grand_total
            );

            Ok(())
        })?;
    }
}