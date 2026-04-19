use common::entities::UserRole;

use crate::password::hash_password;

/// Check if bootstrap is needed (no users exist) and prepare the default admin.
///
/// Returns `Some((username, password_hash, display_name, role))` if bootstrap is needed,
/// `None` if users already exist.
///
/// The controller handles the actual User entity creation in the DB.
pub fn prepare_bootstrap(user_count: usize) -> Option<(String, String, String, UserRole)> {
    if user_count > 0 {
        return None;
    }

    let hash = hash_password("Password1").expect("failed to hash default password");
    Some((
        "admin".to_string(),
        hash,
        "Administrator".to_string(),
        UserRole::Admin,
    ))
}


#[cfg(test)]
mod tests {
    use super::*;
    use crate::password::verify_password;

    #[test]
    fn bootstrap_needed_when_no_users() {
        let result = prepare_bootstrap(0);
        assert!(result.is_some());

        let (username, hash, display_name, role) = result.unwrap();
        assert_eq!(username, "admin");
        assert_eq!(display_name, "Administrator");
        assert_eq!(role, UserRole::Admin);
        // The hash should verify against "Password1"
        assert!(verify_password("Password1", &hash).unwrap());
    }

    #[test]
    fn bootstrap_skipped_when_users_exist() {
        assert!(prepare_bootstrap(1).is_none());
        assert!(prepare_bootstrap(5).is_none());
        assert!(prepare_bootstrap(100).is_none());
    }

    #[test]
    fn bootstrap_hash_is_valid_argon2() {
        let (_, hash, _, _) = prepare_bootstrap(0).unwrap();
        // Argon2 hashes start with $argon2
        assert!(hash.starts_with("$argon2"));
    }
}
