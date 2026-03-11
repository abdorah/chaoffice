use chrono::{DateTime, Utc};
use sqlx::SqlitePool;
use uuid::Uuid;

use crate::error::{AppError, AppResult};
use crate::models::domain::{FundTransfer, Wallet, WalletTransaction, WalletType};
use crate::persistence::queries::wallets as queries;

/// Service for managing wallets and fund transfers.
pub struct WalletServiceImpl {
    pool: SqlitePool,
}

impl WalletServiceImpl {
    pub fn new(pool: SqlitePool) -> Self {
        Self { pool }
    }

    /// Fetch all wallets (Req 9.1, 9.6).
    pub async fn get_wallets(&self) -> AppResult<Vec<Wallet>> {
        let rows = queries::list_all(&self.pool).await?;
        rows.into_iter().map(row_to_wallet).collect()
    }

    /// Fetch transaction history for a wallet (Req 9.6).
    pub async fn get_transaction_history(
        &self,
        wallet_id: Uuid,
    ) -> AppResult<Vec<WalletTransaction>> {
        let rows = queries::get_transactions(&self.pool, &wallet_id.to_string()).await?;
        rows.into_iter().map(row_to_transaction).collect()
    }

    /// Atomically transfer funds between two wallets (Req 9.2, 9.3).
    ///
    /// - Begins a transaction
    /// - Fetches source wallet, checks balance >= amount
    /// - Debits source, credits destination
    /// - Inserts two wallet_transactions (negative for source, positive for destination)
    /// - Commits
    pub async fn transfer_funds(
        &self,
        source_id: Uuid,
        destination_id: Uuid,
        amount: f64,
    ) -> AppResult<FundTransfer> {
        let now = Utc::now();
        let now_str = now.to_rfc3339();

        let mut tx = self.pool.begin().await?;

        // Fetch source wallet and check balance
        let source = queries::get_by_id_in_tx(&mut *tx, &source_id.to_string())
            .await?
            .ok_or_else(|| AppError::Validation {
                field: "source_id".to_string(),
                message: format!("Source wallet {source_id} not found"),
            })?;

        if source.current_balance < amount {
            return Err(AppError::InsufficientFunds {
                wallet_name: source.name,
                available: source.current_balance,
                requested: amount,
            });
        }

        // Fetch destination wallet (validate it exists)
        let dest = queries::get_by_id_in_tx(&mut *tx, &destination_id.to_string())
            .await?
            .ok_or_else(|| AppError::Validation {
                field: "destination_id".to_string(),
                message: format!("Destination wallet {destination_id} not found"),
            })?;

        // Debit source
        let new_source_balance = source.current_balance - amount;
        queries::update_balance_in_tx(&mut *tx, &source_id.to_string(), new_source_balance, &now_str).await?;

        // Credit destination
        let new_dest_balance = dest.current_balance + amount;
        queries::update_balance_in_tx(&mut *tx, &destination_id.to_string(), new_dest_balance, &now_str).await?;

        // Record debit transaction on source
        let transfer_id = Uuid::new_v4();
        let description = format!("Transfer to {}", dest.name);
        queries::insert_transaction_in_tx(
            &mut *tx,
            &Uuid::new_v4().to_string(),
            &source_id.to_string(),
            -amount,
            &description,
            Some(&transfer_id.to_string()),
            &now_str,
            &now_str,
        )
        .await?;

        // Record credit transaction on destination
        let description = format!("Transfer from {}", source.name);
        queries::insert_transaction_in_tx(
            &mut *tx,
            &Uuid::new_v4().to_string(),
            &destination_id.to_string(),
            amount,
            &description,
            Some(&transfer_id.to_string()),
            &now_str,
            &now_str,
        )
        .await?;

        tx.commit().await?;

        Ok(FundTransfer {
            id: transfer_id,
            source_wallet_id: source_id,
            destination_wallet_id: destination_id,
            amount,
            timestamp: now,
        })
    }

