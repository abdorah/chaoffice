# Implementation Plan: Inventory Management

## Overview

Implement the four custom use cases in the `inventory_management` crate: `import_products`, `export_products`, `check_stock_levels`, and `transfer_stock`. The implementation builds incrementally — shared types first, then parsing/serialization, then each handler, with property tests wired in alongside each component.

## Tasks

- [x] 1. Set up crate structure and shared types
  - [x] 1.1 Create the `crates/inventory_management/` crate with `Cargo.toml` (dependencies: `csv`, `serde`, `serde_json`, `thiserror`, `uuid`; dev-dependencies: `proptest`, `tempfile`)
    - Define the crate module structure: `lib.rs`, `types.rs`, `parse.rs`, `serialize.rs`, `resolve.rs`, `error.rs`, and handler modules
    - _Requirements: 7.1, 7.2, 7.3, 7.4_
  - [x] 1.2 Implement `ProductRecord` struct and error types in `types.rs` and `error.rs`
    - `ProductRecord` with serde Serialize/Deserialize derives
    - `ImportError`, `ExportError`, `TransferError`, `InventoryError` enums
    - `ProductStatus` parsing helper (string to enum and back)
    - _Requirements: 7.1, 7.5_

- [x] 2. Implement CSV and JSON parsing/serialization
  - [x] 2.1 Implement `parse_csv` and `parse_json` in `parse.rs`
    - `parse_csv`: use `csv::ReaderBuilder` with headers, deserialize into `Vec<ProductRecord>`
    - `parse_json`: use `serde_json::from_reader` to deserialize `Vec<ProductRecord>`
    - Return appropriate `ImportError` variants for file-not-found, malformed CSV, invalid JSON
    - _Requirements: 1.1, 1.2, 1.11, 1.12_
  - [x] 2.2 Implement `write_csv` and `write_json` in `serialize.rs`
    - `write_csv`: use `csv::WriterBuilder` with header row in specified column order
    - `write_json`: use `serde_json::to_writer_pretty` for array of objects
    - Return `ExportError` for non-writable paths
    - _Requirements: 2.1, 2.2, 7.1, 7.3_
  - [ ]* 2.3 Write property tests for CSV round-trip (parse then write then parse)
    - **Property 1: Import/Export round-trip (CSV)**
    - **Validates: Requirements 1.1, 1.4, 1.6, 1.8, 2.1, 3.1, 3.2, 3.3**
  - [ ]* 2.4 Write property tests for JSON round-trip (parse then write then parse)
    - **Property 2: Import/Export round-trip (JSON)**
    - **Validates: Requirements 1.2, 1.4, 1.6, 1.8, 2.2, 3.1, 3.2, 3.3**

- [x] 3. Implement relationship resolution
  - [x] 3.1 Implement `resolve_category`, `resolve_supplier`, `resolve_location` in `resolve.rs`
    - Query repositories by name, return `Option<u32>` or error for non-empty unresolved names
    - Supplier resolution checks `Person.role == Supplier`
    - _Requirements: 1.4, 1.5, 1.6, 1.7, 1.8, 1.9_

- [x] 4. Implement ImportProductsHandler
  - [x] 4.1 Implement the import handler in `handlers/import_products.rs`
    - Gate with `#[require_permission("import:*")]`
    - Parse file based on format, iterate records, validate required fields, parse status, resolve relationships
    - Create Product entities for valid rows, collect errors for invalid rows
    - Report progress as percentage of rows processed
    - Return `ImportProductsReturnDto`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10, 6.1, 6.2, 6.3_
  - [ ]* 4.2 Write property test for import count invariant
    - **Property 3: Import count invariant**
    - **Validates: Requirements 1.3**
  - [ ]* 4.3 Write property test for unknown reference causes skip
    - **Property 4: Unknown reference causes skip**
    - **Validates: Requirements 1.5, 1.7, 1.9**
  - [ ]* 4.4 Write property test for missing required fields causes skip
    - **Property 5: Missing required fields causes skip**
    - **Validates: Requirements 1.10**
  - [ ]* 4.5 Write property test for invalid status causes skip
    - **Property 6: Invalid status causes skip**
    - **Validates: Requirements 7.5**
  - [ ]* 4.6 Write unit tests for import edge cases
    - Test non-existent file path, malformed CSV, invalid JSON
    - _Requirements: 1.11, 1.12_

