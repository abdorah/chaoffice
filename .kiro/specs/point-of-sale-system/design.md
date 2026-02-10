# Design Document: Point of Sale System

## Overview

This design document specifies the technical implementation for transforming the existing basic billing view into a comprehensive Point of Sale (POS) system. The POS system will enhance the ChaOffice Parts Inventory application with flexible pricing, discount management, payment method tracking, and an optimized part search interface.

The implementation follows the existing JavaFX MVC architecture pattern used throughout the application, maintaining consistency with established patterns for database access (singleton DatabaseConnection), service layer operations, and UI component structure.

## Architecture

### Technology Stack
- **Language**: Java 17+
- **UI Framework**: JavaFX
- **Database**: SQLite with JDBC
- **Build Tool**: Maven
- **Testing**: JUnit 5 + jqwik (property-based testing)

### Architectural Patterns
- **MVC Pattern**: Separation of Model, View, and Controller layers
- **Service Layer**: Business logic encapsulated in service classes
- **Singleton Pattern**: DatabaseConnection for connection management
- **JavaFX Properties**: Observable properties for UI binding

### Component Layers

```
┌─────────────────────────────────────────────────────┐
│                   View Layer                        │
│  (BillingView, PartSearchComponent)                 │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│                Controller Layer                      │
│            (BillingController)                       │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│                 Service Layer                        │
│  (BillService, PartService, SearchService)          │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│                  Model Layer                         │
│  (Bill, Command, Part, Category)                    │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│              Database Layer                          │
│         (DatabaseConnection, SQLite)                 │
└─────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. Enhanced Bill Model

**Purpose**: Extend the existing Bill model to support discount and payment method tracking.

**New Properties**:
```java
public class Bill {
    // Existing properties
    private IntegerProperty id;
    private StringProperty clientName;
    private StringProperty clientPhone;
    private FloatProperty totalPrice;
    private ObjectProperty<LocalDate> date;
    
    // New properties for POS
    private FloatProperty subtotal;
    private StringProperty discountType;  // "none", "percentage", "fixed"
    private FloatProperty discountValue;
    private StringProperty paymentMethod; // "cash", "card", "check"
}
```

**Key Methods**:
- `calculateFinalTotal()`: Computes final total based on subtotal and discount
- `applyDiscount(String type, float value)`: Validates and applies discount
- `setPaymentMethod(String method)`: Sets payment method with validation

### 2. Enhanced Command Model

**Purpose**: Support editable pricing for individual line items.

**Behavior**:
- The existing `priceConsidered` field already supports custom pricing
- No model changes required
- Controller will handle price editing logic

### 3. PartSearchComponent (New Custom Control)

**Purpose**: Provide real-time part search with filtering and visual presentation.

**Structure**:
```java
public class PartSearchComponent extends VBox {
    private TextField searchField;
    private ComboBox<Category> categoryFilter;
    private ListView<Part> resultsView;
    private ObservableList<Part> allParts;
    private ObservableList<Part> filteredParts;
}
```

**Key Features**:
- Real-time search with text input listeners
- Category dropdown filter
- Custom cell renderer for part display
- Visual indicators for stock availability
- Click-to-add functionality

**Cell Renderer**:
```java
public class PartCell extends ListCell<Part> {
    @Override
    protected void updateItem(Part part, boolean empty) {
        // Display: image/icon, name, category, maker, price, stock
        // Apply distinct styling for out-of-stock items
    }
}
```

### 4. Enhanced BillingView

**Purpose**: Extend the existing BillingView with POS features.

**New UI Components**:
- Discount controls (RadioButtons for type, TextField for value)
- Payment method selector (RadioButtons or ComboBox)
- Editable price cells in commands table
- Enhanced total display (subtotal, discount, final total)
- Integrated PartSearchComponent

**Layout Structure**:
```
┌─────────────────────────────────────────────────────┐
│  Client Information (Name, Phone)                   │
├─────────────────────────────────────────────────────┤
│  Part Search Component                              │
│  ┌───────────────────────────────────────────────┐  │
│  │ Search: [________] Category: [All ▼]         │  │
│  │ ┌─────────────────────────────────────────┐  │  │
│  │ │ [Icon] Part Name - $XX.XX               │  │  │
│  │ │        Category | Maker | Stock: XX     │  │  │
│  │ └─────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────┤
│  Commands Table (Part, Qty, Price*, Subtotal)      │
│  * Editable price column                            │
├─────────────────────────────────────────────────────┤
│  Discount: ○ None ○ Percentage [__]% ○ Fixed $[__] │
│  Payment: ○ Cash ○ Card ○ Check                     │
├─────────────────────────────────────────────────────┤
│  Subtotal: $XXX.XX                                  │
│  Discount: -$XX.XX                                  │
│  ─────────────────                                  │
│  Total: $XXX.XX                    [Complete Sale]  │
└─────────────────────────────────────────────────────┘
```

### 5. Enhanced BillingController

**Purpose**: Orchestrate POS operations and coordinate between view and services.

**Key Responsibilities**:
- Initialize and bind UI components
- Handle part search and filtering
- Manage command list with editable prices
- Calculate and update totals in real-time
- Validate and complete sales transactions
- Handle discount application
- Manage payment method selection

**Key Methods**:
```java
public class BillingController {
    void initialize();
    void handleSearch(String query);
    void handleCategoryFilter(Category category);
    void handlePartSelection(Part part);
    void handlePriceEdit(Command command, float newPrice);
    void handleDiscountChange(String type, float value);
    void handlePaymentMethodChange(String method);
    void calculateTotals();
    void completeSale();
}
```

### 6. SearchService (New Service)

**Purpose**: Provide optimized part search functionality.

**Key Methods**:
```java
public class SearchService {
    List<Part> searchParts(String query, Integer categoryId);
    List<Part> searchByText(String query);
    List<Part> filterByCategory(int categoryId);
    List<Part> searchWithFilters(String query, Integer categoryId);
}
```

**Search Implementation**:
```sql
-- Base search query with indexes
SELECT p.id, p.name, m.name as maker_name, p.description, 
       p.price, p.quantity, p.catid, c.name as category_name
