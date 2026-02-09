# Implementation Plan: Parts Inventory Migration

## Overview

This implementation plan breaks down the migration of the parts inventory management system into ChaOffice into discrete, incremental coding tasks. Each task builds on previous work, with regular checkpoints to ensure quality and catch issues early.

## Tasks

- [x] 1. Set up project dependencies and database infrastructure
  - Update pom.xml with SQLite JDBC, OpenPDF, and jqwik dependencies
  - Update module-info.java to require necessary modules (java.sql, org.xerial.sqlitejdbc, com.github.librepdf.openpdf)
  - Create database initialization SQL script (ddl.sql) in resources/sql/
  - _Requirements: 10.1, 10.2, 16.1, 16.2, 16.3, 16.4, 16.5, 16.6_

- [ ] 2. Create domain models with JavaFX properties
  - [x] 2.1 Create User model with IntegerProperty, StringProperty for all fields
    - Include id, username, password, role, firstName, lastName
    - Implement constructors, getters, setters, and property accessors
    - _Requirements: 3.4, 3.5_

  - [x] 2.2 Create Part model with properties
    - Include id, name, maker, description, price, quantity, category
    - Use FloatProperty for price, ObjectProperty<Category> for category
    - _Requirements: 4.1_

  - [x] 2.3 Create Category model with properties
    - Include id, name, description, image (ObjectProperty<byte[]>)
    - _Requirements: 5.1_

  - [x] 2.4 Create Maker model with properties
    - Include id, name
    - _Requirements: 4.1_

  - [x] 2.5 Create Bill model with properties
    - Include id, clientName, clientPhone, totalPrice, date (ObjectProperty<LocalDate>)
    - _Requirements: 6.1, 6.8_

  - [x] 2.6 Create Command model with properties
    - Include billId, partId, quantity, priceConsidered
    - _Requirements: 6.2_

- [ ] 3. Implement utility classes
  - [x] 3.1 Create DatabaseConnection utility
    - Implement singleton pattern for connection management
    - Add getConnection(), closeConnection(), initializeDatabase() methods
    - Load database from resources or create in user directory
    - _Requirements: 10.3, 10.6_

  - [x] 3.2 Create SessionManager utility
    - Implement singleton pattern
    - Add setCurrentUser(), getCurrentUser(), isAuthenticated(), clearSession() methods
    - _Requirements: 3.4, 3.6_

  - [x] 3.3 Create LocaleManager utility
    - Add setLocale(), getString(), getCurrentLocale() methods
    - Load ResourceBundles from resources/messages/
    - Support English, French, Arabic
    - _Requirements: 9.1, 9.2, 9.4_

  - [x] 3.4 Create AlertHelper utility
    - Add showError(), showInfo(), showConfirmation() methods
    - Apply consistent styling to alerts
    - _Requirements: 15.1, 15.5_

  - [x] 3.5 Create ValidationHelper utility
    - Add isNotEmpty(), isPositiveNumber(), isValidPhone() methods
    - _Requirements: 15.2, 15.3_

  - [x] 3.6 Create ImageHelper utility
    - Add byteArrayToImage(), imageToByteArray(), fileToByteArray() methods
    - Support PNG and JPG formats
    - _Requirements: 5.7_

- [x] 4. Checkpoint - Verify utilities and models compile
  - Ensure all utilities and models compile without errors
  - Verify database connection can be established
  - Ask the user if questions arise


- [ ] 5. Implement service layer - Authentication and User management
  - [x] 5.1 Create AuthenticationService
    - Implement authenticate(username, password) method with database query
    - Implement validateCredentials() method
    - Implement logout() method that clears session
    - _Requirements: 3.2, 3.3, 3.6_

  - [ ]* 5.2 Write property test for authentication
    - **Property 1: Authentication Success and Failure**
    - **Validates: Requirements 3.2, 3.3, 3.4**

  - [ ]* 5.3 Write property test for session logout
    - **Property 2: Session Logout Clears State**
    - **Validates: Requirements 3.6**

  - [ ]* 5.4 Write property test for user persistence
    - **Property 3: User Persistence Round Trip**
    - **Validates: Requirements 3.7**

