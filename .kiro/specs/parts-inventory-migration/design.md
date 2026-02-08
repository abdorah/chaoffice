# Design Document: Parts Inventory Migration

## Overview

This design document outlines the architecture and implementation approach for migrating the parts inventory management system into ChaOffice. The migration transforms an FXML-based application into a pure Java implementation while modernizing the UI with Material Design 3 principles and maintaining ChaOffice's modular architecture.

### Key Design Goals

1. **Pure Java UI**: All views created programmatically using JavaFX layouts (no FXML)
2. **Modular Architecture**: Clean separation of concerns with controller/, service/, view/, model/, util/ packages
3. **Material Design 3**: Modern, responsive UI with consistent styling
4. **Database Integration**: SQLite for reliable data persistence
5. **Internationalization**: Support for English, French, and Arabic
6. **PDF Generation**: Professional reports using OpenPDF
7. **Maintainability**: Clean code with proper separation of concerns

### Technology Stack

- **Java**: 17
- **JavaFX**: 22.0.2 (controls, web, swing, media modules)
- **Database**: SQLite JDBC 3.44.1.0
- **PDF**: OpenPDF 2.0.2
- **Logging**: SLF4J 2.0.9
- **Build**: Maven

## Architecture

### High-Level Architecture

The application follows a layered MVC architecture:

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (Views - Pure Java UI Components)     │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│         Controller Layer                │
│  (Controllers - Scene Management)       │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│         Service Layer                   │
│  (Business Logic & Data Access)         │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│         Data Layer                      │
│  (SQLite Database)                      │
└─────────────────────────────────────────┘
```


### Package Structure

```
org.chaos.office/
├── ChaOfficeApplication.java          # Main application entry point
├── controller/                        # Scene controllers
│   ├── LoginController.java
│   ├── DashboardController.java
│   ├── PartsInventoryController.java
│   ├── CategoryManagementController.java
│   ├── BillingController.java
│   ├── BillsHistoryController.java
│   ├── SalesAnalyticsController.java
│   └── SettingsController.java
├── view/                              # Pure Java UI views
│   ├── LoginView.java
│   ├── DashboardView.java
│   ├── PartsInventoryView.java
│   ├── CategoryManagementView.java
│   ├── BillingView.java
│   ├── BillsHistoryView.java
│   ├── SalesAnalyticsView.java
│   └── SettingsView.java
├── model/                             # Domain models
│   ├── User.java
│   ├── Part.java
│   ├── Category.java
│   ├── Maker.java
│   ├── Bill.java
│   └── Command.java
├── service/                           # Business logic services
│   ├── AuthenticationService.java
│   ├── PartService.java
│   ├── CategoryService.java
│   ├── BillService.java
│   ├── SalesService.java
│   ├── ReportService.java
│   └── DatabaseService.java
└── util/                              # Utility classes
    ├── SessionManager.java
    ├── LocaleManager.java
    ├── DatabaseConnection.java
    ├── AlertHelper.java
    ├── ValidationHelper.java
    └── ImageHelper.java
```

### Navigation Flow

```
Login → Dashboard → [Parts Inventory | Categories | Billing | Bills History | Sales Analytics | Settings]
                                                                                                    ↓
                                                                                                 Logout → Login
