//! Authentication FFI functions: login, logout, change_password.

use crate::dtos::{FfiChangePasswordResult, FfiLoginResult};
use crate::error::FfiError;
use crate::get_app_context;
use authentication::dtos::{ChangePasswordDto, LoginDto, LogoutDto};
use frontend::commands::{authentication_commands, session_commands, user_commands};

/// Helper: resolve a session token to the owning user's ID.
///
/// Walks all sessions to find the matching token, then walks all users to find
/// the one whose `session` field points at that session ID.
fn resolve_user_id_from_token(session_token: &str) -> Result<i64, FfiError> {
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

    Ok(user.id as i64)
}

/// Log in with username and password.
///
/// Returns an `FfiLoginResult` containing the session token, user ID,
/// display name, and role on success. If the credentials are invalid the
/// backend returns a result with `success == false` — we convert that into
/// an `FfiError::AuthenticationError`.
#[uniffi::export]
pub fn mobile_login(username: String, password: String) -> Result<FfiLoginResult, FfiError> {
    let ctx = get_app_context()?;
    let dto = LoginDto { username, password };
    let result = authentication_commands::login(ctx, &dto).map_err(FfiError::from)?;

    if !result.success {
        return Err(FfiError::AuthenticationError {
            message: result.error_message,
        });
    }

    Ok(result.into())
}

/// Log out the session identified by `session_token`.
#[uniffi::export]
pub fn mobile_logout(session_token: String) -> Result<(), FfiError> {
    let ctx = get_app_context()?;
    let dto = LogoutDto {
        token: session_token,
    };
    authentication_commands::logout(ctx, &dto).map_err(FfiError::from)?;
    Ok(())
}

/// Change the password for the user owning `session_token`.
///
/// The session token is resolved to a user ID internally so the caller
/// does not need to know their own user ID.
#[uniffi::export]
pub fn mobile_change_password(
    session_token: String,
    old_password: String,
    new_password: String,
) -> Result<FfiChangePasswordResult, FfiError> {
    let user_id = resolve_user_id_from_token(&session_token)?;

    let ctx = get_app_context()?;
    let dto = ChangePasswordDto {
        user_id,
        old_password,
        new_password,
    };
    let result = authentication_commands::change_password(ctx, &dto).map_err(FfiError::from)?;
    Ok(result.into())
}
