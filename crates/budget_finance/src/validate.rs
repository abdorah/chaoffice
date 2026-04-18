use crate::error::BudgetError;
use common::entities::{Deal, Product};
use common::types::EntityId;

/// Validate amount and description fields of a budget entry.
pub fn validate_entry_input(amount: f64, description: &str) -> Result<(), BudgetError> {
    if amount < 0.0 {
        return Err(BudgetError::NegativeAmount);
    }
    if description.trim().is_empty() {
        return Err(BudgetError::EmptyDescription);
    }
    Ok(())
}

/// Verify that the Product referenced by product_id exists.
/// If product_id == 0, the relationship is absent → returns Ok(None).
/// `get_fn` is a closure that looks up a Product by EntityId (from the UoW).
pub fn validate_product<F>(product_id: i64, get_fn: F) -> Result<Option<EntityId>, BudgetError>
where
    F: FnOnce(&EntityId) -> anyhow::Result<Option<Product>>,
{
    if product_id == 0 {
        return Ok(None);
    }
    let id = product_id as EntityId;
    match get_fn(&id).map_err(|e| BudgetError::Internal(e.to_string()))? {
        Some(_) => Ok(Some(id)),
        None => Err(BudgetError::ProductNotFound { id: product_id }),
    }
}

/// Verify that the Deal referenced by deal_id exists.
/// If deal_id == 0, the relationship is absent → returns Ok(None).
/// `get_fn` is a closure that looks up a Deal by EntityId (from the UoW).
pub fn validate_deal<F>(deal_id: i64, get_fn: F) -> Result<Option<EntityId>, BudgetError>
where
    F: FnOnce(&EntityId) -> anyhow::Result<Option<Deal>>,
{
    if deal_id == 0 {
        return Ok(None);
    }
    let id = deal_id as EntityId;
    match get_fn(&id).map_err(|e| BudgetError::Internal(e.to_string()))? {
        Some(_) => Ok(Some(id)),
        None => Err(BudgetError::DealNotFound { id: deal_id }),
    }
}
