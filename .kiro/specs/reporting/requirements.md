# Requirements Document

## Introduction

This document specifies the reporting feature for the Inventory Management Application built with Rust, Qleany, and Slint UI. The feature provides report generation in three output formats (Excel, PDF, CSV) across four report types: inventory, stock movement, budget, and purchasing. Each report is a read-only long operation that runs on a background thread with progress reporting, keeping the UI responsive. Reports pull data from existing entities across all major domains (products, stock movements, budget entries, deals) and apply user-specified filters. All report generation is gated by RBAC requiring `report:*` or `report:generate` permission (Admin, Manager, Viewer).

## Glossary

- **Report_Engine**: The shared infrastructure responsible for coordinating report generation, including format dispatch, progress reporting, and file writing.
- **Inventory_Report_Handler**: The use case handler responsible for generating an inventory report listing products with their category, location, supplier, quantity, and price information.
- **Stock_Movement_Report_Handler**: The use case handler responsible for generating a stock movement report listing movement history within a date range with movement type, quantities, locations, and performing users.
- **Budget_Report_Handler**: The use case handler responsible for generating a budget report listing financial entries within a date range with optional projection appendix.
- **Purchasing_Report_Handler**: The use case handler responsible for generating a purchasing report listing deals with supplier, product, status, frequency, and cost information.
- **ReportFormat**: An enum with values Excel, Pdf, and Csv specifying the output file format for report generation.
- **DealStatusFilter**: An enum with values All, ActiveOnly, and CompletedOnly specifying which deals to include in the purchasing report.
- **Excel_Writer**: The component responsible for writing report data to .xlsx files using the `rust_xlsxwriter` crate, supporting formatted headers, multiple sheets, and auto-column-width.
- **Pdf_Writer**: The component responsible for writing report data to .pdf files using the `genpdf` crate, supporting table-based layouts with headers and page breaks.
- **Csv_Writer**: The component responsible for writing report data to .csv files using the `csv` crate, with a header row and one data row per record.
- **Progress_Reporter**: The Qleany long_operation progress mechanism that reports completion percentage from the background thread to the UI.
- **Reports_Page**: The Slint UI page containing four report sections, each with a format selector, filter options, generate button, and progress indicator.
- **Product**: An inventory item entity with name, reference, description, quantity, price_unit, status, and relationships to Category, Person (supplier), and Location.
- **Category**: A hierarchical classification entity with name and description.
- **Location**: A storage site entity with name, address, and capacity.
- **Person**: A business contact entity with name and role (Manager/Supplier).
- **StockMovement**: An audit entity recording stock changes with movement_type, quantity, note, product, from/to locations, and performing user.
- **BudgetEntry**: A financial record entity with entry_type, amount, description, entry_date, and relationships to Product, Deal, and User.
- **Deal**: A purchasing agreement entity with title, description, unit_cost, total_value, frequency, status, and relationships to Product, supplier Person, and manager Person.
- **User**: An application identity entity with username, display_name, and role.
- **RBAC_Engine**: The role-based access control enforcement layer from the `inventory_security_macros` crate that gates use case execution via `#[require_permission]`.

## Requirements

### Requirement 1: Generate Inventory Report

**User Story:** As a manager or viewer, I want to generate an inventory report, so that I can review the current state of all products with their categories, locations, suppliers, quantities, and prices.

#### Acceptance Criteria

1. WHEN a user provides a valid GenerateInventoryReportDto with output_path, format, and include_zero_stock, THE Inventory_Report_Handler SHALL generate a report file at the specified output_path in the specified format and return a GenerateReportReturnDto containing the file_path and row_count.
2. WHEN include_zero_stock is false, THE Inventory_Report_Handler SHALL exclude Product entities whose quantity equals zero from the report.
3. WHEN include_zero_stock is true, THE Inventory_Report_Handler SHALL include all Product entities regardless of quantity.
4. THE Inventory_Report_Handler SHALL include the following columns in the report: product name, reference, description, quantity, price_unit, status, category name, location name, and supplier name.
5. WHEN a Product has no associated Category, Location, or supplier, THE Inventory_Report_Handler SHALL write empty strings for those fields in the report.
6. WHEN a non-authorized user attempts to generate an inventory report, THE RBAC_Engine SHALL reject the operation with an AccessDenied error (requires `report:*` or `report:generate` permission).

