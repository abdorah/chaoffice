//! SyncEngine — central orchestrator for all sync operations.
//!
//! Architecture:
//! - redb is the primary data store (all CRUD goes through it)
//! - LibSQL is the sync transport layer (redb → LibSQL → TursoDB)
//! - Push: redb → LibSQL → db.sync() → TursoDB
//! - Pull: TursoDB → db.sync() → LibSQL → redb → UI refresh
//!
//! At startup the engine opens LibSQL in local-only mode for instant init.
//! On first sync operation, it lazily rebuilds as a remote replica if a
//! TursoDB URL is configured.

use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::Arc;
use std::sync::OnceLock;
use std::sync::atomic::{AtomicBool, Ordering};

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

/// Filename for the libsql sync database. This MUST be different from the
/// redb database file (`inventory_data.db`) because they use incompatible
/// file formats.
const LIBSQL_DB_NAME: &str = "inventory_sync.db";

/// Absolute path to the libsql database, derived from the redb database path
/// passed to `mobile_init`. On Android the working directory is `/`, so a
/// relative path would try to write to `/inventory_data.db` which is not
/// writable. This is set once by `set_libsql_db_dir` and read by the sync
/// engine.
pub static LIBSQL_DB_DIR: OnceLock<PathBuf> = OnceLock::new();

/// Called from `mobile_init` to store the app-private data directory so the
/// sync engine can derive an absolute path for its libsql database.
pub fn set_libsql_db_dir(dir: PathBuf) {
    let _ = LIBSQL_DB_DIR.set(dir);
}

/// Returns the absolute path to the libsql sync database file.
fn libsql_db_path() -> PathBuf {
    LIBSQL_DB_DIR
        .get()
        .map(|dir| dir.join(LIBSQL_DB_NAME))
        .unwrap_or_else(|| PathBuf::from(LIBSQL_DB_NAME))
}

/// The core sync engine orchestrating hydrate, dehydrate, and full_sync.
pub struct SyncEngine {
    config: RwLock<SyncConfig>,
    config_path: PathBuf,
    db: RwLock<libsql::Database>,
    conn: RwLock<libsql::Connection>,
    change_tracker: Arc<ChangeTracker>,
    db_context: DbContext,
    /// True once the db has been successfully rebuilt as a remote replica.
    is_remote: AtomicBool,
    auto_sync_handle: Mutex<Option<tokio::task::JoinHandle<()>>>,
}

impl SyncEngine {
    /// Construct a new SyncEngine.
    /// If a valid remote replica exists (db + metadata files), opens it directly.
    /// Otherwise opens local-only for fast init — remote is lazy via ensure_remote().
    pub async fn new(
        config: SyncConfig,
        config_path: PathBuf,
        change_tracker: Arc<ChangeTracker>,
        db_context: DbContext,
    ) -> Result<Self, SyncError> {
        // Install bundled Mozilla CA root certificates for TLS.
        // On Android the native cert store is not accessible via standard
        // filesystem paths, so rustls-native-certs finds 0 valid roots.
        // This must happen before any TLS connection is attempted.
        Self::install_tls_roots();

        let db_path = libsql_db_path();
        let db_path_str = db_path.to_string_lossy();
        let metadata_exists = std::path::Path::new(&format!("{}-metadata", db_path_str)).exists()
            || std::path::Path::new(&format!("{}-info", db_path_str)).exists();
        let db_exists = db_path.exists();
        let has_url = !config.turso_url.is_empty();

        // If we have both db + metadata + a URL, reopen as remote replica directly
        let (db, is_remote) = if db_exists && metadata_exists && has_url {
            log::info!("SyncEngine: reopening existing remote replica");
            match libsql::Builder::new_remote_replica(
                db_path_str.as_ref(),
                config.turso_url.clone(),
                config.turso_auth_token.clone(),
            )
            .build()
            .await
            {
                Ok(db) => (db, true),
                Err(e) => {
                    log::warn!("SyncEngine: failed to reopen remote replica ({}), falling back to local-only", e);
                    Self::clean_aux_files();
                    let db = libsql::Builder::new_local(db_path_str.as_ref())
                        .build()
                        .await
                        .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;
                    (db, false)
                }
            }
        } else {
            // Clean stale metadata if db doesn't exist
            if !db_exists && metadata_exists {
                Self::clean_aux_files();
            }
            log::info!("SyncEngine: opening local-only database at {}", db_path_str);
            let db = libsql::Builder::new_local(db_path_str.as_ref())
                .build()
                .await
                .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;
            (db, false)
        };

        let conn = db
            .connect()
            .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;

        SchemaManager::init_schema(&conn).await?;
        log::info!("SyncEngine: initialized (remote={})", is_remote);

        Ok(Self {
            config: RwLock::new(config),
            config_path,
            db: RwLock::new(db),
            conn: RwLock::new(conn),
            change_tracker,
            db_context,
            is_remote: AtomicBool::new(is_remote),
            auto_sync_handle: Mutex::new(None),
        })
    }

