use chrono::{DateTime, Utc};
use common::entities::{Person, PersonRole};

use crate::error::PurchasingError;

/// Validate all scalar fields of a CreatePurchaseDealDto before entity creation.
/// Returns Ok(()) or the first validation error encountered.
pub fn validate_deal_input(
    title: &str,
    unit_cost: f64,
    total_value: f64,
    start_date: &DateTime<Utc>,
    end_date: &DateTime<Utc>,
) -> Result<(), PurchasingError> {
    if title.trim().is_empty() {
        return Err(PurchasingError::EmptyTitle);
    }
    if unit_cost < 0.0 {
        return Err(PurchasingError::NegativeUnitCost);
    }
    if total_value < 0.0 {
        return Err(PurchasingError::NegativeTotalValue);
    }
    if end_date < start_date {
        return Err(PurchasingError::InvalidDateRange);
    }
    Ok(())
}

/// Verify that the Person referenced by supplier_id exists and has role Supplier.
/// Pass the result of `uow.get_person(&supplier_id)`.
pub fn validate_supplier(
    person: Option<Person>,
    supplier_id: i64,
) -> Result<(), PurchasingError> {
    match person {
        None => Err(PurchasingError::SupplierNotFound { id: supplier_id }),
        Some(p) if p.role != PersonRole::Supplier => {
            Err(PurchasingError::NotASupplier { id: supplier_id })
        }
        Some(_) => Ok(()),
    }
}

/// Verify that the Person referenced by manager_id exists and has role Manager.
/// Pass the result of `uow.get_person(&manager_id)`.
pub fn validate_manager(
    person: Option<Person>,
    manager_id: i64,
) -> Result<(), PurchasingError> {
    match person {
        None => Err(PurchasingError::ManagerNotFound { id: manager_id }),
        Some(p) if p.role != PersonRole::Manager => {
            Err(PurchasingError::NotAManager { id: manager_id })
        }
        Some(_) => Ok(()),
    }
}

/// Verify that the Product referenced by product_id exists.
/// Pass the result of `uow.get_product(&product_id)`.
pub fn validate_product(
    product: Option<common::entities::Product>,
    product_id: i64,
) -> Result<(), PurchasingError> {
    match product {
        None => Err(PurchasingError::ProductNotFound { id: product_id }),
        Some(_) => Ok(()),
    }
}
