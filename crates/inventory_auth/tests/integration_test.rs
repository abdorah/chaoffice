//! Integration test: exercises the core auth flow end-to-end using pure
//! business-logic functions (no DB required).

use chrono::Utc;
use common::entities::{User, UserRole};
use inventory_auth::bootstrap::prepare_bootstrap;
use inventory_auth::password::hash_password;
use inventory_auth::{prepare_create_user, validate_login, validate_logout, validate_password_change};
use inventory_security::AuthError;

/// Helper: build a User entity in memory (simulates what the DB layer would return).
fn make_user(id: u64, username: &str, password: &str, role: UserRole, is_active: bool) -> User {
    let hash = hash_password(password).expect("hash_password failed");
    User {
        id,
        created_at: Utc::now(),
        updated_at: Utc::now(),
        username: username.to_string(),
        password_hash: hash,
        display_name: username.to_string(),
        role,
        is_active,
        person: None,
        session: None,
    }
}

#[test]
fn full_auth_lifecycle() {
    // ── 1. Bootstrap creates admin user ──────────────────────────────
    let bootstrap = prepare_bootstrap(0);
    assert!(bootstrap.is_some(), "bootstrap should trigger when user_count=0");
    let (username, password_hash, display_name, role) = bootstrap.unwrap();
    assert_eq!(username, "admin");
    assert_eq!(display_name, "Administrator");
    assert_eq!(role, UserRole::Admin);

    // Simulate persisting the bootstrapped admin
    let admin = User {
        id: 1,
        created_at: Utc::now(),
        updated_at: Utc::now(),
        username,
        password_hash,
        display_name,
        role,
        is_active: true,
        person: None,
        session: None,
    };

    // ── 2. Login with admin/admin succeeds, password_change_required ─
    let login1 = validate_login(&admin, "admin").expect("admin login should succeed");
    assert!(login1.success);
    assert!(login1.password_change_required, "default admin should require password change");
    assert!(!login1.token.is_empty());

    // ── 3. Change password from "admin" to "newpassword123" ──────────
    let new_hash = validate_password_change(&admin.password_hash, "admin", "newpassword123")
        .expect("password change should succeed");

    // Simulate updating the admin in the DB
    let admin_updated = User {
        password_hash: new_hash,
        ..admin.clone()
    };

    // ── 4. Login with new password succeeds, no password_change_required
    let login2 = validate_login(&admin_updated, "newpassword123")
        .expect("login with new password should succeed");
    assert!(login2.success);
    assert!(
        !login2.password_change_required,
        "after password change, flag should be false"
    );

    // ── 5. Create a new user "operator1" with role Operator ──────────
    let existing_usernames = vec!["admin".to_string()];
    let op_hash = prepare_create_user("operator1", "operator1pass", &existing_usernames)
        .expect("create operator1 should succeed");

    let operator1 = make_user(2, "operator1", "operator1pass", UserRole::Operator, true);
    // Verify the hash from prepare_create_user works
    assert!(
        inventory_auth::password::verify_password("operator1pass", &op_hash).unwrap(),
        "prepared hash should verify"
    );

    // ── 6. Login as operator1 succeeds ───────────────────────────────
    let login3 = validate_login(&operator1, "operator1pass")
        .expect("operator1 login should succeed");
    assert!(login3.success);
    assert!(!login3.password_change_required);

    // ── 7. Deactivate operator1 ──────────────────────────────────────
    let operator1_deactivated = User {
        is_active: false,
        ..operator1
    };

    // ── 8. Login as deactivated operator1 fails with AccountDeactivated
    let err = validate_login(&operator1_deactivated, "operator1pass")
        .expect_err("deactivated user login should fail");
    assert!(
        matches!(err, AuthError::AccountDeactivated),
        "expected AccountDeactivated, got: {err:?}"
    );

    // ── 9. Logout with valid token succeeds ──────────────────────────
    validate_logout(&login3.token).expect("logout with valid token should succeed");

    // ── 10. Logout with empty token fails ────────────────────────────
    let logout_err = validate_logout("").expect_err("logout with empty token should fail");
    assert!(
        matches!(logout_err, AuthError::InvalidSession),
        "expected InvalidSession, got: {logout_err:?}"
    );
}
