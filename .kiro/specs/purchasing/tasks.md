# Implementation Plan: Purchasing

## Overview

Implement the three use cases in the `purchasing` crate: `create_purchase_deal`, `get_deals_by_supplier`, and `get_active_deals`. The implementation builds incrementally — shared types and validation first, then the status lifecycle module, then each handler, with property tests alongside each component. Finally, wire the handlers to the Qleany controller and build the Slint DealsPage UI.

## Tasks

- [ ] 1. Set up crate structure and shared types
  - [ ] 1.1 Create the `crates/purchasing/` crate with `Cargo.toml` (dependencies: `chrono`, `thiserror`, `inventory_security`, `inventory_security_macros`; dev-dependencies: `proptest`)
    - Define the crate module structure: `lib.rs`, `error.rs`, `validate.rs`, `lifecycle.rs`, and handler modules
    - _Requirements: 1.1, 4.2_
  - [ ] 1.2 Implement `PurchasingError` enum in `error.rs`
    - All variants: `ProductNotFound`, `SupplierNotFound`, `ManagerNotFound`, `NotASupplier`, `NotAManager`, `EmptyTitle`, `NegativeUnitCost`, `NegativeTotalValue`, `InvalidDateRange`, `InvalidStatusTransition`, `Auth`, `Internal`
    - Derive `thiserror::Error` with display messages
    - _Requirements: 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10, 4.3, 4.4, 4.5_

- [ ] 2. Implement validation and lifecycle modules
  - [ ] 2.1 Implement `validate_deal_input`, `validate_supplier`, `validate_manager`, `validate_product` in `validate.rs`
    - `validate_deal_input`: check title non-empty, unit_cost >= 0, total_value >= 0, end_date >= start_date
    - `validate_supplier`: fetch Person by ID, check exists and role == Supplier
    - `validate_manager`: fetch Person by ID, check exists and role == Manager
    - `validate_product`: fetch Product by ID, check exists
    - _Requirements: 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10_
  - [ ] 2.2 Implement `can_transition` and `transition_status` in `lifecycle.rs`
    - Encode the transition matrix: Draft→{Active,Cancelled}, Active→{Completed,Cancelled}, Completed→{}, Cancelled→{}
    - Return `InvalidStatusTransition` error for disallowed transitions
    - _Requirements: 4.3, 4.4, 4.5_
  - [ ]* 2.3 Write property test for status transition correctness
    - **Property 8: Status transition correctness**
    - **Validates: Requirements 4.3, 4.4, 4.5**

- [ ] 3. Implement CreatePurchaseDealHandler
  - [ ] 3.1 Implement the create handler in `handlers/create_purchase_deal.rs`
    - Gate with `#[require_permission("deal:*")]`
    - Call all validation functions, snapshot state for undo, create Deal entity with status Draft
    - Return `CreatePurchaseDealReturnDto { deal_id }`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10, 1.11, 1.12, 1.13_
  - [ ]* 3.2 Write property test for create deal field persistence round-trip
    - **Property 1: Create deal field persistence round-trip**
    - **Validates: Requirements 1.1, 1.11, 4.1**
  - [ ]* 3.3 Write property test for Person role validation
    - **Property 2: Person role validation rejects wrong roles**
    - **Validates: Requirements 1.2, 1.3**
  - [ ]* 3.4 Write property test for negative financial fields rejected
    - **Property 3: Negative financial fields rejected**
    - **Validates: Requirements 1.9, 1.10**
  - [ ]* 3.5 Write property test for invalid date range rejected
    - **Property 4: Invalid date range rejected**
    - **Validates: Requirements 1.8**
  - [ ]* 3.6 Write property test for deal creation undo
    - **Property 5: Deal creation undo removes deal**
    - **Validates: Requirements 1.12**
  - [ ]* 3.7 Write unit tests for create deal edge cases
    - Test non-existent product_id, supplier_id, manager_id, empty title
    - _Requirements: 1.4, 1.5, 1.6, 1.7_

