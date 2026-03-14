# Requirements Document

## Introduction

Sweet Lab Hardening is a comprehensive remediation and integration effort for the Sweet Lab ERP system. A senior code review identified 8 critical, 19 high, and 12 medium severity issues across the Rust core engine. Additionally, the desktop Slint UI is scaffolded but not wired to the Core_Engine. This spec addresses all 39 issues systematically and completes the desktop UI integration so the application functions end-to-end on both Android and Desktop platforms.

The fixes span monetary precision, authorization enforcement, data security, race conditions, input validation, financial correctness, query performance, schema integrity, code deduplication, and full desktop UI wiring.

## Glossary

- **Core_Engine**: The pure Rust library crate (`sweet-lab-core`) containing all business logic, persistence, RBAC, and report generation.
- **Auth_Module**: The Rust module responsible for user authentication (argon2 password hashing), session management, and RBAC enforcement.
- **Inventory_Service**: The Rust module managing raw material stock levels and finished goods quantities.
- **Sales_Service**: The Rust module handling sales transactions, customer management, and invoice generation.
- **Wallet_Service**: The Rust module managing financial wallets and fund transfers.
- **Debt_Tracker**: The Rust module that monitors customer payment obligations and FIFO allocation.
- **Report_Generator**: The Rust module that produces financial summaries, inventory reports, and invoices.
- **Sync_Engine**: The Rust module responsible for data synchronization between local SQLite and the cloud.
- **Desktop_App**: The Slint-based desktop application that calls the Core_Engine directly via Rust.
- **Money**: An integer-cents representation (i64) where 1 unit = 0.01 of the display currency, eliminating floating-point rounding errors.
- **Session_Token**: A UUID identifying an authenticated user session, required for all API calls that mutate data.
- **Pagination_Cursor**: A token or offset used to retrieve a bounded subset of list query results.

## Requirements

### Requirement 1: Monetary Precision — Replace f64 with Integer Cents

**User Story:** As a system admin, I want all monetary values stored and computed as integer cents (i64), so that financial calculations are free from floating-point rounding errors.

#### Acceptance Criteria

1. THE Core_Engine SHALL represent all monetary fields (prices, balances, amounts, totals, debts, expenses, revenues) as i64 integer cents internally and in the SQLite schema.
2. WHEN displaying monetary values to the user, THE Core_Engine SHALL provide a formatting function that converts integer cents to a human-readable decimal string with exactly two decimal places.
3. WHEN accepting monetary input from the UI, THE Core_Engine SHALL provide a parsing function that converts a decimal string to integer cents, rejecting malformed input.
4. FOR ALL valid Money values, parsing the formatted string back to cents SHALL produce the original value (round-trip property).
5. WHEN migrating existing REAL columns to INTEGER, THE Core_Engine SHALL multiply existing values by 100 and round to the nearest integer.

### Requirement 2: Authorization Enforcement on API Methods

**User Story:** As a system admin, I want every mutating API method to verify the caller's session and role permissions, so that unauthorized users cannot perform privileged operations.

#### Acceptance Criteria

1. WHEN any mutating API method is called, THE Core_Engine SHALL require a valid Session_Token parameter and verify the session has not expired.
2. WHEN a valid session is provided, THE Auth_Module SHALL check the session's role against the Casbin RBAC policy for the requested resource and action before executing the operation.
3. IF a session token is missing, expired, or invalid, THEN THE Core_Engine SHALL return an Authentication error and refuse the operation.
4. IF the session's role lacks permission for the requested operation, THEN THE Core_Engine SHALL return an Authorization error and refuse the operation.
5. THE Core_Engine SHALL enforce authorization checks on all API methods including create_sale, record_expense, execute_production, transfer_funds, create_user, update_user_role, record_debt_payment, and all inventory mutation methods.

### Requirement 3: Password Hash Exclusion from API Responses

**User Story:** As a security engineer, I want the password hash never exposed in API responses or protobuf messages, so that credential material cannot leak to clients.

#### Acceptance Criteria

1. THE Core_Engine SHALL define a separate SafeUser type that excludes the password_hash field for all API responses and client-facing data.
2. WHEN the Core_Engine returns user data through any API method, THE Core_Engine SHALL use the SafeUser type that omits the password_hash.
3. THE Schema_Layer SHALL remove the password_hash field from the AppUser protobuf message used in API responses.
4. WHEN serializing user data to JSON, THE Core_Engine SHALL exclude the password_hash field from the output.

### Requirement 4: Atomic Inventory Deduction

**User Story:** As a developer, I want single-item inventory deductions to be atomic (check-and-update in one SQL statement), so that concurrent requests cannot cause negative stock.

#### Acceptance Criteria

