# Implementation Plan: Sweet Lab ERP

## Overview

Incremental implementation of the Sweet Lab ERP as a Kotlin Multiplatform (Android + Desktop) app using Compose Multiplatform, SQLDelight, Supabase, and Koin. Tasks build on each other — starting with KMP project scaffolding and data layer, then domain use cases with property tests, then shared presentation layer, and finally sync/offline support.

## Tasks

- [ ] 1. KMP project scaffolding and core setup
  - [ ] 1.1 Set up KMP project structure with shared, androidApp, and desktopApp modules
    - Create root `build.gradle.kts` with KMP plugin, Compose Multiplatform plugin
    - Create `shared/build.gradle.kts` with commonMain/androidMain/desktopMain source sets
    - Add dependencies: SQLDelight, Supabase-kt (Auth, Postgrest, Realtime), Koin, Ktor Client, kotlinx-serialization, kotlinx-datetime, konform, Kermit, OpenPDF, Kotest property, JUnit 5, MockK
    - Create `androidApp/build.gradle.kts` and `desktopApp/build.gradle.kts`
    - Configure SQLDelight plugin for `SweetLabDatabase`
    - _Requirements: Technical Context_

  - [ ] 1.2 Define domain models and enums in commonMain
    - Create `UserRole`, `WalletType`, `ExpenseCategory`, `SyncStatus` enums
    - Create all domain data classes: `AppUser`, `RawMaterial`, `FinishedGood`, `Recipe`, `RecipeIngredient`, `ProductionLog`, `RecipeAvailability`, `Customer`, `Sale`, `SaleLineItem`, `Receipt`, `Wallet`, `WalletTransaction`, `FundTransfer`, `DebtRecord`, `DebtPayment`, `DebtAllocation`, `Expense`, `FinancialSummary`, `InventoryReport`, `Invoice`, `SyncQueueItem`, `ConflictLog`
    - Create sealed class `AppError` with all error subtypes
    - Create `Permission` enum and `RolePermissions` object for hand-rolled RBAC
    - Use `kotlinx-datetime.Instant` for all timestamp fields
    - _Requirements: All requirements (domain model foundation)_

  - [ ] 1.3 Create SQLDelight schema files (.sq) in commonMain
    - Create `Users.sq`, `RawMaterials.sq`, `FinishedGoods.sq`, `Recipes.sq`, `ProductionLogs.sq`, `Customers.sq`, `Sales.sq`, `Wallets.sq`, `DebtRecords.sq`, `Expenses.sq`, `SyncQueue.sq` as defined in the design Data Models section
    - Include all named queries for CRUD, search, aggregation, and transactional operations
    - _Requirements: 14.1, 14.2_

  - [ ] 1.4 Implement platform-specific SQLDriver (expect/actual)
    - Create `DatabaseDriverFactory` expect declaration in commonMain
    - Implement `AndroidSqliteDriver` actual in androidMain
    - Implement `JdbcSqliteDriver` actual in desktopMain
    - Create SQLDelight-to-domain and domain-to-SQLDelight mapper functions
    - _Requirements: Technical Context (multiplatform database)_

  - [ ] 1.5 Set up Koin modules
    - Create shared `domainModule`, `dataModule`, `supabaseModule` in commonMain
    - Create `androidPlatformModule` in androidMain (Android SQLDriver)
    - Create `desktopPlatformModule` in desktopMain (JVM SQLDriver)
    - _Requirements: Technical Context (Koin DI)_

- [ ] 2. Serialization and schema validation
  - [ ] 2.1 Implement Kotlinx Serialization models and EntityMapper
    - Create `@Serializable` counterparts for all domain models in commonMain
    - Implement custom `InstantSerializer` for kotlinx-datetime
    - Implement `EntityMapper` with `toSerializable()` and `toDomain()` extension functions
    - Implement `SchemaValidator` interface and its konform-based implementation
    - _Requirements: 14.1, 14.2, 14.4_

  - [ ]* 2.2 Write property test: JSON serialization round-trip
    - **Property 38: JSON serialization round-trip**
    - Create `Arb` generators for all domain objects in `DomainGenerators.kt`
    - Test serialize→deserialize produces equivalent object for each entity type
    - **Validates: Requirements 14.1, 14.2, 14.3**

  - [ ]* 2.3 Write property test: Schema validation rejects malformed data
    - **Property 39: Schema validation rejects malformed data**
    - Generate malformed JSON (missing fields, wrong types, out-of-range values) and verify rejection
    - **Validates: Requirements 14.4**

