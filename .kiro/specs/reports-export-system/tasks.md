# Implementation Plan: Reports Export System

## Overview

This implementation plan breaks down the Reports Export System into incremental coding tasks. The approach follows the existing JavaFX MVC architecture and integrates with BillService and PartService. We'll build the system in layers: data models, report generation logic, export functionality, and UI components. Each major component includes property-based tests to validate correctness properties and unit tests for specific scenarios.

## Tasks

- [x] 1. Set up project structure and data models
  - [x] 1.1 Create package structure for reports module
    - Create packages: `reports.models`, `reports.services`, `reports.controllers`, `reports.views`
    - _Requirements: 10.7_
  
  - [x] 1.2 Implement base ReportData model and supporting enums
    - Create abstract `ReportData` class with common fields (reportType, generatedAt, companyName)
    - Create `PaymentMethod` enum (CASH, CARD, CHECK)
    - Create `StockStatus` enum (NORMAL, LOW_STOCK, OUT_OF_STOCK)
    - _Requirements: 1.1, 4.1_
  
  - [x] 1.3 Implement SalesReportData model
    - Create `SalesReportData` class extending `ReportData`
    - Include fields: startDate, endDate, totalRevenue, salesCount, averageSaleValue, paymentMethodBreakdown, totalDiscounts, averageDiscountPercentage, topSellingParts
    - Create `TopSellingPart` helper class
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.3, 2.4, 3.1, 3.2_
  
  - [x] 1.4 Implement InventoryReportData model
    - Create `InventoryReportData` class extending `ReportData`
    - Include fields: stockThreshold, totalInventoryValue, partsByCategory, lowStockParts, outOfStockParts
    - Create `PartInventoryItem` helper class
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 5.1, 5.2_

- [x] 2. Implement sales report generation
  - [x] 2.1 Create SalesReportGenerator class
    - Implement `generate(LocalDate startDate, LocalDate endDate)` method
    - Integrate with BillService to retrieve sales data
    - Implement date range filtering logic
    - _Requirements: 1.1, 11.1_
  
  - [x] 2.2 Implement sales metrics calculations
    - Implement `calculateTotalRevenue()` method
    - Implement `calculateSalesCount()` method
    - Implement `calculateAverageSaleValue()` method
    - Handle edge case: zero sales (return zero for all metrics)
    - _Requirements: 1.2, 1.3, 1.4, 1.5_
  
  - [x] 2.3 Implement payment method breakdown
    - Implement `groupByPaymentMethod()` method
    - Calculate total revenue for each payment method
    - _Requirements: 2.1, 2.2_
  
  - [x] 2.4 Implement discount analysis
    - Implement `calculateDiscountAnalysis()` method
    - Calculate total discounts and average discount percentage
    - Handle edge case: no discounts (return zero)
    - _Requirements: 2.3, 2.4, 2.5_
  
  - [x] 2.5 Implement top selling parts calculation
    - Implement `calculateTopSellingParts()` method
    - Aggregate quantities by part across all bills
    - Sort by quantity descending and take top 10
    - Calculate revenue for each top selling part
    - Handle edge case: fewer than 10 parts sold
    - _Requirements: 3.1, 3.2, 3.3, 3.4_
  
  - [ ]* 2.6 Write property tests for sales report generation
    - **Property 1: Date range filtering completeness**
    - **Property 2: Total revenue calculation correctness**
    - **Property 3: Sales count accuracy**
    - **Property 4: Average sale value calculation**
    - **Property 5: Payment method breakdown completeness**
    - **Property 6: Discount calculation correctness**
    - **Property 7: Top selling parts ranking**
    - **Property 8: Top selling parts data completeness**
    - **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 2.1, 2.3, 2.4, 3.1, 3.2, 3.4**
  
  - [ ]* 2.7 Write unit tests for sales report edge cases
    - Test empty date range (no sales)
    - Test zero discounts
    - Test fewer than 10 parts sold
    - Test single sale scenario
    - _Requirements: 1.5, 2.5, 3.3_

- [x] 3. Implement inventory report generation
  - [x] 3.1 Create InventoryReportGenerator class
    - Implement `generate(int stockThreshold)` method
    - Integrate with PartService to retrieve inventory data
    - _Requirements: 4.1, 11.2_
  
  - [x] 3.2 Implement inventory calculations
    - Implement `calculateStockValue()` method for individual parts
    - Implement `calculateTotalInventoryValue()` method
    - _Requirements: 4.2, 4.3_
  
  - [x] 3.3 Implement category grouping and sorting
    - Implement `groupByCategory()` method
    - Sort parts alphabetically within each category
    - _Requirements: 4.4, 4.5_
  
  - [x] 3.4 Implement stock status classification
    - Implement `identifyLowStockParts()` method (0 < quantity <= threshold)
    - Implement `identifyOutOfStockParts()` method (quantity == 0)
    - Apply custom threshold when specified
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_
  
  - [ ]* 3.5 Write property tests for inventory report generation
    - **Property 9: Inventory completeness**
    - **Property 10: Stock value calculation**
    - **Property 11: Total inventory value**
    - **Property 12: Category grouping correctness**
    - **Property 13: Alphabetical ordering within categories**
    - **Property 14: Low stock classification**
    - **Property 15: Out of stock classification**
    - **Property 16: Stock status distinction**
    - **Property 17: Custom threshold application**
    - **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 5.1, 5.2, 5.3, 5.4, 5.5**
  
  - [ ]* 3.6 Write unit tests for inventory report edge cases
    - Test all parts out of stock
    - Test all parts low stock
    - Test single part in inventory
    - Test custom threshold values
    - _Requirements: 5.1, 5.2, 5.4_