1. WHEN deducting a single raw material, THE Inventory_Service SHALL use a single SQL UPDATE with a WHERE clause that checks `current_quantity >= requested` and deduct atomically, instead of separate SELECT then UPDATE.
2. IF the atomic UPDATE affects zero rows (insufficient stock), THEN THE Inventory_Service SHALL return an InsufficientStock error with the material name and shortfall.
3. WHEN deducting finished goods for a sale, THE Sales_Service SHALL use the same atomic UPDATE pattern within the transaction.

### Requirement 5: Sale Input Validation

**User Story:** As a representative, I want the system to reject invalid sale inputs (negative payment, NaN, overpayment, wrong prices), so that financial records remain accurate.

#### Acceptance Criteria

1. WHEN creating a sale, THE Sales_Service SHALL reject an amount_paid that is negative and return a Validation error.
2. WHEN creating a sale, THE Sales_Service SHALL reject an amount_paid that exceeds the computed total_amount and return a Validation error.
3. WHEN creating a sale, THE Sales_Service SHALL verify each line item's unit_price against the finished good's current unit_price in the database and reject mismatches.
4. WHEN creating a sale, THE Sales_Service SHALL reject line items with zero or negative quantity and return a Validation error.
5. WHEN creating a sale with an empty line_items list, THE Sales_Service SHALL return a Validation error.

### Requirement 6: Debt Payment Wallet Credit

**User Story:** As a representative, I want debt payments to credit the receiving wallet, so that the wallet balance reflects collected funds.

#### Acceptance Criteria

1. WHEN recording a debt payment, THE Debt_Tracker SHALL credit the specified wallet by the payment amount within the same transaction as the FIFO debt allocation.
2. WHEN recording a debt payment, THE Debt_Tracker SHALL validate that the specified wallet exists before proceeding.
3. WHEN a debt payment exceeds the total outstanding debt, THE Debt_Tracker SHALL allocate only the owed amount to debts and credit the wallet only for the amount actually allocated, returning the unallocated remainder in the response.

### Requirement 7: Wallet Operation Validation

**User Story:** As a system admin, I want wallet operations to reject invalid inputs (negative amounts, self-transfers), so that wallet balances remain consistent.

#### Acceptance Criteria

1. WHEN a wallet credit, debit, or transfer operation receives a negative or zero amount, THE Wallet_Service SHALL return a Validation error.
2. WHEN a fund transfer is initiated where the source and destination wallets are the same, THE Wallet_Service SHALL return a Validation error.
3. WHEN updating a wallet balance, THE Wallet_Service SHALL use atomic SQL delta operations (`current_balance = current_balance + ?`) instead of absolute SET, to prevent lost updates under concurrency.

### Requirement 8: Financial Reporting Accuracy

**User Story:** As a system admin, I want financial summaries to use amount_paid for revenue and account for cost of goods sold, so that reports reflect actual cash flow and profitability.

#### Acceptance Criteria

1. WHEN calculating total revenue in a financial summary, THE Report_Generator SHALL sum the amount_paid field (not total_amount) from sales in the date range.
2. WHEN calculating net profit, THE Report_Generator SHALL subtract both expenses and cost of goods sold (COGS) from revenue, or clearly label the metric as "gross margin" if COGS is excluded.
3. WHEN an overpayment occurs on a sale (amount_paid > total_amount after validation changes), THE Sales_Service SHALL handle the excess as a wallet credit and not silently discard it.

### Requirement 9: NotFound Error Variant

**User Story:** As a developer, I want a dedicated NotFound error variant, so that missing-entity errors are semantically distinct from validation errors.

#### Acceptance Criteria

1. THE Core_Engine SHALL add a NotFound variant to AppError with entity_type and entity_id fields.
2. WHEN any service looks up an entity by ID and the entity does not exist, THE Core_Engine SHALL return AppError::NotFound instead of AppError::Validation.
3. WHEN the NotFound error is returned, THE Core_Engine SHALL include the entity type and the requested ID in the error message.

### Requirement 10: Session Persistence and Inactivity Timeout

**User Story:** As any employee, I want my session to survive server restarts and expire after 8 hours of inactivity (not just 8 hours total), so that I am not unexpectedly logged out.

#### Acceptance Criteria

1. THE Auth_Module SHALL persist sessions to the SQLite database instead of storing them only in memory.
2. WHEN any authenticated API call is made, THE Auth_Module SHALL update the session's last_activity timestamp.
3. WHEN a session's last_activity exceeds 8 hours from the current time, THE Auth_Module SHALL consider the session expired and require re-authentication.
4. THE Auth_Module SHALL enforce a maximum absolute session lifetime of 24 hours regardless of activity.

### Requirement 11: Password Strength Validation

**User Story:** As a system admin, I want password creation to enforce minimum strength rules, so that user accounts are protected against weak credentials.

