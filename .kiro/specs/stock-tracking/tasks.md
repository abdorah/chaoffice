# Implementation Plan: Stock Tracking

## Overview

Implement the three use cases in the `stock_tracking` crate: `record_stock_movement`, `get_stock_history`, and `get_stock_summary`. The implementation builds incrementally — shared types and validation first, then each handler with property tests alongside, then Slint UI integration.

## Tasks

- [ ] 1. Set up crate structure and shared types
  - [ ] 1.1 Create the `crates/stock_tracking/` crate with `Cargo.toml` (dependencies: `chrono`, `thiserror`, `serde`; dev-dependencies: `proptest`)
    - Define the crate module structure: `lib.rs`, `error.rs`, `validation.rs`, `quantity.rs`, `running_total.rs`, `summary.rs`, and handler modules
    - _Requirements: 1.1–1.15, 2.1–2.6, 3.1–3.6_
  - [ ] 1.2 Implement error types in `error.rs`
    - `StockTrackingError` enum with all variants: ProductNotFound, LocationNotFound, InsufficientStock, ZeroQuantity, NegativeQuantity, MissingToLocation, MissingFromLocation, MissingTransferLocations, SameLocation, AdjustmentUnderflow, Auth, Internal
    - _Requirements: 1.10, 1.11, 1.12, 1.13_
  - [ ] 1.3 Implement `quantity.rs` with `compute_delta` and `apply_delta` functions
    - `compute_delta`: returns signed delta based on MovementType (+qty for Inbound/Return, -qty for Outbound, +/-qty for Adjustment)
    - `apply_delta`: adds delta to current quantity
    - _Requirements: 1.1, 1.2, 1.4, 1.5_
  - [ ] 1.4 Implement `validation.rs` with `validate_movement` function
    - Check zero quantity, movement-type-specific location requirements, sufficient stock for Outbound/Transfer, non-negative result for Adjustment
    - _Requirements: 1.7, 1.8, 1.9, 1.10, 1.13_

- [ ] 2. Implement RecordStockMovementHandler
  - [ ] 2.1 Implement the handler in `handlers/record_stock_movement.rs`
    - Gate with `#[require_permission("stock:*")]`
    - Load Product, validate locations exist, call `validate_movement`, compute delta, update Product quantity, create StockMovement entity with performed_by from SecurityContext
    - Return `RecordStockMovementResultDto { movement_id, new_product_quantity }`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10, 1.11, 1.12, 1.13, 1.14, 1.15_
  - [ ]* 2.2 Write property test: Quantity update per movement type
    - **Property 1: Quantity update per movement type**
    - **Validates: Requirements 1.1, 1.2, 1.4, 1.5, 1.6**
  - [ ]* 2.3 Write property test: Transfer quantity conservation
    - **Property 2: Transfer quantity conservation**
    - **Validates: Requirements 1.3, 1.6**
  - [ ]* 2.4 Write property test: Insufficient stock rejection
    - **Property 3: Insufficient stock rejection**
    - **Validates: Requirements 1.10**
  - [ ]* 2.5 Write property test: Audit trail records performing user
    - **Property 4: Audit trail records performing user**
    - **Validates: Requirements 1.14**
  - [ ]* 2.6 Write unit tests for movement validation edge cases
    - Test zero quantity, missing locations per type, same location for Transfer, non-existent product, non-existent location, Adjustment underflow
    - _Requirements: 1.7, 1.8, 1.9, 1.11, 1.12, 1.13_

- [ ] 3. Checkpoint - Ensure record movement tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 4. Implement GetStockHistoryHandler
  - [ ] 4.1 Implement `running_total.rs` with `compute_running_totals` function
    - Takes initial_quantity and chronologically ordered movements, returns Vec of running totals
    - _Requirements: 2.3_
  - [ ] 4.2 Implement the handler in `handlers/get_stock_history.rs`
    - Gate with `#[require_permission("stock:*", "*:read")]`
    - Validate product exists, query StockMovement entities within date range ordered by created_at ASC
    - Compute initial quantity by reconstructing from movements before from_date
    - Call `compute_running_totals`, build parallel arrays, return `StockHistoryDto`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_
  - [ ]* 4.3 Write property test: History chronological ordering and date filtering
    - **Property 5: History chronological ordering and date filtering**
    - **Validates: Requirements 2.1**
  - [ ]* 4.4 Write property test: Running totals correctness
    - **Property 6: Running totals correctness**
    - **Validates: Requirements 2.2, 2.3**
  - [ ]* 4.5 Write unit tests for history edge cases
    - Test non-existent product, empty date range, no movements in range
    - _Requirements: 2.4, 2.5_

- [ ] 5. Implement GetStockSummaryHandler
  - [ ] 5.1 Implement `summary.rs` with `aggregate_30d` function
    - Classify movements: Inbound/Return/Transfer-in → inbound_30d, Outbound/Transfer-out → outbound_30d
    - Sum quantities for movements within cutoff date
    - _Requirements: 3.2, 3.3, 3.5_
  - [ ] 5.2 Implement the handler in `handlers/get_stock_summary.rs`
    - Gate with `#[require_permission("stock:*", "*:read")]`
    - Load all Products, compute cutoff = now - 30 days, aggregate movements per product
    - Build parallel arrays, return `StockSummaryDto`
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_
  - [ ]* 5.3 Write property test: 30-day summary aggregation correctness
    - **Property 7: 30-day summary aggregation correctness**
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5**
  - [ ]* 5.4 Write unit tests for summary edge cases
    - Test products with no movements, Transfer counted in both directions
    - _Requirements: 3.4, 3.5_

- [ ] 6. Checkpoint - Ensure all handler tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 7. Wire handlers to Qleany controller and RBAC
  - [ ] 7.1 Register all three handlers with the `StockTrackingController`
    - Wire `record_stock_movement`, `get_stock_history`, `get_stock_summary` to the controller dispatch
    - Ensure `record_stock_movement` is registered as non-undoable
    - _Requirements: 1.15, 2.6, 3.6, 4.1_
  - [ ]* 7.2 Write unit tests for RBAC enforcement on all three use cases
    - Verify Admin, Manager, Operator can record movements; Viewer is rejected
    - Verify all roles can query history and summary
    - _Requirements: 1.15, 2.6, 3.6_

- [ ] 8. Implement Slint UI StockTrackingPage
  - [ ] 8.1 Create `ui/pages/stock_tracking_page.slint` with layout
    - Movement history table (type, quantity, date, from_location, to_location, user, note columns)
    - Record movement form (product selector, movement type dropdown, quantity input, from/to location selectors, note field, submit button)
    - 30-day KPI cards (total inbound, total outbound)
    - Stock trend line chart placeholder for selected product
    - _Requirements: 5.1, 5.2, 5.5_
  - [ ] 8.2 Implement `StockTrackingAdapter` bridging Slint callbacks to controller
    - `record-movement` callback → calls `record_stock_movement`, refreshes history on success, shows error on failure
    - `load-history` callback → calls `get_stock_history`, populates table and chart data
    - `load-summary` callback → calls `get_stock_summary`, populates KPI cards
    - _Requirements: 5.3, 5.4, 5.5_

- [ ] 9. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties using `proptest`
- Unit tests validate specific examples and edge cases
- StockMovement entities are non-undoable (immutable audit trail) per Requirement 4
- The `record_stock_movement` handler also serves as the recording mechanism when `inventory_management::transfer_stock` creates a Transfer movement
