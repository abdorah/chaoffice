# Design Document: Reports Export System

## Overview

The Reports Export System adds professional reporting capabilities to the ChaOffice parts inventory application. The system enables directors to generate, preview, and export sales and inventory reports in PDF and CSV formats.

The design follows the existing JavaFX MVC architecture and integrates with the current BillService and PartService to retrieve data. The system consists of three main components:

1. **Report Generation Engine**: Calculates metrics and aggregates data from sales and inventory
2. **Export Subsystem**: Handles PDF and CSV file generation with professional formatting
3. **Reports UI**: Provides an intuitive interface for report configuration, preview, and export

This MVP implementation focuses on core reporting needs: sales analysis with date range filtering and inventory status with low stock alerts.

## Architecture

### Component Structure

```
ReportsModule
├── Controllers
│   ├── ReportsViewController (Main UI controller)
│   └── ReportPreviewController (Preview display)
├── Services
│   ├── ReportService (Report generation orchestration)
│   ├── SalesReportGenerator (Sales report logic)
│   ├── InventoryReportGenerator (Inventory report logic)
│   ├── PDFExporter (PDF generation)
│   └── CSVExporter (CSV generation)
├── Models
│   ├── ReportData (Base report data structure)
│   ├── SalesReportData (Sales-specific data)
│   └── InventoryReportData (Inventory-specific data)
└── Views
    ├── ReportsView.fxml (Main reports interface)
    └── ReportPreviewView.fxml (Preview display)
```

### Data Flow

1. **Report Configuration**: User selects report type and parameters (date range, stock threshold)
2. **Data Retrieval**: ReportService delegates to appropriate generator (Sales/Inventory)
3. **Data Aggregation**: Generator queries BillService/PartService and calculates metrics
4. **Preview Display**: ReportData is rendered in the UI for user verification
5. **Export**: User selects format (PDF/CSV), appropriate exporter generates file
6. **Confirmation**: System displays success message with file location

### Integration Points

- **BillService**: Provides sales transaction data with date filtering
- **PartService**: Provides current inventory data with category information
- **DashboardView**: Add "Reports" navigation button
- **Theme System**: Apply existing application theme to Reports UI

## Components and Interfaces

### ReportService

Central orchestration service for report generation.

**Interface:**
```java
public interface ReportService {
    SalesReportData generateSalesReport(LocalDate startDate, LocalDate endDate);
    InventoryReportData generateInventoryReport(int stockThreshold);
    void exportToPDF(ReportData reportData, String filePath);
    void exportToCSV(ReportData reportData, String filePath);
}
```

**Responsibilities:**
- Coordinate report generation workflow
- Delegate to specialized generators
- Manage export operations
- Handle error conditions

### SalesReportGenerator

Generates sales reports with revenue metrics and analysis.

**Interface:**
```java
public class SalesReportGenerator {
    public SalesReportData generate(LocalDate startDate, LocalDate endDate);
    private BigDecimal calculateTotalRevenue(List<Bill> bills);
    private int calculateSalesCount(List<Bill> bills);
    private BigDecimal calculateAverageSaleValue(BigDecimal totalRevenue, int salesCount);
    private Map<PaymentMethod, BigDecimal> groupByPaymentMethod(List<Bill> bills);
    private DiscountAnalysis calculateDiscountAnalysis(List<Bill> bills);
    private List<TopSellingPart> calculateTopSellingParts(List<Bill> bills, int limit);
}
```

**Key Algorithms:**

*Total Revenue Calculation:*
```
totalRevenue = sum(bill.totalAmount for all bills in date range)
```

*Average Sale Value:*
```
averageSaleValue = totalRevenue / salesCount
if salesCount == 0: averageSaleValue = 0
```

*Payment Method Breakdown:*
```
for each bill in bills:
    paymentMethodTotals[bill.paymentMethod] += bill.totalAmount
```

