use common::entities::{Category, Location, Person, PersonRole};
use common::types::EntityId;

/// Resolve a category name to a Category ID.
/// Returns Ok(None) if the name is empty.
/// Returns Err with the name if non-empty but not found.
pub fn resolve_category(
    categories: &[Category],
    category_name: &str,
) -> Result<Option<EntityId>, String> {
    if category_name.is_empty() {
        return Ok(None);
    }
    categories
        .iter()
        .find(|c| c.name == category_name)
        .map(|c| Some(c.id))
        .ok_or_else(|| category_name.to_string())
}

/// Resolve a supplier name to a Person ID (must have role Supplier).
/// Returns Ok(None) if the name is empty.
/// Returns Err with the name if non-empty but not found or not a Supplier.
pub fn resolve_supplier(
    persons: &[Person],
    supplier_name: &str,
) -> Result<Option<EntityId>, String> {
    if supplier_name.is_empty() {
        return Ok(None);
    }
    persons
        .iter()
        .find(|p| p.name == supplier_name && p.role == PersonRole::Supplier)
        .map(|p| Some(p.id))
        .ok_or_else(|| supplier_name.to_string())
}

/// Resolve a location name to a Location ID.
/// Returns Ok(None) if the name is empty.
/// Returns Err with the name if non-empty but not found.
pub fn resolve_location(
    locations: &[Location],
    location_name: &str,
) -> Result<Option<EntityId>, String> {
    if location_name.is_empty() {
        return Ok(None);
    }
    locations
        .iter()
        .find(|l| l.name == location_name)
        .map(|l| Some(l.id))
        .ok_or_else(|| location_name.to_string())
}
