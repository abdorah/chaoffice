use thiserror::Error;

use crate::models::Money;

#[derive(Error, Debug)]
pub enum AppError {
    #[error("Authentication failed: {message}")]
    Authentication { message: String },

    #[error("Authorization denied: role {actual_role} cannot access {resource}")]
    Authorization {
        actual_role: String,
        resource: String,
    },

    #[error("Not found: {entity_type} with id {entity_id}")]
    NotFound {
        entity_type: String,
        entity_id: String,
    },

    #[error("Insufficient stock: {material_name} has {available}, requested {requested}")]
    InsufficientStock {
        material_name: String,
        available: i64,
        requested: i64,
    },

    #[error("Insufficient funds: {wallet_name} has {available}, requested {requested}")]
    InsufficientFunds {
        wallet_name: String,
        available: Money,
        requested: Money,
    },

    #[error("Validation error: {field} — {message}")]
    Validation { field: String, message: String },

    #[error("Duplicate: {field} = {value} already exists")]
    Duplicate { field: String, value: String },

    #[error("Deletion blocked: {entity} — {reason}")]
    DeletionBlocked { entity: String, reason: String },

    #[error("Sync conflict: {entity_type}/{entity_id}")]
    SyncConflict {
        entity_type: String,
        entity_id: String,
    },

    #[error("Network error: {0}")]
    Network(#[from] reqwest::Error),

    #[error("Database error: {0}")]
    Database(#[from] sqlx::Error),

    #[error("Serialization error: {0}")]
    Serialization(String),

    #[error("Unknown error: {0}")]
    Unknown(String),
}

pub type AppResult<T> = Result<T, AppError>;
