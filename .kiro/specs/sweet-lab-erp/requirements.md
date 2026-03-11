# Requirements Document

## Introduction

Sweet Lab ERP is a specialized ERP-lite Kotlin Multiplatform application (Android + Desktop) for a confectionery factory called "Sweet Lab". It transforms the existing "Mohasib Soft" (ChaOffice) JavaFX desktop application into a multiplatform, cloud-synced system supporting production management (recipe-based raw material to finished goods conversion), multi-role access control, sales and financial tracking with wallet-based fund management, customer debt aging, and offline-capable operation for 3–5 concurrent employees.

## Technical Context

- **Platform**: Kotlin Multiplatform (Android + Desktop)
- **UI Framework**: Compose Multiplatform (shared composables where possible, platform-specific where needed)
- **Architecture**: MVVM + Clean Architecture
- **Local Database**: SQLDelight (multiplatform SQLite)
- **Cloud Database**: Supabase (PostgreSQL with Row-Level Security)
- **Authentication**: Supabase Auth
- **Real-time Sync**: Supabase Realtime
- **Dependency Injection**: Koin (multiplatform)
- **Serialization**: Kotlinx Serialization
- **Date/Time**: kotlinx-datetime
- **Validation**: konform
- **PDF Generation**: OpenPDF (JVM — shared between Android and Desktop)
- **Logging**: Kermit
- **Testing**: JUnit 5 + Kotest (property-based testing)
- **HTTP Client**: Ktor Client (multiplatform)
- **Transition Strategy**: The existing JavaFX project serves as the reference MVP. Core business logic (billing, inventory, categories, authentication) is re-implemented in Kotlin Multiplatform with equivalent data models and service layers. The SQLite schema maps to SQLDelight tables. Development occurs on a separate branch (`feature/sweet-lab-erp`).

## Glossary

- **Sweet_Lab_App**: The Kotlin Multiplatform (Android + Desktop) application built with Compose Multiplatform, serving as the primary user interface for all Sweet Lab ERP operations.
- **Auth_Service**: The Supabase Auth-backed service responsible for user authentication, session management, and role-based access control enforcement.
- **Inventory_Service**: The backend service managing raw material stock levels and finished goods quantities.
- **Recipe_Engine**: The component that defines and executes material-to-product transformation logic based on predefined recipes.
- **Sales_Service**: The backend service handling sales transactions, customer management, and invoice generation.
- **Wallet_Service**: The backend service managing financial wallets (Bank, Cash, Representative) and fund transfers between them.
- **Debt_Tracker**: The component that monitors customer payment obligations, calculates overdue durations, and flags late payments.
- **Sync_Engine**: The component responsible for data synchronization between the local SQLDelight database and Supabase PostgreSQL, using Supabase Realtime for live updates and a local queue for offline writes.
- **Report_Generator**: The component that produces financial summaries, inventory reports, debt aging reports, and printable invoices.
- **Raw_Material**: An ingredient used in confectionery production (e.g., milk, cream, cheese, sugar).
- **Finished_Good**: A completed product (e.g., a "Sweet Box") produced by combining raw materials according to a recipe.
- **Recipe**: A predefined formula specifying the quantities of raw materials required to produce one unit of a finished good.
- **Wallet**: A named financial account (Bank, Cash, or Representative Wallet) used to track fund balances and transfers.
- **Debt_Record**: A record tracking an unpaid customer balance, including the amount owed and the number of days overdue.
- **RBAC**: Role-Based Access Control — a method of restricting system access based on user roles (Admin, Chef, Representative).

## Requirements

### Requirement 1: User Authentication and Session Management

**User Story:** As any employee, I want to log in with my credentials and be directed to the interface matching my role, so that I can access only the features relevant to my job.

#### Acceptance Criteria

