//! Integration tests for critical end-to-end flows.
//!
//! These tests exercise the full production, sale, and payment pipelines
//! against an in-memory SQLite database, verifying that inventory, wallet,
//! and debt state changes are correct and atomic.
//!
//! **Validates: Requirements 5.2, 8.2, 8.3, 9.5, 10.3**

use chrono::Utc;
use sqlx::SqlitePool;
use uuid::Uuid;

use sweet_lab_core::debt::tracker::DebtServiceImpl;
use sweet_lab_core::models::domain::SaleLineItem;
use sweet_lab_core::models::Money;
use sweet_lab_core::persistence::db;
use sweet_lab_core::persistence::queries::customers as customer_queries;
use sweet_lab_core::persistence::queries::finished_goods as fg_queries;
use sweet_lab_core::persistence::queries::raw_materials as rm_queries;
use sweet_lab_core::persistence::queries::recipes as recipe_queries;
use sweet_lab_core::persistence::queries::users as user_queries;
use sweet_lab_core::persistence::queries::wallets as wallet_queries;
use sweet_lab_core::recipes::production::ProductionServiceImpl;
use sweet_lab_core::sales::transactions::SalesServiceImpl;

// ── Helpers ────────────────────────────────────────────────────────────────

async fn init_pool() -> SqlitePool {
    db::init_db(":memory:").await.expect("DB init failed")
}

struct ProductionFixture {
    chef_id: Uuid,
    recipe_id: Uuid,
    fg_id: Uuid,
    rm_milk_id: Uuid,
    rm_sugar_id: Uuid,
}

async fn seed_production_fixture(pool: &SqlitePool) -> ProductionFixture {
    let now = Utc::now().to_rfc3339();
    let chef_id = Uuid::new_v4();
    let recipe_id = Uuid::new_v4();
    let fg_id = Uuid::new_v4();
    let rm_milk_id = Uuid::new_v4();
    let rm_sugar_id = Uuid::new_v4();

    // Chef user
    user_queries::insert_user(pool, &chef_id.to_string(), "chef1", "Chef Ahmad", "Chef", "hash")
        .await.unwrap();

    // Raw materials
    rm_queries::insert(pool, &rm_milk_id.to_string(), "Milk", "litre", 100.0, &now).await.unwrap();
    rm_queries::insert(pool, &rm_sugar_id.to_string(), "Sugar", "kg", 50.0, &now).await.unwrap();

    // Finished good
    fg_queries::insert(pool, &fg_id.to_string(), "Sweet Box", 0.0, 2500, &now).await.unwrap();

    // Recipe: 1 Sweet Box = 2L milk + 0.5kg sugar
    recipe_queries::insert_recipe(pool, &recipe_id.to_string(), "Sweet Box Recipe", &fg_id.to_string(), &now).await.unwrap();
    recipe_queries::insert_ingredient(pool, &Uuid::new_v4().to_string(), &recipe_id.to_string(), &rm_milk_id.to_string(), 2.0).await.unwrap();
    recipe_queries::insert_ingredient(pool, &Uuid::new_v4().to_string(), &recipe_id.to_string(), &rm_sugar_id.to_string(), 0.5).await.unwrap();

    ProductionFixture { chef_id, recipe_id, fg_id, rm_milk_id, rm_sugar_id }
}

struct SaleFixture {
    customer_id: Uuid,
    wallet_id: Uuid,
    fg_id: Uuid,
}

async fn seed_sale_fixture(pool: &SqlitePool, fg_qty: f64) -> SaleFixture {
    let now = Utc::now().to_rfc3339();
    let customer_id = Uuid::new_v4();
    let wallet_id = Uuid::new_v4();
    let fg_id = Uuid::new_v4();

    customer_queries::insert(pool, &customer_id.to_string(), "Khaled", "Aleppo", "+963922222222", &now)
        .await.unwrap();
    wallet_queries::insert_wallet(pool, &wallet_id.to_string(), "Cash Box", "Cash", 100000, &now)
        .await.unwrap();
    fg_queries::insert(pool, &fg_id.to_string(), "Chocolate Bar", fg_qty, 1000, &now)
        .await.unwrap();

    SaleFixture { customer_id, wallet_id, fg_id }
}

