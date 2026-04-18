# Requirements Document

## Introduction

This document specifies the budget and finance tracking feature for the Inventory Management Application built with Rust, Qleany, and Slint UI. The feature enables recording financial entries (purchases, sales, expenses, forecasts), generating summaries with monthly breakdowns over a date range, and projecting future budgets based on recurring deals and historical trends. It covers three use cases: `record_budget_entry` (undoable), `get_budget_summary` (read-only), and `get_budget_projection` (read-only). The Slint UI BudgetPage provides KPI stat cards, a bar chart for monthly breakdowns, a line chart for projections, an entry table, and an add-entry form. All operations are gated by RBAC via `inventory_security_macros` proc macros.

## Glossary

- **BudgetEntry**: A financial record entity with entry_type (Purchase, Sale, Expense, Forecast), amount, description, entry_date, and relationships to a Product (optional), a Deal (optional), and a User (recorded_by).
- **BudgetEntryType**: An enum with values Purchase, Sale, Expense, Forecast classifying the nature of a financial entry.
- **Record_Entry_Handler**: The undoable use case handler responsible for validating input, resolving the authenticated user from the Security_Context, creating a BudgetEntry entity, and supporting undo via Qleany's snapshot mechanism.
- **Summary_Handler**: The read-only use case handler responsible for aggregating BudgetEntry entities within a date range into totals and monthly breakdowns.
- **Projection_Handler**: The read-only use case handler responsible for computing future budget projections based on active recurring Deal costs and historical BudgetEntry trends.
- **Budget_Page**: The Slint UI page that displays KPI stat cards, a bar chart (monthly breakdown), a line chart (projection), a trends tab, an entry table, and an add-entry form.
- **Deal**: A purchasing agreement entity with frequency, status, unit_cost, and total_value fields used by the Projection_Handler to calculate recurring costs.
- **DealFrequency**: An enum with values OneTime, Weekly, Monthly, Quarterly, Yearly specifying how often a Deal recurs.
- **Product**: An inventory item entity optionally referenced by a BudgetEntry.
- **User**: An application identity entity; the recorded_by relationship on BudgetEntry tracks who created the entry.
- **Security_Context**: The per-operation structure containing the authenticated user's identity, role, and permissions, used to resolve recorded_by and enforce RBAC.
- **RBAC_Engine**: The role-based access control enforcement layer from the `inventory_security_macros` crate that gates use case execution via proc macros.
- **BarData**: A Rust struct containing monthly labels and parallel arrays of purchase, sale, and expense values, passed to the Slint bar chart component.
- **Net_Balance**: A computed value defined as total_sales minus total_purchases minus total_expenses.

## Requirements

### Requirement 1: Record Budget Entry

**User Story:** As a manager, I want to record a financial entry (purchase, sale, expense, or forecast), so that I can track the organization's financial activity against products and deals.

#### Acceptance Criteria

1. WHEN a user provides a valid RecordBudgetEntryDto with entry_type, amount, description, entry_date, product_id, and deal_id, THE Record_Entry_Handler SHALL create a new BudgetEntry entity with the recorded_by field set to the authenticated user from the Security_Context and return the entry_id.
2. WHEN the product_id does not reference an existing Product, THE Record_Entry_Handler SHALL reject the creation and return an error indicating the Product was not found.
3. WHEN the deal_id does not reference an existing Deal, THE Record_Entry_Handler SHALL reject the creation and return an error indicating the Deal was not found.
4. WHEN the amount is negative, THE Record_Entry_Handler SHALL reject the creation and return a validation error indicating amount must be non-negative.
5. WHEN the description is empty, THE Record_Entry_Handler SHALL reject the creation and return a validation error indicating description is required.
6. WHEN a budget entry is successfully created, THE Record_Entry_Handler SHALL persist all provided fields (entry_type, amount, description, entry_date, product, deal, recorded_by) on the BudgetEntry entity.
7. WHEN a budget entry creation is undone via Qleany's undo mechanism, THE Record_Entry_Handler SHALL remove the created BudgetEntry entity from the database.
8. WHEN a non-authorized user attempts to record a budget entry, THE RBAC_Engine SHALL reject the operation with an AccessDenied error (requires `budget:*` permission — Admin, Manager only).
9. WHEN the product_id is zero, THE Record_Entry_Handler SHALL treat the product relationship as absent and create the BudgetEntry without a linked Product.
10. WHEN the deal_id is zero, THE Record_Entry_Handler SHALL treat the deal relationship as absent and create the BudgetEntry without a linked Deal.