*Discount Analysis:*
```
totalDiscounts = sum(bill.discountAmount for all bills)
discountedBills = filter(bills where discountAmount > 0)
averageDiscountPercentage = sum(bill.discountPercentage for discountedBills) / count(discountedBills)
if count(discountedBills) == 0: averageDiscountPercentage = 0
```

*Top Selling Parts:*
```
1. Aggregate quantities by part across all bills
2. Sort parts by total quantity descending
3. Take top 10 parts
4. For each part, calculate total revenue from sales
```

### InventoryReportGenerator

Generates inventory reports with stock levels and alerts.

**Interface:**
```java
public class InventoryReportGenerator {
    public InventoryReportData generate(int stockThreshold);
    private BigDecimal calculateStockValue(Part part);
    private BigDecimal calculateTotalInventoryValue(List<Part> parts);
    private Map<String, List<Part>> groupByCategory(List<Part> parts);
    private List<Part> identifyLowStockParts(List<Part> parts, int threshold);
    private List<Part> identifyOutOfStockParts(List<Part> parts);
}
```

**Key Algorithms:**

*Stock Value Calculation:*
```
stockValue = part.quantity * part.price
```

*Total Inventory Value:*
```
totalValue = sum(part.quantity * part.price for all parts)
```

*Category Grouping:*
```
for each part in parts:
    categoryMap[part.category].add(part)
sort parts within each category alphabetically by name
```

*Low Stock Identification:*
```
lowStockParts = filter(parts where 0 < quantity <= stockThreshold)
```

*Out of Stock Identification:*
```
outOfStockParts = filter(parts where quantity == 0)
```

### PDFExporter

Generates professionally formatted PDF documents.

**Interface:**
```java
public class PDFExporter {
    public void export(ReportData reportData, String filePath);
    private void addHeader(Document document, ReportData reportData);
    private void addSalesContent(Document document, SalesReportData data);
    private void addInventoryContent(Document document, InventoryReportData data);
    private void formatTable(PdfPTable table);
    private String formatCurrency(BigDecimal amount);
    private String generateFilename(String reportType);
}
```

**PDF Structure:**
```
Header Section:
  - Company Name: "ChaOffice Parts Inventory"
  - Report Type: "Sales Report" or "Inventory Report"
  - Generation Date/Time: "Generated on: YYYY-MM-DD HH:MM:SS"
  - Report Parameters: Date range or stock threshold

Content Section:
  - Summary metrics (cards or highlighted boxes)
  - Detailed tables with borders and headers
  - Highlighted alerts (low stock, out of stock)

Footer Section:
  - Page numbers
```

**Formatting Rules:**
- Currency: "$X,XXX.XX" format
- Dates: "YYYY-MM-DD" format
- Percentages: "XX.XX%" format
- Tables: Alternating row colors for readability
- Fonts: Professional sans-serif (Arial or Helvetica)

### CSVExporter

Generates CSV files for spreadsheet analysis.

**Interface:**
```java
public class CSVExporter {
    public void export(ReportData reportData, String filePath);
    private void writeSalesCSV(SalesReportData data, PrintWriter writer);
    private void writeInventoryCSV(InventoryReportData data, PrintWriter writer);
    private String escapeCSVField(String field);
    private String generateFilename(String reportType);
}
```

**CSV Structure:**

*Sales Report CSV:*
```
Report Type,Sales Report
Date Range,YYYY-MM-DD to YYYY-MM-DD
Generated On,YYYY-MM-DD HH:MM:SS

Summary Metrics
Total Revenue,$X,XXX.XX
Number of Sales,XXX
Average Sale Value,$XXX.XX

Payment Method Breakdown
Payment Method,Total Revenue
Cash,$X,XXX.XX
Card,$X,XXX.XX
Check,$X,XXX.XX

Discount Analysis
Total Discounts Given,$XXX.XX
Average Discount Percentage,XX.XX%

Top Selling Parts
Part Name,Quantity Sold,Revenue
Part A,XXX,$X,XXX.XX
...
```

