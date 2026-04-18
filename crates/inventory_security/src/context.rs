use common::entities::UserRole;

use crate::error::AuthError;
use crate::permission::{Permission, permission_matches, permissions_for_role};

#[derive(Debug, Clone)]
pub struct SecurityContext {
    pub user_id: u64,
    pub role: UserRole,
    pub permissions: Vec<Permission>,
    pub token: String,
}

impl SecurityContext {
    /// Build a SecurityContext from a session token.
    /// NOTE: This requires the frontend/controller layer to resolve the token
    /// to a User via DB lookup. Use `from_user` when you already have the user info.
    pub fn from_token(_token: &str) -> Result<Self, AuthError> {
        // Full implementation requires DB access to look up the session and user.
        // The Qleany controller layer should call `from_user` after resolving the token.
        Err(AuthError::Internal(
            "from_token requires DB access — use from_user after resolving the session".to_string(),
        ))
    }

    /// Build a SecurityContext from known user information.
    /// Resolves the role's permissions automatically.
    pub fn from_user(user_id: u64, role: UserRole, token: String) -> Self {
        let permissions = permissions_for_role(&role);
        Self {
            user_id,
            role,
            permissions,
            token,
        }
    }

    /// Check if the user has the specified role.
    pub fn has_role(&self, role: UserRole) -> bool {
        self.role == role
    }

    /// Check if the user has any of the specified roles.
    pub fn has_any_role(&self, roles: &[UserRole]) -> bool {
        roles.iter().any(|r| self.role == *r)
    }

    /// Check if the user holds a permission that satisfies the required permission.
    /// Admin always has all permissions via the `*:*` wildcard.
    pub fn has_permission(&self, required: &str) -> bool {
        self.permissions.iter().any(|p| permission_matches(p, required))
    }

    /// Check if the user is an Admin.
    pub fn is_admin(&self) -> bool {
        self.role == UserRole::Admin
    }
}
