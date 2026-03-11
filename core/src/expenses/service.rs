use std::collections::HashMap;

use chrono::{DateTime, Utc};
use sqlx::SqlitePool;
use uuid::Uuid;

use crate::error::{AppError, AppResult};
use crate::models::domain::{Expense, ExpenseCategory};
use crate::persistence::queries::expenses as expense_queries;
use crate::persistence::queries::wallets as wallet_queries;

/// Service for recording and querying expenses.
pub struct ExpenseServiceImpl {
    pool: SqlitePool,
}

impl ExpenseServiceImpl {
    pub fn new(pool: SqlitePool) -> Self {
        Self { pool }
    }

    /// Record an expense: validate inputs, debit wallet, store expense — all in one transaction.
    /// (Req 11.1, 11.2, 11.3)
    pub async fn record_expense(
        &self,
        description: &str,
        amount: f64,
        category: ExpenseCategory,
        wallet_id: Uuid,
        recorded_by: Uuid,
    ) -> AppResult<Expense> {
        // Validate description
        if description.trim().is_empty() {
            return Err(AppError::Validation {
                field: "description".to_string(),
                message: "Description must not be empty".to_string(),
            });
        }

        // Validate amount
        if amount <= 0.0 {
            return Err(AppError::Validation {
                field: "amount".to_string(),
                message: "Amount must be positive".to_string(),
            });
        }

        let now = Utc::now();
        let now_str = now.to_rfc3339();
        let expense_id = Uuid::new_v4();
        let category_str = category_to_str(&category);

        let mut tx = self.pool.begin().await?;

        // Fetch wallet, check balance (Req 11.3)
        let wallet = wallet_queries::get_by_id_in_tx(&mut *tx, &wallet_id.to_string())
            .await?
            .ok_or_else(|| AppError::Validation {
                field: "wallet_id".to_string(),
                message: format!("Wallet {wallet_id} not found"),
            })?;

        if wallet.current_balance < amount {
            return Err(AppError::InsufficientFunds {
                wallet_name: wallet.name.clone(),
                available: wallet.current_balance,
                requested: amount,
            });
        }

        // Debit wallet (Req 11.2)
        let new_balance = wallet.current_balance - amount;
        wallet_queries::update_balance_in_tx(
            &mut *tx,
            &wallet_id.to_string(),
            new_balance,
            &now_str,
        )
        .await?;

        // Log wallet transaction
        wallet_queries::insert_transaction_in_tx(
            &mut *tx,
            &Uuid::new_v4().to_string(),
            &wallet_id.to_string(),
            -amount,
            &format!("Expense: {description}"),
            Some(&expense_id.to_string()),
            &now_str,
            &now_str,
        )
        .await?;

        // Insert expense record (Req 11.1)
        expense_queries::insert_expense_in_tx(
            &mut *tx,
            &expense_id.to_string(),
            description,
            amount,
            category_str,
            &wallet_id.to_string(),
            &recorded_by.to_string(),
            &now_str,
            &now_str,
        )
        .await?;

        tx.commit().await?;

        Ok(Expense {
            id: expense_id,
            description: description.to_string(),
            amount,
            category,
            wallet_id,
            wallet_name: wallet.name,
            recorded_by,
            timestamp: now,
        })
    }

    /// Get expenses within a date range (Req 11.4).
    pub async fn get_expenses(
        &self,
        start_date: DateTime<Utc>,
        end_date: DateTime<Utc>,
    ) -> AppResult<Vec<Expense>> {
        let rows = expense_queries::get_by_date_range(
            &self.pool,
            &start_date.to_rfc3339(),
            &end_date.to_rfc3339(),
        )
        .await?;

        rows.into_iter().map(row_to_expense).collect()
    }

    /// Get expenses grouped by category within a date range (Req 11.4).
    pub async fn get_expenses_by_category(
        &self,
        start_date: DateTime<Utc>,
        end_date: DateTime<Utc>,
    ) -> AppResult<HashMap<ExpenseCategory, Vec<Expense>>> {
        let rows = expense_queries::get_by_date_range_with_category(
            &self.pool,
            &start_date.to_rfc3339(),
            &end_date.to_rfc3339(),
        )
        .await?;

        let mut map: HashMap<ExpenseCategory, Vec<Expense>> = HashMap::new();
        for row in rows {
            let expense = row_to_expense(row)?;
            map.entry(expense.category.clone()).or_default().push(expense);
        }

        Ok(map)
    }
}