*Inventory Report CSV:*
```
Report Type,Inventory Report
Stock Threshold,XX units
Generated On,YYYY-MM-DD HH:MM:SS

Summary Metrics
Total Inventory Value,$XX,XXX.XX
Low Stock Items,XX
Out of Stock Items,XX

Inventory Details
Category,Part Name,Quantity,Price,Stock Value,Status
Category A,Part 1,XXX,$XX.XX,$X,XXX.XX,Normal
Category A,Part 2,5,$XX.XX,$XXX.XX,Low Stock
...
```

**CSV Escaping Rules:**
- Fields containing commas: Wrap in double quotes
- Fields containing quotes: Escape with double quotes ("")
- Fields containing newlines: Wrap in double quotes

### ReportsViewController

Main controller for the Reports UI.

**Interface:**
```java
public class ReportsViewController {
    @FXML private ComboBox<String> reportTypeComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> datePresetComboBox;
    @FXML private TextField stockThresholdField;
    @FXML private Button previewButton;
    @FXML private Button exportPDFButton;
    @FXML private Button exportCSVButton;
    @FXML private Button printButton;
    @FXML private VBox previewContainer;
    
    public void initialize();
    public void onReportTypeChanged();
    public void onDatePresetSelected();
    public void onPreviewClicked();
    public void onExportPDFClicked();
    public void onExportCSVClicked();
    public void onPrintClicked();
    private void displayPreview(ReportData reportData);
    private void clearPreview();
    private boolean validateInputs();
}
```

**UI Behavior:**

*Report Type Selection:*
- When "Sales Report" selected: Show date range controls, hide stock threshold
- When "Inventory Report" selected: Show stock threshold control, hide date range

*Date Preset Selection:*
- "Today": Set start and end date to current date
- "This Week": Set start to beginning of week (Monday), end to today
- "This Month": Set start to first day of month, end to today
- "Custom": Enable manual date picker selection

*Preview Generation:*
- Validate inputs (date range valid, threshold positive)
- Call ReportService to generate report data
- Display formatted preview in previewContainer
- Enable export and print buttons

*Export Operations:*
- Open file chooser dialog with suggested filename
- Call appropriate exporter (PDF/CSV)
- Display success message with file location
- Handle errors gracefully

## Data Models

### ReportData (Abstract Base)

```java
public abstract class ReportData {
    private String reportType;
    private LocalDateTime generatedAt;
    private String companyName = "ChaOffice Parts Inventory";
    
    public abstract String getReportTitle();
    public abstract Map<String, String> getParameters();
}
```

### SalesReportData

```java
public class SalesReportData extends ReportData {
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalRevenue;
    private int salesCount;
    private BigDecimal averageSaleValue;
    private Map<PaymentMethod, BigDecimal> paymentMethodBreakdown;
    private BigDecimal totalDiscounts;
    private BigDecimal averageDiscountPercentage;
    private List<TopSellingPart> topSellingParts;
    
    @Override
    public String getReportTitle() {
        return "Sales Report";
    }
    
    @Override
    public Map<String, String> getParameters() {
        return Map.of(
            "Date Range", startDate + " to " + endDate
        );
    }
}
```

### InventoryReportData

```java
public class InventoryReportData extends ReportData {
    private int stockThreshold;
    private BigDecimal totalInventoryValue;
    private Map<String, List<PartInventoryItem>> partsByCategory;
    private List<PartInventoryItem> lowStockParts;
    private List<PartInventoryItem> outOfStockParts;
    
    @Override
    public String getReportTitle() {
        return "Inventory Report";
    }
    
    @Override
    public Map<String, String> getParameters() {
        return Map.of(
            "Stock Threshold", stockThreshold + " units"
        );
    }
}
```

### Supporting Models

