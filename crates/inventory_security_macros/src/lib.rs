//! RBAC guard functions for use-case handlers.
//!
//! Instead of proc macros (which require syn/quote parsing), this crate provides
//! simple guard functions that can be called at the start of use-case handlers
//! to enforce role-based and permission-based access control.

// Re-export from inventory_security for convenience
pub use inventory_security::{AuthError, Permission, SecurityContext};

use common::entities::UserRole;

/// Check that the security context has the required role (Admin always passes).
///
/// # Errors
/// Returns `AuthError::AccessDeniedRole` if the user does not have the required role
/// and is not an Admin.
pub fn check_role(ctx: &SecurityContext, required: UserRole) -> Result<(), AuthError> {
    let label = format!("{:?}", required);
    if ctx.is_admin() || ctx.has_role(required) {
        Ok(())
    } else {
        Err(AuthError::AccessDeniedRole { required: label })
    }
}

/// Check that the security context has any of the required roles (Admin always passes).
///
/// # Errors
/// Returns `AuthError::AccessDeniedRole` if the user does not have any of the required
/// roles and is not an Admin.
pub fn check_any_role(ctx: &SecurityContext, required: &[UserRole]) -> Result<(), AuthError> {
    if ctx.is_admin() || ctx.has_any_role(required) {
        Ok(())
    } else {
        let names: Vec<String> = required.iter().map(|r| format!("{:?}", r)).collect();
        Err(AuthError::AccessDeniedRole {
            required: names.join(" or "),
        })
    }
}

/// Check that the security context has the required permission (Admin always passes).
///
/// # Errors
/// Returns `AuthError::AccessDeniedPermission` if the user does not hold the required
/// permission and is not an Admin.
pub fn check_permission(ctx: &SecurityContext, required: &str) -> Result<(), AuthError> {
    if ctx.has_permission(required) {
        Ok(())
    } else {
        Err(AuthError::AccessDeniedPermission {
            required: required.to_string(),
        })
    }
}
