# Implementation Plan: Sweet Lab ERP

## Overview

Incremental implementation of the Sweet Lab ERP as a polyglot system: Protobuf schema layer → Rust core engine (business logic, SQLx/SQLite, Casbin-RS RBAC, genpdf/xlsx reports) → UniFFI bindings → Android (Kotlin/Compose) and Desktop (Slint) UI shells. Tasks build on each other — starting with schema and core scaffolding, then business logic modules with proptest property tests, then platform UIs, and finally sync/offline support.

## Tasks

- [x] 1. Schema layer and Rust core scaffolding
  - [x] 1.1 Set up workspace structure with proto/, core/, android/, desktop/ directories
    - Create root `Cargo.toml` workspace with `core` and `desktop` members
    - Create `proto/buf.yaml` and `proto/sweetlab/models.proto`, `auth.proto`, `services.proto` as defined in the design
    - Create `core/Cargo.toml` with dependencies: prost, prost-build, sqlx (sqlite, runtime-tokio), casbin, serde, serde_json, thiserror, chrono, uuid, argon2, reqwest, genpdf, rust_xlsxwriter, tracing, tracing-subscriber, tokio, uniffi, proptest (dev)
    - Create `core/build.rs` with prost codegen from proto files + UniFFI scaffolding
    - Create `core/uniffi.toml` configuration
    - _Requirements: Technical Context_

  - [x] 1.2 Define domain models and error types in Rust core
    - Create `core/src/models/domain.rs` with all domain structs: `AppUser`, `RawMaterial`, `FinishedGood`, `Recipe`, `RecipeIngredient`, `ProductionLog`, `RecipeAvailability`, `Customer`, `Sale`, `SaleLineItem`, `Receipt`, `Wallet`, `WalletTransaction`, `FundTransfer`, `DebtRecord`, `DebtPayment`, `DebtAllocation`, `Expense`, `FinancialSummary`, `InventoryReport`, `Invoice`, `SyncQueueItem`, `ConflictLog`
    - Create enums: `UserRole`, `WalletType`, `ExpenseCategory`, `SyncStatus`
    - Create `core/src/error.rs` with `AppError` (thiserror) and `AppResult<T>` type alias
    - Derive `Serialize`, `Deserialize`, `Clone`, `Debug` on all models
    - _Requirements: All requirements (domain model foundation)_

  - [x] 1.3 Create SQLx migrations
    - Create all 11 migration files as defined in the design: `001_create_users.sql` through `011_create_sync_tables.sql`
    - Include CHECK constraints, UNIQUE constraints, foreign keys, and indexes
    - _Requirements: 14.1, 14.2_

  - [x] 1.4 Set up SQLx database module and connection pool
    - Create `core/src/persistence/db.rs` with SQLx pool initialization, migration runner
    - Create `core/src/persistence/mod.rs` exposing the database module
    - Configure compile-time checked queries with `sqlx::query!` macros
    - _Requirements: Technical Context (SQLx persistence)_

  - [x] 1.5 Set up Casbin-RS RBAC
    - Create `core/policies/model.conf` with RBAC model definition as specified in design
    - Create `core/policies/policy.csv` with all role permissions (Admin, Chef, Representative)
    - Create `core/src/auth/rbac.rs` with Casbin enforcer initialization and `check_permission()` function
    - _Requirements: 2.1_

- [ ] 2. Serialization and schema validation
  - [x] 2.1 Implement serde serialization and proto-to-domain mappers
    - Create `core/src/models/generated.rs` — prost-generated structs (auto from build.rs)
    - Create `core/src/models/mappers.rs` — conversion functions between prost structs and domain models
    - Implement JSON schema validation for deserialized data
    - _Requirements: 14.1, 14.2, 14.4_

  - [ ]* 2.2 Write property test: JSON serialization round-trip
    - **Property 38: JSON serialization round-trip**
    - Create `core/tests/generators.rs` with proptest strategies for all domain objects
    - Test serde_json serialize→deserialize produces equivalent object for each entity type
    - **Validates: Requirements 14.1, 14.2, 14.3**

  - [ ]* 2.3 Write property test: Schema validation rejects malformed data
    - **Property 39: Schema validation rejects malformed data**
    - Generate malformed JSON (missing fields, wrong types, out-of-range values) and verify `AppError::Serialization`
    - **Validates: Requirements 14.4**

