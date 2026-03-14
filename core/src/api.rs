//! UniFFI-exported public API surface.
//!
//! `SweetLabCore` is the single entry point for all platform UIs.
//! It wraps every service implementation and delegates each public method
//! to the corresponding service.

use std::collections::HashMap;

use chrono::{DateTime, Utc};
use sqlx::SqlitePool;
use uuid::Uuid;

use crate::auth::service::AuthServiceImpl;
use crate::debt::tracker::DebtServiceImpl;
use crate::error::{AppError, AppResult};
use crate::expenses::service::ExpenseServiceImpl;
use crate::inventory::finished_goods::FinishedGoodService;
use crate::inventory::raw_materials::RawMaterialService;
use crate::models::domain::*;
use crate::models::Money;
use crate::persistence::db;
use crate::recipes::management::RecipeServiceImpl;
use crate::recipes::production::ProductionServiceImpl;
use crate::reports::ReportServiceImpl;
use crate::sales::customers::CustomerService;
use crate::sales::receipts;
use crate::sales::transactions::SalesServiceImpl;
use crate::sync::SyncManagerImpl;
use crate::wallet::service::WalletServiceImpl;

/// The main entry point for all platform UIs.
///
/// Wraps all concrete service implementations into a single object.
/// Android calls this via UniFFI-generated Kotlin bindings (JNI);
/// the Desktop app calls it directly from Rust.
pub struct SweetLabCore {
    pool: SqlitePool,
    business_name: String,
    auth: AuthServiceImpl,
    raw_materials: RawMaterialService,
    finished_goods: FinishedGoodService,
    recipes: RecipeServiceImpl,
    production: ProductionServiceImpl,
    customers: CustomerService,
    sales: SalesServiceImpl,
    wallet: WalletServiceImpl,
    debt: DebtServiceImpl,
    expenses: ExpenseServiceImpl,
    reports: ReportServiceImpl,
    sync: SyncManagerImpl,
}

impl SweetLabCore {
    /// Initialize the core engine.
    ///
    /// 1. Creates a SQLite connection pool and runs migrations.
    /// 2. Creates all service implementations with built-in RBAC.
    pub async fn new(
        db_path: String,
        business_name: String,
        sync_base_url: Option<String>,
        font_dir: Option<String>,
    ) -> AppResult<Self> {
        // 1. Database
        let pool = db::init_db(&db_path).await?;

        // 2. Services (RBAC is built into AuthServiceImpl)
        let auth = AuthServiceImpl::new(pool.clone());
        let raw_materials = RawMaterialService::new(pool.clone());
        let finished_goods = FinishedGoodService::new(pool.clone());
        let recipes = RecipeServiceImpl::new(pool.clone());
        let production = ProductionServiceImpl::new(pool.clone());
        let customers = CustomerService::new(pool.clone());
        let sales = SalesServiceImpl::new(pool.clone());
        let wallet = WalletServiceImpl::new(pool.clone());
        let debt = DebtServiceImpl::new(pool.clone());
        let expenses = ExpenseServiceImpl::new(pool.clone());
        let reports = ReportServiceImpl::new(pool.clone(), font_dir);
        // Initialize sync with the provided URL, or empty string for offline-only mode (Req 20.1, 20.2)
        let sync = SyncManagerImpl::new(pool.clone(), sync_base_url.unwrap_or_default());

        Ok(Self {
            pool,
            business_name: business_name,
            auth,
            raw_materials,
            finished_goods,
            recipes,
            production,
            customers,
            sales,
            wallet,
            debt,
            expenses,
            reports,
            sync,
        })
    }

    // ── Authorization helpers ──────────────────────────────────────────

    /// Validate session and check RBAC permission. Returns the session on success.
    async fn authorize(&self, session_token: Uuid, resource: &str, action: &str) -> AppResult<Session> {
        let session = self.auth.get_session(session_token).await?
            .ok_or_else(|| AppError::Authentication {
                message: "Invalid or expired session".to_string(),
            })?;

        let now = Utc::now();
        if !session.is_valid(now) {
            self.auth.logout(session_token).await?;
            return Err(AppError::Authentication {
                message: "Session expired".to_string(),
            });
        }

        // Update last_activity (Req 10.2)
        self.auth.touch_session(session_token, now).await?;

        let allowed = self.auth.check_permission(&session.role, resource, action).await?;
        if !allowed {
            return Err(AppError::Authorization {
                actual_role: session.role.to_string(),
                resource: resource.to_string(),
            });
        }

        Ok(session)
    }

