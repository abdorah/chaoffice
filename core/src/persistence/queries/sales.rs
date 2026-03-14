use sqlx::Executor;

use crate::error::{AppError, AppResult};
use crate::models::domain::Pagination;

/// Row type matching the `sales` table schema.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct SaleRow {
    pub id: String,
    pub customer_id: String,
    pub total_amount: i64,
    pub amount_paid: i64,
    pub payment_wallet_id: String,
    pub timestamp: String,
    pub sync_status: String,
    pub updated_at: String,
}

/// Row type matching the `sale_line_items` table schema.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct SaleLineItemRow {
    pub id: String,
    pub sale_id: String,
    pub finished_good_id: String,
    pub finished_good_name: String,
    pub quantity: i32,
    pub unit_price: i64,
}

/// Row for sale joined with customer name.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct SaleWithCustomerRow {
    pub id: String,
    pub customer_id: String,
    pub customer_name: String,
    pub total_amount: i64,
    pub amount_paid: i64,
    pub payment_wallet_id: String,
    pub timestamp: String,
}

/// Insert a sale record within a transaction.
pub async fn insert_sale_in_tx<'e, E>(
    executor: E,
    id: &str,
    customer_id: &str,
    total_amount: i64,
    amount_paid: i64,
    payment_wallet_id: &str,
    timestamp: &str,
    now: &str,
) -> AppResult<()>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    sqlx::query(
        "INSERT INTO sales (id, customer_id, total_amount, amount_paid, payment_wallet_id, timestamp, sync_status, updated_at)
         VALUES (?, ?, ?, ?, ?, ?, 'Synced', ?)",
    )
    .bind(id)
    .bind(customer_id)
    .bind(total_amount)
    .bind(amount_paid)
    .bind(payment_wallet_id)
    .bind(timestamp)
    .bind(now)
    .execute(executor)
    .await?;

    Ok(())
}

/// Insert a sale line item within a transaction.
pub async fn insert_line_item_in_tx<'e, E>(
    executor: E,
    id: &str,
    sale_id: &str,
    finished_good_id: &str,
    finished_good_name: &str,
    quantity: i32,
    unit_price: i64,
) -> AppResult<()>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    sqlx::query(
        "INSERT INTO sale_line_items (id, sale_id, finished_good_id, finished_good_name, quantity, unit_price)
         VALUES (?, ?, ?, ?, ?, ?)",
    )
    .bind(id)
    .bind(sale_id)
    .bind(finished_good_id)
    .bind(finished_good_name)
    .bind(quantity)
    .bind(unit_price)
    .execute(executor)
    .await?;

    Ok(())
}

/// Insert a debt record within a transaction.
pub async fn insert_debt_in_tx<'e, E>(
    executor: E,
    id: &str,
    customer_id: &str,
    sale_id: &str,
    original_amount: i64,
    remaining_amount: i64,
    sale_date: &str,
    now: &str,
) -> AppResult<()>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    sqlx::query(
        "INSERT INTO debt_records (id, customer_id, sale_id, original_amount, remaining_amount, sale_date, is_settled, sync_status, updated_at)
         VALUES (?, ?, ?, ?, ?, ?, 0, 'Synced', ?)",
    )
    .bind(id)
    .bind(customer_id)
    .bind(sale_id)
    .bind(original_amount)
    .bind(remaining_amount)
    .bind(sale_date)
    .bind(now)
    .execute(executor)
    .await?;

    Ok(())
}

/// Fetch a finished good by id within a transaction.
pub async fn get_finished_good_in_tx<'e, E>(
    executor: E,
    id: &str,
) -> AppResult<Option<FinishedGoodRow>>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    let row = sqlx::query_as::<_, FinishedGoodRow>(
        "SELECT id, name, current_quantity, unit_price FROM finished_goods WHERE id = ?",
    )
    .bind(id)
    .fetch_optional(executor)
    .await?;

    Ok(row)
}

/// Minimal finished good row for stock checks within transactions.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct FinishedGoodRow {
    pub id: String,
    pub name: String,
    pub current_quantity: f64,
    pub unit_price: i64,
}