- [ ] 6. Implement service layer - Part management
  - [x] 6.1 Create PartService
    - Implement getAllParts(), getPartById(), savePart(), updatePart(), deletePart() methods
    - Implement searchParts() with filtering by name, maker, or description
    - Implement filterByCategory() method
    - Add validation for required fields and non-negative values
    - _Requirements: 4.2, 4.3, 4.5, 4.7, 4.8_

  - [ ]* 6.2 Write property test for part validation
    - **Property 4: Part Validation Rejects Invalid Data**
    - **Validates: Requirements 4.2, 4.8**

  - [ ]* 6.3 Write property test for part persistence
    - **Property 5: Part Persistence Round Trip**
    - **Validates: Requirements 4.3, 4.5**

  - [ ]* 6.4 Write property test for part search
    - **Property 6: Part Search Returns Matching Results**
    - **Validates: Requirements 4.7**

- [ ] 7. Implement service layer - Category management
  - [x] 7.1 Create CategoryService
    - Implement getAllCategories(), getCategoryById(), saveCategory(), updateCategory(), deleteCategory() methods
    - Implement hasParts() method to check for dependent parts
    - Prevent deletion if category has parts
    - _Requirements: 5.3, 5.5_

  - [ ]* 7.2 Write property test for category icon persistence
    - **Property 7: Category Icon Persistence Round Trip**
    - **Validates: Requirements 5.3**

  - [ ]* 7.3 Write property test for category deletion prevention
    - **Property 8: Category Deletion Prevention with Parts**
    - **Validates: Requirements 5.5**

  - [ ]* 7.4 Write property test for image format support
    - **Property 9: Image Format Support**
    - **Validates: Requirements 5.7**

- [ ] 8. Checkpoint - Verify services work correctly
  - Run all property tests
  - Verify database operations work correctly
  - Ensure all tests pass, ask the user if questions arise


- [ ] 9. Implement service layer - Billing and Sales
  - [x] 9.1 Create BillService
    - Implement getAllBills(), getBillById(), saveBill() methods
    - Implement getCommandsForBill() method
    - Implement filterByDateRange() method
    - Add transaction support for saving bill with commands
    - Validate inventory availability before completing bill
    - Reduce inventory quantities when bill is completed
    - _Requirements: 6.3, 6.6, 6.7, 6.9, 7.2_

  - [ ]* 9.2 Write property test for bill quantity validation
    - **Property 10: Bill Quantity Validation**
    - **Validates: Requirements 6.3**

  - [ ]* 9.3 Write property test for bill price recording
    - **Property 11: Bill Price Recording**
    - **Validates: Requirements 6.4**

  - [ ]* 9.4 Write property test for bill total calculation
    - **Property 12: Bill Total Calculation**
    - **Validates: Requirements 6.5**

  - [ ]* 9.5 Write property test for inventory reduction
    - **Property 13: Inventory Reduction on Sale**
    - **Validates: Requirements 6.6**

  - [ ]* 9.6 Write property test for bill persistence
    - **Property 14: Bill Persistence Round Trip**
    - **Validates: Requirements 6.7**

  - [ ]* 9.7 Write property test for bill date assignment
    - **Property 15: Bill Date Assignment**
    - **Validates: Requirements 6.9**

- [ ] 10. Implement service layer - Sales Analytics
  - [x] 10.1 Create SalesService
    - Implement getSalesByDate() method returning Map<LocalDate, Double>
    - Implement getTotalRevenue() method
    - Implement getTransactionCount() method
    - _Requirements: 7.2, 7.3, 7.4_

  - [ ]* 10.2 Write property test for date range filtering
    - **Property 16: Sales Date Range Filtering**
    - **Validates: Requirements 7.2**

  - [ ]* 10.3 Write property test for revenue calculation
    - **Property 17: Revenue Calculation Accuracy**
    - **Validates: Requirements 7.3**

  - [ ]* 10.4 Write property test for transaction count
    - **Property 18: Transaction Count Accuracy**
    - **Validates: Requirements 7.4**

