# Implementation Plan: Point of Sale System

## Overview

This implementation plan breaks down the POS system development into discrete, incremental tasks. Each task builds on previous work, with testing integrated throughout to catch errors early. The plan follows the existing JavaFX MVC architecture and maintains backwards compatibility with the current system.

## Tasks

- [x] 1. Database schema migration and setup
  - Execute ALTER TABLE statements to add new columns to bills table (subtotal, discount_type, discount_value, payment_method)
  - Create database indexes on parts.name and parts.catid for search optimization
  - Write migration verification script to confirm schema changes
  - Add database constraint checks for discount_type and payment_method enums
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 8.1, 8.2_

- [ ]* 1.1 Write property test for database schema
  - **Property 8: Database constraint enforcement - discount_type**
  - **Property 9: Database constraint enforcement - payment_method**
  - **Validates: Requirements 7.2, 7.3**

- [ ]* 1.2 Write unit tests for schema migration
  - Test schema verification with existing database
  - Test index creation and verification
  - Test default value application on existing records
  - _Requirements: 7.6, 10.1_

- [x] 2. Extend Bill model with POS properties
  - Add JavaFX properties: subtotal, discountType, discountValue, paymentMethod
  - Implement calculateFinalTotal() method with discount logic
  - Implement applyDiscount(String type, float value) with validation
  - Implement setPaymentMethod(String method) with validation
  - Add property change listeners for automatic recalculation
  - _Requirements: 1.1, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2_

- [ ]* 2.1 Write property tests for Bill model calculations
  - **Property 1: Price validation**
  - **Property 3: Discount validation - percentage**
  - **Property 4: Discount validation - fixed amount**
  - **Property 5: Final total calculation correctness**
  - **Property 6: Discount amount never exceeds subtotal**
  - **Validates: Requirements 1.3, 1.4, 2.2, 2.4, 2.6, 11.4**

- [ ]* 2.2 Write unit tests for Bill model
  - Test default values initialization
  - Test discount type transitions (none → percentage → fixed)
  - Test payment method changes
  - Test edge cases (zero subtotal, 100% discount, discount equals subtotal)
  - _Requirements: 2.1, 3.1, 3.5_

- [x] 3. Extend BillService for POS persistence
  - Modify saveBill() to include new Bill properties in INSERT statement
  - Update mapResultSetToBill() to handle new columns with NULL safety
  - Add validation for discount and payment method values before save
  - Implement backwards compatibility handling for existing bills
  - _Requirements: 2.7, 3.4, 10.1, 10.2, 10.3_

- [ ]* 3.1 Write property tests for BillService persistence
  - **Property 7: Round-trip persistence**
  - **Property 10: Numeric precision preservation**
  - **Property 11: Backwards compatibility with NULL values**
  - **Validates: Requirements 1.6, 2.7, 3.4, 7.4, 7.5, 10.2, 10.3**

- [ ]* 3.2 Write unit tests for BillService
  - Test saving bill with all discount types
  - Test saving bill with all payment methods
  - Test retrieving existing bills with NULL values
  - Test transaction rollback on error
  - _Requirements: 2.7, 3.4, 10.2_

- [x] 4. Checkpoint - Verify model and persistence layer
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Create SearchService for optimized part search
  - Implement searchParts(String query, Integer categoryId) with indexed SQL queries
  - Implement searchByText(String query) searching across name, description, maker, category
  - Implement filterByCategory(int categoryId) for category-only filtering
  - Implement searchWithFilters(String query, Integer categoryId) for combined search
  - Add case-insensitive LIKE queries with proper parameter binding
  - _Requirements: 4.1, 4.2, 4.6, 6.2, 6.3_

- [ ]* 5.1 Write property tests for SearchService
  - **Property 12: Search coverage**
  - **Property 13: Case-insensitive search**
  - **Property 14: Category filter correctness**
  - **Property 15: Combined filter correctness**
  - **Property 16: Empty search returns all parts** (edge case)
  - **Validates: Requirements 4.2, 4.5, 4.6, 6.2, 6.3**

- [ ]* 5.2 Write unit tests for SearchService
  - Test search with various query patterns
  - Test search with special characters
  - Test category filter with valid and invalid IDs
  - Test combined filters with edge cases
  - _Requirements: 4.1, 4.2, 6.2, 6.3_