FROM parts p
JOIN makers m ON p.maker_id = m.id
JOIN categories c ON p.catid = c.id
WHERE (? IS NULL OR LOWER(p.name) LIKE LOWER(?))
  AND (? IS NULL OR LOWER(m.name) LIKE LOWER(?))
  AND (? IS NULL OR LOWER(p.description) LIKE LOWER(?))
  AND (? IS NULL OR LOWER(c.name) LIKE LOWER(?))
  AND (? IS NULL OR p.catid = ?)
ORDER BY p.name
```

### 7. Enhanced BillService

**Purpose**: Extend existing BillService to handle new POS fields.

**Modified Methods**:
```java
public class BillService {
    // Modified to save discount and payment method
    void saveBill(Bill bill, List<Command> commands);
    
    // New method for backwards compatibility
    Bill mapResultSetToBill(ResultSet rs);
}
```

**Database Operations**:
- INSERT: Include new columns with default values
- SELECT: Handle NULL values gracefully for existing records
- UPDATE: Maintain existing validation logic

### 8. Database Migration

**Purpose**: Extend bills table schema without breaking existing data.

**Migration SQL**:
```sql
-- Add new columns with default values
ALTER TABLE bills ADD COLUMN subtotal REAL DEFAULT 0.0;
ALTER TABLE bills ADD COLUMN discount_type TEXT DEFAULT 'none' 
    CHECK (discount_type IN ('none', 'percentage', 'fixed'));
ALTER TABLE bills ADD COLUMN discount_value REAL DEFAULT 0.0;
ALTER TABLE bills ADD COLUMN payment_method TEXT DEFAULT 'cash' 
    CHECK (payment_method IN ('cash', 'card', 'check'));

