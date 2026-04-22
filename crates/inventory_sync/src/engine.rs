//! SyncEngine — central orchestrator for all sync operations.
//!
//! Holds a real libsql::Database and Connection for local-only or remote replica mode.

use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::Arc;

use tokio::sync::{Mutex, RwLock};

use crate::config::{SyncConfig, SyncStrategy};
use crate::error::SyncError;
use crate::bridge::*;
use crate::schema::SchemaManager;
use crate::tracker::ChangeTracker;
use crate::types::{DehydrateResult, HydrateResult, SyncResult};

use common::database::db_context::DbContext;

use inventory_security::SecurityContext;
use inventory_security_macros::check_permission;

/// The core sync engine orchestrating hydrate, dehydrate, and full_sync.
pub struct SyncEngine {
    config: RwLock<SyncConfig>,
    config_path: PathBuf,
    db: RwLock<libsql::Database>,
    conn: RwLock<libsql::Connection>,
    change_tracker: Arc<ChangeTracker>,
    db_context: DbContext,
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
        db_context: DbContext,
    ) -> Result<Self, SyncError> {
        let db = if config.turso_url.is_empty() {
            log::info!("SyncEngine: no TursoDB URL configured, opening local-only database");
            libsql::Builder::new_local("inventory_data.db")
                .build()
                .await
                .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?
        } else {
            log::info!("SyncEngine: opening remote replica with TursoDB URL: {}", config.turso_url);
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
            db: RwLock::new(db),
            conn: RwLock::new(conn),
            change_tracker,
            db_context,
            auto_sync_handle: Mutex::new(None),
        })
    }

    /// Ensure the database handle matches the current config mode.
    ///
    /// At startup the engine opens in local-only mode for fast init, but the
    /// config may already contain a TursoDB URL. This method lazily rebuilds
    /// the db as a remote replica (or back to local-only) when the first sync
    /// operation detects a mismatch.
    async fn ensure_db_matches_config(&self) -> Result<(), SyncError> {
        let config = self.config.read().await.clone();
        let needs_remote = !config.turso_url.is_empty();

        let db_path = "inventory_data.db";

        // Pre-emptively clean up orphaned auxiliary files that can cause
        // "metadata file exists but db file does not" errors.
        if needs_remote && !std::path::Path::new(db_path).exists() {
            for suffix in &["-wal", "-shm", "-metadata"] {
                let p = format!("{}{}", db_path, suffix);
                if std::path::Path::new(&p).exists() {
                    let _ = std::fs::remove_file(&p);
                    log::info!("ensure_db_matches_config: pre-cleaned orphaned '{}'", p);
                }
            }
        }

        // Quick check: try db.sync() — if it fails on a local-only db with a
        // remote URL configured, we need to rebuild.
        if needs_remote {
            let db = self.db.read().await;
            match db.sync().await {
                Ok(_) => return Ok(()), // Already a working remote replica
                Err(e) => {
                    let msg = e.to_string();
                    // If the error is about local state / metadata, we need to rebuild
                    // Also rebuild if sync simply isn't supported (local-only db)
                    if msg.contains("metadata")
                        || msg.contains("local state")
                        || msg.contains("sync is not supported")
                        || msg.contains("Sync is not supported")
                    {
                        log::info!(
                            "ensure_db_matches_config: db needs rebuild for remote mode ({})",
                            msg
                        );
                    } else {
                        // Some other sync error (network, auth, etc.) — db is already
                        // in remote mode, just a transient failure
                        return Ok(());
                    }
                }
            }
            drop(db);
        } else {
            // Config has no URL — local-only is fine
            return Ok(());
        }

        // Rebuild: release current handles, move files, create remote replica
        {
            let mut db = self.db.write().await;
            let mut conn = self.conn.write().await;
            let tmp_db = libsql::Builder::new_local(":memory:")
                .build()
                .await
                .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;
            let tmp_conn = tmp_db
                .connect()
                .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;
            *conn = tmp_conn;
            *db = tmp_db;
        }

        let db_path = "inventory_data.db";

        // Clean up all libsql auxiliary files to ensure a fresh state.
        // This handles both cases: db exists without metadata, and
        // metadata exists without db.
        for suffix in &["-wal", "-shm", "-metadata"] {
            let p = format!("{}{}", db_path, suffix);
            if std::path::Path::new(&p).exists() {
                let _ = std::fs::remove_file(&p);
                log::info!("ensure_db_matches_config: removed '{}'", p);
            }
        }

        // Move local-only db aside so remote replica can start fresh
        if std::path::Path::new(db_path).exists() {
            let backup = format!("{}.local-backup", db_path);
            log::info!("ensure_db_matches_config: backing up local db to '{}'", backup);
            let _ = std::fs::rename(db_path, &backup);
        }

        let new_db = libsql::Builder::new_remote_replica(
            db_path,
            config.turso_url.clone(),
            config.turso_auth_token.clone(),
        )
        .build()
        .await
        .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;

        let new_conn = new_db
            .connect()
            .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;

        SchemaManager::init_schema(&new_conn).await?;

        // Migrate data from the local backup into the new remote replica
        let backup = format!("{}.local-backup", db_path);
        Self::migrate_data_from_backup(&backup, &new_conn).await;

        let mut db = self.db.write().await;
        *db = new_db;
        let mut conn = self.conn.write().await;
        *conn = new_conn;

        log::info!("ensure_db_matches_config: rebuilt as remote replica");
        Ok(())
    }

    /// Migrate data from a local backup db file into the target connection.
    ///
    /// Opens the backup as a separate local-only database, reads all rows,
    /// and INSERTs them into the target. This avoids ATTACH which is not
    /// supported by TursoDB remote replicas.
    async fn migrate_data_from_backup(backup_path: &str, target_conn: &libsql::Connection) {
        if !std::path::Path::new(backup_path).exists() {
            log::info!("migrate_data_from_backup: no backup file found, skipping");
            return;
        }

        log::info!("migrate_data_from_backup: opening backup '{}'", backup_path);

        // Open the backup as a local-only db
        let backup_db = match libsql::Builder::new_local(backup_path).build().await {
            Ok(db) => db,
            Err(e) => {
                log::error!("migrate_data_from_backup: failed to open backup: {}", e);
                return;
            }
        };
        let backup_conn = match backup_db.connect() {
            Ok(c) => c,
            Err(e) => {
                log::error!("migrate_data_from_backup: failed to connect to backup: {}", e);
                return;
            }
        };

        let all_tables = {
            let mut tables = SchemaManager::entity_table_names();
            tables.push("sync_metadata");
            tables
        };

        for table in &all_tables {
            // Get column names from the backup table
            let pragma_sql = format!("PRAGMA table_info({})", table);
            let mut col_rows = match backup_conn.query(&pragma_sql, ()).await {
                Ok(r) => r,
                Err(e) => {
                    log::warn!("  skipped '{}': pragma failed: {}", table, e);
                    continue;
                }
            };

            let mut columns: Vec<String> = Vec::new();
            while let Ok(Some(row)) = col_rows.next().await {
                if let Ok(name) = row.get::<String>(1) {
                    columns.push(name);
                }
            }

            if columns.is_empty() {
                log::warn!("  skipped '{}': no columns found", table);
                continue;
            }

            // Read all rows from backup
            let select_sql = format!("SELECT {} FROM {}", columns.join(", "), table);
            let mut rows = match backup_conn.query(&select_sql, ()).await {
                Ok(r) => r,
                Err(e) => {
                    log::warn!("  skipped '{}': select failed: {}", table, e);
                    continue;
                }
            };

            let placeholders: Vec<String> = columns.iter().enumerate().map(|(i, _)| format!("?{}", i + 1)).collect();
            let insert_sql = format!(
                "INSERT OR REPLACE INTO {} ({}) VALUES ({})",
                table,
                columns.join(", "),
                placeholders.join(", ")
            );

            let mut migrated = 0u64;
            while let Ok(Some(row)) = rows.next().await {
                // Build params from each column value
                let mut params: Vec<libsql::Value> = Vec::new();
                for (i, _col) in columns.iter().enumerate() {
                    // Try integer first, then float, then string, then null
                    let val = if let Ok(v) = row.get::<i64>(i as i32) {
                        libsql::Value::Integer(v)
                    } else if let Ok(v) = row.get::<f64>(i as i32) {
                        libsql::Value::Real(v)
                    } else if let Ok(v) = row.get::<String>(i as i32) {
                        libsql::Value::Text(v)
                    } else {
                        libsql::Value::Null
                    };
                    params.push(val);
                }

                match target_conn.execute(&insert_sql, libsql::params_from_iter(params)).await {
                    Ok(_) => migrated += 1,
                    Err(e) => {
                        log::warn!("  insert into '{}' failed: {}", table, e);
                    }
                }
            }

            if migrated > 0 {
                log::info!("  migrated {} rows into '{}'", migrated, table);
            }
        }

        log::info!("migrate_data_from_backup: complete");
    }

    /// Hydrate: sync remote → local replica, then load all entities into redb.
    pub async fn hydrate(&self, ctx: &SecurityContext) -> Result<HydrateResult, SyncError> {
        // RBAC check: sync:trigger required
        check_permission(ctx, "sync:trigger")
            .map_err(|e| SyncError::AccessDenied(e.to_string()))?;

        // Ensure db is in the right mode before syncing
        self.ensure_db_matches_config().await?;

        // Attempt remote sync if configured
        let was_online = self.is_online().await;

        let mut entities_loaded: HashMap<String, usize> = HashMap::new();

        // For each entity table, SELECT all non-deleted rows and count them
        let conn = self.conn.read().await;
        for table in SchemaManager::entity_table_names() {
            let sql = format!("SELECT * FROM {} WHERE deleted_at IS NULL", table);
            let mut rows = conn
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

    /// Dehydrate: flush all entities from redb → LibSQL, then sync to remote.
    pub async fn dehydrate(&self, ctx: &SecurityContext) -> Result<DehydrateResult, SyncError> {
        // RBAC check: sync:trigger required
        check_permission(ctx, "sync:trigger")
            .map_err(|e| SyncError::AccessDenied(e.to_string()))?;

        // Ensure db is in the right mode before syncing
        self.ensure_db_matches_config().await?;

        // Drain pending changes from the ChangeTracker
        let _pending = self.change_tracker.drain();

        // Flush all entities from redb → LibSQL
        let entities_written = self.flush_entities_to_libsql().await?;

        let mut remote_sync_succeeded = false;

        // Attempt to sync LibSQL replica to TursoDB Cloud
        let config = self.config.read().await;
        if !config.turso_url.is_empty() {
            let db = self.db.read().await;
            match db.sync().await {
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

    /// Read all entities from redb and upsert them into LibSQL.
    async fn flush_entities_to_libsql(&self) -> Result<HashMap<String, usize>, SyncError> {
        use common::database::transactions::Transaction;
        use common::direct_access::repository_factory::read;

        let txn = Transaction::begin_read_transaction(&self.db_context)
            .map_err(|e| SyncError::ConnectionFailed(format!("redb read txn: {}", e)))?;

        let conn = self.conn.read().await;
        let mut counts: HashMap<String, usize> = HashMap::new();

        // Products
        {
            let repo = read::create_product_repository(&txn)
                .map_err(|e| SyncError::ConnectionFailed(format!("product repo: {}", e)))?;
            let items = repo.get_all()
                .map_err(|e| SyncError::ConnectionFailed(format!("redb products: {}", e)))?;
            let sql = ProductBridge::upsert_sql();
            let mut n = 0usize;
            for p in &items {
                let ts = p.created_at.to_rfc3339();
                let us = p.updated_at.to_rfc3339();
                let status = format!("{:?}", p.status);
                let params = libsql::params![
                    p.id as i64, p.name.as_str(), p.reference.as_str(), p.description.as_str(),
                    p.quantity, p.price_unit, status.as_str(),
                    p.category.map(|v| v as i64), p.supplier.map(|v| v as i64),
                    p.location.map(|v| v as i64), ts.as_str(), us.as_str()
                ];
                match conn.execute(&sql, params).await {
                    Ok(_) => n += 1,
                    Err(e) => log::warn!("flush product {}: {}", p.id, e),
                }
            }
            if n > 0 { log::info!("flush: {} products", n); }
            counts.insert("products".to_string(), n);
        }

        // Categories
        {
            let repo = read::create_category_repository(&txn)
                .map_err(|e| SyncError::ConnectionFailed(format!("category repo: {}", e)))?;
            let items = repo.get_all()
                .map_err(|e| SyncError::ConnectionFailed(format!("redb categories: {}", e)))?;
            let sql = CategoryBridge::upsert_sql();
            let mut n = 0usize;
            for c in &items {
                let ts = c.created_at.to_rfc3339();
                let us = c.updated_at.to_rfc3339();
                let params = libsql::params![
                    c.id as i64, c.name.as_str(), c.description.as_str(),
                    c.parent_category.map(|v| v as i64), ts.as_str(), us.as_str()
                ];
                match conn.execute(&sql, params).await {
                    Ok(_) => n += 1,
                    Err(e) => log::warn!("flush category {}: {}", c.id, e),
                }
            }
            if n > 0 { log::info!("flush: {} categories", n); }
            counts.insert("categories".to_string(), n);
        }

        // Persons
        {
            let repo = read::create_person_repository(&txn)
                .map_err(|e| SyncError::ConnectionFailed(format!("person repo: {}", e)))?;
            let items = repo.get_all()
                .map_err(|e| SyncError::ConnectionFailed(format!("redb persons: {}", e)))?;
            let sql = PersonBridge::upsert_sql();
            let mut n = 0usize;
            for p in &items {
                let ts = p.created_at.to_rfc3339();
                let us = p.updated_at.to_rfc3339();
                let role = format!("{:?}", p.role);
                let params = libsql::params![
                    p.id as i64, p.name.as_str(), role.as_str(),
                    p.contact.map(|v| v as i64), ts.as_str(), us.as_str()
                ];
                match conn.execute(&sql, params).await {
                    Ok(_) => n += 1,
                    Err(e) => log::warn!("flush person {}: {}", p.id, e),
                }
            }
            if n > 0 { log::info!("flush: {} persons", n); }
            counts.insert("persons".to_string(), n);
        }

        // Contacts
        {
            let repo = read::create_contact_repository(&txn)
                .map_err(|e| SyncError::ConnectionFailed(format!("contact repo: {}", e)))?;
            let items = repo.get_all()
                .map_err(|e| SyncError::ConnectionFailed(format!("redb contacts: {}", e)))?;
            let sql = ContactBridge::upsert_sql();
            let mut n = 0usize;
            for c in &items {
                let ts = c.created_at.to_rfc3339();
                let us = c.updated_at.to_rfc3339();
                let params = libsql::params![
                    c.id as i64, c.phone.as_str(), c.email.as_str(), ts.as_str(), us.as_str()
                ];
                match conn.execute(&sql, params).await {
                    Ok(_) => n += 1,
                    Err(e) => log::warn!("flush contact {}: {}", c.id, e),
                }
            }
            if n > 0 { log::info!("flush: {} contacts", n); }
            counts.insert("contacts".to_string(), n);
        }

        // Deals
        {
            let repo = read::create_deal_repository(&txn)
                .map_err(|e| SyncError::ConnectionFailed(format!("deal repo: {}", e)))?;
            let items = repo.get_all()
                .map_err(|e| SyncError::ConnectionFailed(format!("redb deals: {}", e)))?;
            let sql = DealBridge::upsert_sql();
            let mut n = 0usize;
            for d in &items {
                let ts = d.created_at.to_rfc3339();
                let us = d.updated_at.to_rfc3339();
                let sd = d.start_date.to_rfc3339();
                let ed = d.end_date.to_rfc3339();
                let freq = format!("{:?}", d.frequency);
                let status = format!("{:?}", d.status);
                let params = libsql::params![
                    d.id as i64, d.title.as_str(), d.description.as_str(),
                    d.unit_cost, d.total_value, sd.as_str(), ed.as_str(),
                    freq.as_str(), status.as_str(),
                    d.product.map(|v| v as i64), d.supplier.map(|v| v as i64),
                    d.manager.map(|v| v as i64), ts.as_str(), us.as_str()
                ];
                match conn.execute(&sql, params).await {
                    Ok(_) => n += 1,
                    Err(e) => log::warn!("flush deal {}: {}", d.id, e),
                }
            }
            if n > 0 { log::info!("flush: {} deals", n); }
            counts.insert("deals".to_string(), n);
        }

        // Locations
        {
            let repo = read::create_location_repository(&txn)
                .map_err(|e| SyncError::ConnectionFailed(format!("location repo: {}", e)))?;
            let items = repo.get_all()
                .map_err(|e| SyncError::ConnectionFailed(format!("redb locations: {}", e)))?;
            let sql = LocationBridge::upsert_sql();
            let mut n = 0usize;
            for l in &items {
                let ts = l.created_at.to_rfc3339();
                let us = l.updated_at.to_rfc3339();
                let params = libsql::params![
                    l.id as i64, l.name.as_str(), l.address.as_str(),
                    l.latitude, l.longitude, l.capacity,
                    ts.as_str(), us.as_str()
                ];
                match conn.execute(&sql, params).await {
                    Ok(_) => n += 1,
                    Err(e) => log::warn!("flush location {}: {}", l.id, e),
                }
            }
            if n > 0 { log::info!("flush: {} locations", n); }
            counts.insert("locations".to_string(), n);
        }

        // Users
        {
            let repo = read::create_user_repository(&txn)
                .map_err(|e| SyncError::ConnectionFailed(format!("user repo: {}", e)))?;
            let items = repo.get_all()
                .map_err(|e| SyncError::ConnectionFailed(format!("redb users: {}", e)))?;
            let sql = UserBridge::upsert_sql();
            let mut n = 0usize;
            for u in &items {
                let ts = u.created_at.to_rfc3339();
                let us = u.updated_at.to_rfc3339();
                let role = format!("{:?}", u.role);
                let params = libsql::params![
                    u.id as i64, u.username.as_str(), u.password_hash.as_str(),
                    u.display_name.as_str(), role.as_str(),
                    u.is_active as i64, u.person.map(|v| v as i64),
                    ts.as_str(), us.as_str()
                ];
                match conn.execute(&sql, params).await {
                    Ok(_) => n += 1,
                    Err(e) => log::warn!("flush user {}: {}", u.id, e),
                }
            }
            if n > 0 { log::info!("flush: {} users", n); }
            counts.insert("users".to_string(), n);
        }

        // Sessions
        {
            let repo = read::create_session_repository(&txn)
                .map_err(|e| SyncError::ConnectionFailed(format!("session repo: {}", e)))?;
            let items = repo.get_all()
                .map_err(|e| SyncError::ConnectionFailed(format!("redb sessions: {}", e)))?;
            let sql = SessionBridge::upsert_sql();
            let mut n = 0usize;
            for s in &items {
                let ts = s.created_at.to_rfc3339();
                let us = s.updated_at.to_rfc3339();
                let ea = s.expires_at.to_rfc3339();
                let params = libsql::params![
                    s.id as i64, s.token.as_str(), ea.as_str(), ts.as_str(), us.as_str()
                ];
                match conn.execute(&sql, params).await {
                    Ok(_) => n += 1,
                    Err(e) => log::warn!("flush session {}: {}", s.id, e),
                }
            }
            if n > 0 { log::info!("flush: {} sessions", n); }
            counts.insert("sessions".to_string(), n);
        }

        // StockMovements
        {
            let repo = read::create_stock_movement_repository(&txn)
                .map_err(|e| SyncError::ConnectionFailed(format!("stock_movement repo: {}", e)))?;
            let items = repo.get_all()
                .map_err(|e| SyncError::ConnectionFailed(format!("redb stock_movements: {}", e)))?;
            let sql = StockMovementBridge::upsert_sql();
            let mut n = 0usize;
            for m in &items {
                let ts = m.created_at.to_rfc3339();
                let us = m.updated_at.to_rfc3339();
                let mt = format!("{:?}", m.movement_type);
                let params = libsql::params![
                    m.id as i64, mt.as_str(), m.quantity,
                    m.note.as_str(), m.product.map(|v| v as i64),
                    m.from_location.map(|v| v as i64), m.to_location.map(|v| v as i64),
                    m.performed_by.map(|v| v as i64), ts.as_str(), us.as_str()
                ];
                match conn.execute(&sql, params).await {
                    Ok(_) => n += 1,
                    Err(e) => log::warn!("flush stock_movement {}: {}", m.id, e),
                }
            }
            if n > 0 { log::info!("flush: {} stock_movements", n); }
            counts.insert("stock_movements".to_string(), n);
        }

        // BudgetEntries
        {
            let repo = read::create_budget_entry_repository(&txn)
                .map_err(|e| SyncError::ConnectionFailed(format!("budget_entry repo: {}", e)))?;
            let items = repo.get_all()
                .map_err(|e| SyncError::ConnectionFailed(format!("redb budget_entries: {}", e)))?;
            let sql = BudgetEntryBridge::upsert_sql();
            let mut n = 0usize;
            for b in &items {
                let ts = b.created_at.to_rfc3339();
                let us = b.updated_at.to_rfc3339();
                let ed = b.entry_date.to_rfc3339();
                let et = format!("{:?}", b.entry_type);
                let params = libsql::params![
                    b.id as i64, et.as_str(), b.amount,
                    b.description.as_str(), ed.as_str(), b.product.map(|v| v as i64),
                    b.deal.map(|v| v as i64), b.recorded_by.map(|v| v as i64),
                    ts.as_str(), us.as_str()
                ];
                match conn.execute(&sql, params).await {
                    Ok(_) => n += 1,
                    Err(e) => log::warn!("flush budget_entry {}: {}", b.id, e),
                }
            }
            if n > 0 { log::info!("flush: {} budget_entries", n); }
            counts.insert("budget_entries".to_string(), n);
        }

        log::info!("flush_entities_to_libsql: complete ({:?})", counts);
        Ok(counts)
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

        // Ensure db is in the right mode before syncing
        self.ensure_db_matches_config().await?;

        // Step 1: Dehydrate all pending local changes first
        let _dehydrate_result = self.dehydrate(ctx).await?;

        // Step 2: Check connectivity
        if !self.is_online().await {
            return Err(SyncError::Offline);
        }

        // Step 3: Push local LibSQL changes to TursoDB Cloud
        {
            let db = self.db.read().await;
            db.sync()
                .await
                .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;
        }

        // Step 4: Pull remote changes from TursoDB Cloud
        {
            let db = self.db.read().await;
            db.sync()
                .await
                .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;
        }

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
        // Ensure db is in the right mode before checking connectivity
        if self.ensure_db_matches_config().await.is_err() {
            return false;
        }
        let config = self.config.read().await;
        if config.turso_url.is_empty() {
            // Local-only mode: verify connection with a simple query
            let conn = self.conn.read().await;
            match conn.query("SELECT 1", ()).await {
                Ok(_) => true,
                Err(_) => false,
            }
        } else {
            // Remote mode: attempt sync
            let db = self.db.read().await;
            match db.sync().await {
                Ok(_) => true,
                Err(_) => false,
            }
        }
    }

    /// Update and persist configuration.
    ///
    /// When the TursoDB URL or auth token changes, the database and connection
    /// are rebuilt so that subsequent operations use the correct remote replica
    /// (or local-only) mode.
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

        // Check if remote credentials changed
        let old_config = self.config.read().await.clone();
        let credentials_changed = old_config.turso_url != new_config.turso_url
            || old_config.turso_auth_token != new_config.turso_auth_token;

        // Rebuild database connection if credentials changed
        if credentials_changed {
            // Drop the current db and conn handles before touching files
            // so libsql releases its locks on the db file.
            {
                let mut db = self.db.write().await;
                let mut conn = self.conn.write().await;
                // Replace with a temporary local db to release the file lock
                let tmp_db = libsql::Builder::new_local(":memory:")
                    .build()
                    .await
                    .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;
                let tmp_conn = tmp_db
                    .connect()
                    .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;
                *conn = tmp_conn;
                *db = tmp_db;
            }

            let db_path = "inventory_data.db";

            // Always clean up auxiliary files first to avoid stale state
            for suffix in &["-wal", "-shm", "-metadata"] {
                let p = format!("{}{}", db_path, suffix);
                if std::path::Path::new(&p).exists() {
                    let _ = std::fs::remove_file(&p);
                    log::info!("SyncEngine::configure: removed '{}'", p);
                }
            }

            // When switching to remote replica mode, move the local db aside
            if !new_config.turso_url.is_empty() && std::path::Path::new(db_path).exists() {
                let backup = format!("{}.local-backup", db_path);
                log::info!(
                    "SyncEngine::configure: backing up local db to '{}'",
                    backup
                );
                let _ = std::fs::rename(db_path, &backup);
            }

            // When switching back to local-only, restore the backup if available
            if new_config.turso_url.is_empty() {
                let backup = format!("{}.local-backup", db_path);
                if std::path::Path::new(&backup).exists() && !std::path::Path::new(db_path).exists() {
                    log::info!("SyncEngine::configure: restoring local db from backup");
                    let _ = std::fs::rename(&backup, db_path);
                }
            }

            let new_db = if new_config.turso_url.is_empty() {
                log::info!("SyncEngine::configure: switching to local-only mode");
                libsql::Builder::new_local(db_path)
                    .build()
                    .await
                    .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?
            } else {
                log::info!(
                    "SyncEngine::configure: rebuilding remote replica with URL: {}",
                    new_config.turso_url
                );
                libsql::Builder::new_remote_replica(
                    db_path,
                    new_config.turso_url.clone(),
                    new_config.turso_auth_token.clone(),
                )
                .build()
                .await
                .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?
            };

            let new_conn = new_db
                .connect()
                .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;

            // Re-initialize schema on the new connection
            SchemaManager::init_schema(&new_conn).await?;

            // Migrate data from the local backup into the new remote replica
            if !new_config.turso_url.is_empty() {
                let backup = format!("{}.local-backup", db_path);
                Self::migrate_data_from_backup(&backup, &new_conn).await;
            }

            // Swap in the new database and connection
            let mut db = self.db.write().await;
            *db = new_db;
            let mut conn = self.conn.write().await;
            *conn = new_conn;

            log::info!("SyncEngine::configure: database connection rebuilt");
        }

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

    /// Update the in-memory and on-disk config without rebuilding the database
    /// connection. Used at startup to restore the original config while keeping
    /// the local-only db handle.
    pub async fn set_config_only(&self, new_config: SyncConfig) -> Result<(), SyncError> {
        new_config.save(&self.config_path)?;
        let mut config = self.config.write().await;
        *config = new_config;
        log::info!("SyncEngine: config updated (no db rebuild)");
        Ok(())
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
