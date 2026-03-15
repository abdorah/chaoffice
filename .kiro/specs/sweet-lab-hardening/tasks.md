# Implementation Plan: Sweet Lab Hardening

## Overview

Incremental hardening of the Sweet Lab ERP core engine addressing all 39 code review issues plus desktop UI wiring. Tasks are ordered by dependency: foundation types and migrations first, then security/correctness fixes, then financial/data integrity, then desktop wiring. Each phase ends with a checkpoint.

## Tasks

- [x] 1. Foundation: Money type, error types, and shared utilities
  - [x] 1.1 Create the Money newtype and arithmetic in `core/src/models/money.rs`
    - Define `Money(pub i64)` with `ZERO`, `from_f64()`, `to_display()`, `parse()`
    - Implement `Add`, `Sub`, `Mul<i64>`, `Serialize`, `Deserialize`, `Display`
    - Implement `Sum` trait for iterators
    - Add `mod money;` to `core/src/models/mod.rs` and re-export
    - _Requirements: 1.1, 1.2, 1.3_

  - [ ]* 1.2 Write property tests for Money type
    - **Property 1: Money format/parse round-trip**
    - **Property 2: Money parsing rejects malformed input**
    - Create `core/tests/money_properties.rs`
    - **Validates: Requirements 1.2, 1.3, 1.4**

  - [x] 1.3 Add NotFound variant to AppError and update error types
    - Add `NotFound { entity_type: String, entity_id: String }` to `AppError` in `core/src/error.rs`
    - Change `InsufficientStock` fields `available` and `requested` from `f64` to `i64`
    - Change `InsufficientFunds` fields `available` and `requested` from `f64` to `Money`
    - _Requirements: 9.1_

  - [x] 1.4 Create shared utilities module `core/src/utils.rs`
    - Implement `parse_uuid(entity_type, id_str) -> AppResult<Uuid>`
    - Implement `parse_wallet_type(s) -> AppResult<WalletType>`
    - Implement `parse_timestamp(s) -> AppResult<DateTime<Utc>>`
    - Add `pub mod utils;` to `core/src/lib.rs`
    - _Requirements: 17.1, 17.2_

  - [x] 1.5 Create SafeUser type in `core/src/models/domain.rs`
    - Define `SafeUser { id, username, full_name, role }` (no password_hash)
    - Implement `From<AppUser> for SafeUser`
    - _Requirements: 3.1_

  - [ ]* 1.6 Write property test for SafeUser serialization
    - **Property 7: SafeUser excludes password hash from serialization**
    - Create `core/tests/auth_guard_properties.rs` (will be extended in task 3)
    - **Validates: Requirements 3.2, 3.4**

  - [x] 1.7 Add Pagination struct to `core/src/models/domain.rs`
    - Define `Pagination { limit: i64, offset: i64 }` with `Default` impl (limit=100, offset=0)
    - _Requirements: 13.1_

  - [x] 1.8 Update all domain model monetary fields from f64 to Money
    - Update `FinishedGood.unit_price`, `Sale.total_amount`, `Sale.amount_paid`, `SaleLineItem.unit_price`, `Wallet.current_balance`, `WalletTransaction.amount`, `FundTransfer.amount`, `DebtRecord.original_amount`, `DebtRecord.remaining_amount`, `DebtPayment.amount`, `DebtAllocation.amount_applied`, `Expense.amount`, `FinancialSummary.total_revenue/total_expenses/net_profit`, `Invoice.total_amount/amount_paid/remaining_balance`, `Receipt.remaining_balance`, `Customer.total_debt`
    - Update `AppError::InsufficientFunds` fields to use Money
    - Fix all compilation errors in service modules, queries, and tests
    - _Requirements: 1.1_