    // ── File cleanup helpers ──────────────────────────────────────────

    /// Install bundled Mozilla CA root certificates for TLS on Android.
    /// On Android the native cert store is not accessible via the standard
    /// filesystem paths that rustls-native-certs expects. We install the
    /// ring crypto provider and set SSL_CERT_DIR to Android's system CA
    /// directory so rustls-native-certs can find the certificates.
    fn install_tls_roots() {
        use std::sync::Once;

        static INIT: Once = Once::new();
        INIT.call_once(|| {
            // Install the ring crypto provider globally so that libsql's
            // internal hyper-rustls picks it up automatically.
            let _ = rustls::crypto::ring::default_provider().install_default();

            // Android stores CA certs as individual DER files in this directory.
            // rustls-native-certs (via openssl-probe) checks SSL_CERT_DIR.
            // On newer Android versions (7+), apps can also read from
            // /system/etc/security/cacerts/.
            if std::env::var("SSL_CERT_FILE").is_err() && std::env::var("SSL_CERT_DIR").is_err() {
                // Try the standard Android CA cert locations
                let cert_dirs = [
                    "/system/etc/security/cacerts",
                    "/etc/security/cacerts",
                ];
                for dir in &cert_dirs {
                    if std::path::Path::new(dir).is_dir() {
                        // SAFETY: called exactly once via Once::call_once during
                        // init, before any other threads read env vars.
                        unsafe {
                            std::env::set_var("SSL_CERT_DIR", dir);
                        }
                        log::info!("install_tls_roots: set SSL_CERT_DIR={}", dir);
                        break;
                    }
                }
            }

            log::info!("install_tls_roots: TLS configured");
        });
    }

    /// Remove all libsql auxiliary files (wal, shm, metadata/info).
    fn clean_aux_files() {
        let db_path = libsql_db_path();
        let db_path_str = db_path.to_string_lossy();
        for suffix in &["-wal", "-shm", "-metadata", "-info"] {
            let p = format!("{}{}", db_path_str, suffix);
            if std::path::Path::new(&p).exists() {
                let _ = std::fs::remove_file(&p);
                log::info!("clean_aux_files: removed '{}'", p);
            }
        }
    }

    /// Remove the backup file if it exists.
    fn clean_backup() {
        let db_path = libsql_db_path();
        let backup = format!("{}.local-backup", db_path.to_string_lossy());
        if std::path::Path::new(&backup).exists() {
            let _ = std::fs::remove_file(&backup);
            log::info!("clean_backup: removed '{}'", backup);
        }
    }

    // ── Lazy remote rebuild ───────────────────────────────────────────

