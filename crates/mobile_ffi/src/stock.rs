//! Stock tracking FFI functions: record movements, get history, get summary.

use crate::dtos::{FfiRecordStockMovementDto, FfiStockHistoryDto, FfiStockMovementResultDto, FfiStockSummaryDto};
use crate::error::FfiError;
use crate::get_app_context;
use common::entities::UserRole;
use frontend::commands::{session_commands, stock_tracking_commands, user_commands};
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

/// Record a new stock movement.
///
/// Resolves the session token to a [`SecurityContext`], converts the FFI DTO
/// to the internal `RecordStockMovementDto`, and delegates to
/// `stock_tracking_commands::record_stock_movement`.
///
/// Returns the created movement ID and the product's new quantity.
#[uniffi::export]
pub fn mobile_record_stock_movement(
    session_token: String,
    dto: FfiRecordStockMovementDto,
) -> Result<FfiStockMovementResultDto, FfiError> {
    let ctx = get_app_context()?;
    let security_context = build_security_context(&session_token)?;
    let internal_dto: stock_tracking::dtos::RecordStockMovementDto = dto.into();
    let result =
        stock_tracking_commands::record_stock_movement(ctx, &security_context, &internal_dto)
            .map_err(FfiError::from)?;
    Ok(FfiStockMovementResultDto {
        movement_id: result.movement_id,
        new_product_quantity: result.new_product_quantity,
    })
}

/// Get stock movement history for a specific product.
///
/// Retrieves all movements for the given product ID. Uses a wide date range
/// (epoch to now) to return the full history.
#[uniffi::export]
pub fn mobile_get_stock_history(
    session_token: String,
    product_id: i64,
) -> Result<FfiStockHistoryDto, FfiError> {
    let ctx = get_app_context()?;
    let security_context = build_security_context(&session_token)?;
    let dto = stock_tracking::dtos::GetStockHistoryDto {
        product_id,
        from_date: chrono::DateTime::<chrono::Utc>::MIN_UTC,
        to_date: chrono::Utc::now(),
    };
    let result =
        stock_tracking_commands::get_stock_history(ctx, &security_context, &dto)
            .map_err(FfiError::from)?;
    Ok(result.into())
}

/// Get a summary of current stock levels across all products.
#[uniffi::export]
pub fn mobile_get_stock_summary(
    session_token: String,
) -> Result<FfiStockSummaryDto, FfiError> {
    let ctx = get_app_context()?;
    let security_context = build_security_context(&session_token)?;
    let result =
        stock_tracking_commands::get_stock_summary(ctx, &security_context)
            .map_err(FfiError::from)?;
    Ok(result.into())
}