    /// Credit a wallet (increase balance) and log the transaction (Req 9.5).
    pub async fn credit_wallet(
        &self,
        wallet_id: Uuid,
        amount: f64,
        description: &str,
        related_entity_id: Option<Uuid>,
    ) -> AppResult<()> {
        let now = Utc::now().to_rfc3339();

        let mut tx = self.pool.begin().await?;

        let wallet = queries::get_by_id_in_tx(&mut *tx, &wallet_id.to_string())
            .await?
            .ok_or_else(|| AppError::Validation {
                field: "wallet_id".to_string(),
                message: format!("Wallet {wallet_id} not found"),
            })?;

        let new_balance = wallet.current_balance + amount;
        queries::update_balance_in_tx(&mut *tx, &wallet_id.to_string(), new_balance, &now).await?;

        queries::insert_transaction_in_tx(
            &mut *tx,
            &Uuid::new_v4().to_string(),
            &wallet_id.to_string(),
            amount,
            description,
            related_entity_id.as_ref().map(|id| id.to_string()).as_deref(),
            &now,
            &now,
        )
        .await?;

        tx.commit().await?;
        Ok(())
    }

    /// Debit a wallet (decrease balance) with balance validation (Req 9.3, 9.4).
    ///
    /// Returns `InsufficientFunds` if the debit amount exceeds the wallet balance.
    pub async fn debit_wallet(
        &self,
        wallet_id: Uuid,
        amount: f64,
        description: &str,
        related_entity_id: Option<Uuid>,
    ) -> AppResult<()> {
        let now = Utc::now().to_rfc3339();

        let mut tx = self.pool.begin().await?;

        let wallet = queries::get_by_id_in_tx(&mut *tx, &wallet_id.to_string())
            .await?
            .ok_or_else(|| AppError::Validation {
                field: "wallet_id".to_string(),
                message: format!("Wallet {wallet_id} not found"),
            })?;

        if wallet.current_balance < amount {
            return Err(AppError::InsufficientFunds {
                wallet_name: wallet.name,
                available: wallet.current_balance,
                requested: amount,
            });
        }

        let new_balance = wallet.current_balance - amount;
        queries::update_balance_in_tx(&mut *tx, &wallet_id.to_string(), new_balance, &now).await?;

        queries::insert_transaction_in_tx(
            &mut *tx,
            &Uuid::new_v4().to_string(),
            &wallet_id.to_string(),
            -amount,
            description,
            related_entity_id.as_ref().map(|id| id.to_string()).as_deref(),
            &now,
            &now,
        )
        .await?;

        tx.commit().await?;
        Ok(())
    }
}

/// Parse a WalletType from its string representation (as stored in SQLite).
fn parse_wallet_type(s: &str) -> AppResult<WalletType> {
    match s {
        "Bank" => Ok(WalletType::Bank),
        "Cash" => Ok(WalletType::Cash),
        "Representative" => Ok(WalletType::Representative),
        other => Err(AppError::Unknown(format!("Invalid wallet type: {other}"))),
    }
}

/// Convert a persistence row to a Wallet domain model.
fn row_to_wallet(row: queries::WalletRow) -> AppResult<Wallet> {
    let id = Uuid::parse_str(&row.id)
        .map_err(|e| AppError::Unknown(format!("Invalid UUID: {e}")))?;

    Ok(Wallet {
        id,
        name: row.name,
        wallet_type: parse_wallet_type(&row.wallet_type)?,
        current_balance: row.current_balance,
    })
}

/// Convert a persistence row to a WalletTransaction domain model.
fn row_to_transaction(row: queries::WalletTransactionRow) -> AppResult<WalletTransaction> {
    let id = Uuid::parse_str(&row.id)
        .map_err(|e| AppError::Unknown(format!("Invalid UUID: {e}")))?;
    let wallet_id = Uuid::parse_str(&row.wallet_id)
        .map_err(|e| AppError::Unknown(format!("Invalid UUID: {e}")))?;
    let timestamp: DateTime<Utc> = row
        .timestamp
        .parse()
        .map_err(|e| AppError::Unknown(format!("Invalid timestamp: {e}")))?;
    let related_entity_id = row
        .related_entity_id
        .as_deref()
        .filter(|s| !s.is_empty())
        .map(|s| Uuid::parse_str(s))
        .transpose()
        .map_err(|e| AppError::Unknown(format!("Invalid related_entity UUID: {e}")))?;

    Ok(WalletTransaction {
        id,
        wallet_id,
        amount: row.amount,
        description: row.description,
        related_entity_id,
        timestamp,
    })
}


