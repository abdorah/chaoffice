//! Sync FFI functions: push, pull, get/set config.
//!
//! The [`SyncEngine`] is created lazily on first use and cached for the
//! lifetime of the app.  All async engine methods are driven through the
//! tokio [`Runtime`] stored in [`AppState`] via `runtime.block_on()`.

use std::path::PathBuf;
use std::sync::{Arc, Mutex, OnceLock};

use inventory_sync::config::SyncConfig;
use inventory_sync::{ChangeTracker, SyncEngine};

use crate::dtos::{FfiSyncConfig, FfiSyncResult};
use crate::error::FfiError;
use crate::{get_app_context, get_runtime};

use common::entities::UserRole;
use frontend::commands::{session_commands, user_commands};
use inventory_security::SecurityContext;

/// Lazily-initialised sync engine singleton.
static SYNC_ENGINE: OnceLock<SyncEngine> = OnceLock::new();

/// Guard that prevents concurrent sync operations (push/pull).
/// `try_lock()` is used so that a second caller gets an immediate error
/// instead of blocking (which would deadlock the shared tokio runtime).
pub static SYNC_GUARD: Mutex<()> = Mutex::new(());

/// Build a [`SecurityContext`] from a session token by resolving the owning
/// user's ID and role through the session and user tables.
fn build_security_context(session_token: &str) -> Result<SecurityContext, FfiError> {
    let ctx = get_app_context()?;

    let sessions = session_commands::get_all_session(ctx).map_err(FfiError::from)?;
    let session = sessions
        .iter()
        .find(|s| s.token == session_token)
        .ok_or_else(|| FfiError::AuthenticationError {
            message: "Session not found for the provided token".into(),
        })?;

    let users = user_commands::get_all_user(ctx).map_err(FfiError::from)?;
    let user = users
        .iter()
        .find(|u| u.session == Some(session.id))
        .ok_or_else(|| FfiError::AuthenticationError {
            message: "No user associated with the provided session token".into(),
        })?;

    let role: UserRole = user.role.clone();
    Ok(SecurityContext::from_user(
        user.id,
        role,
        session_token.to_string(),
    ))
}

/// Get or create the [`SyncEngine`] singleton.
///
/// The engine is created with a default config and a fresh [`ChangeTracker`].
/// The config file is stored next to the database in the app's data directory.
fn get_or_init_sync_engine() -> Result<&'static SyncEngine, FfiError> {
    if let Some(engine) = SYNC_ENGINE.get() {
        return Ok(engine);
    }

    let runtime = get_runtime()?;
    let ctx = get_app_context()?;

    // Derive a config path next to the database.
    let config_path = PathBuf::from("sync_config.json");
    let config = SyncConfig::load(&config_path).unwrap_or_default();

    let change_tracker = Arc::new(ChangeTracker::new());
    let db_context = ctx.db_context.clone();

    let engine = runtime
        .block_on(SyncEngine::new(
            config,
            config_path,
            change_tracker,
            db_context,
        ))
        .map_err(|e| FfiError::SyncError {
            message: e.to_string(),
        })?;

    // OnceLock::set returns Err if already initialised – ignore the race.
    let _ = SYNC_ENGINE.set(engine);
    Ok(SYNC_ENGINE.get().unwrap())
}

/// Push local changes to the remote TursoDB instance.
///
/// Resolves the session token to a [`SecurityContext`], then delegates to
/// [`SyncEngine::dehydrate`] which flushes redb → LibSQL → TursoDB.
///
/// Returns [`FfiError::SyncError`] immediately if another sync operation is
/// already in progress (concurrent sync prevention per Requirement 14.5).
#[uniffi::export]
pub fn mobile_sync_push(session_token: String) -> Result<FfiSyncResult, FfiError> {
    let _guard = SYNC_GUARD.try_lock().map_err(|_| FfiError::SyncError {
        message: "A sync operation is already in progress".into(),
    })?;

    let engine = get_or_init_sync_engine()?;
    let runtime = get_runtime()?;
    let security_context = build_security_context(&session_token)?;

    let result = runtime
        .block_on(engine.dehydrate(&security_context))
        .map_err(|e| FfiError::SyncError {
            message: e.to_string(),
        })?;

    Ok(FfiSyncResult {
        entities_pushed: result.entities_written.values().sum::<usize>() as u64,
        entities_pulled: 0,
        conflicts: 0,
    })
}

/// Pull remote changes from TursoDB into the local database.
///
/// Resolves the session token to a [`SecurityContext`], then delegates to
/// [`SyncEngine::hydrate`] which pulls TursoDB → LibSQL → redb.
///
/// Returns [`FfiError::SyncError`] immediately if another sync operation is
/// already in progress (concurrent sync prevention per Requirement 14.5).
#[uniffi::export]
pub fn mobile_sync_pull(session_token: String) -> Result<FfiSyncResult, FfiError> {
    let _guard = SYNC_GUARD.try_lock().map_err(|_| FfiError::SyncError {
        message: "A sync operation is already in progress".into(),
    })?;

    let engine = get_or_init_sync_engine()?;
    let runtime = get_runtime()?;
    let security_context = build_security_context(&session_token)?;

    let result = runtime
        .block_on(engine.hydrate(&security_context))
        .map_err(|e| FfiError::SyncError {
            message: e.to_string(),
        })?;

    Ok(FfiSyncResult {
        entities_pushed: 0,
        entities_pulled: result.entities_loaded.values().sum::<usize>() as u64,
        conflicts: 0,
    })
}

/// Retrieve the current sync configuration.
#[uniffi::export]
pub fn mobile_get_sync_config() -> Result<FfiSyncConfig, FfiError> {
    let engine = get_or_init_sync_engine()?;
    let runtime = get_runtime()?;

    let config = runtime.block_on(engine.current_config());
    Ok(config.into())
}

/// Persist a new sync configuration.
///
/// Updates the in-memory config and writes it to the config file on disk.
/// Does **not** rebuild the remote database connection — that happens
/// lazily on the next push/pull.
#[uniffi::export]
pub fn mobile_set_sync_config(config: FfiSyncConfig) -> Result<(), FfiError> {
    let engine = get_or_init_sync_engine()?;
    let runtime = get_runtime()?;

    let sync_config: SyncConfig = config.into();
    runtime
        .block_on(engine.set_config_only(sync_config))
        .map_err(|e| FfiError::SyncError {
            message: e.to_string(),
        })?;

    Ok(())
}
