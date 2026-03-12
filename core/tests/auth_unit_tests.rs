//! Unit tests for authentication edge cases.
//!
//! Validates: Requirements 1.4, 1.5, 2.3

use std::sync::Arc;

use argon2::password_hash::{PasswordHash, PasswordHasher, PasswordVerifier, SaltString};
use argon2::Argon2;
use chrono::{Duration, Utc};
use tokio::sync::Mutex;
use uuid::Uuid;

use sweet_lab_core::auth::rbac;
use sweet_lab_core::auth::service::AuthServiceImpl;
use sweet_lab_core::error::AppError;
use sweet_lab_core::models::domain::{Session, UserRole};
use sweet_lab_core::persistence::db;

/// Helper: create an in-memory DB, run migrations, and build an AuthServiceImpl.
async fn setup() -> AuthServiceImpl {
    let pool = db::init_db(":memory:").await.expect("DB init failed");
    let enforcer = rbac::init_enforcer("policies/model.conf", "policies/policy.csv")
        .await
        .expect("Casbin init failed");
    AuthServiceImpl::new(pool, Arc::new(Mutex::new(enforcer)))
}

// ── Session expiry at exactly 8 hours (Req 1.4) ───────────────────────────

#[tokio::test]
async fn session_invalid_at_exactly_8_hours_inactivity() {
    let now = Utc::now();
    let session = Session {
        session_id: Uuid::new_v4(),
        user_id: Uuid::new_v4(),
        role: UserRole::Admin,
        created_at: now,
        last_activity: now,
        expires_at: now + Duration::hours(24),
    };

    // One millisecond before 8 hours: still valid
    let just_before = now + Duration::hours(8) - Duration::milliseconds(1);
    assert!(session.is_valid(just_before), "session should be valid just before 8h");

    // Exactly 8 hours of inactivity: invalid (uses strict < comparison)
    let exactly_8h = now + Duration::hours(8);
    assert!(!session.is_valid(exactly_8h), "session must expire at exactly 8h inactivity");

    // One millisecond after 8 hours: still invalid
    let just_after = now + Duration::hours(8) + Duration::milliseconds(1);
    assert!(!session.is_valid(just_after), "session must be invalid past 8h inactivity");
}

#[tokio::test]
async fn session_valid_when_activity_refreshed() {
    let now = Utc::now();
    // Simulate a session where last_activity was refreshed 1 hour ago
    let session = Session {
        session_id: Uuid::new_v4(),
        user_id: Uuid::new_v4(),
        role: UserRole::Chef,
        created_at: now - Duration::hours(10),
        last_activity: now - Duration::hours(1),
        expires_at: now + Duration::hours(14),
    };

    // Even though session was created 10h ago, last activity was 1h ago → valid
    assert!(session.is_valid(now));
}

#[tokio::test]
async fn session_invalid_when_expired_despite_recent_activity() {
    let now = Utc::now();
    let session = Session {
        session_id: Uuid::new_v4(),
        user_id: Uuid::new_v4(),
        role: UserRole::Representative,
        created_at: now - Duration::hours(25),
        last_activity: now, // very recent activity
        expires_at: now - Duration::hours(1), // but already expired
    };

    assert!(!session.is_valid(now), "expired session must be invalid even with recent activity");
}

// ── Login with empty username/password (Req 1.2) ──────────────────────────

#[tokio::test]
async fn login_empty_username_returns_auth_error() {
    let svc = setup().await;

    let err = svc.login("", "somepassword").await.unwrap_err();
    match &err {
        AppError::Authentication { message } => {
            assert_eq!(message, "Invalid credentials");
            // Must not reveal which field is wrong
            assert!(!message.to_lowercase().contains("username"));
            assert!(!message.to_lowercase().contains("password"));
        }
        other => panic!("Expected Authentication error, got: {other:?}"),
    }
}

#[tokio::test]
async fn login_empty_password_returns_auth_error() {
    let svc = setup().await;
    // Create a real user first so the username lookup succeeds
    svc.create_user("testuser", "realpass", "Test User", UserRole::Admin)
        .await
        .unwrap();

    let err = svc.login("testuser", "").await.unwrap_err();
    match &err {
        AppError::Authentication { message } => {
            assert_eq!(message, "Invalid credentials");
            assert!(!message.to_lowercase().contains("username"));
            assert!(!message.to_lowercase().contains("password"));
        }
        other => panic!("Expected Authentication error, got: {other:?}"),
    }
}