### Requirement 2: Generate Stock Movement Report

**User Story:** As a manager or viewer, I want to generate a stock movement report for a date range, so that I can audit inventory changes over a specific period.

#### Acceptance Criteria

1. WHEN a user provides a valid GenerateStockMovementReportDto with output_path, format, from_date, and to_date, THE Stock_Movement_Report_Handler SHALL generate a report file containing all StockMovement entities whose created_at falls within the inclusive range [from_date, to_date], and return a GenerateStockReportReturnDto containing the file_path and row_count.
2. THE Stock_Movement_Report_Handler SHALL include the following columns in the report: movement date, movement type, product name, product reference, quantity, from location name, to location name, performing user display name, and note.
3. THE Stock_Movement_Report_Handler SHALL order rows chronologically by the StockMovement created_at timestamp.
4. WHEN from_date is later than to_date, THE Stock_Movement_Report_Handler SHALL return a validation error indicating the date range is invalid.
5. WHEN no StockMovement entities exist within the date range, THE Stock_Movement_Report_Handler SHALL generate a report file with headers only and return row_count of zero.
6. WHEN a non-authorized user attempts to generate a stock movement report, THE RBAC_Engine SHALL reject the operation with an AccessDenied error (requires `report:*` or `report:generate` permission).

### Requirement 3: Generate Budget Report

**User Story:** As a manager or viewer, I want to generate a budget report for a date range with optional projections, so that I can review financial activity and plan ahead.

#### Acceptance Criteria

1. WHEN a user provides a valid GenerateBudgetReportDto with output_path, format, from_date, to_date, and include_projections, THE Budget_Report_Handler SHALL generate a report file containing all BudgetEntry entities whose entry_date falls within the inclusive range [from_date, to_date], and return a GenerateBudgetReportReturnDto containing the file_path and row_count.
2. THE Budget_Report_Handler SHALL include the following columns in the report: entry date, entry type, amount, description, product name, deal title, and recorded by user display name.
3. WHEN include_projections is true, THE Budget_Report_Handler SHALL append a projection section to the report containing projected income, projected expenses, projected balance, and recurring deal costs for the next 6 months.
4. WHEN include_projections is false, THE Budget_Report_Handler SHALL generate the report without a projection section.
5. WHEN from_date is later than to_date, THE Budget_Report_Handler SHALL return a validation error indicating the date range is invalid.
6. WHEN no BudgetEntry entities exist within the date range, THE Budget_Report_Handler SHALL generate a report file with headers only and return row_count of zero.
7. WHEN a non-authorized user attempts to generate a budget report, THE RBAC_Engine SHALL reject the operation with an AccessDenied error (requires `report:*` or `report:generate` permission).

### Requirement 4: Generate Purchasing Report

**User Story:** As a manager or viewer, I want to generate a purchasing report filtered by deal status, so that I can review purchasing agreements and their financial details.

#### Acceptance Criteria

1. WHEN a user provides a valid GeneratePurchasingReportDto with output_path, format, and status_filter of All, THE Purchasing_Report_Handler SHALL generate a report file containing all Deal entities and return a GeneratePurchasingReportReturnDto containing the file_path and row_count.
2. WHEN status_filter is ActiveOnly, THE Purchasing_Report_Handler SHALL include only Deal entities whose status is Active.
3. WHEN status_filter is CompletedOnly, THE Purchasing_Report_Handler SHALL include only Deal entities whose status is Completed.
4. THE Purchasing_Report_Handler SHALL include the following columns in the report: deal title, description, supplier name, product name, status, frequency, unit_cost, total_value, start_date, and end_date.
5. WHEN a Deal has no associated supplier or product, THE Purchasing_Report_Handler SHALL write empty strings for those fields in the report.
6. WHEN no Deal entities match the status_filter, THE Purchasing_Report_Handler SHALL generate a report file with headers only and return row_count of zero.
7. WHEN a non-authorized user attempts to generate a purchasing report, THE RBAC_Engine SHALL reject the operation with an AccessDenied error (requires `report:*` or `report:generate` permission).

