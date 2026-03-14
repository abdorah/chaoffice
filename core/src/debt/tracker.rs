use chrono::{DateTime, Utc};
use sqlx::SqlitePool;
use uuid::Uuid;

use crate::error::{AppError, AppResult};
use crate::models::domain::{DebtAllocation, DebtPayment, DebtRecord, Pagination};
use crate::models::Money;
use crate::persistence::queries::debt_payments as payment_queries;
use crate::persistence::queries::debts as queries;
use crate::persistence::queries::wallets as wallet_queries;

/// Service for debt tracking, FIFO payment allocation, and aging reports.
pub struct DebtServiceImpl {
    pool: SqlitePool,
}

impl DebtServiceImpl {
    pub fn new(pool: SqlitePool) -> Self {
        Self { pool }
    }

    /// Create a debt record for an unpaid sale balance (Req 10.1, 8.3).
    pub async fn create_debt_record(
        &self,
        customer_id: Uuid,
        sale_id: Uuid,
        amount: Money,
    ) -> AppResult<DebtRecord> {
        if amount.0 <= 0 {
            return Err(AppError::Validation {
                field: "amount".to_string(),
                message: "Debt amount must be positive".to_string(),
            });
        }

        let now = Utc::now();
        let now_str = now.to_rfc3339();
        let id = Uuid::new_v4();

        // Look up customer name
        let customer = crate::persistence::queries::customers::get_by_id(
            &self.pool,
            &customer_id.to_string(),
        )
        .await?
        .ok_or_else(|| AppError::Validation {
            field: "customer_id".to_string(),
            message: format!("Customer {customer_id} not found"),
        })?;

        queries::insert_debt(
            &self.pool,
            &id.to_string(),
            &customer_id.to_string(),
            &sale_id.to_string(),
            amount.0,
            amount.0, // remaining = original at creation
            &now_str,
            &now_str,
        )
        .await?;

        let overdue_days = 0i32; // just created, 0 days overdue
        Ok(DebtRecord {
            id,
            customer_id,
            customer_name: customer.name,
            sale_id,
            original_amount: amount,
            remaining_amount: amount,
            sale_date: now,
            overdue_days,
            is_critical: false,
            is_settled: false,
        })
    }

