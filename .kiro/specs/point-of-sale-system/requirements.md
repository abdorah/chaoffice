# Requirements Document: Point of Sale System

## Introduction

This document specifies the requirements for a comprehensive Point of Sale (POS) system for the ChaOffice Parts Inventory System. The POS system will transform the existing basic billing view into a full-featured sales interface optimized for sellers, enabling flexible pricing, discount management, payment method selection, and efficient part search capabilities.

## Glossary

- **POS_System**: The Point of Sale system component of the ChaOffice Parts Inventory application
- **Seller**: A user with permissions to create and process sales transactions
- **Bill**: A sales transaction record containing client information, purchased parts, pricing, and payment details
- **Part**: An inventory item available for sale with associated metadata (name, category, maker, price, stock)
- **Discount**: A reduction in the total price, applied as either a percentage or fixed amount
- **Payment_Method**: The form of payment used to complete a transaction (cash, card, or check)
- **Search_Component**: The custom UI component that enables real-time part search and selection
- **Subtotal**: The sum of all item prices before discount application
- **Final_Total**: The amount due after discount application
- **Stock_Availability**: The current quantity of a part available in inventory

## Requirements

### Requirement 1: Editable Item Pricing

**User Story:** As a seller, I want to adjust individual item prices during transaction creation, so that I can negotiate special deals with specific clients.

#### Acceptance Criteria

1. WHEN a seller adds a part to a bill, THE POS_System SHALL display the item with its default price in an editable format
2. WHEN a seller clicks on an item price field, THE POS_System SHALL allow direct text input to modify the price
3. WHEN a seller modifies an item price, THE POS_System SHALL validate that the new price is a positive number
4. WHEN an invalid price is entered, THE POS_System SHALL reject the input and display an error message
5. WHEN a seller changes an item price, THE POS_System SHALL recalculate the subtotal immediately
6. THE POS_System SHALL store the actual sale price (not the default price) in the bill record

### Requirement 2: Discount Management

**User Story:** As a seller, I want to apply discounts to transactions using either percentage or fixed amount, so that I can offer promotions and negotiate with clients.

#### Acceptance Criteria

1. WHEN a seller views a bill in progress, THE POS_System SHALL display discount controls with three options: none, percentage, or fixed amount
2. WHEN a seller selects percentage discount mode, THE POS_System SHALL provide an input field for percentage values between 0 and 100
3. WHEN a seller selects fixed amount discount mode, THE POS_System SHALL provide an input field for monetary values
4. WHEN a discount value is entered, THE POS_System SHALL validate that it does not exceed the subtotal
5. WHEN a valid discount is applied, THE POS_System SHALL display the breakdown: subtotal, discount amount, and final total
6. WHEN discount type or value changes, THE POS_System SHALL recalculate the final total immediately
7. THE POS_System SHALL store discount_type, discount_value, subtotal, and final_total in the bill record

### Requirement 3: Payment Method Selection

**User Story:** As a seller, I want to record the payment method used for each transaction, so that I can track payment types for accounting and reporting purposes.

#### Acceptance Criteria

1. WHEN a seller creates a bill, THE POS_System SHALL default the payment method to cash
2. THE POS_System SHALL provide three payment method options: cash, card, and check
3. WHEN a seller selects a payment method, THE POS_System SHALL visually indicate the selected option
4. WHEN a bill is saved, THE POS_System SHALL store the selected payment_method in the bill record
5. THE POS_System SHALL allow payment method changes at any time before bill completion

### Requirement 4: Real-Time Part Search

**User Story:** As a seller, I want to search for parts by typing keywords, so that I can quickly find items without scrolling through long lists.

#### Acceptance Criteria

1. WHEN a seller types in the search field, THE Search_Component SHALL filter parts in real-time without requiring a submit action
2. THE Search_Component SHALL search across part name, description, category name, and maker name fields
3. WHEN search text is entered, THE Search_Component SHALL execute a SQL query using LIKE operators with proper indexes
4. WHEN search results are returned, THE Search_Component SHALL display them within 500 milliseconds for datasets under 10,000 parts
5. WHEN the search field is empty, THE Search_Component SHALL display all available parts or a default view
6. THE Search_Component SHALL perform case-insensitive searches

### Requirement 5: Visual Part Presentation

**User Story:** As a seller, I want to see detailed part information in search results, so that I can quickly identify the correct item and verify stock availability.

#### Acceptance Criteria

1. WHEN parts are displayed in search results, THE Search_Component SHALL show part name, category, maker, current price, and stock availability for each part
2. WHERE a part has an associated image, THE Search_Component SHALL display the image or icon
3. WHERE a part has no image, THE Search_Component SHALL display a default placeholder icon
4. WHEN a part is out of stock, THE Search_Component SHALL visually indicate unavailability with distinct styling
5. THE Search_Component SHALL organize information in a scannable layout optimized for quick visual identification

