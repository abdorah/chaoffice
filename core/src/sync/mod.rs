//! Offline mode and data synchronization (Req 13.1–13.5).
//!
//! The sync module manages an offline write queue backed by the `sync_queue`
//! SQLite table and processes it in FIFO order when connectivity is restored.
//! Conflicts are resolved with a last-write-wins strategy and logged in the
//! `conflict_log` table for admin review.

pub mod conflict;
pub mod engine;
pub mod queue;

use reqwest::Client;
use sqlx::SqlitePool;
use uuid::Uuid;

use crate::error::AppResult;
use crate::models::domain::{ConflictLog, Pagination, SyncStatus};

/// Concrete implementation of the SyncManager trait.
pub struct SyncManagerImpl {
    pool: SqlitePool,
    client: Client,
    base_url: String,
}

impl SyncManagerImpl {
    pub fn new(pool: SqlitePool, base_url: String) -> Self {
        Self {
            pool,
            client: Client::new(),
            base_url,
        }
    }

    /// Create with a custom reqwest client (useful for testing).
    #[cfg(test)]
    pub fn with_client(pool: SqlitePool, base_url: String, client: Client) -> Self {
        Self {
            pool,
            client,
            base_url,
        }
    }

    /// Check whether the remote sync endpoint is reachable.
    pub async fn is_online(&self) -> bool {
        engine::check_online(&self.client, &self.base_url).await
    }

    /// Determine the overall sync status based on the queue state (Req 13.4).
    ///
    /// - If any items have status `Conflict` → `SyncStatus::Conflict`
    /// - If any items have status `Pending`  → `SyncStatus::Pending`
    /// - Otherwise                           → `SyncStatus::Synced`
    pub async fn get_sync_status(&self) -> SyncStatus {
        let has_conflict = queue::has_items_with_status(&self.pool, "Conflict")
            .await
            .unwrap_or(false);
        if has_conflict {
            return SyncStatus::Conflict;
        }

        let has_pending = queue::has_items_with_status(&self.pool, "Pending")
            .await
            .unwrap_or(false);
        if has_pending {
            return SyncStatus::Pending;
        }

        SyncStatus::Synced
    }

    /// Process all pending queue items in FIFO order (Req 13.2, 13.5).
    pub async fn sync_all(&self) -> AppResult<()> {
        engine::sync_all(&self.pool, &self.client, &self.base_url).await
    }

    /// Queue a data modification for later sync (Req 13.1).
    pub async fn queue_modification(
        &self,
        entity_type: &str,
        entity_id: Uuid,
        operation: &str,
        payload: &str,
    ) -> AppResult<()> {
        queue::enqueue(&self.pool, entity_type, entity_id, operation, payload).await?;
        Ok(())
    }

    /// Retrieve the conflict log with pagination for admin review (Req 23.1).
    pub async fn get_conflict_log(&self, pagination: &Pagination) -> AppResult<Vec<ConflictLog>> {
        conflict::get_all(&self.pool, pagination).await
    }

    /// Delete conflict log entries older than `retention_days` days (Req 23.2).
    pub async fn cleanup_conflict_log(&self, retention_days: i64) -> AppResult<u64> {
        conflict::cleanup_older_than(&self.pool, retention_days).await
    }
}


// ── Tests ──────────────────────────────────────────────────────────────────

#[cfg(test)]
mod tests {
    use super::*;
    use crate::persistence::db;

    async fn setup() -> SyncManagerImpl {
        let pool = db::init_db(":memory:").await.unwrap();
        // Use a non-routable URL so is_online() returns false in tests
        SyncManagerImpl::new(pool, "http://192.0.2.1:1".to_string())
    }

    #[tokio::test]
    async fn sync_status_synced_when_queue_empty() {
        let mgr = setup().await;
        assert_eq!(mgr.get_sync_status().await, SyncStatus::Synced);
    }

    #[tokio::test]
    async fn sync_status_pending_after_queue_modification() {
        let mgr = setup().await;
        mgr.queue_modification("product", Uuid::new_v4(), "CREATE", "{}")
            .await
            .unwrap();

        assert_eq!(mgr.get_sync_status().await, SyncStatus::Pending);
    }

    #[tokio::test]
    async fn queue_modification_validates_operation() {
        let mgr = setup().await;
        let result = mgr
            .queue_modification("x", Uuid::new_v4(), "BADOP", "{}")
            .await;
        assert!(result.is_err());
    }

    #[tokio::test]
    async fn get_conflict_log_empty_initially() {
        let mgr = setup().await;
        let logs = mgr.get_conflict_log(&Pagination::default()).await.unwrap();
        assert!(logs.is_empty());
    }

    #[tokio::test]
    async fn is_online_returns_false_for_unreachable_host() {
        let mgr = setup().await;
        // 192.0.2.1 is a TEST-NET address that should be unreachable
        assert!(!mgr.is_online().await);
    }

    #[tokio::test]
    async fn sync_all_succeeds_with_empty_queue() {
        let mgr = setup().await;
        // No pending items — should succeed immediately
        let result = mgr.sync_all().await;
        assert!(result.is_ok());
    }

    #[tokio::test]
    async fn sync_status_conflict_when_conflict_items_exist() {
        let mgr = setup().await;

        // Insert an item and manually set it to Conflict status
        let item = queue::enqueue(&mgr.pool, "x", Uuid::new_v4(), "CREATE", "{}")
            .await
            .unwrap();
        queue::update_status(&mgr.pool, item.id, &SyncStatus::Conflict)
            .await
            .unwrap();

        assert_eq!(mgr.get_sync_status().await, SyncStatus::Conflict);
    }

    #[tokio::test]
    async fn conflict_takes_priority_over_pending() {
        let mgr = setup().await;

        // One pending item
        mgr.queue_modification("a", Uuid::new_v4(), "CREATE", "{}")
            .await
            .unwrap();

        // One conflict item
        let item = queue::enqueue(&mgr.pool, "b", Uuid::new_v4(), "UPDATE", "{}")
            .await
            .unwrap();
        queue::update_status(&mgr.pool, item.id, &SyncStatus::Conflict)
            .await
            .unwrap();

        // Conflict should take priority
        assert_eq!(mgr.get_sync_status().await, SyncStatus::Conflict);
    }
}
