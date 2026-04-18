use inventory_security::AuthError;

#[derive(Debug, thiserror::Error)]
pub enum StockTrackingError {
    #[error("Product not found: {id}")]
    ProductNotFound { id: i64 },
    #[error("Location not found: {id}")]
    LocationNotFound { id: i64 },
    #[error("Insufficient stock: available {available}, requested {requested}")]
    InsufficientStock { available: i64, requested: i64 },
    #[error("Movement quantity must not be zero")]
    ZeroQuantity,
    #[error("Outbound/Transfer quantity must be positive")]
    NegativeQuantity,
    #[error("Inbound movement requires a to_location")]
    MissingToLocation,
    #[error("Outbound movement requires a from_location")]
    MissingFromLocation,
    #[error("Transfer requires both from_location and to_location")]
    MissingTransferLocations,
    #[error("Transfer source and destination must differ")]
    SameLocation,
    #[error("Adjustment would result in negative quantity: current {current}, adjustment {adjustment}")]
    AdjustmentUnderflow { current: i64, adjustment: i64 },
    #[error(transparent)]
    Auth(#[from] AuthError),
    #[error("Internal error: {0}")]
    Internal(String),
}
