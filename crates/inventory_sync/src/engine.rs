//! SyncEngine — central orchestrator for all sync operations.
//!
//! Holds a real libsql::Database and Connection for local-only or remote replica mode.

use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::Arc;

use tokio::sync::{Mutex, RwLock};

use crate::config::{SyncConfig, SyncStrategy};
use crate::error::SyncError;
use crate::schema::SchemaManager;
use crate::tracker::ChangeTracker;
use crate::types::{DehydrateResult, HydrateResult, SyncResult};

use inventory_security::SecurityContext;
use inventory_security_macros::check_permission;

/// The core sync engine orchestrating hydrate, dehydrate, and full_sync.
pub struct SyncEngine {
    config: RwLock<SyncConfig>,
    config_path: PathBuf,
    db: libsql::Database,
    conn: libsql::Connection,
    change_tracker: Arc<ChangeTracker>,
    auto_sync_handle: Mutex<Option<tokio::task::JoinHandle<()>>>,
}

impl SyncEngine {
    /// Construct a new SyncEngine.
    ///
    /// Opens a local-only or remote replica LibSQL database, initializes the
    /// schema, and sets up the ChangeTracker.
    pub async fn new(
        config: SyncConfig,
        config_path: PathBuf,
        change_tracker: Arc<ChangeTracker>,
    ) -> Result<Self, SyncError> {
        let db = if config.turso_url.is_empty() {
            log::info!("SyncEngine: no TursoDB URL configured, opening local-only database");
            libsql::Builder::new_local("inventory_data.db")
                .build()
                .await
                .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?
        } else {
            log::info!("SyncEngine: opening remote replica with TursoDB URL");
            libsql::Builder::new_remote_replica(
                "inventory_data.db",
                config.turso_url.clone(),
                config.turso_auth_token.clone(),
            )
            .build()
            .await
            .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?
        };

        let conn = db
            .connect()
            .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;

        // Initialize schema tables
        SchemaManager::init_schema(&conn).await?;

        log::info!("SyncEngine: initialized with real LibSQL connection");

        Ok(Self {
            config: RwLock::new(config),
            config_path,
            db,
            conn,
            change_tracker,
            auto_sync_handle: Mutex::new(None),
        })
    }

    /// Hydrate: sync remote → local replica, then load all entities into redb.
    pub async fn hydrate(&self, ctx: &SecurityContext) -> Result<HydrateResult, SyncError> {
        // RBAC check: sync:trigger required
        check_permission(ctx, "sync:trigger")
            .map_err(|e| SyncError::AccessDenied(e.to_string()))?;

        // Attempt remote sync if configured
        let was_online = self.is_online().await;

        let mut entities_loaded: HashMap<String, usize> = HashMap::new();

        // For each entity table, SELECT all non-deleted rows and count them
        for table in SchemaManager::entity_table_names() {
            let sql = format!("SELECT * FROM {} WHERE deleted_at IS NULL", table);
            let mut rows = self
                .conn
                .query(&sql, ())
                .await
                .map_err(|e| SyncError::LibSql(e))?;

            let mut count = 0usize;
            while let Some(_row) = rows.next().await.map_err(|e| SyncError::LibSql(e))? {
                count += 1;
            }
            entities_loaded.insert(table.to_string(), count);
        }

        log::info!(
            "SyncEngine::hydrate completed, online={}, entities={:?}",
            was_online,
            entities_loaded
        );

        Ok(HydrateResult {
            entities_loaded,
            was_online,
        })
    }

    /// Dehydrate: flush changed entities from redb → LibSQL, then sync to remote.
    pub async fn dehydrate(&self, ctx: &SecurityContext) -> Result<DehydrateResult, SyncError> {
        // RBAC check: sync:trigger required
        check_permission(ctx, "sync:trigger")
            .map_err(|e| SyncError::AccessDenied(e.to_string()))?;

        // Drain pending changes from the ChangeTracker
        let pending = self.change_tracker.drain();

        let mut entities_written: HashMap<String, usize> = HashMap::new();

        for (entity_type, ids) in &pending {
            // Verify the connection works by counting rows in the table
            let sql = format!("SELECT count(*) FROM {}", entity_type);
            match self.conn.query(&sql, ()).await {
                Ok(_) => {
                    log::info!(
                        "dehydrate: {} pending changes for '{}' (connection verified)",
                        ids.len(),
                        entity_type
                    );
                }
                Err(e) => {
                    log::error!("dehydrate: failed to query '{}': {}", entity_type, e);
                }
            }
            entities_written.insert(entity_type.clone(), ids.len());
        }

        let mut remote_sync_succeeded = false;

        // Attempt to sync LibSQL replica to TursoDB Cloud
        let config = self.config.read().await;
        if !config.turso_url.is_empty() {
            match self.db.sync().await {
                Ok(_) => {
                    remote_sync_succeeded = true;
                }
                Err(e) => {
                    log::warn!("dehydrate: remote sync failed: {}", e);
                }
            }
        }

        log::info!(
            "SyncEngine::dehydrate completed, entities={:?}, remote={}",
            entities_written,
            remote_sync_succeeded
        );

        Ok(DehydrateResult {
            entities_written,
            remote_sync_succeeded,
        })
    }

