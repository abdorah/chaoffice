use sqlx::{Executor, SqlitePool};

use crate::error::{AppError, AppResult};
use crate::models::domain::Pagination;

/// Row type matching the `debt_records` table schema.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct DebtRecordRow {
    pub id: String,
    pub customer_id: String,
    pub sale_id: String,
    pub original_amount: i64,
    pub remaining_amount: i64,
    pub sale_date: String,
    pub is_settled: bool,
    pub sync_status: String,
    pub updated_at: String,
}

/// Row type for debt records joined with customer name.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct DebtRecordWithCustomerRow {
    pub id: String,
    pub customer_id: String,
    pub customer_name: String,
    pub sale_id: String,
    pub original_amount: i64,
    pub remaining_amount: i64,
    pub sale_date: String,
    pub is_settled: bool,
}

/// Insert a new debt record.
pub async fn insert_debt<'e, E>(
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

/// Fetch active (unsettled) debts for a customer, ordered by sale_date ASC (oldest first for FIFO).
pub async fn get_active_debts_for_customer<'e, E>(
    executor: E,
    customer_id: &str,
) -> AppResult<Vec<DebtRecordWithCustomerRow>>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    let rows = sqlx::query_as::<_, DebtRecordWithCustomerRow>(
        "SELECT d.id, d.customer_id, c.name AS customer_name, d.sale_id,
                d.original_amount, d.remaining_amount, d.sale_date, d.is_settled
         FROM debt_records d
         JOIN customers c ON d.customer_id = c.id
         WHERE d.customer_id = ? AND d.is_settled = 0
         ORDER BY d.sale_date ASC",
    )
    .bind(customer_id)
    .fetch_all(executor)
    .await?;

    Ok(rows)
}

/// Update the remaining amount on a debt record within a transaction.
pub async fn update_remaining<'e, E>(
    executor: E,
    id: &str,
    new_remaining: i64,
    now: &str,
) -> AppResult<()>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    let result = sqlx::query(
        "UPDATE debt_records SET remaining_amount = ?, updated_at = ? WHERE id = ?",
    )
    .bind(new_remaining)
    .bind(now)
    .bind(id)
    .execute(executor)
    .await?;

    if result.rows_affected() == 0 {
        return Err(AppError::Validation {
            field: "debt_id".to_string(),
            message: format!("Debt record {id} not found"),
        });
    }

    Ok(())
}

/// Mark a debt record as settled within a transaction.
pub async fn mark_settled<'e, E>(executor: E, id: &str, now: &str) -> AppResult<()>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    let result = sqlx::query(
        "UPDATE debt_records SET is_settled = 1, remaining_amount = 0, updated_at = ? WHERE id = ?",
    )
    .bind(now)
    .bind(id)
    .execute(executor)
    .await?;

    if result.rows_affected() == 0 {
        return Err(AppError::Validation {
            field: "debt_id".to_string(),
            message: format!("Debt record {id} not found"),
        });
    }

    Ok(())
}

/// Fetch all active (unsettled) debts with customer names for the aging report.
pub async fn get_aging_report(pool: &SqlitePool, pagination: &Pagination) -> AppResult<Vec<DebtRecordWithCustomerRow>> {
    let sql = format!(
        "SELECT d.id, d.customer_id, c.name AS customer_name, d.sale_id,
                d.original_amount, d.remaining_amount, d.sale_date, d.is_settled
         FROM debt_records d
         JOIN customers c ON d.customer_id = c.id
         WHERE d.is_settled = 0
         ORDER BY d.sale_date ASC
         LIMIT {} OFFSET {}",
        pagination.limit, pagination.offset
    );
    let rows = sqlx::query_as::<_, DebtRecordWithCustomerRow>(&sql)
        .fetch_all(pool)
        .await?;

    Ok(rows)
}

/// Fetch all active (unsettled) debts across all customers.
pub async fn get_all_active(pool: &SqlitePool, pagination: &Pagination) -> AppResult<Vec<DebtRecordWithCustomerRow>> {
    let sql = format!(
        "SELECT d.id, d.customer_id, c.name AS customer_name, d.sale_id,
                d.original_amount, d.remaining_amount, d.sale_date, d.is_settled
         FROM debt_records d
         JOIN customers c ON d.customer_id = c.id
         WHERE d.is_settled = 0
         ORDER BY d.sale_date ASC
         LIMIT {} OFFSET {}",
        pagination.limit, pagination.offset
    );
    let rows = sqlx::query_as::<_, DebtRecordWithCustomerRow>(&sql)
        .fetch_all(pool)
        .await?;

    Ok(rows)
}