```java
public class TopSellingPart {
    private String partName;
    private int quantitySold;
    private BigDecimal revenue;
}

public class PartInventoryItem {
    private String partName;
    private String category;
    private int quantity;
    private BigDecimal price;
    private BigDecimal stockValue;
    private StockStatus status; // NORMAL, LOW_STOCK, OUT_OF_STOCK
}

public enum PaymentMethod {
    CASH, CARD, CHECK
}

public enum StockStatus {
    NORMAL, LOW_STOCK, OUT_OF_STOCK
}
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Sales Report Properties

**Property 1: Date range filtering completeness**
*For any* date range and any set of sales transactions, all transactions returned by the report generator should have transaction dates within the specified range (inclusive), and no transactions outside the range should be included.
**Validates: Requirements 1.1**

**Property 2: Total revenue calculation correctness**
*For any* set of sales transactions, the total revenue in the report should equal the sum of all transaction amounts.
**Validates: Requirements 1.2**

**Property 3: Sales count accuracy**
*For any* set of sales transactions, the number of sales in the report should equal the count of transactions.
**Validates: Requirements 1.3**

**Property 4: Average sale value calculation**
*For any* sales report with non-zero sales count, the average sale value should equal total revenue divided by sales count. When sales count is zero, average sale value should be zero.
**Validates: Requirements 1.4**

**Property 5: Payment method breakdown completeness**
*For any* set of sales transactions, the sum of revenue across all payment methods should equal the total revenue, and each transaction should be counted exactly once in the payment method breakdown.
**Validates: Requirements 2.1**

**Property 6: Discount calculation correctness**
*For any* set of sales transactions, the total discounts should equal the sum of all discount amounts, and the average discount percentage should equal the mean of discount percentages for transactions with non-zero discounts.
**Validates: Requirements 2.3, 2.4**

**Property 7: Top selling parts ranking**
*For any* set of sales transactions, the top selling parts list should contain at most 10 parts, all parts should be ranked by total quantity sold in descending order (each part's quantity >= the next part's quantity), and if fewer than 10 parts were sold, all sold parts should be included.
**Validates: Requirements 3.1, 3.4**

**Property 8: Top selling parts data completeness**
*For any* top selling part in the report, the part should have a non-empty name, a positive quantity sold, and a revenue value that matches the sum of that part's sales.
**Validates: Requirements 3.2**

### Inventory Report Properties

**Property 9: Inventory completeness**
*For any* inventory report, all parts in the database should be included in the report exactly once.
**Validates: Requirements 4.1**

**Property 10: Stock value calculation**
*For any* part in the inventory report, the stock value should equal the part's quantity multiplied by its price.
**Validates: Requirements 4.2**

**Property 11: Total inventory value**
*For any* inventory report, the total inventory value should equal the sum of all individual part stock values.
**Validates: Requirements 4.3**

**Property 12: Category grouping correctness**
*For any* inventory report, all parts should be grouped by category such that each part appears in exactly one category group, and all parts in a category group have the same category value.
**Validates: Requirements 4.4**

**Property 13: Alphabetical ordering within categories**
*For any* category group in the inventory report, parts should be sorted alphabetically by name (each part's name <= the next part's name lexicographically).
**Validates: Requirements 4.5**

**Property 14: Low stock classification**
*For any* part in the inventory report, if the part's quantity is greater than zero and less than or equal to the stock threshold, the part should be marked as low stock.
**Validates: Requirements 5.1**

**Property 15: Out of stock classification**
*For any* part in the inventory report, if the part's quantity is zero, the part should be marked as out of stock.
**Validates: Requirements 5.2**

**Property 16: Stock status distinction**
*For any* inventory report, parts marked as out of stock should have a different status value than parts marked as low stock, and both should differ from normal stock parts.
**Validates: Requirements 5.3**

**Property 17: Custom threshold application**
*For any* custom stock threshold value provided, the inventory report should use that threshold for low stock calculations and display it in the report parameters.
**Validates: Requirements 5.4, 5.5**

### Date Range Properties

**Property 18: Custom date range preservation**
*For any* custom start and end dates where start <= end, the generated report should use exactly those dates for filtering.
**Validates: Requirements 6.4**

**Property 19: Invalid date range rejection**
*For any* date range where start date is after end date, the system should reject the input and display an error message without generating a report.
**Validates: Requirements 6.5**

### Export Properties

**Property 20: PDF header completeness**
*For any* generated PDF report, the document should contain the company name "ChaOffice Parts Inventory", the report type, and a generation timestamp in the header.
**Validates: Requirements 7.2**

**Property 21: PDF currency formatting**
*For any* monetary value in a PDF report, the value should be formatted with a currency symbol and exactly two decimal places (matching pattern $X.XX or $X,XXX.XX).
**Validates: Requirements 7.4**

**Property 22: PDF filename format**
*For any* generated PDF file, the filename should match the pattern ReportType_YYYYMMDD_HHMMSS.pdf where YYYYMMDD_HHMMSS represents the generation timestamp.
**Validates: Requirements 7.5**

**Property 23: CSV structure correctness**
*For any* generated CSV file, the first row should contain column headers, and each subsequent data row should have the same number of comma-separated fields as the header row.
**Validates: Requirements 8.2, 8.3**

**Property 24: CSV escaping correctness**
*For any* field value containing commas in a CSV report, the field should be wrapped in double quotes, and any quotes within the field should be escaped with double quotes.
**Validates: Requirements 8.4**

**Property 25: CSV filename format**
*For any* generated CSV file, the filename should match the pattern ReportType_YYYYMMDD_HHMMSS.csv where YYYYMMDD_HHMMSS represents the generation timestamp.
**Validates: Requirements 8.5**

### Preview Properties

**Property 26: Preview and export consistency**
*For any* report preview, the data and formatting displayed in the preview should match the data and formatting in the exported PDF and CSV files.
**Validates: Requirements 9.2, 9.3**

**Property 27: Fresh data retrieval**
*For any* report generation, the system should query the database directly without using cached data, ensuring the report reflects the current state of the database.
**Validates: Requirements 11.3**

**Property 28: Print content completeness**
*For any* print operation, the printed content should include all data visible in the report preview.
**Validates: Requirements 12.3**

## Error Handling

### Input Validation Errors

**Invalid Date Range:**
- Error: "Start date cannot be after end date"
- Action: Display error message, prevent report generation, keep UI in current state

**Invalid Stock Threshold:**
- Error: "Stock threshold must be a positive number"
- Action: Display error message, prevent report generation, highlight invalid field

**Missing Required Fields:**
- Error: "Please select a date range for the sales report"
- Action: Display error message, prevent report generation, highlight missing fields

### Data Retrieval Errors

**Database Connection Failure:**
- Error: "Unable to connect to database. Please try again."
- Action: Display error message, log error details, prevent report generation

**Service Call Failure:**
- Error: "Unable to retrieve [sales/inventory] data. Please try again."
- Action: Display error message, log error details, prevent report generation

### Export Errors

**File Write Permission Error:**
- Error: "Unable to save file to selected location. Please choose a different location."
- Action: Display error message, reopen file chooser dialog

**Disk Space Error:**
- Error: "Insufficient disk space to save report. Please free up space and try again."
- Action: Display error message, prevent file creation

**PDF Generation Error:**
- Error: "Unable to generate PDF report. Please try again."
- Action: Display error message, log error details, keep preview visible

**CSV Generation Error:**
- Error: "Unable to generate CSV report. Please try again."
- Action: Display error message, log error details, keep preview visible

### Print Errors

**Print Service Unavailable:**
- Error: "Print service is not available. Please check your printer settings."
- Action: Display error message, return to Reports view

**Print Cancelled:**
- Action: Return to Reports view without changes, no error message needed

### Error Handling Principles

1. **User-Friendly Messages**: All error messages should be clear and actionable
2. **State Preservation**: Errors should not clear user input or preview data
3. **Logging**: All errors should be logged with stack traces for debugging
4. **Graceful Degradation**: Errors in one export format should not prevent other formats
5. **Recovery Guidance**: Error messages should suggest next steps when possible

## Testing Strategy

### Dual Testing Approach

The Reports Export System will use both unit testing and property-based testing to ensure comprehensive coverage:

- **Unit tests**: Verify specific examples, edge cases, and error conditions
- **Property tests**: Verify universal properties across all inputs

Both testing approaches are complementary and necessary. Unit tests catch concrete bugs in specific scenarios, while property tests verify general correctness across a wide range of inputs.

### Property-Based Testing Configuration

**Library Selection:**
- Use **jqwik** for Java property-based testing
- jqwik integrates with JUnit 5 and provides powerful generators for Java types

**Test Configuration:**
- Each property test must run minimum 100 iterations
- Use `@Property` annotation with `tries = 100` parameter
- Each test must reference its design document property in a comment

**Tag Format:**
```java
// Feature: reports-export-system, Property 1: Date range filtering completeness
@Property(tries = 100)
void testDateRangeFilteringCompleteness(@ForAll LocalDate startDate, 
                                        @ForAll LocalDate endDate,
                                        @ForAll List<Bill> bills) {
    // Test implementation
}
```

### Unit Testing Focus

Unit tests should focus on:

1. **Specific Examples**: Concrete scenarios that demonstrate correct behavior
   - Example: Sales report for a specific date range with known data
   - Example: Inventory report with exactly 10 parts at various stock levels

2. **Edge Cases**: Boundary conditions and special cases
   - Empty date ranges (no sales)
   - Zero discounts
   - Fewer than 10 parts sold
   - All parts out of stock
   - Single part in inventory

3. **Error Conditions**: Invalid inputs and failure scenarios
   - Invalid date ranges (start > end)
   - Negative stock thresholds
   - Database connection failures
   - File write permission errors

4. **Integration Points**: Interactions between components
   - ReportService calling BillService
   - ReportService calling PartService
   - UI controller calling ReportService
   - Export operations creating files

### Property Testing Focus

Property tests should focus on:

1. **Calculation Correctness**: Mathematical relationships that always hold
   - Total revenue = sum of all sales
   - Average = total / count
   - Stock value = quantity × price

2. **Data Integrity**: Invariants about data structure
   - All transactions in date range
   - All parts included exactly once
   - Payment method totals sum to total revenue

3. **Ordering and Ranking**: Sort order properties
   - Top parts in descending order
   - Parts alphabetical within categories

4. **Format Compliance**: Output format rules
   - Currency formatting pattern
   - Filename format pattern
   - CSV structure and escaping

5. **Consistency**: Relationships between different outputs
   - Preview matches export
   - PDF and CSV contain same data

### Test Data Generation

**For Property Tests:**
- Generate random dates within reasonable ranges (2020-2030)
- Generate random monetary values (0.01 to 10000.00)
- Generate random quantities (0 to 1000)
- Generate random part names and categories
- Generate random payment methods
- Ensure generators cover edge cases (zero values, empty lists)

**For Unit Tests:**
- Use fixed, meaningful test data
- Create builder patterns for test objects
- Use test data factories for common scenarios

### Coverage Goals

- **Line Coverage**: Minimum 80% for all service and generator classes
- **Branch Coverage**: Minimum 75% for all conditional logic
- **Property Coverage**: Each correctness property must have at least one property test
- **Error Path Coverage**: Each error condition must have at least one unit test

### Test Organization

```
src/test/java/
├── unit/
│   ├── services/
│   │   ├── SalesReportGeneratorTest.java
│   │   ├── InventoryReportGeneratorTest.java
│   │   ├── PDFExporterTest.java
│   │   └── CSVExporterTest.java
│   └── controllers/
│       └── ReportsViewControllerTest.java
└── properties/
    ├── SalesReportPropertiesTest.java
    ├── InventoryReportPropertiesTest.java
    ├── ExportPropertiesTest.java
    └── PreviewPropertiesTest.java
```

### Continuous Testing

- Run unit tests on every commit
- Run property tests on every pull request
- Include both test types in CI/CD pipeline
- Monitor test execution time and optimize slow tests
- Review and update tests when requirements change
