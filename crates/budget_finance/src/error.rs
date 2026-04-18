use inventory_security::AuthError;

#[derive(Debug, thiserror::Error)]
pub enum BudgetError {
    #[error("Product not found: {id}")]
    ProductNotFound { id: i64 },
    #[error("Deal not found: {id}")]
    DealNotFound { id: i64 },
    #[error("Amount must be non-negative")]
    NegativeAmount,
    #[error("Description is required")]
    EmptyDescription,
    #[error("from_date must not be later than to_date")]
    InvalidDateRange,
    #[error("months_ahead must be a positive integer")]
    InvalidMonthsAhead,
    #[error(transparent)]
    Auth(#[from] AuthError),
    #[error("Internal error: {0}")]
    Internal(String),
}
