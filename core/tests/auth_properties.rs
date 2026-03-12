// Property tests for Authentication & RBAC (Properties 2–5)
//
// **Validates: Requirements 1.2, 1.5, 2.1, 2.2, 2.3**

mod generators;

use std::collections::HashSet;
use std::sync::Arc;

use proptest::prelude::*;
use tokio::runtime::Runtime;
use tokio::sync::Mutex;

use sweet_lab_core::auth::rbac;
use sweet_lab_core::auth::service::AuthServiceImpl;
use sweet_lab_core::error::AppError;
use sweet_lab_core::models::domain::UserRole;
use sweet_lab_core::persistence::db;

// ── Helpers ────────────────────────────────────────────────────────────────

/// Build an AuthServiceImpl backed by an in-memory SQLite DB.
async fn setup() -> AuthServiceImpl {
    let pool = db::init_db(":memory:").await.expect("DB init failed");
    let enforcer = rbac::init_enforcer("policies/model.conf", "policies/policy.csv")
        .await
        .expect("Casbin init failed");
    AuthServiceImpl::new(pool, Arc::new(Mutex::new(enforcer)))
}

/// The complete set of (resource, action) pairs defined in policy.csv.
fn all_policy_pairs() -> Vec<(&'static str, &'static str)> {
    vec![
        ("admin_dashboard", "view"),
        ("users", "manage"),
        ("recipes", "manage"),
        ("inventory", "view"),
        ("inventory", "manage"),
        ("production", "view"),
        ("production", "execute"),
        ("sales", "view"),
        ("sales", "create"),
        ("customers", "manage"),
        ("wallets", "manage"),
        ("wallets", "view"),
        ("funds", "transfer"),
        ("expenses", "view"),
        ("expenses", "record"),
        ("debts", "view"),
        ("debts", "collect"),
        ("reports", "view"),
        ("reports", "export"),
    ]
}

/// Expected permission set for each role, derived from policy.csv.
fn expected_permissions(role: &UserRole) -> HashSet<(&'static str, &'static str)> {
    match role {
        UserRole::Admin => vec![
            ("admin_dashboard", "view"),
            ("users", "manage"),
            ("recipes", "manage"),
            ("inventory", "view"),
            ("inventory", "manage"),
            ("production", "view"),
            ("sales", "view"),
            ("sales", "create"),
            ("customers", "manage"),
            ("wallets", "manage"),
            ("wallets", "view"),
            ("funds", "transfer"),
            ("expenses", "view"),
            ("expenses", "record"),
            ("debts", "view"),
            ("debts", "collect"),
            ("reports", "view"),
            ("reports", "export"),
        ]
        .into_iter()
        .collect(),
        UserRole::Chef => vec![
            ("production", "execute"),
            ("production", "view"),
            ("inventory", "view"),
        ]
        .into_iter()
        .collect(),
        UserRole::Representative => vec![
            ("sales", "create"),
            ("sales", "view"),
            ("customers", "manage"),
            ("wallets", "view"),
            ("expenses", "record"),
            ("expenses", "view"),
            ("debts", "view"),
            ("debts", "collect"),
            ("inventory", "view"),
            ("inventory", "manage"),
        ]
        .into_iter()
        .collect(),
    }
}


// ── Property 2: Role permission enforcement ────────────────────────────────
//
// For any role and resource/action combination, Casbin result matches the
// expected permission set derived from policy.csv.
//
// **Validates: Requirements 1.5, 2.1**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(200))]

    #[test]
    fn prop2_role_permission_enforcement(
        role in generators::arb_user_role(),
        pair_idx in 0..19usize,  // 19 unique (resource, action) pairs
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let svc = setup().await;
            let pairs = all_policy_pairs();
            let (resource, action) = pairs[pair_idx % pairs.len()];
            let allowed = svc.check_permission(&role, resource, action).await.unwrap();
            let expected = expected_permissions(&role);
            let should_allow = expected.contains(&(resource, action));
            prop_assert_eq!(
                allowed, should_allow,
                "Role {:?} + ({}, {}): got {}, expected {}",
                role, resource, action, allowed, should_allow
            );
            Ok(())
        })?;
    }
}