/// Convert a category enum to its DB string representation.
fn category_to_str(category: &ExpenseCategory) -> &'static str {
    match category {
        ExpenseCategory::Purchase => "Purchase",
        ExpenseCategory::OperatingCost => "OperatingCost",
    }
}

/// Parse a category string from DB into the enum.
fn parse_category(s: &str) -> AppResult<ExpenseCategory> {
    match s {
        "Purchase" => Ok(ExpenseCategory::Purchase),
        "OperatingCost" => Ok(ExpenseCategory::OperatingCost),
        other => Err(AppError::Unknown(format!("Invalid expense category: {other}"))),
    }
}

/// Convert a persistence row to an Expense domain model.
fn row_to_expense(row: expense_queries::ExpenseWithWalletRow) -> AppResult<Expense> {
    let id = Uuid::parse_str(&row.id)
        .map_err(|e| AppError::Unknown(format!("Invalid UUID: {e}")))?;
    let wallet_id = Uuid::parse_str(&row.wallet_id)
        .map_err(|e| AppError::Unknown(format!("Invalid UUID: {e}")))?;
    let recorded_by = Uuid::parse_str(&row.recorded_by)
        .map_err(|e| AppError::Unknown(format!("Invalid UUID: {e}")))?;
    let timestamp: DateTime<Utc> = row
        .timestamp
        .parse()
        .map_err(|e| AppError::Unknown(format!("Invalid timestamp: {e}")))?;

    Ok(Expense {
        id,
        description: row.description,
        amount: row.amount,
        category: parse_category(&row.category)?,
        wallet_id,
        wallet_name: row.wallet_name,
        recorded_by,
        timestamp,
    })
}


#[cfg(test)]
mod tests {
    use super::*;
    use crate::persistence::db;
    use crate::persistence::queries::wallets as wallet_queries;
    use chrono::Duration;

    async fn setup() -> (SqlitePool, ExpenseServiceImpl) {
        let pool = db::init_db(":memory:").await.expect("DB init failed");
        let svc = ExpenseServiceImpl::new(pool.clone());
        (pool, svc)
    }

    async fn seed_wallet(pool: &SqlitePool, id: &str, name: &str, balance: f64) {
        let now = Utc::now().to_rfc3339();
        wallet_queries::insert_wallet(pool, id, name, "Cash", balance, &now)
            .await
            .expect("seed wallet failed");
    }

    async fn seed_user(pool: &SqlitePool, id: &str) {
        let now = Utc::now().to_rfc3339();
        sqlx::query(
            "INSERT INTO users (id, username, full_name, role, password_hash, sync_status, updated_at, created_at)
             VALUES (?, 'testuser', 'Test User', 'Representative', 'hash', 'Synced', ?, ?)",
        )
        .bind(id)
        .bind(&now)
        .bind(&now)
        .execute(pool)
        .await
        .expect("seed user failed");
    }

    // ── record_expense: success ────────────────────────────────────────

    #[tokio::test]
    async fn record_expense_success() {
        let (pool, svc) = setup().await;
        let wallet_id = Uuid::new_v4();
        let user_id = Uuid::new_v4();
        seed_wallet(&pool, &wallet_id.to_string(), "Cash Box", 500.0).await;
        seed_user(&pool, &user_id.to_string()).await;

        let expense = svc
            .record_expense("Office supplies", 100.0, ExpenseCategory::OperatingCost, wallet_id, user_id)
            .await
            .unwrap();

        assert_eq!(expense.description, "Office supplies");
        assert_eq!(expense.amount, 100.0);
        assert_eq!(expense.category, ExpenseCategory::OperatingCost);
        assert_eq!(expense.wallet_id, wallet_id);
        assert_eq!(expense.wallet_name, "Cash Box");
        assert_eq!(expense.recorded_by, user_id);
    }