#### Acceptance Criteria

1. WHEN creating a user or changing a password, THE Auth_Module SHALL require a minimum password length of 8 characters.
2. WHEN a password does not meet the strength requirements, THE Auth_Module SHALL return a Validation error describing the specific requirement that was not met.

### Requirement 12: Query Consolidation and Performance

**User Story:** As a developer, I want redundant database round-trips consolidated into single queries and N+1 patterns eliminated, so that the system performs efficiently.

#### Acceptance Criteria

1. WHEN fetching sales history, THE Sales_Service SHALL retrieve sales and their line items using a JOIN query or a batched query instead of issuing one query per sale (N+1 elimination).
2. WHEN fetching recipe availability, THE Core_Engine SHALL compute availability in a single query joining recipes, ingredients, and raw materials instead of multiple round-trips.
3. THE Auth_Module SHALL use the shared persistence query layer for user lookups instead of duplicating SQL inline.
4. WHEN fetching customer data with debt summaries, THE Sales_Service SHALL compute total_debt and overdue_days from the debt_records table via a JOIN or subquery, not return hardcoded zeros.

### Requirement 13: Pagination on List Queries

**User Story:** As a system admin, I want all list queries to support pagination, so that the system remains responsive as data grows.

#### Acceptance Criteria

1. WHEN listing sales, customers, expenses, debts, production logs, or inventory items, THE Core_Engine SHALL accept optional limit and offset parameters.
2. WHEN pagination parameters are provided, THE Core_Engine SHALL return at most `limit` results starting from `offset`.
3. WHEN pagination parameters are omitted, THE Core_Engine SHALL apply a default limit of 100 results.

### Requirement 14: Debt Payment Persistence

**User Story:** As a system admin, I want debt payments persisted in a dedicated table, so that payment history is auditable and survives restarts.

#### Acceptance Criteria

1. THE Core_Engine SHALL create a debt_payments table storing each payment with id, customer_id, amount, wallet_id, timestamp, and sync_status.
2. THE Core_Engine SHALL create a debt_payment_allocations table storing each allocation with payment_id, debt_record_id, and amount_applied.
3. WHEN recording a debt payment, THE Debt_Tracker SHALL insert records into both tables within the same transaction as the FIFO allocation.

### Requirement 15: Expense Validation

**User Story:** As a developer, I want expense recording to validate that the recorded_by user exists, so that expenses are always linked to valid users.

#### Acceptance Criteria

1. WHEN recording an expense, THE Core_Engine SHALL verify that the recorded_by user ID exists in the users table.
2. IF the recorded_by user does not exist, THEN THE Core_Engine SHALL return a NotFound error.

### Requirement 16: Schema Integrity and Indexing

**User Story:** As a developer, I want foreign key constraints and indexes on all FK columns, so that data integrity is enforced and queries perform well.

#### Acceptance Criteria

1. THE Core_Engine SHALL add a REFERENCES finished_goods(id) foreign key constraint to the sale_line_items.finished_good_id column via a new migration.
2. THE Core_Engine SHALL add indexes on all foreign key columns that lack them: sales.customer_id, sales.payment_wallet_id, sale_line_items.sale_id, production_logs.recipe_id, production_logs.chef_id, production_logs.finished_good_id, recipe_ingredients.recipe_id, recipe_ingredients.raw_material_id, debt_records.customer_id, debt_records.sale_id, expenses.wallet_id, expenses.recorded_by, wallet_transactions.wallet_id.
3. THE Core_Engine SHALL enable `PRAGMA foreign_keys = ON` at connection initialization.

### Requirement 17: Code Deduplication

**User Story:** As a developer, I want duplicated utility code consolidated into shared modules, so that the codebase is maintainable.

#### Acceptance Criteria

1. THE Core_Engine SHALL provide a single shared `parse_uuid()` utility function used by all modules instead of duplicating UUID parsing in 5+ files.
2. THE Core_Engine SHALL provide a single shared `parse_wallet_type()` function used by both wallet/service.rs and reports/financial.rs.
3. THE Core_Engine SHALL consolidate the `generate_invoice` and `generate_receipt` functions to share common formatting logic, eliminating duplication.
4. THE Core_Engine SHALL extract common `row_to_domain` conversion patterns into a shared trait or macro to reduce boilerplate.

### Requirement 18: Inventory Deduction Threshold Fix

**User Story:** As a chef, I want the insufficient materials check to catch all cases where stock is less than needed (not just zero), so that production runs never proceed with inadequate materials.

#### Acceptance Criteria

1. WHEN checking material availability for a recipe, THE Recipe_Engine SHALL flag a material as insufficient when its current_quantity is less than the required_quantity (not only when current_quantity equals zero).
2. WHEN listing insufficient materials in RecipeAvailability, THE Recipe_Engine SHALL include all materials where current_quantity < required_quantity × production_quantity.

