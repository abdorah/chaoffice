//! Offline queue management — insert, fetch pending, update status.
//!
//! All items are persisted in the `sync_queue` SQLite table and processed
//! in FIFO order (by `created_at ASC`) when connectivity is restored.

use chrono::Utc;
use sqlx::SqlitePool;
use tracing::{info, warn};
use uuid::Uuid;

use crate::error::{AppError, AppResult};
use crate::models::domain::{SyncQueueItem, SyncStatus};

// ── Row type ───────────────────────────────────────────────────────────────

#[derive(Debug, Clone, sqlx::FromRow)]
pub struct SyncQueueRow {
    pub id: String,
    pub entity_type: String,
    pub entity_id: String,
    pub operation: String,
    pub payload: String,
    pub created_at: String,
    pub status: String,
}

// ── Helpers ────────────────────────────────────────────────────────────────

const VALID_OPERATIONS: &[&str] = &["CREATE", "UPDATE", "DELETE"];

fn parse_sync_status(s: &str) -> SyncStatus {
    match s {
        "Synced" => SyncStatus::Synced,
        "Conflict" => SyncStatus::Conflict,
        _ => SyncStatus::Pending,
    }
}

fn sync_status_to_str(s: &SyncStatus) -> &'static str {
    match s {
        SyncStatus::Synced => "Synced",
        SyncStatus::Pending => "Pending",
        SyncStatus::Conflict => "Conflict",
    }
}

fn row_to_item(row: SyncQueueRow) -> AppResult<SyncQueueItem> {
    let id = Uuid::parse_str(&row.id)
        .map_err(|e| AppError::Serialization(format!("Invalid queue item id: {e}")))?;
    let entity_id = Uuid::parse_str(&row.entity_id)
        .map_err(|e| AppError::Serialization(format!("Invalid entity_id: {e}")))?;
    let created_at = chrono::DateTime::parse_from_rfc3339(&row.created_at)
        .map_err(|e| AppError::Serialization(format!("Invalid created_at: {e}")))?
        .with_timezone(&Utc);

    Ok(SyncQueueItem {
        id,
        entity_type: row.entity_type,
        entity_id,
        operation: row.operation,
        payload: row.payload,
        created_at,
        status: parse_sync_status(&row.status),
    })
}

// ── Public API ─────────────────────────────────────────────────────────────

/// Insert a new modification into the sync queue (Req 13.1).
pub async fn enqueue(
    pool: &SqlitePool,
    entity_type: &str,
    entity_id: Uuid,
    operation: &str,
    payload: &str,
) -> AppResult<SyncQueueItem> {
    if !VALID_OPERATIONS.contains(&operation) {
        return Err(AppError::Validation {
            field: "operation".to_string(),
            message: format!(
                "Invalid operation '{operation}'. Must be one of: CREATE, UPDATE, DELETE"
            ),
        });
    }

    let id = Uuid::new_v4();
    let now = Utc::now();
    let now_str = now.to_rfc3339();

    sqlx::query(
        "INSERT INTO sync_queue (id, entity_type, entity_id, operation, payload, created_at, status)
         VALUES (?, ?, ?, ?, ?, ?, 'Pending')",
    )
    .bind(id.to_string())
    .bind(entity_type)
    .bind(entity_id.to_string())
    .bind(operation)
    .bind(payload)
    .bind(&now_str)
    .execute(pool)
    .await?;

    info!(
        entity_type = entity_type,
        entity_id = %entity_id,
        operation = operation,
        "Queued sync modification"
    );

    Ok(SyncQueueItem {
        id,
        entity_type: entity_type.to_string(),
        entity_id,
        operation: operation.to_string(),
        payload: payload.to_string(),
        created_at: now,
        status: SyncStatus::Pending,
    })
}

/// Fetch all pending items ordered by created_at ASC (FIFO) for sync processing (Req 13.2).
pub async fn get_pending(pool: &SqlitePool) -> AppResult<Vec<SyncQueueItem>> {
    let rows = sqlx::query_as::<_, SyncQueueRow>(
        "SELECT id, entity_type, entity_id, operation, payload, created_at, status
         FROM sync_queue
         WHERE status = 'Pending'
         ORDER BY created_at ASC",
    )
    .fetch_all(pool)
    .await?;

    rows.into_iter().map(row_to_item).collect()
}