- [x] 4. Checkpoint - Ensure report generation tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. Implement PDF export functionality
  - [x] 5.1 Set up PDF library dependency
    - Add Apache PDFBox or iText dependency to project
    - Configure library for PDF generation
    - _Requirements: 7.1_
  
  - [x] 5.2 Create PDFExporter class
    - Implement `export(ReportData reportData, String filePath)` method
    - Implement `generateFilename(String reportType)` with timestamp format
    - _Requirements: 7.1, 7.5_
  
  - [x] 5.3 Implement PDF header generation
    - Implement `addHeader()` method
    - Include company name, report type, generation timestamp
    - Include report parameters (date range or stock threshold)
    - _Requirements: 7.2_
  
  - [x] 5.4 Implement PDF content formatting for sales reports
    - Implement `addSalesContent()` method
    - Format summary metrics section
    - Create tables for payment method breakdown, discount analysis, top selling parts
    - Apply professional table formatting with borders and headers
    - _Requirements: 7.1, 7.3_
  
  - [x] 5.5 Implement PDF content formatting for inventory reports
    - Implement `addInventoryContent()` method
    - Format summary metrics section
    - Create tables for inventory details grouped by category
    - Highlight low stock and out of stock items
    - _Requirements: 7.1, 7.3_
  
  - [x] 5.6 Implement currency formatting for PDF
    - Implement `formatCurrency()` method
    - Format all monetary values with currency symbol and two decimal places
    - _Requirements: 7.4_
  
  - [ ]* 5.7 Write property tests for PDF export
    - **Property 20: PDF header completeness**
    - **Property 21: PDF currency formatting**
    - **Property 22: PDF filename format**
    - **Validates: Requirements 7.2, 7.4, 7.5**
  
  - [ ]* 5.8 Write unit tests for PDF export
    - Test PDF generation for sales report
    - Test PDF generation for inventory report
    - Test file creation and success message
    - Test error handling for file write failures
    - _Requirements: 7.1, 7.6_

- [ ] 6. Implement CSV export functionality
  - [x] 6.1 Create CSVExporter class
    - Implement `export(ReportData reportData, String filePath)` method
    - Implement `generateFilename(String reportType)` with timestamp format
    - _Requirements: 8.1, 8.5_
  
  - [x] 6.2 Implement CSV structure for sales reports
    - Implement `writeSalesCSV()` method
    - Write header rows with report metadata
    - Write column headers for each section
    - Write data rows with comma-separated values
    - _Requirements: 8.1, 8.2, 8.3_
  
  - [x] 6.3 Implement CSV structure for inventory reports
    - Implement `writeInventoryCSV()` method
    - Write header rows with report metadata
    - Write column headers for inventory details
    - Write data rows grouped by category
    - _Requirements: 8.1, 8.2, 8.3_
  
  - [x] 6.4 Implement CSV field escaping
    - Implement `escapeCSVField()` method
    - Escape commas by wrapping in quotes
    - Escape quotes by doubling them
    - _Requirements: 8.4_
  
  - [ ]* 6.5 Write property tests for CSV export
    - **Property 23: CSV structure correctness**
    - **Property 24: CSV escaping correctness**
    - **Property 25: CSV filename format**
    - **Validates: Requirements 8.2, 8.3, 8.4, 8.5**
  
  - [ ]* 6.6 Write unit tests for CSV export
    - Test CSV generation for sales report
    - Test CSV generation for inventory report
    - Test field escaping with commas and quotes
    - Test file creation and success message
    - Test error handling for file write failures
    - _Requirements: 8.1, 8.6_

- [ ] 7. Implement ReportService orchestration
  - [x] 7.1 Create ReportService class
    - Implement `generateSalesReport()` method delegating to SalesReportGenerator
    - Implement `generateInventoryReport()` method delegating to InventoryReportGenerator
    - Implement `exportToPDF()` method delegating to PDFExporter
    - Implement `exportToCSV()` method delegating to CSVExporter
    - _Requirements: 1.1, 4.1, 7.1, 8.1_
  
  - [x] 7.2 Implement error handling in ReportService
    - Handle database connection failures
    - Handle service call failures
    - Handle export errors (file write, disk space)
    - Display user-friendly error messages
    - Log all errors with stack traces
    - _Requirements: 11.4_
  
  - [x] 7.3 Implement fresh data retrieval
    - Ensure reports query database directly without caching
    - _Requirements: 11.3_
  
  - [ ]* 7.4 Write property test for fresh data retrieval
    - **Property 27: Fresh data retrieval**
    - **Validates: Requirements 11.3**
  
  - [ ]* 7.5 Write unit tests for ReportService
    - Test service orchestration for sales reports
    - Test service orchestration for inventory reports
    - Test error handling scenarios
    - Test integration with BillService and PartService
    - _Requirements: 11.1, 11.2, 11.4_