1. WHEN an employee submits valid credentials, THE Auth_Service SHALL authenticate the user and create a session with the corresponding role (Admin, Chef, or Representative).
2. WHEN an employee submits invalid credentials, THE Auth_Service SHALL reject the login attempt and display a descriptive error message without revealing which field is incorrect.
3. WHEN a session is created, THE Sweet_Lab_App SHALL redirect the user to the role-specific dashboard (Admin Dashboard, Chef Production Screen, or Representative Sales Screen).
4. WHEN a user session exceeds 8 hours of inactivity, THE Auth_Service SHALL expire the session and require re-authentication.
5. IF a user attempts to access a resource outside their role permissions, THEN THE Auth_Service SHALL deny the request and return an authorization error.

### Requirement 2: Role-Based Access Control

**User Story:** As a system admin, I want to manage user accounts and assign roles, so that each employee has appropriate access to system features.

#### Acceptance Criteria

1. THE Auth_Service SHALL enforce three distinct roles: Admin (full access), Chef (production-only access), and Representative (sales, purchases, expenses, and customer management access).
2. WHEN an Admin creates a new user account, THE Auth_Service SHALL require a username, password, full name, and role assignment.
3. WHEN an Admin updates a user role, THE Auth_Service SHALL apply the new permissions on the user's next login.
4. WHEN a Chef user logs in, THE Sweet_Lab_App SHALL display only the production interface with recipe execution and production logging capabilities.
5. WHEN a Representative user logs in, THE Sweet_Lab_App SHALL display the sales interface with access to sales logging, purchase recording, expense management, and customer management.
6. WHEN an Admin user logs in, THE Sweet_Lab_App SHALL display the full admin dashboard with access to financial reports, inventory management, user management, and all other modules.

### Requirement 3: Raw Material Inventory Management

**User Story:** As a system admin or representative, I want to track raw material stock levels in real time, so that I can ensure production has sufficient ingredients.

#### Acceptance Criteria

1. THE Inventory_Service SHALL maintain a current quantity for each raw material, updated in real time as purchases and production events occur.
2. WHEN a representative records a raw material purchase, THE Inventory_Service SHALL increase the corresponding raw material quantity by the purchased amount.
3. WHEN a production event consumes raw materials, THE Inventory_Service SHALL decrease each consumed raw material quantity by the amount specified in the recipe.
4. IF a raw material quantity falls below zero after a deduction attempt, THEN THE Inventory_Service SHALL reject the operation and return an insufficient stock error identifying the specific material and shortfall amount.
5. WHEN an admin views the inventory screen, THE Sweet_Lab_App SHALL display each raw material with its name, unit of measure, current quantity, and last-updated timestamp.

### Requirement 4: Recipe Management

**User Story:** As a system admin, I want to define and manage recipes that specify how raw materials combine into finished goods, so that production follows standardized formulas.

#### Acceptance Criteria

1. WHEN an admin creates a recipe, THE Recipe_Engine SHALL store the recipe with a name, the target finished good, and a list of 1 to 10 raw material ingredients each with a required quantity.
2. WHEN an admin updates a recipe, THE Recipe_Engine SHALL validate that all referenced raw materials exist in the Inventory_Service before saving.
3. THE Recipe_Engine SHALL enforce that each recipe produces exactly one type of finished good.
4. IF a recipe references a raw material that does not exist in inventory, THEN THE Recipe_Engine SHALL reject the recipe and identify the missing material.
5. WHEN an admin deletes a recipe, THE Recipe_Engine SHALL prevent deletion if the recipe has been used in any production log entry.

### Requirement 5: Production Execution (Chef Role)

**User Story:** As a chef, I want to execute a recipe to convert raw materials into finished goods, so that production is logged and inventory is updated automatically.

#### Acceptance Criteria

1. WHEN a chef selects a recipe and specifies a production quantity, THE Recipe_Engine SHALL calculate the total raw material requirements by multiplying each ingredient quantity by the production quantity.
2. WHEN a chef confirms a production run, THE Inventory_Service SHALL atomically deduct all required raw materials and increase the finished good quantity by the production quantity.
3. IF any required raw material has insufficient stock for the production run, THEN THE Recipe_Engine SHALL reject the entire production run and list all materials with insufficient quantities.
4. WHEN a production run completes, THE Sweet_Lab_App SHALL log the production event with the chef's identity, recipe used, production quantity, timestamp, and all material quantities consumed.
5. WHEN a chef views the production screen, THE Sweet_Lab_App SHALL display available recipes with current raw material availability status for each recipe.

