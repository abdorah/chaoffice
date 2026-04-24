//! Deal CRUD FFI functions.

use crate::dtos::{FfiCreateDealDto, FfiDealDto, FfiUpdateDealDto};
use crate::error::FfiError;
use crate::get_app_context;
use frontend::commands::deal_commands;

/// Create a new deal.
///
/// Converts the FFI DTO to the internal `CreateDealDto`, delegates to
/// `deal_commands::create_orphan_deal`, and converts the result back.
#[uniffi::export]
pub fn mobile_create_deal(dto: FfiCreateDealDto) -> Result<FfiDealDto, FfiError> {
    let ctx = get_app_context()?;
    let create_dto = dto.into();
    let deal = deal_commands::create_orphan_deal(ctx, None, &create_dto)
        .map_err(FfiError::from)?;
    Ok(deal.into())
}

/// Get a single deal by ID.
///
/// Returns `None` if the deal does not exist.
#[uniffi::export]
pub fn mobile_get_deal(id: u64) -> Result<Option<FfiDealDto>, FfiError> {
    let ctx = get_app_context()?;
    let deal = deal_commands::get_deal(ctx, &id).map_err(FfiError::from)?;
    Ok(deal.map(|d| d.into()))
}

/// Get all deals.
#[uniffi::export]
pub fn mobile_get_all_deals() -> Result<Vec<FfiDealDto>, FfiError> {
    let ctx = get_app_context()?;
    let deals = deal_commands::get_all_deal(ctx).map_err(FfiError::from)?;
    Ok(deals.into_iter().map(|d| d.into()).collect())
}

/// Update an existing deal.
///
/// Converts the FFI DTO to the internal `UpdateDealDto`, delegates to
/// `deal_commands::update_deal`, and converts the result back.
#[uniffi::export]
pub fn mobile_update_deal(dto: FfiUpdateDealDto) -> Result<FfiDealDto, FfiError> {
    let ctx = get_app_context()?;
    let update_dto = dto.into();
    let deal =
        deal_commands::update_deal(ctx, None, &update_dto).map_err(FfiError::from)?;
    Ok(deal.into())
}

/// Remove a deal by ID.
#[uniffi::export]
pub fn mobile_remove_deal(id: u64) -> Result<(), FfiError> {
    let ctx = get_app_context()?;
    deal_commands::remove_deal(ctx, None, &id).map_err(FfiError::from)?;
    Ok(())
}