- [x] 5. Implement ExportProductsHandler
  - [x] 5.1 Implement the export handler in `handlers/export_products.rs`
    - Gate with `#[require_permission("export:*")]`
    - Load all Products with related Category, Location, Person names
    - Map to `ProductRecord` (empty string for missing relationships)
    - Write based on format, return `ExportProductsReturnDto`
    - _Requirements: 2.1, 2.2, 2.3, 2.4_
  - [ ]* 5.2 Write property test for export count matches product count
    - **Property 7: Export count matches product count**
    - **Validates: Requirements 2.3**
  - [ ]* 5.3 Write unit tests for export edge cases
    - Test non-writable path, products without relationships
    - _Requirements: 2.4, 2.5_

- [x] 6. Checkpoint - Ensure import/export tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Implement CheckStockLevelsHandler
  - [x] 7.1 Implement the stock check handler in `handlers/check_stock_levels.rs`
    - Gate with `#[require_permission("product:read")]`
    - Return empty arrays for threshold <= 0
    - Filter products where quantity < threshold
    - Build parallel arrays for StockAlertDto
    - _Requirements: 4.1, 4.2, 4.3, 4.4_
  - [ ]* 7.2 Write property test for stock check filter correctness
    - **Property 8: Stock check filter correctness**
    - **Validates: Requirements 4.1, 4.2**
  - [ ]* 7.3 Write unit tests for stock check edge cases
    - Test threshold 0, negative threshold, no products below threshold
    - _Requirements: 4.3, 4.4_

- [x] 8. Implement TransferStockHandler
  - [x] 8.1 Implement the transfer handler in `handlers/transfer_stock.rs`
    - Gate with `#[require_permission("stock:*")]`
    - Validate: quantity > 0, from != to, product exists, locations exist, sufficient stock
    - Snapshot state for Qleany undo
    - Update quantities, create StockMovement record
    - Return `TransferStockReturnDto`
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9_
  - [ ]* 8.2 Write property test for transfer quantity conservation
    - **Property 9: Transfer quantity conservation**
    - **Validates: Requirements 5.1, 5.2**
  - [ ]* 8.3 Write property test for transfer rejects insufficient stock
    - **Property 10: Transfer rejects insufficient stock**
    - **Validates: Requirements 5.3**
  - [ ]* 8.4 Write property test for transfer creates audit StockMovement
    - **Property 11: Transfer creates audit StockMovement**
    - **Validates: Requirements 5.8**
  - [ ]* 8.5 Write property test for transfer undo restores quantities
    - **Property 12: Transfer undo restores quantities**
    - **Validates: Requirements 5.9**
  - [ ]* 8.6 Write unit tests for transfer validation edge cases
    - Test zero/negative quantity, same location, non-existent product, non-existent location
    - _Requirements: 5.4, 5.5, 5.6, 5.7_

- [x] 9. Checkpoint - Ensure all handler tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. Wire handlers to Qleany controller and RBAC
  - [x] 10.1 Register all four handlers with the `InventoryManagementController`
    - Wire `import_products`, `export_products`, `check_stock_levels`, `transfer_stock` to the controller dispatch
    - Ensure `import_products` is registered as a `long_operation`
    - Ensure `transfer_stock` is registered as `undoable`
    - _Requirements: 1.13, 2.6, 4.5, 5.10, 6.1_
  - [ ]* 10.2 Write unit tests for RBAC enforcement on all four use cases
    - Verify Admin and Manager can import/export
    - Verify Operator and Viewer are rejected for import/export
    - Verify all roles can check stock levels
    - Verify Viewer is rejected for transfer_stock
    - _Requirements: 1.13, 2.6, 4.5, 5.10_

- [x] 11. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties using `proptest`
- Unit tests validate specific examples and edge cases
- The `csv` and `serde_json` crates handle the heavy lifting for parsing/serialization
- Qleany generates the CRUD repos and controller scaffolding; this plan covers the custom handler logic
