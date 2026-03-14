use chrono::{DateTime, Duration, Utc};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::fmt;
use uuid::Uuid;

use super::Money;

// ── Enums ──────────────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub enum UserRole {
    Admin,
    Chef,
    Representative,
}

impl fmt::Display for UserRole {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            UserRole::Admin => write!(f, "Admin"),
            UserRole::Chef => write!(f, "Chef"),
            UserRole::Representative => write!(f, "Representative"),
        }
    }
}

impl UserRole {
    /// Parse a role from its string representation (as stored in SQLite).
    pub fn from_str_value(s: &str) -> Option<UserRole> {
        match s {
            "Admin" => Some(UserRole::Admin),
            "Chef" => Some(UserRole::Chef),
            "Representative" => Some(UserRole::Representative),
            _ => None,
        }
    }

    /// Return the navigation destination screen for this role after login.
    ///
    /// - Admin → "admin_dashboard"
    /// - Chef → "production_screen"
    /// - Representative → "sales_screen"
    pub fn navigation_destination(&self) -> &'static str {
        match self {
            UserRole::Admin => "admin_dashboard",
            UserRole::Chef => "production_screen",
            UserRole::Representative => "sales_screen",
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub enum WalletType {
    Bank,
    Cash,
    Representative,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq, Hash)]
pub enum ExpenseCategory {
    Purchase,
    OperatingCost,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub enum SyncStatus {
    Synced,
    Pending,
    Conflict,
}

// ── Auth & Users ───────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AppUser {
    pub id: Uuid,
    pub username: String,
    pub full_name: String,
    pub role: UserRole,
    pub password_hash: String,
}

/// User data safe for API responses — no password_hash.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SafeUser {
    pub id: Uuid,
    pub username: String,
    pub full_name: String,
    pub role: UserRole,
}

impl From<AppUser> for SafeUser {
    fn from(u: AppUser) -> Self {
        SafeUser {
            id: u.id,
            username: u.username,
            full_name: u.full_name,
            role: u.role,
        }
    }
}

#[derive(Debug, Clone)]
pub struct Session {
    pub session_id: Uuid,
    pub user_id: Uuid,
    pub role: UserRole,
    pub created_at: DateTime<Utc>,
    pub last_activity: DateTime<Utc>,
    pub expires_at: DateTime<Utc>,
}

impl Session {
    /// A session is valid if it hasn't expired and the inactivity gap is under 8 hours.
    pub fn is_valid(&self, now: DateTime<Utc>) -> bool {
        now < self.expires_at && (now - self.last_activity) < Duration::hours(8)
    }
}

// ── Inventory ──────────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RawMaterial {
    pub id: Uuid,
    pub name: String,
    pub unit: String,
    pub current_quantity: f64,
    pub last_updated: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FinishedGood {
    pub id: Uuid,
    pub name: String,
    pub current_quantity: f64,
    pub unit_price: Money,
    pub last_updated: DateTime<Utc>,
}

