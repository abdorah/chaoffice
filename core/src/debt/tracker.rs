use chrono::{DateTime, Utc};
use sqlx::SqlitePool;
use uuid::Uuid;

use crate::error::{AppError, AppResult};
use crate::models::domain::{DebtAllocation, DebtPayment, DebtRecord};
use crate::persistence::queries::debts as queries;

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
        amount: f64,
    ) -> AppResult<DebtRecord> {
        if amount <= 0.0 {
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
            amount,
            amount, // remaining = original at creation
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

    /// Record a payment against a customer's debts using FIFO allocation (Req 10.3, 10.4).
    ///
    /// Fetches active debts ordered by sale_date ASC (oldest first), then allocates
    /// the payment amount to each debt until the payment is exhausted. Debts that
    /// are fully paid are marked as settled.
    ///
    /// NOTE: This method does NOT handle wallet crediting — that is the caller's
    /// responsibility (e.g., SalesService).
    pub async fn record_payment(
        &self,
        customer_id: Uuid,
        amount: f64,
        wallet_id: Uuid,
    ) -> AppResult<DebtPayment> {
        if amount <= 0.0 {
            return Err(AppError::Validation {
                field: "amount".to_string(),
                message: "Payment amount must be positive".to_string(),
            });
        }

        let now = Utc::now();
        let now_str = now.to_rfc3339();

        let mut tx = self.pool.begin().await?;

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

        let mut remaining_payment = amount;
        let mut allocations = Vec::new();

        for debt_row in &active_debts {
            if remaining_payment <= 0.0 {
                break;
            }

            let debt_id = Uuid::parse_str(&debt_row.id)
                .map_err(|e| AppError::Unknown(format!("Invalid UUID: {e}")))?;

            let amount_to_apply = remaining_payment.min(debt_row.remaining_amount);
            let new_remaining = debt_row.remaining_amount - amount_to_apply;

            if new_remaining <= 0.0 {
                // Fully paid — mark as settled
                queries::mark_settled(&mut *tx, &debt_row.id, &now_str).await?;
            } else {
                // Partially paid — update remaining
                queries::update_remaining(&mut *tx, &debt_row.id, new_remaining, &now_str).await?;
            }

            allocations.push(DebtAllocation {
                debt_record_id: debt_id,
                amount_applied: amount_to_apply,
            });

            remaining_payment -= amount_to_apply;
        }

        tx.commit().await?;

        let payment_id = Uuid::new_v4();
        Ok(DebtPayment {
            id: payment_id,
            customer_id,
            amount,
            wallet_id,
            allocations,
            timestamp: now,
        })
    }

    /// Get all active (unsettled) debts with computed overdue_days and is_critical (Req 10.1, 10.5).
    pub async fn get_active_debts(&self) -> AppResult<Vec<DebtRecord>> {
        let rows = queries::get_all_active(&self.pool).await?;
        let now = Utc::now();
        rows.into_iter().map(|r| row_to_debt_record(r, now)).collect()
    }

    /// Get the debt aging report: all active debts sorted by overdue_days DESC (Req 10.2).
    pub async fn get_debt_aging_report(&self) -> AppResult<Vec<DebtRecord>> {
        let rows = queries::get_aging_report(&self.pool).await?;
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
    let id = Uuid::parse_str(&row.id)
        .map_err(|e| AppError::Unknown(format!("Invalid UUID: {e}")))?;
    let customer_id = Uuid::parse_str(&row.customer_id)
        .map_err(|e| AppError::Unknown(format!("Invalid UUID: {e}")))?;
    let sale_id = Uuid::parse_str(&row.sale_id)
        .map_err(|e| AppError::Unknown(format!("Invalid UUID: {e}")))?;
    let sale_date: DateTime<Utc> = row
        .sale_date
        .parse()
        .map_err(|e| AppError::Unknown(format!("Invalid sale_date: {e}")))?;

    let overdue_days = (now - sale_date).num_days() as i32;
    let is_critical = overdue_days > 30;

    Ok(DebtRecord {
        id,
        customer_id,
        customer_name: row.customer_name,
        sale_id,
        original_amount: row.original_amount,
        remaining_amount: row.remaining_amount,
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
             VALUES (?, 'TestWallet', 'Cash', 1000.0, 'Synced', ?)",
        )
        .bind(&wallet_id)
        .bind(&now)
        .execute(pool)
        .await
        .expect("seed wallet failed");

        sqlx::query(
            "INSERT INTO sales (id, customer_id, total_amount, amount_paid, payment_wallet_id, timestamp, sync_status, updated_at)
             VALUES (?, ?, 100.0, 0.0, ?, ?, 'Synced', ?)",
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
        amount: f64,
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

    // ── create_debt_record ─────────────────────────────────────────────

    #[tokio::test]
    async fn create_debt_record_success() {
        let (pool, svc) = setup().await;
        let cust_id = Uuid::new_v4();
        let sale_id = Uuid::new_v4();
        seed_customer(&pool, &cust_id.to_string(), "Alice").await;
        seed_sale(&pool, &sale_id.to_string(), &cust_id.to_string()).await;

        let debt = svc.create_debt_record(cust_id, sale_id, 250.0).await.unwrap();

        assert_eq!(debt.customer_id, cust_id);
        assert_eq!(debt.sale_id, sale_id);
        assert_eq!(debt.original_amount, 250.0);
        assert_eq!(debt.remaining_amount, 250.0);
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
            .create_debt_record(cust_id, Uuid::new_v4(), 0.0)
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
            .create_debt_record(cust_id, Uuid::new_v4(), -10.0)
            .await
            .unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }

    #[tokio::test]
    async fn create_debt_record_nonexistent_customer_fails() {
        let (_pool, svc) = setup().await;
        let err = svc
            .create_debt_record(Uuid::new_v4(), Uuid::new_v4(), 100.0)
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

        // Older debt: 10 days ago, 100.0
        let old_date = Utc::now() - Duration::days(10);
        seed_debt_with_date(&pool, &debt1.to_string(), &cust_id.to_string(), &sale1.to_string(), 100.0, old_date).await;

        // Newer debt: 2 days ago, 200.0
        let new_date = Utc::now() - Duration::days(2);
        seed_debt_with_date(&pool, &debt2.to_string(), &cust_id.to_string(), &sale2.to_string(), 200.0, new_date).await;

        // Pay 150 — should fully pay debt1 (100) and partially pay debt2 (50)
        let payment = svc.record_payment(cust_id, 150.0, wallet_id).await.unwrap();

        assert_eq!(payment.amount, 150.0);
        assert_eq!(payment.allocations.len(), 2);
        assert_eq!(payment.allocations[0].debt_record_id, debt1);
        assert_eq!(payment.allocations[0].amount_applied, 100.0);
        assert_eq!(payment.allocations[1].debt_record_id, debt2);
        assert_eq!(payment.allocations[1].amount_applied, 50.0);

        // Verify debt1 is settled, debt2 has 150 remaining
        let active = svc.get_active_debts().await.unwrap();
        assert_eq!(active.len(), 1);
        assert_eq!(active[0].id, debt2);
        assert_eq!(active[0].remaining_amount, 150.0);
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
        seed_debt_with_date(&pool, &debt_id.to_string(), &cust_id.to_string(), &sale_id.to_string(), 100.0, Utc::now()).await;

        let payment = svc.record_payment(cust_id, 100.0, wallet_id).await.unwrap();
        assert_eq!(payment.allocations.len(), 1);
        assert_eq!(payment.allocations[0].amount_applied, 100.0);

        let active = svc.get_active_debts().await.unwrap();
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
        seed_debt_with_date(&pool, &debt_id.to_string(), &cust_id.to_string(), &sale_id.to_string(), 50.0, Utc::now()).await;

        // Pay 200 when only 50 is owed
        let payment = svc.record_payment(cust_id, 200.0, wallet_id).await.unwrap();
        assert_eq!(payment.allocations.len(), 1);
        assert_eq!(payment.allocations[0].amount_applied, 50.0);

        let active = svc.get_active_debts().await.unwrap();
        assert!(active.is_empty());
    }

    #[tokio::test]
    async fn record_payment_no_active_debts_fails() {
        let (pool, svc) = setup().await;
        let cust_id = Uuid::new_v4();
        seed_customer(&pool, &cust_id.to_string(), "Grace").await;

        let err = svc.record_payment(cust_id, 50.0, Uuid::new_v4()).await.unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
    }

    #[tokio::test]
    async fn record_payment_rejects_zero_amount() {
        let (_pool, svc) = setup().await;
        let err = svc.record_payment(Uuid::new_v4(), 0.0, Uuid::new_v4()).await.unwrap_err();
        assert!(matches!(err, AppError::Validation { .. }));
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
        seed_debt_with_date(&pool, &debt_id.to_string(), &cust_id.to_string(), &sale_id.to_string(), 100.0, sale_date).await;

        let debts = svc.get_active_debts().await.unwrap();
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
        seed_debt_with_date(&pool, &debt_id.to_string(), &cust_id.to_string(), &sale_id.to_string(), 100.0, sale_date).await;

        let debts = svc.get_active_debts().await.unwrap();
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
        seed_debt_with_date(&pool, &debt_id.to_string(), &cust_id.to_string(), &sale_id.to_string(), 100.0, sale_date).await;

        let debts = svc.get_active_debts().await.unwrap();
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

        // 5 days, 40 days, 20 days
        seed_debt_with_date(&pool, &Uuid::new_v4().to_string(), &cust_id.to_string(), &sale1.to_string(), 50.0, Utc::now() - Duration::days(5)).await;
        seed_debt_with_date(&pool, &Uuid::new_v4().to_string(), &cust_id.to_string(), &sale2.to_string(), 100.0, Utc::now() - Duration::days(40)).await;
        seed_debt_with_date(&pool, &Uuid::new_v4().to_string(), &cust_id.to_string(), &sale3.to_string(), 75.0, Utc::now() - Duration::days(20)).await;

        let report = svc.get_debt_aging_report().await.unwrap();
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
        seed_debt_with_date(&pool, &debt_id.to_string(), &cust_id.to_string(), &sale_id.to_string(), 100.0, Utc::now()).await;

        // Settle the debt
        svc.record_payment(cust_id, 100.0, wallet_id).await.unwrap();

        let report = svc.get_debt_aging_report().await.unwrap();
        assert!(report.is_empty());
    }

    // ── get_active_debts ───────────────────────────────────────────────

    #[tokio::test]
    async fn get_active_debts_returns_empty_when_none() {
        let (_pool, svc) = setup().await;
        let debts = svc.get_active_debts().await.unwrap();
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

        seed_debt_with_date(&pool, &Uuid::new_v4().to_string(), &cust_id.to_string(), &sale1.to_string(), 50.0, Utc::now() - Duration::days(5)).await;
        let debt2_id = Uuid::new_v4();
        seed_debt_with_date(&pool, &debt2_id.to_string(), &cust_id.to_string(), &sale2.to_string(), 100.0, Utc::now() - Duration::days(1)).await;

        // Pay off the first (older) debt
        svc.record_payment(cust_id, 50.0, wallet_id).await.unwrap();

        let active = svc.get_active_debts().await.unwrap();
        assert_eq!(active.len(), 1);
        assert_eq!(active[0].remaining_amount, 100.0);
    }
}