```


## Components and Interfaces

### 1. Application Entry Point

**ChaOfficeApplication**
- Extends `javafx.application.Application`
- Initializes database on startup
- Loads initial scene (LoginController)
- Configures primary stage properties

```java
public class ChaOfficeApplication extends Application {
    @Override
    public void start(Stage primaryStage) {
        // Initialize database
        // Load locale settings
        // Set up primary stage
        // Show login scene
    }
}
```

### 2. View Layer (Pure Java UI)

All views extend JavaFX layout containers and create UI programmatically.

**LoginView** (extends VBox)
- Username TextField
- Password PasswordField
- Login Button
- Language selector ComboBox
- Styled with Material Design 3 principles

**DashboardView** (extends BorderPane)
- Top: Header with app title, user info, logout button
- Left: Navigation sidebar with menu items
- Center: Content area (dynamically loaded)
- Responsive layout

**PartsInventoryView** (extends BorderPane)
- Top: Search bar and filter controls
- Center: TableView with parts data
- Bottom: Action buttons (Add, Edit, Delete)
- Columns: ID, Name, Maker, Description, Price, Quantity, Category

**CategoryManagementView** (extends BorderPane)
- Center: GridPane or FlowPane with category cards
- Each card shows: icon, name, description
- Action buttons: Add, Edit, Delete

**BillingView** (extends BorderPane)
- Top: Client information form (name, phone)
- Center: TableView for selected parts
- Right: Part selection panel
- Bottom: Total price display and Complete Sale button

**BillsHistoryView** (extends BorderPane)
- Top: Date range filter
- Center: TableView with bills
- Bottom: Action buttons (View Details, Generate PDF)

**SalesAnalyticsView** (extends BorderPane)
- Top: Date range selector
- Center: Charts (LineChart for sales over time)
- Right: Summary statistics panel

**SettingsView** (extends VBox)
- Language selection
- Database configuration
- Theme selection (future enhancement)
- Save button


### 3. Controller Layer

Controllers extend `javafx.scene.Scene` and manage view lifecycle.

**LoginController**
- Creates LoginView
- Handles authentication via AuthenticationService
- Navigates to DashboardController on success
- Displays error alerts on failure

**DashboardController**
- Creates DashboardView
- Manages navigation between different sections
- Updates center content based on menu selection
- Maintains reference to current user session

**PartsInventoryController**
- Creates PartsInventoryView
- Loads parts data via PartService
- Handles CRUD operations
- Manages search and filter logic

**CategoryManagementController**
- Creates CategoryManagementView
- Loads categories via CategoryService
- Handles category CRUD operations
- Manages image upload for category icons

**BillingController**
- Creates BillingView
- Manages bill creation workflow
- Validates inventory availability
- Calculates totals
- Saves bill via BillService

**BillsHistoryController**
- Creates BillsHistoryView
- Loads bills via BillService
- Handles filtering by date range
- Triggers PDF generation via ReportService

**SalesAnalyticsController**
- Creates SalesAnalyticsView
- Loads sales data via SalesService
- Generates charts
- Calculates statistics

**SettingsController**
- Creates SettingsView
- Loads current settings
- Saves settings via LocaleManager and preferences
- Applies language changes


### 4. Service Layer

Services encapsulate business logic and data access.

**AuthenticationService**
```java
public class AuthenticationService {
    public Optional<User> authenticate(String username, String password);
    public boolean validateCredentials(String username, String password);
    public void logout();
}
```

**PartService**
```java
public class PartService {
    public List<Part> getAllParts();
    public Optional<Part> getPartById(int id);
    public void savePart(Part part);
    public void updatePart(Part part);
    public void deletePart(int id);
    public List<Part> searchParts(String query);
    public List<Part> filterByCategory(int categoryId);
}
```

**CategoryService**
```java
public class CategoryService {
    public List<Category> getAllCategories();
    public Optional<Category> getCategoryById(int id);
    public void saveCategory(Category category);
    public void updateCategory(Category category);
    public void deleteCategory(int id);
    public boolean hasParts(int categoryId);
}
```

**BillService**
```java
public class BillService {
    public List<Bill> getAllBills();
    public Optional<Bill> getBillById(int id);
    public void saveBill(Bill bill, List<Command> commands);
    public List<Command> getCommandsForBill(int billId);
    public List<Bill> filterByDateRange(LocalDate start, LocalDate end);
}
```

**SalesService**
```java
public class SalesService {
    public Map<LocalDate, Double> getSalesByDate(LocalDate start, LocalDate end);
    public double getTotalRevenue(LocalDate start, LocalDate end);
    public int getTransactionCount(LocalDate start, LocalDate end);
}
```

**ReportService**
```java
public class ReportService {
    public void generateBillPDF(Bill bill, List<Command> commands, File outputFile);
}
```

**DatabaseService**
```java
public class DatabaseService {
    public void initializeDatabase();
    public void executeDDL(String sqlScript);
}
```


### 5. Utility Classes

**SessionManager**
- Singleton pattern
- Stores current authenticated user
- Provides access to user information throughout the application

```java
public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    
    public static SessionManager getInstance();
    public void setCurrentUser(User user);
    public User getCurrentUser();
    public boolean isAuthenticated();
    public void clearSession();
}
```

**LocaleManager**
- Manages application locale
- Loads ResourceBundles for different languages
- Provides translated strings

```java
public class LocaleManager {
    private static Locale currentLocale;
    private static ResourceBundle bundle;
    
