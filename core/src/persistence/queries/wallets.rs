use sqlx::{Executor, SqlitePool};

use crate::error::{AppError, AppResult};
use crate::models::domain::Pagination;

/// Row type matching the `wallets` table schema.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct WalletRow {
    pub id: String,
    pub name: String,
    pub wallet_type: String,
    pub current_balance: i64,
    pub sync_status: String,
    pub updated_at: String,
}

/// Row type matching the `wallet_transactions` table schema.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct WalletTransactionRow {
    pub id: String,
    pub wallet_id: String,
    pub amount: i64,
    pub description: String,
    pub related_entity_id: Option<String>,
    pub timestamp: String,
    pub sync_status: String,
    pub updated_at: String,
}

/// Fetch all wallets.
pub async fn list_all(pool: &SqlitePool, pagination: &Pagination) -> AppResult<Vec<WalletRow>> {
    let sql = format!(
        "SELECT id, name, wallet_type, current_balance, sync_status, updated_at FROM wallets ORDER BY name ASC LIMIT {} OFFSET {}",
        pagination.limit, pagination.offset
    );
    let rows = sqlx::query_as::<_, WalletRow>(&sql)
        .fetch_all(pool)
        .await?;

    Ok(rows)
}

/// Fetch a single wallet by id. Returns `None` if not found.
pub async fn get_by_id(pool: &SqlitePool, id: &str) -> AppResult<Option<WalletRow>> {
    let row = sqlx::query_as::<_, WalletRow>(
        "SELECT id, name, wallet_type, current_balance, sync_status, updated_at FROM wallets WHERE id = ?",
    )
    .bind(id)
    .fetch_optional(pool)
    .await?;

    Ok(row)
}

/// Fetch a wallet by id within a transaction. Returns `None` if not found.
pub async fn get_by_id_in_tx<'e, E>(executor: E, id: &str) -> AppResult<Option<WalletRow>>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    let row = sqlx::query_as::<_, WalletRow>(
        "SELECT id, name, wallet_type, current_balance, sync_status, updated_at FROM wallets WHERE id = ?",
    )
    .bind(id)
    .fetch_optional(executor)
    .await?;

    Ok(row)
}

/// Credit a wallet's balance using an atomic delta operation (Req 7.3).
/// `delta` must be positive — it is added to current_balance.
pub async fn update_balance_in_tx<'e, E>(
    executor: E,
    id: &str,
    delta: i64,
    now: &str,
) -> AppResult<()>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    let result = sqlx::query(
        "UPDATE wallets SET current_balance = current_balance + ?, updated_at = ? WHERE id = ?",
    )
    .bind(delta)
    .bind(now)
    .bind(id)
    .execute(executor)
    .await?;

    if result.rows_affected() == 0 {
        return Err(AppError::Validation {
            field: "wallet_id".to_string(),
            message: format!("Wallet {id} not found"),
        });
    }

    Ok(())
}

/// Atomically debit a wallet's balance with a balance check (Req 7.3).
/// Returns `Ok(())` if the debit succeeds, or `Err` if the wallet is not found
/// or has insufficient funds. The caller must provide the wallet name for error messages.
pub async fn atomic_debit_in_tx<'e, E>(
    executor: E,
    id: &str,
    amount: i64,
    now: &str,
) -> AppResult<u64>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    let result = sqlx::query(
        "UPDATE wallets SET current_balance = current_balance - ?, updated_at = ? WHERE id = ? AND current_balance >= ?",
    )
    .bind(amount)
    .bind(now)
    .bind(id)
    .bind(amount)
    .execute(executor)
    .await?;

    Ok(result.rows_affected())
}

/// Insert a wallet transaction record within a transaction.
pub async fn insert_transaction_in_tx<'e, E>(
    executor: E,
    id: &str,
    wallet_id: &str,
    amount: i64,
    description: &str,
    related_entity_id: Option<&str>,
    timestamp: &str,
    now: &str,
) -> AppResult<()>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    sqlx::query(
        "INSERT INTO wallet_transactions (id, wallet_id, amount, description, related_entity_id, timestamp, sync_status, updated_at)
         VALUES (?, ?, ?, ?, ?, ?, 'Synced', ?)",
    )
    .bind(id)
    .bind(wallet_id)
    .bind(amount)
    .bind(description)
    .bind(related_entity_id)
    .bind(timestamp)
    .bind(now)
    .execute(executor)
    .await?;

    Ok(())
}

/// Fetch transaction history for a wallet, ordered by timestamp descending.
pub async fn get_transactions(
    pool: &SqlitePool,
    wallet_id: &str,
    pagination: &Pagination,
) -> AppResult<Vec<WalletTransactionRow>> {
    let sql = format!(
        "SELECT id, wallet_id, amount, description, related_entity_id, timestamp, sync_status, updated_at
         FROM wallet_transactions
         WHERE wallet_id = ?
         ORDER BY timestamp DESC
         LIMIT {} OFFSET {}",
        pagination.limit, pagination.offset
    );
    let rows = sqlx::query_as::<_, WalletTransactionRow>(&sql)
        .bind(wallet_id)
        .fetch_all(pool)
        .await?;

    Ok(rows)
}

/// Insert a new wallet (used primarily in tests and seeding).
/// `current_balance` is in integer cents (e.g., 50000 = $500.00).
pub async fn insert_wallet(
    pool: &SqlitePool,
    id: &str,
    name: &str,
    wallet_type: &str,
    current_balance: i64,
    now: &str,
) -> AppResult<()> {
    sqlx::query(
        "INSERT INTO wallets (id, name, wallet_type, current_balance, sync_status, updated_at)
         VALUES (?, ?, ?, ?, 'Synced', ?)",
    )
    .bind(id)
    .bind(name)
    .bind(wallet_type)
    .bind(current_balance)
    .bind(now)
    .execute(pool)
    .await?;

    Ok(())
}