- [ ] 3. Authentication and RBAC
  - [ ] 3.1 Implement AuthUseCase with Supabase Auth integration
    - Implement `login()` with Supabase Auth email/password sign-in
    - Implement session management with 8-hour inactivity timeout using kotlinx-datetime
    - Implement `createUser()` with field validation (email, password, fullName, role required)
    - Implement `updateUserRole()` — update `users` table role + Supabase user metadata
    - Implement role-based permission checking via `RolePermissions` object
    - _Requirements: 1.1, 1.2, 1.4, 2.1, 2.2, 2.3_

  - [ ] 3.2 Implement UserRepository (SQLDelight + Supabase Auth)
    - Local user caching in SQLDelight
    - Supabase Auth integration for credential operations via supabase-kt
    - Role read/write from `users` Supabase table
    - _Requirements: 1.1, 2.2, 2.3_

  - [ ]* 3.3 Write property tests for authentication
    - **Property 2: Role permission enforcement** — for any role/resource combination, access matches permission set
    - **Property 3: Invalid credentials rejection** — error message does not reveal which field is wrong
    - **Property 4: User creation field validation** — missing fields rejected, complete fields accepted
    - **Property 5: Role update applies on next login**
    - **Validates: Requirements 1.2, 1.5, 2.1, 2.2, 2.3**

  - [ ]* 3.4 Write unit tests for auth edge cases
    - Test session expiry at exactly 8 hours
    - Test login with empty email/password
    - Test role update for non-existent user
    - _Requirements: 1.4, 1.5, 2.3_

- [ ] 4. Checkpoint — Core foundation
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. Inventory management
  - [ ] 5.1 Implement InventoryUseCase in commonMain
    - Implement `addRawMaterialPurchase()` — increase quantity by purchased amount
    - Implement `deductRawMaterials()` — atomic multi-material deduction in SQLDelight `transaction { }` with insufficient stock check
    - Implement `addFinishedGoods()` and `deductFinishedGoods()` with stock validation
    - Implement `getRawMaterials()` and `getFinishedGoods()` as reactive `Flow` from SQLDelight queries
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 6.1, 6.2, 6.3, 6.4_

  - [ ] 5.2 Implement RawMaterialRepository and FinishedGoodRepository
    - SQLDelight query integration with Supabase Postgrest sync
    - `Flow`-based reactive queries via SQLDelight `.asFlow().mapToList()`
    - _Requirements: 3.1, 6.1_

  - [ ]* 5.3 Write property tests for inventory
    - **Property 6: Raw material purchase increases quantity**
    - **Property 7: Insufficient raw material stock rejection**
    - **Property 15: Finished good insufficient stock rejection**
    - **Validates: Requirements 3.2, 3.4, 6.4**

  - [ ]* 5.4 Write unit tests for inventory edge cases
    - Test deduction of exactly available quantity (boundary)
    - Test deduction of zero quantity
    - Test concurrent deductions
    - _Requirements: 3.4, 6.4_

- [ ] 6. Recipe management
  - [ ] 6.1 Implement RecipeUseCase in commonMain
    - Implement `createRecipe()` with konform validation (1-10 ingredients, positive quantities) and material existence check
    - Implement `updateRecipe()` with same validations
    - Implement `deleteRecipe()` with production log reference check via `countProductionLogsForRecipe` query
    - Implement `validateIngredients()` for material existence
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [ ] 6.2 Implement RecipeRepository
    - SQLDelight queries with recipe + ingredients join
    - Supabase Postgrest sync for recipes and recipe_ingredients
    - _Requirements: 4.1_

  - [ ]* 6.3 Write property tests for recipes
    - **Property 12: Recipe ingredient count bounds** — accept 1-10, reject 0 or 11+
    - **Property 13: Recipe material existence validation** — reject recipes with non-existent materials
    - **Property 14: Recipe deletion protection** — block deletion if production logs exist
    - **Validates: Requirements 4.1, 4.2, 4.4, 4.5**