    public static void setLocale(Locale locale);
    public static String getString(String key);
    public static Locale getCurrentLocale();
}
```

**DatabaseConnection**
- Manages SQLite connection
- Provides connection pooling or single connection management
- Handles connection lifecycle

```java
public class DatabaseConnection {
    private static Connection connection;
    
    public static Connection getConnection();
    public static void closeConnection();
    public static void initializeDatabase();
}
```

**AlertHelper**
- Utility for displaying alerts and dialogs
- Consistent alert styling

```java
public class AlertHelper {
    public static void showError(String title, String message);
    public static void showInfo(String title, String message);
    public static boolean showConfirmation(String title, String message);
}
```

**ValidationHelper**
- Input validation utilities
- Common validation patterns

```java
public class ValidationHelper {
    public static boolean isValidEmail(String email);
    public static boolean isValidPhone(String phone);
    public static boolean isPositiveNumber(String value);
    public static boolean isNotEmpty(String value);
}
```

**ImageHelper**
- Image loading and conversion utilities
- Converts between byte[] and JavaFX Image

```java
public class ImageHelper {
    public static Image byteArrayToImage(byte[] bytes);
    public static byte[] imageToByteArray(Image image);
    public static byte[] fileToByteArray(File file);
}
```


## Data Models

All models use JavaFX properties for UI binding.

### User Model

```java
public class User {
    private IntegerProperty id;
    private StringProperty username;
    private StringProperty password;
    private StringProperty role;  // admin, user, guest
    private StringProperty firstName;
    private StringProperty lastName;
    
    // Constructors, getters, setters, property accessors
}
```

### Part Model

```java
public class Part {
    private IntegerProperty id;
    private StringProperty name;
    private StringProperty maker;
    private StringProperty description;
    private FloatProperty price;
    private IntegerProperty quantity;
    private ObjectProperty<Category> category;
    
    // Constructors, getters, setters, property accessors
}
```

### Category Model

```java
public class Category {
    private IntegerProperty id;
    private StringProperty name;
    private StringProperty description;
    private ObjectProperty<byte[]> image;
    
    // Constructors, getters, setters, property accessors
}
```

### Maker Model

```java
public class Maker {
    private IntegerProperty id;
    private StringProperty name;
    
    // Constructors, getters, setters, property accessors
}
```

### Bill Model

```java
public class Bill {
    private IntegerProperty id;
    private StringProperty clientName;
    private StringProperty clientPhone;
    private FloatProperty totalPrice;
    private ObjectProperty<LocalDate> date;
    
    // Constructors, getters, setters, property accessors
}
```

### Command Model

```java
public class Command {
    private IntegerProperty billId;
    private IntegerProperty partId;
    private IntegerProperty quantity;
    private FloatProperty priceConsidered;
    
    // Constructors, getters, setters, property accessors
}
```


### Database Schema

```sql
-- Users table
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('admin', 'user', 'guest')),
    firstname TEXT,
    lastname TEXT
);

-- Makers table
CREATE TABLE IF NOT EXISTS makers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

-- Categories table
CREATE TABLE IF NOT EXISTS categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    image BLOB
);

-- Parts table
CREATE TABLE IF NOT EXISTS parts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    maker_id INTEGER NOT NULL,
    description TEXT NOT NULL,
    image BLOB,
    price REAL NOT NULL CHECK (price >= 0),
    quantity INTEGER NOT NULL CHECK (quantity >= 0),
    catid INTEGER NOT NULL,
    FOREIGN KEY (catid) REFERENCES categories(id),
    FOREIGN KEY (maker_id) REFERENCES makers(id)
);

-- Bills table
CREATE TABLE IF NOT EXISTS bills (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    totalprice REAL NOT NULL CHECK (totalprice >= 0),
    clientname TEXT NOT NULL,
    clientphone TEXT NOT NULL,
    date TEXT NOT NULL
);

