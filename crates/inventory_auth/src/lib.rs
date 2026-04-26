pub mod bootstrap;
pub mod password;
pub mod session;
pub mod session_store;

use common::entities::User;
use common::types::EntityId;
use inventory_security::AuthError;

use crate::password::{hash_password, verify_password};
use crate::session::create_session;

/// Result returned from a successful login validation.
#[derive(Debug, Clone)]
pub struct LoginResult {
    pub success: bool,
    pub token: String,
    pub user_id: EntityId,
    pub role: String,
    pub display_name: String,
    pub error_message: String,
    pub password_change_required: bool,
}

/// Summary of a user for list operations.
#[derive(Debug, Clone)]
pub struct UserSummary {
    pub id: EntityId,
    pub username: String,
    pub display_name: String,
    pub role: String,
    pub is_active: bool,
}

/// Validate login credentials against a User entity.
///
/// The controller is responsible for fetching the User by username from the DB.
/// This function performs pure business logic validation:
/// 1. Checks if the account is active
/// 2. Verifies the password against the stored hash
/// 3. Creates a session token
/// 4. Detects default admin needing password change
pub fn validate_login(user: &User, password: &str) -> Result<LoginResult, AuthError> {
    // 1. Check if account is active
    if !user.is_active {
        return Err(AuthError::AccountDeactivated);
    }

    // 2. Verify password against stored hash
    let password_valid = verify_password(password, &user.password_hash)?;
    if !password_valid {
        return Err(AuthError::InvalidCredentials);
    }

    // 3. Create a new session
    let session = create_session(user.id);

    // 4. Check if this is the default admin with default password
    let password_change_required =
        user.username == "admin" && verify_password("Password1", &user.password_hash).unwrap_or(false);

    let role_str = format!("{:?}", user.role);

    Ok(LoginResult {
        success: true,
        token: session.token,
        user_id: user.id,
        role: role_str,
        display_name: user.display_name.clone(),
        error_message: String::new(),
        password_change_required,
    })
}

/// Validate a logout request by checking the token is non-empty.
///
/// The controller handles the actual Session entity deletion from the DB.
/// This function just validates the token format.
pub fn validate_logout(token: &str) -> Result<(), AuthError> {
    if token.is_empty() {
        return Err(AuthError::InvalidSession);
    }
    Ok(())
}

/// Validate and process a password change.
///
/// The controller handles the actual User entity update in the DB.
/// This function validates the old password, enforces password policy,
/// and returns the new password hash.
pub fn validate_password_change(
    current_password_hash: &str,
    old_password: &str,
    new_password: &str,
) -> Result<String, AuthError> {
    // 1. Verify old password against current hash
    let old_valid = verify_password(old_password, current_password_hash)?;
    if !old_valid {
        return Err(AuthError::InvalidCredentials);
    }

    // 2. Check new password minimum length
    if new_password.len() < 8 {
        return Err(AuthError::PasswordTooShort);
    }

    // 3. Check new password differs from old password
    let same_password = verify_password(new_password, current_password_hash)?;
    if same_password {
        return Err(AuthError::PasswordUnchanged);
    }

    // 4. Hash the new password and return it
    hash_password(new_password)
}

/// Validate and prepare a new user for creation.
///
/// Checks username uniqueness and password policy, then returns the password hash.
/// The controller handles the actual User entity creation in the DB.
pub fn prepare_create_user(
    username: &str,
    password: &str,
    existing_usernames: &[String],
) -> Result<String, AuthError> {
    // 1. Check username uniqueness
    if existing_usernames.iter().any(|u| u == username) {
        return Err(AuthError::DuplicateUsername {
            username: username.to_string(),
        });
    }

    // 2. Check password minimum length
    if password.len() < 8 {
        return Err(AuthError::PasswordTooShort);
    }

    // 3. Hash password and return
    hash_password(password)
}

/// Map a User entity to a UserSummary for list operations.
pub fn user_to_summary(user: &User) -> UserSummary {
    UserSummary {
        id: user.id,
        username: user.username.clone(),
        display_name: user.display_name.clone(),
        role: format!("{:?}", user.role),
        is_active: user.is_active,
    }
}


#[cfg(test)]
mod tests {
    use super::*;
    use chrono::Utc;
    use common::entities::UserRole;

    fn make_user(username: &str, password: &str, role: UserRole, is_active: bool) -> User {
        let hash = crate::password::hash_password(password).unwrap();
        User {
            id: 1,
            created_at: Utc::now(),
            updated_at: Utc::now(),
            username: username.to_string(),
            password_hash: hash,
            display_name: "Test User".to_string(),
            role,
            is_active,
            person: None,
            session: None,
        }
    }

    // --- validate_login tests ---

    #[test]
    fn login_success_returns_correct_result() {
        let user = make_user("testuser", "password123", UserRole::Operator, true);
        let result = validate_login(&user, "password123").unwrap();

        assert!(result.success);
        assert!(!result.token.is_empty());
        assert_eq!(result.user_id, 1);
        assert_eq!(result.role, "Operator");
        assert_eq!(result.display_name, "Test User");
        assert!(result.error_message.is_empty());
        assert!(!result.password_change_required);
    }

    #[test]
    fn login_wrong_password_returns_invalid_credentials() {
        let user = make_user("testuser", "password123", UserRole::Operator, true);
        let err = validate_login(&user, "wrongpassword").unwrap_err();

        assert!(matches!(err, AuthError::InvalidCredentials));
    }

    #[test]
    fn login_inactive_user_returns_account_deactivated() {
        let user = make_user("testuser", "password123", UserRole::Operator, false);
        let err = validate_login(&user, "password123").unwrap_err();

        assert!(matches!(err, AuthError::AccountDeactivated));
    }

