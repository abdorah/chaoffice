//! Sync orchestration — processes the offline queue in FIFO order when
//! connectivity is restored (Req 13.2, 13.5).
//!
//! The engine pushes each pending item to the configured remote endpoint via
//! `reqwest`. On success the item is marked `Synced`; on a 409 conflict the
//! last-write-wins strategy is applied and the conflict is logged; on a
//! network error the item stays `Pending`.

use reqwest::Client;
use sqlx::SqlitePool;
use tracing::{error, info, warn};

use crate::error::{AppError, AppResult};
use crate::models::domain::SyncStatus;

use super::conflict;
use super::queue;

/// Push a single queue item to the remote sync endpoint.
///
/// Returns:
/// - `Ok(SyncOutcome::Synced)` on 2xx
/// - `Ok(SyncOutcome::Conflict { remote_version })` on 409
/// - `Err(AppError::Network)` on connection failure
#[derive(Debug)]
pub enum SyncOutcome {
    Synced,
    Conflict { remote_version: String },
}

pub async fn push_item(
    client: &Client,
    base_url: &str,
    entity_type: &str,
    entity_id: &str,
    operation: &str,
    payload: &str,
) -> AppResult<SyncOutcome> {
    let url = format!("{}/sync/{}/{}", base_url, entity_type, entity_id);

    let response = client
        .post(&url)
        .json(&serde_json::json!({
            "operation": operation,
            "payload": payload,
        }))
        .send()
        .await?;

    let status = response.status();

    if status.is_success() {
        Ok(SyncOutcome::Synced)
    } else if status.as_u16() == 409 {
        // Extract remote version from response body if available
        let body = response.text().await.unwrap_or_default();
        Ok(SyncOutcome::Conflict {
            remote_version: body,
        })
    } else {
        Err(AppError::Unknown(format!(
            "Sync push failed with status {status}"
        )))
    }
}

/// Process all pending queue items in FIFO order (Req 13.2, 13.5).
///
/// For each item:
/// 1. Attempt to push to remote
/// 2. On success → mark Synced
/// 3. On 409 conflict → apply last-write-wins, log conflict, mark Synced
/// 4. On network error → leave as Pending, stop processing, return error
pub async fn sync_all(
    pool: &SqlitePool,
    client: &Client,
    base_url: &str,
) -> AppResult<()> {
    let pending = queue::get_pending(pool).await?;

    if pending.is_empty() {
        info!("No pending items to sync");
        return Ok(());
    }

    info!(count = pending.len(), "Starting sync of pending items");

    for item in &pending {
        let result = push_item(
            client,
            base_url,
            &item.entity_type,
            &item.entity_id.to_string(),
            &item.operation,
            &item.payload,
        )
        .await;

        match result {
            Ok(SyncOutcome::Synced) => {
                queue::update_status(pool, item.id, &SyncStatus::Synced).await?;
                info!(
                    id = %item.id,
                    entity_type = %item.entity_type,
                    "Item synced successfully"
                );
            }
            Ok(SyncOutcome::Conflict { remote_version }) => {
                // Last-write-wins: keep local version, log the conflict
                conflict::log_conflict(
                    pool,
                    &item.entity_type,
                    item.entity_id,
                    &item.payload,
                    &remote_version,
                    "LOCAL",
                )
                .await?;
                queue::update_status(pool, item.id, &SyncStatus::Synced).await?;
                warn!(
                    id = %item.id,
                    entity_type = %item.entity_type,
                    "Conflict resolved with last-write-wins (LOCAL)"
                );
            }
            Err(e) => {
                error!(
                    id = %item.id,
                    entity_type = %item.entity_type,
                    error = %e,
                    "Sync failed — item remains Pending"
                );
                return Err(e);
            }
        }
    }

    info!("Sync completed successfully");
    Ok(())
}

/// Simple connectivity check — attempt a HEAD request to the sync endpoint.
pub async fn check_online(client: &Client, base_url: &str) -> bool {
    let url = format!("{}/health", base_url);
    match client.head(&url).send().await {
        Ok(resp) => resp.status().is_success(),
        Err(_) => false,
    }
}
