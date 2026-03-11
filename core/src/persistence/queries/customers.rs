use sqlx::SqlitePool;

use crate::error::AppResult;

/// Row type matching the `customers` table schema.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct CustomerRow {
    pub id: String,
    pub name: String,
    pub city: String,
    pub mobile: String,
    pub reliability_rating: i32,
    pub sync_status: String,
    pub updated_at: String,
}

/// Fetch all customers.
pub async fn list_all(pool: &SqlitePool) -> AppResult<Vec<CustomerRow>> {
    let rows = sqlx::query_as::<_, CustomerRow>(
        "SELECT id, name, city, mobile, reliability_rating, sync_status, updated_at FROM customers ORDER BY name ASC",
    )
    .fetch_all(pool)
    .await?;

    Ok(rows)
}

/// Fetch a single customer by id. Returns `None` if not found.
pub async fn get_by_id(pool: &SqlitePool, id: &str) -> AppResult<Option<CustomerRow>> {
    let row = sqlx::query_as::<_, CustomerRow>(
        "SELECT id, name, city, mobile, reliability_rating, sync_status, updated_at FROM customers WHERE id = ?",
    )
    .bind(id)
    .fetch_optional(pool)
    .await?;

    Ok(row)
}

/// Insert a new customer. Returns a database error if mobile is not unique.
pub async fn insert(
    pool: &SqlitePool,
    id: &str,
    name: &str,
    city: &str,
    mobile: &str,
    now: &str,
) -> AppResult<()> {
    sqlx::query(
        "INSERT INTO customers (id, name, city, mobile, reliability_rating, sync_status, updated_at)
         VALUES (?, ?, ?, ?, 0, 'Synced', ?)",
    )
    .bind(id)
    .bind(name)
    .bind(city)
    .bind(mobile)
    .bind(now)
    .execute(pool)
    .await?;

    Ok(())
}

/// Update a customer's reliability rating.
pub async fn update_rating(
    pool: &SqlitePool,
    id: &str,
    rating: i32,
    now: &str,
) -> AppResult<u64> {
    let result = sqlx::query(
        "UPDATE customers SET reliability_rating = ?, updated_at = ? WHERE id = ?",
    )
    .bind(rating)
    .bind(now)
    .bind(id)
    .execute(pool)
    .await?;

    Ok(result.rows_affected())
}

/// Search customers by name, city, or mobile using SQL LIKE.
pub async fn search(pool: &SqlitePool, query: &str) -> AppResult<Vec<CustomerRow>> {
    let pattern = format!("%{query}%");
    let rows = sqlx::query_as::<_, CustomerRow>(
        "SELECT id, name, city, mobile, reliability_rating, sync_status, updated_at
         FROM customers
         WHERE name LIKE ? OR city LIKE ? OR mobile LIKE ?
         ORDER BY name ASC",
    )
    .bind(&pattern)
    .bind(&pattern)
    .bind(&pattern)
    .fetch_all(pool)
    .await?;

    Ok(rows)
}