### Requirement 6: Finished Goods Inventory

**User Story:** As a system admin or representative, I want to track finished goods stock levels, so that I can manage sales and know what is available.

#### Acceptance Criteria

1. THE Inventory_Service SHALL maintain a current quantity for each finished good, updated as production and sales events occur.
2. WHEN a production run completes, THE Inventory_Service SHALL increase the finished good quantity by the production quantity.
3. WHEN a sale is recorded, THE Inventory_Service SHALL decrease the sold finished good quantity by the sold amount.
4. IF a sale would reduce a finished good quantity below zero, THEN THE Inventory_Service SHALL reject the sale and return an insufficient stock error.
5. WHEN a user views the finished goods screen, THE Sweet_Lab_App SHALL display each finished good with its name, current quantity, unit price, and last-updated timestamp.

### Requirement 7: Customer Management

**User Story:** As a representative, I want to manage customer profiles with contact details and reliability ratings, so that I can track customer relationships and creditworthiness.

#### Acceptance Criteria

1. WHEN a representative creates a customer profile, THE Sales_Service SHALL store the customer name, city, mobile number, and an initial reliability rating of 0 stars.
2. WHEN a representative updates a customer's reliability rating, THE Sales_Service SHALL accept a value between 1 and 5 stars inclusive.
3. THE Sales_Service SHALL enforce that each customer mobile number is unique across all customer profiles.
4. WHEN a representative searches for a customer, THE Sales_Service SHALL support search by name, city, or mobile number and return matching results.
5. WHEN a representative views a customer profile, THE Sweet_Lab_App SHALL display the customer's name, city, mobile, reliability rating, total debt amount, and number of overdue days.

### Requirement 8: Sales Transaction Processing

**User Story:** As a representative, I want to record sales of finished goods to customers, so that inventory and financial records are updated accurately.

#### Acceptance Criteria

1. WHEN a representative creates a sale, THE Sales_Service SHALL record the customer, list of finished goods with quantities and unit prices, total amount, payment wallet, and timestamp.
2. WHEN a sale is recorded with full payment, THE Wallet_Service SHALL credit the specified wallet with the sale amount and THE Inventory_Service SHALL deduct the sold quantities.
3. WHEN a sale is recorded with partial or no payment, THE Debt_Tracker SHALL create a debt record for the unpaid balance linked to the customer.
4. THE Sales_Service SHALL calculate the sale total as the sum of each line item quantity multiplied by its unit price.
5. WHEN a sale is finalized, THE Sweet_Lab_App SHALL generate a printable receipt containing the customer name, itemized list, total, amount paid, remaining balance, and date.

### Requirement 9: Wallet Management and Fund Transfers

**User Story:** As a system admin or representative, I want to manage multiple financial wallets and transfer funds between them, so that cash flow is tracked accurately across accounts.

#### Acceptance Criteria

1. THE Wallet_Service SHALL maintain three wallet types: Bank, Cash, and Representative Wallet, each with a current balance.
2. WHEN a fund transfer is initiated between two wallets, THE Wallet_Service SHALL atomically debit the source wallet and credit the destination wallet by the transfer amount.
3. IF a transfer amount exceeds the source wallet balance, THEN THE Wallet_Service SHALL reject the transfer and return an insufficient funds error.
4. WHEN a representative records an expense, THE Wallet_Service SHALL debit the specified representative wallet by the expense amount.
5. WHEN a representative collects a customer payment, THE Wallet_Service SHALL credit the specified wallet and THE Debt_Tracker SHALL reduce the customer's outstanding debt by the payment amount.
6. WHEN an admin views the wallet summary, THE Sweet_Lab_App SHALL display each wallet with its name, current balance, and a transaction history list.

### Requirement 10: Debt Tracking and Aging