### Requirement 2: Get Budget Summary

**User Story:** As a user with read access, I want to view a financial summary with monthly breakdowns for a date range, so that I can understand the organization's financial position over time.

#### Acceptance Criteria

1. WHEN a user provides a valid date range (from_date, to_date), THE Summary_Handler SHALL return a BudgetSummaryDto containing total_purchases, total_sales, total_expenses, net_balance, and parallel arrays of monthly_labels, monthly_purchases, monthly_sales, and monthly_expenses.
2. THE Summary_Handler SHALL compute net_balance as total_sales minus total_purchases minus total_expenses.
3. THE Summary_Handler SHALL group BudgetEntry entities by calendar month within the date range and compute per-month totals for purchases, sales, and expenses.
4. THE Summary_Handler SHALL produce monthly_labels in chronological order formatted as "YYYY-MM" strings.
5. WHEN the date range contains no BudgetEntry entities, THE Summary_Handler SHALL return zero for all totals and empty arrays for monthly breakdowns.
6. THE Summary_Handler SHALL include only BudgetEntry entities whose entry_date falls within the inclusive range [from_date, to_date].
7. THE Summary_Handler SHALL compute total_purchases as the sum of amounts for all entries with entry_type Purchase within the date range.
8. THE Summary_Handler SHALL compute total_sales as the sum of amounts for all entries with entry_type Sale within the date range.
9. THE Summary_Handler SHALL compute total_expenses as the sum of amounts for all entries with entry_type Expense within the date range.
10. WHEN from_date is later than to_date, THE Summary_Handler SHALL return a validation error indicating the date range is invalid.
11. WHEN a non-authorized user attempts to get a budget summary, THE RBAC_Engine SHALL reject the operation with an AccessDenied error (requires `budget:*` or `*:read` permission — Admin, Manager, Viewer).
12. THE Summary_Handler SHALL ensure that the sum of monthly_purchases equals total_purchases, the sum of monthly_sales equals total_sales, and the sum of monthly_expenses equals total_expenses.

### Requirement 3: Get Budget Projection

**User Story:** As a user with read access, I want to project future budgets based on recurring deals and historical trends, so that I can plan ahead financially.

#### Acceptance Criteria

1. WHEN a user provides months_ahead (a positive integer), THE Projection_Handler SHALL return a BudgetProjectionDto containing parallel arrays of month_labels, projected_income, projected_expenses, projected_balance, and recurring_deal_costs, each with exactly months_ahead elements.
2. THE Projection_Handler SHALL compute recurring_deal_costs for each projected month by summing the unit_cost of all active Deal entities (status Active) multiplied by their frequency factor per month (Weekly: ~4.33, Monthly: 1, Quarterly: 1/3, Yearly: 1/12, OneTime: 0 for projection purposes).
3. THE Projection_Handler SHALL compute projected_expenses for each month as the sum of recurring_deal_costs and the historical average monthly expense (from BudgetEntry entities with entry_type Expense).
4. THE Projection_Handler SHALL compute projected_income for each month as the historical average monthly income (from BudgetEntry entities with entry_type Sale).
5. THE Projection_Handler SHALL compute projected_balance for each month as projected_income minus projected_expenses for that month.
6. THE Projection_Handler SHALL produce month_labels as "YYYY-MM" strings starting from the month after the current date, in chronological order.
7. WHEN months_ahead is zero or negative, THE Projection_Handler SHALL return a validation error indicating months_ahead must be a positive integer.
8. WHEN no active recurring deals exist, THE Projection_Handler SHALL set recurring_deal_costs to zero for each projected month.
9. WHEN no historical BudgetEntry data exists, THE Projection_Handler SHALL use zero for historical averages.
10. WHEN a non-authorized user attempts to get a budget projection, THE RBAC_Engine SHALL reject the operation with an AccessDenied error (requires `budget:*` or `*:read` permission — Admin, Manager, Viewer).