- [ ] 7. Production execution
  - [ ] 7.1 Implement ProductionUseCase in commonMain
    - Implement `executeProduction()` — calculate requirements, validate stock, atomic deduction + increment + log, all in SQLDelight `transaction { }`
    - Implement `getRecipeAvailability()` — compute maxProducible per recipe
    - Implement `getProductionHistory()` as reactive `Flow`
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [ ]* 7.2 Write property tests for production
    - **Property 8: Production run inventory conservation** — raw materials decrease by recipe×qty, finished good increases by qty
    - **Property 9: Production run rejection on insufficient stock** — entire run rejected, no quantities change
    - **Property 10: Production log completeness** — log contains all required fields
    - **Property 11: Recipe availability calculation** — maxProducible = min(stock/required) floored
    - **Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5**

- [ ] 8. Checkpoint — Production pipeline
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. Customer management
  - [ ] 9.1 Implement CustomerUseCase in commonMain
    - Implement `createCustomer()` with initial rating = 0 and mobile uniqueness check
    - Implement `updateReliabilityRating()` with konform validation [1,5] bounds
    - Implement `searchCustomers()` supporting name, city, and mobile search via SQLDelight `search` query
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [ ] 9.2 Implement CustomerRepository
    - SQLDelight queries with UNIQUE constraint on mobile
    - Supabase Postgrest sync
    - _Requirements: 7.1, 7.3, 7.4_

  - [ ]* 9.3 Write property tests for customers
    - **Property 16: Customer initial reliability rating** — new customers have rating 0
    - **Property 17: Customer reliability rating bounds** — accept [1,5], reject outside
    - **Property 18: Customer mobile uniqueness** — duplicate mobile rejected
    - **Property 19: Customer search correctness** — matching queries return the customer
    - **Validates: Requirements 7.1, 7.2, 7.3, 7.4**

- [ ] 10. Wallet management
  - [ ] 10.1 Implement WalletUseCase in commonMain
    - Implement `transferFunds()` — atomic debit source + credit destination in SQLDelight `transaction { }`
    - Implement `creditWallet()` and `debitWallet()` with balance validation and transaction logging
    - Implement `getWallets()` and `getTransactionHistory()` as reactive `Flow`
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.6_

  - [ ] 10.2 Implement WalletRepository
    - SQLDelight queries with wallet + transactions
    - Supabase Postgrest sync for wallets and wallet_transactions
    - _Requirements: 9.1_

  - [ ]* 10.3 Write property tests for wallets
    - **Property 23: Wallet transfer conservation** — source decreases, destination increases, total conserved
    - **Property 24: Wallet insufficient funds rejection** — reject debit > balance, balance unchanged
    - **Validates: Requirements 9.2, 9.3, 11.3**

- [ ] 11. Sales transactions
  - [ ] 11.1 Implement SalesUseCase in commonMain
    - Implement `createSale()` — validate stock, deduct inventory, credit wallet, create debt if partial payment, all in SQLDelight `transaction { }`
    - Implement sale total calculation as sum(qty × unitPrice)
    - Implement `generateReceipt()` with all required fields
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

  - [ ] 11.2 Implement SaleRepository
    - SQLDelight queries with sale + line items join
    - Supabase Postgrest sync for sales and sale_line_items
    - _Requirements: 8.1_

  - [ ]* 11.3 Write property tests for sales
    - **Property 20: Sale total calculation** — total = sum(qty × unitPrice)
    - **Property 21: Sale financial orchestration** — wallet credited, inventory deducted, debt created if partial
    - **Property 22: Receipt completeness** — receipt contains all required fields
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5**

