//! Product CRUD FFI functions.

use crate::dtos::{FfiCreateProductDto, FfiProductDto, FfiUpdateProductDto};
use crate::error::FfiError;
use crate::get_app_context;
use frontend::commands::product_commands;

/// Create a new product.
///
/// Converts the FFI DTO to the internal `CreateProductDto`, delegates to
/// `product_commands::create_orphan_product`, and converts the result back.
#[uniffi::export]
pub fn mobile_create_product(dto: FfiCreateProductDto) -> Result<FfiProductDto, FfiError> {
    let ctx = get_app_context()?;
    let create_dto = dto.into();
    let product = product_commands::create_orphan_product(ctx, None, &create_dto)
        .map_err(FfiError::from)?;
    Ok(product.into())
}

/// Get a single product by ID.
///
/// Returns `None` if the product does not exist.
#[uniffi::export]
pub fn mobile_get_product(id: u64) -> Result<Option<FfiProductDto>, FfiError> {
    let ctx = get_app_context()?;
    let product = product_commands::get_product(ctx, &id).map_err(FfiError::from)?;
    Ok(product.map(|p| p.into()))
}

/// Get all products.
#[uniffi::export]
pub fn mobile_get_all_products() -> Result<Vec<FfiProductDto>, FfiError> {
    let ctx = get_app_context()?;
    let products = product_commands::get_all_product(ctx).map_err(FfiError::from)?;
    Ok(products.into_iter().map(|p| p.into()).collect())
}

/// Update an existing product.
///
/// Converts the FFI DTO to the internal `UpdateProductDto`, delegates to
/// `product_commands::update_product`, and converts the result back.
#[uniffi::export]
pub fn mobile_update_product(dto: FfiUpdateProductDto) -> Result<FfiProductDto, FfiError> {
    let ctx = get_app_context()?;
    let update_dto = dto.into();
    let product =
        product_commands::update_product(ctx, None, &update_dto).map_err(FfiError::from)?;
    Ok(product.into())
}

/// Remove a product by ID.
#[uniffi::export]
pub fn mobile_remove_product(id: u64) -> Result<(), FfiError> {
    let ctx = get_app_context()?;
    product_commands::remove_product(ctx, None, &id).map_err(FfiError::from)?;
    Ok(())
}
