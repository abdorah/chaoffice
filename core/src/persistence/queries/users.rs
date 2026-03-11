use sqlx::SqlitePool;

use crate::error::{AppError, AppResult};

/// Row type matching the `users` table schema.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct UserRow {
    pub id: String,
    pub username: String,
    pub full_name: String,
    pub role: String,
    pub password_hash: String,
    pub last_login_at: Option<String>,
    pub session_expires_at: Option<String>,
    pub sync_status: String,
    pub updated_at: String,
    pub created_at: String,
}

/// Insert a new user into the `users` table.
pub async fn insert_user(
    pool: &SqlitePool,
    id: &str,
    username: &str,
    full_name: &str,
    role: &str,
    password_hash: &str,
) -> AppResult<()> {
    let now = chrono::Utc::now().to_rfc3339();

    sqlx::query(
        "INSERT INTO users (id, username, full_name, role, password_hash, sync_status, updated_at, created_at)
         VALUES (?, ?, ?, ?, ?, 'Synced', ?, ?)",
    )
    .bind(id)
    .bind(username)
    .bind(full_name)
    .bind(role)
    .bind(password_hash)
    .bind(&now)
    .bind(&now)
    .execute(pool)
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

    Ok(())
}

/// Fetch a user by username. Returns `None` if not found.
pub async fn get_user_by_username(
    pool: &SqlitePool,
    username: &str,
) -> AppResult<Option<UserRow>> {
    let row = sqlx::query_as::<_, UserRow>("SELECT * FROM users WHERE username = ?")
        .bind(username)
        .fetch_optional(pool)
        .await?;

    Ok(row)
}

/// Fetch a user by id. Returns `None` if not found.
pub async fn get_user_by_id(pool: &SqlitePool, id: &str) -> AppResult<Option<UserRow>> {
    let row = sqlx::query_as::<_, UserRow>("SELECT * FROM users WHERE id = ?")
        .bind(id)
        .fetch_optional(pool)
        .await?;

    Ok(row)
}

/// Update a user's role. Returns `true` if a row was updated, `false` if the user was not found.
pub async fn update_role(
    pool: &SqlitePool,
    user_id: &str,
    new_role: &str,
) -> AppResult<bool> {
    let now = chrono::Utc::now().to_rfc3339();

    let result = sqlx::query("UPDATE users SET role = ?, updated_at = ? WHERE id = ?")
        .bind(new_role)
        .bind(&now)
        .bind(user_id)
        .execute(pool)
        .await?;

    Ok(result.rows_affected() > 0)
}

/// List all users.
pub async fn list_users(pool: &SqlitePool) -> AppResult<Vec<UserRow>> {
    let rows = sqlx::query_as::<_, UserRow>("SELECT * FROM users ORDER BY created_at ASC")
        .fetch_all(pool)
        .await?;

    Ok(rows)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::persistence::db;

    async fn setup_pool() -> SqlitePool {
        db::init_db(":memory:").await.expect("DB init failed")
    }

    #[tokio::test]
    async fn insert_and_get_by_username() {
        let pool = setup_pool().await;

        insert_user(&pool, "id-1", "alice", "Alice Smith", "Admin", "hash123")
            .await
            .expect("insert_user failed");

        let row = get_user_by_username(&pool, "alice")
            .await
            .expect("query failed")
            .expect("user not found");

        assert_eq!(row.id, "id-1");
        assert_eq!(row.username, "alice");
        assert_eq!(row.full_name, "Alice Smith");
        assert_eq!(row.role, "Admin");
        assert_eq!(row.password_hash, "hash123");
        assert_eq!(row.sync_status, "Synced");
    }

    #[tokio::test]
    async fn insert_and_get_by_id() {
        let pool = setup_pool().await;

        insert_user(&pool, "id-2", "bob", "Bob Jones", "Chef", "hash456")
            .await
            .unwrap();

        let row = get_user_by_id(&pool, "id-2")
            .await
            .unwrap()
            .expect("user not found");

        assert_eq!(row.username, "bob");
        assert_eq!(row.role, "Chef");
    }

    #[tokio::test]
    async fn get_nonexistent_user_returns_none() {
        let pool = setup_pool().await;

        assert!(get_user_by_username(&pool, "nobody").await.unwrap().is_none());
        assert!(get_user_by_id(&pool, "no-id").await.unwrap().is_none());
    }

    #[tokio::test]
    async fn insert_duplicate_username_returns_error() {
        let pool = setup_pool().await;

        insert_user(&pool, "id-a", "dup", "First", "Admin", "h1")
            .await
            .unwrap();

        let err = insert_user(&pool, "id-b", "dup", "Second", "Chef", "h2")
            .await
            .unwrap_err();

        assert!(matches!(err, AppError::Duplicate { field, .. } if field == "username"));
    }

    #[tokio::test]
    async fn update_role_succeeds() {
        let pool = setup_pool().await;

        insert_user(&pool, "id-3", "carol", "Carol", "Chef", "hash")
            .await
            .unwrap();

        let updated = update_role(&pool, "id-3", "Admin").await.unwrap();
        assert!(updated);

        let row = get_user_by_id(&pool, "id-3").await.unwrap().unwrap();
        assert_eq!(row.role, "Admin");
    }

    #[tokio::test]
    async fn update_role_nonexistent_returns_false() {
        let pool = setup_pool().await;

        let updated = update_role(&pool, "no-such-id", "Admin").await.unwrap();
        assert!(!updated);
    }

    #[tokio::test]
    async fn list_users_returns_all() {
        let pool = setup_pool().await;

        insert_user(&pool, "id-x", "user1", "User One", "Admin", "h1")
            .await
            .unwrap();
        insert_user(&pool, "id-y", "user2", "User Two", "Chef", "h2")
            .await
            .unwrap();
        insert_user(&pool, "id-z", "user3", "User Three", "Representative", "h3")
            .await
            .unwrap();

        let users = list_users(&pool).await.unwrap();
        assert_eq!(users.len(), 3);
        assert_eq!(users[0].username, "user1");
        assert_eq!(users[1].username, "user2");
        assert_eq!(users[2].username, "user3");
    }

    #[tokio::test]
    async fn list_users_empty_db() {
        let pool = setup_pool().await;

        let users = list_users(&pool).await.unwrap();
        assert!(users.is_empty());
    }
}