    #[test]
    fn login_inactive_checked_before_password() {
        // Even with correct password, inactive should return AccountDeactivated
        let user = make_user("testuser", "password123", UserRole::Operator, false);
        let err = validate_login(&user, "password123").unwrap_err();

        assert!(matches!(err, AuthError::AccountDeactivated));
    }

    #[test]
    fn login_default_admin_flags_password_change() {
        let user = make_user("admin", "Password1", UserRole::Admin, true);
        let result = validate_login(&user, "Password1").unwrap();

        assert!(result.success);
        assert!(result.password_change_required);
    }

    #[test]
    fn login_admin_with_changed_password_no_flag() {
        let user = make_user("admin", "newSecurePass123", UserRole::Admin, true);
        let result = validate_login(&user, "newSecurePass123").unwrap();

        assert!(result.success);
        assert!(!result.password_change_required);
    }

    #[test]
    fn login_token_is_uuid_format() {
        let user = make_user("testuser", "password123", UserRole::Manager, true);
        let result = validate_login(&user, "password123").unwrap();

        // UUID v4 format: 8-4-4-4-12 hex chars
        assert!(uuid::Uuid::parse_str(&result.token).is_ok());
    }

    // --- validate_logout tests ---

    #[test]
    fn logout_valid_token_succeeds() {
        assert!(validate_logout("some-valid-token").is_ok());
    }

    #[test]
    fn logout_empty_token_returns_invalid_session() {
        let err = validate_logout("").unwrap_err();
        assert!(matches!(err, AuthError::InvalidSession));
    }

    // --- validate_password_change tests ---

    #[test]
    fn password_change_success() {
        let old_password = "oldpassword123";
        let hash = crate::password::hash_password(old_password).unwrap();
        let new_hash = validate_password_change(&hash, old_password, "newpassword456").unwrap();

        // New hash should verify against new password
        assert!(crate::password::verify_password("newpassword456", &new_hash).unwrap());
        // New hash should NOT verify against old password
        assert!(!crate::password::verify_password(old_password, &new_hash).unwrap());
    }

    #[test]
    fn password_change_wrong_old_password() {
        let hash = crate::password::hash_password("correctpassword").unwrap();
        let err = validate_password_change(&hash, "wrongpassword", "newpassword456").unwrap_err();

        assert!(matches!(err, AuthError::InvalidCredentials));
    }

    #[test]
    fn password_change_too_short() {
        let hash = crate::password::hash_password("oldpassword123").unwrap();
        let err = validate_password_change(&hash, "oldpassword123", "short").unwrap_err();

        assert!(matches!(err, AuthError::PasswordTooShort));
    }

    #[test]
    fn password_change_same_password() {
        let password = "samepassword123";
        let hash = crate::password::hash_password(password).unwrap();
        let err = validate_password_change(&hash, password, password).unwrap_err();

        assert!(matches!(err, AuthError::PasswordUnchanged));
    }

    #[test]
    fn password_change_exactly_8_chars_succeeds() {
        let old = "oldpassword123";
        let hash = crate::password::hash_password(old).unwrap();
        let result = validate_password_change(&hash, old, "exactly8");

        assert!(result.is_ok());
    }

    #[test]
    fn password_change_7_chars_fails() {
        let old = "oldpassword123";
        let hash = crate::password::hash_password(old).unwrap();
        let err = validate_password_change(&hash, old, "seven77").unwrap_err();

        assert!(matches!(err, AuthError::PasswordTooShort));
    }

    // --- prepare_create_user tests ---

    #[test]
    fn create_user_success() {
        let existing: Vec<String> = vec!["alice".to_string(), "bob".to_string()];
        let hash = prepare_create_user("charlie", "password123", &existing).unwrap();

        assert!(crate::password::verify_password("password123", &hash).unwrap());
    }

    #[test]
    fn create_user_duplicate_username() {
        let existing = vec!["alice".to_string(), "bob".to_string()];
        let err = prepare_create_user("alice", "password123", &existing).unwrap_err();

        match err {
            AuthError::DuplicateUsername { username } => assert_eq!(username, "alice"),
            _ => panic!("expected DuplicateUsername"),
        }
    }

    #[test]
    fn create_user_short_password() {
        let existing: Vec<String> = vec![];
        let err = prepare_create_user("newuser", "short", &existing).unwrap_err();

        assert!(matches!(err, AuthError::PasswordTooShort));
    }

    // --- user_to_summary tests ---

    #[test]
    fn user_to_summary_maps_correctly() {
        let user = User {
            id: 42,
            created_at: Utc::now(),
            updated_at: Utc::now(),
            username: "jdoe".to_string(),
            password_hash: "hash".to_string(),
            display_name: "John Doe".to_string(),
            role: UserRole::Manager,
            is_active: true,
            person: None,
            session: None,
        };

        let summary = user_to_summary(&user);
        assert_eq!(summary.id, 42);
        assert_eq!(summary.username, "jdoe");
        assert_eq!(summary.display_name, "John Doe");
        assert_eq!(summary.role, "Manager");
        assert!(summary.is_active);
    }

    #[test]
    fn user_to_summary_inactive_user() {
        let user = User {
            id: 7,
            created_at: Utc::now(),
            updated_at: Utc::now(),
            username: "inactive".to_string(),
            password_hash: "hash".to_string(),
            display_name: "Gone User".to_string(),
            role: UserRole::Viewer,
            is_active: false,
            person: None,
            session: None,
        };

        let summary = user_to_summary(&user);
        assert_eq!(summary.role, "Viewer");
        assert!(!summary.is_active);
    }
}
