# Requirements Document

## Introduction

This document specifies requirements for improving the ChaOffice application's user interface, settings management, and internationalization. The improvements address layout issues in the billing view, add customization options for store branding and currency, enhance user account management, fix date filtering bugs, and complete internationalization support.

## Glossary

- **Billing_View**: The user interface for creating sales transactions and bills
- **Settings_View**: The user interface for configuring application preferences
- **Bills_History_View**: The user interface for viewing historical sales records
- **Reports_View**: The user interface for generating and exporting reports
- **Command_Table**: The table displaying selected parts and quantities in a bill
- **Discount_Controls**: UI elements for applying discounts (percentage or fixed amount)
- **Payment_Controls**: UI elements for selecting payment method (cash, card, check)
- **Store_Branding**: Customizable store name and logo for the application
- **Currency_Settings**: Configuration for currency symbol and display format
- **Date_Filter**: UI component for filtering bills by date range
- **Internationalization**: Support for multiple languages in the user interface
- **LocaleManager**: Utility class managing language resources and translations

## Requirements

### Requirement 1: Billing View Layout Optimization

**User Story:** As a cashier, I want the billing view to use screen space efficiently, so that I can see all necessary information without excessive scrolling.

#### Acceptance Criteria

1. WHEN the Billing_View is displayed, THE Command_Table SHALL expand horizontally to fill all available space
2. WHEN the Command_Table is empty or contains few items, THE Discount_Controls and Payment_Controls SHALL remain visible without requiring vertical scrolling
3. WHEN the Billing_View is displayed, THE Discount_Controls and Payment_Controls SHALL be positioned alongside the complete sale button and total summary
4. WHEN the Command_Table contains many items, THE Command_Table SHALL provide vertical scrolling while keeping controls visible
5. THE Billing_View SHALL maintain a maximum height for the Command_Table to prevent it from pushing other controls off-screen

### Requirement 2: Store Branding Configuration

**User Story:** As a store owner, I want to customize the application with my store name and logo, so that the application reflects my business identity.

#### Acceptance Criteria

1. WHEN accessing Settings_View, THE System SHALL provide an optional field for entering a store name
2. WHEN accessing Settings_View, THE System SHALL provide an optional field for uploading a store logo image
3. WHEN a store name is configured, THE System SHALL display it as the application title in the window header
4. WHEN a store logo is configured, THE System SHALL display it as the application logo in the window header
5. WHEN a store name is configured, THE System SHALL include it in all generated PDF documents
6. WHEN a store logo is configured, THE System SHALL include it in all generated PDF documents
7. WHEN no store name is configured, THE System SHALL display the default application title "ChaOffice - Parts Inventory Management"
8. WHEN no store logo is configured, THE System SHALL display the default application logo
9. THE System SHALL persist store branding settings across application restarts

### Requirement 3: User Account Management

**User Story:** As an administrator, I want to change the application username and password, so that I can maintain account security.

#### Acceptance Criteria

1. WHEN accessing Settings_View, THE System SHALL provide a section for changing the username
2. WHEN accessing Settings_View, THE System SHALL provide a section for changing the password
3. WHEN changing the password, THE System SHALL require the current password for verification
4. WHEN changing the password, THE System SHALL require the new password to be entered twice for confirmation
5. WHEN the new password confirmation does not match, THE System SHALL display an error message and prevent the change
6. WHEN the current password is incorrect, THE System SHALL display an error message and prevent the change
7. WHEN username or password is successfully changed, THE System SHALL display a success message
8. WHEN username or password is successfully changed, THE System SHALL persist the changes to the database
9. THE System SHALL require the new username to be non-empty
10. THE System SHALL require the new password to be at least 6 characters long

### Requirement 4: Bills History Date Filter Correction

**User Story:** As a user, I want the bills history date filter to show accurate results, so that I don't see incorrect data for future dates.

#### Acceptance Criteria

1. WHEN the Date_Filter start date is set to a future date (after today), THE Bills_History_View SHALL display no bills
2. WHEN the Date_Filter end date is set to a future date (after today), THE System SHALL treat the end date as today's date
3. WHEN both Date_Filter dates are in the future, THE Bills_History_View SHALL display no bills
4. WHEN the Date_Filter start date is after the end date, THE System SHALL display an error message
5. WHEN the Date_Filter dates are valid and in the past, THE Bills_History_View SHALL display bills within the specified range

### Requirement 5: Currency Configuration

**User Story:** As a store owner, I want to configure the currency used in the application, so that prices are displayed in my local currency.

#### Acceptance Criteria

1. WHEN accessing Settings_View, THE System SHALL provide a field for selecting a currency symbol
2. WHEN accessing Settings_View, THE System SHALL provide a field for entering a currency acronym
3. THE System SHALL support common currency symbols including $, €, £, ¥, and custom symbols
4. WHEN a currency symbol is configured, THE System SHALL display it before all price values throughout the application
5. WHEN a currency acronym is configured, THE System SHALL use it in generated reports and documents
6. WHEN no currency is configured, THE System SHALL default to "$" as the currency symbol
7. THE System SHALL persist currency settings across application restarts
8. WHEN currency settings are changed, THE System SHALL immediately update all displayed prices in the current view

### Requirement 6: Reports View Internationalization

**User Story:** As a user, I want the reports view to be fully translated, so that I can use the application in my preferred language.

#### Acceptance Criteria

1. WHEN the Reports_View is displayed, THE System SHALL display the title using the localized string from LocaleManager
2. WHEN the Reports_View is displayed, THE "Preset" label SHALL use the localized string from LocaleManager
3. THE System SHALL add the missing localization key "reports.preset" to all language resource files
4. WHEN the application language is changed, THE Reports_View SHALL update all text to the selected language
5. THE System SHALL ensure all hardcoded English strings in Reports_View are replaced with LocaleManager calls

### Requirement 7: Report Document Internationalization

**User Story:** As a user, I want generated reports to be in my selected language, so that I can share reports with stakeholders who speak my language.

#### Acceptance Criteria

1. WHEN a PDF report is generated, THE System SHALL use the current application language for all text content
2. WHEN a CSV report is generated, THE System SHALL use the current application language for column headers
3. WHEN an Excel report is generated, THE System SHALL use the current application language for column headers and labels
4. WHEN generating a sales report, THE System SHALL use localized strings for labels such as "Date", "Total", "Client Name", and "Revenue"
5. WHEN generating an inventory report, THE System SHALL use localized strings for labels such as "Part Name", "Quantity", "Price", and "Category"
6. THE System SHALL ensure all report generation code uses LocaleManager for text content instead of hardcoded English strings

