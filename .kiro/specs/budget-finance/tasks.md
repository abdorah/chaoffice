# Implementation Plan: Budget & Finance

## Overview

Implement the `budget_finance` feature crate with three use case handlers (record_budget_entry, get_budget_summary, get_budget_projection), supporting modules (validate, aggregator, projector, chart), error types, and the Slint BudgetPage UI. All handlers are RBAC-gated. The record handler supports undo via Qleany snapshots. Chart data (BarData and SVG paths) is computed in Rust and passed to Slint.

## Tasks

- [x] 1. Set up budget_finance crate structure and core types
  - [x] 1.1 Create `crates/budget_finance/` crate with `Cargo.toml` and `src/lib.rs`
    - Add dependencies: `chrono`, `thiserror`, references to `inventory_security`, `inventory_security_macros`, and Qleany-generated entity crates
    - Define module declarations: `validate`, `aggregator`, `projector`, `chart`, `error`, `handlers`
    - _Requirements: 1.1, 2.1, 3.1_
  - [x] 1.2 Implement `BudgetError` enum in `src/error.rs`
    - Define variants: `ProductNotFound`, `DealNotFound`, `NegativeAmount`, `EmptyDescription`, `InvalidDateRange`, `InvalidMonthsAhead`, `Auth`, `Internal`
    - Implement `From<AuthError>` for `BudgetError`
    - _Requirements: 1.2, 1.3, 1.4, 1.5, 2.10, 3.7_
  - [x] 1.3 Define DTOs in `src/dto.rs` (or use Qleany-generated DTOs)
    - `RecordBudgetEntryDto`, `GetBudgetSummaryDto`, `GetBudgetProjectionDto`
    - `RecordBudgetEntryResultDto`, `BudgetSummaryDto`, `BudgetProjectionDto`
    - `BudgetEntryTypeInput` enum
    - _Requirements: 1.1, 2.1, 3.1_

- [x] 2. Implement validation module
  - [x] 2.1 Implement `validate_entry_input` in `src/validate.rs`
    - Reject negative amount (`BudgetError::NegativeAmount`)
    - Reject empty or whitespace-only description (`BudgetError::EmptyDescription`)
    - _Requirements: 1.4, 1.5_
  - [x] 2.2 Implement `validate_product` and `validate_deal` in `src/validate.rs`
    - If id == 0, return `Ok(None)` (absent relationship)
    - If id != 0, look up entity; return `ProductNotFound` / `DealNotFound` if missing
    - _Requirements: 1.2, 1.3, 1.9, 1.10_
  - [ ]* 2.3 Write property test for invalid entry input rejection
    - **Property 2: Invalid entry input rejection**
    - **Validates: Requirements 1.4, 1.5**

- [x] 3. Implement RecordBudgetEntryHandler
  - [x] 3.1 Implement handler in `src/handlers/record_budget_entry.rs`
    - Gate with `#[require_permission("budget:*")]`
    - Call `validate_entry_input`, `validate_product`, `validate_deal`
    - Snapshot state for undo support
    - Create BudgetEntry with `recorded_by = security_context.user_id`
    - Return `RecordBudgetEntryResultDto { entry_id }`
    - _Requirements: 1.1, 1.6, 1.7, 1.8, 1.9, 1.10, 4.1_
  - [ ]* 3.2 Write property test for record entry field persistence round-trip
    - **Property 1: Record entry field persistence round-trip**
    - **Validates: Requirements 1.1, 1.6**
  - [ ]* 3.3 Write property test for entry creation undo
    - **Property 3: Entry creation undo removes entry**
    - **Validates: Requirements 1.7, 4.2**
  - [ ]* 3.4 Write property test for undo then redo
    - **Property 4: Undo then redo restores entry**
    - **Validates: Requirements 4.3**
  - [ ]* 3.5 Write unit tests for record entry edge cases
    - Test non-existent product_id returns ProductNotFound (Req 1.2)
    - Test non-existent deal_id returns DealNotFound (Req 1.3)
    - Test product_id=0 creates entry without product link (Req 1.9)
    - Test deal_id=0 creates entry without deal link (Req 1.10)
    - Test RBAC rejects Operator role (Req 1.8)
    - _Requirements: 1.2, 1.3, 1.8, 1.9, 1.10_

- [x] 4. Checkpoint - Ensure record entry tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Implement aggregator module
  - [x] 5.1 Implement `aggregate_entries` in `src/aggregator.rs`
    - Filter entries by date range [from_date, to_date]
    - Group by calendar month (year-month)
    - Compute per-month totals for Purchase, Sale, Expense entry types
    - Compute overall totals and net_balance (sales - purchases - expenses)
    - Produce monthly_labels in chronological "YYYY-MM" order
    - Exclude Forecast entries from totals
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.6, 2.7, 2.8, 2.9, 2.12_
  - [ ]* 5.2 Write property test for net balance invariant
    - **Property 5: Net balance invariant**
    - **Validates: Requirements 2.2**
  - [ ]* 5.3 Write property test for monthly breakdowns sum to totals
    - **Property 6: Monthly breakdowns sum to totals**
    - **Validates: Requirements 2.12**
  - [ ]* 5.4 Write property test for aggregation date filtering and monthly grouping
    - **Property 7: Aggregation date filtering and monthly grouping**
    - **Validates: Requirements 2.3, 2.4, 2.6**

