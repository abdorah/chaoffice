# Requirements Document

## Introduction

This document specifies the stock tracking feature for the Inventory Management Application built with Rust, Qleany, and Slint UI. Stock tracking provides the audit trail for all inventory changes by recording every stock movement (Inbound, Outbound, Transfer, Adjustment, Return), querying movement history with running totals, and generating 30-day summaries for dashboard KPIs. The feature comprises three use cases in the `stock_tracking` feature group: `record_stock_movement`, `get_stock_history`, and `get_stock_summary`. Stock movements are immutable (not undoable) to preserve audit integrity.

## Glossary

- **Movement_Recorder**: The use case handler responsible for recording a StockMovement entity and updating the associated Product quantity.
- **History_Query**: The use case handler responsible for retrieving chronological movement history for a product within a date range, including computed running totals.
- **Summary_Query**: The use case handler responsible for computing 30-day inbound and outbound aggregates across all products for dashboard KPI display.
- **StockMovement**: An audit entity recording a stock change with movement_type (Inbound/Outbound/Transfer/Adjustment/Return), quantity, note, product, from_location, to_location, and performed_by user.
- **Product**: An inventory item entity with name, reference, description, quantity, price_unit, status, and relationships to Category, Person (supplier), and Location.
- **Location**: A storage site entity with name, address, latitude, longitude, and capacity.
- **User**: An application identity entity with username, password_hash, display_name, role (Admin/Manager/Operator/Viewer), and is_active flag.
- **MovementType**: An enum with values Inbound, Outbound, Transfer, Adjustment, and Return classifying the nature of a stock change.
- **Running_Total**: A cumulative product quantity computed by applying each movement's signed quantity change in chronological order to an initial quantity.
- **RBAC_Engine**: The role-based access control enforcement layer from the `inventory_security_macros` crate that gates use case execution via `#[require_permission]` and `#[require_any_role]`.
- **StockTrackingPage**: The Slint UI page displaying movement history table, stock trend line chart, record movement form, and 30-day KPI cards.

## Requirements

### Requirement 1: Record Stock Movement

**User Story:** As an operator, I want to record a stock movement so that every inventory change is tracked with an immutable audit trail.

#### Acceptance Criteria

1. WHEN a user submits a valid RecordStockMovementDto with movement_type Inbound and a positive quantity, THE Movement_Recorder SHALL create a StockMovement entity and increase the associated Product quantity by the specified amount.
2. WHEN a user submits a valid RecordStockMovementDto with movement_type Outbound and a positive quantity, THE Movement_Recorder SHALL create a StockMovement entity and decrease the associated Product quantity by the specified amount.
3. WHEN a user submits a valid RecordStockMovementDto with movement_type Transfer and a positive quantity, THE Movement_Recorder SHALL create a StockMovement entity, decrease the Product quantity at the from_location, and increase the Product quantity at the to_location by the specified amount.
4. WHEN a user submits a valid RecordStockMovementDto with movement_type Adjustment, THE Movement_Recorder SHALL create a StockMovement entity and adjust the Product quantity by the specified amount (positive increases, negative decreases).
5. WHEN a user submits a valid RecordStockMovementDto with movement_type Return and a positive quantity, THE Movement_Recorder SHALL create a StockMovement entity and increase the associated Product quantity by the specified amount.
6. WHEN a stock movement is successfully recorded, THE Movement_Recorder SHALL return a RecordStockMovementResultDto containing the movement_id and the new_product_quantity.
7. WHEN a user submits a RecordStockMovementDto with movement_type Inbound, THE Movement_Recorder SHALL require a valid to_location_id.
8. WHEN a user submits a RecordStockMovementDto with movement_type Outbound, THE Movement_Recorder SHALL require a valid from_location_id.
9. WHEN a user submits a RecordStockMovementDto with movement_type Transfer, THE Movement_Recorder SHALL require both a valid from_location_id and a valid to_location_id, and the two location IDs must differ.
10. WHEN a user submits a RecordStockMovementDto with movement_type Outbound and the requested quantity exceeds the current Product quantity, THE Movement_Recorder SHALL reject the movement with an InsufficientStock error and leave the Product quantity unchanged.
11. WHEN a user submits a RecordStockMovementDto with a product_id that does not reference an existing Product, THE Movement_Recorder SHALL return a ProductNotFound error.
12. WHEN a user submits a RecordStockMovementDto with a from_location_id or to_location_id that does not reference an existing Location, THE Movement_Recorder SHALL return a LocationNotFound error.
13. WHEN a user submits a RecordStockMovementDto with quantity of zero, THE Movement_Recorder SHALL reject the movement with a validation error.
14. THE Movement_Recorder SHALL record the performing user (from SecurityContext) in the StockMovement performed_by field for audit purposes.
15. WHEN a non-authorized user attempts to record a stock movement, THE RBAC_Engine SHALL reject the operation with an AccessDenied error (requires `stock:*` permission — Admin, Manager, or Operator).

