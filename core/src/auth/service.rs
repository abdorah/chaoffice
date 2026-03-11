use std::collections::HashMap;
use std::sync::Arc;

use argon2::password_hash::{PasswordHash, PasswordHasher, PasswordVerifier, SaltString};
use argon2::Argon2;
use casbin::Enforcer;
use chrono::{Duration, Utc};
use sqlx::SqlitePool;
use tokio::sync::Mutex;
use uuid::Uuid;

use crate::auth::rbac;
use crate::error::{AppError, AppResult};
use crate::models::domain::{AppUser, Session, UserRole};

/// Concrete implementation of the AuthService.
pub struct AuthServiceImpl {
    pool: SqlitePool,
    enforcer: Arc<Mutex<Enforcer>>,
    sessions: Arc<Mutex<HashMap<Uuid, Session>>>,
}

impl AuthServiceImpl {
    pub fn new(pool: SqlitePool, enforcer: Arc<Mutex<Enforcer>>) -> Self {
        Self {
            pool,
            enforcer,
            sessions: Arc::new(Mutex::new(HashMap::new())),
        }
    }

    /// Authenticate a user and create a session.
    ///
    /// Returns a generic "Invalid credentials" error for both wrong username
    /// and wrong password (Req 1.2 — don't reveal which field is wrong).
    pub async fn login(&self, username: &str, password: &str) -> AppResult<Session> {
        let invalid = || AppError::Authentication {
            message: "Invalid credentials".to_string(),
        };

        // Look up user by username
        let row = sqlx::query_as::<_, UserRow>(
            "SELECT id, username, full_name, role, password_hash FROM users WHERE username = ?",
        )
        .bind(username)
        .fetch_optional(&self.pool)
        .await?;

        let user_row = row.ok_or_else(invalid)?;

        // Verify password with argon2
        let parsed_hash =
            PasswordHash::new(&user_row.password_hash).map_err(|_| invalid())?;
        Argon2::default()
            .verify_password(password.as_bytes(), &parsed_hash)
            .map_err(|_| invalid())?;

        let role = UserRole::from_str_value(&user_row.role).ok_or_else(|| {
            AppError::Unknown(format!("Invalid role in database: {}", user_row.role))
        })?;

        let user_id =
            Uuid::parse_str(&user_row.id).map_err(|e| AppError::Unknown(e.to_string()))?;

        let now = Utc::now();
        let session = Session {
            session_id: Uuid::new_v4(),
            user_id,
            role,
            created_at: now,
            last_activity: now,
            expires_at: now + Duration::hours(24),
        };

        self.sessions
            .lock()
            .await
            .insert(session.session_id, session.clone());

        Ok(session)
    }

    /// Remove a session (logout).
    pub async fn logout(&self, session_id: Uuid) -> AppResult<()> {
        self.sessions.lock().await.remove(&session_id);
        Ok(())
    }

    /// Create a new user with field validation and argon2 password hashing.
    pub async fn create_user(
        &self,
        username: &str,
        password: &str,
        full_name: &str,
        role: UserRole,
    ) -> AppResult<AppUser> {
        // Validate required fields are non-empty
        validate_field("username", username)?;
        validate_field("password", password)?;
        validate_field("full_name", full_name)?;

        // Hash password with argon2
        let salt = SaltString::generate(&mut rand_core::OsRng);
        let password_hash = Argon2::default()
            .hash_password(password.as_bytes(), &salt)
            .map_err(|e| AppError::Unknown(format!("Password hashing failed: {e}")))?
            .to_string();

        let id = Uuid::new_v4();
        let now = Utc::now().to_rfc3339();
        let role_str = role.to_string();
        let id_str = id.to_string();

        sqlx::query(
            "INSERT INTO users (id, username, full_name, role, password_hash, sync_status, updated_at, created_at)
             VALUES (?, ?, ?, ?, ?, 'Synced', ?, ?)",
        )
        .bind(&id_str)
        .bind(username)
        .bind(full_name)
        .bind(&role_str)
        .bind(&password_hash)
        .bind(&now)
        .bind(&now)
        .execute(&self.pool)
        .await
        .map_err(|e| {
            if let sqlx::Error::Database(ref db_err) = e {
                if db_err.message().contains("UNIQUE") {
                    return AppError::Duplicate {
                        field: "username".to_string(),
                        value: username.to_string(),
                    };
                }
            }
            AppError::Database(e)
        })?;

        Ok(AppUser {
            id,
            username: username.to_string(),
            full_name: full_name.to_string(),
            role,
            password_hash,
        })
    }