- [x] 6. Implement GetBudgetSummaryHandler
  - [x] 6.1 Implement handler in `src/handlers/get_budget_summary.rs`
    - Gate with `#[require_permission("budget:read")]`
    - Validate from_date <= to_date
    - Query BudgetEntry entities in date range
    - Call `aggregator::aggregate_entries`
    - Map `AggregatedSummary` to `BudgetSummaryDto`
    - _Requirements: 2.1, 2.10, 2.11_
  - [ ]* 6.2 Write unit tests for summary edge cases
    - Test empty date range returns zeros and empty arrays (Req 2.5)
    - Test from_date > to_date returns InvalidDateRange (Req 2.10)
    - Test RBAC allows Viewer, rejects Operator (Req 2.11)
    - _Requirements: 2.5, 2.10, 2.11_

- [x] 7. Implement projector module
  - [x] 7.1 Implement `frequency_to_monthly_factor` in `src/projector.rs`
    - OneTime: 0.0, Weekly: 52.0/12.0, Monthly: 1.0, Quarterly: 1.0/3.0, Yearly: 1.0/12.0
    - _Requirements: 3.2_
  - [x] 7.2 Implement `compute_recurring_costs`, `compute_historical_averages`, and `project_budget` in `src/projector.rs`
    - `compute_recurring_costs`: sum of (unit_cost × monthly_factor) for active deals
    - `compute_historical_averages`: average monthly income (Sale) and expense (Expense) from all entries
    - `project_budget`: build Projection with month_labels starting from next month, projected arrays using recurring costs + historical averages
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_
  - [ ]* 7.3 Write property test for projection output structure
    - **Property 8: Projection output structure**
    - **Validates: Requirements 3.1, 3.6**
  - [ ]* 7.4 Write property test for recurring deal cost computation
    - **Property 9: Recurring deal cost computation**
    - **Validates: Requirements 3.2**
  - [ ]* 7.5 Write property test for projection formula correctness
    - **Property 10: Projection formula correctness**
    - **Validates: Requirements 3.3, 3.4, 3.5**

- [x] 8. Implement GetBudgetProjectionHandler
  - [x] 8.1 Implement handler in `src/handlers/get_budget_projection.rs`
    - Gate with `#[require_permission("budget:read")]`
    - Validate months_ahead > 0
    - Query active deals and all budget entries
    - Call `projector::project_budget`
    - Map `Projection` to `BudgetProjectionDto`
    - _Requirements: 3.1, 3.7, 3.10_
  - [ ]* 8.2 Write unit tests for projection edge cases
    - Test months_ahead=0 returns InvalidMonthsAhead (Req 3.7)
    - Test no active deals sets recurring_deal_costs to zero (Req 3.8)
    - Test no historical data uses zero averages (Req 3.9)
    - Test RBAC allows Viewer, rejects Operator (Req 3.10)
    - _Requirements: 3.7, 3.8, 3.9, 3.10_

- [x] 9. Checkpoint - Ensure all handler and module tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. Implement chart module
  - [x] 10.1 Implement `BarData` struct and `build_bar_data` in `src/chart.rs`
    - Map AggregatedSummary monthly arrays to BarData with equal-length arrays
    - Handle empty summary (produce empty arrays)
    - _Requirements: 6.1, 6.2, 6.3_
  - [x] 10.2 Implement `build_line_chart_paths` in `src/chart.rs`
    - Generate SVG "M x0,y0 L x1,y1 ..." path strings from Projection arrays
    - Scale y-coordinates using max value across all three series
    - Handle all-zero case (flat baseline lines)
    - Produce exactly months_ahead data points per path
    - _Requirements: 7.1, 7.2, 7.3, 7.4_
  - [ ]* 10.3 Write property test for BarData mapping
    - **Property 11: BarData mapping from summary**
    - **Validates: Requirements 6.1, 6.2**
  - [ ]* 10.4 Write property test for SVG path computation
    - **Property 12: SVG path computation**
    - **Validates: Requirements 7.1, 7.2, 7.4**
  - [ ]* 10.5 Write unit tests for chart edge cases
    - Test BarData with empty summary produces empty arrays (Req 6.3)
    - Test SVG paths with all-zero projection produce flat baseline (Req 7.3)
    - _Requirements: 6.3, 7.3_

- [x] 11. Implement BudgetPage Slint UI
  - [x] 11.1 Create `ui/pages/budget_page.slint` with layout structure
    - KPI stat cards row: total_purchases, total_sales, total_expenses, net_balance
    - Bar chart component bound to BarData model
    - Line chart component bound to SVG path strings
    - Entry table with columns: entry_type, amount, description, entry_date, product_name, deal_title, recorded_by
    - Date range filter (from_date, to_date pickers)
    - Projection months-ahead slider (3-12)
    - Add entry form (visible for Admin/Manager only)
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.6, 5.7, 5.8_
  - [x] 11.2 Wire BudgetPage to Rust backend via BudgetFinanceController
    - Bind date range changes to re-fetch summary → update KPIs, bar chart, entry table
    - Bind months-ahead slider to re-fetch projection → update line chart
    - Bind add-entry form submit to RecordBudgetEntryHandler → refresh table and KPIs
    - Gate add-entry form visibility on user role (Admin/Manager)
    - _Requirements: 5.5, 5.7, 5.8_

- [x] 12. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties using `proptest`
- Unit tests validate specific examples and edge cases
- Qleany-generated CRUD infrastructure is assumed to exist for BudgetEntry, Deal, Product, User entities
- The `inventory_security_macros` crate provides `#[require_permission]` proc macro for RBAC gating