### Requirement 4: Budget Entry Undo Support

**User Story:** As a manager, I want to undo a mistakenly recorded budget entry, so that I can correct errors without manual deletion.

#### Acceptance Criteria

1. WHEN a budget entry is created via the Record_Entry_Handler, THE Record_Entry_Handler SHALL snapshot the database state before the creation to enable undo.
2. WHEN the undo operation is invoked for a recorded budget entry, THE Record_Entry_Handler SHALL restore the database to the pre-creation snapshot, removing the entry.
3. WHEN the redo operation is invoked after an undo, THE Record_Entry_Handler SHALL re-apply the creation, restoring the BudgetEntry entity.

### Requirement 5: Budget Page UI

**User Story:** As a desktop user, I want a budget management page, so that I can view financial KPIs, monthly breakdowns, projections, and record new entries through the Slint UI.

#### Acceptance Criteria

1. WHEN a user navigates to the Budget_Page, THE Budget_Page SHALL display KPI stat cards showing total_purchases, total_sales, total_expenses, and net_balance for the selected date range.
2. WHEN a user navigates to the Budget_Page, THE Budget_Page SHALL display a bar chart visualizing monthly purchases, sales, and expenses using BarData computed in Rust.
3. WHEN a user navigates to the Budget_Page, THE Budget_Page SHALL display a line chart visualizing projected income, expenses, and balance using SVG path strings computed in Rust from projection data.
4. WHEN a user navigates to the Budget_Page, THE Budget_Page SHALL display an entry table listing all BudgetEntry entities within the selected date range, showing entry_type, amount, description, entry_date, product name, deal title, and recorded_by display name.
5. WHEN an authorized user (Admin or Manager) submits the add-entry form with valid data, THE Budget_Page SHALL call the Record_Entry_Handler and refresh the entry table and KPI cards to reflect the new entry.
6. WHEN a non-authorized user (Operator) views the Budget_Page, THE Budget_Page SHALL hide the add-entry form and display data in read-only mode.
7. WHEN a user changes the date range filter, THE Budget_Page SHALL re-fetch the budget summary and update the KPI cards, bar chart, and entry table accordingly.
8. WHEN a user changes the projection months-ahead slider, THE Budget_Page SHALL re-fetch the budget projection and update the line chart accordingly.

### Requirement 6: Bar Chart Data Computation

**User Story:** As a developer, I want bar chart data computed in Rust and passed to Slint, so that the UI renders monthly financial breakdowns accurately.

#### Acceptance Criteria

1. THE Record_Entry_Handler SHALL compute BarData from the BudgetSummaryDto by mapping monthly_labels to bar labels and monthly_purchases, monthly_sales, and monthly_expenses to parallel value arrays.
2. THE BarData arrays SHALL have equal lengths matching the number of months in the summary.
3. WHEN the summary contains no monthly data, THE BarData SHALL contain empty arrays.

### Requirement 7: Line Chart Data Computation

**User Story:** As a developer, I want line chart SVG path strings computed in Rust from projection data, so that the UI renders budget projections as smooth line charts.

#### Acceptance Criteria

1. THE Projection_Handler SHALL compute SVG path strings from the BudgetProjectionDto arrays (projected_income, projected_expenses, projected_balance) for rendering in the Slint line chart component.
2. THE SVG path strings SHALL scale data points to fit within the chart dimensions, using the maximum value across all three series as the vertical scale reference.
3. WHEN all projected values are zero, THE SVG path strings SHALL represent flat lines at the baseline.
4. THE SVG path computation SHALL produce one path string per data series (income, expenses, balance), each containing exactly months_ahead data points.
