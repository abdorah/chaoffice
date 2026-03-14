// Property tests for Reporting and Invoicing (Properties 32, 33, 34)
//
// **Validates: Requirements 12.1, 12.2, 12.3**

mod generators;

use proptest::prelude::*;
use tokio::runtime::Runtime;

use chrono::{Duration, Utc};
use sweet_lab_core::expenses::service::ExpenseServiceImpl;
use sweet_lab_core::models::domain::{ExpenseCategory, SaleLineItem};
use sweet_lab_core::models::Money;
use sweet_lab_core::persistence::db;
use sweet_lab_core::persistence::queries::customers as customer_queries;
use sweet_lab_core::persistence::queries::finished_goods as fg_queries;
use sweet_lab_core::persistence::queries::raw_materials as rm_queries;
use sweet_lab_core::persistence::queries::wallets as wallet_queries;
use sweet_lab_core::reports::financial;
use sweet_lab_core::reports::inventory_report;
use sweet_lab_core::sales::transactions::SalesServiceImpl;
use uuid::Uuid;

// ── Helpers ────────────────────────────────────────────────────────────────

async fn setup() -> sqlx::SqlitePool {
    db::init_db(":memory:").await.expect("DB init failed")
}

async fn seed_wallet(pool: &sqlx::SqlitePool, id: &str, balance_cents: i64) {
    let now = Utc::now().to_rfc3339();
    wallet_queries::insert_wallet(pool, id, "Test Wallet", "Cash", balance_cents, &now)
        .await
        .expect("seed wallet failed");
}

async fn seed_customer(pool: &sqlx::SqlitePool, id: &str, mobile: &str) {
    let now = Utc::now().to_rfc3339();
    customer_queries::insert(pool, id, "Test Customer", "Test City", mobile, &now)
        .await
        .expect("seed customer failed");
}

async fn seed_user(pool: &sqlx::SqlitePool, id: &str) {
    let now = Utc::now().to_rfc3339();
    sqlx::query(
        "INSERT INTO users (id, username, full_name, role, password_hash, sync_status, updated_at, created_at)
         VALUES (?, 'testuser', 'Test User', 'Admin', 'hash', 'Synced', ?, ?)",
    )
    .bind(id)
    .bind(&now)
    .bind(&now)
    .execute(pool)
    .await
    .expect("seed user failed");
}

async fn seed_finished_good(pool: &sqlx::SqlitePool, id: &str, name: &str, qty: f64, price_cents: i64) {
    let now = Utc::now().to_rfc3339();
    fg_queries::insert(pool, id, name, qty, price_cents, &now)
        .await
        .expect("seed finished good failed");
}


// ── Property 32: Financial summary calculation ─────────────────────────────
//
// For any date range, the financial summary SHALL report:
// total_revenue = sum of all sale total_amounts,
// total_expenses = sum of all expense amounts,
// net_profit = total_revenue - total_expenses.
//
// **Validates: Requirements 12.1**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(20))]

    #[test]
    fn prop32_financial_summary_calculation(
        // Generate 1-5 sales, each with 1-3 line items
        sale_specs in prop::collection::vec(
            prop::collection::vec(
                1i32..=20i32,  // quantity only — price comes from DB
                1..=3
            ),
            1..=5
        ),
        // Generate 0-4 expenses
        expense_amounts in prop::collection::vec(1u64..=5000u64, 0..=4),
        // The unit price for the finished good (in cents)
        fg_price_cents in 100i64..=10000i64,
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let pool = setup().await;

            let wallet_id = Uuid::new_v4();
            let customer_id = Uuid::new_v4();
            let user_id = Uuid::new_v4();
            let fg_id = Uuid::new_v4();

            // Compute expected revenue from sale specs (as Money)
            let fg_price = Money(fg_price_cents);
            let mut expected_revenue = Money::ZERO;
            for sale_items in &sale_specs {
                let sale_total: Money = sale_items
                    .iter()
                    .map(|qty| fg_price * (*qty as i64))
                    .sum();
                expected_revenue = expected_revenue + sale_total;
            }

            let expected_expenses: Money = expense_amounts.iter().map(|c| Money(*c as i64)).sum();

            // Need enough stock for all sales
            let total_qty: i32 = sale_specs
                .iter()
                .flat_map(|items| items.iter().map(|q| *q))
                .sum();

            // Seed data: wallet with enough balance for expenses, large stock
            seed_wallet(&pool, &wallet_id.to_string(), expected_expenses.0 + 1_000_000).await;
            seed_customer(&pool, &customer_id.to_string(), "+963900000001").await;
            seed_user(&pool, &user_id.to_string()).await;
            seed_finished_good(&pool, &fg_id.to_string(), "Product", total_qty as f64 + 1000.0, fg_price_cents).await;

            let sales_svc = SalesServiceImpl::new(pool.clone());
            let expense_svc = ExpenseServiceImpl::new(pool.clone());

            // Create sales
            for sale_items in &sale_specs {
                let line_items: Vec<SaleLineItem> = sale_items
                    .iter()
                    .map(|qty| SaleLineItem {
                        finished_good_id: fg_id,
                        finished_good_name: "Product".into(),
                        quantity: *qty,
                        unit_price: fg_price,
                    })
                    .collect();
                let total: Money = line_items
                    .iter()
                    .map(|li| li.unit_price * li.quantity as i64)
                    .sum();
                sales_svc
                    .create_sale(customer_id, line_items, total, wallet_id)
                    .await
                    .unwrap();
            }

            // Record expenses
            for (i, cents) in expense_amounts.iter().enumerate() {
                let amount = Money(*cents as i64);
                expense_svc
                    .record_expense(
                        &format!("Expense {i}"),
                        amount,
                        ExpenseCategory::Purchase,
                        wallet_id,
                        user_id,
                    )
                    .await
                    .unwrap();
            }

            // Query financial summary over a wide date range
            let start = Utc::now() - Duration::hours(1);
            let end = Utc::now() + Duration::hours(1);
            let summary = financial::get_financial_summary(&pool, start, end).await.unwrap();

            // Verify: total_revenue = sum of all sale total_amounts
            prop_assert_eq!(
                summary.total_revenue, expected_revenue,
                "Revenue: expected {} got {}",
                expected_revenue, summary.total_revenue
            );

            // Verify: total_expenses = sum of all expense amounts
            prop_assert_eq!(
                summary.total_expenses, expected_expenses,
                "Expenses: expected {} got {}",
                expected_expenses, summary.total_expenses
            );

            // Verify: net_profit = total_revenue - total_expenses
            let expected_net = expected_revenue - expected_expenses;
            prop_assert_eq!(
                summary.net_profit, expected_net,
                "Net profit: expected {} got {}",
                expected_net, summary.net_profit
            );

            Ok(())
        })?;
    }
}


