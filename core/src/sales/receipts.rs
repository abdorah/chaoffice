use sqlx::SqlitePool;
use uuid::Uuid;

use crate::error::AppResult;
use crate::models::domain::{Receipt, Sale};
use crate::models::Money;
use crate::sales::helpers::fetch_sale_with_details;

/// Generate a receipt for a given sale (Req 8.5).
///
/// Fetches the sale with line items and customer details, then assembles
/// a Receipt with all required fields: customer name, itemized list, total,
/// amount paid, remaining balance, and date.
pub async fn generate_receipt(pool: &SqlitePool, sale_id: Uuid, business_name: &str) -> AppResult<Receipt> {
    let data = fetch_sale_with_details(pool, sale_id).await?;

    let sale = Sale {
        id: crate::utils::parse_uuid("sale", &data.sale_row.id)?,
        customer_id: crate::utils::parse_uuid("customer", &data.sale_row.customer_id)?,
        customer_name: data.sale_row.customer_name,
        line_items: data.line_items,
        total_amount: Money(data.sale_row.total_amount),
        amount_paid: Money(data.sale_row.amount_paid),
        payment_wallet_id: crate::utils::parse_uuid("wallet", &data.sale_row.payment_wallet_id)?,
        timestamp: data.timestamp,
    };

    let remaining_balance = sale.total_amount - sale.amount_paid;
    let formatted_date = data.timestamp.format("%Y-%m-%d %H:%M:%S").to_string();

    Ok(Receipt {
        customer_name: data.customer.name,
        customer_city: data.customer.city,
        customer_mobile: data.customer.mobile,
        business_name: business_name.to_string(),
        remaining_balance,
        formatted_date,
        sale,
    })
}


#[cfg(test)]
mod tests {
    use super::*;
    use chrono::Utc;
    use uuid::Uuid;
    use crate::error::AppError;
    use crate::models::domain::SaleLineItem;
    use crate::models::Money;
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
        wallet_queries::insert_wallet(pool, &wallet_id.to_string(), "Cash", "Cash", 100000, &now)
            .await
            .unwrap();
        fg_queries::insert(pool, &fg_id.to_string(), "Sweet Box", 50.0, 2000, &now)
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
            unit_price: Money::from_f64(20.0),
        }];

        // total = 60, paid = 40 → remaining = 20
        let sale = svc.create_sale(cust_id, items, Money::from_f64(40.0), wallet_id).await.unwrap();

        let receipt = generate_receipt(&pool, sale.id, "Sweet Lab").await.unwrap();

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
        assert_eq!(receipt.sale.line_items[0].unit_price, Money::from_f64(20.0));

        // Totals
        assert_eq!(receipt.sale.total_amount, Money::from_f64(60.0));
        assert_eq!(receipt.sale.amount_paid, Money::from_f64(40.0));
        assert_eq!(receipt.remaining_balance, Money::from_f64(20.0));

        // Date is formatted
        assert!(!receipt.formatted_date.is_empty());
    }

    #[tokio::test]
    async fn receipt_for_nonexistent_sale_fails() {
        let (pool, _svc) = setup().await;
        let err = generate_receipt(&pool, Uuid::new_v4(), "Sweet Lab").await.unwrap_err();
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
            unit_price: Money::from_f64(20.0),
        }];

        let sale = svc.create_sale(cust_id, items, Money::from_f64(40.0), wallet_id).await.unwrap();
        let receipt = generate_receipt(&pool, sale.id, "Sweet Lab").await.unwrap();

        assert_eq!(receipt.remaining_balance, Money::ZERO);
    }
}