- [x] 2. Database migrations
  - [x] 2.1 Create `core/migrations/012_create_sessions.sql`
    - Sessions table with id, user_id (FK), role, created_at, last_activity, expires_at
    - Index on user_id and expires_at
    - _Requirements: 10.1_

  - [x] 2.2 Create `core/migrations/013_create_debt_payments.sql`
    - debt_payments table with id, customer_id (FK), amount (INTEGER), wallet_id (FK), timestamp, sync_status, updated_at
    - debt_payment_allocations table with id, payment_id (FK), debt_record_id (FK), amount_applied (INTEGER)
    - Indexes on all FK columns
    - _Requirements: 14.1, 14.2_

  - [x] 2.3 Create `core/migrations/014_add_fk_indexes.sql`
    - Add indexes on: sales.customer_id, sales.payment_wallet_id, sale_line_items.sale_id, production_logs.recipe_id, production_logs.chef_id, production_logs.finished_good_id, recipe_ingredients.recipe_id, recipe_ingredients.raw_material_id, debt_records.customer_id, debt_records.sale_id, expenses.wallet_id, expenses.recorded_by, wallet_transactions.wallet_id
    - _Requirements: 16.2_

  - [x] 2.4 Create `core/migrations/015_add_sale_line_items_fk.sql`
    - Recreate sale_line_items table with REFERENCES finished_goods(id) on finished_good_id
    - Migrate data, drop old table, rename new table
    - Recreate indexes
    - _Requirements: 16.1_

  - [x] 2.5 Create `core/migrations/016_convert_monetary_to_integer_cents.sql`
    - Convert finished_goods.unit_price, sales.total_amount, sales.amount_paid, wallets.current_balance, wallet_transactions.amount, debt_records.original_amount, debt_records.remaining_amount, expenses.amount, sale_line_items.unit_price from REAL to INTEGER (multiply by 100, round)
    - Recreate tables with INTEGER columns, migrate data, drop old, rename
    - Recreate all indexes on affected tables
    - _Requirements: 1.5_

  - [x] 2.6 Enable PRAGMA foreign_keys = ON in database initialization
    - Update `core/src/persistence/db.rs` to execute `PRAGMA foreign_keys = ON` after pool creation
    - _Requirements: 16.3_

- [x] 3. Checkpoint — Foundation complete
  - Ensure all tests pass after Money type migration and schema changes, ask the user if questions arise.

- [x] 4. Security: Auth guard, session persistence, password validation
  - [x] 4.1 Implement session persistence in SQLite
    - Update `core/src/auth/service.rs`: replace `Arc<Mutex<HashMap<Uuid, Session>>>` with SQLite queries
    - Add `core/src/persistence/queries/sessions.rs` with insert_session, get_session, delete_session, update_last_activity, delete_expired
    - Update `login()` to INSERT into sessions table
    - Update `logout()` to DELETE from sessions table
    - Add `get_session(token) -> Option<Session>` reading from DB
    - Add `touch_session(token, now)` updating last_activity
    - _Requirements: 10.1, 10.2_

  - [ ]* 4.2 Write property tests for session persistence and expiry
    - **Property 15: Session persistence survives service restart**
    - **Property 16: Session expiry rules**
    - Create `core/tests/session_properties.rs`
    - **Validates: Requirements 10.1, 10.2, 10.3, 10.4**

  - [x] 4.3 Implement authorization guard on SweetLabCore API
    - Add `authorize(session_token, resource, action) -> AppResult<Session>` method to `SweetLabCore`
    - Add `session_token: Uuid` parameter to all mutating API methods in `core/src/api.rs`
    - Each mutating method calls `self.authorize(...)` before delegating to the service
    - Read-only methods (get_*, list_*) require valid session but skip RBAC check
    - _Requirements: 2.1, 2.2, 2.5_

  - [ ]* 4.4 Write property tests for authorization guard
    - **Property 5: Authorization guard rejects invalid sessions**
    - **Property 6: Authorization guard enforces RBAC**
    - Extend `core/tests/auth_guard_properties.rs`
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.4**

  - [x] 4.5 Implement password strength validation
    - Update `create_user()` in `core/src/auth/service.rs` to check `password.len() >= 8`
    - Return `AppError::Validation` with descriptive message on failure
    - _Requirements: 11.1, 11.2_

  - [ ]* 4.6 Write property test for password validation
    - **Property 17: Password minimum length enforcement**
    - Create `core/tests/password_properties.rs`
    - **Validates: Requirements 11.1, 11.2**

  - [x] 4.7 Update API responses to use SafeUser instead of AppUser
    - Change `create_user()` return type from `AppResult<AppUser>` to `AppResult<SafeUser>`
    - Update any other API methods that return user data to use SafeUser
    - Remove password_hash from AppUser protobuf message in `proto/sweetlab/models.proto`
    - _Requirements: 3.2, 3.3_

- [x] 5. Checkpoint — Security complete
  - Ensure all tests pass after auth guard, session persistence, and password validation, ask the user if questions arise.

