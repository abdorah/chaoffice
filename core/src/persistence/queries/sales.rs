use sqlx::Executor;

use crate::error::{AppError, AppResult};

/// Row type matching the `sales` table schema.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct SaleRow {
    pub id: String,
    pub customer_id: String,
    pub total_amount: f64,
    pub amount_paid: f64,
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
    pub unit_price: f64,
}

/// Row for sale joined with customer name.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct SaleWithCustomerRow {
    pub id: String,
    pub customer_id: String,
    pub customer_name: String,
    pub total_amount: f64,
    pub amount_paid: f64,
    pub payment_wallet_id: String,
    pub timestamp: String,
}

/// Insert a sale record within a transaction.
pub async fn insert_sale_in_tx<'e, E>(
    executor: E,
    id: &str,
    customer_id: &str,
    total_amount: f64,
    amount_paid: f64,
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
    unit_price: f64,
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
    original_amount: f64,
    remaining_amount: f64,
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
    pub unit_price: f64,
}

/// Deduct finished good quantity within a transaction.
pub async fn deduct_finished_good_in_tx<'e, E>(
    executor: E,
    id: &str,
    amount: f64,
    now: &str,
) -> AppResult<()>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    let result = sqlx::query(
        "UPDATE finished_goods SET current_quantity = current_quantity - ?, last_updated = ?, updated_at = ? WHERE id = ?",
    )
    .bind(amount)
    .bind(now)
    .bind(now)
    .bind(id)
    .execute(executor)
    .await?;

    if result.rows_affected() == 0 {
        return Err(AppError::Validation {
            field: "finished_good_id".to_string(),
            message: format!("Finished good {id} not found"),
        });
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

/// Fetch all sales with customer names, ordered by timestamp descending.
pub async fn list_all_with_customers<'e, E>(
    executor: E,
) -> AppResult<Vec<SaleWithCustomerRow>>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    let rows = sqlx::query_as::<_, SaleWithCustomerRow>(
        "SELECT s.id, s.customer_id, c.name AS customer_name, s.total_amount, s.amount_paid, s.payment_wallet_id, s.timestamp
         FROM sales s
         JOIN customers c ON s.customer_id = c.id
         ORDER BY s.timestamp DESC",
    )
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
