#[derive(Debug, thiserror::Error)]
pub enum SyncError {
    #[error("LibSQL error: {0}")]
    LibSql(#[from] libsql::Error),

    #[error("Connection failed: {0}")]
    ConnectionFailed(String),

    #[error("Schema initialization failed: {0}")]
    SchemaInit(String),

    #[error("Entity serialization error: {entity_type} id={entity_id}: {message}")]
    Serialization {
        entity_type: String,
        entity_id: u64,
        message: String,
    },

    #[error("Config error: {0}")]
    Config(String),

    #[error("Redb access error: {0}")]
    RedbAccess(String),

    #[error("Access denied: {0}")]
    AccessDenied(String),

    #[error("Sync offline: remote unreachable")]
    Offline,
}

impl From<serde_json::Error> for SyncError {
    fn from(e: serde_json::Error) -> Self {
        SyncError::Config(e.to_string())
    }
}

impl From<std::io::Error> for SyncError {
    fn from(e: std::io::Error) -> Self {
        SyncError::Config(e.to_string())
    }
}
