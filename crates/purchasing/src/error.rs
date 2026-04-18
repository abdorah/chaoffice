use common::entities::DealStatus;
use inventory_security::AuthError;

#[derive(Debug, thiserror::Error)]
pub enum PurchasingError {
    #[error("Product not found: {id}")]
    ProductNotFound { id: i64 },
    #[error("Supplier Person not found: {id}")]
    SupplierNotFound { id: i64 },
    #[error("Manager Person not found: {id}")]
    ManagerNotFound { id: i64 },
    #[error("Person {id} does not have role Supplier")]
    NotASupplier { id: i64 },
    #[error("Person {id} does not have role Manager")]
    NotAManager { id: i64 },
    #[error("Title is required")]
    EmptyTitle,
    #[error("unit_cost must be non-negative")]
    NegativeUnitCost,
    #[error("total_value must be non-negative")]
    NegativeTotalValue,
    #[error("end_date must not be earlier than start_date")]
    InvalidDateRange,
    #[error("Invalid status transition from {from:?} to {to:?}")]
    InvalidStatusTransition { from: DealStatus, to: DealStatus },
    #[error(transparent)]
    Auth(#[from] AuthError),
    #[error("Internal error: {0}")]
    Internal(String),
}
