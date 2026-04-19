use std::collections::HashMap;

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

use crate::config::SyncStrategy;

/// Result of a hydrate (LibSQL → redb) operation.
#[derive(Debug, Clone)]
pub struct HydrateResult {
    pub entities_loaded: HashMap<String, usize>,
    pub was_online: bool,
}

/// Result of a dehydrate (redb → LibSQL) operation.
#[derive(Debug, Clone)]
pub struct DehydrateResult {
    pub entities_written: HashMap<String, usize>,
    pub remote_sync_succeeded: bool,
}

/// Result of a full bidirectional sync operation.
#[derive(Debug, Clone)]
pub struct SyncResult {
    pub pushed: HashMap<String, usize>,
    pub pulled: HashMap<String, usize>,
    pub conflicts_resolved: usize,
    pub strategy_used: SyncStrategy,
}

/// Generic row representation for conflict resolution.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EntityRow {
    pub entity_type: String,
    pub id: u64,
    pub data: serde_json::Value,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
    pub deleted_at: Option<DateTime<Utc>>,
}
