//! Category CRUD FFI functions.

use crate::dtos::{FfiCategoryDto, FfiCreateCategoryDto, FfiUpdateCategoryDto};
use crate::error::FfiError;

use crate::get_app_context;
use frontend::commands::category_commands;

/// Create a new category.
///
/// Converts the FFI DTO to the internal `CreateCategoryDto`, delegates to
/// `category_commands::create_orphan_category`, and converts the result back.
#[uniffi::export]
pub fn mobile_create_category(dto: FfiCreateCategoryDto) -> Result<FfiCategoryDto, FfiError> {
    let ctx = get_app_context()?;
    let create_dto = dto.into();
    let category = category_commands::create_orphan_category(ctx, None, &create_dto)
        .map_err(FfiError::from)?;
    Ok(category.into())
}

/// Get a single category by ID.
///
/// Returns `None` if the category does not exist.
#[uniffi::export]
pub fn mobile_get_category(id: i64) -> Result<Option<FfiCategoryDto>, FfiError> {
    let ctx = get_app_context()?;
    let category = category_commands::get_category(ctx, &id).map_err(FfiError::from)?;
    Ok(category.map(|c| c.into()))
}

/// Get all categories.
#[uniffi::export]
pub fn mobile_get_all_categories() -> Result<Vec<FfiCategoryDto>, FfiError> {
    let ctx = get_app_context()?;
    let categories = category_commands::get_all_category(ctx).map_err(FfiError::from)?;
    Ok(categories.into_iter().map(|c| c.into()).collect())
}

/// Update an existing category.
///
/// Converts the FFI DTO to the internal `UpdateCategoryDto`, delegates to
/// `category_commands::update_category`, and converts the result back.
#[uniffi::export]
pub fn mobile_update_category(dto: FfiUpdateCategoryDto) -> Result<FfiCategoryDto, FfiError> {
    let ctx = get_app_context()?;
    let update_dto = dto.into();
    let category =
        category_commands::update_category(ctx, None, &update_dto).map_err(FfiError::from)?;
    Ok(category.into())
}

/// Remove a category by ID.
#[uniffi::export]
pub fn mobile_remove_category(id: i64) -> Result<(), FfiError> {
    let ctx = get_app_context()?;
    category_commands::remove_category(ctx, None, &id).map_err(FfiError::from)?;
    Ok(())
}