// ── Property 3: Invalid credentials rejection ──────────────────────────────
//
// For any login attempt with invalid credentials, the error message does NOT
// reveal which specific field (username or password) was incorrect.
//
// **Validates: Requirements 1.2**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(50))]

    #[test]
    fn prop3_invalid_credentials_generic_error(
        username in "[a-zA-Z]{3,20}",
        password in "[a-zA-Z0-9]{4,20}",
        wrong_password in "[a-zA-Z0-9]{4,20}",
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let svc = setup().await;

            // Create a user with the generated credentials
            svc.create_user(&username, &password, "Test User", UserRole::Admin)
                .await
                .unwrap();

            // Attempt login with wrong password
            let err = svc.login(&username, &format!("{wrong_password}_bad")).await;
            prop_assert!(err.is_err(), "Login with wrong password should fail");
            if let Err(AppError::Authentication { message }) = &err {
                let msg_lower = message.to_lowercase();
                prop_assert!(
                    !msg_lower.contains("password"),
                    "Error message must not reveal 'password': {message}"
                );
                prop_assert!(
                    !msg_lower.contains("username"),
                    "Error message must not reveal 'username': {message}"
                );
            } else {
                prop_assert!(false, "Expected Authentication error, got: {:?}", err);
            }

            // Attempt login with non-existent username
            let err2 = svc.login(&format!("{username}_nonexistent"), &password).await;
            prop_assert!(err2.is_err(), "Login with wrong username should fail");
            if let Err(AppError::Authentication { message }) = &err2 {
                let msg_lower = message.to_lowercase();
                prop_assert!(
                    !msg_lower.contains("password"),
                    "Error message must not reveal 'password': {message}"
                );
                prop_assert!(
                    !msg_lower.contains("username"),
                    "Error message must not reveal 'username': {message}"
                );
                // Both errors should produce the same message
                if let Err(AppError::Authentication { message: msg1 }) = &err {
                    prop_assert_eq!(
                        msg1, message,
                        "Wrong-password and wrong-username errors must be identical"
                    );
                }
            } else {
                prop_assert!(false, "Expected Authentication error, got: {:?}", err2);
            }

            Ok(())
        })?;
    }
}

// ── Property 4: User creation field validation ─────────────────────────────
//
// For any user creation request missing required fields, return Validation
// error. With all fields present and valid, creation succeeds.
//
// **Validates: Requirements 2.2**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(50))]

    #[test]
    fn prop4_user_creation_missing_fields_rejected(
        valid_username in "[a-zA-Z]{3,15}",
        valid_password in "[a-zA-Z0-9]{4,15}",
        valid_full_name in "[a-zA-Z ]{3,20}",
        role in generators::arb_user_role(),
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let svc = setup().await;

            // Missing username (empty)
            let err = svc.create_user("", &valid_password, &valid_full_name, role.clone()).await;
            prop_assert!(
                matches!(&err, Err(AppError::Validation { field, .. }) if field == "username"),
                "Empty username should be rejected: {:?}", err
            );

            // Missing password (empty)
            let err = svc.create_user(&valid_username, "", &valid_full_name, role.clone()).await;
            prop_assert!(
                matches!(&err, Err(AppError::Validation { field, .. }) if field == "password"),
                "Empty password should be rejected: {:?}", err
            );

            // Missing full_name (empty)
            let err = svc.create_user(&valid_username, &valid_password, "", role.clone()).await;
            prop_assert!(
                matches!(&err, Err(AppError::Validation { field, .. }) if field == "full_name"),
                "Empty full_name should be rejected: {:?}", err
            );

            // Whitespace-only fields should also be rejected
            let err = svc.create_user("   ", &valid_password, &valid_full_name, role.clone()).await;
            prop_assert!(
                matches!(&err, Err(AppError::Validation { field, .. }) if field == "username"),
                "Whitespace-only username should be rejected: {:?}", err
            );

            // All fields present → creation succeeds
            let result = svc.create_user(&valid_username, &valid_password, &valid_full_name, role.clone()).await;
            prop_assert!(result.is_ok(), "Valid fields should succeed: {:?}", result);
            let user = result.unwrap();
            prop_assert_eq!(&user.username, &valid_username);
            prop_assert_eq!(&user.full_name, &valid_full_name);
            prop_assert_eq!(&user.role, &role);

            Ok(())
        })?;
    }
}

// ── Property 5: Role update applies on next login ──────────────────────────
//
// After role update and subsequent login, session role reflects new role.
//
// **Validates: Requirements 2.3**

proptest! {
    #![proptest_config(ProptestConfig::with_cases(30))]

    #[test]
    fn prop5_role_update_applies_on_next_login(
        username in "[a-zA-Z]{3,15}",
        password in "[a-zA-Z0-9]{4,15}",
        initial_role in generators::arb_user_role(),
        new_role in generators::arb_user_role(),
    ) {
        let rt = Runtime::new().unwrap();
        rt.block_on(async {
            let svc = setup().await;

            // Create user with initial role
            let user = svc
                .create_user(&username, &password, "Test User", initial_role.clone())
                .await
                .unwrap();

            // Verify initial login has the initial role
            let session1 = svc.login(&username, &password).await.unwrap();
            prop_assert_eq!(
                &session1.role, &initial_role,
                "Initial login should have initial role"
            );

            // Update role
            svc.update_user_role(user.id, new_role.clone()).await.unwrap();

            // Login again — session should reflect the new role
            let session2 = svc.login(&username, &password).await.unwrap();
            prop_assert_eq!(
                &session2.role, &new_role,
                "After role update, next login should reflect new role {:?}, got {:?}",
                new_role, session2.role
            );

            Ok(())
        })?;
    }
}