**User Story:** As a system admin, I want to see which customers have overdue payments and for how many days, so that I can take action to prevent debt accumulation.

#### Acceptance Criteria

1. THE Debt_Tracker SHALL calculate the number of overdue days for each unpaid debt record as the difference between the current date and the sale date.
2. WHEN an admin views the debt aging report, THE Report_Generator SHALL display each debtor with customer name, total owed, oldest unpaid invoice date, and number of overdue days, sorted by overdue days descending.
3. WHEN a customer payment is received, THE Debt_Tracker SHALL apply the payment to the oldest outstanding debt records first (FIFO).
4. WHEN a debt record is fully paid, THE Debt_Tracker SHALL mark the record as settled and remove it from the active debts list.
5. THE Debt_Tracker SHALL flag any debt record exceeding 30 days overdue as "critical" in the debt aging report.

### Requirement 11: Expense Management

**User Story:** As a representative, I want to record purchases and operating costs from my wallet, so that all expenditures are tracked and deducted from the correct account.

#### Acceptance Criteria

1. WHEN a representative records an expense, THE Sales_Service SHALL store the expense with a description, amount, category (purchase or operating cost), wallet source, and timestamp.
2. WHEN an expense is recorded, THE Wallet_Service SHALL debit the specified wallet by the expense amount.
3. IF the expense amount exceeds the wallet balance, THEN THE Wallet_Service SHALL reject the expense and return an insufficient funds error.
4. WHEN an admin views the expense report, THE Report_Generator SHALL display expenses grouped by category with subtotals and a grand total for the selected date range.

### Requirement 12: Reporting and Invoicing

**User Story:** As a system admin, I want to generate financial summaries, inventory reports, and printable invoices, so that I can monitor business performance and provide documentation to customers.

#### Acceptance Criteria

1. WHEN an admin requests a financial summary, THE Report_Generator SHALL calculate and display total sales revenue, total expenses, net profit, and wallet balances for the selected date range.
2. WHEN an admin requests an inventory report, THE Report_Generator SHALL display all raw materials and finished goods with current quantities, values, and low-stock alerts for items below a configurable threshold.
3. WHEN a user generates an invoice, THE Report_Generator SHALL produce a formatted document containing the business name, customer details, itemized products with quantities and prices, total amount, payment status, and date.
4. THE Report_Generator SHALL support exporting reports in PDF format.
5. WHEN real-time totals are displayed on any screen, THE Sweet_Lab_App SHALL recalculate totals within 1 second of any underlying data change.

### Requirement 13: Offline Mode and Data Synchronization

**User Story:** As any employee, I want the app to remain functional during poor connectivity, so that my work is not interrupted by network issues.

#### Acceptance Criteria

1. WHEN the device loses network connectivity, THE Sync_Engine SHALL switch to offline mode and store all data modifications in a local queue.
2. WHEN network connectivity is restored, THE Sync_Engine SHALL synchronize all queued modifications to the cloud database in the order they were created.
3. IF a synchronization conflict occurs (concurrent modification of the same record), THEN THE Sync_Engine SHALL apply a last-write-wins strategy and log the conflict for admin review.
4. WHILE in offline mode, THE Sweet_Lab_App SHALL display a visible offline indicator and allow the user to continue all read and write operations using locally cached data.
5. WHEN the app starts, THE Sync_Engine SHALL attempt to synchronize with the cloud database and cache the latest data locally.

### Requirement 14: Data Persistence and Serialization

**User Story:** As a developer, I want all application data to be reliably persisted and synchronized between the local cache and the cloud database, so that no data is lost.

#### Acceptance Criteria

1. WHEN storing data locally for offline use, THE Sync_Engine SHALL serialize application objects to JSON format.
2. WHEN reading locally cached data, THE Sync_Engine SHALL deserialize JSON data back into application objects, producing equivalent objects to the originals.
3. FOR ALL valid application objects, serializing to JSON then deserializing SHALL produce an object equivalent to the original (round-trip property).
4. THE Sync_Engine SHALL validate the schema of deserialized data before accepting it into the application state.