/// Update the status of a queue item.
pub async fn update_status(pool: &SqlitePool, id: Uuid, status: &SyncStatus) -> AppResult<()> {
    let result = sqlx::query("UPDATE sync_queue SET status = ? WHERE id = ?")
        .bind(sync_status_to_str(status))
        .bind(id.to_string())
        .execute(pool)
        .await?;

    if result.rows_affected() == 0 {
        warn!(id = %id, "Attempted to update non-existent sync queue item");
        return Err(AppError::Validation {
            field: "id".to_string(),
            message: format!("Sync queue item {id} not found"),
        });
    }

    Ok(())
}

/// Check whether any items exist with a given status.
pub async fn has_items_with_status(pool: &SqlitePool, status: &str) -> AppResult<bool> {
    let row: (i64,) =
        sqlx::query_as("SELECT COUNT(*) FROM sync_queue WHERE status = ?")
            .bind(status)
            .fetch_one(pool)
            .await?;
    Ok(row.0 > 0)
}


// ── Tests ──────────────────────────────────────────────────────────────────

#[cfg(test)]
mod tests {
    use super::*;
    use crate::persistence::db;

    async fn setup() -> SqlitePool {
        let pool = db::init_db(":memory:").await.unwrap();
        pool
    }

    #[tokio::test]
    async fn enqueue_inserts_pending_item() {
        let pool = setup().await;
        let entity_id = Uuid::new_v4();

        let item = enqueue(&pool, "product", entity_id, "CREATE", r#"{"name":"cake"}"#)
            .await
            .unwrap();

        assert_eq!(item.entity_type, "product");
        assert_eq!(item.entity_id, entity_id);
        assert_eq!(item.operation, "CREATE");
        assert_eq!(item.status, SyncStatus::Pending);
    }

    #[tokio::test]
    async fn enqueue_rejects_invalid_operation() {
        let pool = setup().await;
        let entity_id = Uuid::new_v4();

        let result = enqueue(&pool, "product", entity_id, "INVALID", "{}").await;
        assert!(result.is_err());
        let err = result.unwrap_err().to_string();
        assert!(err.contains("Invalid operation"));
    }

    #[tokio::test]
    async fn enqueue_accepts_all_valid_operations() {
        let pool = setup().await;
        for op in &["CREATE", "UPDATE", "DELETE"] {
            let result = enqueue(&pool, "item", Uuid::new_v4(), op, "{}").await;
            assert!(result.is_ok(), "Operation {op} should be accepted");
        }
    }

    #[tokio::test]
    async fn get_pending_returns_fifo_order() {
        let pool = setup().await;

        // Insert three items with slight time gaps
        let id1 = Uuid::new_v4();
        let id2 = Uuid::new_v4();
        let id3 = Uuid::new_v4();

        enqueue(&pool, "a", id1, "CREATE", "1").await.unwrap();
        enqueue(&pool, "b", id2, "UPDATE", "2").await.unwrap();
        enqueue(&pool, "c", id3, "DELETE", "3").await.unwrap();

        let pending = get_pending(&pool).await.unwrap();
        assert_eq!(pending.len(), 3);
        assert_eq!(pending[0].entity_id, id1);
        assert_eq!(pending[1].entity_id, id2);
        assert_eq!(pending[2].entity_id, id3);
    }

    #[tokio::test]
    async fn get_pending_excludes_synced_items() {
        let pool = setup().await;

        let item = enqueue(&pool, "x", Uuid::new_v4(), "CREATE", "{}").await.unwrap();
        update_status(&pool, item.id, &SyncStatus::Synced).await.unwrap();

        enqueue(&pool, "y", Uuid::new_v4(), "UPDATE", "{}").await.unwrap();

        let pending = get_pending(&pool).await.unwrap();
        assert_eq!(pending.len(), 1);
        assert_eq!(pending[0].entity_type, "y");
    }

    #[tokio::test]
    async fn update_status_changes_item_status() {
        let pool = setup().await;
        let item = enqueue(&pool, "z", Uuid::new_v4(), "CREATE", "{}").await.unwrap();

        update_status(&pool, item.id, &SyncStatus::Synced).await.unwrap();

        let pending = get_pending(&pool).await.unwrap();
        assert!(pending.is_empty());
    }

    #[tokio::test]
    async fn update_status_nonexistent_fails() {
        let pool = setup().await;
        let result = update_status(&pool, Uuid::new_v4(), &SyncStatus::Synced).await;
        assert!(result.is_err());
    }

    #[tokio::test]
    async fn has_items_with_status_detects_pending() {
        let pool = setup().await;

        assert!(!has_items_with_status(&pool, "Pending").await.unwrap());

        enqueue(&pool, "t", Uuid::new_v4(), "CREATE", "{}").await.unwrap();

        assert!(has_items_with_status(&pool, "Pending").await.unwrap());
        assert!(!has_items_with_status(&pool, "Synced").await.unwrap());
    }
}
