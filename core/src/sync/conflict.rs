//! Conflict resolution — last-write-wins strategy with logging (Req 13.3).
//!
//! When a sync conflict is detected (concurrent modification of the same record),
//! the engine applies last-write-wins and logs the conflict for admin review.

use chrono::Utc;
use sqlx::SqlitePool;
use tracing::{info, warn};
use uuid::Uuid;

use crate::error::{AppError, AppResult};
use crate::models::domain::ConflictLog;

// ── Row type ───────────────────────────────────────────────────────────────

#[derive(Debug, Clone, sqlx::FromRow)]
pub struct ConflictLogRow {
    pub id: String,
    pub entity_type: String,
    pub entity_id: String,
    pub local_version: String,
    pub remote_version: String,
    pub resolved_with: String,
    pub timestamp: String,
}

fn row_to_conflict(row: ConflictLogRow) -> AppResult<ConflictLog> {
    let id = Uuid::parse_str(&row.id)
        .map_err(|e| AppError::Serialization(format!("Invalid conflict log id: {e}")))?;
    let entity_id = Uuid::parse_str(&row.entity_id)
        .map_err(|e| AppError::Serialization(format!("Invalid entity_id: {e}")))?;
    let timestamp = chrono::DateTime::parse_from_rfc3339(&row.timestamp)
        .map_err(|e| AppError::Serialization(format!("Invalid timestamp: {e}")))?
        .with_timezone(&Utc);

    Ok(ConflictLog {
        id,
        entity_type: row.entity_type,
        entity_id,
        local_version: row.local_version,
        remote_version: row.remote_version,
        resolved_with: row.resolved_with,
        timestamp,
    })
}

// ── Public API ─────────────────────────────────────────────────────────────

/// Resolve a conflict using last-write-wins and log it (Req 13.3).
///
/// `resolved_with` should be `"LOCAL"` or `"REMOTE"` depending on which
/// version has the later timestamp.
pub async fn log_conflict(
    pool: &SqlitePool,
    entity_type: &str,
    entity_id: Uuid,
    local_version: &str,
    remote_version: &str,
    resolved_with: &str,
) -> AppResult<ConflictLog> {
    if resolved_with != "LOCAL" && resolved_with != "REMOTE" {
        return Err(AppError::Validation {
            field: "resolved_with".to_string(),
            message: format!(
                "Invalid resolution '{resolved_with}'. Must be LOCAL or REMOTE"
            ),
        });
    }

    let id = Uuid::new_v4();
    let now = Utc::now();
    let now_str = now.to_rfc3339();

    sqlx::query(
        "INSERT INTO conflict_log (id, entity_type, entity_id, local_version, remote_version, resolved_with, timestamp)
         VALUES (?, ?, ?, ?, ?, ?, ?)",
    )
    .bind(id.to_string())
    .bind(entity_type)
    .bind(entity_id.to_string())
    .bind(local_version)
    .bind(remote_version)
    .bind(resolved_with)
    .bind(&now_str)
    .execute(pool)
    .await?;

    if resolved_with == "LOCAL" {
        info!(
            entity_type = entity_type,
            entity_id = %entity_id,
            "Conflict resolved with LOCAL version (last-write-wins)"
        );
    } else {
        warn!(
            entity_type = entity_type,
            entity_id = %entity_id,
            "Conflict resolved with REMOTE version (last-write-wins)"
        );
    }

    Ok(ConflictLog {
        id,
        entity_type: entity_type.to_string(),
        entity_id,
        local_version: local_version.to_string(),
        remote_version: remote_version.to_string(),
        resolved_with: resolved_with.to_string(),
        timestamp: now,
    })
}

/// Fetch all conflict log entries ordered by timestamp DESC (Req 13.3).
pub async fn get_all(pool: &SqlitePool) -> AppResult<Vec<ConflictLog>> {
    let rows = sqlx::query_as::<_, ConflictLogRow>(
        "SELECT id, entity_type, entity_id, local_version, remote_version, resolved_with, timestamp
         FROM conflict_log
         ORDER BY timestamp DESC",
    )
    .fetch_all(pool)
    .await?;

    rows.into_iter().map(row_to_conflict).collect()
}


// ── Tests ──────────────────────────────────────────────────────────────────

#[cfg(test)]
mod tests {
    use super::*;
    use crate::persistence::db;
    use sqlx::SqlitePool;

    async fn setup() -> SqlitePool {
        db::init_db(":memory:").await.unwrap()
    }

    #[tokio::test]
    async fn log_conflict_stores_entry() {
        let pool = setup().await;
        let entity_id = Uuid::new_v4();

        let entry = log_conflict(
            &pool,
            "product",
            entity_id,
            r#"{"v":1}"#,
            r#"{"v":2}"#,
            "LOCAL",
        )
        .await
        .unwrap();

        assert_eq!(entry.entity_type, "product");
        assert_eq!(entry.entity_id, entity_id);
        assert_eq!(entry.resolved_with, "LOCAL");
    }

    #[tokio::test]
    async fn log_conflict_accepts_remote_resolution() {
        let pool = setup().await;

        let entry = log_conflict(
            &pool,
            "sale",
            Uuid::new_v4(),
            "local",
            "remote",
            "REMOTE",
        )
        .await
        .unwrap();

        assert_eq!(entry.resolved_with, "REMOTE");
    }

    #[tokio::test]
    async fn log_conflict_rejects_invalid_resolution() {
        let pool = setup().await;

        let result = log_conflict(
            &pool,
            "x",
            Uuid::new_v4(),
            "a",
            "b",
            "INVALID",
        )
        .await;

        assert!(result.is_err());
        let err = result.unwrap_err().to_string();
        assert!(err.contains("Invalid resolution"));
    }

    #[tokio::test]
    async fn get_all_returns_desc_order() {
        let pool = setup().await;

        log_conflict(&pool, "first", Uuid::new_v4(), "a", "b", "LOCAL")
            .await
            .unwrap();
        log_conflict(&pool, "second", Uuid::new_v4(), "c", "d", "REMOTE")
            .await
            .unwrap();

        let logs = get_all(&pool).await.unwrap();
        assert_eq!(logs.len(), 2);
        // Most recent first
        assert_eq!(logs[0].entity_type, "second");
        assert_eq!(logs[1].entity_type, "first");
    }

    #[tokio::test]
    async fn get_all_empty_when_no_conflicts() {
        let pool = setup().await;
        let logs = get_all(&pool).await.unwrap();
        assert!(logs.is_empty());
    }
}
