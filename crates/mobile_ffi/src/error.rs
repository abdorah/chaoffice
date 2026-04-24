use common::error::RepositoryError;
use inventory_security::AuthError;
use inventory_sync::error::SyncError;
use reporting::error::ReportError;

/// Unified error type exposed across the FFI boundary.
///
/// Every fallible FFI function returns `Result<T, FfiError>`.
/// The `From<anyhow::Error>` impl inspects the error chain with
/// `downcast_ref` to pick the most specific variant; anything
/// unrecognised falls through to `Unknown`.
#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum FfiError {
    #[error("Authentication error: {message}")]
    AuthenticationError { message: String },
    #[error("Database error: {message}")]
    DatabaseError { message: String },
    #[error("Sync error: {message}")]
    SyncError { message: String },
    #[error("Report error: {message}")]
    ReportError { message: String },
    #[error("Validation error: {message}")]
    ValidationError { message: String },
    #[error("Unknown error: {message}")]
    Unknown { message: String },
}

impl From<anyhow::Error> for FfiError {
    fn from(err: anyhow::Error) -> Self {
        if let Some(e) = err.downcast_ref::<AuthError>() {
            return FfiError::AuthenticationError {
                message: e.to_string(),
            };
        }
        if let Some(e) = err.downcast_ref::<RepositoryError>() {
            return FfiError::DatabaseError {
                message: e.to_string(),
            };
        }
        if let Some(e) = err.downcast_ref::<SyncError>() {
            return FfiError::SyncError {
                message: e.to_string(),
            };
        }
        if let Some(e) = err.downcast_ref::<ReportError>() {
            return FfiError::ReportError {
                message: e.to_string(),
            };
        }
        // Validation-style errors from domain crates
        if let Some(e) = err.downcast_ref::<stock_tracking::error::StockTrackingError>() {
            return FfiError::ValidationError {
                message: e.to_string(),
            };
        }
        if let Some(e) = err.downcast_ref::<purchasing::error::PurchasingError>() {
            return FfiError::ValidationError {
                message: e.to_string(),
            };
        }
        if let Some(e) = err.downcast_ref::<budget_finance::BudgetError>() {
            return FfiError::ValidationError {
                message: e.to_string(),
            };
        }

        FfiError::Unknown {
            message: format!("{err:#}"),
        }
    }
}
