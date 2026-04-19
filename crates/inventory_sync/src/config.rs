use std::path::Path;

use serde::{Deserialize, Serialize};

use crate::error::SyncError;

/// Sync strategy enum defining the sync approach.
#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq, Eq)]
pub enum SyncStrategy {
    Full,
    Incremental,
    ConflictResolveLocal,
    ConflictResolveRemote,
}

/// Configuration for the sync engine.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SyncConfig {
    pub turso_url: String,
    pub turso_auth_token: String,
    pub auto_sync_enabled: bool,
    pub sync_interval_seconds: u64,
    pub default_strategy: SyncStrategy,
}

impl Default for SyncConfig {
    fn default() -> Self {
        Self {
            turso_url: String::new(),
            turso_auth_token: String::new(),
            auto_sync_enabled: false,
            sync_interval_seconds: 300,
            default_strategy: SyncStrategy::Incremental,
        }
    }
}

impl SyncConfig {
    /// Load configuration from a JSON file.
    pub fn load(path: &Path) -> Result<Self, SyncError> {
        let contents = std::fs::read_to_string(path)?;
        let config: SyncConfig = serde_json::from_str(&contents)?;
        Ok(config)
    }

    /// Save configuration to a JSON file.
    pub fn save(&self, path: &Path) -> Result<(), SyncError> {
        let contents = serde_json::to_string_pretty(self)?;
        std::fs::write(path, contents)?;
        Ok(())
    }
}