- [x] 8. Checkpoint - Ensure export functionality tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. Implement Reports UI
  - [x] 9.1 Create ReportsView.fxml layout
    - Design layout with report type selection (ComboBox)
    - Add date range controls (DatePickers, preset ComboBox)
    - Add stock threshold input (TextField)
    - Add action buttons (Preview, Export PDF, Export CSV, Print)
    - Add preview container (VBox)
    - Apply existing application theme
    - _Requirements: 10.3, 10.4, 10.5, 10.6, 10.7_
  
  - [x] 9.2 Create ReportsViewController class
    - Implement `initialize()` method to set up UI components
    - Implement `onReportTypeChanged()` to show/hide relevant controls
    - Implement date preset selection logic (today, this week, this month)
    - _Requirements: 10.4, 10.5, 6.1, 6.2, 6.3_
  
  - [x] 9.3 Implement input validation
    - Implement `validateInputs()` method
    - Validate date range (start <= end)
    - Validate stock threshold (positive number)
    - Display error messages for invalid inputs
    - _Requirements: 6.5_
  
  - [x] 9.4 Implement preview functionality
    - Implement `onPreviewClicked()` method
    - Call ReportService to generate report data
    - Implement `displayPreview()` method to render report in UI
    - Enable export and print buttons after successful preview
    - _Requirements: 9.1, 9.2, 9.4_
  
  - [x] 9.5 Implement preview state management
    - Implement `clearPreview()` method
    - Clear preview and disable export buttons when parameters change
    - _Requirements: 9.5_
  
  - [x] 9.6 Implement export button handlers
    - Implement `onExportPDFClicked()` method
    - Implement `onExportCSVClicked()` method
    - Open file chooser dialog with suggested filename
    - Call ReportService export methods
    - Display success message with file location
    - Handle export errors gracefully
    - _Requirements: 7.1, 7.6, 8.1, 8.6_
  
  - [x] 9.7 Implement print functionality
    - Implement `onPrintClicked()` method
    - Open system print dialog
    - Format report content for printing
    - Handle print cancellation
    - _Requirements: 12.1, 12.3, 12.4_
  
  - [ ]* 9.8 Write property tests for preview and print
    - **Property 26: Preview and export consistency**
    - **Property 28: Print content completeness**
    - **Validates: Requirements 9.2, 9.3, 12.3**
  
  - [ ]* 9.9 Write unit tests for ReportsViewController
    - Test report type selection behavior
    - Test date preset selection
    - Test input validation
    - Test preview generation and display
    - Test export button handlers
    - Test print functionality
    - Test error handling
    - _Requirements: 6.1, 6.2, 6.3, 6.5, 9.1, 9.4, 9.5, 10.4, 10.5, 12.1, 12.4_

- [ ] 10. Integrate Reports module with Dashboard
  - [x] 10.1 Add Reports navigation button to DashboardView
    - Add "Reports" button to dashboard navigation
    - _Requirements: 10.1_
  
  - [x] 10.2 Implement navigation to Reports view
    - Wire Reports button to load ReportsView
    - Ensure proper view switching
    - _Requirements: 10.2_
  
  - [ ]* 10.3 Write unit tests for dashboard integration
    - Test Reports button presence
    - Test navigation to Reports view
    - _Requirements: 10.1, 10.2_

- [ ] 11. Implement date range property tests
  - [ ]* 11.1 Write property tests for date range handling
    - **Property 18: Custom date range preservation**
    - **Property 19: Invalid date range rejection**
    - **Validates: Requirements 6.4, 6.5**

- [ ] 12. Final integration and testing
  - [x] 12.1 End-to-end integration testing
    - Test complete flow: select report type → configure parameters → preview → export PDF
    - Test complete flow: select report type → configure parameters → preview → export CSV
    - Test complete flow: select report type → configure parameters → preview → print
    - Verify all components work together correctly
    - _Requirements: All_
  
  - [ ]* 12.2 Write integration tests
    - Test sales report generation and export workflow
    - Test inventory report generation and export workflow
    - Test error handling across components
    - _Requirements: All_

- [x] 13. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at key milestones
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples, edge cases, and error conditions
- The implementation follows the existing JavaFX MVC architecture
- Integration with BillService and PartService ensures data consistency
- jqwik library is used for property-based testing with minimum 100 iterations per test