- [ ] 4. Checkpoint - Ensure create deal tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. Implement query handlers
  - [ ] 5.1 Implement `GetDealsBySupplierHandler` in `handlers/get_deals_by_supplier.rs`
    - Gate with `#[require_permission("deal:read")]`
    - Validate supplier_id exists, query deals by supplier relationship
    - Build parallel arrays: deal_ids, titles
    - Return `DealListDto`
    - _Requirements: 2.1, 2.2, 2.3, 2.4_
  - [ ] 5.2 Implement `GetActiveDealsHandler` in `handlers/get_active_deals.rs`
    - Gate with `#[require_permission("deal:read")]`
    - Query all deals with status Active
    - Build parallel arrays: deal_ids, titles, statuses
    - Return `ActiveDealsDto`
    - _Requirements: 3.1, 3.2, 3.3_
  - [ ]* 5.3 Write property test for get deals by supplier filter
    - **Property 6: Get deals by supplier returns only matching deals**
    - **Validates: Requirements 2.1, 2.2**
  - [ ]* 5.4 Write property test for get active deals filter
    - **Property 7: Get active deals returns only Active deals**
    - **Validates: Requirements 3.1, 3.2**
  - [ ]* 5.5 Write unit tests for query handler edge cases
    - Test non-existent supplier, no matching deals, no active deals
    - _Requirements: 2.2, 2.3, 3.2_

- [ ] 6. Checkpoint - Ensure all handler tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 7. Wire handlers to Qleany controller and RBAC
  - [ ] 7.1 Register all three handlers with the `PurchasingController`
    - Wire `create_purchase_deal`, `get_deals_by_supplier`, `get_active_deals` to the controller dispatch
    - Ensure `create_purchase_deal` is registered as `undoable`
    - _Requirements: 1.13, 2.4, 3.3_
  - [ ]* 7.2 Write unit tests for RBAC enforcement on all three use cases
    - Verify Admin and Manager can create deals
    - Verify Operator and Viewer are rejected for create_purchase_deal
    - Verify Admin, Manager, and Viewer can query deals
    - Verify Operator can query deals (has `*:read` via dashboard:read? — check permission matrix)
    - _Requirements: 1.13, 2.4, 3.3_

- [ ] 8. Implement DealsPage UI
  - [ ] 8.1 Create `ui/pages/deals_page.slint` with table view, search field, frequency/status filter dropdowns
    - Table columns: title, supplier_name, product_name, status, frequency, unit_cost, total_value, start_date, end_date
    - Search field filters by title or supplier_name (case-insensitive)
    - Frequency dropdown with DealFrequency values + "All" option
    - Status dropdown with DealStatus values + "All" option
    - _Requirements: 5.1, 5.2, 5.3, 5.4_
  - [ ] 8.2 Implement add-deal form in DealsPage
    - Form fields: title, description, product dropdown, supplier dropdown, manager dropdown, unit_cost, total_value, frequency, start_date, end_date
    - Populate supplier dropdown from Person entities with role Supplier
    - Populate product dropdown from Product entities
    - Visible only for Admin/Manager roles
    - _Requirements: 5.5, 5.6, 5.7_
  - [ ] 8.3 Wire DealsPage callbacks to Rust backend
    - Wire add-deal form submit to `create_purchase_deal` handler
    - Wire page load to fetch all deals and populate table
    - Wire search and filter inputs to client-side filtering logic
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_
  - [ ]* 8.4 Write property tests for deals search and filter logic
    - **Property 9: Deals search filter correctness**
    - **Validates: Requirements 5.2**
    - **Property 10: Deals frequency and status filter correctness**
    - **Validates: Requirements 5.3, 5.4**

- [ ] 9. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties using `proptest`
- Unit tests validate specific examples and edge cases
- Qleany generates the CRUD repos and controller scaffolding; this plan covers the custom handler logic
- The `lifecycle` module is kept separate from handlers so it can be unit-tested independently and reused if a future "update deal status" use case is added