-- Create indexes for search performance
CREATE INDEX IF NOT EXISTS idx_parts_name ON parts(name);
CREATE INDEX IF NOT EXISTS idx_parts_catid ON parts(catid);
CREATE INDEX IF NOT EXISTS idx_makers_name ON makers(name);
```

## Data Models

### Bill Entity (Extended)

```java
public class Bill {
    private int id;                    // Primary key
    private String clientName;         // Required
    private String clientPhone;        // Required
    private float subtotal;            // Sum of all command subtotals
    private String discountType;       // "none", "percentage", "fixed"
    private float discountValue;       // Discount amount or percentage
    private float totalPrice;          // Final total after discount
    private String paymentMethod;      // "cash", "card", "check"
    private LocalDate date;            // Transaction date
}
```

**Invariants**:
- `subtotal >= 0`
- `totalPrice >= 0`
- `discountValue >= 0`
- `discountType IN ('none', 'percentage', 'fixed')`
- `paymentMethod IN ('cash', 'card', 'check')`
- If `discountType == 'percentage'`: `0 <= discountValue <= 100`
- If `discountType == 'fixed'`: `discountValue <= subtotal`
- `totalPrice = subtotal - calculateDiscountAmount()`

### Command Entity (Unchanged)

```java
public class Command {
    private int billId;              // Foreign key to bills
    private int partId;              // Foreign key to parts
    private int quantity;            // Must be positive
    private float priceConsidered;   // Price at time of sale (editable)
    private String partName;         // Denormalized for display
}
```

**Invariants**:
- `quantity > 0`
- `priceConsidered >= 0`

### Part Entity (Unchanged)

```java
public class Part {
    private int id;
    private String name;
    private String maker;
    private String description;
    private float price;
    private int quantity;
    private Category category;
}
```

**Invariants**:
- `price >= 0`
- `quantity >= 0`

### Search Result Display Model

```java
public class PartDisplayInfo {
    private Part part;
    private boolean inStock;
    private String displayText;
    private Image icon;
}
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property Reflection

After analyzing all acceptance criteria, I identified the following redundancies and consolidations:

**Redundancy Analysis**:
1. Properties 1.5 and 11.1 both test subtotal recalculation - can be combined
2. Properties 2.6 and 11.5 both test total recalculation - can be combined
3. Properties 1.6, 2.7, and 3.4 all test round-trip persistence - can be combined into one comprehensive property
4. Properties 4.1 and 6.5 both test real-time UI updates - can be combined
5. Properties 7.4 and 7.5 both test numeric precision - can be combined
6. Properties 9.1, 9.2, 9.3, and 9.4 all test inventory validation - can be combined into comprehensive properties

**Consolidated Properties**:
- Combined price/discount change properties into single "calculation correctness" property
- Combined all persistence properties into single "round-trip persistence" property
- Combined inventory validation properties into two focused properties
- Combined search filtering properties for better coverage

### Core Calculation Properties

**Property 1: Price validation**
*For any* price input value, the system should accept it if and only if it is a positive number (> 0)
**Validates: Requirements 1.3, 1.4**

**Property 2: Subtotal calculation correctness**
*For any* list of commands with their quantities and prices, the subtotal should equal the sum of (quantity × price) for all commands
**Validates: Requirements 1.5, 11.1**

**Property 3: Discount validation - percentage**
*For any* percentage discount value, the system should accept it if and only if it is between 0 and 100 (inclusive)
**Validates: Requirements 2.2**

**Property 4: Discount validation - fixed amount**
*For any* fixed discount amount and subtotal, the system should accept the discount if and only if it does not exceed the subtotal
**Validates: Requirements 2.4**

**Property 5: Final total calculation correctness**
*For any* bill with subtotal and discount (type and value), the final total should equal:
- If discount_type = "none": final_total = subtotal
- If discount_type = "percentage": final_total = subtotal × (1 - discount_value/100)
- If discount_type = "fixed": final_total = subtotal - discount_value
**Validates: Requirements 2.6, 11.4, 11.5**

**Property 6: Discount amount never exceeds subtotal**
*For any* valid bill, the calculated discount amount should never exceed the subtotal, and the final total should never be negative
**Validates: Requirements 2.4**

### Persistence Properties

**Property 7: Round-trip persistence**
*For any* bill with custom prices, discount settings, and payment method, saving the bill to the database and then retrieving it should produce a bill with identical values for:
- All command prices (priceConsidered)
- discount_type
- discount_value
- subtotal
- totalPrice (final_total)
- payment_method
**Validates: Requirements 1.6, 2.7, 3.4**

**Property 8: Database constraint enforcement - discount_type**
*For any* attempt to save a bill with discount_type, the database should accept only values in {'none', 'percentage', 'fixed'} and reject all other values
**Validates: Requirements 7.2**

**Property 9: Database constraint enforcement - payment_method**
*For any* attempt to save a bill with payment_method, the database should accept only values in {'cash', 'card', 'check'} and reject all other values
**Validates: Requirements 7.3**

**Property 10: Numeric precision preservation**
*For any* monetary value (subtotal, discount_value, totalPrice, priceConsidered) with up to 2 decimal places, storing and retrieving the value should preserve the exact value without precision loss
**Validates: Requirements 7.4, 7.5**

