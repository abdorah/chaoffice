use argon2::password_hash::{PasswordHash, PasswordHasher, PasswordVerifier, SaltString};
use argon2::Argon2;
use chrono::{DateTime, Duration, Utc};
use sqlx::SqlitePool;
use uuid::Uuid;

use crate::auth::rbac::PermissionTable;
use crate::error::{AppError, AppResult};
use crate::models::domain::{AppUser, Session, UserRole};
use crate::persistence::queries::sessions as session_queries;
use crate::persistence::queries::users as user_queries;

/// Concrete implementation of the AuthService.
pub struct AuthServiceImpl {
    pool: SqlitePool,
    permissions: PermissionTable,
}

impl AuthServiceImpl {
    pub fn new(pool: SqlitePool) -> Self {
        Self {
            pool,
            permissions: PermissionTable::new(),
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

        // Look up user by username (Req 12.3: use shared query layer)
        let user_row = user_queries::get_user_by_username(&self.pool, username)
            .await?
            .ok_or_else(invalid)?;

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
            crate::utils::parse_uuid("user", &user_row.id)?;

        let now = Utc::now();
        let session = Session {
            session_id: Uuid::new_v4(),
            user_id,
            role,
            created_at: now,
            last_activity: now,
            expires_at: now + Duration::hours(24),
        };

        // Persist session to SQLite (Req 10.1)
        session_queries::insert_session(
            &self.pool,
            &session.session_id.to_string(),
            &session.user_id.to_string(),
            &session.role.to_string(),
            &session.created_at.to_rfc3339(),
            &session.last_activity.to_rfc3339(),
            &session.expires_at.to_rfc3339(),
        )
        .await?;

        Ok(session)
    }

    /// Remove a session (logout).
    pub async fn logout(&self, session_id: Uuid) -> AppResult<()> {
        session_queries::delete_session(&self.pool, &session_id.to_string()).await
    }

    /// Retrieve a session by token. Returns None if not found.
    pub async fn get_session(&self, session_id: Uuid) -> AppResult<Option<Session>> {
        let row = session_queries::get_session(&self.pool, &session_id.to_string()).await?;
        match row {
            Some(r) => Ok(Some(self.row_to_session(&r)?)),
            None => Ok(None),
        }
    }

    /// Update the last_activity timestamp for a session (Req 10.2).
    pub async fn touch_session(&self, session_id: Uuid, now: DateTime<Utc>) -> AppResult<()> {
        session_queries::update_last_activity(
            &self.pool,
            &session_id.to_string(),
            &now.to_rfc3339(),
        )
        .await
    }

    /// Convert a SessionRow into a domain Session.
    fn row_to_session(&self, row: &session_queries::SessionRow) -> AppResult<Session> {
        let session_id = crate::utils::parse_uuid("session", &row.id)?;
        let user_id = crate::utils::parse_uuid("user", &row.user_id)?;
        let role = UserRole::from_str_value(&row.role).ok_or_else(|| {
            AppError::Unknown(format!("Invalid role in session: {}", row.role))
        })?;
        let created_at = crate::utils::parse_timestamp(&row.created_at)?;
        let last_activity = crate::utils::parse_timestamp(&row.last_activity)?;
        let expires_at = crate::utils::parse_timestamp(&row.expires_at)?;

        Ok(Session {
            session_id,
            user_id,
            role,
            created_at,
            last_activity,
            expires_at,
        })
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

        // Validate password strength (Req 11.1, 11.2)
        if password.len() < 8 {
            return Err(AppError::Validation {
                field: "password".to_string(),
                message: "Password must be at least 8 characters".to_string(),
            });
        }

        // Hash password with argon2
        let salt = SaltString::generate(&mut rand_core::OsRng);
        let password_hash = Argon2::default()
            .hash_password(password.as_bytes(), &salt)
            .map_err(|e| AppError::Unknown(format!("Password hashing failed: {e}")))?
            .to_string();

        let id = Uuid::new_v4();
        let role_str = role.to_string();
        let id_str = id.to_string();

        // Use shared query layer for user insertion (Req 12.3)
        user_queries::insert_user(
            &self.pool,
            &id_str,
            username,
            full_name,
            &role_str,
            &password_hash,
        )
        .await?;

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

        // Use shared query layer for role update (Req 12.3)
        let updated = user_queries::update_role(&self.pool, &user_id.to_string(), &role_str).await?;

        if !updated {
            return Err(AppError::Validation {
                field: "user_id".to_string(),
                message: format!("User {user_id} not found"),
            });
        }

        Ok(())
    }

    /// Check whether a role has permission to perform an action on a resource.
    pub async fn check_permission(
        &self,
        role: &UserRole,
        resource: &str,
        action: &str,
    ) -> AppResult<bool> {
        self.permissions.check_permission(role, resource, action)
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


#[cfg(test)]
mod tests {
    use super::*;
    use crate::persistence::db;

    /// Helper: create an in-memory DB, run migrations, and build an AuthServiceImpl.
    async fn setup() -> AuthServiceImpl {
        let pool = db::init_db(":memory:").await.expect("DB init failed");
        AuthServiceImpl::new(pool)
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
        svc.create_user("bob", "correct1", "Bob", UserRole::Chef)
            .await
            .unwrap();

        let err = svc.login("bob", "wrongpwd1").await.unwrap_err();
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
        svc.create_user("dup", "password1", "First", UserRole::Admin)
            .await
            .unwrap();

        let err = svc
            .create_user("dup", "password2", "Second", UserRole::Chef)
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::Duplicate { field, .. } if field == "username"));
    }

    #[tokio::test]
    async fn update_user_role_succeeds() {
        let svc = setup().await;
        let user = svc
            .create_user("carol", "password1", "Carol", UserRole::Chef)
            .await
            .unwrap();

        svc.update_user_role(user.id, UserRole::Admin)
            .await
            .expect("update_user_role failed");

        // Verify: login should still work, and the new role is reflected
        // (Req 2.3: role update applies on next login)
        let session = svc.login("carol", "password1").await.unwrap();
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
    async fn check_permission_uses_permission_table() {
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
        svc.create_user("dave", "password1", "Dave", UserRole::Admin)
            .await
            .unwrap();

        let session = svc.login("dave", "password1").await.unwrap();
        let sid = session.session_id;

        // Session exists in DB
        assert!(svc.get_session(sid).await.unwrap().is_some());

        svc.logout(sid).await.unwrap();

        // Session removed from DB
        assert!(svc.get_session(sid).await.unwrap().is_none());
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
