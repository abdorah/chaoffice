//! Shared helpers for sale-related operations (invoice, receipt generation).
//!
//! Consolidates duplicate sale-fetching and line-item-building logic
//! previously duplicated across `reports/financial.rs` and `sales/receipts.rs`.
//! (Req 17.3)

use chrono::{DateTime, Utc};
use sqlx::SqlitePool;
use uuid::Uuid;

use crate::error::{AppError, AppResult};
use crate::models::domain::SaleLineItem;
use crate::models::Money;
use crate::persistence::queries::sales as sale_queries;

/// Fetched sale data with parsed line items, customer info, and timestamp.
pub struct FetchedSaleData {
    pub sale_row: sale_queries::SaleWithCustomerRow,
    pub line_items: Vec<SaleLineItem>,
    pub customer: sale_queries::CustomerDetailRow,
    pub timestamp: DateTime<Utc>,
}

/// Fetch a sale by ID with its line items, customer details, and parsed timestamp.
///
/// This consolidates the common pattern used by both `generate_invoice` and
/// `generate_receipt` (Req 17.3).
pub async fn fetch_sale_with_details(
    pool: &SqlitePool,
    sale_id: Uuid,
) -> AppResult<FetchedSaleData> {
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
                finished_good_id: crate::utils::parse_uuid("finished_good", &li.finished_good_id)?,
                finished_good_name: li.finished_good_name,
                quantity: li.quantity,
                unit_price: Money(li.unit_price),
            })
        })
        .collect::<AppResult<Vec<_>>>()?;

    let customer = sale_queries::get_customer_detail(pool, &sale_row.customer_id)
        .await?
        .ok_or_else(|| AppError::Unknown("Customer not found for sale".to_string()))?;

    let timestamp = crate::utils::parse_timestamp(&sale_row.timestamp)?;

    Ok(FetchedSaleData {
        sale_row,
        line_items,
        customer,
        timestamp,
    })
}
