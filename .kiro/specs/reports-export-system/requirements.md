# Requirements Document: Reports Export System

## Introduction

The Reports Export System enables directors to generate and export professional sales and inventory reports in PDF and CSV formats. This MVP implementation provides essential reporting capabilities for analyzing business performance and managing inventory levels in the ChaOffice parts inventory application.

## Glossary

- **Report_Generator**: The system component responsible for creating sales and inventory reports
- **Sales_Report**: A report showing sales transactions, revenue, and performance metrics for a specified date range
- **Inventory_Report**: A report displaying current stock levels, values, and alerts for all parts
- **PDF_Exporter**: The component that generates professionally formatted PDF documents
- **CSV_Exporter**: The component that generates comma-separated value files for spreadsheet analysis
- **Date_Range**: A period defined by start and end dates for filtering sales data
- **Stock_Threshold**: The minimum quantity level that triggers a low stock alert (default: 10 units)
- **Report_Preview**: A display of report data before export
- **Director**: A user with permissions to generate and export reports

## Requirements

### Requirement 1: Sales Report Generation

**User Story:** As a director, I want to generate sales reports for specific date ranges, so that I can analyze business performance and revenue trends.

#### Acceptance Criteria

1. WHEN a director selects a date range, THE Report_Generator SHALL retrieve all sales transactions within that period
2. WHEN generating a sales report, THE Report_Generator SHALL calculate total revenue as the sum of all sale amounts
3. WHEN generating a sales report, THE Report_Generator SHALL calculate the number of sales as the count of all transactions
4. WHEN generating a sales report, THE Report_Generator SHALL calculate average sale value as total revenue divided by number of sales
5. WHEN the date range contains no sales, THE Report_Generator SHALL display zero values for all metrics

### Requirement 2: Sales Breakdown Analysis

**User Story:** As a director, I want to see sales broken down by payment method and discount information, so that I can understand payment preferences and discount impact.

#### Acceptance Criteria

1. WHEN generating a sales report, THE Report_Generator SHALL group sales by payment method (cash, card, check)
2. WHEN generating a sales report, THE Report_Generator SHALL calculate total revenue for each payment method
3. WHEN generating a sales report, THE Report_Generator SHALL calculate total discounts given as the sum of all discount amounts
4. WHEN generating a sales report, THE Report_Generator SHALL calculate average discount percentage across all sales with discounts
5. WHEN no discounts were applied in the date range, THE Report_Generator SHALL display zero for discount metrics

### Requirement 3: Top Selling Parts Analysis

**User Story:** As a director, I want to see the top 10 selling parts, so that I can identify popular products and optimize inventory.

#### Acceptance Criteria

1. WHEN generating a sales report, THE Report_Generator SHALL identify the top 10 parts by quantity sold
2. WHEN generating a sales report, THE Report_Generator SHALL display part name, quantity sold, and revenue for each top selling part
3. WHEN fewer than 10 different parts were sold, THE Report_Generator SHALL display all parts that were sold
4. WHEN calculating top selling parts, THE Report_Generator SHALL rank parts by total quantity sold in descending order

### Requirement 4: Inventory Report Generation

**User Story:** As a director, I want to generate inventory reports showing current stock levels, so that I can monitor inventory status and identify restocking needs.

#### Acceptance Criteria

1. WHEN a director requests an inventory report, THE Report_Generator SHALL retrieve current stock levels for all parts
2. WHEN generating an inventory report, THE Report_Generator SHALL calculate stock value as quantity multiplied by price for each part
3. WHEN generating an inventory report, THE Report_Generator SHALL calculate total inventory value as the sum of all part stock values
4. WHEN generating an inventory report, THE Report_Generator SHALL group parts by category
5. THE Report_Generator SHALL display parts in alphabetical order within each category

### Requirement 5: Low Stock and Out-of-Stock Alerts

**User Story:** As a director, I want to see low stock alerts and out-of-stock items highlighted, so that I can prioritize restocking decisions.

#### Acceptance Criteria

1. WHEN a part quantity is less than or equal to the stock threshold, THE Report_Generator SHALL mark that part as low stock
2. WHEN a part quantity is zero, THE Report_Generator SHALL mark that part as out of stock
3. WHEN generating an inventory report, THE Report_Generator SHALL highlight out-of-stock items distinctly from low stock items
4. WHERE a custom stock threshold is specified, THE Report_Generator SHALL use that value instead of the default 10 units
5. THE Report_Generator SHALL display the stock threshold value used in the report header

### Requirement 6: Date Range Selection

**User Story:** As a director, I want to select date ranges using presets or custom dates, so that I can quickly generate reports for common time periods.

#### Acceptance Criteria

