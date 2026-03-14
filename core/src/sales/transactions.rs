use chrono::Utc;
use sqlx::SqlitePool;
use uuid::Uuid;

use crate::error::{AppError, AppResult};
use crate::models::domain::{Pagination, Sale, SaleLineItem};
use crate::models::Money;
use crate::persistence::queries::sales as sale_queries;
use crate::persistence::queries::wallets as wallet_queries;

/// Service for managing sales transactions.
pub struct SalesServiceImpl {
    pool: SqlitePool,
}

impl SalesServiceImpl {
    pub fn new(pool: SqlitePool) -> Self {
        Self { pool }
    }

    /// Create a sale: validate stock, deduct inventory, credit wallet, create debt if partial payment.
    /// All operations run inside a single SQLx transaction (Req 8.1, 8.2, 8.3, 8.4).
    pub async fn create_sale(
        &self,
        customer_id: Uuid,
        line_items: Vec<SaleLineItem>,
        amount_paid: Money,
        wallet_id: Uuid,
    ) -> AppResult<Sale> {
        // === Sale input validation (Req 5) ===

        // Reject empty line_items (Req 5.5)
        if line_items.is_empty() {
            return Err(AppError::Validation {
                field: "line_items".to_string(),
                message: "At least one line item required".to_string(),
            });
        }

        // Reject line items with quantity <= 0 (Req 5.4)
        for item in &line_items {
            if item.quantity <= 0 {
                return Err(AppError::Validation {
                    field: "quantity".to_string(),
                    message: "Quantity must be positive".to_string(),
                });
            }
        }

        // Reject negative amount_paid (Req 5.1)
        if amount_paid < Money::ZERO {
            return Err(AppError::Validation {
                field: "amount_paid".to_string(),
                message: "Cannot be negative".to_string(),
            });
        }

        // Calculate total = sum(qty × unit_price) (Req 8.4)
        let total_amount: Money = line_items
            .iter()
            .map(|item| item.unit_price * item.quantity as i64)
            .sum();

        // Reject amount_paid > total_amount (Req 5.2)
        if amount_paid > total_amount {
            return Err(AppError::Validation {
                field: "amount_paid".to_string(),
                message: "Cannot exceed total".to_string(),
            });
        }

        let now = Utc::now();
        let now_str = now.to_rfc3339();
        let sale_id = Uuid::new_v4();

        let mut tx = self.pool.begin().await?;

        // Verify each line item's unit_price against DB (Req 5.3)
        for item in &line_items {
            let fg = sale_queries::get_finished_good_in_tx(&mut *tx, &item.finished_good_id.to_string())
                .await?
                .ok_or_else(|| AppError::NotFound {
                    entity_type: "finished_good".to_string(),
                    entity_id: item.finished_good_id.to_string(),
                })?;
            if item.unit_price != Money(fg.unit_price) {
                return Err(AppError::Validation {
                    field: "unit_price".to_string(),
                    message: "Price mismatch".to_string(),
                });
            }
        }

        // Validate customer exists
        let customer = sale_queries::get_customer_detail(&mut *tx, &customer_id.to_string())
            .await?
            .ok_or_else(|| AppError::Validation {
                field: "customer_id".to_string(),
                message: format!("Customer {customer_id} not found"),
            })?;

        // For each line item: atomically deduct finished good stock (Req 4.3)
        for item in &line_items {
            let required = item.quantity as f64;

            // Use the shared atomic deduction query (Req 4.3)
            sale_queries::deduct_finished_good_in_tx(
                &mut *tx,
                &item.finished_good_id.to_string(),
                required,
                &now_str,
            )
            .await?;
        }

        // If amount_paid > 0: credit wallet (Req 8.2)
        if amount_paid.0 > 0 {
            let _wallet = wallet_queries::get_by_id_in_tx(&mut *tx, &wallet_id.to_string())
                .await?
                .ok_or_else(|| AppError::Validation {
                    field: "wallet_id".to_string(),
                    message: format!("Wallet {wallet_id} not found"),
                })?;

            // Credit wallet using delta operation (Req 7.3)
            wallet_queries::update_balance_in_tx(
                &mut *tx,
                &wallet_id.to_string(),
                amount_paid.0,
                &now_str,
            )
            .await?;

            // Log wallet transaction
            wallet_queries::insert_transaction_in_tx(
                &mut *tx,
                &Uuid::new_v4().to_string(),
                &wallet_id.to_string(),
                amount_paid.0,
                &format!("Sale payment from {}", customer.name),
                Some(&sale_id.to_string()),
                &now_str,
                &now_str,
            )
            .await?;
        }

        // INSERT sale
        sale_queries::insert_sale_in_tx(
            &mut *tx,
            &sale_id.to_string(),
            &customer_id.to_string(),
            total_amount.0,
            amount_paid.0,
            &wallet_id.to_string(),
            &now_str,
            &now_str,
        )
        .await?;

        // INSERT sale line items
        for item in &line_items {
            sale_queries::insert_line_item_in_tx(
                &mut *tx,
                &Uuid::new_v4().to_string(),
                &sale_id.to_string(),
                &item.finished_good_id.to_string(),
                &item.finished_good_name,
                item.quantity,
                item.unit_price.0,
            )
            .await?;
        }

        // If amount_paid < total: INSERT debt_record for (total - amount_paid) (Req 8.3)
        if amount_paid < total_amount {
            let debt_amount = total_amount - amount_paid;
            sale_queries::insert_debt_in_tx(
                &mut *tx,
                &Uuid::new_v4().to_string(),
                &customer_id.to_string(),
                &sale_id.to_string(),
                debt_amount.0,
                debt_amount.0,
                &now_str,
                &now_str,
            )
            .await?;
        }

        tx.commit().await?;

        Ok(Sale {
            id: sale_id,
            customer_id,
            customer_name: customer.name,
            line_items,
            total_amount,
            amount_paid,
            payment_wallet_id: wallet_id,
            timestamp: now,
        })
    }