- [ ] 11. Implement service layer - Report Generation
  - [x] 11.1 Create ReportService
    - Implement generateBillPDF() method using OpenPDF
    - Include client information, date, parts list, and total
    - Format PDF professionally with proper layout
    - _Requirements: 8.2, 8.3_

  - [ ]* 11.2 Write property test for PDF completeness
    - **Property 19: PDF Report Completeness**
    - **Validates: Requirements 8.2**

- [ ] 12. Implement service layer - Database initialization
  - [x] 12.1 Create DatabaseService
    - Implement initializeDatabase() method
    - Implement executeDDL() method to run SQL scripts
    - Create tables if they don't exist
    - Seed with default admin user
    - _Requirements: 10.2, 10.3, 10.4_

  - [ ]* 12.2 Write property test for database error handling
    - **Property 22: Database Error Handling**
    - **Validates: Requirements 10.7**

  - [ ]* 12.3 Write property test for transaction atomicity
    - **Property 23: Database Transaction Atomicity**
    - **Validates: Requirements 10.8**

- [ ] 13. Checkpoint - Verify all services complete
  - Run all property tests
  - Verify all services integrate correctly
  - Ensure all tests pass, ask the user if questions arise


- [ ] 14. Create internationalization resources
  - [x] 14.1 Create messages_en_US.properties
    - Add all UI strings in English
    - Include labels, buttons, error messages, validation messages
    - _Requirements: 9.1, 9.6_

  - [x] 14.2 Create messages_fr.properties
    - Translate all strings to French
    - _Requirements: 9.1, 9.6_

  - [x] 14.3 Create messages_ar.properties
    - Translate all strings to Arabic
    - _Requirements: 9.1, 9.6_

  - [ ]* 14.4 Write property test for ResourceBundle loading
    - **Property 20: Language ResourceBundle Loading**
    - **Validates: Requirements 9.2**

  - [ ]* 14.5 Write property test for language preference persistence
    - **Property 21: Language Preference Persistence**
    - **Validates: Requirements 9.4**

- [ ] 15. Create Material Design 3 CSS stylesheet
  - [x] 15.1 Update main.css with Material Design 3 styles
    - Define color palette (primary, secondary, surface, background)
    - Style buttons with rounded corners and elevation
    - Style text fields with underline and focus effects
    - Style tables with alternating row colors
    - Style cards with elevation and rounded corners
    - Add hover, focus, and active states for interactive elements
    - Define spacing and padding variables
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7_

- [ ] 16. Implement view layer - Login
  - [x] 16.1 Create LoginView (extends VBox)
    - Add username TextField with label
    - Add password PasswordField with label
    - Add login Button
    - Add language selector ComboBox (English, French, Arabic)
    - Apply Material Design 3 styling
    - Center content vertically and horizontally
    - _Requirements: 1.2, 1.3, 3.1, 9.1_

  - [x] 16.2 Create LoginController (extends Scene)
    - Create LoginView instance
    - Handle login button click
    - Call AuthenticationService.authenticate()
    - On success: set session and navigate to DashboardController
    - On failure: display error via AlertHelper
    - Handle language selection changes via LocaleManager
    - _Requirements: 3.2, 3.3, 9.2_

