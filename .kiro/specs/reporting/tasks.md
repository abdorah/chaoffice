# Implementation Plan: Reporting

## Overview

Implement the reporting feature as a `reporting` crate with four use case handlers sharing a common `ReportWriter` trait abstraction. Format writers (Excel, PDF, CSV) implement the trait. Collector functions query entities and build row vectors. A `generate_report` orchestrator wires collectors to writers with progress reporting. All handlers are gated by RBAC. The Slint UI ReportsPage provides the frontend.

## Tasks

- [x] 1. Set up reporting crate structure and shared types
  - [x] 1.1 Create `crates/reporting/` with `Cargo.toml` and `src/lib.rs`
    - Add dependencies: `rust_xlsxwriter`, `genpdf`, `csv`, `chrono`, `thiserror`, `serde`
    - Add dev-dependencies: `proptest`, `tempfile`
    - Define `ReportError` enum in `src/error.rs`
    - Define `ReportFormat`, `DealStatusFilter` enums in `src/types.rs`
    - Define `ReportRow` type alias and `ReportWriter` trait in `src/writer.rs`
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 2. Implement format writers
  - [x] 2.1 Implement `CsvReportWriter` in `src/csv_writer.rs`
    - Implement `ReportWriter` trait: `begin` writes header row, `write_row` writes data, `begin_section` writes separator + section headers, `finish` flushes
    - _Requirements: 5.3_
  - [x] 2.2 Implement `ExcelWriter` in `src/excel_writer.rs`
    - Implement `ReportWriter` trait using `rust_xlsxwriter`: bold header format, auto-column-width, `begin_section` creates new worksheet, `finish` saves workbook
    - _Requirements: 5.1_
  - [x] 2.3 Implement `PdfWriter` in `src/pdf_writer.rs`
    - Implement `ReportWriter` trait using `genpdf`: document with title, table-based layout, `begin_section` adds page break, `finish` renders to file
    - _Requirements: 5.2_
  - [x] 2.4 Implement `create_writer` factory function in `src/writer.rs`
    - Dispatch to the correct writer based on `ReportFormat`
    - Validate output path is writable before creating writer
    - _Requirements: 5.4_

- [x] 3. Implement report generation orchestrator
  - [x] 3.1 Implement `generate_report` function in `src/generate.rs`
    - Accept title, headers, rows, format, output_path, progress reporter
    - Create writer, call begin, iterate rows with progress percentage, call finish
    - Return (file_path, row_count)
    - _Requirements: 5.5, 6.1, 6.2, 7.1_
  - [x] 3.2 Implement `generate_report_with_appendix` function in `src/generate.rs`
    - Extend `generate_report` to call `begin_section` and write appendix rows after main data
    - Row count only counts main data rows (not appendix)
    - _Requirements: 3.3, 3.4, 7.1_

- [x] 4. Checkpoint - Ensure writer infrastructure compiles
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Implement data collectors
  - [x] 5.1 Implement `collect_inventory_rows` in `src/collectors.rs`
    - Query all Products with Category, Location, Person joins
    - Filter by include_zero_stock flag
    - Build ReportRow vectors with 9 columns (empty strings for absent relationships)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
  - [x] 5.2 Implement `collect_stock_movement_rows` in `src/collectors.rs`
    - Query StockMovements within [from_date, to_date], join Product, Location, User
    - Order by created_at ascending
    - Build ReportRow vectors with 9 columns
    - _Requirements: 2.1, 2.2, 2.3_
  - [x] 5.3 Implement `collect_budget_rows` and `collect_budget_projection_rows` in `src/collectors.rs`
    - Query BudgetEntries within [from_date, to_date], join Product, Deal, User
    - Build ReportRow vectors with 7 columns
    - Projection: compute 6-month projection rows with 5 columns using active deals and historical averages
    - _Requirements: 3.1, 3.2, 3.3_
  - [x] 5.4 Implement `collect_purchasing_rows` in `src/collectors.rs`
    - Query Deals filtered by DealStatusFilter, join Product, Person (supplier)
    - Build ReportRow vectors with 10 columns (empty strings for absent relationships)
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 6. Implement use case handlers
  - [x] 6.1 Implement `GenerateInventoryReportHandler` in `src/handlers/inventory_report.rs`
    - Add `#[require_permission("report:generate")]` RBAC gate
    - Call `collect_inventory_rows`, then `generate_report`
    - Return `GenerateReportReturnDto`
    - _Requirements: 1.1, 1.6_
  - [x] 6.2 Implement `GenerateStockMovementReportHandler` in `src/handlers/stock_movement_report.rs`
    - Add `#[require_permission("report:generate")]` RBAC gate
    - Validate from_date <= to_date
    - Call `collect_stock_movement_rows`, then `generate_report`
    - Return `GenerateStockReportReturnDto`
    - _Requirements: 2.1, 2.4, 2.6_
  - [x] 6.3 Implement `GenerateBudgetReportHandler` in `src/handlers/budget_report.rs`
    - Add `#[require_permission("report:generate")]` RBAC gate
    - Validate from_date <= to_date
    - Call `collect_budget_rows`; if include_projections, also call `collect_budget_projection_rows` and use `generate_report_with_appendix`; else use `generate_report`
    - Return `GenerateBudgetReportReturnDto`
    - _Requirements: 3.1, 3.3, 3.4, 3.5, 3.7_
  - [x] 6.4 Implement `GeneratePurchasingReportHandler` in `src/handlers/purchasing_report.rs`
    - Add `#[require_permission("report:generate")]` RBAC gate
    - Call `collect_purchasing_rows`, then `generate_report`
    - Return `GeneratePurchasingReportReturnDto`
    - _Requirements: 4.1, 4.7_

