//! Budget and finance FFI functions: record entries, get summary, get projection.

use crate::dtos::{
    FfiBudgetProjectionDto, FfiBudgetSummaryDto, FfiGetBudgetProjectionDto,
    FfiGetBudgetSummaryDto, FfiRecordBudgetEntryDto, FfiRecordBudgetEntryResultDto,
};
use crate::error::FfiError;
use crate::get_app_context;
use common::entities::UserRole;
use frontend::commands::{budget_finance_commands, session_commands, user_commands};
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

/// Record a new budget entry.
///
/// Resolves the session token to a [`SecurityContext`], converts the FFI DTO
/// to the internal `RecordBudgetEntryDto`, and delegates to
/// `budget_finance_commands::record_budget_entry`.
#[uniffi::export]
pub fn mobile_record_budget_entry(
    session_token: String,
    dto: FfiRecordBudgetEntryDto,
) -> Result<FfiRecordBudgetEntryResultDto, FfiError> {
    let ctx = get_app_context()?;
    let security_context = build_security_context(&session_token)?;
    let internal_dto: budget_finance::dtos::RecordBudgetEntryDto = dto.into();
    let result =
        budget_finance_commands::record_budget_entry(ctx, None, &security_context, &internal_dto)
            .map_err(FfiError::from)?;
    Ok(result.into())
}

/// Get a budget summary for the specified date range.
#[uniffi::export]
pub fn mobile_get_budget_summary(
    session_token: String,
    dto: FfiGetBudgetSummaryDto,
) -> Result<FfiBudgetSummaryDto, FfiError> {
    let ctx = get_app_context()?;
    let security_context = build_security_context(&session_token)?;
    let internal_dto: budget_finance::dtos::GetBudgetSummaryDto = dto.into();
    let result =
        budget_finance_commands::get_budget_summary(ctx, &security_context, &internal_dto)
            .map_err(FfiError::from)?;
    Ok(result.into())
}

/// Get budget projections for a number of months ahead.
#[uniffi::export]
pub fn mobile_get_budget_projection(
    session_token: String,
    dto: FfiGetBudgetProjectionDto,
) -> Result<FfiBudgetProjectionDto, FfiError> {
    let ctx = get_app_context()?;
    let security_context = build_security_context(&session_token)?;
    let internal_dto: budget_finance::dtos::GetBudgetProjectionDto = dto.into();
    let result =
        budget_finance_commands::get_budget_projection(ctx, &security_context, &internal_dto)
            .map_err(FfiError::from)?;
    Ok(result.into())
}
