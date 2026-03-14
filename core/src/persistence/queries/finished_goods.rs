use sqlx::SqlitePool;

use crate::error::{AppError, AppResult};
use crate::models::domain::Pagination;

/// Row type matching the `finished_goods` table schema.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct FinishedGoodRow {
    pub id: String,
    pub name: String,
    pub current_quantity: f64,
    pub unit_price: i64,
    pub last_updated: String,
    pub sync_status: String,
    pub updated_at: String,
}

/// Fetch all finished goods.
pub async fn list_all(pool: &SqlitePool, pagination: &Pagination) -> AppResult<Vec<FinishedGoodRow>> {
    let sql = format!(
        "SELECT id, name, current_quantity, unit_price, last_updated, sync_status, updated_at FROM finished_goods ORDER BY name ASC LIMIT {} OFFSET {}",
        pagination.limit, pagination.offset
    );
    let rows = sqlx::query_as::<_, FinishedGoodRow>(&sql)
        .fetch_all(pool)
        .await?;

    Ok(rows)
}

/// Fetch a single finished good by id. Returns `None` if not found.
pub async fn get_by_id(pool: &SqlitePool, id: &str) -> AppResult<Option<FinishedGoodRow>> {
    let row = sqlx::query_as::<_, FinishedGoodRow>(
        "SELECT id, name, current_quantity, unit_price, last_updated, sync_status, updated_at FROM finished_goods WHERE id = ?",
    )
    .bind(id)
    .fetch_optional(pool)
    .await?;

    Ok(row)
}

/// Increase a finished good's quantity by `amount`. Returns the updated row.
pub async fn add_quantity(
    pool: &SqlitePool,
    id: &str,
    amount: f64,
    now: &str,
) -> AppResult<FinishedGoodRow> {
    let result = sqlx::query(
        "UPDATE finished_goods SET current_quantity = current_quantity + ?, last_updated = ?, updated_at = ? WHERE id = ?",
    )
    .bind(amount)
    .bind(now)
    .bind(now)
    .bind(id)
    .execute(pool)
    .await?;

    if result.rows_affected() == 0 {
        return Err(AppError::Validation {
            field: "good_id".to_string(),
            message: format!("Finished good {id} not found"),
        });
    }

    get_by_id(pool, id)
        .await?
        .ok_or_else(|| AppError::Unknown("Finished good disappeared after update".to_string()))
}

/// Deduct `amount` from a finished good's quantity atomically.
///
/// Uses a single UPDATE with a WHERE clause that checks `current_quantity >= amount`
/// to prevent race conditions (Req 4.3). If rows_affected == 0, distinguishes
/// NotFound vs InsufficientStock.
pub async fn deduct_quantity(
    pool: &SqlitePool,
    id: &str,
    amount: f64,
    now: &str,
) -> AppResult<FinishedGoodRow> {
    // Atomic check-and-deduct in a single statement
    let result = sqlx::query(
        "UPDATE finished_goods SET current_quantity = current_quantity - ?, last_updated = ?, updated_at = ? WHERE id = ? AND current_quantity >= ?",
    )
    .bind(amount)
    .bind(now)
    .bind(now)
    .bind(id)
    .bind(amount)
    .execute(pool)
    .await?;

    if result.rows_affected() == 0 {
        // Distinguish NotFound vs InsufficientStock
        let row = get_by_id(pool, id).await?;
        return match row {
            None => Err(AppError::NotFound {
                entity_type: "finished_good".to_string(),
                entity_id: id.to_string(),
            }),
            Some(existing) => Err(AppError::InsufficientStock {
                material_name: existing.name,
                available: existing.current_quantity as i64,
                requested: amount as i64,
            }),
        };
    }

    get_by_id(pool, id)
        .await?
        .ok_or_else(|| AppError::Unknown("Finished good disappeared after update".to_string()))
}

/// Insert a new finished good (used primarily in tests).
/// `unit_price` is in integer cents (e.g., 2500 = $25.00).
pub async fn insert(
    pool: &SqlitePool,
    id: &str,
    name: &str,
    current_quantity: f64,
    unit_price: i64,
    now: &str,
) -> AppResult<()> {
    sqlx::query(
        "INSERT INTO finished_goods (id, name, current_quantity, unit_price, last_updated, sync_status, updated_at)
         VALUES (?, ?, ?, ?, ?, 'Synced', ?)",
    )
    .bind(id)
    .bind(name)
    .bind(current_quantity)
    .bind(unit_price)
    .bind(now)
    .bind(now)
    .execute(pool)
    .await?;

    Ok(())
}