- [x] 6. Data integrity: Atomic operations, input validation, wallet fixes
  - [x] 6.1 Implement atomic single-item inventory deduction
    - Update `core/src/persistence/queries/raw_materials.rs`: change `deduct_quantity()` to use single `UPDATE ... WHERE id = ? AND current_quantity >= ?`
    - If rows_affected == 0, SELECT to distinguish NotFound vs InsufficientStock
    - Update `core/src/persistence/queries/finished_goods.rs` with same pattern for `deduct_quantity()`
    - Update `core/src/sales/transactions.rs` to use atomic deduction in sale creation
    - _Requirements: 4.1, 4.2, 4.3_

  - [ ]* 6.2 Write property tests for atomic deductions
    - **Property 8: Atomic raw material deduction correctness**
    - **Property 9: Atomic finished good deduction correctness**
    - Create `core/tests/atomic_deduction_properties.rs`
    - **Validates: Requirements 4.1, 4.2, 4.3**

  - [x] 6.3 Implement sale input validation
    - Update `core/src/sales/transactions.rs` `create_sale()`:
      - Reject empty line_items
      - Reject line items with quantity <= 0
      - Verify each line item's unit_price against DB
      - Reject negative amount_paid
      - Reject amount_paid > total_amount
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [ ]* 6.4 Write property tests for sale validation
    - **Property 3: Sale amount_paid bounds validation**
    - **Property 4: Sale unit price verification**
    - Create `core/tests/sale_validation_properties.rs`
    - **Validates: Requirements 5.1, 5.2, 5.3**

  - [x] 6.5 Implement wallet validation and atomic delta operations
    - Update `core/src/wallet/service.rs`:
      - Add amount > 0 check to `credit_wallet()`, `debit_wallet()`, `transfer_funds()`
      - Add source_id != destination_id check to `transfer_funds()`
    - Update `core/src/persistence/queries/wallets.rs`:
      - Change `update_balance_in_tx()` to use `current_balance = current_balance + ?` (delta) instead of absolute SET
      - Add `atomic_debit_in_tx()` with `WHERE current_balance >= ?` check
    - _Requirements: 7.1, 7.2, 7.3_

  - [ ]* 6.6 Write property tests for wallet validation
    - **Property 11: Wallet rejects non-positive amounts**
    - **Property 12: Wallet rejects self-transfer**
    - Create `core/tests/wallet_validation_properties.rs`
    - **Validates: Requirements 7.1, 7.2**

  - [x] 6.7 Implement debt payment with wallet credit and persistence
    - Update `core/src/debt/tracker.rs` `record_payment()`:
      - Validate wallet exists (return NotFound if not)
      - Credit wallet by allocated amount within the same transaction
      - Cap allocation at total outstanding debt, return unallocated remainder
    - Add `core/src/persistence/queries/debt_payments.rs` with insert_payment, insert_allocation queries
    - Insert into debt_payments and debt_payment_allocations tables
    - _Requirements: 6.1, 6.2, 6.3, 14.3_

  - [ ]* 6.8 Write property tests for debt payment
    - **Property 10: Debt payment credits wallet capped at total debt**
    - **Property 20: Debt payment persistence**
    - Create `core/tests/debt_payment_properties.rs`
    - **Validates: Requirements 6.1, 6.3, 14.3**

  - [x] 6.9 Implement expense recorded_by validation
    - Update `core/src/expenses/service.rs` `record_expense()`:
      - Query users table to verify recorded_by exists
      - Return NotFound if user doesn't exist
    - _Requirements: 15.1, 15.2_

  - [ ]* 6.10 Write property test for expense validation
    - **Property 21: Expense validates recorded_by user exists**
    - Create `core/tests/expense_validation_properties.rs`
    - **Validates: Requirements 15.1, 15.2**

- [x] 7. Checkpoint — Data integrity complete
  - Ensure all tests pass after atomic operations, input validation, and wallet fixes, ask the user if questions arise.

