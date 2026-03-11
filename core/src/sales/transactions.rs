use chrono::{DateTime, Utc};
use sqlx::SqlitePool;
use uuid::Uuid;

use crate::error::{AppError, AppResult};
use crate::models::domain::{Sale, SaleLineItem};
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
        amount_paid: f64,
        wallet_id: Uuid,
    ) -> AppResult<Sale> {
        // Calculate total = sum(qty × unit_price) (Req 8.4)
        let total_amount: f64 = line_items
            .iter()
            .map(|item| item.quantity as f64 * item.unit_price)
            .sum();

        let now = Utc::now();
        let now_str = now.to_rfc3339();
        let sale_id = Uuid::new_v4();

        let mut tx = self.pool.begin().await?;

        // Validate customer exists
        let customer = sale_queries::get_customer_detail(&mut *tx, &customer_id.to_string())
            .await?
            .ok_or_else(|| AppError::Validation {
                field: "customer_id".to_string(),
                message: format!("Customer {customer_id} not found"),
            })?;

        // For each line item: check finished good stock, then deduct (Req 8.2, 6.4)
        for item in &line_items {
            let fg = sale_queries::get_finished_good_in_tx(
                &mut *tx,
                &item.finished_good_id.to_string(),
            )
            .await?
            .ok_or_else(|| AppError::Validation {
                field: "finished_good_id".to_string(),
                message: format!("Finished good {} not found", item.finished_good_id),
            })?;

            let required = item.quantity as f64;
            if fg.current_quantity < required {
                return Err(AppError::InsufficientStock {
                    material_name: fg.name,
                    available: fg.current_quantity,
                    requested: required,
                });
            }

            sale_queries::deduct_finished_good_in_tx(
                &mut *tx,
                &item.finished_good_id.to_string(),
                required,
                &now_str,
            )
            .await?;
        }

        // If amount_paid > 0: credit wallet (Req 8.2)
        if amount_paid > 0.0 {
            let wallet = wallet_queries::get_by_id_in_tx(&mut *tx, &wallet_id.to_string())
                .await?
                .ok_or_else(|| AppError::Validation {
                    field: "wallet_id".to_string(),
                    message: format!("Wallet {wallet_id} not found"),
                })?;

            let new_balance = wallet.current_balance + amount_paid;
            wallet_queries::update_balance_in_tx(
                &mut *tx,
                &wallet_id.to_string(),
                new_balance,
                &now_str,
            )
            .await?;

            // Log wallet transaction
            wallet_queries::insert_transaction_in_tx(
                &mut *tx,
                &Uuid::new_v4().to_string(),
                &wallet_id.to_string(),
                amount_paid,
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
            total_amount,
            amount_paid,
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
                item.unit_price,
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
                debt_amount,
                debt_amount,
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
    pub async fn get_sales_history(&self) -> AppResult<Vec<Sale>> {
        let sale_rows = sale_queries::list_all_with_customers(&self.pool).await?;
        let mut sales = Vec::with_capacity(sale_rows.len());

        for row in sale_rows {
            let line_item_rows = sale_queries::get_line_items(&self.pool, &row.id).await?;
            let line_items: Vec<SaleLineItem> = line_item_rows
                .into_iter()
                .map(|li| -> AppResult<SaleLineItem> {
                    Ok(SaleLineItem {
                        finished_good_id: Uuid::parse_str(&li.finished_good_id)
                            .map_err(|e| AppError::Unknown(format!("Invalid UUID: {e}")))?,
                        finished_good_name: li.finished_good_name,
                        quantity: li.quantity,
                        unit_price: li.unit_price,
                    })
                })
                .collect::<AppResult<Vec<_>>>()?;

            let timestamp: DateTime<Utc> = row
                .timestamp
                .parse()
                .map_err(|e| AppError::Unknown(format!("Invalid timestamp: {e}")))?;

            sales.push(Sale {
                id: Uuid::parse_str(&row.id)
                    .map_err(|e| AppError::Unknown(format!("Invalid UUID: {e}")))?,
                customer_id: Uuid::parse_str(&row.customer_id)
                    .map_err(|e| AppError::Unknown(format!("Invalid UUID: {e}")))?,
                customer_name: row.customer_name,
                line_items,
                total_amount: row.total_amount,
                amount_paid: row.amount_paid,
                payment_wallet_id: Uuid::parse_str(&row.payment_wallet_id)
                    .map_err(|e| AppError::Unknown(format!("Invalid UUID: {e}")))?,
                timestamp,
            });
        }

        Ok(sales)
    }
}


#[cfg(test)]
mod tests {
    use super::*;
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
        wallet_queries::insert_wallet(pool, &wallet_id.to_string(), "Cash Box", "Cash", 500.0, &now)
            .await
            .unwrap();
        fg_queries::insert(pool, &fg1_id.to_string(), "Sweet Box", 100.0, 25.0, &now)
            .await
            .unwrap();
        fg_queries::insert(pool, &fg2_id.to_string(), "Chocolate Bar", 50.0, 10.0, &now)
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
                unit_price: 25.0,
            },
            SaleLineItem {
                finished_good_id: f.fg2_id,
                finished_good_name: "Chocolate Bar".into(),
                quantity: 5,
                unit_price: 10.0,
            },
        ];

        // total = 3*25 + 5*10 = 75 + 50 = 125
        let sale = svc.create_sale(f.customer_id, items, 125.0, f.wallet_id).await.unwrap();
        assert_eq!(sale.total_amount, 125.0);
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
            unit_price: 25.0,
        }];

        let sale = svc.create_sale(f.customer_id, items, 50.0, f.wallet_id).await.unwrap();
        assert_eq!(sale.total_amount, 50.0);
        assert_eq!(sale.amount_paid, 50.0);

        // Wallet credited: 500 + 50 = 550
        let wallet = wallet_queries::get_by_id(&pool, &f.wallet_id.to_string()).await.unwrap().unwrap();
        assert_eq!(wallet.current_balance, 550.0);

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
            unit_price: 25.0,
        }];

        // total = 100, paid = 60 → debt = 40
        let sale = svc.create_sale(f.customer_id, items, 60.0, f.wallet_id).await.unwrap();
        assert_eq!(sale.total_amount, 100.0);
        assert_eq!(sale.amount_paid, 60.0);

        // Wallet credited: 500 + 60 = 560
        let wallet = wallet_queries::get_by_id(&pool, &f.wallet_id.to_string()).await.unwrap().unwrap();
        assert_eq!(wallet.current_balance, 560.0);

        // Inventory deducted: 100 - 4 = 96
        let fg = fg_queries::get_by_id(&pool, &f.fg1_id.to_string()).await.unwrap().unwrap();
        assert_eq!(fg.current_quantity, 96.0);

        // Debt record created for 40
        let debt: (f64,) = sqlx::query_as(
            "SELECT remaining_amount FROM debt_records WHERE sale_id = ?",
        )
        .bind(sale.id.to_string())
        .fetch_one(&pool)
        .await
        .unwrap();
        assert_eq!(debt.0, 40.0);
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
            unit_price: 10.0,
        }];

        // total = 30, paid = 0 → debt = 30
        let sale = svc.create_sale(f.customer_id, items, 0.0, f.wallet_id).await.unwrap();
        assert_eq!(sale.total_amount, 30.0);
        assert_eq!(sale.amount_paid, 0.0);

        // Wallet unchanged: still 500
        let wallet = wallet_queries::get_by_id(&pool, &f.wallet_id.to_string()).await.unwrap().unwrap();
        assert_eq!(wallet.current_balance, 500.0);

        // Inventory deducted: 50 - 3 = 47
        let fg = fg_queries::get_by_id(&pool, &f.fg2_id.to_string()).await.unwrap().unwrap();
        assert_eq!(fg.current_quantity, 47.0);

        // Debt record for full amount
        let debt: (f64,) = sqlx::query_as(
            "SELECT remaining_amount FROM debt_records WHERE sale_id = ?",
        )
        .bind(sale.id.to_string())
        .fetch_one(&pool)
        .await
        .unwrap();
        assert_eq!(debt.0, 30.0);
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
            unit_price: 10.0,
        }];

        let err = svc.create_sale(f.customer_id, items, 9990.0, f.wallet_id).await.unwrap_err();
        match err {
            AppError::InsufficientStock { material_name, available, requested } => {
                assert_eq!(material_name, "Chocolate Bar");
                assert_eq!(available, 50.0);
                assert_eq!(requested, 999.0);
            }
            other => panic!("Expected InsufficientStock, got: {other:?}"),
        }

        // Inventory unchanged
        let fg = fg_queries::get_by_id(&pool, &f.fg2_id.to_string()).await.unwrap().unwrap();
        assert_eq!(fg.current_quantity, 50.0);

        // Wallet unchanged
        let wallet = wallet_queries::get_by_id(&pool, &f.wallet_id.to_string()).await.unwrap().unwrap();
        assert_eq!(wallet.current_balance, 500.0);
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
            unit_price: 25.0,
        }];

        svc.create_sale(f.customer_id, items, 25.0, f.wallet_id).await.unwrap();

        let history = svc.get_sales_history().await.unwrap();
        assert_eq!(history.len(), 1);
        assert_eq!(history[0].customer_name, "Ahmad");
        assert_eq!(history[0].line_items.len(), 1);
        assert_eq!(history[0].total_amount, 25.0);
    }
}