    /// Fetch all sales with customer names and line items (Req 8.1).
    pub async fn get_sales_history(&self, pagination: Option<Pagination>) -> AppResult<Vec<Sale>> {
        let pg = pagination.unwrap_or_default();
        let sale_rows = sale_queries::list_all_with_customers(&self.pool, &pg).await?;

        // Collect all sale IDs for a single batched line-item query (Req 12.1 — N+1 elimination)
        let sale_ids: Vec<String> = sale_rows.iter().map(|r| r.id.clone()).collect();
        let all_line_item_rows = sale_queries::get_line_items_batch(&self.pool, &sale_ids).await?;

        // Group line items by sale_id in a HashMap
        let mut line_items_by_sale: std::collections::HashMap<String, Vec<SaleLineItem>> =
            std::collections::HashMap::new();
        for li in all_line_item_rows {
            let item = SaleLineItem {
                finished_good_id: crate::utils::parse_uuid("finished_good", &li.finished_good_id)?,
                finished_good_name: li.finished_good_name,
                quantity: li.quantity,
                unit_price: Money(li.unit_price),
            };
            line_items_by_sale
                .entry(li.sale_id)
                .or_default()
                .push(item);
        }

        let mut sales = Vec::with_capacity(sale_rows.len());
        for row in sale_rows {
            let line_items = line_items_by_sale.remove(&row.id).unwrap_or_default();

            let timestamp = crate::utils::parse_timestamp(&row.timestamp)?;

            sales.push(Sale {
                id: crate::utils::parse_uuid("sale", &row.id)?,
                customer_id: crate::utils::parse_uuid("customer", &row.customer_id)?,
                customer_name: row.customer_name,
                line_items,
                total_amount: Money(row.total_amount),
                amount_paid: Money(row.amount_paid),
                payment_wallet_id: crate::utils::parse_uuid("wallet", &row.payment_wallet_id)?,
                timestamp,
            });
        }

        Ok(sales)
    }
}


#[cfg(test)]
mod tests {
    use super::*;
    use crate::models::Money;
    use crate::persistence::db;
    use crate::persistence::queries::customers as customer_queries;
    use crate::persistence::queries::finished_goods as fg_queries;
    use crate::persistence::queries::wallets as wallet_queries;

    async fn setup() -> (SqlitePool, SalesServiceImpl) {
        let pool = db::init_db(":memory:").await.expect("DB init failed");
        let svc = SalesServiceImpl::new(pool.clone());
        (pool, svc)
    }

    /// Seed a customer, finished goods, and a wallet for sale tests.
    struct TestFixture {
        customer_id: Uuid,
        wallet_id: Uuid,
        fg1_id: Uuid,
        fg2_id: Uuid,
    }