**Property 11: Backwards compatibility with NULL values**
*For any* existing bill record with NULL values in new columns (discount_type, discount_value, payment_method, subtotal), querying and displaying the bill should not cause errors and should use appropriate default values
**Validates: Requirements 10.2, 10.3**

### Search and Filter Properties

**Property 12: Search coverage**
*For any* search query string and any part where the query appears (case-insensitive) in the part's name, description, category name, or maker name, that part should appear in the search results
**Validates: Requirements 4.2**

**Property 13: Case-insensitive search**
*For any* search query string, searching with different case variations (uppercase, lowercase, mixed) should return identical result sets
**Validates: Requirements 4.6**

**Property 14: Category filter correctness**
*For any* selected category, all parts in the search results should belong to that category, and no parts from other categories should appear
**Validates: Requirements 6.2**

**Property 15: Combined filter correctness**
*For any* search query and selected category, all parts in the results should both (1) match the search query in at least one searchable field AND (2) belong to the selected category
**Validates: Requirements 6.3**

**Property 16: Empty search returns all parts**
*For any* empty or whitespace-only search query with no category filter, the search results should contain all available parts
**Validates: Requirements 4.5** (edge case)

### UI Display Properties

**Property 17: Part display completeness**
*For any* part displayed in search results, the rendered display should contain all of: part name, category name, maker name, price, and stock quantity
**Validates: Requirements 5.1**

**Property 18: Out-of-stock visual indication**
*For any* part with quantity = 0, the part's display should have distinct styling that differs from parts with quantity > 0
**Validates: Requirements 5.4**

**Property 19: Monetary value formatting**
*For any* monetary value displayed in the UI, the formatted string should include a currency symbol and exactly 2 decimal places
**Validates: Requirements 11.3**

### Inventory Validation Properties

**Property 20: Stock availability check on addition**
*For any* part and requested quantity, attempting to add the part to a bill should succeed if and only if the part's current stock quantity >= requested quantity
**Validates: Requirements 9.1, 9.2**

**Property 21: Stock availability check on completion**
*For any* bill with multiple commands, completing the bill should succeed if and only if, at the moment of completion, all parts have sufficient stock for their requested quantities
**Validates: Requirements 9.3, 9.4**

**Property 22: Zero-stock parts cannot be added**
*For any* part with quantity = 0, attempting to add it to a bill should be prevented and should display an error message
**Validates: Requirements 12.5**

### Part Selection Properties

**Property 23: Default price initialization**
*For any* part added to a bill, the initial priceConsidered value in the command should equal the part's current default price at the time of addition
**Validates: Requirements 12.2**

**Property 24: Part addition from search**
*For any* part clicked in search results (with quantity > 0), the part should be added to the current bill's command list
**Validates: Requirements 12.1**

### Payment Method Properties

**Property 25: Payment method mutability**
*For any* bill in progress (not yet completed), changing the payment method should be allowed and should update the bill's payment_method value
**Validates: Requirements 3.5**


## Error Handling

### Input Validation Errors

**Price Validation**:
- **Error**: Non-positive price entered
- **Handling**: Reject input, display error message "Price must be greater than zero", revert to previous value
- **User Feedback**: Red border on input field, error tooltip

**Discount Validation**:
- **Error**: Percentage discount > 100 or < 0
- **Handling**: Reject input, display error message "Percentage must be between 0 and 100"
- **User Feedback**: Red border on input field, error tooltip

- **Error**: Fixed discount exceeds subtotal
- **Handling**: Reject input, display error message "Discount cannot exceed subtotal of $XX.XX"
- **User Feedback**: Red border on input field, error tooltip

**Client Information Validation**:
- **Error**: Empty client name or phone
- **Handling**: Prevent bill completion, display error message "Client name and phone are required"
- **User Feedback**: Highlight empty required fields in red

### Inventory Errors

**Insufficient Stock on Addition**:
- **Error**: Requested quantity exceeds available stock
- **Handling**: Prevent addition, display error message "Insufficient stock. Available: XX units"
- **User Feedback**: Alert dialog with available quantity information

**Insufficient Stock on Completion**:
- **Error**: Stock depleted between addition and completion
- **Handling**: Reject transaction, display error message listing parts with insufficient stock
- **User Feedback**: Alert dialog with detailed stock information, allow user to adjust quantities

