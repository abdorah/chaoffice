//! Purchasing FFI functions: create purchase deals, get deals by supplier, get active deals.

use crate::dtos::{FfiActiveDealsDto, FfiCreatePurchaseDealDto, FfiCreatePurchaseDealReturnDto, FfiDealListDto};
use crate::error::FfiError;
use crate::get_app_context;
use common::entities::UserRole;
use frontend::commands::{session_commands, user_commands, purchasing_commands};
use inventory_security::SecurityContext;

/// Build a [`SecurityContext`] from a session token by resolving the owning
/// user's ID and role through the session and user tables.
fn build_security_context(session_token: &str) -> Result<SecurityContext, FfiError> {
    let ctx = get_app_context()?;

    let sessions = session_commands::get_all_session(ctx).map_err(FfiError::from)?;
    let session = sessions
        .iter()
        .find(|s| s.token == session_token)
        .ok_or_else(|| FfiError::AuthenticationError {
            message: "Session not found for the provided token".into(),
        })?;

    let users = user_commands::get_all_user(ctx).map_err(FfiError::from)?;
    let user = users
        .iter()
        .find(|u| u.session == Some(session.id))
        .ok_or_else(|| FfiError::AuthenticationError {
            message: "No user associated with the provided session token".into(),
        })?;

    let role: UserRole = user.role.clone();
    Ok(SecurityContext::from_user(
        user.id,
        role,
        session_token.to_string(),
    ))
}

/// Create a new purchase deal.
///
/// Resolves the session token to a [`SecurityContext`], converts the FFI DTO
/// to the internal `CreatePurchaseDealDto`, and delegates to
/// `purchasing_commands::create_purchase_deal`.
#[uniffi::export]
pub fn mobile_create_purchase_deal(
    session_token: String,
    dto: FfiCreatePurchaseDealDto,
) -> Result<FfiCreatePurchaseDealReturnDto, FfiError> {
    let ctx = get_app_context()?;
    let security_context = build_security_context(&session_token)?;
    let internal_dto: purchasing::CreatePurchaseDealDto = dto.into();
    let result =
        purchasing_commands::create_purchase_deal(ctx, None, &security_context, &internal_dto)
            .map_err(FfiError::from)?;
    Ok(result.into())
}

/// Get all deals for a specific supplier.
///
/// Resolves the session token to a [`SecurityContext`] and delegates to
/// `purchasing_commands::get_deals_by_supplier`.
#[uniffi::export]
pub fn mobile_get_deals_by_supplier(
    session_token: String,
    supplier_id: i64,
) -> Result<FfiDealListDto, FfiError> {
    let ctx = get_app_context()?;
    let security_context = build_security_context(&session_token)?;
    let dto = purchasing::GetDealsBySupplierDto { supplier_id };
    let result =
        purchasing_commands::get_deals_by_supplier(ctx, &security_context, &dto)
            .map_err(FfiError::from)?;
    Ok(result.into())
}

/// Get all currently active deals.
///
/// Resolves the session token to a [`SecurityContext`] and delegates to
/// `purchasing_commands::get_active_deals`.
#[uniffi::export]
pub fn mobile_get_active_deals(
    session_token: String,
) -> Result<FfiActiveDealsDto, FfiError> {
    let ctx = get_app_context()?;
    let security_context = build_security_context(&session_token)?;
    let result =
        purchasing_commands::get_active_deals(ctx, &security_context)
            .map_err(FfiError::from)?;
    Ok(result.into())
}