    /// Update a user's role in the database.
    /// Per Req 2.3, the new permissions apply on the user's next login.
    pub async fn update_user_role(&self, user_id: Uuid, new_role: UserRole) -> AppResult<()> {
        let role_str = new_role.to_string();
        let now = Utc::now().to_rfc3339();

        let result = sqlx::query(
            "UPDATE users SET role = ?, updated_at = ? WHERE id = ?",
        )
        .bind(&role_str)
        .bind(&now)
        .bind(user_id.to_string())
        .execute(&self.pool)
        .await?;

        if result.rows_affected() == 0 {
            return Err(AppError::Validation {
                field: "user_id".to_string(),
                message: format!("User {user_id} not found"),
            });
        }

        Ok(())
    }

    /// Check whether a role has permission to perform an action on a resource.
    /// Delegates to the Casbin-RS enforcer.
    pub async fn check_permission(
        &self,
        role: &UserRole,
        resource: &str,
        action: &str,
    ) -> AppResult<bool> {
        let mut enforcer = self.enforcer.lock().await;
        rbac::check_permission(&mut enforcer, role, resource, action)
    }
}

/// Validate that a field is non-empty after trimming.
fn validate_field(field: &str, value: &str) -> AppResult<()> {
    if value.trim().is_empty() {
        return Err(AppError::Validation {
            field: field.to_string(),
            message: format!("{field} is required"),
        });
    }
    Ok(())
}

/// SQLx row type for reading users from the database.
#[derive(sqlx::FromRow)]
#[allow(dead_code)]
struct UserRow {
    id: String,
    username: String,
    full_name: String,
    role: String,
    password_hash: String,
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::persistence::db;

    /// Helper: create an in-memory DB, run migrations, and build an AuthServiceImpl.
    async fn setup() -> AuthServiceImpl {
        let pool = db::init_db(":memory:").await.expect("DB init failed");
        let enforcer = rbac::init_enforcer("policies/model.conf", "policies/policy.csv")
            .await
            .expect("Casbin init failed");
        AuthServiceImpl::new(pool, Arc::new(Mutex::new(enforcer)))
    }

    #[tokio::test]
    async fn create_user_and_login() {
        let svc = setup().await;

        let user = svc
            .create_user("alice", "secret123", "Alice Smith", UserRole::Admin)
            .await
            .expect("create_user failed");

        assert_eq!(user.username, "alice");
        assert_eq!(user.full_name, "Alice Smith");
        assert_eq!(user.role, UserRole::Admin);

        let session = svc.login("alice", "secret123").await.expect("login failed");
        assert_eq!(session.user_id, user.id);
        assert_eq!(session.role, UserRole::Admin);
        assert!(session.is_valid(Utc::now()));
    }

    #[tokio::test]
    async fn login_wrong_password_returns_generic_error() {
        let svc = setup().await;
        svc.create_user("bob", "correct", "Bob", UserRole::Chef)
            .await
            .unwrap();

        let err = svc.login("bob", "wrong").await.unwrap_err();
        match &err {
            AppError::Authentication { message } => {
                assert_eq!(message, "Invalid credentials");
                // Req 1.2: must NOT reveal which field is wrong
                assert!(!message.to_lowercase().contains("password"));
                assert!(!message.to_lowercase().contains("username"));
            }
            other => panic!("Expected Authentication error, got: {other:?}"),
        }
    }