- [ ] 17. Implement view layer - Dashboard
  - [x] 17.1 Create DashboardView (extends BorderPane)
    - Top: Header HBox with app title, user info label, logout button
    - Left: Navigation VBox with menu buttons (Parts, Categories, Billing, Bills History, Analytics, Settings)
    - Center: StackPane for dynamic content
    - Apply Material Design 3 styling
    - Make responsive to window resizing
    - _Requirements: 1.2, 1.3, 14.1, 14.2, 14.6, 14.7_

  - [x] 17.2 Create DashboardController (extends Scene)
    - Create DashboardView instance
    - Handle navigation button clicks
    - Load appropriate controller in center pane
    - Handle logout button click (clear session, return to login)
    - Display current user name and role
    - _Requirements: 14.1, 14.2, 14.7, 3.6_


- [ ] 18. Implement view layer - Parts Inventory
  - [x] 18.1 Create PartsInventoryView (extends BorderPane)
    - Top: HBox with search TextField and filter ComboBox (by category)
    - Center: TableView<Part> with columns (ID, Name, Maker, Description, Price, Quantity, Category)
    - Bottom: HBox with Add, Edit, Delete buttons
    - Apply Material Design 3 styling
    - _Requirements: 1.2, 1.3, 4.1, 13.1_

  - [x] 18.2 Create PartsInventoryController (extends Scene)
    - Create PartsInventoryView instance
    - Load parts data via PartService
    - Bind TableView to ObservableList<Part>
    - Handle search text changes (filter parts in real-time)
    - Handle Add button (show dialog for new part)
    - Handle Edit button (show dialog with selected part)
    - Handle Delete button (confirm and delete via PartService)
    - Validate input using ValidationHelper
    - Display errors via AlertHelper
    - _Requirements: 4.2, 4.3, 4.5, 4.6, 4.7, 13.2, 13.4, 15.1_

  - [ ]* 18.3 Write property test for input validation
    - **Property 24: Input Validation Error Display**
    - **Validates: Requirements 15.1, 15.3**

- [ ] 19. Implement view layer - Category Management
  - [x] 19.1 Create CategoryManagementView (extends BorderPane)
    - Center: GridPane or FlowPane with category cards
    - Each card: ImageView (icon), Label (name), Label (description)
    - Bottom: HBox with Add, Edit, Delete buttons
    - Apply Material Design 3 styling with card elevation
    - _Requirements: 1.2, 1.3, 5.1, 5.6_

  - [x] 19.2 Create CategoryManagementController (extends Scene)
    - Create CategoryManagementView instance
    - Load categories via CategoryService
    - Handle Add button (show dialog for new category with image picker)
    - Handle Edit button (show dialog with selected category)
    - Handle Delete button (check hasParts, confirm and delete)
    - Use ImageHelper for image conversion
    - Display errors via AlertHelper
    - _Requirements: 5.2, 5.3, 5.4, 5.5_

- [ ] 20. Checkpoint - Verify UI views render correctly
  - Test navigation between views
  - Verify data loads correctly in tables
  - Ensure styling is consistent
  - Ask the user if questions arise


- [ ] 21. Implement view layer - Billing
  - [x] 21.1 Create BillingView (extends BorderPane)
    - Top: GridPane with client name TextField, client phone TextField
    - Center: TableView<Command> showing selected parts (Part Name, Quantity, Price, Subtotal)
    - Right: VBox with part selection (ComboBox or ListView), quantity spinner, Add Part button
    - Bottom: HBox with total price label and Complete Sale button
    - Apply Material Design 3 styling
    - _Requirements: 1.2, 1.3, 6.1, 6.2_

  - [x] 21.2 Create BillingController (extends Scene)
    - Create BillingView instance
    - Load available parts via PartService
    - Handle Add Part button (validate quantity, add to commands list)
    - Calculate and display running total
    - Handle Complete Sale button (validate, save via BillService)
    - Clear form after successful sale
    - Display errors via AlertHelper
    - _Requirements: 6.3, 6.4, 6.5, 6.6, 6.7, 6.9_