// ═══════════════════════════════════════════════════════════════════════════
// Flow 1: Create recipe → Execute production → Verify inventory changes
// Validates: Requirement 5.2
// ═══════════════════════════════════════════════════════════════════════════

#[tokio::test]
async fn production_flow_deducts_raw_materials_and_adds_finished_goods() {
    let pool = init_pool().await;
    let f = seed_production_fixture(&pool).await;
    let production_svc = ProductionServiceImpl::new(pool.clone());

    // Produce 10 Sweet Boxes (requires 20L milk, 5kg sugar)
    let log = production_svc.execute_production(f.recipe_id, 10, f.chef_id).await.unwrap();

    // Production log is correct
    assert_eq!(log.production_quantity, 10);
    assert_eq!(log.recipe_id, f.recipe_id);
    assert_eq!(log.chef_id, f.chef_id);
    assert_eq!(log.finished_good_id, f.fg_id);

    // Raw materials decreased: milk 100 - 20 = 80, sugar 50 - 5 = 45
    let milk = rm_queries::get_by_id(&pool, &f.rm_milk_id.to_string()).await.unwrap().unwrap();
    assert_eq!(milk.current_quantity, 80.0);

    let sugar = rm_queries::get_by_id(&pool, &f.rm_sugar_id.to_string()).await.unwrap().unwrap();
    assert_eq!(sugar.current_quantity, 45.0);

    // Finished good increased: 0 + 10 = 10
    let fg = fg_queries::get_by_id(&pool, &f.fg_id.to_string()).await.unwrap().unwrap();
    assert_eq!(fg.current_quantity, 10.0);
}

#[tokio::test]
async fn production_flow_insufficient_stock_rejects_and_leaves_inventory_unchanged() {
    let pool = init_pool().await;
    let f = seed_production_fixture(&pool).await;
    let production_svc = ProductionServiceImpl::new(pool.clone());

    // Try to produce 200 Sweet Boxes (needs 400L milk — only 100 available)
    let err = production_svc.execute_production(f.recipe_id, 200, f.chef_id).await.unwrap_err();
    assert!(format!("{err:?}").contains("stock") || format!("{err:?}").contains("Validation"));

    // All quantities unchanged
    let milk = rm_queries::get_by_id(&pool, &f.rm_milk_id.to_string()).await.unwrap().unwrap();
    assert_eq!(milk.current_quantity, 100.0);

    let sugar = rm_queries::get_by_id(&pool, &f.rm_sugar_id.to_string()).await.unwrap().unwrap();
    assert_eq!(sugar.current_quantity, 50.0);

    let fg = fg_queries::get_by_id(&pool, &f.fg_id.to_string()).await.unwrap().unwrap();
    assert_eq!(fg.current_quantity, 0.0);
}

#[tokio::test]
async fn production_flow_multiple_runs_accumulate_correctly() {
    let pool = init_pool().await;
    let f = seed_production_fixture(&pool).await;
    let production_svc = ProductionServiceImpl::new(pool.clone());

    // Run 1: produce 5 (uses 10L milk, 2.5kg sugar)
    production_svc.execute_production(f.recipe_id, 5, f.chef_id).await.unwrap();
    // Run 2: produce 3 (uses 6L milk, 1.5kg sugar)
    production_svc.execute_production(f.recipe_id, 3, f.chef_id).await.unwrap();

    // milk: 100 - 10 - 6 = 84, sugar: 50 - 2.5 - 1.5 = 46, fg: 0 + 5 + 3 = 8
    let milk = rm_queries::get_by_id(&pool, &f.rm_milk_id.to_string()).await.unwrap().unwrap();
    assert_eq!(milk.current_quantity, 84.0);

    let sugar = rm_queries::get_by_id(&pool, &f.rm_sugar_id.to_string()).await.unwrap().unwrap();
    assert_eq!(sugar.current_quantity, 46.0);

    let fg = fg_queries::get_by_id(&pool, &f.fg_id.to_string()).await.unwrap().unwrap();
    assert_eq!(fg.current_quantity, 8.0);

    // Production history has 2 entries
    let history = production_svc.get_production_history(None).await.unwrap();
    assert_eq!(history.len(), 2);
}

