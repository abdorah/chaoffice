//! Location CRUD FFI functions.

use crate::dtos::{FfiCreateLocationDto, FfiLocationDto, FfiUpdateLocationDto};
use crate::error::FfiError;
use crate::get_app_context;
use frontend::commands::location_commands;

/// Create a new location.
///
/// Converts the FFI DTO to the internal `CreateLocationDto`, delegates to
/// `location_commands::create_orphan_location`, and converts the result back.
#[uniffi::export]
pub fn mobile_create_location(dto: FfiCreateLocationDto) -> Result<FfiLocationDto, FfiError> {
    let ctx = get_app_context()?;
    let create_dto = dto.into();
    let location = location_commands::create_orphan_location(ctx, None, &create_dto)
        .map_err(FfiError::from)?;
    Ok(location.into())
}

/// Get a single location by ID.
///
/// Returns `None` if the location does not exist.
#[uniffi::export]
pub fn mobile_get_location(id: i64) -> Result<Option<FfiLocationDto>, FfiError> {
    let ctx = get_app_context()?;
    let location = location_commands::get_location(ctx, &id).map_err(FfiError::from)?;
    Ok(location.map(|l| l.into()))
}

/// Get all locations.
#[uniffi::export]
pub fn mobile_get_all_locations() -> Result<Vec<FfiLocationDto>, FfiError> {
    let ctx = get_app_context()?;
    let locations = location_commands::get_all_location(ctx).map_err(FfiError::from)?;
    Ok(locations.into_iter().map(|l| l.into()).collect())
}

/// Update an existing location.
///
/// Converts the FFI DTO to the internal `UpdateLocationDto`, delegates to
/// `location_commands::update_location`, and converts the result back.
#[uniffi::export]
pub fn mobile_update_location(dto: FfiUpdateLocationDto) -> Result<FfiLocationDto, FfiError> {
    let ctx = get_app_context()?;
    let update_dto = dto.into();
    let location =
        location_commands::update_location(ctx, None, &update_dto).map_err(FfiError::from)?;
    Ok(location.into())
}

/// Remove a location by ID.
#[uniffi::export]
pub fn mobile_remove_location(id: i64) -> Result<(), FfiError> {
    let ctx = get_app_context()?;
    location_commands::remove_location(ctx, None, &id).map_err(FfiError::from)?;
    Ok(())
}