    /// Ensure the db handle is a remote replica if a URL is configured.
    /// This is a no-op if already in remote mode or no URL is set.
    async fn ensure_remote(&self) -> Result<(), SyncError> {
        let config = self.config.read().await.clone();
        if config.turso_url.is_empty() {
            return Ok(()); // No URL → local-only is correct
        }
        if self.is_remote.load(Ordering::Relaxed) {
            return Ok(()); // Already rebuilt
        }

        log::info!("ensure_remote: rebuilding db as remote replica");

        // 1. Release current handles
        {
            let mut db = self.db.write().await;
            let mut conn = self.conn.write().await;
            let tmp = libsql::Builder::new_local(":memory:")
                .build().await
                .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;
            *conn = tmp.connect().map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;
            *db = tmp;
        }

        // 2. Clean all aux files and move local db aside
        Self::clean_aux_files();
        let db_path = libsql_db_path();
        let db_path_str = db_path.to_string_lossy();
        if db_path.exists() {
            let backup = format!("{}.local-backup", db_path_str);
            let _ = std::fs::rename(&db_path, &backup);
            log::info!("ensure_remote: backed up local db");
        }

        // 3. Build remote replica
        let new_db = libsql::Builder::new_remote_replica(
            db_path_str.as_ref(),
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

        // 4. Swap in
        {
            let mut db = self.db.write().await;
            *db = new_db;
            let mut conn = self.conn.write().await;
            *conn = new_conn;
        }

        self.is_remote.store(true, Ordering::Relaxed);
        Self::clean_backup(); // No longer needed
        log::info!("ensure_remote: done");
        Ok(())
    }

    // ── Push (dehydrate): redb → LibSQL → TursoDB ─────────────────────

    pub async fn dehydrate(&self, ctx: &SecurityContext) -> Result<DehydrateResult, SyncError> {
        check_permission(ctx, "sync:trigger")
            .map_err(|e| SyncError::AccessDenied(e.to_string()))?;

        self.ensure_remote().await?;

        // Flush redb → LibSQL
        let entities_written = self.flush_redb_to_libsql().await?;

        // Push LibSQL → TursoDB
        let mut remote_sync_succeeded = false;
        let config = self.config.read().await;
        if !config.turso_url.is_empty() {
            let db = self.db.read().await;
            match db.sync().await {
                Ok(_) => remote_sync_succeeded = true,
                Err(e) => log::warn!("dehydrate: remote sync failed: {}", e),
            }
        }

        log::info!("dehydrate: entities={:?}, remote={}", entities_written, remote_sync_succeeded);
        Ok(DehydrateResult { entities_written, remote_sync_succeeded })
    }

    // ── Pull (hydrate): TursoDB → LibSQL → redb ──────────────────────

    pub async fn hydrate(&self, ctx: &SecurityContext) -> Result<HydrateResult, SyncError> {
        check_permission(ctx, "sync:trigger")
            .map_err(|e| SyncError::AccessDenied(e.to_string()))?;

        self.ensure_remote().await?;

        // Pull TursoDB → LibSQL (db.sync pulls remote frames into local replica)
        let config = self.config.read().await.clone();
        let was_online = if !config.turso_url.is_empty() {
            let db = self.db.read().await;
            match db.sync().await {
                Ok(_) => true,
                Err(e) => {
                    log::warn!("hydrate: remote sync failed: {}", e);
                    false
                }
            }
        } else {
            false
        };

        // Import LibSQL → redb
        let conn = self.conn.read().await;
        let mut entities_loaded = HashMap::new();
        self.import_libsql_to_redb(&conn, &mut entities_loaded).await?;

        log::info!("hydrate: online={}, entities={:?}", was_online, entities_loaded);
        Ok(HydrateResult { entities_loaded, was_online })
    }

    // ── Connection test ───────────────────────────────────────────────

    pub async fn is_online(&self) -> bool {
        let config = self.config.read().await.clone();
        if config.turso_url.is_empty() {
            let conn = self.conn.read().await;
            return conn.query("SELECT 1", ()).await.is_ok();
        }
        if self.ensure_remote().await.is_err() {
            return false;
        }
        let db = self.db.read().await;
        db.sync().await.is_ok()
    }

    // ── Full sync ─────────────────────────────────────────────────────

    pub async fn full_sync(
        &self,
        ctx: &SecurityContext,
        strategy: SyncStrategy,
    ) -> Result<SyncResult, SyncError> {
        check_permission(ctx, "sync:trigger")
            .map_err(|e| SyncError::AccessDenied(e.to_string()))?;

        self.ensure_remote().await?;
        let _dehydrate = self.dehydrate(ctx).await?;

        if !self.is_online().await {
            return Err(SyncError::Offline);
        }

        {
            let db = self.db.read().await;
            db.sync().await.map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;
        }

        log::info!("full_sync: completed, strategy={:?}", strategy);
        Ok(SyncResult {
            pushed: HashMap::new(),
            pulled: HashMap::new(),
            conflicts_resolved: 0,
            strategy_used: strategy,
        })
    }

    // ── Configure ─────────────────────────────────────────────────────

    pub async fn configure(
        &self,
        ctx: &SecurityContext,
        new_config: SyncConfig,
    ) -> Result<(), SyncError> {
        if !ctx.is_admin() {
            return Err(SyncError::AccessDenied("sync:configure requires Admin role".into()));
        }

        new_config.save(&self.config_path)?;

        let old_config = self.config.read().await.clone();
        let creds_changed = old_config.turso_url != new_config.turso_url
            || old_config.turso_auth_token != new_config.turso_auth_token;

        // Reset the remote flag so ensure_remote will rebuild on next sync
        if creds_changed {
            self.is_remote.store(false, Ordering::Relaxed);

            // If switching to local-only, rebuild immediately as local
            if new_config.turso_url.is_empty() {
                {
                    let mut db = self.db.write().await;
                    let mut conn = self.conn.write().await;
                    let tmp = libsql::Builder::new_local(":memory:")
                        .build().await
                        .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;
                    *conn = tmp.connect().map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;
                    *db = tmp;
                }
                Self::clean_aux_files();
                let db_path = libsql_db_path();
                let new_db = libsql::Builder::new_local(db_path.to_string_lossy().as_ref())
                    .build().await
                    .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;
                let new_conn = new_db.connect()
                    .map_err(|e| SyncError::ConnectionFailed(e.to_string()))?;
                SchemaManager::init_schema(&new_conn).await?;
                let mut db = self.db.write().await;
                *db = new_db;
                let mut conn = self.conn.write().await;
                *conn = new_conn;
                log::info!("configure: switched to local-only mode");
            }
        }

        let mut config = self.config.write().await;
        *config = new_config;
        log::info!("configure: config updated");
        Ok(())
    }

    // ── Config helpers ────────────────────────────────────────────────

    pub fn change_tracker(&self) -> &Arc<ChangeTracker> {
        &self.change_tracker
    }

    pub async fn current_config(&self) -> SyncConfig {
        self.config.read().await.clone()
    }

    /// Update config in memory + disk without rebuilding the db.
    /// Used at startup to restore the original config while keeping local-only mode.
    pub async fn set_config_only(&self, new_config: SyncConfig) -> Result<(), SyncError> {
        new_config.save(&self.config_path)?;
        let mut config = self.config.write().await;
        *config = new_config;
        log::info!("set_config_only: config updated (no db rebuild)");
        Ok(())
    }

    // ── Auto-sync timer ───────────────────────────────────────────────

    pub async fn start_auto_sync(&self, _ctx: Arc<SecurityContext>) {
        self.stop_auto_sync().await;
        let config = self.config.read().await.clone();
        if !config.auto_sync_enabled || config.sync_interval_seconds == 0 {
            return;
        }
        let interval_secs = config.sync_interval_seconds;
        let tracker = self.change_tracker.clone();
        let handle = tokio::spawn(async move {
            let mut interval = tokio::time::interval(tokio::time::Duration::from_secs(interval_secs));
            interval.tick().await; // skip first immediate tick
            loop {
                interval.tick().await;
                if tracker.pending_count() > 0 {
                    log::info!("Auto-sync tick: {} pending changes", tracker.pending_count());
                }
            }
        });
        *self.auto_sync_handle.lock().await = Some(handle);
        log::info!("Auto-sync timer started (interval={}s)", interval_secs);
    }

    pub async fn stop_auto_sync(&self) {
        if let Some(h) = self.auto_sync_handle.lock().await.take() {
            h.abort();
            log::info!("Auto-sync timer stopped");
        }
    }

    // ── redb → LibSQL (push data) ─────────────────────────────────────

    async fn flush_redb_to_libsql(&self) -> Result<HashMap<String, usize>, SyncError> {
        use common::database::transactions::Transaction;
        use common::direct_access::repository_factory::read;

        let txn = Transaction::begin_read_transaction(&self.db_context)
            .map_err(|e| SyncError::ConnectionFailed(format!("redb read txn: {}", e)))?;
        let conn = self.conn.read().await;
        let mut counts: HashMap<String, usize> = HashMap::new();

        // Products
        {
            let repo = read::create_product_repository(&txn).map_err(|e| SyncError::ConnectionFailed(format!("product repo: {}", e)))?;
            let items = repo.get_all().map_err(|e| SyncError::ConnectionFailed(format!("redb products: {}", e)))?;
            let sql = ProductBridge::upsert_sql();
            let mut n = 0usize;
            for p in &items {
                let (ts, us) = (p.created_at.to_rfc3339(), p.updated_at.to_rfc3339());
                let st = format!("{:?}", p.status);
                let params = libsql::params![p.id, p.name.as_str(), p.reference.as_str(), p.description.as_str(), p.quantity, p.price_unit, st.as_str(), p.category, p.supplier, p.location, ts.as_str(), us.as_str()];
                if conn.execute(&sql, params).await.is_ok() { n += 1; }
            }
            counts.insert("products".into(), n);
        }
        // Categories
        {
            let repo = read::create_category_repository(&txn).map_err(|e| SyncError::ConnectionFailed(format!("category repo: {}", e)))?;
            let items = repo.get_all().map_err(|e| SyncError::ConnectionFailed(format!("redb categories: {}", e)))?;
            let sql = CategoryBridge::upsert_sql();
            let mut n = 0usize;
            for c in &items {
                let (ts, us) = (c.created_at.to_rfc3339(), c.updated_at.to_rfc3339());
                let params = libsql::params![c.id, c.name.as_str(), c.description.as_str(), c.parent_category, ts.as_str(), us.as_str()];
                if conn.execute(&sql, params).await.is_ok() { n += 1; }
            }
            counts.insert("categories".into(), n);
        }
        // Persons
        {
            let repo = read::create_person_repository(&txn).map_err(|e| SyncError::ConnectionFailed(format!("person repo: {}", e)))?;
            let items = repo.get_all().map_err(|e| SyncError::ConnectionFailed(format!("redb persons: {}", e)))?;
            let sql = PersonBridge::upsert_sql();
            let mut n = 0usize;
            for p in &items {
                let (ts, us) = (p.created_at.to_rfc3339(), p.updated_at.to_rfc3339());
                let r = format!("{:?}", p.role);
                let params = libsql::params![p.id, p.name.as_str(), r.as_str(), p.contact, ts.as_str(), us.as_str()];
                if conn.execute(&sql, params).await.is_ok() { n += 1; }
            }
            counts.insert("persons".into(), n);
        }
        // Contacts
        {
            let repo = read::create_contact_repository(&txn).map_err(|e| SyncError::ConnectionFailed(format!("contact repo: {}", e)))?;
            let items = repo.get_all().map_err(|e| SyncError::ConnectionFailed(format!("redb contacts: {}", e)))?;
            let sql = ContactBridge::upsert_sql();
            let mut n = 0usize;
            for c in &items {
                let (ts, us) = (c.created_at.to_rfc3339(), c.updated_at.to_rfc3339());
                let params = libsql::params![c.id, c.phone.as_str(), c.email.as_str(), ts.as_str(), us.as_str()];
                if conn.execute(&sql, params).await.is_ok() { n += 1; }
            }
            counts.insert("contacts".into(), n);
        }
        // Deals
        {
            let repo = read::create_deal_repository(&txn).map_err(|e| SyncError::ConnectionFailed(format!("deal repo: {}", e)))?;
            let items = repo.get_all().map_err(|e| SyncError::ConnectionFailed(format!("redb deals: {}", e)))?;
            let sql = DealBridge::upsert_sql();
            let mut n = 0usize;
            for d in &items {
                let (ts, us) = (d.created_at.to_rfc3339(), d.updated_at.to_rfc3339());
                let (sd, ed) = (d.start_date.to_rfc3339(), d.end_date.to_rfc3339());
                let (f, s) = (format!("{:?}", d.frequency), format!("{:?}", d.status));
                let params = libsql::params![d.id, d.title.as_str(), d.description.as_str(), d.unit_cost, d.total_value, sd.as_str(), ed.as_str(), f.as_str(), s.as_str(), d.product, d.supplier, d.manager, ts.as_str(), us.as_str()];
                if conn.execute(&sql, params).await.is_ok() { n += 1; }
            }
            counts.insert("deals".into(), n);
        }
        // Locations
        {
            let repo = read::create_location_repository(&txn).map_err(|e| SyncError::ConnectionFailed(format!("location repo: {}", e)))?;
            let items = repo.get_all().map_err(|e| SyncError::ConnectionFailed(format!("redb locations: {}", e)))?;
            let sql = LocationBridge::upsert_sql();
            let mut n = 0usize;
            for l in &items {
                let (ts, us) = (l.created_at.to_rfc3339(), l.updated_at.to_rfc3339());
                let params = libsql::params![l.id, l.name.as_str(), l.address.as_str(), l.latitude, l.longitude, l.capacity, l.manager, ts.as_str(), us.as_str()];
                if conn.execute(&sql, params).await.is_ok() { n += 1; }
            }
            counts.insert("locations".into(), n);
        }
        // Users
        {
            let repo = read::create_user_repository(&txn).map_err(|e| SyncError::ConnectionFailed(format!("user repo: {}", e)))?;
            let items = repo.get_all().map_err(|e| SyncError::ConnectionFailed(format!("redb users: {}", e)))?;
            let sql = UserBridge::upsert_sql();
            let mut n = 0usize;
            for u in &items {
                let (ts, us) = (u.created_at.to_rfc3339(), u.updated_at.to_rfc3339());
                let r = format!("{:?}", u.role);
                let params = libsql::params![u.id, u.username.as_str(), u.password_hash.as_str(), u.display_name.as_str(), r.as_str(), u.is_active as i64, u.person, ts.as_str(), us.as_str()];
                if conn.execute(&sql, params).await.is_ok() { n += 1; }
            }
            counts.insert("users".into(), n);
        }
        // Sessions
        {
            let repo = read::create_session_repository(&txn).map_err(|e| SyncError::ConnectionFailed(format!("session repo: {}", e)))?;
            let items = repo.get_all().map_err(|e| SyncError::ConnectionFailed(format!("redb sessions: {}", e)))?;
            let sql = SessionBridge::upsert_sql();
            let mut n = 0usize;
            for s in &items {
                let (ts, us, ea) = (s.created_at.to_rfc3339(), s.updated_at.to_rfc3339(), s.expires_at.to_rfc3339());
                let params = libsql::params![s.id, s.token.as_str(), ea.as_str(), ts.as_str(), us.as_str()];
                if conn.execute(&sql, params).await.is_ok() { n += 1; }
            }
            counts.insert("sessions".into(), n);
        }
        // StockMovements
        {
            let repo = read::create_stock_movement_repository(&txn).map_err(|e| SyncError::ConnectionFailed(format!("stock_movement repo: {}", e)))?;
            let items = repo.get_all().map_err(|e| SyncError::ConnectionFailed(format!("redb stock_movements: {}", e)))?;
            let sql = StockMovementBridge::upsert_sql();
            let mut n = 0usize;
            for m in &items {
                let (ts, us) = (m.created_at.to_rfc3339(), m.updated_at.to_rfc3339());
                let mt = format!("{:?}", m.movement_type);
                let params = libsql::params![m.id, mt.as_str(), m.quantity, m.note.as_str(), m.product, m.from_location, m.to_location, m.performed_by, ts.as_str(), us.as_str()];
                if conn.execute(&sql, params).await.is_ok() { n += 1; }
            }
            counts.insert("stock_movements".into(), n);
        }
        // BudgetEntries
        {
            let repo = read::create_budget_entry_repository(&txn).map_err(|e| SyncError::ConnectionFailed(format!("budget_entry repo: {}", e)))?;
            let items = repo.get_all().map_err(|e| SyncError::ConnectionFailed(format!("redb budget_entries: {}", e)))?;
            let sql = BudgetEntryBridge::upsert_sql();
            let mut n = 0usize;
            for b in &items {
                let (ts, us, ed) = (b.created_at.to_rfc3339(), b.updated_at.to_rfc3339(), b.entry_date.to_rfc3339());
                let et = format!("{:?}", b.entry_type);
                let params = libsql::params![b.id, et.as_str(), b.amount, b.description.as_str(), ed.as_str(), b.product, b.deal, b.recorded_by, ts.as_str(), us.as_str()];
                if conn.execute(&sql, params).await.is_ok() { n += 1; }
            }
            counts.insert("budget_entries".into(), n);
        }

        log::info!("flush_redb_to_libsql: complete ({:?})", counts);
        Ok(counts)
    }

    // ── LibSQL → redb (pull data) ─────────────────────────────────────

    async fn import_libsql_to_redb(
        &self,
        conn: &libsql::Connection,
        counts: &mut HashMap<String, usize>,
    ) -> Result<(), SyncError> {
        use common::database::transactions::Transaction;
        use common::direct_access::repository_factory::write;
        use common::event::EventBuffer;

        fn parse_dt(s: &str) -> chrono::DateTime<chrono::Utc> {
            chrono::DateTime::parse_from_rfc3339(s)
                .map(|d| d.with_timezone(&chrono::Utc))
                .unwrap_or_else(|_| chrono::Utc::now())
        }

        macro_rules! import_entity {
            ($name:expr, $sql:expr, $repo_fn:path, $row:ident => $entity:expr) => {{
                let mut rows = conn.query($sql, ()).await.map_err(|e| SyncError::LibSql(e))?;
                let mut n = 0usize;
                while let Ok(Some($row)) = rows.next().await {
                    let entity = $entity;
                    let mut txn = Transaction::begin_write_transaction(&self.db_context)
                        .map_err(|e| SyncError::ConnectionFailed(format!("redb write: {}", e)))?;
                    {
                        let mut eb = EventBuffer::new();
                        let mut repo = $repo_fn(&txn)
                            .map_err(|e| SyncError::ConnectionFailed(format!("{} repo: {}", $name, e)))?;
                        // Try update_with_relationships first (sets junction tables),
                        // fall back to plain update (constraint violations on shared FKs),
                        // then try create if entity doesn't exist yet.
                        if repo.update_with_relationships(&mut eb, &entity).is_err() {
                            if repo.update(&mut eb, &entity).is_err() {
                                let _ = repo.create(&mut eb, &entity, 1, -1);
                            }
                        }
                    }
                    txn.commit().map_err(|e| SyncError::ConnectionFailed(format!("redb commit: {}", e)))?;
                    n += 1;
                }
                if n > 0 { log::info!("hydrate: imported {} {}", n, $name); }
                counts.insert($name.to_string(), n);
            }};
        }

        // Products
        import_entity!("products",
            "SELECT id, name, reference, description, quantity, price_unit, status, category_id, supplier_id, location_id, created_at, updated_at FROM products WHERE deleted_at IS NULL",
            write::create_product_repository,
            row => {
                let status = match row.get::<String>(6).unwrap_or_default().as_str() {
                    "OutOfStock" => common::entities::ProductStatus::OutOfStock,
                    "Discontinued" => common::entities::ProductStatus::Discontinued,
                    "Reserved" => common::entities::ProductStatus::Reserved,
                    _ => common::entities::ProductStatus::Available,
                };
                common::entities::Product {
                    id: row.get::<i64>(0).unwrap_or(0),
                    name: row.get::<String>(1).unwrap_or_default(),
                    reference: row.get::<String>(2).unwrap_or_default(),
                    description: row.get::<String>(3).unwrap_or_default(),
                    quantity: row.get::<i64>(4).unwrap_or(0),
                    price_unit: row.get::<f64>(5).unwrap_or(0.0),
                    status,
                    category: row.get::<i64>(7).ok(),
                    supplier: row.get::<i64>(8).ok(),
                    location: row.get::<i64>(9).ok(),
                    created_at: parse_dt(&row.get::<String>(10).unwrap_or_default()),
                    updated_at: parse_dt(&row.get::<String>(11).unwrap_or_default()),
                }
            }
        );

        // Categories
        import_entity!("categories",
            "SELECT id, name, description, parent_category_id, created_at, updated_at FROM categories WHERE deleted_at IS NULL",
            write::create_category_repository,
            row => common::entities::Category {
                id: row.get::<i64>(0).unwrap_or(0),
                name: row.get::<String>(1).unwrap_or_default(),
                description: row.get::<String>(2).unwrap_or_default(),
                parent_category: row.get::<i64>(3).ok(),
                subcategories: vec![],
                created_at: parse_dt(&row.get::<String>(4).unwrap_or_default()),
                updated_at: parse_dt(&row.get::<String>(5).unwrap_or_default()),
            }
        );

        // Contacts (before Persons so contact IDs exist)
        import_entity!("contacts",
            "SELECT id, phone, email, created_at, updated_at FROM contacts WHERE deleted_at IS NULL",
            write::create_contact_repository,
            row => common::entities::Contact {
                id: row.get::<i64>(0).unwrap_or(0),
                phone: row.get::<String>(1).unwrap_or_default(),
                email: row.get::<String>(2).unwrap_or_default(),
                created_at: parse_dt(&row.get::<String>(3).unwrap_or_default()),
                updated_at: parse_dt(&row.get::<String>(4).unwrap_or_default()),
            }
        );

        // Persons
        import_entity!("persons",
            "SELECT id, name, role, contact_id, created_at, updated_at FROM persons WHERE deleted_at IS NULL",
            write::create_person_repository,
            row => {
                let role = match row.get::<String>(2).unwrap_or_default().as_str() {
                    "Supplier" => common::entities::PersonRole::Supplier,
                    _ => common::entities::PersonRole::Manager,
                };
                common::entities::Person {
                    id: row.get::<i64>(0).unwrap_or(0),
                    name: row.get::<String>(1).unwrap_or_default(),
                    role,
                    contact: row.get::<i64>(3).ok(),
                    created_at: parse_dt(&row.get::<String>(4).unwrap_or_default()),
                    updated_at: parse_dt(&row.get::<String>(5).unwrap_or_default()),
                }
            }
        );

        // Deals
        import_entity!("deals",
            "SELECT id, title, description, unit_cost, total_value, start_date, end_date, frequency, status, product_id, supplier_id, manager_id, created_at, updated_at FROM deals WHERE deleted_at IS NULL",
            write::create_deal_repository,
            row => {
                let freq = match row.get::<String>(7).unwrap_or_default().as_str() {
                    "Weekly" => common::entities::DealFrequency::Weekly,
                    "Monthly" => common::entities::DealFrequency::Monthly,
                    "Quarterly" => common::entities::DealFrequency::Quarterly,
                    "Yearly" => common::entities::DealFrequency::Yearly,
                    _ => common::entities::DealFrequency::OneTime,
                };
                let status = match row.get::<String>(8).unwrap_or_default().as_str() {
                    "Active" => common::entities::DealStatus::Active,
                    "Completed" => common::entities::DealStatus::Completed,
                    "Cancelled" => common::entities::DealStatus::Cancelled,
                    _ => common::entities::DealStatus::Draft,
                };
                common::entities::Deal {
                    id: row.get::<i64>(0).unwrap_or(0),
                    title: row.get::<String>(1).unwrap_or_default(),
                    description: row.get::<String>(2).unwrap_or_default(),
                    unit_cost: row.get::<f64>(3).unwrap_or(0.0),
                    total_value: row.get::<f64>(4).unwrap_or(0.0),
                    start_date: parse_dt(&row.get::<String>(5).unwrap_or_default()),
                    end_date: parse_dt(&row.get::<String>(6).unwrap_or_default()),
                    frequency: freq, status,
                    product: row.get::<i64>(9).ok(),
                    supplier: row.get::<i64>(10).ok(),
                    manager: row.get::<i64>(11).ok(),
                    created_at: parse_dt(&row.get::<String>(12).unwrap_or_default()),
                    updated_at: parse_dt(&row.get::<String>(13).unwrap_or_default()),
                }
            }
        );

        // Locations
        import_entity!("locations",
            "SELECT id, name, address, latitude, longitude, capacity, manager_id, created_at, updated_at FROM locations WHERE deleted_at IS NULL",
            write::create_location_repository,
            row => common::entities::Location {
                id: row.get::<i64>(0).unwrap_or(0),
                name: row.get::<String>(1).unwrap_or_default(),
                address: row.get::<String>(2).unwrap_or_default(),
                latitude: row.get::<f64>(3).unwrap_or(0.0),
                longitude: row.get::<f64>(4).unwrap_or(0.0),
                capacity: row.get::<i64>(5).unwrap_or(0),
                manager: row.get::<i64>(6).ok(),
                created_at: parse_dt(&row.get::<String>(7).unwrap_or_default()),
                updated_at: parse_dt(&row.get::<String>(8).unwrap_or_default()),
            }
        );

        // Users
        import_entity!("users",
            "SELECT id, username, password_hash, display_name, role, is_active, person_id, created_at, updated_at FROM users WHERE deleted_at IS NULL",
            write::create_user_repository,
            row => {
                let role = match row.get::<String>(4).unwrap_or_default().as_str() {
                    "Manager" => common::entities::UserRole::Manager,
                    "Operator" => common::entities::UserRole::Operator,
                    "Viewer" => common::entities::UserRole::Viewer,
                    _ => common::entities::UserRole::Admin,
                };
                common::entities::User {
                    id: row.get::<i64>(0).unwrap_or(0),
                    username: row.get::<String>(1).unwrap_or_default(),
                    password_hash: row.get::<String>(2).unwrap_or_default(),
                    display_name: row.get::<String>(3).unwrap_or_default(),
                    role,
                    is_active: row.get::<i64>(5).unwrap_or(1) != 0,
                    person: row.get::<i64>(6).ok(),
                    session: None,
                    created_at: parse_dt(&row.get::<String>(7).unwrap_or_default()),
                    updated_at: parse_dt(&row.get::<String>(8).unwrap_or_default()),
                }
            }
        );

        // Sessions
        import_entity!("sessions",
            "SELECT id, token, expires_at, created_at, updated_at FROM sessions WHERE deleted_at IS NULL",
            write::create_session_repository,
            row => common::entities::Session {
                id: row.get::<i64>(0).unwrap_or(0),
                token: row.get::<String>(1).unwrap_or_default(),
                expires_at: parse_dt(&row.get::<String>(2).unwrap_or_default()),
                created_at: parse_dt(&row.get::<String>(3).unwrap_or_default()),
                updated_at: parse_dt(&row.get::<String>(4).unwrap_or_default()),
            }
        );

        // StockMovements
        import_entity!("stock_movements",
            "SELECT id, movement_type, quantity, note, product_id, from_location_id, to_location_id, performed_by_id, created_at, updated_at FROM stock_movements WHERE deleted_at IS NULL",
            write::create_stock_movement_repository,
            row => {
                let mt = match row.get::<String>(1).unwrap_or_default().as_str() {
                    "Outbound" => common::entities::MovementType::Outbound,
                    "Transfer" => common::entities::MovementType::Transfer,
                    "Adjustment" => common::entities::MovementType::Adjustment,
                    "Return" => common::entities::MovementType::Return,
                    _ => common::entities::MovementType::Inbound,
                };
                common::entities::StockMovement {
                    id: row.get::<i64>(0).unwrap_or(0),
                    movement_type: mt,
                    quantity: row.get::<i64>(2).unwrap_or(0),
                    note: row.get::<String>(3).unwrap_or_default(),
                    product: row.get::<i64>(4).ok(),
                    from_location: row.get::<i64>(5).ok(),
                    to_location: row.get::<i64>(6).ok(),
                    performed_by: row.get::<i64>(7).ok(),
                    created_at: parse_dt(&row.get::<String>(8).unwrap_or_default()),
                    updated_at: parse_dt(&row.get::<String>(9).unwrap_or_default()),
                }
            }
        );

        // BudgetEntries
        import_entity!("budget_entries",
            "SELECT id, entry_type, amount, description, entry_date, product_id, deal_id, recorded_by_id, created_at, updated_at FROM budget_entries WHERE deleted_at IS NULL",
            write::create_budget_entry_repository,
            row => {
                let et = match row.get::<String>(1).unwrap_or_default().as_str() {
                    "Sale" => common::entities::BudgetEntryType::Sale,
                    "Expense" => common::entities::BudgetEntryType::Expense,
                    "Forecast" => common::entities::BudgetEntryType::Forecast,
                    _ => common::entities::BudgetEntryType::Purchase,
                };
                common::entities::BudgetEntry {
                    id: row.get::<i64>(0).unwrap_or(0),
                    entry_type: et,
                    amount: row.get::<f64>(2).unwrap_or(0.0),
                    description: row.get::<String>(3).unwrap_or_default(),
                    entry_date: parse_dt(&row.get::<String>(4).unwrap_or_default()),
                    product: row.get::<i64>(5).ok(),
                    deal: row.get::<i64>(6).ok(),
                    recorded_by: row.get::<i64>(7).ok(),
                    created_at: parse_dt(&row.get::<String>(8).unwrap_or_default()),
                    updated_at: parse_dt(&row.get::<String>(9).unwrap_or_default()),
                }
            }
        );

        log::info!("import_libsql_to_redb: complete ({:?})", counts);
        Ok(())
    }
}