- [x] 6. Create PartSearchComponent custom control
  - Extend VBox to create custom JavaFX component
  - Add TextField for search input with real-time listener
  - Add ComboBox for category filter with "All Categories" option
  - Add ListView with custom PartCell renderer
  - Implement search debouncing (300ms delay) for performance
  - Wire up SearchService integration
  - Add visual feedback for part selection
  - _Requirements: 4.1, 4.5, 5.1, 5.2, 5.3, 5.4, 6.1, 6.4, 6.5, 12.3_

- [ ]* 6.1 Write property tests for PartSearchComponent
  - **Property 17: Part display completeness**
  - **Property 18: Out-of-stock visual indication**
  - **Property 24: Part addition from search**
  - **Validates: Requirements 5.1, 5.4, 12.1**

- [ ]* 6.2 Write unit tests for PartSearchComponent
  - Test search field input handling
  - Test category filter selection
  - Test part cell rendering with and without images
  - Test out-of-stock styling
  - Test click handler for part selection
  - _Requirements: 4.1, 5.2, 5.3, 5.4, 6.1, 6.4_

- [x] 7. Create custom PartCell renderer
  - Extend ListCell<Part> for custom rendering
  - Display part image or default placeholder icon
  - Display part name, category, maker, price, and stock
  - Apply distinct styling for out-of-stock parts (quantity = 0)
  - Format price with currency symbol and 2 decimal places
  - Implement scannable layout with proper spacing
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 11.3_

- [ ]* 7.1 Write property tests for PartCell
  - **Property 19: Monetary value formatting**
  - **Validates: Requirements 11.3**

- [ ]* 7.2 Write unit tests for PartCell
  - Test rendering with image present
  - Test rendering with no image (placeholder)
  - Test out-of-stock styling application
  - Test price formatting with various values
  - _Requirements: 5.2, 5.3, 5.4, 11.3_

- [x] 8. Checkpoint - Verify search functionality
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Extend BillingView with POS UI components
  - Add discount controls section (RadioButtons for type, TextField for value)
  - Add payment method selector (RadioButtons for cash/card/check)
  - Replace part selector ComboBox with PartSearchComponent
  - Make price column in commands table editable (TextFieldTableCell)
  - Add enhanced total display section (subtotal, discount, final total labels)
  - Update layout to accommodate new components
  - Apply consistent styling with existing application theme
  - _Requirements: 1.1, 1.2, 2.1, 2.5, 3.2, 3.3, 11.2_

- [ ]* 9.1 Write unit tests for BillingView
  - Test UI component initialization
  - Test discount controls visibility and state
  - Test payment method selector initialization
  - Test editable price cell behavior
  - Test total display formatting
  - _Requirements: 1.1, 2.1, 2.5, 3.2, 11.2_

- [x] 10. Implement BillingController POS logic
  - Initialize PartSearchComponent with SearchService
  - Implement handleSearch(String query) for real-time search
  - Implement handleCategoryFilter(Category category) for filtering
  - Implement handlePartSelection(Part part) to add parts to bill
  - Implement handlePriceEdit(Command command, float newPrice) with validation
  - Implement handleDiscountChange(String type, float value) with validation
  - Implement handlePaymentMethodChange(String method)
  - Implement calculateTotals() for real-time total updates
  - Implement completeSale() with inventory validation
  - Add property change listeners for automatic recalculation
  - _Requirements: 1.3, 1.4, 1.5, 2.2, 2.4, 2.6, 3.5, 11.5, 12.1, 12.2_

- [ ]* 10.1 Write property tests for BillingController
  - **Property 2: Subtotal calculation correctness**
  - **Property 20: Stock availability check on addition**
  - **Property 21: Stock availability check on completion**
  - **Property 22: Zero-stock parts cannot be added**
  - **Property 23: Default price initialization**
  - **Property 25: Payment method mutability**
  - **Validates: Requirements 1.5, 9.1, 9.2, 9.3, 9.4, 11.1, 12.2, 12.5, 3.5**

- [ ]* 10.2 Write unit tests for BillingController
  - Test part addition with sufficient stock
  - Test part addition with insufficient stock (should fail)
  - Test price edit with valid values
  - Test price edit with invalid values (should reject)
  - Test discount application with valid values
  - Test discount application with invalid values (should reject)
  - Test payment method changes
  - Test total recalculation on various changes
  - Test complete sale with valid bill
  - Test complete sale with insufficient stock (should fail)
  - _Requirements: 1.3, 1.4, 1.5, 2.2, 2.4, 2.6, 3.5, 9.1, 9.2, 9.3, 9.4, 12.5_

