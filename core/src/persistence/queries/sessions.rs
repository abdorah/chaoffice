use sqlx::SqlitePool;

use crate::error::AppResult;

/// Row type matching the `sessions` table schema.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct SessionRow {
    pub id: String,
    pub user_id: String,
    pub role: String,
    pub created_at: String,
    pub last_activity: String,
    pub expires_at: String,
}

/// Insert a new session into the `sessions` table.
pub async fn insert_session(
    pool: &SqlitePool,
    id: &str,
    user_id: &str,
    role: &str,
    created_at: &str,
    last_activity: &str,
    expires_at: &str,
) -> AppResult<()> {
    sqlx::query(
        "INSERT INTO sessions (id, user_id, role, created_at, last_activity, expires_at)
         VALUES (?, ?, ?, ?, ?, ?)",
    )
    .bind(id)
    .bind(user_id)
    .bind(role)
    .bind(created_at)
    .bind(last_activity)
    .bind(expires_at)
    .execute(pool)
    .await?;

    Ok(())
}

/// Fetch a session by its ID. Returns `None` if not found.
pub async fn get_session(pool: &SqlitePool, id: &str) -> AppResult<Option<SessionRow>> {
    let row = sqlx::query_as::<_, SessionRow>("SELECT * FROM sessions WHERE id = ?")
        .bind(id)
        .fetch_optional(pool)
        .await?;

    Ok(row)
}

/// Delete a session by its ID.
pub async fn delete_session(pool: &SqlitePool, id: &str) -> AppResult<()> {
    sqlx::query("DELETE FROM sessions WHERE id = ?")
        .bind(id)
        .execute(pool)
        .await?;

    Ok(())
}

/// Update the last_activity timestamp for a session.
pub async fn update_last_activity(
    pool: &SqlitePool,
    id: &str,
    last_activity: &str,
) -> AppResult<()> {
    sqlx::query("UPDATE sessions SET last_activity = ? WHERE id = ?")
        .bind(last_activity)
        .bind(id)
        .execute(pool)
        .await?;

    Ok(())
}

/// Delete all sessions whose expires_at is before the given timestamp.
pub async fn delete_expired(pool: &SqlitePool, now: &str) -> AppResult<u64> {
    let result = sqlx::query("DELETE FROM sessions WHERE expires_at <= ?")
        .bind(now)
        .execute(pool)
        .await?;

    Ok(result.rows_affected())
}
