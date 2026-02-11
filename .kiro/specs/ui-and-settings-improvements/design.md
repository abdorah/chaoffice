# Design Document: UI and Settings Improvements

## Overview

This design addresses multiple improvements to the ChaOffice application focusing on user interface optimization, settings management, and internationalization. The improvements include:

1. **Billing View Layout Optimization**: Restructure the billing view to use screen space more efficiently by repositioning discount and payment controls alongside the total summary
2. **Store Branding Configuration**: Add settings for customizable store name and logo that appear in the application and generated documents
3. **User Account Management**: Implement username and password change functionality with proper validation
4. **Bills History Date Filter Fix**: Correct the date filtering logic to handle future dates appropriately
5. **Currency Configuration**: Add settings for customizable currency symbol and acronym
6. **Reports Internationalization**: Complete the internationalization of the Reports view and generated report documents

## Architecture

### Component Overview

The improvements span multiple layers of the application:

- **View Layer**: Modifications to `BillingView`, `SettingsView`, `ReportsView`
- **Controller Layer**: Updates to `BillingController`, `SettingsController`, `BillsHistoryController`, report generation controllers
- **Service Layer**: New `BrandingService` for managing store branding, updates to `SettingsService`
- **Model Layer**: New `BrandingSettings` and `CurrencySettings` models
- **Utility Layer**: Updates to `LocaleManager` for report internationalization
- **Report Generation**: Updates to PDF, CSV, and Excel generators for internationalization and branding

### Design Principles

1. **Minimal Disruption**: Changes should integrate seamlessly with existing code
2. **Backward Compatibility**: Default values ensure the application works without configuration
3. **Separation of Concerns**: Branding, currency, and user management are separate concerns
4. **Internationalization First**: All user-facing text must use LocaleManager
5. **Responsive Layout**: UI components should adapt to available space

## Components and Interfaces

### 1. Billing View Layout Optimization

#### Current Layout Issues
- Command table has empty horizontal space
- Command table grows too tall, pushing discount/payment controls off-screen
- Users must scroll to access discount and payment options

#### Proposed Layout

```
┌─────────────────────────────────────────────────────────────┐
│ Client Information (Name, Phone)                            │
├─────────────────────────────────────────────────────────────┤
│ Part Search Component                                       │
│ Quantity Controls + Add Button                              │
├─────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────┬─────────────────────────────┐  │
│ │                         │  Discount Controls          │  │
│ │                         │  ○ None                     │  │
│ │   Command Table         │  ○ Percentage [____]        │  │
│ │   (Max Height: 300px)   │  ○ Fixed Amount [____]      │  │
│ │   (Scrollable)          │                             │  │
│ │                         │  Payment Method             │  │
│ │                         │  ○ Cash                     │  │
│ │                         │  ○ Card                     │  │
│ │                         │  ○ Check                    │  │
│ │                         │                             │  │
│ │                         │  ─────────────────────      │  │
│ │                         │  Subtotal:    $XXX.XX       │  │
│ │                         │  Discount:    -$XX.XX       │  │
│ │                         │  ═════════════════════      │  │
│ │                         │  Total:       $XXX.XX       │  │
│ │                         │                             │  │
│ │                         │  [Complete Sale]            │  │
│ └─────────────────────────┴─────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

#### Implementation Changes

**BillingView.java**:
- Replace center `VBox` with `HBox` containing two sections:
  - Left: Command table (70% width, max height 300px, scrollable)
  - Right: Controls panel (30% width) containing discount, payment, totals, and complete button
- Remove discount/payment controls from bottom section
- Set `HBox.setHgrow(commandsTable, Priority.ALWAYS)` for horizontal expansion
- Set fixed `maxHeight` on command table to prevent vertical overflow
- Wrap command table in `ScrollPane` for vertical scrolling when needed

### 2. Store Branding Configuration

#### New Model: BrandingSettings

```java
public class BrandingSettings {
    private String storeName;        // Optional, defaults to null
    private byte[] logoImage;        // Optional, stored as BLOB
    private String logoFormat;       // "PNG", "JPG", etc.
    