    async fn seed_fixtures(pool: &SqlitePool) -> TestFixture {
        let now = Utc::now().to_rfc3339();
        let customer_id = Uuid::new_v4();
        let wallet_id = Uuid::new_v4();
        let fg1_id = Uuid::new_v4();
        let fg2_id = Uuid::new_v4();

        customer_queries::insert(pool, &customer_id.to_string(), "Ahmad", "Damascus", "+963911111111", &now)
            .await
            .unwrap();
        wallet_queries::insert_wallet(pool, &wallet_id.to_string(), "Cash Box", "Cash", 50000, &now)
            .await
            .unwrap();
        fg_queries::insert(pool, &fg1_id.to_string(), "Sweet Box", 100.0, 2500, &now)
            .await
            .unwrap();
        fg_queries::insert(pool, &fg2_id.to_string(), "Chocolate Bar", 50.0, 1000, &now)
            .await
            .unwrap();

        TestFixture { customer_id, wallet_id, fg1_id, fg2_id }
    }

    // ── Sale total calculation (Req 8.4) ───────────────────────────────

    #[tokio::test]
    async fn sale_total_equals_sum_of_qty_times_price() {
        let (pool, svc) = setup().await;
        let f = seed_fixtures(&pool).await;

        let items = vec![
            SaleLineItem {
                finished_good_id: f.fg1_id,
                finished_good_name: "Sweet Box".into(),
                quantity: 3,
                unit_price: Money::from_f64(25.0),
            },
            SaleLineItem {
                finished_good_id: f.fg2_id,
                finished_good_name: "Chocolate Bar".into(),
                quantity: 5,
                unit_price: Money::from_f64(10.0),
            },
        ];

        // total = 3*25 + 5*10 = 75 + 50 = 125
        let sale = svc.create_sale(f.customer_id, items, Money::from_f64(125.0), f.wallet_id).await.unwrap();
        assert_eq!(sale.total_amount, Money::from_f64(125.0));
    }

    // ── Full payment (Req 8.2) ─────────────────────────────────────────

    #[tokio::test]
    async fn full_payment_credits_wallet_deducts_inventory_no_debt() {
        let (pool, svc) = setup().await;
        let f = seed_fixtures(&pool).await;

        let items = vec![SaleLineItem {
            finished_good_id: f.fg1_id,
            finished_good_name: "Sweet Box".into(),
            quantity: 2,
            unit_price: Money::from_f64(25.0),
        }];

        let sale = svc.create_sale(f.customer_id, items, Money::from_f64(50.0), f.wallet_id).await.unwrap();
        assert_eq!(sale.total_amount, Money::from_f64(50.0));
        assert_eq!(sale.amount_paid, Money::from_f64(50.0));

        // Wallet credited: 50000 + 5000 = 55000 cents ($500 + $50 = $550)
        let wallet = wallet_queries::get_by_id(&pool, &f.wallet_id.to_string()).await.unwrap().unwrap();
        assert_eq!(wallet.current_balance, 55000);

        // Inventory deducted: 100 - 2 = 98
        let fg = fg_queries::get_by_id(&pool, &f.fg1_id.to_string()).await.unwrap().unwrap();
        assert_eq!(fg.current_quantity, 98.0);

        // No debt record created
        let debt_count: (i64,) = sqlx::query_as("SELECT COUNT(*) FROM debt_records WHERE sale_id = ?")
            .bind(sale.id.to_string())
            .fetch_one(&pool)
            .await
            .unwrap();
        assert_eq!(debt_count.0, 0);
    }

    // ── Partial payment (Req 8.3) ──────────────────────────────────────

    #[tokio::test]
    async fn partial_payment_credits_wallet_deducts_inventory_creates_debt() {
        let (pool, svc) = setup().await;
        let f = seed_fixtures(&pool).await;

        let items = vec![SaleLineItem {
            finished_good_id: f.fg1_id,
            finished_good_name: "Sweet Box".into(),
            quantity: 4,
            unit_price: Money::from_f64(25.0),
        }];

        // total = 100, paid = 60 → debt = 40
        let sale = svc.create_sale(f.customer_id, items, Money::from_f64(60.0), f.wallet_id).await.unwrap();
        assert_eq!(sale.total_amount, Money::from_f64(100.0));
        assert_eq!(sale.amount_paid, Money::from_f64(60.0));

        // Wallet credited: 50000 + 6000 = 56000 cents ($500 + $60 = $560)
        let wallet = wallet_queries::get_by_id(&pool, &f.wallet_id.to_string()).await.unwrap().unwrap();
        assert_eq!(wallet.current_balance, 56000);

        // Inventory deducted: 100 - 4 = 96
        let fg = fg_queries::get_by_id(&pool, &f.fg1_id.to_string()).await.unwrap().unwrap();
        assert_eq!(fg.current_quantity, 96.0);

        // Debt record created for 4000 cents ($40)
        let debt: (i64,) = sqlx::query_as(
            "SELECT remaining_amount FROM debt_records WHERE sale_id = ?",
        )
        .bind(sale.id.to_string())
        .fetch_one(&pool)
        .await
        .unwrap();
        assert_eq!(debt.0, 4000);
    }

