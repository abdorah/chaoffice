use chrono::{DateTime, Utc};
use sqlx::SqlitePool;
use uuid::Uuid;

use crate::error::{AppError, AppResult};
use crate::models::domain::{
    FinancialSummary, Invoice, SaleLineItem, Wallet, WalletType,
};
use crate::persistence::queries::expenses as expense_queries;
use crate::persistence::queries::sales as sale_queries;
use crate::persistence::queries::wallets as wallet_queries;

/// Aggregate financial data for the given date range (Req 12.1).
///
/// - total_revenue = sum of sales.total_amount within range
/// - total_expenses = sum of expenses.amount within range
/// - net_profit = total_revenue - total_expenses
/// - wallet_balances = current snapshot of all wallets
pub async fn get_financial_summary(
    pool: &SqlitePool,
    start_date: DateTime<Utc>,
    end_date: DateTime<Utc>,
) -> AppResult<FinancialSummary> {
    let start_str = start_date.to_rfc3339();
    let end_str = end_date.to_rfc3339();

    // Sum sales revenue in date range
    let sale_rows = sale_queries::list_by_date_range(pool, &start_str, &end_str).await?;
    let total_revenue: f64 = sale_rows.iter().map(|r| r.total_amount).sum();

    // Sum expenses in date range
    let expense_rows = expense_queries::get_by_date_range(pool, &start_str, &end_str).await?;
    let total_expenses: f64 = expense_rows.iter().map(|r| r.amount).sum();

    let net_profit = total_revenue - total_expenses;

    // Current wallet balances
    let wallet_rows = wallet_queries::list_all(pool).await?;
    let wallet_balances: Vec<Wallet> = wallet_rows
        .into_iter()
        .map(|w| -> AppResult<Wallet> {
            Ok(Wallet {
                id: Uuid::parse_str(&w.id)
                    .map_err(|e| AppError::Unknown(format!("Invalid UUID: {e}")))?,
                name: w.name,
                wallet_type: parse_wallet_type(&w.wallet_type)?,
                current_balance: w.current_balance,
            })
        })
        .collect::<AppResult<Vec<_>>>()?;

    Ok(FinancialSummary {
        total_revenue,
        total_expenses,
        net_profit,
        wallet_balances,
        start_date,
        end_date,
    })
}

/// Build an Invoice from a persisted sale (Req 12.3).
pub async fn generate_invoice(pool: &SqlitePool, sale_id: Uuid) -> AppResult<Invoice> {
    let sale_row = sale_queries::get_sale_with_customer(pool, &sale_id.to_string())
        .await?
        .ok_or_else(|| AppError::Validation {
            field: "sale_id".to_string(),
            message: format!("Sale {sale_id} not found"),
        })?;

    let line_item_rows = sale_queries::get_line_items(pool, &sale_id.to_string()).await?;
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

    let customer = sale_queries::get_customer_detail(pool, &sale_row.customer_id)
        .await?
        .ok_or_else(|| AppError::Unknown("Customer not found for sale".to_string()))?;

    let timestamp: DateTime<Utc> = sale_row
        .timestamp
        .parse()
        .map_err(|e| AppError::Unknown(format!("Invalid timestamp: {e}")))?;

    let remaining_balance = sale_row.total_amount - sale_row.amount_paid;
    let date = timestamp.format("%Y-%m-%d").to_string();
    // Use first 8 chars of sale UUID as invoice number prefix
    let invoice_number = format!("INV-{}", &sale_row.id[..8.min(sale_row.id.len())]);

    Ok(Invoice {
        business_name: "Sweet Lab".to_string(),
        customer_name: customer.name,
        customer_city: customer.city,
        customer_mobile: customer.mobile,
        line_items,
        total_amount: sale_row.total_amount,
        amount_paid: sale_row.amount_paid,
        remaining_balance,
        date,
        invoice_number,
    })
}

