use sqlx::{Executor, SqlitePool};

use crate::error::AppResult;

/// Row type matching the `expenses` table schema.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct ExpenseRow {
    pub id: String,
    pub description: String,
    pub amount: f64,
    pub category: String,
    pub wallet_id: String,
    pub recorded_by: String,
    pub timestamp: String,
    pub sync_status: String,
    pub updated_at: String,
}

/// Row type for expenses joined with wallet name.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct ExpenseWithWalletRow {
    pub id: String,
    pub description: String,
    pub amount: f64,
    pub category: String,
    pub wallet_id: String,
    pub wallet_name: String,
    pub recorded_by: String,
    pub timestamp: String,
}

/// Insert an expense record within a transaction.
pub async fn insert_expense_in_tx<'e, E>(
    executor: E,
    id: &str,
    description: &str,
    amount: f64,
    category: &str,
    wallet_id: &str,
    recorded_by: &str,
    timestamp: &str,
    now: &str,
) -> AppResult<()>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    sqlx::query(
        "INSERT INTO expenses (id, description, amount, category, wallet_id, recorded_by, timestamp, sync_status, updated_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, 'Synced', ?)",
    )
    .bind(id)
    .bind(description)
    .bind(amount)
    .bind(category)
    .bind(wallet_id)
    .bind(recorded_by)
    .bind(timestamp)
    .bind(now)
    .execute(executor)
    .await?;

    Ok(())
}

/// Fetch expenses within a date range, joined with wallet name.
pub async fn get_by_date_range(
    pool: &SqlitePool,
    start_date: &str,
    end_date: &str,
) -> AppResult<Vec<ExpenseWithWalletRow>> {
    let rows = sqlx::query_as::<_, ExpenseWithWalletRow>(
        "SELECT e.id, e.description, e.amount, e.category, e.wallet_id, w.name AS wallet_name, e.recorded_by, e.timestamp
         FROM expenses e
         JOIN wallets w ON e.wallet_id = w.id
         WHERE e.timestamp >= ? AND e.timestamp <= ?
         ORDER BY e.timestamp DESC",
    )
    .bind(start_date)
    .bind(end_date)
    .fetch_all(pool)
    .await?;

    Ok(rows)
}

/// Fetch expenses within a date range grouped by category (same query, grouping done in service layer).
/// This returns all expenses in the range; the service groups them by category.
pub async fn get_by_date_range_with_category(
    pool: &SqlitePool,
    start_date: &str,
    end_date: &str,
) -> AppResult<Vec<ExpenseWithWalletRow>> {
    let rows = sqlx::query_as::<_, ExpenseWithWalletRow>(
        "SELECT e.id, e.description, e.amount, e.category, e.wallet_id, w.name AS wallet_name, e.recorded_by, e.timestamp
         FROM expenses e
         JOIN wallets w ON e.wallet_id = w.id
         WHERE e.timestamp >= ? AND e.timestamp <= ?
         ORDER BY e.category ASC, e.timestamp DESC",
    )
    .bind(start_date)
    .bind(end_date)
    .fetch_all(pool)
    .await?;

    Ok(rows)
}
