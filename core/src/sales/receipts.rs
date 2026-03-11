use chrono::{DateTime, Utc};
use sqlx::SqlitePool;
use uuid::Uuid;

use crate::error::{AppError, AppResult};
use crate::models::domain::{Receipt, Sale, SaleLineItem};
use crate::persistence::queries::sales as sale_queries;

/// Generate a receipt for a given sale (Req 8.5).
///
/// Fetches the sale with line items and customer details, then assembles
/// a Receipt with all required fields: customer name, itemized list, total,
/// amount paid, remaining balance, and date.
pub async fn generate_receipt(pool: &SqlitePool, sale_id: Uuid) -> AppResult<Receipt> {
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

    let sale = Sale {
        id: Uuid::parse_str(&sale_row.id)
            .map_err(|e| AppError::Unknown(format!("Invalid UUID: {e}")))?,
        customer_id: Uuid::parse_str(&sale_row.customer_id)
            .map_err(|e| AppError::Unknown(format!("Invalid UUID: {e}")))?,
        customer_name: sale_row.customer_name,
        line_items,
        total_amount: sale_row.total_amount,
        amount_paid: sale_row.amount_paid,
        payment_wallet_id: Uuid::parse_str(&sale_row.payment_wallet_id)
            .map_err(|e| AppError::Unknown(format!("Invalid UUID: {e}")))?,
        timestamp,
    };

    let remaining_balance = sale.total_amount - sale.amount_paid;
    let formatted_date = timestamp.format("%Y-%m-%d %H:%M:%S").to_string();

    Ok(Receipt {
        customer_name: customer.name,
        customer_city: customer.city,
        customer_mobile: customer.mobile,
        business_name: "Sweet Lab".to_string(),
        remaining_balance,
        formatted_date,
        sale,
    })
}


#[cfg(test)]
mod tests {
    use super::*;
    use crate::persistence::db;
    use crate::persistence::queries::customers as customer_queries;
    use crate::persistence::queries::finished_goods as fg_queries;
    use crate::persistence::queries::wallets as wallet_queries;
    use crate::sales::transactions::SalesServiceImpl;

    async fn setup() -> (SqlitePool, SalesServiceImpl) {
        let pool = db::init_db(":memory:").await.expect("DB init failed");
        let svc = SalesServiceImpl::new(pool.clone());
        (pool, svc)
    }

    async fn seed(pool: &SqlitePool) -> (Uuid, Uuid, Uuid) {
        let now = Utc::now().to_rfc3339();
        let cust_id = Uuid::new_v4();
        let wallet_id = Uuid::new_v4();
        let fg_id = Uuid::new_v4();

        customer_queries::insert(pool, &cust_id.to_string(), "Khaled", "Aleppo", "+963922222222", &now)
            .await
            .unwrap();
        wallet_queries::insert_wallet(pool, &wallet_id.to_string(), "Cash", "Cash", 1000.0, &now)
            .await
            .unwrap();
        fg_queries::insert(pool, &fg_id.to_string(), "Sweet Box", 50.0, 20.0, &now)
            .await
            .unwrap();

        (cust_id, wallet_id, fg_id)
    }

    #[tokio::test]
    async fn receipt_contains_all_required_fields() {
        let (pool, svc) = setup().await;
        let (cust_id, wallet_id, fg_id) = seed(&pool).await;

        let items = vec![SaleLineItem {
            finished_good_id: fg_id,
            finished_good_name: "Sweet Box".into(),
            quantity: 3,
            unit_price: 20.0,
        }];

        // total = 60, paid = 40 → remaining = 20
        let sale = svc.create_sale(cust_id, items, 40.0, wallet_id).await.unwrap();

        let receipt = generate_receipt(&pool, sale.id).await.unwrap();

        // Customer details (Req 8.5)
        assert_eq!(receipt.customer_name, "Khaled");
        assert_eq!(receipt.customer_city, "Aleppo");
        assert_eq!(receipt.customer_mobile, "+963922222222");

        // Business name
        assert_eq!(receipt.business_name, "Sweet Lab");

        // Itemized list
        assert_eq!(receipt.sale.line_items.len(), 1);
        assert_eq!(receipt.sale.line_items[0].finished_good_name, "Sweet Box");
        assert_eq!(receipt.sale.line_items[0].quantity, 3);
        assert_eq!(receipt.sale.line_items[0].unit_price, 20.0);

        // Totals
        assert_eq!(receipt.sale.total_amount, 60.0);
        assert_eq!(receipt.sale.amount_paid, 40.0);
        assert_eq!(receipt.remaining_balance, 20.0);

        // Date is formatted
        assert!(!receipt.formatted_date.is_empty());
    }

    #[tokio::test]
    async fn receipt_for_nonexistent_sale_fails() {
        let (pool, _svc) = setup().await;
        let err = generate_receipt(&pool, Uuid::new_v4()).await.unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }

    #[tokio::test]
    async fn receipt_full_payment_has_zero_remaining() {
        let (pool, svc) = setup().await;
        let (cust_id, wallet_id, fg_id) = seed(&pool).await;

        let items = vec![SaleLineItem {
            finished_good_id: fg_id,
            finished_good_name: "Sweet Box".into(),
            quantity: 2,
            unit_price: 20.0,
        }];

        let sale = svc.create_sale(cust_id, items, 40.0, wallet_id).await.unwrap();
        let receipt = generate_receipt(&pool, sale.id).await.unwrap();

        assert_eq!(receipt.remaining_balance, 0.0);
    }
}