- [ ] 3. Authentication and RBAC
  - [x] 3.1 Implement AuthService in Rust core
    - Create `core/src/auth/service.rs` with `login()` using argon2 password verification against SQLx
    - Implement session management with 8-hour inactivity timeout using chrono
    - Implement `create_user()` with field validation (username, password, full_name, role required)
    - Implement `update_user_role()` — update role in SQLx users table
    - Implement `check_permission()` delegating to Casbin-RS enforcer
    - _Requirements: 1.1, 1.2, 1.4, 2.1, 2.2, 2.3_

  - [x] 3.2 Implement user persistence queries
    - Create `core/src/persistence/queries/users.rs` with SQLx queries: insert_user, get_user_by_username, get_user_by_id, update_role, list_users
    - _Requirements: 1.1, 2.2, 2.3_

  - [ ]* 3.3 Write property tests for authentication
    - **Property 2: Role permission enforcement** — for any role/resource combination, Casbin result matches expected permission set
    - **Property 3: Invalid credentials rejection** — error message does not reveal which field is wrong
    - **Property 4: User creation field validation** — missing fields rejected, complete fields accepted
    - **Property 5: Role update applies on next login**
    - **Validates: Requirements 1.2, 1.5, 2.1, 2.2, 2.3**

  - [ ]* 3.4 Write unit tests for auth edge cases
    - Test session expiry at exactly 8 hours
    - Test login with empty username/password
    - Test role update for non-existent user
    - Test argon2 hash verification
    - _Requirements: 1.4, 1.5, 2.3_

- [x] 4. Checkpoint — Core foundation
  - Run `cargo test` — ensure all tests pass, ask the user if questions arise.

- [ ] 5. Inventory management
  - [x] 5.1 Implement InventoryService in Rust core
    - Create `core/src/inventory/raw_materials.rs` — `add_purchase()` increases quantity, `deduct()` with insufficient stock check
    - Create `core/src/inventory/finished_goods.rs` — `add()` and `deduct()` with stock validation
    - Implement `deduct_raw_materials()` — atomic multi-material deduction in SQLx transaction
    - Implement `get_raw_materials()` and `get_finished_goods()` queries
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 6.1, 6.2, 6.3, 6.4_

  - [x] 5.2 Implement inventory persistence queries
    - Create `core/src/persistence/queries/raw_materials.rs` and `finished_goods.rs`
    - SQLx queries: insert, update_quantity, get_by_id, list_all, deduct_atomic
    - _Requirements: 3.1, 6.1_

  - [ ]* 5.3 Write property tests for inventory
    - **Property 6: Raw material purchase increases quantity**
    - **Property 7: Insufficient raw material stock rejection**
    - **Property 15: Finished good insufficient stock rejection**
    - **Validates: Requirements 3.2, 3.4, 6.4**

  - [ ]* 5.4 Write unit tests for inventory edge cases
    - Test deduction of exactly available quantity (boundary)
    - Test deduction of zero quantity
    - Test concurrent deductions within a transaction
    - _Requirements: 3.4, 6.4_

- [ ] 6. Recipe management
  - [x] 6.1 Implement RecipeService in Rust core
    - Create `core/src/recipes/management.rs`
    - Implement `create_recipe()` with 1-10 ingredient validation and material existence check
    - Implement `update_recipe()` with same validations
    - Implement `delete_recipe()` with production log reference check
    - Implement `validate_ingredients()` for material existence
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [x] 6.2 Implement recipe persistence queries
    - Create `core/src/persistence/queries/recipes.rs`
    - SQLx queries: insert_recipe, insert_ingredients, get_with_ingredients (JOIN), delete_recipe, count_production_logs_for_recipe
    - _Requirements: 4.1_

  - [ ]* 6.3 Write property tests for recipes
    - **Property 12: Recipe ingredient count bounds** — accept 1-10, reject 0 or 11+
    - **Property 13: Recipe material existence validation** — reject recipes with non-existent materials
    - **Property 14: Recipe deletion protection** — block deletion if production logs exist
    - **Validates: Requirements 4.1, 4.2, 4.4, 4.5**

