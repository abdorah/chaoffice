//! UniFFI-exported public API surface.
//!
//! `SweetLabCore` is the single entry point for all platform UIs.
//! It wraps every service implementation and delegates each public method
//! to the corresponding service.

use std::collections::HashMap;
use std::sync::Arc;

use chrono::{DateTime, Utc};
use sqlx::SqlitePool;
use tokio::sync::Mutex;
use uuid::Uuid;

use crate::auth::rbac;
use crate::auth::service::AuthServiceImpl;
use crate::debt::tracker::DebtServiceImpl;
use crate::error::AppResult;
use crate::expenses::service::ExpenseServiceImpl;
use crate::inventory::finished_goods::FinishedGoodService;
use crate::inventory::raw_materials::RawMaterialService;
use crate::models::domain::*;
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
    /// 2. Initializes the Casbin-RS enforcer from `policies_dir/model.conf`
    ///    and `policies_dir/policy.csv`.
    /// 3. Creates all service implementations.
    pub async fn new(db_path: &str, policies_dir: &str) -> AppResult<Self> {
        // 1. Database
        let pool = db::init_db(db_path).await?;

        // 2. Casbin RBAC
        let model_path = format!("{}/model.conf", policies_dir);
        let policy_path = format!("{}/policy.csv", policies_dir);
        let enforcer = rbac::init_enforcer(&model_path, &policy_path).await?;
        let enforcer = Arc::new(Mutex::new(enforcer));

        // 3. Services
        let auth = AuthServiceImpl::new(pool.clone(), enforcer);
        let raw_materials = RawMaterialService::new(pool.clone());
        let finished_goods = FinishedGoodService::new(pool.clone());
        let recipes = RecipeServiceImpl::new(pool.clone());
        let production = ProductionServiceImpl::new(pool.clone());
        let customers = CustomerService::new(pool.clone());
        let sales = SalesServiceImpl::new(pool.clone());
        let wallet = WalletServiceImpl::new(pool.clone());
        let debt = DebtServiceImpl::new(pool.clone());
        let expenses = ExpenseServiceImpl::new(pool.clone());
        let reports = ReportServiceImpl::new(pool.clone());
        let sync = SyncManagerImpl::new(pool.clone(), String::new());

        Ok(Self {
            pool,
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

    // ── Auth ───────────────────────────────────────────────────────────

    pub async fn login(&self, username: &str, password: &str) -> AppResult<Session> {
        self.auth.login(username, password).await
    }

    pub async fn logout(&self, session_id: Uuid) -> AppResult<()> {
        self.auth.logout(session_id).await
    }

    pub async fn create_user(
        &self,
        username: &str,
        password: &str,
        full_name: &str,
        role: UserRole,
    ) -> AppResult<AppUser> {
        self.auth.create_user(username, password, full_name, role).await
    }

    pub async fn update_user_role(&self, user_id: Uuid, new_role: UserRole) -> AppResult<()> {
        self.auth.update_user_role(user_id, new_role).await
    }

    pub async fn check_permission(
        &self,
        role: &UserRole,
        resource: &str,
        action: &str,
    ) -> AppResult<bool> {
        self.auth.check_permission(role, resource, action).await
    }

    // ── Inventory — Raw Materials ──────────────────────────────────────

    pub async fn get_raw_materials(&self) -> AppResult<Vec<RawMaterial>> {
        self.raw_materials.get_all().await
    }

    pub async fn add_raw_material_purchase(
        &self,
        material_id: Uuid,
        quantity: f64,
    ) -> AppResult<RawMaterial> {
        self.raw_materials.add_purchase(material_id, quantity).await
    }

    pub async fn deduct_raw_materials(
        &self,
        deductions: HashMap<Uuid, f64>,
    ) -> AppResult<()> {
        self.raw_materials.deduct_multiple(deductions).await
    }

    // ── Inventory — Finished Goods ─────────────────────────────────────

    pub async fn get_finished_goods(&self) -> AppResult<Vec<FinishedGood>> {
        self.finished_goods.get_all().await
    }

    pub async fn add_finished_goods(
        &self,
        good_id: Uuid,
        quantity: f64,
    ) -> AppResult<FinishedGood> {
        self.finished_goods.add(good_id, quantity).await
    }

    pub async fn deduct_finished_goods(
        &self,
        good_id: Uuid,
        quantity: f64,
    ) -> AppResult<FinishedGood> {
        self.finished_goods.deduct(good_id, quantity).await
    }

    // ── Recipes ────────────────────────────────────────────────────────

    pub async fn get_recipes(&self) -> AppResult<Vec<Recipe>> {
        self.recipes.get_recipes().await
    }

    pub async fn create_recipe(
        &self,
        name: &str,
        finished_good_id: Uuid,
        ingredients: Vec<RecipeIngredient>,
    ) -> AppResult<Recipe> {
        self.recipes.create_recipe(name, finished_good_id, ingredients).await
    }

    pub async fn update_recipe(&self, recipe: Recipe) -> AppResult<Recipe> {
        self.recipes.update_recipe(recipe).await
    }

    pub async fn delete_recipe(&self, recipe_id: Uuid) -> AppResult<()> {
        self.recipes.delete_recipe(recipe_id).await
    }

    pub async fn validate_ingredients(
        &self,
        ingredients: &[RecipeIngredient],
    ) -> AppResult<()> {
        self.recipes.validate_ingredients(ingredients).await
    }

    // ── Production ─────────────────────────────────────────────────────

    pub async fn execute_production(
        &self,
        recipe_id: Uuid,
        quantity: i32,
        chef_id: Uuid,
    ) -> AppResult<ProductionLog> {
        self.production.execute_production(recipe_id, quantity, chef_id).await
    }

    pub async fn get_recipe_availability(&self) -> AppResult<Vec<RecipeAvailability>> {
        self.production.get_recipe_availability().await
    }

    pub async fn get_production_history(&self) -> AppResult<Vec<ProductionLog>> {
        self.production.get_production_history().await
    }

    // ── Customers ──────────────────────────────────────────────────────

    pub async fn get_customers(&self) -> AppResult<Vec<Customer>> {
        self.customers.get_customers().await
    }

    pub async fn create_customer(
        &self,
        name: &str,
        city: &str,
        mobile: &str,
    ) -> AppResult<Customer> {
        self.customers.create_customer(name, city, mobile).await
    }

    pub async fn update_reliability_rating(
        &self,
        customer_id: Uuid,
        rating: i32,
    ) -> AppResult<()> {
        self.customers.update_reliability_rating(customer_id, rating).await
    }

    pub async fn search_customers(&self, query: &str) -> AppResult<Vec<Customer>> {
        self.customers.search_customers(query).await
    }

    // ── Sales ──────────────────────────────────────────────────────────

    pub async fn create_sale(
        &self,
        customer_id: Uuid,
        line_items: Vec<SaleLineItem>,
        amount_paid: f64,
        wallet_id: Uuid,
    ) -> AppResult<Sale> {
        self.sales.create_sale(customer_id, line_items, amount_paid, wallet_id).await
    }

    pub async fn get_sales_history(&self) -> AppResult<Vec<Sale>> {
        self.sales.get_sales_history().await
    }

    pub async fn generate_receipt(&self, sale_id: Uuid) -> AppResult<Receipt> {
        receipts::generate_receipt(&self.pool, sale_id).await
    }

    // ── Wallets ────────────────────────────────────────────────────────

    pub async fn get_wallets(&self) -> AppResult<Vec<Wallet>> {
        self.wallet.get_wallets().await
    }

    pub async fn get_transaction_history(
        &self,
        wallet_id: Uuid,
    ) -> AppResult<Vec<WalletTransaction>> {
        self.wallet.get_transaction_history(wallet_id).await
    }

    pub async fn transfer_funds(
        &self,
        source_id: Uuid,
        destination_id: Uuid,
        amount: f64,
    ) -> AppResult<FundTransfer> {
        self.wallet.transfer_funds(source_id, destination_id, amount).await
    }

    pub async fn credit_wallet(
        &self,
        wallet_id: Uuid,
        amount: f64,
        description: &str,
        related_entity_id: Option<Uuid>,
    ) -> AppResult<()> {
        self.wallet.credit_wallet(wallet_id, amount, description, related_entity_id).await
    }

    pub async fn debit_wallet(
        &self,
        wallet_id: Uuid,
        amount: f64,
        description: &str,
        related_entity_id: Option<Uuid>,
    ) -> AppResult<()> {
        self.wallet.debit_wallet(wallet_id, amount, description, related_entity_id).await
    }

    // ── Debt ───────────────────────────────────────────────────────────

    pub async fn get_active_debts(&self) -> AppResult<Vec<DebtRecord>> {
        self.debt.get_active_debts().await
    }

    pub async fn get_debt_aging_report(&self) -> AppResult<Vec<DebtRecord>> {
        self.debt.get_debt_aging_report().await
    }

    pub async fn record_debt_payment(
        &self,
        customer_id: Uuid,
        amount: f64,
        wallet_id: Uuid,
    ) -> AppResult<DebtPayment> {
        self.debt.record_payment(customer_id, amount, wallet_id).await
    }

    pub async fn create_debt_record(
        &self,
        customer_id: Uuid,
        sale_id: Uuid,
        amount: f64,
    ) -> AppResult<DebtRecord> {
        self.debt.create_debt_record(customer_id, sale_id, amount).await
    }

    // ── Expenses ───────────────────────────────────────────────────────

    pub async fn record_expense(
        &self,
        description: &str,
        amount: f64,
        category: ExpenseCategory,
        wallet_id: Uuid,
        recorded_by: Uuid,
    ) -> AppResult<Expense> {
        self.expenses
            .record_expense(description, amount, category, wallet_id, recorded_by)
            .await
    }

    pub async fn get_expenses(
        &self,
        start_date: DateTime<Utc>,
        end_date: DateTime<Utc>,
    ) -> AppResult<Vec<Expense>> {
        self.expenses.get_expenses(start_date, end_date).await
    }

    pub async fn get_expenses_by_category(
        &self,
        start_date: DateTime<Utc>,
        end_date: DateTime<Utc>,
    ) -> AppResult<HashMap<ExpenseCategory, Vec<Expense>>> {
        self.expenses.get_expenses_by_category(start_date, end_date).await
    }

    // ── Reports ────────────────────────────────────────────────────────

    pub async fn get_financial_summary(
        &self,
        start_date: DateTime<Utc>,
        end_date: DateTime<Utc>,
    ) -> AppResult<FinancialSummary> {
        self.reports.get_financial_summary(start_date, end_date).await
    }

    pub async fn get_inventory_report(
        &self,
        low_stock_threshold: f64,
    ) -> AppResult<InventoryReport> {
        self.reports.get_inventory_report(low_stock_threshold).await
    }

    pub async fn generate_invoice(&self, sale_id: Uuid) -> AppResult<Invoice> {
        self.reports.generate_invoice(sale_id).await
    }

    pub async fn export_to_pdf(&self, invoice: &Invoice) -> AppResult<Vec<u8>> {
        self.reports.export_to_pdf(invoice).await
    }

    pub async fn export_to_excel(&self, invoice: &Invoice) -> AppResult<Vec<u8>> {
        self.reports.export_to_excel(invoice).await
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
        entity_type: &str,
        entity_id: Uuid,
        operation: &str,
        payload: &str,
    ) -> AppResult<()> {
        self.sync
            .queue_modification(entity_type, entity_id, operation, payload)
            .await
    }

    pub async fn get_conflict_log(&self) -> AppResult<Vec<ConflictLog>> {
        self.sync.get_conflict_log().await
    }
}