    // Getters and setters
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public byte[] getLogoImage() { return logoImage; }
    public void setLogoImage(byte[] logoImage) { this.logoImage = logoImage; }
    public String getLogoFormat() { return logoFormat; }
    public void setLogoFormat(String logoFormat) { this.logoFormat = logoFormat; }
    
    public boolean hasStoreName() { return storeName != null && !storeName.trim().isEmpty(); }
    public boolean hasLogo() { return logoImage != null && logoImage.length > 0; }
}
```

#### New Service: BrandingService

```java
public class BrandingService {
    private final DatabaseService databaseService;
    private BrandingSettings cachedSettings;
    
    public BrandingSettings getBrandingSettings() {
        // Load from database or return cached
    }
    
    public void saveBrandingSettings(BrandingSettings settings) {
        // Persist to database and update cache
    }
    
    public String getApplicationTitle() {
        BrandingSettings settings = getBrandingSettings();
        if (settings.hasStoreName()) {
            return settings.getStoreName();
        }
        return LocaleManager.getString("app.title");
    }
    
    public Image getApplicationLogo() {
        BrandingSettings settings = getBrandingSettings();
        if (settings.hasLogo()) {
            return new Image(new ByteArrayInputStream(settings.getLogoImage()));
        }
        return getDefaultLogo();
    }
}
```

#### Database Schema Addition

```sql
CREATE TABLE IF NOT EXISTS branding_settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),  -- Single row table
    store_name TEXT,
    logo_image BLOB,
    logo_format TEXT
);
```

#### SettingsView Updates

Add new section for branding:
- TextField for store name
- Button to select logo image file
- ImageView to preview selected logo
- Save button to persist changes

#### Application Title and Logo Integration

**ChaOfficeApplication.java**:
- Inject `BrandingService`
- Call `brandingService.getApplicationTitle()` when setting stage title
- Call `brandingService.getApplicationLogo()` when setting stage icon

#### PDF Generation Integration

**PDFGenerator.java** (or equivalent):
- Inject `BrandingService`
- Add header section to PDF with logo (if configured) and store name (if configured)
- Position logo in top-left corner
- Position store name as document title

### 3. User Account Management

#### New Model: UserCredentials

```java
public class UserCredentials {
    private String username;
    private String passwordHash;
    
    public UserCredentials(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }
    
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
}
```

#### Service Updates: AuthenticationService

```java
public class AuthenticationService {
    // Existing methods...
    
    public boolean changeUsername(String currentPassword, String newUsername) {
        // Verify current password
        // Validate new username (non-empty)
        // Update database
        // Return success/failure
    }
    
    public boolean changePassword(String currentPassword, String newPassword, String confirmPassword) {
        // Verify current password
        // Validate new password (min 6 chars)
        // Verify password confirmation matches
        // Hash new password
        // Update database
        // Return success/failure
    }
    
    private String hashPassword(String password) {
        // Use existing password hashing mechanism
    }
    
    private boolean verifyPassword(String password, String hash) {
        // Use existing password verification mechanism
    }
}
```

#### SettingsView Updates

Add new section for account management:
- Section title: "Account Management"
- Username change:
  - TextField for new username
  - Button "Change Username"
- Password change:
  - PasswordField for current password
  - PasswordField for new password
  - PasswordField for confirm new password
  - Button "Change Password"

#### Validation Rules

- Username: Must be non-empty after trimming
- Password: Must be at least 6 characters
- Current password: Must match existing password hash
- Confirm password: Must exactly match new password

### 4. Bills History Date Filter Fix

#### Current Bug
The date filter shows today's sales when selecting future dates because the SQL query doesn't properly handle future dates.

#### Fix Implementation

**BillsHistoryController.java**:

```java
public void filterBills() {
    LocalDate startDate = view.getStartDatePicker().getValue();
    LocalDate endDate = view.getEndDatePicker().getValue();
    LocalDate today = LocalDate.now();
    
    // Validation: start date must be before or equal to end date
    if (startDate.isAfter(endDate)) {
        showError("Start date must be before or equal to end date");
        return;
    }
    
    // If start date is in the future, show no results
    if (startDate.isAfter(today)) {
        view.getBillsTable().getItems().clear();
        return;
    }
    
    // If end date is in the future, cap it at today
    LocalDate effectiveEndDate = endDate.isAfter(today) ? today : endDate;
    
    // Query database with validated dates
    List<Bill> bills = billService.getBillsByDateRange(startDate, effectiveEndDate);
    view.getBillsTable().getItems().setAll(bills);
}
```

### 5. Currency Configuration

#### New Model: CurrencySettings

```java
public class CurrencySettings {
    private String symbol;      // "$", "€", "£", "¥", etc.
    private String acronym;     // "USD", "EUR", "GBP", "JPY", etc.
    