- [x] 8. Financial and query fixes
  - [x] 8.1 Fix financial summary to use amount_paid for revenue
    - Update `core/src/reports/financial.rs` `get_financial_summary()`:
      - Change `total_revenue` to sum `amount_paid` instead of `total_amount`
      - Update net_profit calculation comment to note COGS is not tracked
    - _Requirements: 8.1, 8.2_

  - [ ]* 8.2 Write property test for financial summary
    - **Property 13: Financial summary uses amount_paid for revenue**
    - Create `core/tests/financial_properties.rs`
    - **Validates: Requirements 8.1, 8.2**

  - [x] 8.3 Fix customer total_debt and overdue_days computation
    - Update `core/src/persistence/queries/customers.rs`:
      - Change list/get queries to LEFT JOIN debt_records and compute total_debt = SUM(remaining_amount), overdue_days = MAX(julianday('now') - julianday(sale_date))
      - Update `CustomerRow` to include computed fields
    - Update `core/src/sales/customers.rs` to use the computed values instead of hardcoded 0
    - _Requirements: 12.4_

  - [ ]* 8.4 Write property test for customer debt computation
    - **Property 18: Customer debt computed from debt records**
    - Create `core/tests/customer_debt_properties.rs`
    - **Validates: Requirements 12.4**

  - [x] 8.5 Fix insufficient materials threshold in recipe availability
    - Update `core/src/recipes/production.rs` `get_recipe_availability()`:
      - Change insufficient check from `== 0.0` to `< required_quantity * production_quantity`
      - Include all materials below threshold in insufficient_materials list
    - _Requirements: 18.1, 18.2_

  - [ ]* 8.6 Write property test for recipe availability threshold
    - **Property 22: Insufficient materials threshold correctness**
    - Create `core/tests/recipe_availability_properties.rs`
    - **Validates: Requirements 18.1, 18.2**

  - [x] 8.7 Add pagination support to all list queries
    - Update `core/src/persistence/queries/` modules (customers, sales, debts, expenses, raw_materials, finished_goods, recipes, wallets) to accept optional `Pagination` and append `LIMIT ? OFFSET ?`
    - Update service methods to accept `Option<Pagination>` and default to `Pagination::default()` when None
    - Update API methods in `core/src/api.rs` to pass pagination through
    - _Requirements: 13.1, 13.2, 13.3_

  - [ ]* 8.8 Write property test for pagination
    - **Property 19: Pagination bounds**
    - Create `core/tests/pagination_properties.rs`
    - **Validates: Requirements 13.1, 13.2, 13.3**

  - [x] 8.9 Consolidate duplicate SQL in auth service
    - Update `core/src/auth/service.rs` to use `core/src/persistence/queries/users.rs` for user lookups instead of inline SQL
    - Remove duplicated `query_as` calls from auth service
    - _Requirements: 12.3_

  - [x] 8.10 Eliminate N+1 query in sales history
    - Update `core/src/sales/transactions.rs` `get_sales_history()`:
      - Replace per-sale line item query with a single batched query (fetch all line items for all sale IDs, then group in Rust)
    - _Requirements: 12.1_

  - [ ]* 8.11 Write property test for NotFound errors
    - **Property 14: NotFound on missing entities**
    - Create `core/tests/notfound_properties.rs`
    - **Validates: Requirements 9.2, 9.3**

- [x] 9. Checkpoint — Financial and query fixes complete
  - Ensure all tests pass after financial fixes, pagination, and query consolidation, ask the user if questions arise.

- [x] 10. Configuration, sync, and code cleanup
  - [x] 10.1 Make business name configurable
    - Add `business_name: String` field to `SweetLabCore`
    - Update `SweetLabCore::new()` to accept `business_name` parameter
    - Update `core/src/reports/financial.rs` `generate_invoice()` to use `self.business_name` instead of hardcoded "Sweet Lab"
    - Update receipt generation similarly
    - _Requirements: 19.1, 19.2_

  - [ ]* 10.2 Write property test for configurable business name
    - **Property 23: Invoice uses configured business name**
    - Create `core/tests/config_properties.rs`
    - **Validates: Requirements 19.2**

  - [x] 10.3 Wire sync module into SweetLabCore initialization
    - Update `SweetLabCore::new()` to accept optional `sync_base_url: Option<String>`
    - Initialize `SyncManagerImpl` with the URL or empty string for offline-only mode
    - Ensure sync_all, is_online, get_sync_status, get_conflict_log are exposed in API
    - _Requirements: 20.1, 20.2, 20.3_

  - [x] 10.4 Add conflict log pagination and cleanup
    - Update `core/src/sync/conflict.rs`:
      - Add pagination support to `get_all()` (accept `Pagination`)
      - Add `cleanup_older_than(pool, retention_days)` function that deletes entries older than the retention period
    - _Requirements: 23.1, 23.2_

  - [ ]* 10.5 Write property test for conflict log cleanup
    - **Property 24: Conflict log cleanup respects retention period**
    - Create `core/tests/conflict_cleanup_properties.rs`
    - **Validates: Requirements 23.2**

  - [x] 10.6 Consolidate duplicate code
    - Replace all `Uuid::parse_str(...).map_err(...)` calls across 5+ files with `utils::parse_uuid()`
    - Replace duplicate `parse_wallet_type()` in `wallet/service.rs` and `reports/financial.rs` with `utils::parse_wallet_type()`
    - Replace duplicate `parse_timestamp()` calls with `utils::parse_timestamp()`
    - Consolidate `generate_invoice` and `generate_receipt` shared formatting logic into a helper
    - Extract common `row_to_domain` patterns where feasible (shared conversion trait or helper functions)
    - _Requirements: 17.1, 17.2, 17.3, 17.4_

  - [x] 10.7 Make PDF font path configurable
    - Update PDF generation in `core/src/reports/pdf.rs` to accept a `font_dir: &str` parameter
    - Return `AppError::Validation` if font directory doesn't exist instead of panicking
    - Update `SweetLabCore` to accept optional `font_dir` config
    - _Requirements: 22.1, 22.2_

  - [x] 10.8 Populate UniFFI UDL file
    - Update `core/src/sweet_lab_core.udl` with all public API types, enums, and function signatures
    - Include: Money, SafeUser, UserRole, WalletType, ExpenseCategory, Session, Pagination, and all SweetLabCore methods
    - Verify build succeeds with `cargo build`
    - _Requirements: 21.1, 21.2_