// ═══════════════════════════════════════════════════════════════════════════
// Flow 2: Create sale → Verify inventory + wallet + debt changes
// Validates: Requirements 8.2, 8.3
// ═══════════════════════════════════════════════════════════════════════════

#[tokio::test]
async fn sale_flow_full_payment_credits_wallet_deducts_inventory_no_debt() {
    let pool = init_pool().await;
    let f = seed_sale_fixture(&pool, 50.0).await;
    let sales_svc = SalesServiceImpl::new(pool.clone());

    let items = vec![SaleLineItem {
        finished_good_id: f.fg_id,
        finished_good_name: "Chocolate Bar".into(),
        quantity: 5,
        unit_price: Money::from_f64(10.0),
    }];

    // Full payment: total = 50, paid = 50
    let sale = sales_svc.create_sale(f.customer_id, items, Money::from_f64(50.0), f.wallet_id).await.unwrap();
    assert_eq!(sale.total_amount, Money::from_f64(50.0));
    assert_eq!(sale.amount_paid, Money::from_f64(50.0));

    // Wallet: 1000 + 50 = 1050 (check via raw query — WalletRow uses f64)
    let wallet = wallet_queries::get_by_id(&pool, &f.wallet_id.to_string()).await.unwrap().unwrap();
    assert_eq!(wallet.current_balance, 105000);

    // Inventory: 50 - 5 = 45
    let fg = fg_queries::get_by_id(&pool, &f.fg_id.to_string()).await.unwrap().unwrap();
    assert_eq!(fg.current_quantity, 45.0);

    // No debt
    let debt_count: (i64,) = sqlx::query_as("SELECT COUNT(*) FROM debt_records WHERE customer_id = ?")
        .bind(f.customer_id.to_string())
        .fetch_one(&pool).await.unwrap();
    assert_eq!(debt_count.0, 0);
}

#[tokio::test]
async fn sale_flow_partial_payment_creates_debt_for_remainder() {
    let pool = init_pool().await;
    let f = seed_sale_fixture(&pool, 50.0).await;
    let sales_svc = SalesServiceImpl::new(pool.clone());

    let items = vec![SaleLineItem {
        finished_good_id: f.fg_id,
        finished_good_name: "Chocolate Bar".into(),
        quantity: 8,
        unit_price: Money::from_f64(10.0),
    }];

    // Partial payment: total = 80, paid = 30 → debt = 50
    let sale = sales_svc.create_sale(f.customer_id, items, Money::from_f64(30.0), f.wallet_id).await.unwrap();
    assert_eq!(sale.total_amount, Money::from_f64(80.0));
    assert_eq!(sale.amount_paid, Money::from_f64(30.0));

    // Wallet: 1000 + 30 = 1030
    let wallet = wallet_queries::get_by_id(&pool, &f.wallet_id.to_string()).await.unwrap().unwrap();
    assert_eq!(wallet.current_balance, 103000);

    // Inventory: 50 - 8 = 42
    let fg = fg_queries::get_by_id(&pool, &f.fg_id.to_string()).await.unwrap().unwrap();
    assert_eq!(fg.current_quantity, 42.0);

    // Debt record for 50 (remaining_amount is INTEGER cents)
    let debt: (i64,) = sqlx::query_as("SELECT remaining_amount FROM debt_records WHERE sale_id = ?")
        .bind(sale.id.to_string())
        .fetch_one(&pool).await.unwrap();
    assert_eq!(debt.0, 5000);
}