// ── Recipes & Production ───────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RecipeIngredient {
    pub raw_material_id: Uuid,
    pub raw_material_name: String,
    pub required_quantity: f64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Recipe {
    pub id: Uuid,
    pub name: String,
    pub finished_good_id: Uuid,
    pub finished_good_name: String,
    pub ingredients: Vec<RecipeIngredient>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RecipeAvailability {
    pub recipe: Recipe,
    pub max_producible: i32,
    pub insufficient_materials: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ProductionLog {
    pub id: Uuid,
    pub recipe_id: Uuid,
    pub recipe_name: String,
    pub chef_id: Uuid,
    pub chef_name: String,
    pub production_quantity: i32,
    pub materials_consumed: HashMap<Uuid, f64>,
    pub finished_good_id: Uuid,
    pub timestamp: DateTime<Utc>,
}

// ── Customers ──────────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Customer {
    pub id: Uuid,
    pub name: String,
    pub city: String,
    pub mobile: String,
    pub reliability_rating: i32,
    pub total_debt: Money,
    pub overdue_days: i32,
}

// ── Sales ──────────────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SaleLineItem {
    pub finished_good_id: Uuid,
    pub finished_good_name: String,
    pub quantity: i32,
    pub unit_price: Money,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Sale {
    pub id: Uuid,
    pub customer_id: Uuid,
    pub customer_name: String,
    pub line_items: Vec<SaleLineItem>,
    pub total_amount: Money,
    pub amount_paid: Money,
    pub payment_wallet_id: Uuid,
    pub timestamp: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Receipt {
    pub sale: Sale,
    pub customer_name: String,
    pub customer_city: String,
    pub customer_mobile: String,
    pub business_name: String,
    pub remaining_balance: Money,
    pub formatted_date: String,
}

// ── Wallets ────────────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Wallet {
    pub id: Uuid,
    pub name: String,
    pub wallet_type: WalletType,
    pub current_balance: Money,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WalletTransaction {
    pub id: Uuid,
    pub wallet_id: Uuid,
    pub amount: Money,
    pub description: String,
    pub related_entity_id: Option<Uuid>,
    pub timestamp: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FundTransfer {
    pub id: Uuid,
    pub source_wallet_id: Uuid,
    pub destination_wallet_id: Uuid,
    pub amount: Money,
    pub timestamp: DateTime<Utc>,
}

// ── Debt Tracking ──────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DebtRecord {
    pub id: Uuid,
    pub customer_id: Uuid,
    pub customer_name: String,
    pub sale_id: Uuid,
    pub original_amount: Money,
    pub remaining_amount: Money,
    pub sale_date: DateTime<Utc>,
    pub overdue_days: i32,
    pub is_critical: bool,
    pub is_settled: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DebtPayment {
    pub id: Uuid,
    pub customer_id: Uuid,
    pub amount: Money,
    pub unallocated: Money,
    pub wallet_id: Uuid,
    pub allocations: Vec<DebtAllocation>,
    pub timestamp: DateTime<Utc>,
}


#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DebtAllocation {
    pub debt_record_id: Uuid,
    pub amount_applied: Money,
}

// ── Expenses ───────────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Expense {
    pub id: Uuid,
    pub description: String,
    pub amount: Money,
    pub category: ExpenseCategory,
    pub wallet_id: Uuid,
    pub wallet_name: String,
    pub recorded_by: Uuid,
    pub timestamp: DateTime<Utc>,
}

// ── Reporting ──────────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FinancialSummary {
    pub total_revenue: Money,
    pub total_expenses: Money,
    pub net_profit: Money,
    pub wallet_balances: Vec<Wallet>,
    pub start_date: DateTime<Utc>,
    pub end_date: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct InventoryReport {
    pub raw_materials: Vec<RawMaterialReport>,
    pub finished_goods: Vec<FinishedGoodReport>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RawMaterialReport {
    pub material: RawMaterial,
    pub is_low_stock: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FinishedGoodReport {
    pub good: FinishedGood,
    pub is_low_stock: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Invoice {
    pub business_name: String,
    pub customer_name: String,
    pub customer_city: String,
    pub customer_mobile: String,
    pub line_items: Vec<SaleLineItem>,
    pub total_amount: Money,
    pub amount_paid: Money,
    pub remaining_balance: Money,
    pub date: String,
    pub invoice_number: String,
}

// ── Pagination ─────────────────────────────────────────────────────────────

#[derive(Debug, Clone)]
pub struct Pagination {
    pub limit: i64,
    pub offset: i64,
}

impl Default for Pagination {
    fn default() -> Self {
        Pagination { limit: 100, offset: 0 }
    }
}

// ── Sync ───────────────────────────────────────────────────────────────────

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SyncQueueItem {
    pub id: Uuid,
    pub entity_type: String,
    pub entity_id: Uuid,
    pub operation: String,
    pub payload: String,
    pub created_at: DateTime<Utc>,
    pub status: SyncStatus,
}

/// Wrapper for HashMap<ExpenseCategory, Vec<Expense>> for UniFFI compatibility.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ExpenseCategoryGroup {
    pub category: ExpenseCategory,
    pub expenses: Vec<Expense>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ConflictLog {
    pub id: Uuid,
    pub entity_type: String,
    pub entity_id: Uuid,
    pub local_version: String,
    pub remote_version: String,
    pub resolved_with: String,
    pub timestamp: DateTime<Utc>,
}