    /// Record a payment against a customer's debts using FIFO allocation (Req 6, 14).
    ///
    /// Fetches active debts ordered by sale_date ASC (oldest first), then allocates
    /// the payment amount to each debt until the payment is exhausted or all debts
    /// are settled. Credits the specified wallet by the amount actually allocated.
    /// Persists the payment and allocations to the debt_payments tables.
    ///
    /// Returns the payment with the total allocated amount and unallocated remainder.
    pub async fn record_payment(
        &self,
        customer_id: Uuid,
        amount: Money,
        wallet_id: Uuid,
    ) -> AppResult<DebtPayment> {
        if amount.0 <= 0 {
            return Err(AppError::Validation {
                field: "amount".to_string(),
                message: "Payment amount must be positive".to_string(),
            });
        }

        let now = Utc::now();
        let now_str = now.to_rfc3339();
        let payment_id = Uuid::new_v4();

        let mut tx = self.pool.begin().await?;

        // Validate wallet exists (Req 6.2)
        let _wallet = wallet_queries::get_by_id_in_tx(&mut *tx, &wallet_id.to_string())
            .await?
            .ok_or_else(|| AppError::NotFound {
                entity_type: "wallet".to_string(),
                entity_id: wallet_id.to_string(),
            })?;

        // Fetch active debts for this customer, oldest first (FIFO)
        let active_debts = queries::get_active_debts_for_customer(
            &mut *tx,
            &customer_id.to_string(),
        )
        .await?;

        if active_debts.is_empty() {
            return Err(AppError::Validation {
                field: "customer_id".to_string(),
                message: format!("No active debts found for customer {customer_id}"),
            });
        }

        // FIFO allocation — cap at total outstanding debt (Req 6.3)
        let mut remaining_payment = amount;
        let mut allocations = Vec::new();

        for debt_row in &active_debts {
            if remaining_payment.0 <= 0 {
                break;
            }

            let debt_id = crate::utils::parse_uuid("debt_record", &debt_row.id)?;

            let row_remaining = Money(debt_row.remaining_amount);
            let amount_to_apply = if remaining_payment.0 < row_remaining.0 {
                remaining_payment
            } else {
                row_remaining
            };
            let new_remaining = row_remaining - amount_to_apply;

            if new_remaining.0 <= 0 {
                // Fully paid — mark as settled
                queries::mark_settled(&mut *tx, &debt_row.id, &now_str).await?;
            } else {
                // Partially paid — update remaining
                queries::update_remaining(&mut *tx, &debt_row.id, new_remaining.0, &now_str).await?;
            }

            allocations.push(DebtAllocation {
                debt_record_id: debt_id,
                amount_applied: amount_to_apply,
            });

            remaining_payment = remaining_payment - amount_to_apply;
        }

        let total_allocated = amount - remaining_payment;

        // Credit wallet by amount actually allocated, not full payment (Req 6.1, 6.3)
        wallet_queries::update_balance_in_tx(
            &mut *tx,
            &wallet_id.to_string(),
            total_allocated.0,
            &now_str,
        )
        .await?;

        // Persist payment record (Req 14.3)
        payment_queries::insert_payment(
            &mut *tx,
            &payment_id.to_string(),
            &customer_id.to_string(),
            total_allocated.0,
            &wallet_id.to_string(),
            &now_str,
            &now_str,
        )
        .await?;

        // Persist allocations (Req 14.3)
        for alloc in &allocations {
            let alloc_id = Uuid::new_v4();
            payment_queries::insert_allocation(
                &mut *tx,
                &alloc_id.to_string(),
                &payment_id.to_string(),
                &alloc.debt_record_id.to_string(),
                alloc.amount_applied.0,
            )
            .await?;
        }

        tx.commit().await?;

        Ok(DebtPayment {
            id: payment_id,
            customer_id,
            amount: total_allocated,
            unallocated: remaining_payment,
            wallet_id,
            allocations,
            timestamp: now,
        })
    }

    /// Get all active (unsettled) debts with computed overdue_days and is_critical (Req 10.1, 10.5).
    pub async fn get_active_debts(&self, pagination: Option<Pagination>) -> AppResult<Vec<DebtRecord>> {
        let pg = pagination.unwrap_or_default();
        let rows = queries::get_all_active(&self.pool, &pg).await?;
        let now = Utc::now();
        rows.into_iter().map(|r| row_to_debt_record(r, now)).collect()
    }

    /// Get the debt aging report: all active debts sorted by overdue_days DESC (Req 10.2).
    pub async fn get_debt_aging_report(&self, pagination: Option<Pagination>) -> AppResult<Vec<DebtRecord>> {
        let pg = pagination.unwrap_or_default();
        let rows = queries::get_aging_report(&self.pool, &pg).await?;
        let now = Utc::now();
        let mut records: Vec<DebtRecord> = rows
            .into_iter()
            .map(|r| row_to_debt_record(r, now))
            .collect::<AppResult<Vec<_>>>()?;

        // Sort by overdue_days descending
        records.sort_by(|a, b| b.overdue_days.cmp(&a.overdue_days));
        Ok(records)
    }
}

/// Convert a persistence row to a DebtRecord domain model, computing overdue_days and is_critical.
fn row_to_debt_record(
    row: queries::DebtRecordWithCustomerRow,
    now: DateTime<Utc>,
) -> AppResult<DebtRecord> {
    let id = crate::utils::parse_uuid("debt_record", &row.id)?;
    let customer_id = crate::utils::parse_uuid("customer", &row.customer_id)?;
    let sale_id = crate::utils::parse_uuid("sale", &row.sale_id)?;
    let sale_date = crate::utils::parse_timestamp(&row.sale_date)?;

    let overdue_days = (now - sale_date).num_days() as i32;
    let is_critical = overdue_days > 30;

    Ok(DebtRecord {
        id,
        customer_id,
        customer_name: row.customer_name,
        sale_id,
        original_amount: Money(row.original_amount),
        remaining_amount: Money(row.remaining_amount),
        sale_date,
        overdue_days,
        is_critical,
        is_settled: row.is_settled,
    })
}