### Requirement 2: Query Stock History

**User Story:** As any authorized user, I want to view the movement history for a product within a date range so that I can audit inventory changes and see running totals.

#### Acceptance Criteria

1. WHEN a user provides a valid product_id, from_date, and to_date, THE History_Query SHALL return all StockMovement entities for that product within the date range (inclusive), ordered chronologically by created_at.
2. WHEN the History_Query returns results, THE History_Query SHALL provide parallel arrays of movement_ids, movement_types, quantities, dates, and running_totals, where each index corresponds to the same StockMovement.
3. THE History_Query SHALL compute running_totals by starting from the Product quantity as of the from_date and cumulatively applying each movement's signed quantity change in chronological order.
4. WHEN no StockMovement entities exist for the given product and date range, THE History_Query SHALL return empty arrays.
5. WHEN the product_id does not reference an existing Product, THE History_Query SHALL return a ProductNotFound error.
6. WHEN a non-authorized user attempts to query stock history, THE RBAC_Engine SHALL reject the operation with an AccessDenied error (requires `stock:*` or `*:read` permission — all roles).

### Requirement 3: Get Stock Summary (30-Day KPIs)

**User Story:** As any authorized user, I want to see a 30-day summary of inbound and outbound quantities for all products so that I can monitor inventory trends on the dashboard.

#### Acceptance Criteria

1. THE Summary_Query SHALL return parallel arrays of product_ids, product_names, current_quantities, inbound_30d, and outbound_30d for all Product entities.
2. THE Summary_Query SHALL compute inbound_30d as the sum of quantities from StockMovement entities with movement_type Inbound or Return created within the last 30 days for each product.
3. THE Summary_Query SHALL compute outbound_30d as the sum of quantities from StockMovement entities with movement_type Outbound created within the last 30 days for each product.
4. WHEN a product has no movements in the last 30 days, THE Summary_Query SHALL return zero for both inbound_30d and outbound_30d for that product.
5. THE Summary_Query SHALL include Transfer movements in the outbound_30d count for the source product and in the inbound_30d count for the destination product.
6. WHEN a non-authorized user attempts to get the stock summary, THE RBAC_Engine SHALL reject the operation with an AccessDenied error (requires `stock:*` or `*:read` permission — all roles).

### Requirement 4: Movement Immutability

**User Story:** As an auditor, I want stock movements to be immutable so that the audit trail cannot be tampered with.

#### Acceptance Criteria

1. THE Movement_Recorder SHALL create StockMovement entities as non-undoable records (matching the Qleany manifest `undoable: false` setting).
2. THE StockMovement entity SHALL preserve its created_at timestamp, movement_type, quantity, product, from_location, to_location, performed_by, and note fields without modification after creation.

### Requirement 5: Slint UI — Stock Tracking Page

**User Story:** As a user, I want a dedicated stock tracking page in the Slint UI so that I can view movement history, record new movements, and see 30-day KPI summaries.

#### Acceptance Criteria

1. WHEN a user navigates to the StockTrackingPage, THE StockTrackingPage SHALL display a movement history table showing movement type, quantity, date, locations, performing user, and note for each movement.
2. WHEN a user navigates to the StockTrackingPage, THE StockTrackingPage SHALL display 30-day KPI cards showing total inbound and total outbound quantities.
3. WHEN a user fills in the record movement form and submits, THE StockTrackingPage SHALL invoke the record_stock_movement use case and refresh the movement history table upon success.
4. IF the record movement use case returns an error, THEN THE StockTrackingPage SHALL display the error message to the user without clearing the form.
5. WHEN a user selects a product in the movement history table, THE StockTrackingPage SHALL display a stock trend line chart showing quantity over time based on running totals.
