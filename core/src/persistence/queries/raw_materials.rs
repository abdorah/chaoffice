use sqlx::SqlitePool;

use crate::error::{AppError, AppResult};

/// Row type matching the `raw_materials` table schema.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct RawMaterialRow {
    pub id: String,
    pub name: String,
    pub unit: String,
    pub current_quantity: f64,
    pub last_updated: String,
    pub sync_status: String,
    pub updated_at: String,
}

/// Fetch all raw materials.
pub async fn list_all(pool: &SqlitePool) -> AppResult<Vec<RawMaterialRow>> {
    let rows = sqlx::query_as::<_, RawMaterialRow>(
        "SELECT id, name, unit, current_quantity, last_updated, sync_status, updated_at FROM raw_materials ORDER BY name ASC",
    )
    .fetch_all(pool)
    .await?;

    Ok(rows)
}

/// Fetch a single raw material by id. Returns `None` if not found.
pub async fn get_by_id(pool: &SqlitePool, id: &str) -> AppResult<Option<RawMaterialRow>> {
    let row = sqlx::query_as::<_, RawMaterialRow>(
        "SELECT id, name, unit, current_quantity, last_updated, sync_status, updated_at FROM raw_materials WHERE id = ?",
    )
    .bind(id)
    .fetch_optional(pool)
    .await?;

    Ok(row)
}

/// Increase a raw material's quantity by `amount`. Returns the updated row.
///
/// Fails with `Database` error if the material does not exist (0 rows affected).
pub async fn add_quantity(
    pool: &SqlitePool,
    id: &str,
    amount: f64,
    now: &str,
) -> AppResult<RawMaterialRow> {
    let result = sqlx::query(
        "UPDATE raw_materials SET current_quantity = current_quantity + ?, last_updated = ?, updated_at = ? WHERE id = ?",
    )
    .bind(amount)
    .bind(now)
    .bind(now)
    .bind(id)
    .execute(pool)
    .await?;

    if result.rows_affected() == 0 {
        return Err(AppError::Validation {
            field: "material_id".to_string(),
            message: format!("Raw material {id} not found"),
        });
    }

    get_by_id(pool, id)
        .await?
        .ok_or_else(|| AppError::Unknown("Material disappeared after update".to_string()))
}

/// Deduct `amount` from a raw material's quantity.
///
/// Returns `InsufficientStock` if the current quantity is less than `amount`.
pub async fn deduct_quantity(
    pool: &SqlitePool,
    id: &str,
    amount: f64,
    now: &str,
) -> AppResult<RawMaterialRow> {
    // Fetch current state to check stock and get the name for error messages
    let row = get_by_id(pool, id).await?.ok_or_else(|| AppError::Validation {
        field: "material_id".to_string(),
        message: format!("Raw material {id} not found"),
    })?;

    if row.current_quantity < amount {
        return Err(AppError::InsufficientStock {
            material_name: row.name,
            available: row.current_quantity,
            requested: amount,
        });
    }

    sqlx::query(
        "UPDATE raw_materials SET current_quantity = current_quantity - ?, last_updated = ?, updated_at = ? WHERE id = ?",
    )
    .bind(amount)
    .bind(now)
    .bind(now)
    .bind(id)
    .execute(pool)
    .await?;

    get_by_id(pool, id)
        .await?
        .ok_or_else(|| AppError::Unknown("Material disappeared after update".to_string()))
}

/// Insert a new raw material (used primarily in tests).
pub async fn insert(
    pool: &SqlitePool,
    id: &str,
    name: &str,
    unit: &str,
    current_quantity: f64,
    now: &str,
) -> AppResult<()> {
    sqlx::query(
        "INSERT INTO raw_materials (id, name, unit, current_quantity, last_updated, sync_status, updated_at)
         VALUES (?, ?, ?, ?, ?, 'Synced', ?)",
    )
    .bind(id)
    .bind(name)
    .bind(unit)
    .bind(current_quantity)
    .bind(now)
    .bind(now)
    .execute(pool)
    .await?;

    Ok(())
}