1. WHEN a director selects the "today" preset, THE Report_Generator SHALL set the date range to the current date
2. WHEN a director selects the "this week" preset, THE Report_Generator SHALL set the date range from the start of the current week to today
3. WHEN a director selects the "this month" preset, THE Report_Generator SHALL set the date range from the first day of the current month to today
4. WHEN a director enters custom start and end dates, THE Report_Generator SHALL use those exact dates for filtering
5. WHEN the start date is after the end date, THE Report_Generator SHALL display an error message and prevent report generation

### Requirement 7: PDF Export

**User Story:** As a director, I want to export reports as professionally formatted PDF files, so that I can share reports with stakeholders and maintain records.

#### Acceptance Criteria

1. WHEN a director clicks the PDF export button, THE PDF_Exporter SHALL generate a PDF document containing the report data
2. WHEN generating a PDF report, THE PDF_Exporter SHALL include a header with company name, report type, and generation timestamp
3. WHEN generating a PDF report, THE PDF_Exporter SHALL format tabular data in professional tables with borders and headers
4. WHEN generating a PDF report, THE PDF_Exporter SHALL format all monetary values with currency symbol and two decimal places
5. WHEN generating a PDF report, THE PDF_Exporter SHALL save the file with a timestamp in the filename format: ReportType_YYYYMMDD_HHMMSS.pdf
6. WHEN the PDF is generated successfully, THE PDF_Exporter SHALL display a success message with the file location

### Requirement 8: CSV Export

**User Story:** As a director, I want to export reports as CSV files, so that I can perform custom analysis in spreadsheet applications.

#### Acceptance Criteria

1. WHEN a director clicks the CSV export button, THE CSV_Exporter SHALL generate a CSV file containing the report data
2. WHEN generating a CSV report, THE CSV_Exporter SHALL include column headers in the first row
3. WHEN generating a CSV report, THE CSV_Exporter SHALL format each data row with comma-separated values
4. WHEN generating a CSV report, THE CSV_Exporter SHALL escape commas within field values using quotes
5. WHEN generating a CSV report, THE CSV_Exporter SHALL save the file with a timestamp in the filename format: ReportType_YYYYMMDD_HHMMSS.csv
6. WHEN the CSV is generated successfully, THE CSV_Exporter SHALL display a success message with the file location

### Requirement 9: Report Preview

**User Story:** As a director, I want to preview report data before exporting, so that I can verify the report contains the expected information.

#### Acceptance Criteria

1. WHEN a director clicks the preview button, THE Report_Generator SHALL display the report data in the user interface
2. WHEN displaying a preview, THE Report_Generator SHALL show all metrics and data that will appear in the exported report
3. WHEN displaying a preview, THE Report_Generator SHALL apply the same formatting rules as the exported reports
4. WHEN preview data is displayed, THE Report_Generator SHALL enable the export buttons
5. WHEN report parameters are changed after preview, THE Report_Generator SHALL clear the preview and disable export buttons until a new preview is generated

### Requirement 10: Reports User Interface

**User Story:** As a director, I want an intuitive reports interface, so that I can easily generate and export reports without confusion.

#### Acceptance Criteria

1. WHEN a director navigates to the dashboard, THE System SHALL display a "Reports" navigation option
2. WHEN a director clicks the Reports option, THE System SHALL display the Reports view with report type selection
3. WHEN the Reports view is displayed, THE System SHALL show options for Sales Report and Inventory Report
4. WHEN a director selects Sales Report, THE System SHALL display date range selection controls
5. WHEN a director selects Inventory Report, THE System SHALL display stock threshold input control
6. WHEN a report preview is displayed, THE System SHALL show Preview, Export PDF, Export CSV, and Print buttons
7. THE System SHALL follow the existing JavaFX MVC architecture and apply the current application theme

### Requirement 11: Data Integration

**User Story:** As a system architect, I want the Reports system to integrate with existing services, so that reports use accurate, up-to-date data from the application database.

#### Acceptance Criteria

1. WHEN retrieving sales data, THE Report_Generator SHALL use the existing BillService
2. WHEN retrieving inventory data, THE Report_Generator SHALL use the existing PartService
3. WHEN generating reports, THE Report_Generator SHALL query data directly from the database without caching
4. WHEN a database query fails, THE Report_Generator SHALL display an error message and prevent report generation

### Requirement 12: Print Functionality

**User Story:** As a director, I want to print reports directly, so that I can quickly produce physical copies without saving files.

#### Acceptance Criteria

1. WHEN a director clicks the Print button, THE System SHALL open the system print dialog
2. WHEN printing a report, THE System SHALL format the report content for the selected paper size
3. WHEN printing a report, THE System SHALL include all report data visible in the preview
4. WHEN the print dialog is cancelled, THE System SHALL return to the Reports view without changes