    // ── No payment (Req 8.3) ───────────────────────────────────────────

    #[tokio::test]
    async fn no_payment_deducts_inventory_creates_full_debt_no_wallet_credit() {
        let (pool, svc) = setup().await;
        let f = seed_fixtures(&pool).await;

        let items = vec![SaleLineItem {
            finished_good_id: f.fg2_id,
            finished_good_name: "Chocolate Bar".into(),
            quantity: 3,
            unit_price: Money::from_f64(10.0),
        }];

        // total = 30, paid = 0 → debt = 30
        let sale = svc.create_sale(f.customer_id, items, Money::ZERO, f.wallet_id).await.unwrap();
        assert_eq!(sale.total_amount, Money::from_f64(30.0));
        assert_eq!(sale.amount_paid, Money::ZERO);

        // Wallet unchanged: still 50000 cents ($500)
        let wallet = wallet_queries::get_by_id(&pool, &f.wallet_id.to_string()).await.unwrap().unwrap();
        assert_eq!(wallet.current_balance, 50000);

        // Inventory deducted: 50 - 3 = 47
        let fg = fg_queries::get_by_id(&pool, &f.fg2_id.to_string()).await.unwrap().unwrap();
        assert_eq!(fg.current_quantity, 47.0);

        // Debt record for full amount: 3000 cents ($30)
        let debt: (i64,) = sqlx::query_as(
            "SELECT remaining_amount FROM debt_records WHERE sale_id = ?",
        )
        .bind(sale.id.to_string())
        .fetch_one(&pool)
        .await
        .unwrap();
        assert_eq!(debt.0, 3000);
    }

    // ── Insufficient stock rejected (Req 6.4) ─────────────────────────

    #[tokio::test]
    async fn insufficient_stock_rejects_sale() {
        let (pool, svc) = setup().await;
        let f = seed_fixtures(&pool).await;

        let items = vec![SaleLineItem {
            finished_good_id: f.fg2_id,
            finished_good_name: "Chocolate Bar".into(),
            quantity: 999, // only 50 in stock
            unit_price: Money::from_f64(10.0),
        }];

        let err = svc.create_sale(f.customer_id, items, Money::from_f64(9990.0), f.wallet_id).await.unwrap_err();
        match err {
            AppError::InsufficientStock { material_name, available, requested } => {
                assert_eq!(material_name, "Chocolate Bar");
                assert_eq!(available, 50);
                assert_eq!(requested, 999);
            }
            other => panic!("Expected InsufficientStock, got: {other:?}"),
        }

        // Inventory unchanged
        let fg = fg_queries::get_by_id(&pool, &f.fg2_id.to_string()).await.unwrap().unwrap();
        assert_eq!(fg.current_quantity, 50.0);

        // Wallet unchanged
        let wallet = wallet_queries::get_by_id(&pool, &f.wallet_id.to_string()).await.unwrap().unwrap();
        assert_eq!(wallet.current_balance, 50000);
    }

    // ── Sales history ──────────────────────────────────────────────────

    #[tokio::test]
    async fn get_sales_history_returns_created_sales() {
        let (pool, svc) = setup().await;
        let f = seed_fixtures(&pool).await;

        let items = vec![SaleLineItem {
            finished_good_id: f.fg1_id,
            finished_good_name: "Sweet Box".into(),
            quantity: 1,
            unit_price: Money::from_f64(25.0),
        }];

        svc.create_sale(f.customer_id, items, Money::from_f64(25.0), f.wallet_id).await.unwrap();

        let history = svc.get_sales_history(None).await.unwrap();
        assert_eq!(history.len(), 1);
        assert_eq!(history[0].customer_name, "Ahmad");
        assert_eq!(history[0].line_items.len(), 1);
        assert_eq!(history[0].total_amount, Money::from_f64(25.0));
    }

    // ── Sale input validation (Req 5) ──────────────────────────────────

    #[tokio::test]
    async fn rejects_empty_line_items() {
        let (_pool, svc) = setup().await;
        let err = svc
            .create_sale(Uuid::new_v4(), vec![], Money::ZERO, Uuid::new_v4())
            .await
            .unwrap_err();
        match err {
            AppError::Validation { field, message } => {
                assert_eq!(field, "line_items");
                assert_eq!(message, "At least one line item required");
            }
            other => panic!("Expected Validation, got: {other:?}"),
        }
    }

