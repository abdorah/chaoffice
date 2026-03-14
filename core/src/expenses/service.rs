use std::collections::HashMap;

use chrono::{DateTime, Utc};
use sqlx::SqlitePool;
use uuid::Uuid;

use crate::error::{AppError, AppResult};
use crate::models::domain::{Expense, ExpenseCategory, Pagination};
use crate::models::Money;
use crate::persistence::queries::expenses as expense_queries;
use crate::persistence::queries::users as user_queries;
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
        amount: Money,
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
        if amount.0 <= 0 {
            return Err(AppError::Validation {
                field: "amount".to_string(),
                message: "Amount must be positive".to_string(),
            });
        }

        // Validate recorded_by user exists (Req 15.1, 15.2)
        let user = user_queries::get_user_by_id(&self.pool, &recorded_by.to_string()).await?;
        if user.is_none() {
            return Err(AppError::NotFound {
                entity_type: "user".to_string(),
                entity_id: recorded_by.to_string(),
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

        let wallet_balance = Money(wallet.current_balance);
        if wallet_balance < amount {
            return Err(AppError::InsufficientFunds {
                wallet_name: wallet.name.clone(),
                available: wallet_balance,
                requested: amount,
            });
        }

        // Debit wallet atomically with balance check (Req 11.2, 7.3)
        let rows_affected = wallet_queries::atomic_debit_in_tx(
            &mut *tx,
            &wallet_id.to_string(),
            amount.0,
            &now_str,
        )
        .await?;

        if rows_affected == 0 {
            return Err(AppError::InsufficientFunds {
                wallet_name: wallet.name.clone(),
                available: wallet_balance,
                requested: amount,
            });
        }

        // Log wallet transaction
        wallet_queries::insert_transaction_in_tx(
            &mut *tx,
            &Uuid::new_v4().to_string(),
            &wallet_id.to_string(),
            -amount.0,
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
            amount.0,
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
        pagination: Option<Pagination>,
    ) -> AppResult<Vec<Expense>> {
        let pg = pagination.unwrap_or_default();
        let rows = expense_queries::get_by_date_range(
            &self.pool,
            &start_date.to_rfc3339(),
            &end_date.to_rfc3339(),
            &pg,
        )
        .await?;

        rows.into_iter().map(row_to_expense).collect()
    }

    /// Get expenses grouped by category within a date range (Req 11.4).
    pub async fn get_expenses_by_category(
        &self,
        start_date: DateTime<Utc>,
        end_date: DateTime<Utc>,
        pagination: Option<Pagination>,
    ) -> AppResult<HashMap<ExpenseCategory, Vec<Expense>>> {
        let pg = pagination.unwrap_or_default();
        let rows = expense_queries::get_by_date_range_with_category(
            &self.pool,
            &start_date.to_rfc3339(),
            &end_date.to_rfc3339(),
            &pg,
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
    let id = crate::utils::parse_uuid("expense", &row.id)?;
    let wallet_id = crate::utils::parse_uuid("wallet", &row.wallet_id)?;
    let recorded_by = crate::utils::parse_uuid("user", &row.recorded_by)?;
    let timestamp = crate::utils::parse_timestamp(&row.timestamp)?;

    Ok(Expense {
        id,
        description: row.description,
        amount: Money(row.amount),
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
    use crate::models::Money;
    use crate::persistence::db;
    use crate::persistence::queries::wallets as wallet_queries;
    use chrono::Duration;

    async fn setup() -> (SqlitePool, ExpenseServiceImpl) {
        let pool = db::init_db(":memory:").await.expect("DB init failed");
        let svc = ExpenseServiceImpl::new(pool.clone());
        (pool, svc)
    }

    async fn seed_wallet(pool: &SqlitePool, id: &str, name: &str, balance: i64) {
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
        seed_wallet(&pool, &wallet_id.to_string(), "Cash Box", 50000).await;
        seed_user(&pool, &user_id.to_string()).await;

        let expense = svc
            .record_expense("Office supplies", Money::from_f64(100.0), ExpenseCategory::OperatingCost, wallet_id, user_id)
            .await
            .unwrap();

        assert_eq!(expense.description, "Office supplies");
        assert_eq!(expense.amount, Money::from_f64(100.0));
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
        seed_wallet(&pool, &wallet_id.to_string(), "Cash Box", 50000).await;
        seed_user(&pool, &user_id.to_string()).await;

        svc.record_expense("Milk purchase", Money::from_f64(150.0), ExpenseCategory::Purchase, wallet_id, user_id)
            .await
            .unwrap();

        // Wallet should be 50000 - 15000 = 35000 cents ($500 - $150 = $350)
        let wallet = wallet_queries::get_by_id(&pool, &wallet_id.to_string()).await.unwrap().unwrap();
        assert_eq!(wallet.current_balance, 35000);
    }

    #[tokio::test]
    async fn record_expense_logs_wallet_transaction() {
        let (pool, svc) = setup().await;
        let wallet_id = Uuid::new_v4();
        let user_id = Uuid::new_v4();
        seed_wallet(&pool, &wallet_id.to_string(), "Cash Box", 50000).await;
        seed_user(&pool, &user_id.to_string()).await;

        svc.record_expense("Sugar", Money::from_f64(50.0), ExpenseCategory::Purchase, wallet_id, user_id)
            .await
            .unwrap();

        let txns = wallet_queries::get_transactions(&pool, &wallet_id.to_string(), &Pagination::default()).await.unwrap();
        assert_eq!(txns.len(), 1);
        assert_eq!(txns[0].amount, -5000);
        assert!(txns[0].description.contains("Expense"));
    }

    // ── record_expense: insufficient funds ─────────────────────────────

    #[tokio::test]
    async fn record_expense_insufficient_funds_rejected() {
        let (pool, svc) = setup().await;
        let wallet_id = Uuid::new_v4();
        let user_id = Uuid::new_v4();
        seed_wallet(&pool, &wallet_id.to_string(), "Cash Box", 3000).await;
        seed_user(&pool, &user_id.to_string()).await;

        let err = svc
            .record_expense("Expensive item", Money::from_f64(100.0), ExpenseCategory::Purchase, wallet_id, user_id)
            .await
            .unwrap_err();

        match err {
            AppError::InsufficientFunds { wallet_name, available, requested } => {
                assert_eq!(wallet_name, "Cash Box");
                assert_eq!(available, Money::from_f64(30.0));
                assert_eq!(requested, Money::from_f64(100.0));
            }
            other => panic!("Expected InsufficientFunds, got: {other:?}"),
        }

        // Wallet unchanged
        let wallet = wallet_queries::get_by_id(&pool, &wallet_id.to_string()).await.unwrap().unwrap();
        assert_eq!(wallet.current_balance, 3000);
    }

    // ── record_expense: validation ─────────────────────────────────────

    #[tokio::test]
    async fn record_expense_empty_description_rejected() {
        let (_pool, svc) = setup().await;
        let err = svc
            .record_expense("", Money::from_f64(50.0), ExpenseCategory::Purchase, Uuid::new_v4(), Uuid::new_v4())
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::Validation { field, .. } if field == "description"));
    }

    #[tokio::test]
    async fn record_expense_whitespace_description_rejected() {
        let (_pool, svc) = setup().await;
        let err = svc
            .record_expense("   ", Money::from_f64(50.0), ExpenseCategory::Purchase, Uuid::new_v4(), Uuid::new_v4())
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::Validation { field, .. } if field == "description"));
    }

    #[tokio::test]
    async fn record_expense_zero_amount_rejected() {
        let (_pool, svc) = setup().await;
        let err = svc
            .record_expense("Test", Money::ZERO, ExpenseCategory::Purchase, Uuid::new_v4(), Uuid::new_v4())
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::Validation { field, .. } if field == "amount"));
    }

    #[tokio::test]
    async fn record_expense_negative_amount_rejected() {
        let (_pool, svc) = setup().await;
        let err = svc
            .record_expense("Test", Money(-1000), ExpenseCategory::Purchase, Uuid::new_v4(), Uuid::new_v4())
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
            .record_expense("Test", Money::from_f64(50.0), ExpenseCategory::Purchase, Uuid::new_v4(), user_id)
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
        seed_wallet(&pool, &wallet_id.to_string(), "Cash Box", 500000).await;
        seed_user(&pool, &user_id.to_string()).await;

        // Record 3 expenses
        svc.record_expense("Expense 1", Money::from_f64(10.0), ExpenseCategory::Purchase, wallet_id, user_id).await.unwrap();
        svc.record_expense("Expense 2", Money::from_f64(20.0), ExpenseCategory::OperatingCost, wallet_id, user_id).await.unwrap();
        svc.record_expense("Expense 3", Money::from_f64(30.0), ExpenseCategory::Purchase, wallet_id, user_id).await.unwrap();

        // Query with a wide range that includes all
        let start = Utc::now() - Duration::hours(1);
        let end = Utc::now() + Duration::hours(1);
        let expenses = svc.get_expenses(start, end, None).await.unwrap();
        assert_eq!(expenses.len(), 3);
    }

    #[tokio::test]
    async fn get_expenses_returns_empty_for_no_match() {
        let (pool, svc) = setup().await;
        let wallet_id = Uuid::new_v4();
        let user_id = Uuid::new_v4();
        seed_wallet(&pool, &wallet_id.to_string(), "Cash Box", 500000).await;
        seed_user(&pool, &user_id.to_string()).await;

        svc.record_expense("Expense 1", Money::from_f64(10.0), ExpenseCategory::Purchase, wallet_id, user_id).await.unwrap();

        // Query a range in the past
        let start = Utc::now() - Duration::days(10);
        let end = Utc::now() - Duration::days(5);
        let expenses = svc.get_expenses(start, end, None).await.unwrap();
        assert!(expenses.is_empty());
    }

    // ── get_expenses_by_category: grouping ─────────────────────────────

    #[tokio::test]
    async fn get_expenses_by_category_groups_correctly() {
        let (pool, svc) = setup().await;
        let wallet_id = Uuid::new_v4();
        let user_id = Uuid::new_v4();
        seed_wallet(&pool, &wallet_id.to_string(), "Cash Box", 500000).await;
        seed_user(&pool, &user_id.to_string()).await;

        svc.record_expense("Milk", Money::from_f64(100.0), ExpenseCategory::Purchase, wallet_id, user_id).await.unwrap();
        svc.record_expense("Sugar", Money::from_f64(50.0), ExpenseCategory::Purchase, wallet_id, user_id).await.unwrap();
        svc.record_expense("Electricity", Money::from_f64(200.0), ExpenseCategory::OperatingCost, wallet_id, user_id).await.unwrap();

        let start = Utc::now() - Duration::hours(1);
        let end = Utc::now() + Duration::hours(1);
        let grouped = svc.get_expenses_by_category(start, end, None).await.unwrap();

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
        let grouped = svc.get_expenses_by_category(start, end, None).await.unwrap();
        assert!(grouped.is_empty());
    }

    // ── exact balance debit ────────────────────────────────────────────

    #[tokio::test]
    async fn record_expense_exact_balance_succeeds() {
        let (pool, svc) = setup().await;
        let wallet_id = Uuid::new_v4();
        let user_id = Uuid::new_v4();
        seed_wallet(&pool, &wallet_id.to_string(), "Cash Box", 7500).await;
        seed_user(&pool, &user_id.to_string()).await;

        svc.record_expense("Full spend", Money::from_f64(75.0), ExpenseCategory::OperatingCost, wallet_id, user_id)
            .await
            .unwrap();

        let wallet = wallet_queries::get_by_id(&pool, &wallet_id.to_string()).await.unwrap().unwrap();
        assert_eq!(wallet.current_balance, 0);
    }

    #[tokio::test]
    async fn record_expense_nonexistent_user_rejected() {
        let (pool, svc) = setup().await;
        let wallet_id = Uuid::new_v4();
        seed_wallet(&pool, &wallet_id.to_string(), "Cash Box", 500000).await;

        let fake_user_id = Uuid::new_v4();
        let err = svc
            .record_expense("Test", Money::from_f64(50.0), ExpenseCategory::Purchase, wallet_id, fake_user_id)
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::NotFound { ref entity_type, .. } if entity_type == "user"));
    }

}