#[cfg(test)]
mod tests {
    use super::*;
    use crate::persistence::db;
    use crate::persistence::queries::wallets as queries;

    async fn setup() -> (SqlitePool, WalletServiceImpl) {
        let pool = db::init_db(":memory:").await.expect("DB init failed");
        let svc = WalletServiceImpl::new(pool.clone());
        (pool, svc)
    }

    async fn seed_wallet(pool: &SqlitePool, id: &str, name: &str, wallet_type: &str, balance: f64) {
        let now = Utc::now().to_rfc3339();
        queries::insert_wallet(pool, id, name, wallet_type, balance, &now)
            .await
            .expect("seed wallet failed");
    }

    // ── get_wallets ────────────────────────────────────────────────────

    #[tokio::test]
    async fn get_wallets_returns_empty_when_none() {
        let (_pool, svc) = setup().await;
        let wallets = svc.get_wallets().await.unwrap();
        assert!(wallets.is_empty());
    }

    #[tokio::test]
    async fn get_wallets_returns_seeded_wallets() {
        let (pool, svc) = setup().await;
        let id = Uuid::new_v4().to_string();
        seed_wallet(&pool, &id, "Main Bank", "Bank", 1000.0).await;

        let wallets = svc.get_wallets().await.unwrap();
        assert_eq!(wallets.len(), 1);
        assert_eq!(wallets[0].name, "Main Bank");
        assert_eq!(wallets[0].wallet_type, WalletType::Bank);
        assert_eq!(wallets[0].current_balance, 1000.0);
    }

    #[tokio::test]
    async fn get_wallets_returns_all_types() {
        let (pool, svc) = setup().await;
        seed_wallet(&pool, &Uuid::new_v4().to_string(), "Bank Account", "Bank", 500.0).await;
        seed_wallet(&pool, &Uuid::new_v4().to_string(), "Cash Box", "Cash", 200.0).await;
        seed_wallet(&pool, &Uuid::new_v4().to_string(), "Rep Wallet", "Representative", 100.0).await;

        let wallets = svc.get_wallets().await.unwrap();
        assert_eq!(wallets.len(), 3);
    }

    // ── credit_wallet ──────────────────────────────────────────────────

    #[tokio::test]
    async fn credit_wallet_increases_balance() {
        let (pool, svc) = setup().await;
        let id = Uuid::new_v4();
        seed_wallet(&pool, &id.to_string(), "Cash Box", "Cash", 100.0).await;

        svc.credit_wallet(id, 50.0, "Sale payment", None).await.unwrap();

        let wallets = svc.get_wallets().await.unwrap();
        let wallet = wallets.iter().find(|w| w.id == id).unwrap();
        assert_eq!(wallet.current_balance, 150.0);
    }

    #[tokio::test]
    async fn credit_wallet_logs_transaction() {
        let (pool, svc) = setup().await;
        let id = Uuid::new_v4();
        seed_wallet(&pool, &id.to_string(), "Cash Box", "Cash", 100.0).await;

        svc.credit_wallet(id, 50.0, "Sale payment", None).await.unwrap();

        let txns = svc.get_transaction_history(id).await.unwrap();
        assert_eq!(txns.len(), 1);
        assert_eq!(txns[0].amount, 50.0);
        assert_eq!(txns[0].description, "Sale payment");
    }

    #[tokio::test]
    async fn credit_wallet_nonexistent_fails() {
        let (_pool, svc) = setup().await;
        let err = svc.credit_wallet(Uuid::new_v4(), 50.0, "test", None).await.unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }

    // ── debit_wallet ───────────────────────────────────────────────────

    #[tokio::test]
    async fn debit_wallet_decreases_balance() {
        let (pool, svc) = setup().await;
        let id = Uuid::new_v4();
        seed_wallet(&pool, &id.to_string(), "Cash Box", "Cash", 100.0).await;

        svc.debit_wallet(id, 30.0, "Expense", None).await.unwrap();

        let wallets = svc.get_wallets().await.unwrap();
        let wallet = wallets.iter().find(|w| w.id == id).unwrap();
        assert_eq!(wallet.current_balance, 70.0);
    }

