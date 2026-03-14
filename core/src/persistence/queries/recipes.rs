use sqlx::{Executor, SqlitePool};

use crate::error::{AppError, AppResult};
use crate::models::domain::Pagination;

/// Row type for the `recipes` table.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct RecipeRow {
    pub id: String,
    pub name: String,
    pub finished_good_id: String,
    pub sync_status: String,
    pub updated_at: String,
}

/// Row type for the `recipe_ingredients` table joined with raw_materials.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct RecipeIngredientRow {
    pub id: String,
    pub recipe_id: String,
    pub raw_material_id: String,
    pub raw_material_name: String,
    pub required_quantity: f64,
}

/// Fetch all recipes.
pub async fn list_all(pool: &SqlitePool, pagination: &Pagination) -> AppResult<Vec<RecipeRow>> {
    let sql = format!(
        "SELECT id, name, finished_good_id, sync_status, updated_at FROM recipes ORDER BY name ASC LIMIT {} OFFSET {}",
        pagination.limit, pagination.offset
    );
    let rows = sqlx::query_as::<_, RecipeRow>(&sql)
        .fetch_all(pool)
        .await?;
    Ok(rows)
}

/// Fetch a single recipe by id.
pub async fn get_by_id(pool: &SqlitePool, id: &str) -> AppResult<Option<RecipeRow>> {
    let row = sqlx::query_as::<_, RecipeRow>(
        "SELECT id, name, finished_good_id, sync_status, updated_at FROM recipes WHERE id = ?",
    )
    .bind(id)
    .fetch_optional(pool)
    .await?;
    Ok(row)
}

/// Fetch ingredients for a recipe, joined with raw_materials for the name.
pub async fn get_ingredients(pool: &SqlitePool, recipe_id: &str) -> AppResult<Vec<RecipeIngredientRow>> {
    let rows = sqlx::query_as::<_, RecipeIngredientRow>(
        "SELECT ri.id, ri.recipe_id, ri.raw_material_id, rm.name AS raw_material_name, ri.required_quantity
         FROM recipe_ingredients ri
         JOIN raw_materials rm ON rm.id = ri.raw_material_id
         WHERE ri.recipe_id = ?
         ORDER BY rm.name ASC",
    )
    .bind(recipe_id)
    .fetch_all(pool)
    .await?;
    Ok(rows)
}

/// Insert a recipe row. Use within a transaction.
pub async fn insert_recipe<'e, E>(executor: E, id: &str, name: &str, finished_good_id: &str, now: &str) -> AppResult<()>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    sqlx::query(
        "INSERT INTO recipes (id, name, finished_good_id, sync_status, updated_at) VALUES (?, ?, ?, 'Synced', ?)",
    )
    .bind(id)
    .bind(name)
    .bind(finished_good_id)
    .bind(now)
    .execute(executor)
    .await?;
    Ok(())
}

/// Insert a single recipe ingredient row. Use within a transaction.
pub async fn insert_ingredient<'e, E>(
    executor: E,
    id: &str,
    recipe_id: &str,
    raw_material_id: &str,
    required_quantity: f64,
) -> AppResult<()>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    sqlx::query(
        "INSERT INTO recipe_ingredients (id, recipe_id, raw_material_id, required_quantity) VALUES (?, ?, ?, ?)",
    )
    .bind(id)
    .bind(recipe_id)
    .bind(raw_material_id)
    .bind(required_quantity)
    .execute(executor)
    .await?;
    Ok(())
}

/// Delete all ingredients for a recipe. Use within a transaction.
pub async fn delete_ingredients<'e, E>(executor: E, recipe_id: &str) -> AppResult<()>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    sqlx::query("DELETE FROM recipe_ingredients WHERE recipe_id = ?")
        .bind(recipe_id)
        .execute(executor)
        .await?;
    Ok(())
}

/// Update a recipe's name and finished_good_id. Use within a transaction.
pub async fn update_recipe<'e, E>(executor: E, id: &str, name: &str, finished_good_id: &str, now: &str) -> AppResult<()>
where
    E: Executor<'e, Database = sqlx::Sqlite>,
{
    let result = sqlx::query(
        "UPDATE recipes SET name = ?, finished_good_id = ?, updated_at = ? WHERE id = ?",
    )
    .bind(name)
    .bind(finished_good_id)
    .bind(now)
    .bind(id)
    .execute(executor)
    .await?;

    if result.rows_affected() == 0 {
        return Err(AppError::Validation {
            field: "recipe_id".to_string(),
            message: format!("Recipe {id} not found"),
        });
    }
    Ok(())
}

/// Delete a recipe by id. CASCADE will remove its ingredients.
pub async fn delete_recipe(pool: &SqlitePool, id: &str) -> AppResult<()> {
    let result = sqlx::query("DELETE FROM recipes WHERE id = ?")
        .bind(id)
        .execute(pool)
        .await?;

    if result.rows_affected() == 0 {
        return Err(AppError::Validation {
            field: "recipe_id".to_string(),
            message: format!("Recipe {id} not found"),
        });
    }
    Ok(())
}

/// Count production log entries that reference a given recipe.
pub async fn count_production_logs(pool: &SqlitePool, recipe_id: &str) -> AppResult<i64> {
    let row: (i64,) = sqlx::query_as(
        "SELECT COUNT(*) FROM production_logs WHERE recipe_id = ?",
    )
    .bind(recipe_id)
    .fetch_one(pool)
    .await?;
    Ok(row.0)
}

/// Get the finished good name for a recipe (via JOIN).
pub async fn get_finished_good_name(pool: &SqlitePool, finished_good_id: &str) -> AppResult<Option<String>> {
    let row: Option<(String,)> = sqlx::query_as(
        "SELECT name FROM finished_goods WHERE id = ?",
    )
    .bind(finished_good_id)
    .fetch_optional(pool)
    .await?;
    Ok(row.map(|r| r.0))
}
