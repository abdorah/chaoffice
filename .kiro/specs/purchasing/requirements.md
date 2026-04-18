# Requirements Document

## Introduction

This document specifies the purchasing feature for the Inventory Management Application built with Rust, Qleany, and Slint UI. The purchasing feature manages buying agreements (deals) between the organization and its suppliers. A deal represents a recurring or one-time purchasing arrangement: "we buy product X from supplier Y, managed by person Z." The feature covers three use cases: creating purchase deals (with undo support), querying deals by supplier, and listing active deals. It also includes the Slint UI DealsPage for viewing, searching, filtering, and creating deals. All operations are gated by RBAC via `inventory_security_macros` proc macros.

## Glossary

- **Deal**: A purchasing agreement entity representing a buying arrangement with a supplier, containing title, description, financial fields (unit_cost, total_value), date range (start_date, end_date), frequency, status, and relationships to a Product, a supplier Person, and a manager Person.
- **DealFrequency**: An enum with values OneTime, Weekly, Monthly, Quarterly, Yearly specifying how often the purchasing arrangement recurs.
- **DealStatus**: An enum with values Draft, Active, Completed, Cancelled representing the lifecycle state of a deal.
- **Create_Deal_Handler**: The use case handler responsible for validating input, enforcing role constraints on supplier and manager Person entities, creating a Deal entity, and supporting undo via Qleany's snapshot mechanism.
- **Deals_By_Supplier_Handler**: The read-only use case handler responsible for querying all Deal entities associated with a given supplier ID and returning their IDs and titles.
- **Active_Deals_Handler**: The read-only use case handler responsible for querying all Deal entities with status Active and returning their IDs, titles, and statuses.
- **Deals_Page**: The Slint UI page that displays deals in a table view with search, add-deal form, and frequency/status filters.
- **Person**: A business contact entity with name and role (Manager or Supplier), used as supplier and manager references in deals.
- **Product**: An inventory item entity referenced by a deal to indicate which product is being purchased.
- **RBAC_Engine**: The role-based access control enforcement layer from the `inventory_security_macros` crate that gates use case execution via `#[require_permission]` and `#[require_role]`.

## Requirements

### Requirement 1: Create Purchase Deal

**User Story:** As a manager, I want to create a new purchasing deal, so that I can formalize a buying agreement with a supplier for a specific product.

#### Acceptance Criteria

1. WHEN a user provides a valid CreatePurchaseDealDto with title, description, product_id, supplier_id, manager_id, unit_cost, total_value, frequency, start_date, and end_date, THE Create_Deal_Handler SHALL create a new Deal entity with status set to Draft and return the deal_id.
2. WHEN the supplier_id references a Person whose role is not Supplier, THE Create_Deal_Handler SHALL reject the creation and return an error indicating the referenced Person is not a Supplier.
3. WHEN the manager_id references a Person whose role is not Manager, THE Create_Deal_Handler SHALL reject the creation and return an error indicating the referenced Person is not a Manager.
4. WHEN the product_id does not reference an existing Product, THE Create_Deal_Handler SHALL reject the creation and return an error indicating the Product was not found.
5. WHEN the supplier_id does not reference an existing Person, THE Create_Deal_Handler SHALL reject the creation and return an error indicating the supplier Person was not found.
6. WHEN the manager_id does not reference an existing Person, THE Create_Deal_Handler SHALL reject the creation and return an error indicating the manager Person was not found.
7. WHEN the title is empty, THE Create_Deal_Handler SHALL reject the creation and return a validation error indicating the title is required.
8. WHEN the end_date is earlier than the start_date, THE Create_Deal_Handler SHALL reject the creation and return a validation error indicating the date range is invalid.
9. WHEN the unit_cost is negative, THE Create_Deal_Handler SHALL reject the creation and return a validation error indicating unit_cost must be non-negative.
10. WHEN the total_value is negative, THE Create_Deal_Handler SHALL reject the creation and return a validation error indicating total_value must be non-negative.
11. WHEN a deal is successfully created, THE Create_Deal_Handler SHALL persist all provided fields (title, description, product, supplier, manager, unit_cost, total_value, frequency, start_date, end_date) on the Deal entity.
12. WHEN a deal creation is undone via Qleany's undo mechanism, THE Create_Deal_Handler SHALL remove the created Deal entity from the database.
13. WHEN a non-authorized user attempts to create a deal, THE RBAC_Engine SHALL reject the operation with an AccessDenied error (requires `deal:*` permission).