#[tokio::test]
async fn login_both_empty_returns_auth_error() {
    let svc = setup().await;

    let err = svc.login("", "").await.unwrap_err();
    match &err {
        AppError::Authentication { message } => {
            assert_eq!(message, "Invalid credentials");
        }
        other => panic!("Expected Authentication error, got: {other:?}"),
    }
}

// ── Role update for non-existent user (Req 2.3) ──────────────────────────

#[tokio::test]
async fn update_role_nonexistent_user_returns_validation_error() {
    let svc = setup().await;
    let fake_id = Uuid::new_v4();

    let err = svc
        .update_user_role(fake_id, UserRole::Chef)
        .await
        .unwrap_err();

    match &err {
        AppError::Validation { field, message } => {
            assert_eq!(field, "user_id");
            assert!(
                message.contains(&fake_id.to_string()),
                "error should reference the missing user ID"
            );
        }
        other => panic!("Expected Validation error, got: {other:?}"),
    }
}

#[tokio::test]
async fn update_role_nonexistent_user_each_role_variant() {
    let svc = setup().await;
    let fake_id = Uuid::new_v4();

    for role in [UserRole::Admin, UserRole::Chef, UserRole::Representative] {
        let err = svc.update_user_role(fake_id, role).await.unwrap_err();
        assert!(
            matches!(err, AppError::Validation { .. }),
            "update_user_role for non-existent user should fail regardless of target role"
        );
    }
}

// ── Argon2 hash verification (Req 1.1) ────────────────────────────────────

#[test]
fn argon2_correct_password_verifies() {
    let password = "my_secure_password_123!";
    let salt = SaltString::generate(&mut rand_core::OsRng);
    let hash = Argon2::default()
        .hash_password(password.as_bytes(), &salt)
        .expect("hashing should succeed")
        .to_string();

    let parsed = PasswordHash::new(&hash).expect("parse hash");
    assert!(
        Argon2::default()
            .verify_password(password.as_bytes(), &parsed)
            .is_ok(),
        "correct password must verify successfully"
    );
}

#[test]
fn argon2_wrong_password_fails_verification() {
    let password = "correct_password";
    let salt = SaltString::generate(&mut rand_core::OsRng);
    let hash = Argon2::default()
        .hash_password(password.as_bytes(), &salt)
        .expect("hashing should succeed")
        .to_string();

    let parsed = PasswordHash::new(&hash).expect("parse hash");
    assert!(
        Argon2::default()
            .verify_password(b"wrong_password", &parsed)
            .is_err(),
        "wrong password must fail verification"
    );
}

#[test]
fn argon2_empty_password_hashes_and_verifies() {
    // Empty string is a valid input for argon2 — it should hash and verify
    let password = "";
    let salt = SaltString::generate(&mut rand_core::OsRng);
    let hash = Argon2::default()
        .hash_password(password.as_bytes(), &salt)
        .expect("hashing empty password should succeed")
        .to_string();

    let parsed = PasswordHash::new(&hash).expect("parse hash");
    assert!(
        Argon2::default()
            .verify_password(b"", &parsed)
            .is_ok(),
        "empty password should verify against its own hash"
    );
    assert!(
        Argon2::default()
            .verify_password(b"notempty", &parsed)
            .is_err(),
        "non-empty password must not verify against empty password hash"
    );
}

#[test]
fn argon2_different_salts_produce_different_hashes() {
    let password = "same_password";
    let salt1 = SaltString::generate(&mut rand_core::OsRng);
    let salt2 = SaltString::generate(&mut rand_core::OsRng);

    let hash1 = Argon2::default()
        .hash_password(password.as_bytes(), &salt1)
        .unwrap()
        .to_string();
    let hash2 = Argon2::default()
        .hash_password(password.as_bytes(), &salt2)
        .unwrap()
        .to_string();

    assert_ne!(hash1, hash2, "different salts must produce different hashes");

    // But both should verify with the original password
    let parsed1 = PasswordHash::new(&hash1).unwrap();
    let parsed2 = PasswordHash::new(&hash2).unwrap();
    assert!(Argon2::default().verify_password(password.as_bytes(), &parsed1).is_ok());
    assert!(Argon2::default().verify_password(password.as_bytes(), &parsed2).is_ok());
}