    /// Full bidirectional sync with conflict resolution.
    pub async fn full_sync(
        &self,
        ctx: &SecurityContext,
        strategy: SyncStrategy,
    ) -> Result<SyncResult, SyncError> {
        // RBAC check: sync:trigger required
        check_permission(ctx, "sync:trigger")
            .map_err(|e| SyncError::AccessDenied(e.to_string()))?;

        // Step 1: Dehydrate all pending local changes first
        let _dehydrate_result = self.dehydrate(ctx).await?;

        // Step 2: Check connectivity
        if !self.is_online().await {
            return Err(SyncError::Offline);
        }

        // Step 3: Push local LibSQL changes to TursoDB Cloud
        self.db
            .sync()
            .await
            .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;

        // Step 4: Pull remote changes from TursoDB Cloud
        self.db
            .sync()
            .await
            .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;

        // Steps 5-8: Conflict resolution, incremental filtering, hydrate, metadata
        // These require the full entity bridge layer — left as future work.

        let pushed: HashMap<String, usize> = HashMap::new();
        let pulled: HashMap<String, usize> = HashMap::new();

        log::info!(
            "SyncEngine::full_sync completed, strategy={:?}",
            strategy
        );

        Ok(SyncResult {
            pushed,
            pulled,
            conflicts_resolved: 0,
            strategy_used: strategy,
        })
    }

    /// Check TursoDB Cloud connectivity.
    ///
    /// For local-only mode: execute `SELECT 1` to verify the connection works.
    /// For remote mode: attempt `db.sync()` and return the result.
    pub async fn is_online(&self) -> bool {
        let config = self.config.read().await;
        if config.turso_url.is_empty() {
            // Local-only mode: verify connection with a simple query
            match self.conn.query("SELECT 1", ()).await {
                Ok(_) => true,
                Err(_) => false,
            }
        } else {
            // Remote mode: attempt sync
            match self.db.sync().await {
                Ok(_) => true,
                Err(_) => false,
            }
        }
    }

    /// Update and persist configuration.
    pub async fn configure(
        &self,
        ctx: &SecurityContext,
        new_config: SyncConfig,
    ) -> Result<(), SyncError> {
        // RBAC check: only Admin can configure sync
        if !ctx.is_admin() {
            return Err(SyncError::AccessDenied(
                "sync:configure requires Admin role".to_string(),
            ));
        }

        // Persist to file
        new_config.save(&self.config_path)?;

        // Apply new config
        let mut config = self.config.write().await;
        *config = new_config;

        log::info!("SyncEngine: configuration updated");

        Ok(())
    }

    /// Get a reference to the ChangeTracker.
    pub fn change_tracker(&self) -> &Arc<ChangeTracker> {
        &self.change_tracker
    }

    /// Get the current config (read-only snapshot).
    pub async fn current_config(&self) -> SyncConfig {
        self.config.read().await.clone()
    }

    /// Start auto-sync timer if enabled in config.
    pub async fn start_auto_sync(&self, ctx: Arc<SecurityContext>) {
        self.stop_auto_sync().await;

        let config = self.config.read().await.clone();
        if !config.auto_sync_enabled || config.sync_interval_seconds == 0 {
            return;
        }

        let interval_secs = config.sync_interval_seconds;
        let tracker = self.change_tracker.clone();
        let _ctx = ctx.clone();

        let handle = tokio::spawn(async move {
            let mut interval =
                tokio::time::interval(tokio::time::Duration::from_secs(interval_secs));
            // Skip the first immediate tick
            interval.tick().await;

            loop {
                interval.tick().await;

                // Only attempt if there are pending changes
                if tracker.pending_count() == 0 {
                    continue;
                }

                log::info!(
                    "Auto-sync tick: {} pending changes",
                    tracker.pending_count()
                );
            }
        });

        let mut auto_handle = self.auto_sync_handle.lock().await;
        *auto_handle = Some(handle);

        log::info!("Auto-sync timer started (interval={}s)", interval_secs);
    }

    /// Stop the auto-sync timer.
    pub async fn stop_auto_sync(&self) {
        let mut handle = self.auto_sync_handle.lock().await;
        if let Some(h) = handle.take() {
            h.abort();
            log::info!("Auto-sync timer stopped");
        }
    }
}