// ── Property 33: Low stock alert accuracy ──────────────────────────────────
//
// For any inventory item and for any configurable threshold T, the item
// SHALL be flagged as low stock if and only if its current quantity is below T.
//
// **Validates: Requirements 12.2**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(30))]

    #[test]
    fn prop33_low_stock_alert_accuracy(
        // Generate raw materials with various quantities (cents to avoid fp issues)
        rm_quantities in prop::collection::vec(0u64..100_000u64, 1..=5),
        // Generate finished goods with various quantities
        fg_quantities in prop::collection::vec(0u64..100_000u64, 1..=5),
        // Threshold (cents)
        threshold_cents in 0u64..50_000u64,
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let pool = setup().await;
            let now = Utc::now().to_rfc3339();
            let threshold = threshold_cents as f64 / 100.0;

            // Seed raw materials
            let mut rm_ids = Vec::new();
            for (i, qty_cents) in rm_quantities.iter().enumerate() {
                let id = Uuid::new_v4();
                let qty = *qty_cents as f64 / 100.0;
                rm_queries::insert(&pool, &id.to_string(), &format!("RM_{i}"), "kg", qty, &now)
                    .await
                    .unwrap();
                rm_ids.push((id, qty));
            }

            // Seed finished goods
            let mut fg_ids = Vec::new();
            for (i, qty_cents) in fg_quantities.iter().enumerate() {
                let id = Uuid::new_v4();
                let qty = *qty_cents as f64 / 100.0;
                fg_queries::insert(&pool, &id.to_string(), &format!("FG_{i}"), qty, 1000, &now)
                    .await
                    .unwrap();
                fg_ids.push((id, qty));
            }

            let report = inventory_report::get_inventory_report(&pool, threshold).await.unwrap();

            // Verify raw materials: flagged iff quantity < threshold
            prop_assert_eq!(
                report.raw_materials.len(), rm_ids.len(),
                "Raw material count mismatch"
            );
            for rm_report in &report.raw_materials {
                let expected_low = rm_report.material.current_quantity < threshold;
                prop_assert_eq!(
                    rm_report.is_low_stock, expected_low,
                    "RM '{}': qty={}, threshold={}, expected low_stock={} got {}",
                    rm_report.material.name,
                    rm_report.material.current_quantity,
                    threshold,
                    expected_low,
                    rm_report.is_low_stock
                );
            }

            // Verify finished goods: flagged iff quantity < threshold
            prop_assert_eq!(
                report.finished_goods.len(), fg_ids.len(),
                "Finished good count mismatch"
            );
            for fg_report in &report.finished_goods {
                let expected_low = fg_report.good.current_quantity < threshold;
                prop_assert_eq!(
                    fg_report.is_low_stock, expected_low,
                    "FG '{}': qty={}, threshold={}, expected low_stock={} got {}",
                    fg_report.good.name,
                    fg_report.good.current_quantity,
                    threshold,
                    expected_low,
                    fg_report.is_low_stock
                );
            }

            Ok(())
        })?;
    }
}


