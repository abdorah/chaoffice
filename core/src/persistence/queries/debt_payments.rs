use sqlx::Executor;

use crate::error::AppResult;

/// Insert a debt payment record within a transaction.
pub async fn insert_payment<'e, E>(
    executor: E,
    id: &str,
    customer_id: &str,
    amount: i64,
    wallet_id: &str,
    timestamp: &str,
    updated_at: &str,
) -> AppResult<()>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    sqlx::query(
        "INSERT INTO debt_payments (id, customer_id, amount, wallet_id, timestamp, sync_status, updated_at)
         VALUES (?, ?, ?, ?, ?, 'Synced', ?)",
    )
    .bind(id)
    .bind(customer_id)
    .bind(amount)
    .bind(wallet_id)
    .bind(timestamp)
    .bind(updated_at)
    .execute(executor)
    .await?;

    Ok(())
}

/// Insert a debt payment allocation record within a transaction.
pub async fn insert_allocation<'e, E>(
    executor: E,
    id: &str,
    payment_id: &str,
    debt_record_id: &str,
    amount_applied: i64,
) -> AppResult<()>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    sqlx::query(
        "INSERT INTO debt_payment_allocations (id, payment_id, debt_record_id, amount_applied)
         VALUES (?, ?, ?, ?)",
    )
    .bind(id)
    .bind(payment_id)
    .bind(debt_record_id)
    .bind(amount_applied)
    .execute(executor)
    .await?;

    Ok(())
}