- [x] 11. Implement error handling and user feedback
  - Add input validation error messages for price edits
  - Add input validation error messages for discount values
  - Add inventory error alerts for insufficient stock
  - Add inventory error alerts for zero-stock parts
  - Add database error handling with user-friendly messages
  - Add visual feedback for successful operations (toast notifications)
  - Implement error styling (red borders, tooltips)
  - _Requirements: 1.4, 2.2, 2.4, 9.2, 12.5_

- [ ]* 11.1 Write unit tests for error handling
  - Test error message content and formatting
  - Test error display for various validation failures
  - Test error recovery and state restoration
  - Test alert dialog display for critical errors
  - _Requirements: 1.4, 2.2, 2.4, 9.2, 12.5_

- [x] 12. Checkpoint - Verify complete POS workflow
  - Ensure all tests pass, ask the user if questions arise.

- [x] 13. Add internationalization support for POS features
  - Add message keys to LocaleManager for all new UI strings
  - Add translations for discount types and payment methods
  - Add translations for error messages
  - Add translations for UI labels and buttons
  - Ensure consistent terminology across application
  - _Requirements: All UI-related requirements_

- [ ]* 13.1 Write unit tests for internationalization
  - Test message key existence for all new strings
  - Test message formatting with parameters
  - Test fallback to default language
  - _Requirements: All UI-related requirements_

- [x] 14. Integration testing and end-to-end scenarios
  - [x] 14.1 Test complete sale workflow with custom prices
    - Create bill, add parts, edit prices, complete sale
    - Verify inventory reduction
    - Verify bill persistence
    - _Requirements: 1.1, 1.2, 1.3, 1.5, 1.6, 9.3_
  
  - [x] 14.2 Test complete sale workflow with percentage discount
    - Create bill, add parts, apply percentage discount, complete sale
    - Verify total calculation
    - Verify discount persistence
    - _Requirements: 2.1, 2.2, 2.5, 2.6, 2.7_
  
  - [x] 14.3 Test complete sale workflow with fixed discount
    - Create bill, add parts, apply fixed discount, complete sale
    - Verify total calculation
    - Verify discount persistence
    - _Requirements: 2.1, 2.3, 2.4, 2.5, 2.6, 2.7_
  
  - [x] 14.4 Test search and filter workflow
    - Search for parts by text
    - Filter by category
    - Combine search and filter
    - Add parts from search results
    - _Requirements: 4.1, 4.2, 6.2, 6.3, 12.1_
  
  - [x] 14.5 Test inventory validation workflow
    - Attempt to add part with insufficient stock (should fail)
    - Attempt to add out-of-stock part (should fail)
    - Complete sale with valid stock
    - Verify inventory reduction
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 12.5_
  
  - [x] 14.6 Test payment method workflow
    - Select each payment method (cash, card, check)
    - Verify visual indication
    - Complete sale
    - Verify payment method persistence
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_
  
  - [x] 14.7 Test backwards compatibility
    - Query existing bills from database
    - Verify display with NULL values in new columns
    - Verify no errors or crashes
    - _Requirements: 10.1, 10.2, 10.3, 10.4_

- [x] 15. Performance testing and optimization
  - [x] 15.1 Test search performance with 10,000+ parts
    - Populate test database with large dataset
    - Measure search query execution time
    - Verify < 500ms response time
    - Verify index usage with EXPLAIN QUERY PLAN
    - _Requirements: 4.4, 8.5_
  
  - [x] 15.2 Test UI responsiveness with large bills
    - Create bill with 100+ line items
    - Test price edit responsiveness
    - Test discount change responsiveness
    - Verify no UI freezing
    - _Requirements: 1.5, 2.6, 11.5_

- [x] 16. Final checkpoint and user acceptance
  - Run complete test suite (unit + property + integration)
  - Verify all requirements covered
  - Test with production-like data
  - Perform manual exploratory testing
  - Document any known issues or limitations
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- Integration tests verify end-to-end workflows
- The implementation maintains backwards compatibility throughout
- All database operations use prepared statements for security
- All monetary calculations preserve 2 decimal place precision