- [ ] 7. Production execution
  - [x] 7.1 Implement ProductionService in Rust core
    - Create `core/src/recipes/production.rs`
    - Implement `execute_production()` — calculate requirements, validate stock, atomic deduction + increment + log, all in SQLx transaction
    - Implement `get_recipe_availability()` — compute max_producible per recipe
    - Implement `get_production_history()` query
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [ ]* 7.2 Write property tests for production
    - **Property 8: Production run inventory conservation** — raw materials decrease by recipe×qty, finished good increases by qty
    - **Property 9: Production run rejection on insufficient stock** — entire run rejected, no quantities change
    - **Property 10: Production log completeness** — log contains all required fields
    - **Property 11: Recipe availability calculation** — max_producible = min(stock/required) floored
    - **Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5**

- [x] 8. Checkpoint — Production pipeline
  - Run `cargo test` — ensure all tests pass, ask the user if questions arise.

- [ ] 9. Customer management
  - [x] 9.1 Implement CustomerService in Rust core
    - Create `core/src/sales/customers.rs`
    - Implement `create_customer()` with initial rating = 0 and mobile uniqueness check
    - Implement `update_reliability_rating()` with [1,5] bounds validation
    - Implement `search_customers()` supporting name, city, and mobile search via SQL LIKE
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [x] 9.2 Implement customer persistence queries
    - Create `core/src/persistence/queries/customers.rs`
    - SQLx queries: insert, update_rating, search (LIKE on name/city/mobile), get_by_id, get_by_mobile
    - _Requirements: 7.1, 7.3, 7.4_

  - [ ]* 9.3 Write property tests for customers
    - **Property 16: Customer initial reliability rating** — new customers have rating 0
    - **Property 17: Customer reliability rating bounds** — accept [1,5], reject outside
    - **Property 18: Customer mobile uniqueness** — duplicate mobile rejected
    - **Property 19: Customer search correctness** — matching queries return the customer
    - **Validates: Requirements 7.1, 7.2, 7.3, 7.4**

- [ ] 10. Wallet management
  - [x] 10.1 Implement WalletService in Rust core
    - Create `core/src/wallet/service.rs`
    - Implement `transfer_funds()` — atomic debit source + credit destination in SQLx transaction
    - Implement `credit_wallet()` and `debit_wallet()` with balance validation and transaction logging
    - Implement `get_wallets()` and `get_transaction_history()` queries
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.6_

  - [x] 10.2 Implement wallet persistence queries
    - Create `core/src/persistence/queries/wallets.rs`
    - SQLx queries: get_wallets, get_by_id, update_balance, insert_transaction, get_transactions
    - _Requirements: 9.1_

  - [ ]* 10.3 Write property tests for wallets
    - **Property 23: Wallet transfer conservation** — source decreases, destination increases, total conserved
    - **Property 24: Wallet insufficient funds rejection** — reject debit > balance, balance unchanged
    - **Validates: Requirements 9.2, 9.3, 11.3**

- [ ] 11. Sales transactions
  - [x] 11.1 Implement SalesService in Rust core
    - Create `core/src/sales/transactions.rs`
    - Implement `create_sale()` — validate stock, deduct inventory, credit wallet, create debt if partial payment, all in SQLx transaction
    - Implement sale total calculation as sum(qty × unit_price)
    - Create `core/src/sales/receipts.rs` — implement `generate_receipt()` with all required fields
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

  - [x] 11.2 Implement sales persistence queries
    - Create `core/src/persistence/queries/sales.rs`
    - SQLx queries: insert_sale, insert_line_items, get_sale_with_items (JOIN), list_sales
    - _Requirements: 8.1_

  - [ ]* 11.3 Write property tests for sales
    - **Property 20: Sale total calculation** — total = sum(qty × unit_price)
    - **Property 21: Sale financial orchestration** — wallet credited, inventory deducted, debt created if partial
    - **Property 22: Receipt completeness** — receipt contains all required fields
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5**

