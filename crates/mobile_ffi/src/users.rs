//! User management FFI functions: create, deactivate, list.

use crate::dtos::{FfiCreateUserDto, FfiUserDto};
use crate::error::FfiError;
use crate::get_app_context;
use frontend::commands::{user_commands, user_management_commands};
use user_management::dtos::DeactivateUserDto;

/// Create a new user.
///
/// Converts the FFI DTO to the internal `CreateUserDto`, delegates to
/// `user_management_commands::create_user`, then fetches the full user
/// record to return a complete `FfiUserDto`.
#[uniffi::export]
pub fn mobile_create_user(dto: FfiCreateUserDto) -> Result<FfiUserDto, FfiError> {
    let ctx = get_app_context()?;
    let create_dto = dto.into();
    let result =
        user_management_commands::create_user(ctx, &create_dto).map_err(FfiError::from)?;

    // The create result only contains the user_id; fetch the full record.
    let user = user_commands::get_user(ctx, &(result.user_id as u64))
        .map_err(FfiError::from)?
        .ok_or_else(|| FfiError::DatabaseError {
            message: format!("User {} created but not found", result.user_id),
        })?;
    Ok(user.into())
}

/// Deactivate an existing user by ID.
#[uniffi::export]
pub fn mobile_deactivate_user(user_id: u64) -> Result<(), FfiError> {
    let ctx = get_app_context()?;
    let dto = DeactivateUserDto {
        user_id: user_id as i64,
    };
    user_management_commands::deactivate_user(ctx, &dto).map_err(FfiError::from)?;
    Ok(())
}

/// List all users.
///
/// Uses `user_commands::get_all_user` to retrieve full user records,
/// which are then converted to `FfiUserDto` with all fields populated.
#[uniffi::export]
pub fn mobile_list_users() -> Result<Vec<FfiUserDto>, FfiError> {
    let ctx = get_app_context()?;
    let users = user_commands::get_all_user(ctx).map_err(FfiError::from)?;
    Ok(users.into_iter().map(|u| u.into()).collect())
}