#[cfg(test)]
mod tests {
    use super::*;
    use crate::persistence::db;
    use crate::persistence::queries::customers as customer_queries;
    use crate::persistence::queries::debts as debt_queries;
    use crate::models::Money;
    use chrono::Duration;

    async fn setup() -> (SqlitePool, DebtServiceImpl) {
        let pool = db::init_db(":memory:").await.expect("DB init failed");
        let svc = DebtServiceImpl::new(pool.clone());
        (pool, svc)
    }

    async fn seed_customer(pool: &SqlitePool, id: &str, name: &str) {
        let now = Utc::now().to_rfc3339();
        customer_queries::insert(pool, id, name, "TestCity", "0500000000", &now)
            .await
            .expect("seed customer failed");
    }

    /// Seed a sale row (minimal — just enough to satisfy the FK).
    async fn seed_sale(pool: &SqlitePool, sale_id: &str, customer_id: &str) {
        let now = Utc::now().to_rfc3339();
        let wallet_id = Uuid::new_v4().to_string();
        // Seed a wallet for the sale FK
        sqlx::query(
            "INSERT INTO wallets (id, name, wallet_type, current_balance, sync_status, updated_at)
             VALUES (?, 'TestWallet', 'Cash', 100000, 'Synced', ?)",
        )
        .bind(&wallet_id)
        .bind(&now)
        .execute(pool)
        .await
        .expect("seed wallet failed");

        sqlx::query(
            "INSERT INTO sales (id, customer_id, total_amount, amount_paid, payment_wallet_id, timestamp, sync_status, updated_at)
             VALUES (?, ?, 10000, 0, ?, ?, 'Synced', ?)",
        )
        .bind(sale_id)
        .bind(customer_id)
        .bind(&wallet_id)
        .bind(&now)
        .bind(&now)
        .execute(pool)
        .await
        .expect("seed sale failed");
    }

    /// Insert a debt record with a specific sale_date (for overdue testing).
    async fn seed_debt_with_date(
        pool: &SqlitePool,
        debt_id: &str,
        customer_id: &str,
        sale_id: &str,
        amount: i64,
        sale_date: DateTime<Utc>,
    ) {
        let now = Utc::now().to_rfc3339();
        debt_queries::insert_debt(
            pool,
            debt_id,
            customer_id,
            sale_id,
            amount,
            amount,
            &sale_date.to_rfc3339(),
            &now,
        )
        .await
        .expect("seed debt failed");
    }

    /// Seed a wallet for use in record_payment tests.
    async fn seed_wallet(pool: &SqlitePool, wallet_id: &str, balance: i64) {
        let now = Utc::now().to_rfc3339();
        crate::persistence::queries::wallets::insert_wallet(
            pool, wallet_id, "PaymentWallet", "Cash", balance, &now,
        )
        .await
        .expect("seed wallet failed");
    }

    // ── create_debt_record ─────────────────────────────────────────────

    #[tokio::test]
    async fn create_debt_record_success() {
        let (pool, svc) = setup().await;
        let cust_id = Uuid::new_v4();
        let sale_id = Uuid::new_v4();
        seed_customer(&pool, &cust_id.to_string(), "Alice").await;
        seed_sale(&pool, &sale_id.to_string(), &cust_id.to_string()).await;

        let debt = svc.create_debt_record(cust_id, sale_id, Money::from_f64(250.0)).await.unwrap();

        assert_eq!(debt.customer_id, cust_id);
        assert_eq!(debt.sale_id, sale_id);
        assert_eq!(debt.original_amount, Money::from_f64(250.0));
        assert_eq!(debt.remaining_amount, Money::from_f64(250.0));
        assert_eq!(debt.overdue_days, 0);
        assert!(!debt.is_critical);
        assert!(!debt.is_settled);
        assert_eq!(debt.customer_name, "Alice");
    }

    #[tokio::test]
    async fn create_debt_record_rejects_zero_amount() {
        let (pool, svc) = setup().await;
        let cust_id = Uuid::new_v4();
        seed_customer(&pool, &cust_id.to_string(), "Bob").await;

        let err = svc
            .create_debt_record(cust_id, Uuid::new_v4(), Money::ZERO)
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }

    #[tokio::test]
    async fn create_debt_record_rejects_negative_amount() {
        let (pool, svc) = setup().await;
        let cust_id = Uuid::new_v4();
        seed_customer(&pool, &cust_id.to_string(), "Carol").await;

        let err = svc
            .create_debt_record(cust_id, Uuid::new_v4(), Money(- 1000))
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }

    #[tokio::test]
    async fn create_debt_record_nonexistent_customer_fails() {
        let (_pool, svc) = setup().await;
        let err = svc
            .create_debt_record(Uuid::new_v4(), Uuid::new_v4(), Money::from_f64(100.0))
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }

    // ── record_payment (FIFO) ──────────────────────────────────────────

    #[tokio::test]
    async fn record_payment_fifo_oldest_first() {
        let (pool, svc) = setup().await;
        let cust_id = Uuid::new_v4();
        let sale1 = Uuid::new_v4();
        let sale2 = Uuid::new_v4();
        let debt1 = Uuid::new_v4();
        let debt2 = Uuid::new_v4();
        let wallet_id = Uuid::new_v4();

        seed_customer(&pool, &cust_id.to_string(), "Dave").await;
        seed_sale(&pool, &sale1.to_string(), &cust_id.to_string()).await;
        seed_sale(&pool, &sale2.to_string(), &cust_id.to_string()).await;
        seed_wallet(&pool, &wallet_id.to_string(), 0).await;

        // Older debt: 10 days ago, 100.00 (10000 cents)
        let old_date = Utc::now() - Duration::days(10);
        seed_debt_with_date(&pool, &debt1.to_string(), &cust_id.to_string(), &sale1.to_string(), 10000, old_date).await;

        // Newer debt: 2 days ago, 200.00 (20000 cents)
        let new_date = Utc::now() - Duration::days(2);
        seed_debt_with_date(&pool, &debt2.to_string(), &cust_id.to_string(), &sale2.to_string(), 20000, new_date).await;

        // Pay 150.00 — should fully pay debt1 (100) and partially pay debt2 (50)
        let payment = svc.record_payment(cust_id, Money(15000), wallet_id).await.unwrap();

        assert_eq!(payment.amount, Money(15000));
        assert_eq!(payment.unallocated, Money::ZERO);
        assert_eq!(payment.allocations.len(), 2);
        assert_eq!(payment.allocations[0].debt_record_id, debt1);
        assert_eq!(payment.allocations[0].amount_applied, Money(10000));
        assert_eq!(payment.allocations[1].debt_record_id, debt2);
        assert_eq!(payment.allocations[1].amount_applied, Money(5000));

        // Verify wallet was credited by 150.00 (15000 cents)
        let wallet = crate::persistence::queries::wallets::get_by_id(&pool, &wallet_id.to_string()).await.unwrap().unwrap();
        assert_eq!(wallet.current_balance, 15000);

        // Verify debt1 is settled, debt2 has 150.00 remaining
        let active = svc.get_active_debts(None).await.unwrap();
        assert_eq!(active.len(), 1);
        assert_eq!(active[0].id, debt2);
        assert_eq!(active[0].remaining_amount, Money(15000));
    }

    #[tokio::test]
    async fn record_payment_fully_settles_all_debts() {
        let (pool, svc) = setup().await;
        let cust_id = Uuid::new_v4();
        let sale_id = Uuid::new_v4();
        let debt_id = Uuid::new_v4();
        let wallet_id = Uuid::new_v4();

        seed_customer(&pool, &cust_id.to_string(), "Eve").await;
        seed_sale(&pool, &sale_id.to_string(), &cust_id.to_string()).await;
        seed_wallet(&pool, &wallet_id.to_string(), 0).await;
        seed_debt_with_date(&pool, &debt_id.to_string(), &cust_id.to_string(), &sale_id.to_string(), 10000, Utc::now()).await;

        let payment = svc.record_payment(cust_id, Money(10000), wallet_id).await.unwrap();
        assert_eq!(payment.allocations.len(), 1);
        assert_eq!(payment.allocations[0].amount_applied, Money(10000));
        assert_eq!(payment.amount, Money(10000));
        assert_eq!(payment.unallocated, Money::ZERO);

        // Verify wallet credited
        let wallet = crate::persistence::queries::wallets::get_by_id(&pool, &wallet_id.to_string()).await.unwrap().unwrap();
        assert_eq!(wallet.current_balance, 10000);

        let active = svc.get_active_debts(None).await.unwrap();
        assert!(active.is_empty());
    }