    /// Validate session exists and is not expired, but skip RBAC check.
    /// Used for read-only methods (get_*, list_*, search_*).
    async fn validate_session(&self, session_token: Uuid) -> AppResult<Session> {
        let session = self.auth.get_session(session_token).await?
            .ok_or_else(|| AppError::Authentication {
                message: "Invalid or expired session".to_string(),
            })?;

        let now = Utc::now();
        if !session.is_valid(now) {
            self.auth.logout(session_token).await?;
            return Err(AppError::Authentication {
                message: "Session expired".to_string(),
            });
        }

        // Update last_activity (Req 10.2)
        self.auth.touch_session(session_token, now).await?;

        Ok(session)
    }

    // ── Auth ───────────────────────────────────────────────────────────

    pub async fn login(&self, username: String, password: String) -> AppResult<Session> {
        self.auth.login(&username, &password).await
    }

    pub async fn logout(&self, session_id: Uuid) -> AppResult<()> {
        self.auth.logout(session_id).await
    }

    pub async fn create_user(
        &self,
        session_token: Uuid,
        username: String,
        password: String,
        full_name: String,
        role: UserRole,
    ) -> AppResult<SafeUser> {
        self.authorize(session_token, "users", "create").await?;
        self.auth.create_user(&username, &password, &full_name, role).await.map(SafeUser::from)
    }

    pub async fn update_user_role(
        &self,
        session_token: Uuid,
        user_id: Uuid,
        new_role: UserRole,
    ) -> AppResult<()> {
        self.authorize(session_token, "users", "update").await?;
        self.auth.update_user_role(user_id, new_role).await
    }

    pub async fn check_permission(
        &self,
        role: UserRole,
        resource: String,
        action: String,
    ) -> AppResult<bool> {
        self.auth.check_permission(&role, &resource, &action).await
    }

    // ── Inventory — Raw Materials ──────────────────────────────────────

    pub async fn get_raw_materials(&self, session_token: Uuid, pagination: Option<Pagination>) -> AppResult<Vec<RawMaterial>> {
        self.validate_session(session_token).await?;
        self.raw_materials.get_all(pagination).await
    }

    pub async fn add_raw_material_purchase(
        &self,
        session_token: Uuid,
        material_id: Uuid,
        quantity: f64,
    ) -> AppResult<RawMaterial> {
        self.authorize(session_token, "inventory", "update").await?;
        self.raw_materials.add_purchase(material_id, quantity).await
    }

    pub async fn deduct_raw_materials(
        &self,
        session_token: Uuid,
        deductions: HashMap<Uuid, f64>,
    ) -> AppResult<()> {
        self.authorize(session_token, "inventory", "update").await?;
        self.raw_materials.deduct_multiple(deductions).await
    }

    // ── Inventory — Finished Goods ─────────────────────────────────────

    pub async fn get_finished_goods(&self, session_token: Uuid, pagination: Option<Pagination>) -> AppResult<Vec<FinishedGood>> {
        self.validate_session(session_token).await?;
        self.finished_goods.get_all(pagination).await
    }

    pub async fn add_finished_goods(
        &self,
        session_token: Uuid,
        good_id: Uuid,
        quantity: f64,
    ) -> AppResult<FinishedGood> {
        self.authorize(session_token, "inventory", "update").await?;
        self.finished_goods.add(good_id, quantity).await
    }

    pub async fn deduct_finished_goods(
        &self,
        session_token: Uuid,
        good_id: Uuid,
        quantity: f64,
    ) -> AppResult<FinishedGood> {
        self.authorize(session_token, "inventory", "update").await?;
        self.finished_goods.deduct(good_id, quantity).await
    }

    // ── Recipes ────────────────────────────────────────────────────────

    pub async fn get_recipes(&self, session_token: Uuid, pagination: Option<Pagination>) -> AppResult<Vec<Recipe>> {
        self.validate_session(session_token).await?;
        self.recipes.get_recipes(pagination).await
    }

    pub async fn create_recipe(
        &self,
        session_token: Uuid,
        name: String,
        finished_good_id: Uuid,
        ingredients: Vec<RecipeIngredient>,
    ) -> AppResult<Recipe> {
        self.authorize(session_token, "recipes", "create").await?;
        self.recipes.create_recipe(&name, finished_good_id, ingredients).await
    }

    pub async fn update_recipe(&self, session_token: Uuid, recipe: Recipe) -> AppResult<Recipe> {
        self.authorize(session_token, "recipes", "update").await?;
        self.recipes.update_recipe(recipe).await
    }

    pub async fn delete_recipe(&self, session_token: Uuid, recipe_id: Uuid) -> AppResult<()> {
        self.authorize(session_token, "recipes", "delete").await?;
        self.recipes.delete_recipe(recipe_id).await
    }

    pub async fn validate_ingredients(
        &self,
        ingredients: Vec<RecipeIngredient>,
    ) -> AppResult<()> {
        self.recipes.validate_ingredients(&ingredients).await
    }