- [x] 7. Checkpoint - Ensure all handlers compile and wire together
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 8. Implement property tests for inventory report
  - [ ]* 8.1 Write property test for file existence and row count (inventory)
    - **Property 1: File existence and row count accuracy**
    - **Validates: Requirements 1.1, 5.5, 7.1, 7.2**
  - [ ]* 8.2 Write property test for zero-stock filter
    - **Property 2: Inventory zero-stock filter**
    - **Validates: Requirements 1.2, 1.3**
  - [ ]* 8.3 Write property test for inventory CSV round-trip
    - **Property 7: CSV round-trip for inventory report**
    - **Validates: Requirements 1.4, 1.5, 8.1**

- [ ] 9. Implement property tests for stock movement report
  - [ ]* 9.1 Write property test for date range filter and chronological ordering
    - **Property 3: Stock movement date range filter and chronological ordering**
    - **Validates: Requirements 2.1, 2.3**
  - [ ]* 9.2 Write property test for stock movement CSV round-trip
    - **Property 8: CSV round-trip for stock movement report**
    - **Validates: Requirements 2.2, 8.2**

- [ ] 10. Implement property tests for budget report
  - [ ]* 10.1 Write property test for budget date range filter
    - **Property 4: Budget date range filter**
    - **Validates: Requirements 3.1**
  - [ ]* 10.2 Write property test for projection appendix toggle
    - **Property 6: Budget projection appendix toggle**
    - **Validates: Requirements 3.3, 3.4**
  - [ ]* 10.3 Write property test for budget CSV round-trip
    - **Property 9: CSV round-trip for budget report**
    - **Validates: Requirements 3.2, 8.3**

- [ ] 11. Implement property tests for purchasing report
  - [ ]* 11.1 Write property test for status filter
    - **Property 5: Purchasing status filter**
    - **Validates: Requirements 4.1, 4.2, 4.3**
  - [ ]* 11.2 Write property test for purchasing CSV round-trip
    - **Property 10: CSV round-trip for purchasing report**
    - **Validates: Requirements 4.4, 4.5, 8.4**

- [ ] 12. Implement unit tests
  - [ ]* 12.1 Write unit tests for error conditions and edge cases
    - Invalid date range returns error (Requirements 2.4, 3.5)
    - Non-writable output path returns error (Requirement 5.4)
    - Empty result sets return row_count 0 (Requirements 2.5, 3.6, 4.6)
    - Excel file has valid .xlsx magic bytes (Requirement 5.1)
    - PDF file has valid %PDF header (Requirement 5.2)
    - CSV file has correct header row (Requirement 5.3)
  - [ ]* 12.2 Write unit tests for RBAC enforcement
    - Operator denied for all four report types (Requirements 1.6, 2.6, 3.7, 4.7)
    - Viewer allowed for all four report types (Requirements 1.6, 2.6, 3.7, 4.7)
    - Manager allowed for all four report types (Requirements 1.6, 2.6, 3.7, 4.7)

- [-] 13. Implement ReportsPage Slint UI
  - [-] 13.1 Create `ui/pages/reports_page.slint`
    - Four report sections: Inventory, Stock Movement, Budget, Purchasing
    - Each section: format ComboBox (Excel/PDF/CSV), report-specific filter controls, output path input, generate button, progress bar, status label
    - Inventory section: include_zero_stock checkbox
    - Stock Movement section: from_date, to_date date pickers
    - Budget section: from_date, to_date date pickers, include_projections checkbox
    - Purchasing section: status_filter ComboBox (All/Active Only/Completed Only)
    - Generate buttons hidden when user lacks report:generate permission
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_
  - [ ] 13.2 Wire ReportsPage to reporting controller in Rust bridge
    - Connect generate button callbacks to use case handlers via controller
    - Bind progress reporter to progress bar updates
    - Display success/error messages in status label
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [ ] 14. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties using `proptest`
- Unit tests validate specific examples and edge cases
- CSV round-trip properties are the primary correctness mechanism for report content accuracy
- All report handlers share the `ReportWriter` trait infrastructure, so format writers are implemented once and reused