- [ ] 12. Debt tracking
  - [x] 12.1 Implement DebtService in Rust core
    - Create `core/src/debt/tracker.rs`
    - Implement `create_debt_record()` for unpaid sale balances
    - Implement `record_payment()` with FIFO allocation (oldest debts first by sale_date, using chrono)
    - Implement overdue days calculation: `(Utc::now() - sale_date).num_days()`
    - Implement critical flag: `is_critical = overdue_days > 30`
    - Implement `get_debt_aging_report()` sorted by overdue days descending
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [x] 12.2 Implement debt persistence queries
    - Create `core/src/persistence/queries/debts.rs`
    - SQLx queries: insert_debt, get_active_debts_for_customer (ORDER BY sale_date ASC), update_remaining, mark_settled, get_aging_report
    - _Requirements: 10.1_

  - [ ]* 12.3 Write property tests for debt tracking
    - **Property 27: Debt overdue days calculation** — overdue_days = (current_date - sale_date) in days
    - **Property 28: Debt aging report sort order** — sorted by overdue days descending
    - **Property 29: Debt payment FIFO allocation** — payment applied oldest first, settled debts marked
    - **Property 30: Debt critical flag threshold** — is_critical iff overdue_days > 30
    - **Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.5**

- [x] 13. Checkpoint — Financial pipeline
  - Run `cargo test` — ensure all tests pass, ask the user if questions arise.

- [ ] 14. Expense management
  - [x] 14.1 Implement ExpenseService in Rust core
    - Create `core/src/expenses/service.rs`
    - Implement `record_expense()` — validate wallet balance via `debit_wallet()`, store expense in SQLx transaction
    - Implement `get_expenses()` and `get_expenses_by_category()` with date range filtering
    - _Requirements: 11.1, 11.2, 11.3, 11.4_

  - [x] 14.2 Implement expense persistence queries
    - Create `core/src/persistence/queries/expenses.rs`
    - SQLx queries: insert_expense, get_by_date_range, get_grouped_by_category
    - _Requirements: 11.1_

  - [ ]* 14.3 Write property tests for expenses
    - **Property 25: Expense wallet debit** — wallet decreases by amount, expense contains all fields
    - **Property 31: Expense report grouping and totals** — grouped by category, subtotals correct, grand total = sum of subtotals
    - **Validates: Requirements 9.4, 11.1, 11.2, 11.4**

- [ ] 15. Reporting and invoicing
  - [x] 15.1 Implement ReportService in Rust core
    - Create `core/src/reports/financial.rs` — `get_financial_summary()` aggregating sales revenue, expenses, net profit, wallet balances
    - Create `core/src/reports/inventory_report.rs` — `get_inventory_report()` with low-stock flag based on configurable threshold
    - Create `core/src/reports/pdf.rs` — `export_to_pdf()` using genpdf with invoice template
    - Create `core/src/reports/excel.rs` — `export_to_excel()` using rust_xlsxwriter
    - Implement `generate_invoice()` — formatted invoice with all required fields
    - _Requirements: 12.1, 12.2, 12.3, 12.4_

  - [ ]* 15.2 Write property tests for reporting
    - **Property 32: Financial summary calculation** — revenue = sum(sales), expenses = sum(expenses), net = revenue - expenses
    - **Property 33: Low stock alert accuracy** — flagged iff quantity < threshold
    - **Property 34: Invoice completeness** — invoice contains all required fields
    - **Validates: Requirements 12.1, 12.2, 12.3**