    #[tokio::test]
    async fn debit_wallet_exact_balance_succeeds() {
        let (pool, svc) = setup().await;
        let id = Uuid::new_v4();
        seed_wallet(&pool, &id.to_string(), "Cash Box", "Cash", 50.0).await;

        svc.debit_wallet(id, 50.0, "Full debit", None).await.unwrap();

        let wallets = svc.get_wallets().await.unwrap();
        let wallet = wallets.iter().find(|w| w.id == id).unwrap();
        assert_eq!(wallet.current_balance, 0.0);
    }

    #[tokio::test]
    async fn debit_wallet_insufficient_funds_rejected() {
        let (pool, svc) = setup().await;
        let id = Uuid::new_v4();
        seed_wallet(&pool, &id.to_string(), "Cash Box", "Cash", 20.0).await;

        let err = svc.debit_wallet(id, 50.0, "Too much", None).await.unwrap_err();
        match err {
            AppError::InsufficientFunds {
                wallet_name,
                available,
                requested,
            } => {
                assert_eq!(wallet_name, "Cash Box");
                assert_eq!(available, 20.0);
                assert_eq!(requested, 50.0);
            }
            other => panic!("Expected InsufficientFunds, got: {other:?}"),
        }

        // Balance unchanged
        let wallets = svc.get_wallets().await.unwrap();
        let wallet = wallets.iter().find(|w| w.id == id).unwrap();
        assert_eq!(wallet.current_balance, 20.0);
    }

    #[tokio::test]
    async fn debit_wallet_logs_negative_transaction() {
        let (pool, svc) = setup().await;
        let id = Uuid::new_v4();
        seed_wallet(&pool, &id.to_string(), "Cash Box", "Cash", 100.0).await;

        svc.debit_wallet(id, 25.0, "Office supplies", None).await.unwrap();

        let txns = svc.get_transaction_history(id).await.unwrap();
        assert_eq!(txns.len(), 1);
        assert_eq!(txns[0].amount, -25.0);
        assert_eq!(txns[0].description, "Office supplies");
    }

    #[tokio::test]
    async fn debit_wallet_nonexistent_fails() {
        let (_pool, svc) = setup().await;
        let err = svc.debit_wallet(Uuid::new_v4(), 10.0, "test", None).await.unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }

    // ── transfer_funds ─────────────────────────────────────────────────

    #[tokio::test]
    async fn transfer_funds_moves_balance_atomically() {
        let (pool, svc) = setup().await;
        let src = Uuid::new_v4();
        let dst = Uuid::new_v4();
        seed_wallet(&pool, &src.to_string(), "Bank", "Bank", 500.0).await;
        seed_wallet(&pool, &dst.to_string(), "Cash", "Cash", 100.0).await;

        let transfer = svc.transfer_funds(src, dst, 200.0).await.unwrap();

        assert_eq!(transfer.source_wallet_id, src);
        assert_eq!(transfer.destination_wallet_id, dst);
        assert_eq!(transfer.amount, 200.0);

        let wallets = svc.get_wallets().await.unwrap();
        let bank = wallets.iter().find(|w| w.id == src).unwrap();
        let cash = wallets.iter().find(|w| w.id == dst).unwrap();
        assert_eq!(bank.current_balance, 300.0);
        assert_eq!(cash.current_balance, 300.0);
    }

    #[tokio::test]
    async fn transfer_funds_creates_two_transactions() {
        let (pool, svc) = setup().await;
        let src = Uuid::new_v4();
        let dst = Uuid::new_v4();
        seed_wallet(&pool, &src.to_string(), "Bank", "Bank", 500.0).await;
        seed_wallet(&pool, &dst.to_string(), "Cash", "Cash", 100.0).await;

        svc.transfer_funds(src, dst, 200.0).await.unwrap();

        let src_txns = svc.get_transaction_history(src).await.unwrap();
        assert_eq!(src_txns.len(), 1);
        assert_eq!(src_txns[0].amount, -200.0);

        let dst_txns = svc.get_transaction_history(dst).await.unwrap();
        assert_eq!(dst_txns.len(), 1);
        assert_eq!(dst_txns[0].amount, 200.0);
    }