### Requirement 5: Report Output Formats

**User Story:** As a user, I want reports generated in Excel, PDF, or CSV format, so that I can use the format most suitable for my needs.

#### Acceptance Criteria

1. WHEN the format is Excel, THE Report_Engine SHALL generate a .xlsx file using the `rust_xlsxwriter` crate with formatted column headers (bold), auto-adjusted column widths, and data rows.
2. WHEN the format is Pdf, THE Report_Engine SHALL generate a .pdf file using the `genpdf` crate with a report title, table headers, and data rows with appropriate page breaks.
3. WHEN the format is Csv, THE Report_Engine SHALL generate a .csv file using the `csv` crate with a header row followed by data rows.
4. WHEN the output_path is not writable, THE Report_Engine SHALL return an error indicating the path is not writable without creating a partial file.
5. THE Report_Engine SHALL ensure the generated file exists at the output_path upon successful completion.

### Requirement 6: Long Operation and Progress Reporting

**User Story:** As a user generating a large report, I want the operation to run in the background with progress updates, so that the UI remains responsive.

#### Acceptance Criteria

1. WHEN a report generation starts, THE Report_Engine SHALL execute on a background thread as a Qleany long_operation.
2. WHILE a report generation is in progress, THE Report_Engine SHALL report progress as a percentage based on rows processed versus total rows to the Progress_Reporter.
3. WHEN a report generation completes successfully, THE Report_Engine SHALL emit a completion signal with the result DTO that the UI can observe.
4. IF a report generation fails, THEN THE Report_Engine SHALL emit an error signal with a descriptive error message that the UI can observe.

### Requirement 7: Row Count Accuracy

**User Story:** As a user, I want the reported row_count to accurately reflect the number of data rows in the generated file, so that I can verify the report completeness.

#### Acceptance Criteria

1. THE Report_Engine SHALL return a row_count equal to the number of data rows written to the report file (excluding header rows and projection sections).
2. FOR ALL successful report generations, the row_count in the return DTO SHALL match the number of data records in the generated file.

### Requirement 8: CSV Round-Trip Consistency

**User Story:** As a developer, I want CSV report output to be parseable back into structured data, so that reports can be programmatically consumed.

#### Acceptance Criteria

1. FOR ALL inventory reports generated in CSV format, parsing the output CSV file SHALL produce records whose field values match the source Product entity data (with empty strings for absent relationships).
2. FOR ALL stock movement reports generated in CSV format, parsing the output CSV file SHALL produce records whose field values match the source StockMovement entity data.
3. FOR ALL budget reports generated in CSV format, parsing the output CSV file SHALL produce records whose field values match the source BudgetEntry entity data.
4. FOR ALL purchasing reports generated in CSV format, parsing the output CSV file SHALL produce records whose field values match the source Deal entity data.

### Requirement 9: Reports Page UI

**User Story:** As a desktop user, I want a reports page in the Slint UI, so that I can select report type, format, options, and generate reports with visual progress feedback.

#### Acceptance Criteria

1. WHEN a user navigates to the Reports_Page, THE Reports_Page SHALL display four report sections: Inventory Report, Stock Movement Report, Budget Report, and Purchasing Report.
2. WHEN a user selects a report section, THE Reports_Page SHALL display a format selector (Excel, PDF, CSV), report-specific filter options, and a generate button.
3. WHILE a report generation is in progress, THE Reports_Page SHALL display a progress indicator showing the completion percentage.
4. WHEN a report generation completes, THE Reports_Page SHALL display a success message with the output file path.
5. IF a report generation fails, THEN THE Reports_Page SHALL display the error message to the user.
6. WHEN a non-authorized user views the Reports_Page, THE Reports_Page SHALL hide the generate buttons and display a message indicating insufficient permissions.