    #[tokio::test]
    async fn record_payment_overpayment_only_allocates_owed() {
        let (pool, svc) = setup().await;
        let cust_id = Uuid::new_v4();
        let sale_id = Uuid::new_v4();
        let debt_id = Uuid::new_v4();
        let wallet_id = Uuid::new_v4();

        seed_customer(&pool, &cust_id.to_string(), "Frank").await;
        seed_sale(&pool, &sale_id.to_string(), &cust_id.to_string()).await;
        seed_wallet(&pool, &wallet_id.to_string(), 0).await;
        seed_debt_with_date(&pool, &debt_id.to_string(), &cust_id.to_string(), &sale_id.to_string(), 5000, Utc::now()).await;

        // Pay 200.00 when only 50.00 is owed — should allocate 50, return 150 unallocated
        let payment = svc.record_payment(cust_id, Money(20000), wallet_id).await.unwrap();
        assert_eq!(payment.amount, Money(5000));
        assert_eq!(payment.unallocated, Money(15000));
        assert_eq!(payment.allocations.len(), 1);
        assert_eq!(payment.allocations[0].amount_applied, Money(5000));

        // Wallet should only be credited by 50.00 (5000 cents), not 200.00
        let wallet = crate::persistence::queries::wallets::get_by_id(&pool, &wallet_id.to_string()).await.unwrap().unwrap();
        assert_eq!(wallet.current_balance, 5000);

        let active = svc.get_active_debts(None).await.unwrap();
        assert!(active.is_empty());
    }

    #[tokio::test]
    async fn record_payment_no_active_debts_fails() {
        let (pool, svc) = setup().await;
        let cust_id = Uuid::new_v4();
        let wallet_id = Uuid::new_v4();
        seed_customer(&pool, &cust_id.to_string(), "Grace").await;
        seed_wallet(&pool, &wallet_id.to_string(), 0).await;

        let err = svc.record_payment(cust_id, Money(5000), wallet_id).await.unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }

    #[tokio::test]
    async fn record_payment_rejects_zero_amount() {
        let (_pool, svc) = setup().await;
        let err = svc.record_payment(Uuid::new_v4(), Money::ZERO, Uuid::new_v4()).await.unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }

    #[tokio::test]
    async fn record_payment_nonexistent_wallet_returns_not_found() {
        let (pool, svc) = setup().await;
        let cust_id = Uuid::new_v4();
        let sale_id = Uuid::new_v4();
        let debt_id = Uuid::new_v4();
        let fake_wallet = Uuid::new_v4();

        seed_customer(&pool, &cust_id.to_string(), "Nora").await;
        seed_sale(&pool, &sale_id.to_string(), &cust_id.to_string()).await;
        seed_debt_with_date(&pool, &debt_id.to_string(), &cust_id.to_string(), &sale_id.to_string(), 10000, Utc::now()).await;

        let err = svc.record_payment(cust_id, Money(5000), fake_wallet).await.unwrap_err();
        assert!(matches!(err, AppError::NotFound { .. }));
    }