    public CurrencySettings() {
        this.symbol = "$";      // Default
        this.acronym = "USD";   // Default
    }
    
    public CurrencySettings(String symbol, String acronym) {
        this.symbol = symbol;
        this.acronym = acronym;
    }
    
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getAcronym() { return acronym; }
    public void setAcronym(String acronym) { this.acronym = acronym; }
    
    public String formatPrice(float price) {
        return String.format("%s%.2f", symbol, price);
    }
}
```

#### Service Updates: SettingsService

```java
public class SettingsService {
    private CurrencySettings cachedCurrencySettings;
    
    public CurrencySettings getCurrencySettings() {
        if (cachedCurrencySettings == null) {
            cachedCurrencySettings = loadCurrencySettingsFromDatabase();
        }
        return cachedCurrencySettings;
    }
    
    public void saveCurrencySettings(CurrencySettings settings) {
        // Persist to database
        // Update cache
        cachedCurrencySettings = settings;
    }
    
    private CurrencySettings loadCurrencySettingsFromDatabase() {
        // Load from database or return defaults
    }
}
```

#### Database Schema Addition

```sql
CREATE TABLE IF NOT EXISTS currency_settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),  -- Single row table
    symbol TEXT NOT NULL DEFAULT '$',
    acronym TEXT NOT NULL DEFAULT 'USD'
);
```

#### SettingsView Updates

Add new section for currency:
- ComboBox for common currency symbols: $, €, £, ¥, ₹, ¥, Custom
- TextField for custom symbol (enabled when "Custom" selected)
- TextField for currency acronym (e.g., "USD", "EUR")
- Save button to persist changes

#### Application-Wide Currency Display

**Utility Class: CurrencyFormatter**

```java
public class CurrencyFormatter {
    private static SettingsService settingsService;
    
    public static void initialize(SettingsService service) {
        settingsService = service;
    }
    
    public static String format(float price) {
        CurrencySettings settings = settingsService.getCurrencySettings();
        return settings.formatPrice(price);
    }
    
    public static String getSymbol() {
        return settingsService.getCurrencySettings().getSymbol();
    }
    
    public static String getAcronym() {
        return settingsService.getCurrencySettings().getAcronym();
    }
}
```

**View Updates**:
- Replace all hardcoded "$" with `CurrencyFormatter.format(price)` or `CurrencyFormatter.getSymbol()`
- Update: BillingView, BillsHistoryView, PartsInventoryView, AnalyticsView
- Update table cell factories to use CurrencyFormatter

### 6. Reports View Internationalization

#### Missing Localization Keys

Add to all language property files:

```properties
# English (messages_en_US.properties)
reports.preset=Preset

# Arabic (messages_ar.properties)
reports.preset=إعداد مسبق

# French (messages_fr.properties)
reports.preset=Préréglage
```

#### ReportsView.java Updates

Replace hardcoded strings:

```java
// Before:
Label presetLabel = new Label("Preset:");

// After:
Label presetLabel = new Label(LocaleManager.getString("reports.preset") + ":");
```

### 7. Report Document Internationalization

#### Report Generation Architecture

Current report generators (PDF, CSV, Excel) use hardcoded English strings. We need to update them to use LocaleManager.

#### New Localization Keys

Add to all language property files:

```properties
# Report column headers
report.column.date=Date
report.column.total=Total
report.column.client.name=Client Name
report.column.client.phone=Client Phone
report.column.revenue=Revenue
report.column.part.name=Part Name
report.column.quantity=Quantity
report.column.price=Price
report.column.category=Category
report.column.stock=Stock