fn parse_wallet_type(s: &str) -> AppResult<WalletType> {
    match s {
        "Bank" => Ok(WalletType::Bank),
        "Cash" => Ok(WalletType::Cash),
        "Representative" => Ok(WalletType::Representative),
        other => Err(AppError::Unknown(format!("Invalid wallet type: {other}"))),
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::models::domain::ExpenseCategory;
    use crate::persistence::db;
    use crate::persistence::queries::customers as customer_queries;
    use crate::persistence::queries::finished_goods as fg_queries;
    use crate::persistence::queries::wallets as wallet_queries;
    use crate::expenses::service::ExpenseServiceImpl;
    use crate::sales::transactions::SalesServiceImpl;
    use chrono::Duration;

    async fn setup() -> SqlitePool {
        db::init_db(":memory:").await.expect("DB init failed")
    }

    async fn seed_base(pool: &SqlitePool) -> (Uuid, Uuid, Uuid) {
        let now = Utc::now().to_rfc3339();
        let cust_id = Uuid::new_v4();
        let wallet_id = Uuid::new_v4();
        let fg_id = Uuid::new_v4();

        customer_queries::insert(pool, &cust_id.to_string(), "Ahmad", "Damascus", "+963911111111", &now)
            .await.unwrap();
        wallet_queries::insert_wallet(pool, &wallet_id.to_string(), "Cash Box", "Cash", 1000.0, &now)
            .await.unwrap();
        fg_queries::insert(pool, &fg_id.to_string(), "Sweet Box", 100.0, 25.0, &now)
            .await.unwrap();

        // Seed a user for expenses
        sqlx::query(
            "INSERT INTO users (id, username, full_name, role, password_hash, sync_status, updated_at, created_at)
             VALUES (?, 'admin', 'Admin', 'Admin', 'hash', 'Synced', ?, ?)",
        )
        .bind(&cust_id.to_string()) // reuse as user id for simplicity
        .bind(&now)
        .bind(&now)
        .execute(pool)
        .await
        .unwrap();

        (cust_id, wallet_id, fg_id)
    }

    #[tokio::test]
    async fn financial_summary_calculates_revenue_expenses_profit() {
        let pool = setup().await;
        let (cust_id, wallet_id, fg_id) = seed_base(&pool).await;

        let sales_svc = SalesServiceImpl::new(pool.clone());
        let expense_svc = ExpenseServiceImpl::new(pool.clone());

        // Create a sale: 2 × 25 = 50 revenue
        let items = vec![SaleLineItem {
            finished_good_id: fg_id,
            finished_good_name: "Sweet Box".into(),
            quantity: 2,
            unit_price: 25.0,
        }];
        sales_svc.create_sale(cust_id, items, 50.0, wallet_id).await.unwrap();

        // Record an expense: 30
        expense_svc
            .record_expense("Supplies", 30.0, ExpenseCategory::Purchase, wallet_id, cust_id)
            .await.unwrap();

        let start = Utc::now() - Duration::hours(1);
        let end = Utc::now() + Duration::hours(1);
        let summary = get_financial_summary(&pool, start, end).await.unwrap();

        assert_eq!(summary.total_revenue, 50.0);
        assert_eq!(summary.total_expenses, 30.0);
        assert_eq!(summary.net_profit, 20.0);
        assert!(!summary.wallet_balances.is_empty());
    }

    #[tokio::test]
    async fn financial_summary_empty_range_returns_zeros() {
        let pool = setup().await;

        let start = Utc::now() - Duration::days(10);
        let end = Utc::now() - Duration::days(5);
        let summary = get_financial_summary(&pool, start, end).await.unwrap();

        assert_eq!(summary.total_revenue, 0.0);
        assert_eq!(summary.total_expenses, 0.0);
        assert_eq!(summary.net_profit, 0.0);
    }

    #[tokio::test]
    async fn generate_invoice_contains_all_fields() {
        let pool = setup().await;
        let (cust_id, wallet_id, fg_id) = seed_base(&pool).await;

        let sales_svc = SalesServiceImpl::new(pool.clone());
        let items = vec![SaleLineItem {
            finished_good_id: fg_id,
            finished_good_name: "Sweet Box".into(),
            quantity: 3,
            unit_price: 25.0,
        }];
        // total = 75, paid = 50 → remaining = 25
        let sale = sales_svc.create_sale(cust_id, items, 50.0, wallet_id).await.unwrap();

        let invoice = generate_invoice(&pool, sale.id).await.unwrap();

        assert_eq!(invoice.business_name, "Sweet Lab");
        assert_eq!(invoice.customer_name, "Ahmad");
        assert_eq!(invoice.customer_city, "Damascus");
        assert_eq!(invoice.customer_mobile, "+963911111111");
        assert_eq!(invoice.line_items.len(), 1);
        assert_eq!(invoice.total_amount, 75.0);
        assert_eq!(invoice.amount_paid, 50.0);
        assert_eq!(invoice.remaining_balance, 25.0);
        assert!(!invoice.date.is_empty());
        assert!(invoice.invoice_number.starts_with("INV-"));
    }

    #[tokio::test]
    async fn generate_invoice_nonexistent_sale_fails() {
        let pool = setup().await;
        let err = generate_invoice(&pool, Uuid::new_v4()).await.unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }
}