#[tokio::test]
async fn sale_flow_zero_payment_creates_full_debt() {
    let pool = init_pool().await;
    let f = seed_sale_fixture(&pool, 50.0).await;
    let sales_svc = SalesServiceImpl::new(pool.clone());

    let items = vec![SaleLineItem {
        finished_good_id: f.fg_id,
        finished_good_name: "Chocolate Bar".into(),
        quantity: 3,
        unit_price: Money::from_f64(10.0),
    }];

    // No payment: total = 30, paid = 0 → debt = 30
    let sale = sales_svc.create_sale(f.customer_id, items, Money::ZERO, f.wallet_id).await.unwrap();
    assert_eq!(sale.total_amount, Money::from_f64(30.0));

    // Wallet unchanged
    let wallet = wallet_queries::get_by_id(&pool, &f.wallet_id.to_string()).await.unwrap().unwrap();
    assert_eq!(wallet.current_balance, 100000);

    // Inventory: 50 - 3 = 47
    let fg = fg_queries::get_by_id(&pool, &f.fg_id.to_string()).await.unwrap().unwrap();
    assert_eq!(fg.current_quantity, 47.0);

    // Full debt (remaining_amount is INTEGER cents)
    let debt: (i64,) = sqlx::query_as("SELECT remaining_amount FROM debt_records WHERE sale_id = ?")
        .bind(sale.id.to_string())
        .fetch_one(&pool).await.unwrap();
    assert_eq!(debt.0, 3000);
}

// ═══════════════════════════════════════════════════════════════════════════
// Flow 3: Record payment → Verify wallet + debt FIFO allocation
// Validates: Requirements 9.5, 10.3
// ═══════════════════════════════════════════════════════════════════════════

/// Helper: seed multiple sales on credit to create multiple debt records for FIFO testing.
async fn seed_debts_for_fifo(pool: &SqlitePool) -> (Uuid, Uuid, Uuid) {
    let now = Utc::now().to_rfc3339();
    let customer_id = Uuid::new_v4();
    let wallet_id = Uuid::new_v4();
    let fg_id = Uuid::new_v4();

    customer_queries::insert(pool, &customer_id.to_string(), "Sara", "Homs", "+963933333333", &now)
        .await.unwrap();
    wallet_queries::insert_wallet(pool, &wallet_id.to_string(), "Rep Wallet", "Representative", 50000, &now)
        .await.unwrap();
    fg_queries::insert(pool, &fg_id.to_string(), "Gift Box", 200.0, 2000, &now)
        .await.unwrap();

    let sales_svc = SalesServiceImpl::new(pool.clone());

    // Sale 1: total 60, paid 0 → debt 60
    let items1 = vec![SaleLineItem {
        finished_good_id: fg_id,
        finished_good_name: "Gift Box".into(),
        quantity: 3,
        unit_price: Money::from_f64(20.0),
    }];
    sales_svc.create_sale(customer_id, items1, Money::ZERO, wallet_id).await.unwrap();

    // Sale 2: total 40, paid 0 → debt 40
    let items2 = vec![SaleLineItem {
        finished_good_id: fg_id,
        finished_good_name: "Gift Box".into(),
        quantity: 2,
        unit_price: Money::from_f64(20.0),
    }];
    sales_svc.create_sale(customer_id, items2, Money::ZERO, wallet_id).await.unwrap();

    // Sale 3: total 100, paid 0 → debt 100
    let items3 = vec![SaleLineItem {
        finished_good_id: fg_id,
        finished_good_name: "Gift Box".into(),
        quantity: 5,
        unit_price: Money::from_f64(20.0),
    }];
    sales_svc.create_sale(customer_id, items3, Money::ZERO, wallet_id).await.unwrap();

    (customer_id, wallet_id, fg_id)
}