    // ── Production ─────────────────────────────────────────────────────

    pub async fn execute_production(
        &self,
        session_token: Uuid,
        recipe_id: Uuid,
        quantity: i32,
        chef_id: Uuid,
    ) -> AppResult<ProductionLog> {
        self.authorize(session_token, "production", "create").await?;
        self.production.execute_production(recipe_id, quantity, chef_id).await
    }

    pub async fn get_recipe_availability(&self, session_token: Uuid, pagination: Option<Pagination>) -> AppResult<Vec<RecipeAvailability>> {
        self.validate_session(session_token).await?;
        self.production.get_recipe_availability(pagination).await
    }

    pub async fn get_production_history(&self, session_token: Uuid, pagination: Option<Pagination>) -> AppResult<Vec<ProductionLog>> {
        self.validate_session(session_token).await?;
        self.production.get_production_history(pagination).await
    }

    // ── Customers ──────────────────────────────────────────────────────

    pub async fn get_customers(&self, session_token: Uuid, pagination: Option<Pagination>) -> AppResult<Vec<Customer>> {
        self.validate_session(session_token).await?;
        self.customers.get_customers(pagination).await
    }

    pub async fn create_customer(
        &self,
        session_token: Uuid,
        name: String,
        city: String,
        mobile: String,
    ) -> AppResult<Customer> {
        self.authorize(session_token, "customers", "create").await?;
        self.customers.create_customer(&name, &city, &mobile).await
    }

    pub async fn update_reliability_rating(
        &self,
        session_token: Uuid,
        customer_id: Uuid,
        rating: i32,
    ) -> AppResult<()> {
        self.authorize(session_token, "customers", "update").await?;
        self.customers.update_reliability_rating(customer_id, rating).await
    }

    pub async fn search_customers(&self, session_token: Uuid, query: String, pagination: Option<Pagination>) -> AppResult<Vec<Customer>> {
        self.validate_session(session_token).await?;
        self.customers.search_customers(&query, pagination).await
    }

    // ── Sales ──────────────────────────────────────────────────────────

    pub async fn create_sale(
        &self,
        session_token: Uuid,
        customer_id: Uuid,
        line_items: Vec<SaleLineItem>,
        amount_paid: Money,
        wallet_id: Uuid,
    ) -> AppResult<Sale> {
        self.authorize(session_token, "sales", "create").await?;
        self.sales.create_sale(customer_id, line_items, amount_paid, wallet_id).await
    }

    pub async fn get_sales_history(&self, session_token: Uuid, pagination: Option<Pagination>) -> AppResult<Vec<Sale>> {
        self.validate_session(session_token).await?;
        self.sales.get_sales_history(pagination).await
    }

    pub async fn generate_receipt(&self, session_token: Uuid, sale_id: Uuid) -> AppResult<Receipt> {
        self.validate_session(session_token).await?;
        receipts::generate_receipt(&self.pool, sale_id, &self.business_name).await
    }

    // ── Wallets ────────────────────────────────────────────────────────

    pub async fn get_wallets(&self, session_token: Uuid, pagination: Option<Pagination>) -> AppResult<Vec<Wallet>> {
        self.validate_session(session_token).await?;
        self.wallet.get_wallets(pagination).await
    }

    pub async fn get_transaction_history(
        &self,
        session_token: Uuid,
        wallet_id: Uuid,
        pagination: Option<Pagination>,
    ) -> AppResult<Vec<WalletTransaction>> {
        self.validate_session(session_token).await?;
        self.wallet.get_transaction_history(wallet_id, pagination).await
    }

    pub async fn transfer_funds(
        &self,
        session_token: Uuid,
        source_id: Uuid,
        destination_id: Uuid,
        amount: Money,
    ) -> AppResult<FundTransfer> {
        self.authorize(session_token, "wallets", "transfer").await?;
        self.wallet.transfer_funds(source_id, destination_id, amount).await
    }

    pub async fn credit_wallet(
        &self,
        session_token: Uuid,
        wallet_id: Uuid,
        amount: Money,
        description: String,
        related_entity_id: Option<Uuid>,
    ) -> AppResult<()> {
        self.authorize(session_token, "wallets", "credit").await?;
        self.wallet.credit_wallet(wallet_id, amount, &description, related_entity_id).await
    }

    pub async fn debit_wallet(
        &self,
        session_token: Uuid,
        wallet_id: Uuid,
        amount: Money,
        description: String,
        related_entity_id: Option<Uuid>,
    ) -> AppResult<()> {
        self.authorize(session_token, "wallets", "debit").await?;
        self.wallet.debit_wallet(wallet_id, amount, &description, related_entity_id).await
    }

    // ── Debt ───────────────────────────────────────────────────────────