    #[tokio::test]
    async fn record_expense_debits_wallet() {
        let (pool, svc) = setup().await;
        let wallet_id = Uuid::new_v4();
        let user_id = Uuid::new_v4();
        seed_wallet(&pool, &wallet_id.to_string(), "Cash Box", 500.0).await;
        seed_user(&pool, &user_id.to_string()).await;

        svc.record_expense("Milk purchase", 150.0, ExpenseCategory::Purchase, wallet_id, user_id)
            .await
            .unwrap();

        // Wallet should be 500 - 150 = 350
        let wallet = wallet_queries::get_by_id(&pool, &wallet_id.to_string()).await.unwrap().unwrap();
        assert_eq!(wallet.current_balance, 350.0);
    }

    #[tokio::test]
    async fn record_expense_logs_wallet_transaction() {
        let (pool, svc) = setup().await;
        let wallet_id = Uuid::new_v4();
        let user_id = Uuid::new_v4();
        seed_wallet(&pool, &wallet_id.to_string(), "Cash Box", 500.0).await;
        seed_user(&pool, &user_id.to_string()).await;

        svc.record_expense("Sugar", 50.0, ExpenseCategory::Purchase, wallet_id, user_id)
            .await
            .unwrap();

        let txns = wallet_queries::get_transactions(&pool, &wallet_id.to_string()).await.unwrap();
        assert_eq!(txns.len(), 1);
        assert_eq!(txns[0].amount, -50.0);
        assert!(txns[0].description.contains("Expense"));
    }

    // ── record_expense: insufficient funds ─────────────────────────────

    #[tokio::test]
    async fn record_expense_insufficient_funds_rejected() {
        let (pool, svc) = setup().await;
        let wallet_id = Uuid::new_v4();
        let user_id = Uuid::new_v4();
        seed_wallet(&pool, &wallet_id.to_string(), "Cash Box", 30.0).await;
        seed_user(&pool, &user_id.to_string()).await;

        let err = svc
            .record_expense("Expensive item", 100.0, ExpenseCategory::Purchase, wallet_id, user_id)
            .await
            .unwrap_err();

        match err {
            AppError::InsufficientFunds { wallet_name, available, requested } => {
                assert_eq!(wallet_name, "Cash Box");
                assert_eq!(available, 30.0);
                assert_eq!(requested, 100.0);
            }
            other => panic!("Expected InsufficientFunds, got: {other:?}"),
        }

        // Wallet unchanged
        let wallet = wallet_queries::get_by_id(&pool, &wallet_id.to_string()).await.unwrap().unwrap();
        assert_eq!(wallet.current_balance, 30.0);
    }

    // ── record_expense: validation ─────────────────────────────────────

