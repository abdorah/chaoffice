// Property tests for Sales Transactions (Properties 20, 21, 22)
//
// **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5**

mod generators;

use proptest::prelude::*;
use tokio::runtime::Runtime;

use sweet_lab_core::models::domain::SaleLineItem;
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
/// Returns (customer_id, wallet_id, vec of (fg_id, fg_name)).
async fn seed_fixtures(
    pool: &sqlx::SqlitePool,
    wallet_balance: f64,
    goods: &[(uuid::Uuid, &str, f64, f64)], // (id, name, quantity, unit_price)
) -> (uuid::Uuid, uuid::Uuid) {
    let now = chrono::Utc::now().to_rfc3339();
    let cust_id = uuid::Uuid::new_v4();
    let wallet_id = uuid::Uuid::new_v4();

    customer_queries::insert(pool, &cust_id.to_string(), "Test Customer", "Test City", &format!("+963{}", uuid::Uuid::new_v4().as_u128() % 1_000_000_000), &now)
        .await
        .expect("seed customer failed");

    wallet_queries::insert_wallet(pool, &wallet_id.to_string(), "Cash", "Cash", wallet_balance, &now)
        .await
        .expect("seed wallet failed");

    for (id, name, qty, price) in goods {
        fg_queries::insert(pool, &id.to_string(), name, *qty, *price, &now)
            .await
            .expect("seed finished good failed");
    }

    (cust_id, wallet_id)
}

// ── Property 20: Sale total calculation ────────────────────────────────────
//
// For any list of SaleLineItems, the sale total SHALL equal the sum of
// (quantity × unit_price) for each line item.
//
// **Validates: Requirements 8.4**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(30))]

    #[test]
    fn prop20_sale_total_equals_sum_of_qty_times_price(
        // Generate 1-5 line items with reasonable quantities and prices
        num_items in 1usize..=5,
        quantities in prop::collection::vec(1i32..=20, 1..=5),
        prices in prop::collection::vec((1u64..=10_000u64).prop_map(|v| v as f64 / 100.0), 1..=5),
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;

            let item_count = num_items.min(quantities.len()).min(prices.len());
            if item_count == 0 {
                return Ok(());
            }

            // Create finished goods for each line item
            let mut goods = Vec::new();
            let mut line_items = Vec::new();
            for i in 0..item_count {
                let fg_id = uuid::Uuid::new_v4();
                let qty = quantities[i];
                let price = prices[i];
                let name = format!("Good_{}", i);
                // Stock enough to cover the sale
                goods.push((fg_id, name.clone(), qty as f64 + 10.0, price));
                line_items.push(SaleLineItem {
                    finished_good_id: fg_id,
                    finished_good_name: name,
                    quantity: qty,
                    unit_price: price,
                });
            }

            let goods_refs: Vec<(uuid::Uuid, &str, f64, f64)> = goods
                .iter()
                .map(|(id, name, qty, price)| (*id, name.as_str(), *qty, *price))
                .collect();

            // Expected total
            let expected_total: f64 = line_items
                .iter()
                .map(|item| item.quantity as f64 * item.unit_price)
                .sum();

            // Seed with enough wallet balance for full payment
            let (cust_id, wallet_id) = seed_fixtures(&pool, expected_total + 1000.0, &goods_refs).await;

            let sale = svc.create_sale(cust_id, line_items, expected_total, wallet_id).await;
            prop_assert!(sale.is_ok(), "Sale creation failed: {:?}", sale.unwrap_err());

            let sale = sale.unwrap();
            prop_assert!(
                (sale.total_amount - expected_total).abs() < 1e-9,
                "Sale total should be {} but was {}",
                expected_total, sale.total_amount
            );

            Ok(())
        })?;
    }
}

