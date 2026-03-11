use sqlx::sqlite::{SqlitePool, SqlitePoolOptions};
use std::path::Path;

use crate::error::AppResult;

/// Creates and returns a SQLite connection pool for the given database path.
///
/// If the database file does not exist, SQLite will create it automatically
/// (via `create_if_missing`).
pub async fn create_pool(db_path: &str) -> AppResult<SqlitePool> {
    let connection_string = if db_path == ":memory:" {
        "sqlite::memory:".to_string()
    } else {
        // Ensure parent directory exists
        if let Some(parent) = Path::new(db_path).parent() {
            if !parent.as_os_str().is_empty() {
                std::fs::create_dir_all(parent).map_err(|e| {
                    crate::error::AppError::Unknown(format!(
                        "Failed to create database directory: {e}"
                    ))
                })?;
            }
        }
        format!("sqlite:{db_path}?mode=rwc")
    };

    let pool = SqlitePoolOptions::new()
        .max_connections(5)
        .connect(&connection_string)
        .await?;

    // Enable WAL mode and foreign keys for better concurrency and integrity
    sqlx::query("PRAGMA journal_mode=WAL;")
        .execute(&pool)
        .await?;
    sqlx::query("PRAGMA foreign_keys=ON;")
        .execute(&pool)
        .await?;

    Ok(pool)
}

/// Runs all pending SQLx migrations against the given pool.
///
/// Migrations are embedded at compile time from the `core/migrations/` directory.
pub async fn run_migrations(pool: &SqlitePool) -> AppResult<()> {
    sqlx::migrate!("./migrations")
        .run(pool)
        .await
        .map_err(|e| crate::error::AppError::Unknown(format!("Migration failed: {e}")))?;
    Ok(())
}

/// Convenience function: creates a pool and runs migrations in one step.
pub async fn init_db(db_path: &str) -> AppResult<SqlitePool> {
    let pool = create_pool(db_path).await?;
    run_migrations(&pool).await?;
    Ok(pool)
}