#[tokio::test]
async fn payment_flow_fifo_pays_oldest_debt_first() {
    let pool = init_pool().await;
    let (customer_id, wallet_id, _fg_id) = seed_debts_for_fifo(&pool).await;
    let debt_svc = DebtServiceImpl::new(pool.clone());

    // Total debts: 60 + 40 + 100 = 200
    let active_before = debt_svc.get_active_debts(None).await.unwrap();
    assert_eq!(active_before.len(), 3);

    // Pay 70: should fully settle debt1 (60) and partially pay debt2 (10 of 40)
    let payment = debt_svc.record_payment(customer_id, Money::from_f64(70.0), wallet_id).await.unwrap();
    assert_eq!(payment.amount, Money::from_f64(70.0));
    assert_eq!(payment.allocations.len(), 2);

    // First allocation: 60 applied to oldest debt
    assert_eq!(payment.allocations[0].amount_applied, Money::from_f64(60.0));
    // Second allocation: 10 applied to next debt
    assert_eq!(payment.allocations[1].amount_applied, Money::from_f64(10.0));

    // Active debts: debt1 settled, debt2 remaining 30, debt3 remaining 100
    let active_after = debt_svc.get_active_debts(None).await.unwrap();
    assert_eq!(active_after.len(), 2);

    // Remaining amounts (sorted by sale_date ASC = oldest first)
    let remaining: Vec<Money> = active_after.iter().map(|d| d.remaining_amount).collect();
    assert_eq!(remaining, vec![Money::from_f64(30.0), Money::from_f64(100.0)]);
}

#[tokio::test]
async fn payment_flow_fully_settles_all_debts() {
    let pool = init_pool().await;
    let (customer_id, wallet_id, _fg_id) = seed_debts_for_fifo(&pool).await;
    let debt_svc = DebtServiceImpl::new(pool.clone());

    // Pay exactly 200 to settle all debts
    let payment = debt_svc.record_payment(customer_id, Money::from_f64(200.0), wallet_id).await.unwrap();
    assert_eq!(payment.allocations.len(), 3);
    assert_eq!(payment.allocations[0].amount_applied, Money::from_f64(60.0));
    assert_eq!(payment.allocations[1].amount_applied, Money::from_f64(40.0));
    assert_eq!(payment.allocations[2].amount_applied, Money::from_f64(100.0));

    // No active debts remain
    let active = debt_svc.get_active_debts(None).await.unwrap();
    assert!(active.is_empty());
}

#[tokio::test]
async fn payment_flow_overpayment_only_allocates_owed_amount() {
    let pool = init_pool().await;
    let (customer_id, wallet_id, _fg_id) = seed_debts_for_fifo(&pool).await;
    let debt_svc = DebtServiceImpl::new(pool.clone());

    // Pay 500 but only 200 is owed
    let payment = debt_svc.record_payment(customer_id, Money::from_f64(500.0), wallet_id).await.unwrap();

    // All debts settled
    let active = debt_svc.get_active_debts(None).await.unwrap();
    assert!(active.is_empty());

    // Total allocated = 200 (not 500)
    let total_allocated: Money = payment.allocations.iter().map(|a| a.amount_applied).sum();
    assert_eq!(total_allocated, Money::from_f64(200.0));
}

// ═══════════════════════════════════════════════════════════════════════════
// End-to-end: Production → Sale → Payment (full pipeline)
// Validates: Requirements 5.2, 8.2, 8.3, 9.5, 10.3
// ═══════════════════════════════════════════════════════════════════════════