    #[tokio::test]
    async fn record_payment_persists_to_debt_payments_tables() {
        let (pool, svc) = setup().await;
        let cust_id = Uuid::new_v4();
        let sale_id = Uuid::new_v4();
        let debt_id = Uuid::new_v4();
        let wallet_id = Uuid::new_v4();

        seed_customer(&pool, &cust_id.to_string(), "Oscar").await;
        seed_sale(&pool, &sale_id.to_string(), &cust_id.to_string()).await;
        seed_wallet(&pool, &wallet_id.to_string(), 0).await;
        seed_debt_with_date(&pool, &debt_id.to_string(), &cust_id.to_string(), &sale_id.to_string(), 10000, Utc::now()).await;

        let payment = svc.record_payment(cust_id, Money(6000), wallet_id).await.unwrap();

        // Verify debt_payments row exists
        let row = sqlx::query_as::<_, (String, String, i64, String)>(
            "SELECT id, customer_id, amount, wallet_id FROM debt_payments WHERE id = ?",
        )
        .bind(payment.id.to_string())
        .fetch_one(&pool)
        .await
        .expect("debt_payments row should exist");

        assert_eq!(row.0, payment.id.to_string());
        assert_eq!(row.1, cust_id.to_string());
        assert_eq!(row.2, 6000); // 60.00 in cents
        assert_eq!(row.3, wallet_id.to_string());

        // Verify debt_payment_allocations row exists
        let alloc_rows = sqlx::query_as::<_, (String, String, i64)>(
            "SELECT payment_id, debt_record_id, amount_applied FROM debt_payment_allocations WHERE payment_id = ?",
        )
        .bind(payment.id.to_string())
        .fetch_all(&pool)
        .await
        .expect("debt_payment_allocations rows should exist");

        assert_eq!(alloc_rows.len(), 1);
        assert_eq!(alloc_rows[0].0, payment.id.to_string());
        assert_eq!(alloc_rows[0].1, debt_id.to_string());
        assert_eq!(alloc_rows[0].2, 6000); // 60.00 in cents
    }

    // ── overdue days & critical flag ───────────────────────────────────

    #[tokio::test]
    async fn overdue_days_calculated_correctly() {
        let (pool, svc) = setup().await;
        let cust_id = Uuid::new_v4();
        let sale_id = Uuid::new_v4();
        let debt_id = Uuid::new_v4();

        seed_customer(&pool, &cust_id.to_string(), "Hank").await;
        seed_sale(&pool, &sale_id.to_string(), &cust_id.to_string()).await;

        let sale_date = Utc::now() - Duration::days(15);
        seed_debt_with_date(&pool, &debt_id.to_string(), &cust_id.to_string(), &sale_id.to_string(), 10000, sale_date).await;

        let debts = svc.get_active_debts(None).await.unwrap();
        assert_eq!(debts.len(), 1);
        assert_eq!(debts[0].overdue_days, 15);
        assert!(!debts[0].is_critical);
    }

    #[tokio::test]
    async fn critical_flag_set_when_over_30_days() {
        let (pool, svc) = setup().await;
        let cust_id = Uuid::new_v4();
        let sale_id = Uuid::new_v4();
        let debt_id = Uuid::new_v4();

        seed_customer(&pool, &cust_id.to_string(), "Ivy").await;
        seed_sale(&pool, &sale_id.to_string(), &cust_id.to_string()).await;

        let sale_date = Utc::now() - Duration::days(31);
        seed_debt_with_date(&pool, &debt_id.to_string(), &cust_id.to_string(), &sale_id.to_string(), 10000, sale_date).await;

        let debts = svc.get_active_debts(None).await.unwrap();
        assert_eq!(debts.len(), 1);
        assert_eq!(debts[0].overdue_days, 31);
        assert!(debts[0].is_critical);
    }

    #[tokio::test]
    async fn critical_flag_not_set_at_exactly_30_days() {
        let (pool, svc) = setup().await;
        let cust_id = Uuid::new_v4();
        let sale_id = Uuid::new_v4();
        let debt_id = Uuid::new_v4();

        seed_customer(&pool, &cust_id.to_string(), "Jack").await;
        seed_sale(&pool, &sale_id.to_string(), &cust_id.to_string()).await;

        let sale_date = Utc::now() - Duration::days(30);
        seed_debt_with_date(&pool, &debt_id.to_string(), &cust_id.to_string(), &sale_id.to_string(), 10000, sale_date).await;

        let debts = svc.get_active_debts(None).await.unwrap();
        assert_eq!(debts.len(), 1);
        // Exactly 30 days is NOT critical (> 30 required)
        assert!(!debts[0].is_critical);
    }

    // ── aging report ───────────────────────────────────────────────────