# Report titles
report.sales.title=Sales Report
report.inventory.title=Inventory Report
report.period=Period
report.generated.on=Generated on
report.summary=Summary
report.total.revenue=Total Revenue
report.total.transactions=Total Transactions
report.low.stock.items=Low Stock Items
```

#### PDF Generator Updates

**SalesReportPDFGenerator.java**:

```java
public void generateSalesReport(List<Bill> bills, LocalDate startDate, LocalDate endDate, File outputFile) {
    Document document = new Document();
    PdfWriter.getInstance(document, new FileOutputStream(outputFile));
    document.open();
    
    // Add branding if configured
    BrandingSettings branding = brandingService.getBrandingSettings();
    if (branding.hasLogo()) {
        // Add logo image
    }
    if (branding.hasStoreName()) {
        // Add store name as header
    }
    
    // Use localized title
    Paragraph title = new Paragraph(LocaleManager.getString("report.sales.title"));
    document.add(title);
    
    // Use localized period label
    String period = String.format("%s: %s - %s", 
        LocaleManager.getString("report.period"),
        startDate.toString(),
        endDate.toString());
    document.add(new Paragraph(period));
    
    // Create table with localized headers
    PdfPTable table = new PdfPTable(5);
    table.addCell(LocaleManager.getString("report.column.date"));
    table.addCell(LocaleManager.getString("report.column.client.name"));
    table.addCell(LocaleManager.getString("report.column.client.phone"));
    table.addCell(LocaleManager.getString("report.column.total"));
    // ... add data rows
    
    document.add(table);
    document.close();
}
```

#### CSV Generator Updates

**SalesReportCSVGenerator.java**:

```java
public void generateSalesReport(List<Bill> bills, File outputFile) {
    try (PrintWriter writer = new PrintWriter(outputFile)) {
        // Write localized headers
        writer.println(String.join(",",
            LocaleManager.getString("report.column.date"),
            LocaleManager.getString("report.column.client.name"),
            LocaleManager.getString("report.column.client.phone"),
            LocaleManager.getString("report.column.total")
        ));
        
        // Write data rows
        for (Bill bill : bills) {
            writer.println(String.join(",",
                bill.getDate().toString(),
                bill.getClientName(),
                bill.getClientPhone(),
                CurrencyFormatter.format(bill.getTotalPrice())
            ));
        }
    }
}
```

#### Excel Generator Updates

**SalesReportExcelGenerator.java**:

```java
public void generateSalesReport(List<Bill> bills, File outputFile) {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet(LocaleManager.getString("report.sales.title"));
    
    // Create header row with localized strings
    Row headerRow = sheet.createRow(0);
    headerRow.createCell(0).setCellValue(LocaleManager.getString("report.column.date"));
    headerRow.createCell(1).setCellValue(LocaleManager.getString("report.column.client.name"));
    headerRow.createCell(2).setCellValue(LocaleManager.getString("report.column.client.phone"));
    headerRow.createCell(3).setCellValue(LocaleManager.getString("report.column.total"));
    
    // Add data rows
    int rowNum = 1;
    for (Bill bill : bills) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(bill.getDate().toString());
        row.createCell(1).setCellValue(bill.getClientName());
        row.createCell(2).setCellValue(bill.getClientPhone());
        row.createCell(3).setCellValue(bill.getTotalPrice());
    }
    
    // Write to file
    try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
        workbook.write(outputStream);
    }
    workbook.close();
}
```

## Data Models

### Database Schema Changes

```sql
-- Branding settings table
CREATE TABLE IF NOT EXISTS branding_settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    store_name TEXT,
    logo_image BLOB,
    logo_format TEXT
);

-- Currency settings table
CREATE TABLE IF NOT EXISTS currency_settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    symbol TEXT NOT NULL DEFAULT '$',
    acronym TEXT NOT NULL DEFAULT 'USD'
);