    #[tokio::test]
    async fn transfer_funds_insufficient_balance_rejected() {
        let (pool, svc) = setup().await;
        let src = Uuid::new_v4();
        let dst = Uuid::new_v4();
        seed_wallet(&pool, &src.to_string(), "Bank", "Bank", 50.0).await;
        seed_wallet(&pool, &dst.to_string(), "Cash", "Cash", 100.0).await;

        let err = svc.transfer_funds(src, dst, 200.0).await.unwrap_err();
        match err {
            AppError::InsufficientFunds {
                wallet_name,
                available,
                requested,
            } => {
                assert_eq!(wallet_name, "Bank");
                assert_eq!(available, 50.0);
                assert_eq!(requested, 200.0);
            }
            other => panic!("Expected InsufficientFunds, got: {other:?}"),
        }

        // Both balances unchanged
        let wallets = svc.get_wallets().await.unwrap();
        let bank = wallets.iter().find(|w| w.id == src).unwrap();
        let cash = wallets.iter().find(|w| w.id == dst).unwrap();
        assert_eq!(bank.current_balance, 50.0);
        assert_eq!(cash.current_balance, 100.0);
    }

    #[tokio::test]
    async fn transfer_funds_exact_balance_succeeds() {
        let (pool, svc) = setup().await;
        let src = Uuid::new_v4();
        let dst = Uuid::new_v4();
        seed_wallet(&pool, &src.to_string(), "Bank", "Bank", 100.0).await;
        seed_wallet(&pool, &dst.to_string(), "Cash", "Cash", 0.0).await;

        svc.transfer_funds(src, dst, 100.0).await.unwrap();

        let wallets = svc.get_wallets().await.unwrap();
        let bank = wallets.iter().find(|w| w.id == src).unwrap();
        let cash = wallets.iter().find(|w| w.id == dst).unwrap();
        assert_eq!(bank.current_balance, 0.0);
        assert_eq!(cash.current_balance, 100.0);
    }

    #[tokio::test]
    async fn transfer_funds_nonexistent_source_fails() {
        let (pool, svc) = setup().await;
        let dst = Uuid::new_v4();
        seed_wallet(&pool, &dst.to_string(), "Cash", "Cash", 100.0).await;

        let err = svc.transfer_funds(Uuid::new_v4(), dst, 50.0).await.unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }

    #[tokio::test]
    async fn transfer_funds_nonexistent_destination_fails() {
        let (pool, svc) = setup().await;
        let src = Uuid::new_v4();
        seed_wallet(&pool, &src.to_string(), "Bank", "Bank", 500.0).await;

        let err = svc.transfer_funds(src, Uuid::new_v4(), 50.0).await.unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));

        // Source balance unchanged
        let wallets = svc.get_wallets().await.unwrap();
        assert_eq!(wallets[0].current_balance, 500.0);
    }

    // ── get_transaction_history ────────────────────────────────────────

    #[tokio::test]
    async fn transaction_history_empty_for_new_wallet() {
        let (pool, svc) = setup().await;
        let id = Uuid::new_v4();
        seed_wallet(&pool, &id.to_string(), "Cash", "Cash", 100.0).await;

        let txns = svc.get_transaction_history(id).await.unwrap();
        assert!(txns.is_empty());
    }

    #[tokio::test]
    async fn transaction_history_records_related_entity() {
        let (pool, svc) = setup().await;
        let id = Uuid::new_v4();
        let entity_id = Uuid::new_v4();
        seed_wallet(&pool, &id.to_string(), "Cash", "Cash", 100.0).await;

        svc.credit_wallet(id, 50.0, "Sale #1", Some(entity_id)).await.unwrap();

        let txns = svc.get_transaction_history(id).await.unwrap();
        assert_eq!(txns.len(), 1);
        assert_eq!(txns[0].related_entity_id, Some(entity_id));
    }

    // ── conservation property (manual) ─────────────────────────────────

    #[tokio::test]
    async fn transfer_conserves_total_balance() {
        let (pool, svc) = setup().await;
        let src = Uuid::new_v4();
        let dst = Uuid::new_v4();
        seed_wallet(&pool, &src.to_string(), "Bank", "Bank", 1000.0).await;
        seed_wallet(&pool, &dst.to_string(), "Cash", "Cash", 500.0).await;

        let total_before: f64 = svc.get_wallets().await.unwrap().iter().map(|w| w.current_balance).sum();

        svc.transfer_funds(src, dst, 300.0).await.unwrap();

        let total_after: f64 = svc.get_wallets().await.unwrap().iter().map(|w| w.current_balance).sum();
        assert_eq!(total_before, total_after);
    }
}