### Requirement 19: Configurable Business Name

**User Story:** As a system admin, I want the business name to be configurable instead of hardcoded, so that the system can be reused for different businesses.

#### Acceptance Criteria

1. THE Core_Engine SHALL accept a business_name configuration parameter at initialization instead of hardcoding "Sweet Lab".
2. WHEN generating invoices or receipts, THE Report_Generator SHALL use the configured business name.

### Requirement 20: Sync Module Integration

**User Story:** As a developer, I want the sync module wired into the API surface and the SyncManagerImpl initialized with a valid URL, so that offline/online sync functionality is operational.

#### Acceptance Criteria

1. WHEN initializing SweetLabCore, THE Core_Engine SHALL accept an optional sync_base_url parameter and initialize the SyncManagerImpl with it.
2. WHEN sync_base_url is not provided, THE Core_Engine SHALL initialize the SyncManagerImpl in offline-only mode.
3. THE Core_Engine SHALL expose sync_all, is_online, get_sync_status, and get_conflict_log through the public API.

### Requirement 21: UniFFI Bindings Definition

**User Story:** As a developer, I want the UDL file to define all exported types and functions, so that UniFFI can generate working Kotlin bindings for the Android app.

#### Acceptance Criteria

1. THE Core_Engine SHALL populate the sweet_lab_core.udl file with all public API types, enums, and function signatures that the Android app requires.
2. WHEN the UDL file is updated, THE Core_Engine SHALL verify that `uniffi-bindgen generate` produces valid Kotlin bindings without errors.

### Requirement 22: PDF Font Path Configuration

**User Story:** As a developer, I want PDF font paths to be configurable, so that report generation works on both desktop (./fonts/) and Android (app assets) environments.

#### Acceptance Criteria

1. WHEN generating PDF reports, THE Report_Generator SHALL accept a font directory path parameter instead of hardcoding `./fonts/`.
2. WHEN the font directory is not found, THE Report_Generator SHALL return a descriptive error instead of panicking.

### Requirement 23: Conflict Log Management

**User Story:** As a system admin, I want the conflict log to support cleanup and pagination, so that it does not grow unbounded.

#### Acceptance Criteria

1. WHEN querying the conflict log, THE Sync_Engine SHALL support pagination with limit and offset parameters.
2. THE Sync_Engine SHALL provide a cleanup function that deletes conflict log entries older than a configurable retention period.

### Requirement 24: Desktop UI Wiring

**User Story:** As a desktop user, I want the Slint UI connected to the live Core_Engine, so that I can perform all ERP operations (login, inventory, production, sales, wallets, debts, expenses, reports) with real data.

#### Acceptance Criteria

1. WHEN the Desktop_App starts, THE Desktop_App SHALL initialize a SweetLabCore instance with a SQLite database path and run migrations.
2. WHEN a user submits login credentials in the Desktop_App, THE Desktop_App SHALL call Core_Engine.login() and route to the role-appropriate screen on success.
3. WHEN an admin navigates to the inventory screen, THE Desktop_App SHALL fetch and display live raw material and finished goods data from the Core_Engine.
4. WHEN an admin navigates to the user management screen, THE Desktop_App SHALL fetch and display the user list and support creating new users and updating roles via the Core_Engine.
5. WHEN an admin navigates to the wallets screen, THE Desktop_App SHALL fetch and display wallet balances and transaction history from the Core_Engine, and support fund transfers.
6. WHEN an admin navigates to the recipes screen, THE Desktop_App SHALL fetch and display recipes from the Core_Engine and support create, edit, and delete operations.
7. WHEN an admin navigates to the reports screen, THE Desktop_App SHALL fetch and display financial summaries, inventory reports, and debt aging reports from the Core_Engine.
8. WHEN a chef navigates to the production screen, THE Desktop_App SHALL fetch recipe availability from the Core_Engine and support executing production runs.
9. WHEN a representative navigates to the sales screen, THE Desktop_App SHALL support creating sales with customer selection, line item entry, and payment via the Core_Engine.
10. WHEN a representative navigates to the customers screen, THE Desktop_App SHALL fetch and display customer data and support search, create, and rating updates via the Core_Engine.
11. WHEN a representative navigates to the expenses screen, THE Desktop_App SHALL support recording expenses via the Core_Engine.
12. WHEN a representative navigates to the payments screen, THE Desktop_App SHALL support recording debt payments via the Core_Engine.
13. WHEN any data-mutating operation completes in the Desktop_App, THE Desktop_App SHALL refresh the affected screen data automatically.
14. WHILE the Desktop_App is performing an async Core_Engine call, THE Desktop_App SHALL display a loading indicator and disable the triggering control to prevent double-submission.
