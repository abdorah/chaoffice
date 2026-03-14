use sqlx::SqlitePool;

use crate::error::AppResult;
use crate::models::domain::Pagination;

/// Row type matching the `customers` table schema with computed debt fields.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct CustomerRow {
    pub id: String,
    pub name: String,
    pub city: String,
    pub mobile: String,
    pub reliability_rating: i32,
    pub sync_status: String,
    pub updated_at: String,
    pub total_debt: i64,
    pub overdue_days: i32,
}


/// Fetch all customers with computed debt summaries.
pub async fn list_all(pool: &SqlitePool, pagination: &Pagination) -> AppResult<Vec<CustomerRow>> {
    let sql = format!(
        "SELECT c.id, c.name, c.city, c.mobile, c.reliability_rating, c.sync_status, c.updated_at,
                COALESCE(SUM(d.remaining_amount), 0) AS total_debt,
                COALESCE(MAX(CAST((julianday('now') - julianday(d.sale_date)) AS INTEGER)), 0) AS overdue_days
         FROM customers c
         LEFT JOIN debt_records d ON d.customer_id = c.id AND d.is_settled = 0
         GROUP BY c.id
         ORDER BY c.name ASC
         LIMIT {} OFFSET {}",
        pagination.limit, pagination.offset
    );
    let rows = sqlx::query_as::<_, CustomerRow>(&sql)
        .fetch_all(pool)
        .await?;

    Ok(rows)
}

/// Fetch a single customer by id with computed debt summary. Returns `None` if not found.
pub async fn get_by_id(pool: &SqlitePool, id: &str) -> AppResult<Option<CustomerRow>> {
    let row = sqlx::query_as::<_, CustomerRow>(
        "SELECT c.id, c.name, c.city, c.mobile, c.reliability_rating, c.sync_status, c.updated_at,
                COALESCE(SUM(d.remaining_amount), 0) AS total_debt,
                COALESCE(MAX(CAST((julianday('now') - julianday(d.sale_date)) AS INTEGER)), 0) AS overdue_days
         FROM customers c
         LEFT JOIN debt_records d ON d.customer_id = c.id AND d.is_settled = 0
         WHERE c.id = ?
         GROUP BY c.id",
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

/// Search customers by name, city, or mobile using SQL LIKE with computed debt summaries.
pub async fn search(pool: &SqlitePool, query: &str, pagination: &Pagination) -> AppResult<Vec<CustomerRow>> {
    let pattern = format!("%{query}%");
    let sql = format!(
        "SELECT c.id, c.name, c.city, c.mobile, c.reliability_rating, c.sync_status, c.updated_at,
                COALESCE(SUM(d.remaining_amount), 0) AS total_debt,
                COALESCE(MAX(CAST((julianday('now') - julianday(d.sale_date)) AS INTEGER)), 0) AS overdue_days
         FROM customers c
         LEFT JOIN debt_records d ON d.customer_id = c.id AND d.is_settled = 0
         WHERE c.name LIKE ? OR c.city LIKE ? OR c.mobile LIKE ?
         GROUP BY c.id
         ORDER BY c.name ASC
         LIMIT {} OFFSET {}",
        pagination.limit, pagination.offset
    );
    let rows = sqlx::query_as::<_, CustomerRow>(&sql)
        .bind(&pattern)
        .bind(&pattern)
        .bind(&pattern)
        .fetch_all(pool)
        .await?;

    Ok(rows)
}