- [ ] 16. Offline mode and sync
  - [x] 16.1 Implement SyncManager in Rust core
    - Create `core/src/sync/engine.rs` — sync orchestration
    - Create `core/src/sync/queue.rs` — offline queue management using sync_queue SQLx table
    - Create `core/src/sync/conflict.rs` — last-write-wins conflict resolution with ConflictLog entries
    - Implement FIFO sync processing on connectivity restore via reqwest HTTP client
    - Implement `is_online()` and `get_sync_status()` functions
    - Use tracing for structured logging of sync events
    - _Requirements: 13.1, 13.2, 13.3, 13.5_

  - [ ]* 16.2 Write property tests for sync
    - **Property 35: Offline queue persistence** — modifications stored in queue with correct metadata
    - **Property 36: Sync queue FIFO ordering** — processed in created_at ascending order
    - **Property 37: Conflict resolution logging** — last-write-wins applied, conflict logged
    - **Validates: Requirements 13.1, 13.2, 13.3**

- [x] 17. Checkpoint — Rust core complete
  - Run `cargo test` — ensure all tests pass, ask the user if questions arise.

- [ ] 18. UniFFI public API and Kotlin bindings
  - [x] 18.1 Implement UniFFI public API surface
    - Create `core/src/api.rs` with `SweetLabCore` struct wrapping all services
    - Export all public methods via `#[uniffi::export]` as defined in the design
    - Implement `SweetLabCore::new(db_path, policies_dir)` — runs migrations, initializes Casbin, creates all services
    - _Requirements: Technical Context (UniFFI bindings)_

  - [x] 18.2 Generate and verify Kotlin bindings
    - Run `uniffi-bindgen generate` to produce Kotlin bindings
    - Verify generated Kotlin classes match the Rust API surface
    - Create `android/app/src/main/kotlin/org/sweetlab/bindings/` with generated files
    - _Requirements: Technical Context (Android integration)_

- [ ] 19. Android app — Navigation and auth screens
  - [x] 19.1 Set up Android project with Compose and UniFFI native library
    - Create `android/build.gradle.kts` with Compose, Kotlin Coroutines, Navigation Compose dependencies
    - Configure NDK to load the Rust shared library (.so)
    - Create `SweetLabApp.kt` application class initializing `SweetLabCore`
    - _Requirements: Technical Context_

  - [x] 19.2 Set up Compose navigation with role-based routing
    - Create NavGraph with Admin, Chef, and Representative navigation sub-graphs
    - Implement role-based redirect after login (Admin → AdminDashboard, Chef → ProductionScreen, Rep → SalesScreen)
    - Implement offline indicator composable shown across all screens
    - _Requirements: 1.3, 2.4, 2.5, 2.6, 13.4_

  - [x] 19.3 Implement login screen
    - Compose login form with username/password fields
    - Error display for invalid credentials (generic message per Req 1.2)
    - Loading state during authentication
    - Calls `SweetLabCore.login()` via coroutine
    - _Requirements: 1.1, 1.2_

  - [ ]* 19.4 Write property test for role-based navigation
    - **Property 1: Role-based navigation routing** — each role maps to correct screen
    - **Validates: Requirements 1.3, 2.4, 2.5, 2.6**

- [ ] 20. Android app — Admin screens
  - [x] 20.1 Implement Admin Dashboard
    - Overview cards for wallet balances, active debts, low-stock alerts
    - Navigation to all admin modules
    - _Requirements: 2.6_

  - [x] 20.2 Implement User Management screen
    - List users, create user form, role update — calls `SweetLabCore.create_user()`, `update_user_role()`
    - _Requirements: 2.2, 2.3_

  - [x] 20.3 Implement Inventory screens (raw materials + finished goods)
    - List view with name, unit, quantity, last-updated for raw materials
    - List view with name, quantity, unit price, last-updated for finished goods
    - _Requirements: 3.5, 6.5_

  - [x] 20.4 Implement Recipe Management screen
    - Recipe list, create/edit form with ingredient picker (1-10 ingredients)
    - Delete with production log protection warning
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [x] 20.5 Implement Wallet Summary screen
    - Wallet list with balances, transaction history per wallet
    - Fund transfer dialog
    - _Requirements: 9.1, 9.2, 9.6_

  - [x] 20.6 Implement Reporting screens
    - Financial summary with date range picker
    - Inventory report with low-stock highlights
    - Debt aging report sorted by overdue days
    - Expense report grouped by category
    - PDF export button — calls `SweetLabCore.export_to_pdf()`
    - _Requirements: 10.2, 11.4, 12.1, 12.2, 12.4_