-- No changes needed to existing user table for username/password changes
-- Assuming existing user table has username and password_hash columns
```

### Model Classes

1. **BrandingSettings**: Stores optional store name and logo
2. **CurrencySettings**: Stores currency symbol and acronym
3. **UserCredentials**: Represents username and password hash



## Correctness Properties

A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.

### Billing View Layout Properties

Property 1: Command table horizontal expansion
*For any* Billing_View instance, the Command_Table width should equal the available container width minus the controls panel width
**Validates: Requirements 1.1**

Property 2: Command table maximum height constraint
*For any* Billing_View instance, the Command_Table maximum height should not exceed 300 pixels
**Validates: Requirements 1.5**

Property 3: Command table scrolling with many items
*For any* Billing_View with more than 10 command items, the Command_Table should display a vertical scrollbar and the Discount_Controls and Payment_Controls should remain visible without page scrolling
**Validates: Requirements 1.4**

### Store Branding Properties

Property 4: Store name in application title
*For any* configured store name, the application window title should contain that store name
**Validates: Requirements 2.3**

Property 5: Store logo in application icon
*For any* configured logo image, the application window icon should display that logo image
**Validates: Requirements 2.4**

Property 6: Store name in PDF documents
*For any* configured store name and any generated PDF document, the PDF content should contain the store name
**Validates: Requirements 2.5**

Property 7: Store logo in PDF documents
*For any* configured logo image and any generated PDF document, the PDF should contain the logo image
**Validates: Requirements 2.6**

Property 8: Branding persistence round-trip
*For any* branding settings (store name and logo), saving the settings then reloading them should produce equivalent settings
**Validates: Requirements 2.9**

### User Account Management Properties

Property 9: Password change requires current password
*For any* password change attempt without providing the correct current password, the system should reject the change and display an error
**Validates: Requirements 3.3**

Property 10: Password confirmation mismatch rejection
*For any* pair of non-matching password confirmation entries, the system should reject the change and display an error
**Validates: Requirements 3.5**

Property 11: Incorrect current password rejection
*For any* password change attempt with an incorrect current password, the system should reject the change and display an error
**Validates: Requirements 3.6**

Property 12: Successful credential change feedback
*For any* valid username or password change, the system should display a success message
**Validates: Requirements 3.7**

Property 13: Credential persistence round-trip
*For any* valid credential change, the new credentials should be persisted such that authentication with the new credentials succeeds
**Validates: Requirements 3.8**

Property 14: Empty username rejection
*For any* username that is empty or contains only whitespace, the system should reject the change
**Validates: Requirements 3.9**

Property 15: Short password rejection
*For any* password with fewer than 6 characters, the system should reject the change
**Validates: Requirements 3.10**

### Date Filter Properties

Property 16: Future start date returns no bills
*For any* start date that is after today's date, the bills history query should return an empty list
**Validates: Requirements 4.1, 4.3**

Property 17: Future end date normalization
*For any* end date that is after today's date, the bills history query should return the same results as if the end date were set to today
**Validates: Requirements 4.2**

Property 18: Invalid date range rejection
*For any* date range where the start date is after the end date, the system should display an error message and not execute the query
**Validates: Requirements 4.4**

Property 19: Valid date range filtering
*For any* valid date range in the past, the bills history query should return only bills with dates within that range (inclusive)
**Validates: Requirements 4.5**

### Currency Configuration Properties

Property 20: Currency symbol support
*For any* currency symbol from the set {$, €, £, ¥, ₹} or any custom symbol, the system should accept and store the symbol
**Validates: Requirements 5.3**

Property 21: Currency symbol display
*For any* configured currency symbol and any price display in the application, the displayed price should be prefixed with the configured symbol
**Validates: Requirements 5.4**

Property 22: Currency acronym in reports
*For any* configured currency acronym and any generated report document, the report should contain the configured acronym
**Validates: Requirements 5.5**

Property 23: Currency persistence round-trip
*For any* currency settings (symbol and acronym), saving the settings then reloading them should produce equivalent settings
**Validates: Requirements 5.7**

Property 24: Currency change immediate update
*For any* currency symbol change while a view with prices is displayed, all visible prices should immediately update to show the new symbol
**Validates: Requirements 5.8**

### Internationalization Properties

Property 25: Reports view language change
*For any* language change, all text elements in the Reports_View should update to display text in the selected language
**Validates: Requirements 6.4**

Property 26: PDF report localization
*For any* application language setting and any generated PDF report, all text content in the PDF should be in the selected language
**Validates: Requirements 7.1, 7.4, 7.5**

Property 27: CSV report localization
*For any* application language setting and any generated CSV report, all column headers should be in the selected language
**Validates: Requirements 7.2, 7.4, 7.5**

Property 28: Excel report localization
*For any* application language setting and any generated Excel report, all column headers and labels should be in the selected language
**Validates: Requirements 7.3, 7.4, 7.5**

## Error Handling

### Validation Errors

1. **Invalid Date Range**: When start date > end date, display error dialog with message from LocaleManager
2. **Empty Username**: When username is empty/whitespace, display error and prevent save
3. **Short Password**: When password < 6 characters, display error and prevent save
4. **Password Mismatch**: When confirmation doesn't match, display error and prevent save
5. **Wrong Current Password**: When current password is incorrect, display error and prevent change
6. **Invalid Currency Symbol**: When custom symbol is empty, display error and prevent save

### Database Errors

1. **Branding Save Failure**: Log error, display user-friendly message, keep UI in edit mode
2. **Currency Save Failure**: Log error, display user-friendly message, keep UI in edit mode
3. **Credential Update Failure**: Log error, display user-friendly message, don't update UI
4. **Settings Load Failure**: Log error, use default values, notify user

### File Operation Errors

1. **Logo Image Load Failure**: Display error dialog, don't update logo setting
2. **PDF Generation Failure**: Log error, display user-friendly message with details
3. **Report Export Failure**: Log error, display user-friendly message, suggest retry

## Testing Strategy

### Dual Testing Approach

This feature requires both unit tests and property-based tests:

- **Unit tests**: Verify specific UI layouts, default values, error messages, and edge cases
- **Property tests**: Verify universal properties across all inputs (dates, currencies, credentials, etc.)

### Unit Testing Focus

Unit tests should cover:
- Specific UI element existence (buttons, fields, labels)
- Default values (default currency "$", default title)
- Specific error messages for validation failures
- Integration between view and controller layers
- Database schema creation and migration

### Property-Based Testing Focus

Property tests should cover:
- Date range filtering with random dates
- Currency symbol display with random symbols
- Credential validation with random inputs
- Branding persistence with random names/images
- Localization with different language settings
- Report generation with random data

### Property Test Configuration

- Use **jqwik** for property-based testing (already in pom.xml)
- Minimum **100 iterations** per property test
- Each property test must reference its design document property
- Tag format: **Feature: ui-and-settings-improvements, Property {number}: {property_text}**

### Test Coverage Requirements

1. **Billing View Layout**: Unit tests for layout structure, property tests for responsive behavior
2. **Store Branding**: Unit tests for UI elements, property tests for persistence and display
3. **User Account Management**: Unit tests for validation messages, property tests for credential validation
4. **Date Filtering**: Property tests for all date range scenarios
5. **Currency Configuration**: Unit tests for UI, property tests for display and persistence
6. **Internationalization**: Unit tests for key existence, property tests for language switching

### Example Property Test

```java
@Property
@Label("Feature: ui-and-settings-improvements, Property 16: Future start date returns no bills")
void futureStartDateReturnsNoBills(@ForAll @FutureDate LocalDate startDate) {
    // Given: A start date in the future
    LocalDate endDate = LocalDate.now().plusDays(30);
    
    // When: Filtering bills with future start date
    List<Bill> bills = billsHistoryController.filterBills(startDate, endDate);
    
    // Then: No bills should be returned
    assertThat(bills).isEmpty();
}
```

### Testing Tools

- **JUnit 5**: Unit testing framework
- **jqwik**: Property-based testing framework
- **TestFX**: JavaFX UI testing (for view tests)
- **Mockito**: Mocking framework for service layer tests