**Zero Stock Selection**:
- **Error**: User clicks on out-of-stock part
- **Handling**: Prevent addition, display error message "This part is currently out of stock"
- **User Feedback**: Toast notification or alert dialog

### Database Errors

**Connection Failure**:
- **Error**: Cannot connect to database
- **Handling**: Display error message "Database connection failed. Please try again."
- **User Feedback**: Alert dialog, log error details
- **Recovery**: Retry connection, suggest checking database file

**Transaction Rollback**:
- **Error**: Bill save fails mid-transaction
- **Handling**: Rollback all changes, display error message "Failed to save bill. No changes were made."
- **User Feedback**: Alert dialog with error details
- **Recovery**: Preserve bill data in UI, allow user to retry

**Constraint Violation**:
- **Error**: Invalid discount_type or payment_method value
- **Handling**: Reject save, display error message "Invalid data. Please check your selections."
- **User Feedback**: Alert dialog
- **Recovery**: Validate data before save attempt

### Search Errors

**Search Performance Degradation**:
- **Error**: Search takes longer than expected
- **Handling**: Display loading indicator, allow search to complete
- **User Feedback**: Progress spinner, "Searching..." message
- **Recovery**: Optimize query, check indexes

**No Results Found**:
- **Error**: Search returns no results
- **Handling**: Display "No parts found matching your search" message
- **User Feedback**: Empty state with helpful message
- **Recovery**: Suggest clearing filters or trying different search terms

### Concurrent Access Errors

**Stock Race Condition**:
- **Error**: Two users attempt to sell the last unit simultaneously
- **Handling**: First transaction succeeds, second fails with insufficient stock error
- **User Feedback**: Alert dialog explaining stock was sold by another user
- **Recovery**: Refresh part list, allow user to select alternative

## Testing Strategy

### Dual Testing Approach

The POS system will use a comprehensive testing strategy combining unit tests and property-based tests:

**Unit Tests**: Focus on specific examples, edge cases, and integration points
- Specific discount calculations (0%, 50%, 100%, edge values)
- Specific price edits and validations
- UI component initialization and state
- Database schema verification
- Error message content and formatting
- Integration between controller and services

**Property-Based Tests**: Verify universal properties across all inputs
- All 25 correctness properties defined above
- Minimum 100 iterations per property test
- Random generation of bills, commands, parts, discounts, prices
- Comprehensive input coverage through randomization

### Property-Based Testing Configuration

**Framework**: jqwik (Java property-based testing library)

**Test Configuration**:
```java
@Property(tries = 100)
void propertyTest(@ForAll /* generators */) {
    // Test implementation
}
```

**Generators Required**:
- `@ForAll @FloatRange(min = 0.01f, max = 10000.0f) float price`
- `@ForAll @IntRange(min = 1, max = 1000) int quantity`
- `@ForAll @FloatRange(min = 0.0f, max = 100.0f) float percentageDiscount`
- `@ForAll @StringLength(min = 1, max = 100) String searchQuery`
- `@ForAll Part part` (custom generator)
- `@ForAll Bill bill` (custom generator)
- `@ForAll List<Command> commands` (custom generator)

**Tag Format**: Each property test must include a comment tag:
```java
/**
 * Feature: point-of-sale-system, Property 1: Price validation
 * For any price input value, the system should accept it if and only if it is a positive number
 */
@Property(tries = 100)
void testPriceValidation(@ForAll float price) {
    // Test implementation
}
```

### Unit Test Coverage

**Model Tests**:
- Bill calculation methods (calculateFinalTotal, applyDiscount)
- Bill invariant validation
- Command subtotal calculation
- Default value initialization

**Service Tests**:
- BillService.saveBill with new fields
- BillService.mapResultSetToBill with NULL handling
- SearchService.searchParts with various queries
- SearchService.filterByCategory
- SearchService.searchWithFilters (combined)

**Controller Tests**:
- BillingController initialization
- Event handler registration
- Total calculation logic
- Discount application logic
- Payment method selection

**View Tests**:
- BillingView component initialization
- PartSearchComponent rendering
- Editable cell behavior
- UI state updates

**Database Tests**:
- Schema migration verification
- Index creation verification
- Constraint enforcement
- Default value application
- Backwards compatibility with existing records

### Integration Tests