/// Deduct finished good quantity atomically within a transaction (Req 4.3).
///
/// Uses a single UPDATE with WHERE clause checking `current_quantity >= amount`.
/// If rows_affected == 0, does a follow-up SELECT to distinguish NotFound vs InsufficientStock.
pub async fn deduct_finished_good_in_tx(
    tx: &mut sqlx::SqliteConnection,
    id: &str,
    amount: f64,
    now: &str,
) -> AppResult<()> {
    let result = sqlx::query(
        "UPDATE finished_goods SET current_quantity = current_quantity - ?, last_updated = ?, updated_at = ? WHERE id = ? AND current_quantity >= ?",
    )
    .bind(amount)
    .bind(now)
    .bind(now)
    .bind(id)
    .bind(amount)
    .execute(&mut *tx)
    .await?;

    if result.rows_affected() == 0 {
        // Follow-up SELECT to distinguish NotFound vs InsufficientStock (Req 4.2)
        let existing = get_finished_good_in_tx(&mut *tx, id).await?;
        return match existing {
            None => Err(AppError::NotFound {
                entity_type: "finished_good".to_string(),
                entity_id: id.to_string(),
            }),
            Some(fg) => Err(AppError::InsufficientStock {
                material_name: fg.name,
                available: fg.current_quantity as i64,
                requested: amount as i64,
            }),
        };
    }

    Ok(())
}


/// Fetch a sale by id with customer name (JOIN).
pub async fn get_sale_with_customer<'e, E>(
    executor: E,
    sale_id: &str,
) -> AppResult<Option<SaleWithCustomerRow>>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    let row = sqlx::query_as::<_, SaleWithCustomerRow>(
        "SELECT s.id, s.customer_id, c.name AS customer_name, s.total_amount, s.amount_paid, s.payment_wallet_id, s.timestamp
         FROM sales s
         JOIN customers c ON s.customer_id = c.id
         WHERE s.id = ?",
    )
    .bind(sale_id)
    .fetch_optional(executor)
    .await?;

    Ok(row)
}

/// Fetch all line items for a sale.
pub async fn get_line_items<'e, E>(
    executor: E,
    sale_id: &str,
) -> AppResult<Vec<SaleLineItemRow>>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    let rows = sqlx::query_as::<_, SaleLineItemRow>(
        "SELECT id, sale_id, finished_good_id, finished_good_name, quantity, unit_price
         FROM sale_line_items
         WHERE sale_id = ?",
    )
    .bind(sale_id)
    .fetch_all(executor)
    .await?;

    Ok(rows)
}

/// Fetch all line items for a batch of sale IDs in a single query (N+1 elimination, Req 12.1).
pub async fn get_line_items_batch<'e, E>(
    executor: E,
    sale_ids: &[String],
) -> AppResult<Vec<SaleLineItemRow>>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    if sale_ids.is_empty() {
        return Ok(Vec::new());
    }

    // Build a dynamic IN clause with placeholders
    let placeholders: Vec<&str> = sale_ids.iter().map(|_| "?").collect();
    let sql = format!(
        "SELECT id, sale_id, finished_good_id, finished_good_name, quantity, unit_price
         FROM sale_line_items
         WHERE sale_id IN ({})",
        placeholders.join(", ")
    );

    let mut query = sqlx::query_as::<_, SaleLineItemRow>(&sql);
    for id in sale_ids {
        query = query.bind(id);
    }

    let rows = query.fetch_all(executor).await?;
    Ok(rows)
}

/// Fetch all sales with customer names, ordered by timestamp descending.
pub async fn list_all_with_customers<'e, E>(
    executor: E,
    pagination: &Pagination,
) -> AppResult<Vec<SaleWithCustomerRow>>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    let sql = format!(
        "SELECT s.id, s.customer_id, c.name AS customer_name, s.total_amount, s.amount_paid, s.payment_wallet_id, s.timestamp
         FROM sales s
         JOIN customers c ON s.customer_id = c.id
         ORDER BY s.timestamp DESC
         LIMIT {} OFFSET {}",
        pagination.limit, pagination.offset
    );
    let rows = sqlx::query_as::<_, SaleWithCustomerRow>(&sql)
        .fetch_all(executor)
        .await?;

    Ok(rows)
}

/// Fetch customer details by id within a transaction.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct CustomerDetailRow {
    pub id: String,
    pub name: String,
    pub city: String,
    pub mobile: String,
}

pub async fn get_customer_detail<'e, E>(
    executor: E,
    customer_id: &str,
) -> AppResult<Option<CustomerDetailRow>>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    let row = sqlx::query_as::<_, CustomerDetailRow>(
        "SELECT id, name, city, mobile FROM customers WHERE id = ?",
    )
    .bind(customer_id)
    .fetch_optional(executor)
    .await?;

    Ok(row)
}

/// Fetch sales within a date range (for financial summary).
pub async fn list_by_date_range<'e, E>(
    executor: E,
    start_date: &str,
    end_date: &str,
) -> AppResult<Vec<SaleRow>>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    let rows = sqlx::query_as::<_, SaleRow>(
        "SELECT id, customer_id, total_amount, amount_paid, payment_wallet_id, timestamp, sync_status, updated_at
         FROM sales
         WHERE timestamp >= ? AND timestamp <= ?
         ORDER BY timestamp DESC",
    )
    .bind(start_date)
    .bind(end_date)
    .fetch_all(executor)
    .await?;

    Ok(rows)
}