- [ ] 22. Implement view layer - Bills History
  - [x] 22.1 Create BillsHistoryView (extends BorderPane)
    - Top: HBox with date range pickers (start date, end date) and Filter button
    - Center: TableView<Bill> with columns (ID, Client Name, Client Phone, Total Price, Date)
    - Bottom: HBox with View Details and Generate PDF buttons
    - Apply Material Design 3 styling
    - _Requirements: 1.2, 1.3, 6.8, 13.3_

  - [x] 22.2 Create BillsHistoryController (extends Scene)
    - Create BillsHistoryView instance
    - Load bills via BillService
    - Handle Filter button (filter by date range)
    - Handle View Details button (show dialog with bill commands)
    - Handle Generate PDF button (call ReportService, show file chooser)
    - Display errors via AlertHelper
    - _Requirements: 6.8, 7.2, 8.1, 8.5_

- [ ] 23. Implement view layer - Sales Analytics
  - [x] 23.1 Create SalesAnalyticsView (extends BorderPane)
    - Top: HBox with date range pickers and Refresh button
    - Center: LineChart or BarChart showing sales over time
    - Right: VBox with summary statistics (Total Revenue, Transaction Count)
    - Apply Material Design 3 styling
    - _Requirements: 1.2, 1.3, 7.1, 7.5_

  - [x] 23.2 Create SalesAnalyticsController (extends Scene)
    - Create SalesAnalyticsView instance
    - Load sales data via SalesService
    - Populate chart with data
    - Calculate and display statistics
    - Handle Refresh button (reload data for selected date range)
    - _Requirements: 7.2, 7.3, 7.4, 7.6_

- [ ] 24. Implement view layer - Settings
  - [x] 24.1 Create SettingsView (extends VBox)
    - Add language selection ComboBox
    - Add database path TextField (read-only) with info label
    - Add Save button
    - Apply Material Design 3 styling
    - _Requirements: 1.2, 1.3, 12.1, 12.2, 12.3_

  - [x] 24.2 Create SettingsController (extends Scene)
    - Create SettingsView instance
    - Load current settings (language from LocaleManager)
    - Handle Save button (save language preference, apply changes)
    - Display success message via AlertHelper
    - _Requirements: 12.2, 12.4, 12.5_


- [ ] 25. Update ChaOfficeApplication main class
  - [x] 25.1 Modify start() method
    - Initialize DatabaseService (create tables, seed data)
    - Load saved language preference via LocaleManager
    - Set primary stage properties (title, size, icon)
    - Create and show LoginController scene
    - Apply main.css stylesheet
    - _Requirements: 9.5, 10.3, 10.4_

  - [x] 25.2 Add application lifecycle hooks
    - Handle window close event (close database connection)
    - _Requirements: 10.6_

- [ ] 26. Integration and wiring
  - [x] 26.1 Verify all controllers can navigate between each other
    - Test login → dashboard flow
    - Test dashboard → all sections flow
    - Test logout → login flow
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5_

  - [x] 26.2 Verify all services are properly integrated
    - Test end-to-end workflows (add part, create bill, generate report)
    - Verify database transactions work correctly
    - Verify error handling works across all layers
    - _Requirements: 10.7, 10.8, 15.5, 15.6_

  - [x] 26.3 Verify internationalization works
    - Test switching languages updates all UI text
    - Test Arabic right-to-left layout
    - Verify language preference persists across restarts
    - _Requirements: 9.2, 9.3, 9.4, 9.5, 9.7_

  - [ ]* 26.4 Write integration tests for critical workflows
    - Test complete billing workflow (select parts, create bill, verify inventory reduction)
    - Test category deletion prevention when parts exist
    - Test PDF generation for bills
    - _Requirements: 6.3, 6.6, 5.5, 8.2_

- [ ] 27. Final checkpoint - Complete system verification
  - Run all property tests and integration tests
  - Verify all features work end-to-end
  - Test error handling and edge cases
  - Verify UI is responsive and styled correctly
  - Ensure all tests pass, ask the user if questions arise

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Integration tests validate end-to-end workflows
- The migration maintains ChaOffice's architecture while adding comprehensive inventory management features