**End-to-End Scenarios**:
1. Create bill with custom prices and percentage discount
2. Create bill with fixed discount and card payment
3. Search for parts, filter by category, add to bill
4. Attempt to add out-of-stock part (should fail)
5. Edit prices, verify subtotal updates
6. Change discount type, verify total updates
7. Complete sale, verify inventory reduction
8. Retrieve saved bill, verify all fields persisted

**UI Integration Tests**:
- Part search → selection → addition to bill
- Price edit → subtotal update → total update
- Discount change → total update → display update
- Payment method selection → visual feedback
- Complete sale → success message → form reset

### Test Data

**Test Database**:
- Separate SQLite database for testing
- Populated with known test data
- Reset before each test suite
- Includes parts with various stock levels (0, 1, 10, 100)
- Includes all categories and makers

**Test Fixtures**:
- Sample parts with known prices and stock
- Sample bills with various discount configurations
- Sample commands with custom prices
- Edge case data (zero stock, maximum values, minimum values)

### Performance Testing

**Search Performance**:
- Verify search completes within 500ms for 10,000 parts
- Test with various query lengths and complexities
- Verify index usage with EXPLAIN QUERY PLAN

**UI Responsiveness**:
- Verify real-time updates complete within 100ms
- Test with large command lists (100+ items)
- Verify no UI freezing during calculations

### Regression Testing

**Backwards Compatibility**:
- Verify existing bills display correctly
- Verify existing reports work with new schema
- Verify existing services continue to function
- Test with actual production database backup (anonymized)

**Existing Functionality**:
- Verify inventory reduction still works
- Verify bill history retrieval still works
- Verify existing validation logic preserved
- Verify DatabaseConnection singleton pattern maintained

## Implementation Notes

### Database Migration Strategy

1. **Backup**: Create backup of existing database before migration
2. **Schema Update**: Execute ALTER TABLE statements to add new columns
3. **Index Creation**: Create indexes on parts.name and parts.catid
4. **Verification**: Query schema to verify changes applied
5. **Data Validation**: Verify existing records still accessible
6. **Rollback Plan**: Keep backup for rollback if issues occur

### UI Component Reuse

- Leverage existing JavaFX controls (TextField, ComboBox, RadioButton)
- Extend existing BillingView rather than replacing
- Reuse existing styling and themes
- Maintain consistent look and feel with rest of application

### Service Layer Extensions

- Extend BillService with minimal changes
- Create new SearchService for search logic
- Maintain existing service patterns and conventions
- Preserve existing error handling and logging

### Performance Considerations

- Use database indexes for search queries
- Implement debouncing for real-time search (300ms delay)
- Cache category list (rarely changes)
- Use JavaFX observable collections for efficient UI updates
- Batch database operations where possible

### Internationalization

- Use LocaleManager for all user-facing strings
- Add new message keys for POS features
- Support existing language configurations
- Maintain consistent terminology

### Logging

- Log all bill creation and completion events
- Log discount applications with details
- Log inventory validation failures
- Log search performance metrics
- Use SLF4J with existing logging configuration

### Security Considerations

- Validate all user inputs before processing
- Use prepared statements for all SQL queries (prevent SQL injection)
- Maintain existing authentication and authorization
- Log all financial transactions for audit trail
- Validate discount amounts to prevent negative totals

## Dependencies

### Existing Dependencies (from pom.xml)
- JavaFX 17+
- SQLite JDBC driver
- SLF4J for logging
- JUnit 5 for testing

### New Dependencies Required
- **jqwik**: Property-based testing framework for Java
  ```xml
  <dependency>
      <groupId>net.jqwik</groupId>
      <artifactId>jqwik</artifactId>
      <version>1.7.4</version>
      <scope>test</scope>
  </dependency>
  ```

### No Additional Runtime Dependencies
- All POS features use existing libraries
- No new external services required
- No new database engines required

## Deployment Considerations

### Database Migration
- Migration can be performed on application startup
- Check if new columns exist before attempting ALTER TABLE
- Log migration success/failure
- No downtime required (backwards compatible)

### Rollback Strategy
- Keep database backup before migration
- New columns have default values (safe to add)
- Can remove new columns if needed (data loss acceptable for new features)
- Application works with or without new columns (graceful degradation)

### User Training
- Document new POS features in user manual
- Provide screenshots of new UI components
- Explain discount types and payment methods
- Highlight editable price feature

### Monitoring
- Monitor search query performance
- Track discount usage patterns
- Monitor payment method distribution
- Alert on inventory validation failures
