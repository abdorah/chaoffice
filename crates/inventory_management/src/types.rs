use common::entities::ProductStatus;
use serde::{Deserialize, Serialize};

/// Flat record for CSV/JSON import and export.
/// Field names match the CSV header and JSON keys.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ProductRecord {
    pub name: String,
    pub reference: String,
    pub description: String,
    pub quantity: i64,
    pub price_unit: f64,
    pub status: String,
    pub category_name: String,
    pub supplier_name: String,
    pub location_name: String,
}

/// Parse a status string into a ProductStatus enum value.
pub fn parse_product_status(s: &str) -> Option<ProductStatus> {
    match s {
        "Available" => Some(ProductStatus::Available),
        "OutOfStock" => Some(ProductStatus::OutOfStock),
        "Discontinued" => Some(ProductStatus::Discontinued),
        "Reserved" => Some(ProductStatus::Reserved),
        _ => None,
    }
}

/// Convert a ProductStatus enum value to its string representation.
pub fn product_status_to_string(status: &ProductStatus) -> String {
    match status {
        ProductStatus::Available => "Available".to_string(),
        ProductStatus::OutOfStock => "OutOfStock".to_string(),
        ProductStatus::Discontinued => "Discontinued".to_string(),
        ProductStatus::Reserved => "Reserved".to_string(),
    }
}