    #[tokio::test]
    async fn aging_report_sorted_by_overdue_days_desc() {
        let (pool, svc) = setup().await;
        let cust_id = Uuid::new_v4();
        let sale1 = Uuid::new_v4();
        let sale2 = Uuid::new_v4();
        let sale3 = Uuid::new_v4();

        seed_customer(&pool, &cust_id.to_string(), "Kate").await;
        seed_sale(&pool, &sale1.to_string(), &cust_id.to_string()).await;
        seed_sale(&pool, &sale2.to_string(), &cust_id.to_string()).await;
        seed_sale(&pool, &sale3.to_string(), &cust_id.to_string()).await;

        // 5 days, 40 days, 20 days (amounts in cents)
        seed_debt_with_date(&pool, &Uuid::new_v4().to_string(), &cust_id.to_string(), &sale1.to_string(), 5000, Utc::now() - Duration::days(5)).await;
        seed_debt_with_date(&pool, &Uuid::new_v4().to_string(), &cust_id.to_string(), &sale2.to_string(), 10000, Utc::now() - Duration::days(40)).await;
        seed_debt_with_date(&pool, &Uuid::new_v4().to_string(), &cust_id.to_string(), &sale3.to_string(), 7500, Utc::now() - Duration::days(20)).await;

        let report = svc.get_debt_aging_report(None).await.unwrap();
        assert_eq!(report.len(), 3);
        // Should be sorted: 40 days, 20 days, 5 days
        assert_eq!(report[0].overdue_days, 40);
        assert_eq!(report[1].overdue_days, 20);
        assert_eq!(report[2].overdue_days, 5);

        // 40-day debt should be critical
        assert!(report[0].is_critical);
        assert!(!report[1].is_critical);
        assert!(!report[2].is_critical);
    }

    #[tokio::test]
    async fn aging_report_excludes_settled_debts() {
        let (pool, svc) = setup().await;
        let cust_id = Uuid::new_v4();
        let sale_id = Uuid::new_v4();
        let debt_id = Uuid::new_v4();
        let wallet_id = Uuid::new_v4();

        seed_customer(&pool, &cust_id.to_string(), "Leo").await;
        seed_sale(&pool, &sale_id.to_string(), &cust_id.to_string()).await;
        seed_wallet(&pool, &wallet_id.to_string(), 0).await;
        seed_debt_with_date(&pool, &debt_id.to_string(), &cust_id.to_string(), &sale_id.to_string(), 10000, Utc::now()).await;

        // Settle the debt
        svc.record_payment(cust_id, Money(10000), wallet_id).await.unwrap();

        let report = svc.get_debt_aging_report(None).await.unwrap();
        assert!(report.is_empty());
    }

    // ── get_active_debts ───────────────────────────────────────────────

    #[tokio::test]
    async fn get_active_debts_returns_empty_when_none() {
        let (_pool, svc) = setup().await;
        let debts = svc.get_active_debts(None).await.unwrap();
        assert!(debts.is_empty());
    }

    #[tokio::test]
    async fn get_active_debts_returns_only_unsettled() {
        let (pool, svc) = setup().await;
        let cust_id = Uuid::new_v4();
        let sale1 = Uuid::new_v4();
        let sale2 = Uuid::new_v4();
        let wallet_id = Uuid::new_v4();

        seed_customer(&pool, &cust_id.to_string(), "Mia").await;
        seed_sale(&pool, &sale1.to_string(), &cust_id.to_string()).await;
        seed_sale(&pool, &sale2.to_string(), &cust_id.to_string()).await;
        seed_wallet(&pool, &wallet_id.to_string(), 0).await;

        seed_debt_with_date(&pool, &Uuid::new_v4().to_string(), &cust_id.to_string(), &sale1.to_string(), 5000, Utc::now() - Duration::days(5)).await;
        let debt2_id = Uuid::new_v4();
        seed_debt_with_date(&pool, &debt2_id.to_string(), &cust_id.to_string(), &sale2.to_string(), 10000, Utc::now() - Duration::days(1)).await;

        // Pay off the first (older) debt
        svc.record_payment(cust_id, Money(5000), wallet_id).await.unwrap();

        let active = svc.get_active_debts(None).await.unwrap();
        assert_eq!(active.len(), 1);
        assert_eq!(active[0].remaining_amount, Money(10000));
    }
}
