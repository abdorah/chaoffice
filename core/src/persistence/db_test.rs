#[cfg(test)]
mod tests {
    use crate::persistence::db;

    #[tokio::test]
    async fn test_create_pool_in_memory() {
        let pool = db::create_pool(":memory:").await;
        assert!(pool.is_ok(), "Should create an in-memory pool");
    }

    #[tokio::test]
    async fn test_run_migrations_in_memory() {
        let pool = db::create_pool(":memory:").await.unwrap();
        let result = db::run_migrations(&pool).await;
        assert!(result.is_ok(), "Migrations should run successfully: {:?}", result.err());
    }

    #[tokio::test]
    async fn test_init_db_in_memory() {
        let pool = db::init_db(":memory:").await;
        assert!(pool.is_ok(), "init_db should succeed for in-memory DB");

        // Verify a known table exists after migrations
        let result = sqlx::query("SELECT name FROM sqlite_master WHERE type='table' AND name='users'")
            .fetch_optional(pool.as_ref().unwrap())
            .await;
        assert!(result.is_ok());
        assert!(result.unwrap().is_some(), "users table should exist after migrations");
    }

    #[tokio::test]
    async fn test_foreign_keys_enabled() {
        let pool = db::init_db(":memory:").await.unwrap();
        let row: (i32,) = sqlx::query_as("PRAGMA foreign_keys")
            .fetch_one(&pool)
            .await
            .unwrap();
        assert_eq!(row.0, 1, "Foreign keys should be enabled");
    }
}