// ── Property 34: Invoice completeness ──────────────────────────────────────
//
// For any generated invoice, the document SHALL contain the business name,
// customer name, customer city, customer mobile, itemized products with
// quantities and prices, total amount, payment status, and date.
//
// **Validates: Requirements 12.3**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(20))]

    #[test]
    fn prop34_invoice_completeness(
        // Generate 1-4 line items for the sale
        item_quantities in prop::collection::vec(1i32..=10i32, 1..=4),
        // Payment fraction: 0-100% of total
        payment_pct in 0u32..=100u32,
        // The unit price for the finished good (in cents)
        fg_price_cents in 100i64..=10000i64,
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let pool = setup().await;

            let wallet_id = Uuid::new_v4();
            let customer_id = Uuid::new_v4();
            let fg_id = Uuid::new_v4();

            // Total stock needed
            let total_qty: i32 = item_quantities.iter().sum();

            seed_wallet(&pool, &wallet_id.to_string(), 100_000_000).await;
            seed_customer(&pool, &customer_id.to_string(), "+963900000002").await;
            seed_finished_good(&pool, &fg_id.to_string(), "Sweet Box", total_qty as f64 + 100.0, fg_price_cents).await;

            let sales_svc = SalesServiceImpl::new(pool.clone());
            let fg_price = Money(fg_price_cents);

            let line_items: Vec<SaleLineItem> = item_quantities
                .iter()
                .map(|qty| SaleLineItem {
                    finished_good_id: fg_id,
                    finished_good_name: "Sweet Box".into(),
                    quantity: *qty,
                    unit_price: fg_price,
                })
                .collect();

            let total: Money = line_items
                .iter()
                .map(|li| li.unit_price * li.quantity as i64)
                .sum();

            let amount_paid_f64 = (total.to_f64() * payment_pct as f64 / 100.0 * 100.0).floor() / 100.0;
            let amount_paid = Money::from_f64(amount_paid_f64);

            let sale = sales_svc
                .create_sale(customer_id, line_items.clone(), amount_paid, wallet_id)
                .await
                .unwrap();

            // Generate invoice
            let invoice = financial::generate_invoice(&pool, sale.id, "Sweet Lab").await.unwrap();

            // Verify: business name present
            prop_assert!(
                !invoice.business_name.is_empty(),
                "Invoice must contain business name"
            );
            prop_assert_eq!(&invoice.business_name, "Sweet Lab");

            // Verify: customer details present
            prop_assert!(
                !invoice.customer_name.is_empty(),
                "Invoice must contain customer name"
            );
            prop_assert_eq!(&invoice.customer_name, "Test Customer");

            prop_assert!(
                !invoice.customer_city.is_empty(),
                "Invoice must contain customer city"
            );
            prop_assert_eq!(&invoice.customer_city, "Test City");

            prop_assert!(
                !invoice.customer_mobile.is_empty(),
                "Invoice must contain customer mobile"
            );
            prop_assert_eq!(&invoice.customer_mobile, "+963900000002");

            // Verify: itemized products with quantities and prices
            prop_assert!(
                !invoice.line_items.is_empty(),
                "Invoice must contain at least one line item"
            );
            prop_assert_eq!(
                invoice.line_items.len(), item_quantities.len(),
                "Invoice line item count must match sale"
            );
            for li in &invoice.line_items {
                prop_assert!(li.quantity > 0, "Line item quantity must be positive");
                prop_assert!(li.unit_price >= Money::ZERO, "Line item price must be non-negative");
                prop_assert!(
                    !li.finished_good_name.is_empty(),
                    "Line item must have a product name"
                );
            }

            // Verify: total amount
            prop_assert_eq!(
                invoice.total_amount, total,
                "Invoice total: expected {} got {}",
                total, invoice.total_amount
            );

            // Verify: payment status (amount_paid and remaining_balance)
            prop_assert_eq!(
                invoice.amount_paid, amount_paid,
                "Invoice amount_paid: expected {} got {}",
                amount_paid, invoice.amount_paid
            );
            let expected_remaining = total - amount_paid;
            prop_assert_eq!(
                invoice.remaining_balance, expected_remaining,
                "Invoice remaining_balance: expected {} got {}",
                expected_remaining, invoice.remaining_balance
            );

            // Verify: date present
            prop_assert!(
                !invoice.date.is_empty(),
                "Invoice must contain a date"
            );

            // Verify: invoice number present
            prop_assert!(
                !invoice.invoice_number.is_empty(),
                "Invoice must contain an invoice number"
            );

            Ok(())
        })?;
    }
}