- [x] 11. Checkpoint — Core hardening complete
  - Ensure all tests pass, run `cargo test` for the full core crate, ask the user if questions arise.

- [ ] 12. Desktop UI wiring: Core initialization and login
  - [x] 12.1 Initialize SweetLabCore in desktop main.rs
    - Update `desktop/src/main.rs`:
      - Create a Tokio runtime
      - Call `SweetLabCore::new("sweet_lab.db", "Sweet Lab", None)` via `rt.block_on()`
      - Wrap core in `Arc<SweetLabCore>`
      - Store session token in a shared `Arc<Mutex<Option<Uuid>>>` for use across callbacks
    - Update `desktop/Cargo.toml` to add `tokio` dependency with `rt-multi-thread` feature
    - _Requirements: 24.1_

  - [x] 12.2 Wire login callback to Core_Engine
    - Replace placeholder username-based routing with actual `core.login()` call
    - Use `slint::spawn_local()` or `rt.block_on()` for async bridging
    - On success: store session token, set current_role and current_view based on session.role
    - On error: set login_error to generic Arabic message
    - Manage login_loading state
    - _Requirements: 24.2_

  - [x] 12.3 Wire logout callback
    - Call `core.logout(session_token)` on logout
    - Clear session token, reset view to Login
    - _Requirements: 24.2_

- [x] 13. Desktop UI wiring: Admin screens
  - [x] 13.1 Wire admin dashboard with live data
    - Update `desktop/ui/admin/dashboard.slint` to expose data properties (wallet_balances, active_debts_count, low_stock_count)
    - In `desktop/src/main.rs`, add callback/refresh logic that calls `core.get_wallets()`, `core.get_active_debts()`, `core.get_inventory_report()` and populates Slint properties
    - _Requirements: 24.3_

  - [x] 13.2 Wire user management screen
    - Update `desktop/ui/admin/user_management.slint` to expose user list model and create-user callback
    - Wire callbacks to `core.create_user()` and `core.update_user_role()` (passing session_token)
    - Refresh user list after mutations
    - _Requirements: 24.4_

  - [x] 13.3 Wire inventory screen with live data
    - Update `desktop/ui/admin/inventory.slint` to expose raw_materials and finished_goods list models
    - Wire refresh callback to `core.get_raw_materials()` and `core.get_finished_goods()`
    - _Requirements: 24.3_

  - [x] 13.4 Wire recipes screen
    - Update `desktop/ui/admin/recipes.slint` to expose recipe list model and CRUD callbacks
    - Wire to `core.get_recipes()`, `core.create_recipe()`, `core.delete_recipe()`
    - _Requirements: 24.6_

  - [x] 13.5 Wire wallets screen
    - Update `desktop/ui/admin/wallets.slint` to expose wallet list, transaction history, and transfer callback
    - Wire to `core.get_wallets()`, `core.get_transaction_history()`, `core.transfer_funds()`
    - _Requirements: 24.5_

  - [x] 13.6 Wire reports screen
    - Update `desktop/ui/admin/reports.slint` to expose financial summary, inventory report, and debt aging data
    - Wire to `core.get_financial_summary()`, `core.get_inventory_report()`, `core.get_debt_aging_report()`
    - _Requirements: 24.7_