### Requirement 6: Category Filtering

**User Story:** As a seller, I want to filter parts by category, so that I can narrow search results when I know the general type of part needed.

#### Acceptance Criteria

1. THE Search_Component SHALL provide a category filter dropdown containing all available categories
2. WHEN a category is selected, THE Search_Component SHALL display only parts belonging to that category
3. WHEN a category filter is active and search text is entered, THE Search_Component SHALL apply both filters simultaneously
4. THE Search_Component SHALL include an "All Categories" option to clear the category filter
5. WHEN the category filter changes, THE Search_Component SHALL update results immediately

### Requirement 7: Database Schema Extensions

**User Story:** As a system architect, I want the database schema to support new POS features, so that all transaction details are properly persisted.

#### Acceptance Criteria

1. THE POS_System SHALL extend the bills table with columns: discount_type, discount_value, payment_method, and subtotal
2. THE discount_type column SHALL accept values: 'none', 'percentage', or 'fixed'
3. THE payment_method column SHALL accept values: 'cash', 'card', or 'check'
4. THE discount_value column SHALL store numeric values with appropriate precision for monetary amounts
5. THE subtotal column SHALL store the pre-discount total with appropriate precision for monetary amounts
6. WHEN a bill is created without explicit discount or payment method, THE POS_System SHALL use default values ('none' for discount_type, 'cash' for payment_method)

### Requirement 8: Search Performance Optimization

**User Story:** As a system administrator, I want the part search to use database indexes, so that search performance remains fast as the inventory grows.

#### Acceptance Criteria

1. THE POS_System SHALL create a database index on the parts.name column
2. THE POS_System SHALL create a database index on the parts.catid column
3. WHEN search queries are executed, THE POS_System SHALL use indexed columns in WHERE clauses
4. THE POS_System SHALL use standard SQL LIKE queries rather than JSONB operations
5. WHEN the parts table contains 10,000 or more records, search queries SHALL complete within 500 milliseconds

### Requirement 9: Inventory Validation

**User Story:** As a seller, I want the system to prevent overselling, so that I cannot complete transactions for parts that are out of stock.

#### Acceptance Criteria

1. WHEN a seller attempts to add a part to a bill, THE POS_System SHALL verify that sufficient stock is available
2. WHEN insufficient stock exists, THE POS_System SHALL prevent the addition and display an error message indicating available quantity
3. WHEN a bill is completed, THE POS_System SHALL validate stock availability for all items immediately before finalizing
4. IF stock becomes insufficient between addition and completion, THE POS_System SHALL reject the transaction and notify the seller
5. THE POS_System SHALL maintain existing inventory validation logic from BillService

### Requirement 10: Backwards Compatibility

**User Story:** As a system administrator, I want new POS features to be backwards compatible, so that existing bills and reports continue to function correctly.

#### Acceptance Criteria

1. WHEN the database schema is extended, THE POS_System SHALL use default values for new columns on existing records
2. WHEN existing bills are queried, THE POS_System SHALL handle NULL or missing values in new columns gracefully
3. WHEN reports are generated, THE POS_System SHALL display existing bills correctly regardless of whether they have discount or payment method data
4. THE POS_System SHALL maintain the existing structure of users, makers, categories, parts, bills, and commands tables
5. THE POS_System SHALL preserve the singleton DatabaseConnection pattern used by existing services

### Requirement 11: Transaction Display

**User Story:** As a seller, I want to see a clear breakdown of transaction totals, so that I can verify calculations and explain charges to clients.

#### Acceptance Criteria

1. WHEN items are added to a bill, THE POS_System SHALL display the running subtotal
2. WHEN a discount is applied, THE POS_System SHALL display three distinct values: subtotal, discount amount, and final total
3. THE POS_System SHALL format all monetary values with appropriate currency symbols and decimal precision
4. WHEN no discount is applied, THE POS_System SHALL display the subtotal as the final total
5. THE POS_System SHALL update all displayed totals immediately when items, prices, or discounts change

### Requirement 12: Part Selection from Search

**User Story:** As a seller, I want to select parts from search results with a single action, so that I can quickly build transactions.

#### Acceptance Criteria

1. WHEN a seller clicks on a part in search results, THE Search_Component SHALL add that part to the current bill
2. WHEN a part is added, THE POS_System SHALL use the part's current default price as the initial price
3. WHEN a part is added, THE Search_Component SHALL provide visual feedback confirming the addition
4. THE Search_Component SHALL remain open after part selection to allow adding multiple items
5. WHEN a part with zero stock is clicked, THE POS_System SHALL prevent addition and display a stock unavailability message