### Requirement 2: Get Deals by Supplier

**User Story:** As a user, I want to list all deals for a specific supplier, so that I can review the purchasing agreements associated with that supplier.

#### Acceptance Criteria

1. WHEN a user provides a valid supplier_id, THE Deals_By_Supplier_Handler SHALL return a DealListDto containing deal_ids and titles for all Deal entities whose supplier relationship matches the given supplier_id.
2. WHEN the supplier_id does not match any Deal entities, THE Deals_By_Supplier_Handler SHALL return a DealListDto with empty arrays.
3. WHEN the supplier_id does not reference an existing Person, THE Deals_By_Supplier_Handler SHALL return an error indicating the supplier was not found.
4. WHEN a non-authorized user attempts to query deals by supplier, THE RBAC_Engine SHALL reject the operation with an AccessDenied error (requires `deal:*` or `*:read` permission).

### Requirement 3: Get Active Deals

**User Story:** As a user, I want to list all currently active deals, so that I can see which purchasing agreements are in effect.

#### Acceptance Criteria

1. THE Active_Deals_Handler SHALL return an ActiveDealsDto containing deal_ids, titles, and statuses for all Deal entities whose status is Active.
2. WHEN no Deal entities have status Active, THE Active_Deals_Handler SHALL return an ActiveDealsDto with empty arrays.
3. WHEN a non-authorized user attempts to query active deals, THE RBAC_Engine SHALL reject the operation with an AccessDenied error (requires `deal:*` or `*:read` permission).

### Requirement 4: Deal Status Lifecycle

**User Story:** As a manager, I want deals to follow a defined lifecycle, so that the status of each deal accurately reflects its current state.

#### Acceptance Criteria

1. WHEN a new deal is created, THE Create_Deal_Handler SHALL set the initial status to Draft.
2. THE Deal entity SHALL restrict status values to Draft, Active, Completed, and Cancelled.
3. WHILE a Deal has status Draft, THE Deal entity SHALL allow transition to Active or Cancelled.
4. WHILE a Deal has status Active, THE Deal entity SHALL allow transition to Completed or Cancelled.
5. WHILE a Deal has status Completed or Cancelled, THE Deal entity SHALL prevent any further status transitions.

### Requirement 5: Deals Page UI

**User Story:** As a desktop user, I want a deals management page, so that I can view, search, filter, and create purchasing deals through the Slint UI.

#### Acceptance Criteria

1. WHEN a user navigates to the Deals_Page, THE Deals_Page SHALL display a table of all deals showing title, supplier name, product name, status, frequency, unit_cost, total_value, start_date, and end_date.
2. WHEN a user types in the search field, THE Deals_Page SHALL filter the displayed deals to those whose title or supplier name contains the search text (case-insensitive).
3. WHEN a user selects a frequency filter, THE Deals_Page SHALL display only deals matching the selected DealFrequency value.
4. WHEN a user selects a status filter, THE Deals_Page SHALL display only deals matching the selected DealStatus value.
5. WHEN an authorized user submits the add-deal form with valid data, THE Deals_Page SHALL call the Create_Deal_Handler and refresh the deals table to include the new deal.
6. WHEN a non-authorized user views the Deals_Page, THE Deals_Page SHALL hide the add-deal form and display deals in read-only mode.
7. WHEN the Deals_Page loads, THE Deals_Page SHALL populate supplier and product dropdowns from existing Person (role Supplier) and Product entities.