- [x] 14. Desktop UI wiring: Chef and Representative screens
  - [x] 14.1 Wire chef production screen
    - Update `desktop/ui/chef/production.slint` to expose recipe availability model and execute callback
    - Wire to `core.get_recipe_availability()` and `core.execute_production()`
    - _Requirements: 24.8_

  - [x] 14.2 Wire representative sales screen
    - Update `desktop/ui/representative/sales.slint` to expose customer list, finished goods, and create-sale callback
    - Wire to `core.get_customers()`, `core.get_finished_goods()`, `core.create_sale()`
    - _Requirements: 24.9_

  - [x] 14.3 Wire representative customers screen
    - Update `desktop/ui/representative/customers.slint` to expose customer list model, search, and CRUD callbacks
    - Wire to `core.search_customers()`, `core.create_customer()`, `core.update_reliability_rating()`
    - _Requirements: 24.10_

  - [x] 14.4 Wire representative expenses screen
    - Update `desktop/ui/representative/expenses.slint` to expose expense form and submit callback
    - Wire to `core.record_expense()`
    - _Requirements: 24.11_

  - [x] 14.5 Wire representative payments screen
    - Update `desktop/ui/representative/payments.slint` to expose payment form, customer debt summary, and submit callback
    - Wire to `core.get_active_debts()`, `core.record_debt_payment()`
    - _Requirements: 24.12_

  - [x] 14.6 Implement auto-refresh and loading states across all screens
    - After every successful mutation callback, re-fetch and update the affected Slint model properties
    - Set is-loading = true before async calls, false after completion
    - Display error-message on failure
    - _Requirements: 24.13, 24.14_

- [x] 15. Final checkpoint — Full hardening complete
  - Run `cargo test` for core crate, build desktop binary with `cargo build -p desktop`, verify all screens load with live data, ask the user if questions arise.

- [ ] 16. Android mobile: UniFFI binding setup and core initialization
  - [x] 16.1 Generate UniFFI Kotlin bindings
    - Add `uniffi-bindgen` binary target to `core/Cargo.toml` and create `core/uniffi-bindgen.rs`
    - Cross-compile the Rust library for Android targets (arm64-v8a, armeabi-v7a, x86_64) using Android NDK via `cargo ndk`
    - Run `cargo run --bin uniffi-bindgen -- generate --library ... --language kotlin --out-dir ../android/app/src/main/kotlin/org/sweetlab/bindings/`
    - Copy compiled `.so` files to `android/app/src/main/jniLibs/{abi}/`
    - Verify generated Kotlin bindings compile with the Android project
    - _Requirements: 21.1, 21.2_

  - [x] 16.2 Wire SweetLabCore initialization in SweetLabApp.kt
    - Uncomment and update `SweetLabApp.kt`:
      - Import `org.sweetlab.bindings.SweetLabCore`
      - Call `SweetLabCore.new(dbPath, "Sweet Lab", null, null)` in `initializeCore()`
      - Store result in `companion object` `core` property
      - Set `isInitialized = true` on success, `initError` on failure
    - Add shared session state: `var currentSession: Session? = null` and `var currentUserId: String? = null` in companion object
    - _Requirements: 24.1_

  - [x] 16.3 Wire login screen to core engine
    - Update `LoginScreen.kt`:
      - Import `SweetLabApp` and UniFFI bindings
      - Replace placeholder login with `SweetLabApp.core!!.login(username, password)`
      - Store session in `SweetLabApp.currentSession`, user ID in `SweetLabApp.currentUserId`
      - Pass `session.role.name.lowercase()` to `onLoginSuccess`
      - On error: show generic Arabic error message (never reveal which field is wrong)
    - _Requirements: 24.2_

  - [x] 16.4 Add logout support to NavGraph
    - Update `NavGraph.kt` `SweetLabNavHost`:
      - Add logout callback that calls `SweetLabApp.core!!.logout(SweetLabApp.currentSession!!.sessionId)`
      - Clear `SweetLabApp.currentSession` and `currentUserId`
      - Navigate back to `Routes.AUTH` clearing the back stack
    - Wire offline indicator to `SweetLabApp.core!!.isOnline()` periodic check
    - _Requirements: 24.2_

