// Mobile FFI crate - UniFFI bindings for the Android app

uniffi::setup_scaffolding!();

pub mod auth;
pub mod budget;
pub mod categories;
pub mod contacts;
pub mod deals;
pub mod dtos;
pub mod error;
pub mod locations;
pub mod persons;
pub mod products;
pub mod purchasing;
pub mod reporting;
pub mod stock;
pub mod sync;
pub mod users;

pub use dtos::*;
pub use error::FfiError;

use common::database::db_context::DbContext;
use common::event::EventHub;
use common::long_operation::LongOperationManager;
use common::undo_redo::UndoRedoManager;
use frontend::AppContext;
use std::sync::{Arc, Mutex, OnceLock};

/// Holds the initialized backend state for the lifetime of the app.
struct AppState {
    app_context: AppContext,
    runtime: tokio::runtime::Runtime,
}

/// Global singleton – written once by `mobile_init`, read by every FFI call.
static STATE: OnceLock<AppState> = OnceLock::new();

/// Returns a reference to the global `AppState`, or an error if `mobile_init`
/// has not been called yet.
pub(crate) fn get_state() -> Result<&'static AppState, FfiError> {
    STATE.get().ok_or_else(|| FfiError::Unknown {
        message: "mobile_init has not been called".into(),
    })
}

/// Convenience accessor used by every FFI module and integration tests.
pub fn get_app_context() -> Result<&'static AppContext, FfiError> {
    get_state().map(|s| &s.app_context)
}

/// Convenience accessor for the tokio runtime (needed for async sync ops).
pub(crate) fn get_runtime() -> Result<&'static tokio::runtime::Runtime, FfiError> {
    get_state().map(|s| &s.runtime)
}

/// Initialise the backend with a file-backed database at `db_path`.
///
/// Must be called exactly once before any other FFI function.
/// Calling it a second time is a no-op (OnceLock ignores the value).
#[uniffi::export]
pub fn mobile_init(db_path: String) -> Result<(), FfiError> {
    let db_context = DbContext::new_with_path(&db_path).map_err(|e| FfiError::DatabaseError {
        message: e.to_string(),
    })?;

    let event_hub = Arc::new(EventHub::new());
    let quit_signal = Arc::new(std::sync::atomic::AtomicBool::new(false));
    let undo_redo_manager = Arc::new(Mutex::new(UndoRedoManager::new()));
    let long_operation_manager = Arc::new(Mutex::new(LongOperationManager::new()));

    // Wire the event hub into the long-operation manager, same as AppContext::new().
    {
        let mut lom = long_operation_manager.lock().unwrap();
        lom.set_event_hub(&event_hub);
    }

    let app_context = AppContext {
        db_context,
        event_hub,
        quit_signal,
        undo_redo_manager,
        long_operation_manager,
    };

    let runtime = tokio::runtime::Runtime::new().map_err(|e| FfiError::Unknown {
        message: format!("failed to create tokio runtime: {e}"),
    })?;

    // OnceLock::set returns Err if already initialised – we silently ignore that.
    let _ = STATE.set(AppState {
        app_context,
        runtime,
    });

    Ok(())
}

/// Cleanly shut down the backend, signalling the event hub to stop.
#[uniffi::export]
pub fn mobile_shutdown() -> Result<(), FfiError> {
    let state = get_state()?;
    state.app_context.shutdown();
    Ok(())
}

/// Bootstrap the database on first launch: creates the Root entity and
/// a default admin user. Safe to call multiple times — skips if users
/// already exist.
///
/// Default credentials: username `admin`, password `Admin1234`
#[uniffi::export]
pub fn mobile_bootstrap() -> Result<(), FfiError> {
    let ctx = get_app_context()?;

    // Check if users already exist — if so, skip bootstrap
    let users = frontend::commands::user_commands::get_all_user(ctx).map_err(FfiError::from)?;
    if !users.is_empty() {
        return Ok(());
    }

    // Create Root entity (id = 1) — required by all ownership chains
    let root_dto = direct_access::CreateRootDto::default();
    frontend::commands::root_commands::create_orphan_root(ctx, &root_dto)
        .map_err(FfiError::from)?;

    // Create default admin user
    let create_dto = user_management::dtos::CreateUserDto {
        username: "admin".to_string(),
        password: "Admin1234".to_string(),
        display_name: "Administrator".to_string(),
        role: user_management::dtos::CreateUserRole::Admin,
        person_id: 0,
    };
    frontend::commands::user_management_commands::create_user(ctx, &create_dto)
        .map_err(FfiError::from)?;

    Ok(())
}