// ── Property 21: Sale financial orchestration ──────────────────────────────
//
// For any sale with total_amount T and amount_paid P:
// (a) the specified wallet's balance SHALL increase by P,
// (b) inventory SHALL decrease by the sold quantities, and
// (c) if P < T, a debt record SHALL be created for (T - P) linked to the customer.
//
// **Validates: Requirements 8.1, 8.2, 8.3**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(30))]

    #[test]
    fn prop21_sale_financial_orchestration(
        qty in 1i32..=10,
        unit_price in (1u64..=5_000u64).prop_map(|v| v as f64 / 100.0),
        // payment_pct: 0 = no payment, 100 = full payment, in between = partial
        payment_pct in 0u64..=100u64,
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;

            let fg_id = uuid::Uuid::new_v4();
            let stock = qty as f64 + 10.0;
            let initial_wallet_balance = 500.0;

            let goods = vec![(fg_id, "Sweet Box", stock, unit_price)];
            let (cust_id, wallet_id) = seed_fixtures(&pool, initial_wallet_balance, &goods).await;

            let total = qty as f64 * unit_price;
            let amount_paid = (total * payment_pct as f64 / 100.0 * 100.0).floor() / 100.0;
            // Clamp to total
            let amount_paid = if amount_paid > total { total } else { amount_paid };

            let line_items = vec![SaleLineItem {
                finished_good_id: fg_id,
                finished_good_name: "Sweet Box".into(),
                quantity: qty,
                unit_price,
            }];

            let sale = svc.create_sale(cust_id, line_items, amount_paid, wallet_id).await;
            prop_assert!(sale.is_ok(), "Sale creation failed: {:?}", sale.unwrap_err());
            let sale = sale.unwrap();

            // (a) Wallet balance increased by amount_paid
            let wallet_row = wallet_queries::get_by_id(&pool, &wallet_id.to_string())
                .await
                .unwrap()
                .unwrap();
            let expected_wallet = initial_wallet_balance + amount_paid;
            prop_assert!(
                (wallet_row.current_balance - expected_wallet).abs() < 1e-9,
                "Wallet balance should be {} but was {}",
                expected_wallet, wallet_row.current_balance
            );

            // (b) Inventory decreased by sold quantity
            let fg_row = fg_queries::get_by_id(&pool, &fg_id.to_string())
                .await
                .unwrap()
                .unwrap();
            let expected_stock = stock - qty as f64;
            prop_assert!(
                (fg_row.current_quantity - expected_stock).abs() < 1e-9,
                "Finished good stock should be {} but was {}",
                expected_stock, fg_row.current_quantity
            );

            // (c) If partial payment, debt record created for (total - amount_paid)
            let debt_amount = total - amount_paid;
            if debt_amount > 1e-9 {
                // There should be a debt record
                let debts = sqlx::query_as::<_, (String, f64, f64)>(
                    "SELECT customer_id, original_amount, remaining_amount FROM debt_records WHERE sale_id = ?"
                )
                .bind(sale.id.to_string())
                .fetch_all(&pool)
                .await
                .unwrap();

                prop_assert!(
                    !debts.is_empty(),
                    "Debt record should exist for partial payment (debt_amount={})",
                    debt_amount
                );

                let (ref debt_cust_id, orig, remaining) = debts[0];
                prop_assert_eq!(debt_cust_id, &cust_id.to_string());
                prop_assert!(
                    (orig - debt_amount).abs() < 1e-9,
                    "Debt original_amount should be {} but was {}",
                    debt_amount, orig
                );
                prop_assert!(
                    (remaining - debt_amount).abs() < 1e-9,
                    "Debt remaining_amount should be {} but was {}",
                    debt_amount, remaining
                );
            } else {
                // Full payment — no debt record
                let debts: Vec<(String,)> = sqlx::query_as(
                    "SELECT id FROM debt_records WHERE sale_id = ?"
                )
                .bind(sale.id.to_string())
                .fetch_all(&pool)
                .await
                .unwrap();

                prop_assert!(
                    debts.is_empty(),
                    "No debt record should exist for full payment"
                );
            }

            Ok(())
        })?;
    }
}

// ── Property 22: Receipt completeness ──────────────────────────────────────
//
// For any finalized sale, the generated Receipt SHALL contain the customer
// name, itemized list of products, total amount, amount paid, remaining
// balance, and date.
//
// **Validates: Requirements 8.5**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(30))]

    #[test]
    fn prop22_receipt_completeness(
        qty in 1i32..=10,
        unit_price in (1u64..=5_000u64).prop_map(|v| v as f64 / 100.0),
        payment_pct in 0u64..=100u64,
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let (pool, svc) = setup().await;

            let fg_id = uuid::Uuid::new_v4();
            let stock = qty as f64 + 10.0;

            let goods = vec![(fg_id, "Baklava Box", stock, unit_price)];
            let (cust_id, wallet_id) = seed_fixtures(&pool, 10_000.0, &goods).await;

            let total = qty as f64 * unit_price;
            let amount_paid = (total * payment_pct as f64 / 100.0 * 100.0).floor() / 100.0;
            let amount_paid = if amount_paid > total { total } else { amount_paid };

            let line_items = vec![SaleLineItem {
                finished_good_id: fg_id,
                finished_good_name: "Baklava Box".into(),
                quantity: qty,
                unit_price,
            }];

            let sale = svc.create_sale(cust_id, line_items, amount_paid, wallet_id).await;
            prop_assert!(sale.is_ok(), "Sale failed: {:?}", sale.unwrap_err());
            let sale = sale.unwrap();

            let receipt = receipts::generate_receipt(&pool, sale.id).await;
            prop_assert!(receipt.is_ok(), "Receipt failed: {:?}", receipt.unwrap_err());
            let receipt = receipt.unwrap();

            // Customer name present
            prop_assert!(
                !receipt.customer_name.is_empty(),
                "Receipt must contain customer name"
            );
            prop_assert_eq!(&receipt.customer_name, "Test Customer");

            // Customer city and mobile present
            prop_assert!(!receipt.customer_city.is_empty(), "Receipt must contain customer city");
            prop_assert!(!receipt.customer_mobile.is_empty(), "Receipt must contain customer mobile");

            // Business name present
            prop_assert!(!receipt.business_name.is_empty(), "Receipt must contain business name");

            // Itemized list present and matches
            prop_assert!(
                !receipt.sale.line_items.is_empty(),
                "Receipt must contain itemized list"
            );
            prop_assert_eq!(receipt.sale.line_items.len(), 1);
            prop_assert_eq!(receipt.sale.line_items[0].quantity, qty);
            prop_assert!(
                (receipt.sale.line_items[0].unit_price - unit_price).abs() < 1e-9,
                "Line item unit_price mismatch"
            );

            // Total amount
            prop_assert!(
                (receipt.sale.total_amount - total).abs() < 1e-9,
                "Receipt total should be {} but was {}",
                total, receipt.sale.total_amount
            );

            // Amount paid
            prop_assert!(
                (receipt.sale.amount_paid - amount_paid).abs() < 1e-9,
                "Receipt amount_paid should be {} but was {}",
                amount_paid, receipt.sale.amount_paid
            );

            // Remaining balance
            let expected_remaining = total - amount_paid;
            prop_assert!(
                (receipt.remaining_balance - expected_remaining).abs() < 1e-9,
                "Receipt remaining_balance should be {} but was {}",
                expected_remaining, receipt.remaining_balance
            );

            // Date present
            prop_assert!(
                !receipt.formatted_date.is_empty(),
                "Receipt must contain a formatted date"
            );

            Ok(())
        })?;
    }
}