    #[tokio::test]
    async fn rejects_zero_quantity_line_item() {
        let (_pool, svc) = setup().await;
        let items = vec![SaleLineItem {
            finished_good_id: Uuid::new_v4(),
            finished_good_name: "Test".into(),
            quantity: 0,
            unit_price: Money(1000),
        }];
        let err = svc
            .create_sale(Uuid::new_v4(), items, Money::ZERO, Uuid::new_v4())
            .await
            .unwrap_err();
        match err {
            AppError::Validation { field, message } => {
                assert_eq!(field, "quantity");
                assert_eq!(message, "Quantity must be positive");
            }
            other => panic!("Expected Validation, got: {other:?}"),
        }
    }

    #[tokio::test]
    async fn rejects_negative_quantity_line_item() {
        let (_pool, svc) = setup().await;
        let items = vec![SaleLineItem {
            finished_good_id: Uuid::new_v4(),
            finished_good_name: "Test".into(),
            quantity: -3,
            unit_price: Money(1000),
        }];
        let err = svc
            .create_sale(Uuid::new_v4(), items, Money::ZERO, Uuid::new_v4())
            .await
            .unwrap_err();
        match err {
            AppError::Validation { field, .. } => assert_eq!(field, "quantity"),
            other => panic!("Expected Validation, got: {other:?}"),
        }
    }

    #[tokio::test]
    async fn rejects_negative_amount_paid() {
        let (pool, svc) = setup().await;
        let f = seed_fixtures(&pool).await;
        let items = vec![SaleLineItem {
            finished_good_id: f.fg1_id,
            finished_good_name: "Sweet Box".into(),
            quantity: 1,
            unit_price: Money(2500),
        }];
        let err = svc
            .create_sale(f.customer_id, items, Money(-100), f.wallet_id)
            .await
            .unwrap_err();
        match err {
            AppError::Validation { field, message } => {
                assert_eq!(field, "amount_paid");
                assert_eq!(message, "Cannot be negative");
            }
            other => panic!("Expected Validation, got: {other:?}"),
        }
    }

    #[tokio::test]
    async fn rejects_amount_paid_exceeding_total() {
        let (pool, svc) = setup().await;
        let f = seed_fixtures(&pool).await;
        let items = vec![SaleLineItem {
            finished_good_id: f.fg1_id,
            finished_good_name: "Sweet Box".into(),
            quantity: 1,
            unit_price: Money(2500), // total = 2500
        }];
        let err = svc
            .create_sale(f.customer_id, items, Money(3000), f.wallet_id)
            .await
            .unwrap_err();
        match err {
            AppError::Validation { field, message } => {
                assert_eq!(field, "amount_paid");
                assert_eq!(message, "Cannot exceed total");
            }
            other => panic!("Expected Validation, got: {other:?}"),
        }
    }

    #[tokio::test]
    async fn rejects_unit_price_mismatch() {
        let (pool, svc) = setup().await;
        let f = seed_fixtures(&pool).await;
        // fg1 has unit_price = 2500 in DB, but we pass 9999
        let items = vec![SaleLineItem {
            finished_good_id: f.fg1_id,
            finished_good_name: "Sweet Box".into(),
            quantity: 1,
            unit_price: Money(9999),
        }];
        let err = svc
            .create_sale(f.customer_id, items, Money(9999), f.wallet_id)
            .await
            .unwrap_err();
        match err {
            AppError::Validation { field, message } => {
                assert_eq!(field, "unit_price");
                assert_eq!(message, "Price mismatch");
            }
            other => panic!("Expected Validation, got: {other:?}"),
        }
    }

    #[tokio::test]
    async fn rejects_nonexistent_finished_good_in_line_item() {
        let (pool, svc) = setup().await;
        let f = seed_fixtures(&pool).await;
        let fake_fg_id = Uuid::new_v4();
        let items = vec![SaleLineItem {
            finished_good_id: fake_fg_id,
            finished_good_name: "Ghost Item".into(),
            quantity: 1,
            unit_price: Money(1000),
        }];
        let err = svc
            .create_sale(f.customer_id, items, Money(1000), f.wallet_id)
            .await
            .unwrap_err();
        match err {
            AppError::NotFound { entity_type, entity_id } => {
                assert_eq!(entity_type, "finished_good");
                assert_eq!(entity_id, fake_fg_id.to_string());
            }
            other => panic!("Expected NotFound, got: {other:?}"),
        }
    }
}