- [x] 17. Android mobile: Admin screen wiring
  - [x] 17.1 Wire admin dashboard with live data
    - Update `AdminDashboard.kt`:
      - Remove placeholder state, replace with calls to `SweetLabApp.core!!.getWallets(token, null)`, `getActiveDebts(token, null)`, `getInventoryReport(token, 10.0)`
      - Compute `totalWalletBalance` from wallet list (sum `currentBalance`, convert from Money/i64 cents to display)
      - Compute `activeDebtsCount` and `lowStockCount` from results
      - Add loading and error state handling
    - _Requirements: 24.3_

  - [x] 17.2 Wire user management screen
    - Update `UserManagementScreen.kt`:
      - Remove private `UserItem` data class, use UniFFI-generated `SafeUser` type
      - Wire `LaunchedEffect` to fetch users (note: core API doesn't have `listUsers` — use `getCustomers` pattern or add if needed)
      - Wire `createUser()` callback to `SweetLabApp.core!!.createUser(token, username, password, fullName, role)`
      - Wire `updateUserRole()` callback to `SweetLabApp.core!!.updateUserRole(token, userId, newRole)`
      - Convert `UserRole` enum between UniFFI type and display strings
      - Refresh user list after mutations
    - _Requirements: 24.4_

  - [x] 17.3 Wire inventory screen with live data
    - Update `InventoryScreen.kt`:
      - Remove private placeholder data classes
      - Use UniFFI-generated `RawMaterial` and `FinishedGood` types
      - Wire `LaunchedEffect` to `SweetLabApp.core!!.getRawMaterials(token, null)` and `getFinishedGoods(token, null)`
      - Convert `Money` (i64 cents) to display format for `unitPrice`
      - Add loading state
    - _Requirements: 24.3_

  - [x] 17.4 Wire recipe management screen
    - Update `RecipeManagementScreen.kt`:
      - Remove private placeholder data classes, use UniFFI `Recipe`, `RecipeIngredient`, `RawMaterial`
      - Wire `LaunchedEffect` to `SweetLabApp.core!!.getRecipes(token, null)` and `getRawMaterials(token, null)`
      - Wire create callback to `core.createRecipe(token, name, finishedGoodId, ingredients)`
      - Wire update callback to `core.updateRecipe(token, recipe)`
      - Wire delete callback to `core.deleteRecipe(token, recipeId)`
      - Handle `DeletionBlocked` error for recipes with production logs
      - Refresh recipe list after mutations
    - _Requirements: 24.6_

  - [x] 17.5 Wire wallet summary screen
    - Update `WalletSummaryScreen.kt`:
      - Remove private placeholder data classes, use UniFFI `Wallet`, `WalletTransaction`, `WalletType`
      - Wire `LaunchedEffect` to `SweetLabApp.core!!.getWallets(token, null)`
      - Wire transaction history to `core.getTransactionHistory(token, walletId, null)` on wallet selection
      - Wire fund transfer to `core.transferFunds(token, sourceId, destId, amount)` (convert amount to Money cents)
      - Refresh wallets after transfer
    - _Requirements: 24.5_

  - [x] 17.6 Wire reporting screen
    - Update `ReportingScreen.kt`:
      - Remove private placeholder data classes, use UniFFI `FinancialSummary`, `InventoryReport`, `DebtRecord`, `ExpenseCategoryGroup`
      - Wire financial summary tab to `core.getFinancialSummary(token, startDate, endDate)` (convert millis to ISO DateTime string)
      - Wire inventory report tab to `core.getInventoryReport(token, 10.0)`
      - Wire debt aging tab to `core.getDebtAgingReport(token, null)`
      - Wire expense report tab to `core.getExpensesByCategory(token, startDate, endDate, null)`
      - Wire PDF export to `core.exportToPdf(invoice)` — save bytes to file and share via Android intent
      - Convert `Money` (i64 cents) to display format throughout
    - _Requirements: 24.7_

- [x] 18. Android mobile: Chef screen wiring
  - [x] 18.1 Wire production screen
    - Update `ProductionScreen.kt`:
      - Remove private placeholder data classes, use UniFFI `RecipeAvailability`, `ProductionLog`, `RecipeIngredient`
      - Wire `LaunchedEffect` to `SweetLabApp.core!!.getRecipeAvailability(token, null)` and `getProductionHistory(token, null)`
      - Wire execute production to `core.executeProduction(token, recipeId, quantity, SweetLabApp.currentUserId!!)`
      - Handle `InsufficientStock` error to display insufficient materials
      - Refresh availability and history after production
      - Convert `materialsConsumed` map (Uuid keys) to display names
    - _Requirements: 24.8_

- [x] 19. Android mobile: Representative screen wiring
  - [x] 19.1 Wire sales screen
    - Update `SalesScreen.kt`:
      - Remove private placeholder data classes, use UniFFI `Customer`, `FinishedGood`, `Wallet`, `Sale`, `SaleLineItem`, `Receipt`
      - Wire `LaunchedEffect` to `core.getCustomers(token, null)`, `getFinishedGoods(token, null)`, `getWallets(token, null)`, `getSalesHistory(token, null)`
      - Wire create sale to `core.createSale(token, customerId, lineItems, amountPaid, walletId)` — construct `SaleLineItem` list from form, convert amounts to Money cents
      - Wire receipt generation to `core.generateReceipt(token, saleId)` — populate `ReceiptDialog` from UniFFI `Receipt` type
      - Refresh sales history and inventory after sale creation
    - _Requirements: 24.9_

  - [x] 19.2 Wire customer management screen
    - Update `CustomerManagementScreen.kt`:
      - Remove private `CustomerData` class, use UniFFI `Customer` type
      - Wire `LaunchedEffect` to `SweetLabApp.core!!.getCustomers(token, null)`
      - Wire search to `core.searchCustomers(token, query, null)`
      - Wire create customer to `core.createCustomer(token, name, city, mobile)`
      - Wire rating update to `core.updateReliabilityRating(token, customerId, rating)`
      - Handle `Duplicate` error for unique mobile constraint
      - Convert `Money` total_debt to display format
      - Refresh customer list after mutations
    - _Requirements: 24.10_

  - [x] 19.3 Wire expense recording screen
    - Update `ExpenseRecordingScreen.kt`:
      - Remove private placeholder data classes and enum, use UniFFI `Expense`, `Wallet`, `ExpenseCategory`
      - Wire `LaunchedEffect` to `core.getWallets(token, null)` and `core.getExpenses(token, startDate, endDate, null)`
      - Wire record expense to `core.recordExpense(token, description, amount, category, walletId, SweetLabApp.currentUserId!!)`
      - Convert amount to Money cents, map `ExpenseCategory` enum
      - Handle `InsufficientFunds` error
      - Refresh expense history and wallet balances after recording
    - _Requirements: 24.11_

  - [x] 19.4 Wire customer payment screen
    - Update `CustomerPaymentScreen.kt`:
      - Remove private placeholder data classes, use UniFFI `Customer`, `Wallet`, `DebtRecord`, `DebtPayment`
      - Wire `LaunchedEffect` to `core.getCustomers(token, null)` and `core.getWallets(token, null)`
      - Wire customer selection to `core.getActiveDebts(token, null)` filtered by customer
      - Wire payment to `core.recordDebtPayment(token, customerId, amount, walletId)` — convert amount to Money cents
      - Update local customer debt state from `DebtPayment.unallocated` response
      - Refresh debts and wallet balances after payment
    - _Requirements: 24.12_

- [x] 20. Android mobile: Auto-refresh, loading states, and navigation polish
  - [x] 20.1 Add representative bottom navigation
    - Update `NavGraph.kt` representative sub-graph:
      - Add a `BottomNavigationBar` or equivalent for representative screens (Sales, Customers, Expenses, Payments)
      - Ensure all four screens are accessible from the bottom nav
      - Add logout option in top bar or navigation drawer
    - _Requirements: 24.13_

  - [x] 20.2 Implement loading and error states across all screens
    - Add `isLoading` state to all screens that make async core calls
    - Show `CircularProgressIndicator` during loading
    - Show error `Snackbar` or inline error text on failure
    - After every successful mutation, re-fetch and update the affected data lists
    - _Requirements: 24.14_

  - [x] 20.3 Create shared Money display utility
    - Create `android/app/src/main/kotlin/org/sweetlab/ui/util/MoneyFormatter.kt`:
      - `fun Money.toDisplay(): String` — convert i64 cents to "X.XX" format
      - `fun Double.toMoneyCents(): Long` — convert user-entered double to i64 cents
    - Update all screens to use these utilities instead of raw double formatting
    - _Requirements: 24.15_

- [x] 21. Final Android checkpoint
  - Build Android APK with `./gradlew assembleDebug` in `android/` directory
  - Verify all screens load with live data from the core engine
  - Verify login/logout flow works end-to-end
  - Verify offline indicator reflects actual connectivity state
  - Ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties (24 new properties) using `proptest`
- Unit tests validate specific examples and edge cases
- The Money migration (task 2.5) is the most impactful change — it touches every monetary field in the codebase
- Desktop wiring tasks (12-14) depend on all core hardening being complete first
- All core fixes are backward-compatible with the Android app (API signature changes require UniFFI UDL update in task 10.8)
- I am using git bash as a terminal running on windows 11 os and I have podman installed.
