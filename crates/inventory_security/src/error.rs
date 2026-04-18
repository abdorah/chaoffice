#[derive(Debug, thiserror::Error)]
pub enum AuthError {
    #[error("Invalid credentials")]
    InvalidCredentials,
    #[error("Account deactivated")]
    AccountDeactivated,
    #[error("Session expired")]
    SessionExpired,
    #[error("Invalid session")]
    InvalidSession,
    #[error("Access denied: requires role {required}")]
    AccessDeniedRole { required: String },
    #[error("Access denied: requires permission {required}")]
    AccessDeniedPermission { required: String },
    #[error("Duplicate username: {username}")]
    DuplicateUsername { username: String },
    #[error("Password too short: minimum 8 characters")]
    PasswordTooShort,
    #[error("New password must differ from current password")]
    PasswordUnchanged,
    #[error("Password change required")]
    PasswordChangeRequired,
    #[error("Internal error: {0}")]
    Internal(String),
}