    #[tokio::test]
    async fn login_nonexistent_user_returns_generic_error() {
        let svc = setup().await;

        let err = svc.login("nobody", "pass").await.unwrap_err();
        match &err {
            AppError::Authentication { message } => {
                assert_eq!(message, "Invalid credentials");
            }
            other => panic!("Expected Authentication error, got: {other:?}"),
        }
    }

    #[tokio::test]
    async fn create_user_rejects_empty_fields() {
        let svc = setup().await;

        let err = svc
            .create_user("", "pass", "Name", UserRole::Admin)
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::Validation { field, .. } if field == "username"));

        let err = svc
            .create_user("user", "", "Name", UserRole::Admin)
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::Validation { field, .. } if field == "password"));

        let err = svc
            .create_user("user", "pass", "", UserRole::Admin)
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::Validation { field, .. } if field == "full_name"));
    }

    #[tokio::test]
    async fn create_user_rejects_duplicate_username() {
        let svc = setup().await;
        svc.create_user("dup", "pass", "First", UserRole::Admin)
            .await
            .unwrap();

        let err = svc
            .create_user("dup", "pass2", "Second", UserRole::Chef)
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::Duplicate { field, .. } if field == "username"));
    }

    #[tokio::test]
    async fn update_user_role_succeeds() {
        let svc = setup().await;
        let user = svc
            .create_user("carol", "pass", "Carol", UserRole::Chef)
            .await
            .unwrap();

        svc.update_user_role(user.id, UserRole::Admin)
            .await
            .expect("update_user_role failed");

        // Verify: login should still work, and the new role is reflected
        // (Req 2.3: role update applies on next login)
        let session = svc.login("carol", "pass").await.unwrap();
        assert_eq!(session.role, UserRole::Admin);
    }

    #[tokio::test]
    async fn update_role_nonexistent_user_fails() {
        let svc = setup().await;
        let err = svc
            .update_user_role(Uuid::new_v4(), UserRole::Admin)
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }

    #[tokio::test]
    async fn check_permission_delegates_to_casbin() {
        let svc = setup().await;

        assert!(svc
            .check_permission(&UserRole::Admin, "users", "manage")
            .await
            .unwrap());
        assert!(!svc
            .check_permission(&UserRole::Chef, "users", "manage")
            .await
            .unwrap());
        assert!(svc
            .check_permission(&UserRole::Representative, "sales", "create")
            .await
            .unwrap());
    }

    #[tokio::test]
    async fn logout_removes_session() {
        let svc = setup().await;
        svc.create_user("dave", "pass", "Dave", UserRole::Admin)
            .await
            .unwrap();

        let session = svc.login("dave", "pass").await.unwrap();
        let sid = session.session_id;

        // Session exists
        assert!(svc.sessions.lock().await.contains_key(&sid));

        svc.logout(sid).await.unwrap();

        // Session removed
        assert!(!svc.sessions.lock().await.contains_key(&sid));
    }

    #[tokio::test]
    async fn session_validity_respects_inactivity_timeout() {
        let now = Utc::now();
        let session = Session {
            session_id: Uuid::new_v4(),
            user_id: Uuid::new_v4(),
            role: UserRole::Admin,
            created_at: now,
            last_activity: now,
            expires_at: now + Duration::hours(24),
        };

        // Valid right now
        assert!(session.is_valid(now));

        // Valid at 7h59m of inactivity
        assert!(session.is_valid(now + Duration::minutes(479)));

        // Invalid at exactly 8 hours of inactivity
        assert!(!session.is_valid(now + Duration::hours(8)));

        // Invalid past 8 hours
        assert!(!session.is_valid(now + Duration::hours(9)));
    }

    #[tokio::test]
    async fn session_validity_respects_expiry() {
        let now = Utc::now();
        let session = Session {
            session_id: Uuid::new_v4(),
            user_id: Uuid::new_v4(),
            role: UserRole::Admin,
            created_at: now,
            last_activity: now + Duration::hours(23), // recent activity
            expires_at: now + Duration::hours(24),
        };

        // Past expiry even with recent activity
        assert!(!session.is_valid(now + Duration::hours(25)));
    }
}
