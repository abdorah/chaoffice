#[derive(Debug, thiserror::Error)]
pub enum ImportError {
    #[error("File not found: {path}")]
    FileNotFound { path: String },
    #[error("File not readable: {path}")]
    FileNotReadable { path: String },
    #[error("Malformed CSV: {details}")]
    MalformedCsv { details: String },
    #[error("Invalid JSON: {details}")]
    InvalidJson { details: String },
    #[error("Row {row}: missing required field '{field}'")]
    MissingField { row: usize, field: String },
    #[error("Row {row}: unknown category '{name}'")]
    UnknownCategory { row: usize, name: String },
    #[error("Row {row}: unknown supplier '{name}'")]
    UnknownSupplier { row: usize, name: String },
    #[error("Row {row}: unknown location '{name}'")]
    UnknownLocation { row: usize, name: String },
    #[error("Row {row}: invalid status '{value}'")]
    InvalidStatus { row: usize, value: String },
}

#[derive(Debug, thiserror::Error)]
pub enum ExportError {
    #[error("Output path not writable: {path}")]
    NotWritable { path: String },
    #[error("Serialization error: {details}")]
    SerializationError { details: String },
}

#[derive(Debug, thiserror::Error)]
pub enum TransferError {
    #[error("Product not found: {id}")]
    ProductNotFound { id: i64 },
    #[error("Location not found: {id}")]
    LocationNotFound { id: i64 },
    #[error("Insufficient stock: available {available}, requested {requested}")]
    InsufficientStock { available: i64, requested: i64 },
    #[error("Transfer quantity must be positive")]
    InvalidQuantity,
    #[error("Source and destination locations must differ")]
    SameLocation,
}

/// Top-level error wrapping all inventory management errors.
#[derive(Debug, thiserror::Error)]
pub enum InventoryError {
    #[error(transparent)]
    Import(#[from] ImportError),
    #[error(transparent)]
    Export(#[from] ExportError),
    #[error(transparent)]
    Transfer(#[from] TransferError),
    #[error("Internal error: {0}")]
    Internal(String),
}