    #[tokio::test]
    async fn record_expense_empty_description_rejected() {
        let (_pool, svc) = setup().await;
        let err = svc
            .record_expense("", 50.0, ExpenseCategory::Purchase, Uuid::new_v4(), Uuid::new_v4())
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::Validation { field, .. } if field == "description"));
    }

    #[tokio::test]
    async fn record_expense_whitespace_description_rejected() {
        let (_pool, svc) = setup().await;
        let err = svc
            .record_expense("   ", 50.0, ExpenseCategory::Purchase, Uuid::new_v4(), Uuid::new_v4())
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::Validation { field, .. } if field == "description"));
    }

    #[tokio::test]
    async fn record_expense_zero_amount_rejected() {
        let (_pool, svc) = setup().await;
        let err = svc
            .record_expense("Test", 0.0, ExpenseCategory::Purchase, Uuid::new_v4(), Uuid::new_v4())
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::Validation { field, .. } if field == "amount"));
    }

    #[tokio::test]
    async fn record_expense_negative_amount_rejected() {
        let (_pool, svc) = setup().await;
        let err = svc
            .record_expense("Test", -10.0, ExpenseCategory::Purchase, Uuid::new_v4(), Uuid::new_v4())
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::Validation { field, .. } if field == "amount"));
    }

    #[tokio::test]
    async fn record_expense_nonexistent_wallet_rejected() {
        let (pool, svc) = setup().await;
        let user_id = Uuid::new_v4();
        seed_user(&pool, &user_id.to_string()).await;

        let err = svc
            .record_expense("Test", 50.0, ExpenseCategory::Purchase, Uuid::new_v4(), user_id)
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }

    // ── get_expenses: date range filtering ─────────────────────────────

    #[tokio::test]
    async fn get_expenses_filters_by_date_range() {
        let (pool, svc) = setup().await;
        let wallet_id = Uuid::new_v4();
        let user_id = Uuid::new_v4();
        seed_wallet(&pool, &wallet_id.to_string(), "Cash Box", 5000.0).await;
        seed_user(&pool, &user_id.to_string()).await;

        // Record 3 expenses
        svc.record_expense("Expense 1", 10.0, ExpenseCategory::Purchase, wallet_id, user_id).await.unwrap();
        svc.record_expense("Expense 2", 20.0, ExpenseCategory::OperatingCost, wallet_id, user_id).await.unwrap();
        svc.record_expense("Expense 3", 30.0, ExpenseCategory::Purchase, wallet_id, user_id).await.unwrap();

        // Query with a wide range that includes all
        let start = Utc::now() - Duration::hours(1);
        let end = Utc::now() + Duration::hours(1);
        let expenses = svc.get_expenses(start, end).await.unwrap();
        assert_eq!(expenses.len(), 3);
    }

    #[tokio::test]
    async fn get_expenses_returns_empty_for_no_match() {
        let (pool, svc) = setup().await;
        let wallet_id = Uuid::new_v4();
        let user_id = Uuid::new_v4();
        seed_wallet(&pool, &wallet_id.to_string(), "Cash Box", 5000.0).await;
        seed_user(&pool, &user_id.to_string()).await;

        svc.record_expense("Expense 1", 10.0, ExpenseCategory::Purchase, wallet_id, user_id).await.unwrap();

        // Query a range in the past
        let start = Utc::now() - Duration::days(10);
        let end = Utc::now() - Duration::days(5);
        let expenses = svc.get_expenses(start, end).await.unwrap();
        assert!(expenses.is_empty());
    }

    // ── get_expenses_by_category: grouping ─────────────────────────────

    #[tokio::test]
    async fn get_expenses_by_category_groups_correctly() {
        let (pool, svc) = setup().await;
        let wallet_id = Uuid::new_v4();
        let user_id = Uuid::new_v4();
        seed_wallet(&pool, &wallet_id.to_string(), "Cash Box", 5000.0).await;
        seed_user(&pool, &user_id.to_string()).await;

        svc.record_expense("Milk", 100.0, ExpenseCategory::Purchase, wallet_id, user_id).await.unwrap();
        svc.record_expense("Sugar", 50.0, ExpenseCategory::Purchase, wallet_id, user_id).await.unwrap();
        svc.record_expense("Electricity", 200.0, ExpenseCategory::OperatingCost, wallet_id, user_id).await.unwrap();

        let start = Utc::now() - Duration::hours(1);
        let end = Utc::now() + Duration::hours(1);
        let grouped = svc.get_expenses_by_category(start, end).await.unwrap();

        assert_eq!(grouped.len(), 2);
        assert_eq!(grouped[&ExpenseCategory::Purchase].len(), 2);
        assert_eq!(grouped[&ExpenseCategory::OperatingCost].len(), 1);
        assert_eq!(grouped[&ExpenseCategory::OperatingCost][0].description, "Electricity");
    }

    #[tokio::test]
    async fn get_expenses_by_category_returns_empty_map_when_none() {
        let (_pool, svc) = setup().await;
        let start = Utc::now() - Duration::hours(1);
        let end = Utc::now() + Duration::hours(1);
        let grouped = svc.get_expenses_by_category(start, end).await.unwrap();
        assert!(grouped.is_empty());
    }

    // ── exact balance debit ────────────────────────────────────────────

    #[tokio::test]
    async fn record_expense_exact_balance_succeeds() {
        let (pool, svc) = setup().await;
        let wallet_id = Uuid::new_v4();
        let user_id = Uuid::new_v4();
        seed_wallet(&pool, &wallet_id.to_string(), "Cash Box", 75.0).await;
        seed_user(&pool, &user_id.to_string()).await;

        svc.record_expense("Full spend", 75.0, ExpenseCategory::OperatingCost, wallet_id, user_id)
            .await
            .unwrap();

        let wallet = wallet_queries::get_by_id(&pool, &wallet_id.to_string()).await.unwrap().unwrap();
        assert_eq!(wallet.current_balance, 0.0);
    }
}