-- Commands table (bill line items)
CREATE TABLE IF NOT EXISTS commands (
    billid INTEGER NOT NULL,
    partid INTEGER NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    priceconsidered REAL NOT NULL CHECK (priceconsidered >= 0),
    PRIMARY KEY (billid, partid),
    FOREIGN KEY (partid) REFERENCES parts(id),
    FOREIGN KEY (billid) REFERENCES bills(id)
);
```


## Correctness Properties

A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.

### Property 1: Authentication Success and Failure

*For any* user credentials, authentication should succeed if and only if the credentials match a valid user in the database, and upon success, the session should contain the authenticated user's information.

**Validates: Requirements 3.2, 3.3, 3.4**

### Property 2: Session Logout Clears State

*For any* authenticated session, after logout is called, the session should be cleared and no user information should be accessible.

**Validates: Requirements 3.6**

### Property 3: User Persistence Round Trip

*For any* valid user, after saving to the database and then retrieving by username, the retrieved user should have the same properties as the original user.

**Validates: Requirements 3.7**

### Property 4: Part Validation Rejects Invalid Data

*For any* part with missing required fields (name, maker, price, quantity, category) or negative price/quantity values, validation should fail and the part should not be saved.

**Validates: Requirements 4.2, 4.8**

### Property 5: Part Persistence Round Trip

*For any* valid part, after saving to the database and then retrieving by ID, the retrieved part should have the same properties as the original part.

**Validates: Requirements 4.3, 4.5**

### Property 6: Part Search Returns Matching Results

*For any* search query, all returned parts should contain the query string in their name, maker, or description fields (case-insensitive).

**Validates: Requirements 4.7**

### Property 7: Category Icon Persistence Round Trip

*For any* category with an icon image, after saving to the database and then retrieving by ID, the retrieved category's icon should be identical to the original icon bytes.

**Validates: Requirements 5.3**

### Property 8: Category Deletion Prevention with Parts

*For any* category that has parts assigned to it, attempting to delete the category should fail and the category should remain in the database.

**Validates: Requirements 5.5**

### Property 9: Image Format Support

*For any* valid PNG or JPG image file, the system should successfully convert it to byte array format for storage.

**Validates: Requirements 5.7**

### Property 10: Bill Quantity Validation

*For any* part being added to a bill, if the requested quantity exceeds the available inventory quantity, the addition should fail with a validation error.

**Validates: Requirements 6.3**

### Property 11: Bill Price Recording

*For any* part added to a bill, the price considered recorded in the command should equal the part's current price at the time of addition.

**Validates: Requirements 6.4**

### Property 12: Bill Total Calculation

*For any* bill with commands, the total price should equal the sum of (quantity × priceConsidered) for all commands in the bill.

**Validates: Requirements 6.5**

### Property 13: Inventory Reduction on Sale

*For any* part sold in a completed bill, the part's inventory quantity should decrease by exactly the quantity sold.

**Validates: Requirements 6.6**

### Property 14: Bill Persistence Round Trip

*For any* valid bill with commands, after saving to the database and then retrieving by ID, the retrieved bill and its commands should match the original data.

**Validates: Requirements 6.7**

### Property 15: Bill Date Assignment

*For any* bill created, the bill should have a date property set to the current date.

**Validates: Requirements 6.9**

### Property 16: Sales Date Range Filtering

*For any* date range query, all returned bills should have dates that fall within the specified start and end dates (inclusive).

**Validates: Requirements 7.2**

### Property 17: Revenue Calculation Accuracy

*For any* date range, the calculated total revenue should equal the sum of all bill total prices within that date range.

**Validates: Requirements 7.3**

### Property 18: Transaction Count Accuracy

*For any* date range, the transaction count should equal the number of bills within that date range.

**Validates: Requirements 7.4**

### Property 19: PDF Report Completeness

*For any* bill, the generated PDF report should contain the client name, client phone, date, all command details (part name, quantity, price), and the total price.

**Validates: Requirements 8.2**

### Property 20: Language ResourceBundle Loading

*For any* supported language (English, French, Arabic), selecting that language should load the corresponding ResourceBundle and make translated strings accessible.

**Validates: Requirements 9.2**

### Property 21: Language Preference Persistence

*For any* language selection, after saving the preference and restarting the application, the loaded language should match the saved preference.

**Validates: Requirements 9.4**

### Property 22: Database Error Handling

*For any* database operation that encounters an error (connection failure, constraint violation, etc.), the system should catch the exception, log it, and display a user-friendly error message without crashing.

**Validates: Requirements 10.7**

### Property 23: Database Transaction Atomicity

*For any* multi-table operation (e.g., saving a bill with commands), if any part of the operation fails, all changes should be rolled back and the database should remain in a consistent state.

**Validates: Requirements 10.8**

### Property 24: Input Validation Error Display

*For any* invalid input (wrong data type, out of range, etc.), the system should display a validation error message and prevent the invalid data from being saved.

**Validates: Requirements 15.1, 15.3**


## Error Handling

### Error Handling Strategy

The application implements a layered error handling approach:

1. **Validation Layer**: Input validation at the UI level before data reaches services
2. **Service Layer**: Business logic validation and exception handling
3. **Data Layer**: Database error handling with proper transaction management
4. **Global Handler**: Catch-all for unexpected exceptions

### Error Categories

**Validation Errors**
- Missing required fields
- Invalid data types
- Out-of-range values
- Business rule violations
- Display inline error messages near the input field
- Prevent form submission until resolved

**Database Errors**
- Connection failures
- Constraint violations
- Transaction failures
- Log error details with SLF4J
- Display user-friendly message via AlertHelper
- Attempt recovery where possible (e.g., retry connection)

**Business Logic Errors**
- Insufficient inventory
- Category has dependent parts
- Invalid state transitions
- Display clear error message explaining the issue
- Suggest corrective action

**Unexpected Errors**
- Null pointer exceptions
- Runtime exceptions
- Log full stack trace
- Display generic error dialog
- Prevent application crash
- Allow user to continue or restart

### Exception Hierarchy

```java
// Custom exceptions
public class ValidationException extends Exception { }
public class DatabaseException extends Exception { }
public class BusinessRuleException extends Exception { }
public class InsufficientInventoryException extends BusinessRuleException { }
```

### Logging Strategy

- Use SLF4J for all logging
- Log levels:
  - ERROR: Database errors, unexpected exceptions
  - WARN: Business rule violations, validation failures
  - INFO: User actions, state changes
  - DEBUG: Detailed operation flow (development only)


## Testing Strategy

### Dual Testing Approach

The application requires both unit testing and property-based testing for comprehensive coverage:

**Unit Tests**
- Verify specific examples and edge cases
- Test integration points between components
- Test error conditions and exception handling
- Focus on concrete scenarios

**Property-Based Tests**
- Verify universal properties across all inputs
- Use randomized input generation
- Minimum 100 iterations per property test
- Focus on general correctness

Together, unit tests catch concrete bugs while property tests verify general correctness across the input space.

### Property-Based Testing Configuration

**Library Selection**: Use **jqwik** for Java property-based testing
- Modern, well-maintained library for Java
- Integrates with JUnit 5
- Supports custom generators
- Provides shrinking for minimal failing examples

**Test Configuration**
```java
@Property(tries = 100)
void propertyTest(@ForAll Generator input) {
    // Test implementation
}
```

**Test Tagging**
Each property test must reference its design document property:
```java
// Feature: parts-inventory-migration, Property 5: Part Persistence Round Trip
@Property(tries = 100)
void testPartPersistenceRoundTrip(@ForAll Part part) {
    // Test implementation
}
```

### Test Coverage Areas

**Model Tests**
- Property accessors and mutators
- JavaFX property binding
- Validation logic

**Service Tests**
- CRUD operations
- Business logic
- Database interactions
- Error handling

**Utility Tests**
- SessionManager state management
- LocaleManager language switching
- DatabaseConnection lifecycle
- ValidationHelper rules
- ImageHelper conversions

**Integration Tests**
- End-to-end workflows
- Multi-service operations
- Database transactions
- PDF generation

### Testing Guidelines

1. **Unit tests should focus on**:
   - Specific examples that demonstrate correct behavior
   - Edge cases (empty strings, null values, boundary conditions)
   - Error conditions (invalid input, database failures)
   - Integration between components

2. **Property tests should focus on**:
   - Universal properties from the Correctness Properties section
   - Round-trip properties (save/load, serialize/deserialize)
   - Invariants (constraints that always hold)
   - Metamorphic properties (relationships between operations)

3. **Avoid over-testing**:
   - Don't write excessive unit tests for scenarios covered by property tests
   - Property tests handle comprehensive input coverage
   - Unit tests complement with specific scenarios

### Test Data Management

**Test Database**
- Use in-memory SQLite database for tests
- Reset database state between tests
- Seed with minimal test data

**Test Fixtures**
- Create builder classes for test data generation
- Use factories for common test objects
- Implement custom jqwik generators for domain models