- [ ] 12. Debt tracking
  - [ ] 12.1 Implement DebtUseCase in commonMain
    - Implement `createDebtRecord()` for unpaid sale balances
    - Implement `recordPayment()` with FIFO allocation (oldest debts first by saleDate)
    - Implement overdue days calculation using `kotlinx-datetime` (`Clock.System.now()` minus `saleDate`)
    - Implement critical flag (>30 days) computation
    - Implement `getDebtAgingReport()` sorted by overdue days descending
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [ ] 12.2 Implement DebtRepository
    - SQLDelight queries with computed overdue days
    - Supabase Postgrest sync
    - _Requirements: 10.1_

  - [ ]* 12.3 Write property tests for debt tracking
    - **Property 27: Debt overdue days calculation** — overdueDays = (currentDate - saleDate) in days
    - **Property 28: Debt aging report sort order** — sorted by overdue days descending
    - **Property 29: Debt payment FIFO allocation** — payment applied oldest first, settled debts marked
    - **Property 30: Debt critical flag threshold** — isCritical iff overdueDays > 30
    - **Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.5**

- [ ] 13. Checkpoint — Financial pipeline
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 14. Expense management
  - [ ] 14.1 Implement ExpenseUseCase in commonMain
    - Implement `recordExpense()` — validate wallet balance via `WalletUseCase.debitWallet`, store expense
    - Implement `getExpenses()` and `getExpensesByCategory()` with date range filtering
    - _Requirements: 11.1, 11.2, 11.3, 11.4_

  - [ ] 14.2 Implement ExpenseRepository
    - SQLDelight queries with category grouping and date range
    - Supabase Postgrest sync
    - _Requirements: 11.1_

  - [ ]* 14.3 Write property tests for expenses
    - **Property 25: Expense wallet debit** — wallet decreases by amount, expense contains all fields
    - **Property 31: Expense report grouping and totals** — grouped by category, subtotals correct, grand total = sum of subtotals
    - **Validates: Requirements 9.4, 11.1, 11.2, 11.4**

- [ ] 15. Reporting and invoicing
  - [ ] 15.1 Implement ReportUseCase in commonMain
    - Implement `getFinancialSummary()` — aggregate sales revenue, expenses, net profit, wallet balances
    - Implement `getInventoryReport()` — all items with low-stock flag based on configurable threshold
    - Implement `generateInvoice()` — formatted invoice with all required fields
    - Implement `exportToPdf()` — OpenPDF-based PDF generation (JVM, shared between Android and Desktop)
    - _Requirements: 12.1, 12.2, 12.3, 12.4_

  - [ ]* 15.2 Write property tests for reporting
    - **Property 32: Financial summary calculation** — revenue = sum(sales), expenses = sum(expenses), net = revenue - expenses
    - **Property 33: Low stock alert accuracy** — flagged iff quantity < threshold
    - **Property 34: Invoice completeness** — invoice contains all required fields
    - **Validates: Requirements 12.1, 12.2, 12.3**

- [ ] 16. Offline mode and sync
  - [ ] 16.1 Implement SyncManager in commonMain
    - Implement Supabase Realtime channel subscriptions for all tables
    - Implement sync queue (SQLDelight `sync_queue` table) for tracking pending writes
    - Implement FIFO sync processing on connectivity restore via Ktor Client + Supabase Postgrest
    - Implement last-write-wins conflict resolution with `conflict_log` entries
    - Implement `isOnline()` and `getSyncStatus()` as reactive `Flow`
    - Use Kermit for structured logging of sync events
    - _Requirements: 13.1, 13.2, 13.3, 13.5_

  - [ ]* 16.2 Write property tests for sync
    - **Property 35: Offline queue persistence** — modifications stored in queue with correct metadata
    - **Property 36: Sync queue FIFO ordering** — processed in createdAt ascending order
    - **Property 37: Conflict resolution logging** — last-write-wins applied, conflict logged
    - **Validates: Requirements 13.1, 13.2, 13.3**

- [ ] 17. Checkpoint — Backend complete
  - Ensure all tests pass, ask the user if questions arise.