    pub async fn get_active_debts(&self, session_token: Uuid, pagination: Option<Pagination>) -> AppResult<Vec<DebtRecord>> {
        self.validate_session(session_token).await?;
        self.debt.get_active_debts(pagination).await
    }

    pub async fn get_debt_aging_report(&self, session_token: Uuid, pagination: Option<Pagination>) -> AppResult<Vec<DebtRecord>> {
        self.validate_session(session_token).await?;
        self.debt.get_debt_aging_report(pagination).await
    }

    pub async fn record_debt_payment(
        &self,
        session_token: Uuid,
        customer_id: Uuid,
        amount: Money,
        wallet_id: Uuid,
    ) -> AppResult<DebtPayment> {
        self.authorize(session_token, "debts", "create").await?;
        self.debt.record_payment(customer_id, amount, wallet_id).await
    }

    pub async fn create_debt_record(
        &self,
        session_token: Uuid,
        customer_id: Uuid,
        sale_id: Uuid,
        amount: Money,
    ) -> AppResult<DebtRecord> {
        self.authorize(session_token, "debts", "create").await?;
        self.debt.create_debt_record(customer_id, sale_id, amount).await
    }

    // ── Expenses ───────────────────────────────────────────────────────

    pub async fn record_expense(
        &self,
        session_token: Uuid,
        description: String,
        amount: Money,
        category: ExpenseCategory,
        wallet_id: Uuid,
        recorded_by: Uuid,
    ) -> AppResult<Expense> {
        self.authorize(session_token, "expenses", "create").await?;
        self.expenses
            .record_expense(&description, amount, category, wallet_id, recorded_by)
            .await
    }

    pub async fn get_expenses(
        &self,
        session_token: Uuid,
        start_date: DateTime<Utc>,
        end_date: DateTime<Utc>,
        pagination: Option<Pagination>,
    ) -> AppResult<Vec<Expense>> {
        self.validate_session(session_token).await?;
        self.expenses.get_expenses(start_date, end_date, pagination).await
    }

    pub async fn get_expenses_by_category(
        &self,
        session_token: Uuid,
        start_date: DateTime<Utc>,
        end_date: DateTime<Utc>,
        pagination: Option<Pagination>,
    ) -> AppResult<Vec<ExpenseCategoryGroup>> {
        self.validate_session(session_token).await?;
        let map = self.expenses.get_expenses_by_category(start_date, end_date, pagination).await?;
        Ok(map.into_iter().map(|(category, expenses)| ExpenseCategoryGroup { category, expenses }).collect())
    }

    // ── Reports ────────────────────────────────────────────────────────

    pub async fn get_financial_summary(
        &self,
        session_token: Uuid,
        start_date: DateTime<Utc>,
        end_date: DateTime<Utc>,
    ) -> AppResult<FinancialSummary> {
        self.validate_session(session_token).await?;
        self.reports.get_financial_summary(start_date, end_date).await
    }

    pub async fn get_inventory_report(
        &self,
        session_token: Uuid,
        low_stock_threshold: f64,
    ) -> AppResult<InventoryReport> {
        self.validate_session(session_token).await?;
        self.reports.get_inventory_report(low_stock_threshold).await
    }

    pub async fn generate_invoice(&self, session_token: Uuid, sale_id: Uuid) -> AppResult<Invoice> {
        self.validate_session(session_token).await?;
        self.reports.generate_invoice(sale_id, &self.business_name).await
    }

    pub async fn export_to_pdf(&self, invoice: Invoice) -> AppResult<Vec<u8>> {
        self.reports.export_to_pdf(&invoice).await
    }

    pub async fn export_to_excel(&self, invoice: Invoice) -> AppResult<Vec<u8>> {
        self.reports.export_to_excel(&invoice).await
    }

    // ── Sync ───────────────────────────────────────────────────────────

    pub async fn is_online(&self) -> bool {
        self.sync.is_online().await
    }

    pub async fn get_sync_status(&self) -> SyncStatus {
        self.sync.get_sync_status().await
    }

    pub async fn sync_all(&self) -> AppResult<()> {
        self.sync.sync_all().await
    }

    pub async fn queue_modification(
        &self,
        entity_type: String,
        entity_id: Uuid,
        operation: String,
        payload: String,
    ) -> AppResult<()> {
        self.sync
            .queue_modification(&entity_type, entity_id, &operation, &payload)
            .await
    }

    pub async fn get_conflict_log(&self, pagination: Option<Pagination>) -> AppResult<Vec<ConflictLog>> {
        let pagination = pagination.unwrap_or_default();
        self.sync.get_conflict_log(&pagination).await
    }

    pub async fn cleanup_conflict_log(&self, retention_days: i64) -> AppResult<u64> {
        self.sync.cleanup_conflict_log(retention_days).await
    }
}