- [ ] 21. Android app — Chef screens
  - [x] 21.1 Implement Production screen
    - Recipe list with availability status (max_producible per recipe)
    - Production execution form (select recipe, enter quantity) — calls `SweetLabCore.execute_production()`
    - Production history log
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 22. Android app — Representative screens
  - [x] 22.1 Implement Sales screen
    - Sale creation form: customer picker, line item builder, payment entry, wallet selector
    - Sale total auto-calculation
    - Receipt generation and print — calls `SweetLabCore.generate_receipt()`
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

  - [x] 22.2 Implement Customer Management screen
    - Customer list with search (name, city, mobile)
    - Customer profile with debt summary and reliability rating
    - Create/edit customer form
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

  - [x] 22.3 Implement Expense Recording screen
    - Expense form: description, amount, category, wallet selector
    - Expense history list
    - _Requirements: 11.1, 11.2, 11.3_

  - [x] 22.4 Implement Customer Payment screen
    - Payment form: customer picker, amount, wallet selector
    - Display current debt summary for selected customer
    - _Requirements: 9.5, 10.3, 10.4_

- [x] 23. Checkpoint — Android app complete
  - Build APK, test on device/emulator, ensure all flows work end-to-end.

- [ ] 24. Desktop app (Slint)
  - [-] 24.1 Set up Slint desktop project
    - Create `desktop/Cargo.toml` with slint dependency + core crate dependency
    - Create `desktop/src/main.rs` — initialize `SweetLabCore` and Slint app
    - Create `desktop/ui/app.slint` — main window with navigation
    - _Requirements: Technical Context (Slint desktop)_

  - [~] 24.2 Implement Slint login screen
    - Create `desktop/ui/login.slint` — username/password form
    - Wire to `SweetLabCore.login()` via direct Rust calls
    - Role-based navigation after login
    - _Requirements: 1.1, 1.2, 1.3_

  - [~] 24.3 Implement Slint Admin screens
    - Create `desktop/ui/admin/` — dashboard, user management, inventory, recipes, wallets, reports
    - All screens call `SweetLabCore` methods directly (no FFI needed)
    - _Requirements: 2.6, 3.5, 4.1-4.5, 6.5, 9.1-9.6, 10.2, 11.4, 12.1-12.4_

  - [~] 24.4 Implement Slint Chef screens
    - Create `desktop/ui/chef/` — production screen with recipe availability and execution
    - _Requirements: 5.1-5.5_

  - [~] 24.5 Implement Slint Representative screens
    - Create `desktop/ui/representative/` — sales, customers, expenses, payments
    - _Requirements: 7.1-7.5, 8.1-8.5, 9.5, 10.3-10.4, 11.1-11.3_

- [ ] 25. Checkpoint — Desktop app complete
  - Build desktop binary, test all flows, ensure parity with Android app.

- [ ] 26. Integration and end-to-end testing
  - [ ]* 26.1 Write integration tests for critical flows
    - Test full production flow: create recipe → execute production → verify inventory changes
    - Test full sale flow: create sale → verify inventory + wallet + debt changes
    - Test full payment flow: record payment → verify wallet + debt FIFO allocation
    - All tests use in-memory SQLite via SQLx
    - _Requirements: 5.2, 8.2, 8.3, 9.5, 10.3_

- [ ] 27. Final checkpoint — Full integration
  - Run `cargo test` for core, build Android APK, build Desktop binary. Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation — `cargo test` at each checkpoint
- Property tests validate universal correctness properties (39 properties total) using `proptest`
- Unit tests validate specific examples and edge cases
- All business logic lives in the Rust core — platform UIs are thin shells
- Android calls core via UniFFI/JNI; Desktop calls core directly (same Rust)
- Proto files are the single source of truth for data models — prost generates Rust structs, wire generates Kotlin classes