#[tokio::test]
async fn full_pipeline_production_then_sale_then_payment() {
    let pool = init_pool().await;
    let now = Utc::now().to_rfc3339();

    // ── Setup ──────────────────────────────────────────────────────────
    let chef_id = Uuid::new_v4();
    let customer_id = Uuid::new_v4();
    let wallet_id = Uuid::new_v4();
    let fg_id = Uuid::new_v4();
    let rm_cream_id = Uuid::new_v4();
    let recipe_id = Uuid::new_v4();

    user_queries::insert_user(&pool, &chef_id.to_string(), "chef_pipe", "Chef Pipeline", "Chef", "hash")
        .await.unwrap();
    customer_queries::insert(&pool, &customer_id.to_string(), "Omar", "Latakia", "+963944444444", &now)
        .await.unwrap();
    wallet_queries::insert_wallet(&pool, &wallet_id.to_string(), "Bank Account", "Bank", 0, &now)
        .await.unwrap();
    rm_queries::insert(&pool, &rm_cream_id.to_string(), "Cream", "kg", 20.0, &now).await.unwrap();
    fg_queries::insert(&pool, &fg_id.to_string(), "Cream Cake", 0.0, 5000, &now).await.unwrap();

    // Recipe: 1 Cream Cake = 2kg cream
    recipe_queries::insert_recipe(&pool, &recipe_id.to_string(), "Cream Cake Recipe", &fg_id.to_string(), &now)
        .await.unwrap();
    recipe_queries::insert_ingredient(&pool, &Uuid::new_v4().to_string(), &recipe_id.to_string(), &rm_cream_id.to_string(), 2.0)
        .await.unwrap();

    // ── Step 1: Production ─────────────────────────────────────────────
    let production_svc = ProductionServiceImpl::new(pool.clone());
    let log = production_svc.execute_production(recipe_id, 5, chef_id).await.unwrap();
    assert_eq!(log.production_quantity, 5);

    // Cream: 20 - 10 = 10, Cream Cake: 0 + 5 = 5
    let cream = rm_queries::get_by_id(&pool, &rm_cream_id.to_string()).await.unwrap().unwrap();
    assert_eq!(cream.current_quantity, 10.0);
    let fg = fg_queries::get_by_id(&pool, &fg_id.to_string()).await.unwrap().unwrap();
    assert_eq!(fg.current_quantity, 5.0);

    // ── Step 2: Sale (partial payment) ─────────────────────────────────
    let sales_svc = SalesServiceImpl::new(pool.clone());
    let items = vec![SaleLineItem {
        finished_good_id: fg_id,
        finished_good_name: "Cream Cake".into(),
        quantity: 3,
        unit_price: Money::from_f64(50.0),
    }];
    // total = 150, paid = 100 → debt = 50
    let sale = sales_svc.create_sale(customer_id, items, Money::from_f64(100.0), wallet_id).await.unwrap();
    assert_eq!(sale.total_amount, Money::from_f64(150.0));

    // Cream Cake: 5 - 3 = 2
    let fg = fg_queries::get_by_id(&pool, &fg_id.to_string()).await.unwrap().unwrap();
    assert_eq!(fg.current_quantity, 2.0);

    // Wallet: 0 + 100 = 10000 cents ($100)
    let wallet = wallet_queries::get_by_id(&pool, &wallet_id.to_string()).await.unwrap().unwrap();
    assert_eq!(wallet.current_balance, 10000);

    // Debt: 5000 cents ($50)
    let debt: (i64,) = sqlx::query_as("SELECT remaining_amount FROM debt_records WHERE sale_id = ?")
        .bind(sale.id.to_string())
        .fetch_one(&pool).await.unwrap();
    assert_eq!(debt.0, 5000);

    // ── Step 3: Payment settles debt ───────────────────────────────────
    let debt_svc = DebtServiceImpl::new(pool.clone());
    let payment = debt_svc.record_payment(customer_id, Money::from_f64(50.0), wallet_id).await.unwrap();
    assert_eq!(payment.allocations.len(), 1);
    assert_eq!(payment.allocations[0].amount_applied, Money::from_f64(50.0));

    // No active debts
    let active = debt_svc.get_active_debts(None).await.unwrap();
    assert!(active.is_empty());
}
