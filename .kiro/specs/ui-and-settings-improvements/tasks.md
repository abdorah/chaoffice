# Implementation Plan: UI and Settings Improvements

## Overview

This implementation plan breaks down the UI and settings improvements into discrete coding tasks. The tasks are organized to build incrementally, with testing integrated throughout. The implementation covers billing view layout optimization, store branding configuration, user account management, date filter fixes, currency configuration, and internationalization improvements.

## Tasks

- [x] 1. Add missing internationalization keys
  - Add "reports.preset" key to all language property files (messages_en_US.properties, messages_ar.properties, messages_fr.properties)
  - Add report column header keys (report.column.date, report.column.total, etc.) to all language files
  - Add report title keys (report.sales.title, report.inventory.title, etc.) to all language files
  - _Requirements: 6.3, 7.1, 7.2, 7.3_

- [x] 2. Create database schema for new settings tables
  - [x] 2.1 Create branding_settings table in ddl.sql
    - Add table with columns: id (PRIMARY KEY CHECK id=1), store_name (TEXT), logo_image (BLOB), logo_format (TEXT)
    - _Requirements: 2.1, 2.2, 2.9_
  
  - [x] 2.2 Create currency_settings table in ddl.sql
    - Add table with columns: id (PRIMARY KEY CHECK id=1), symbol (TEXT DEFAULT '$'), acronym (TEXT DEFAULT 'USD')
    - _Requirements: 5.1, 5.2, 5.7_

- [x] 3. Implement branding models and service
  - [x] 3.1 Create BrandingSettings model class
    - Add fields: storeName, logoImage (byte[]), logoFormat
    - Add getters, setters, hasStoreName(), hasLogo() methods
    - _Requirements: 2.1, 2.2_
  
  - [x] 3.2 Create BrandingService class
    - Implement getBrandingSettings() with database loading and caching
    - Implement saveBrandingSettings() with database persistence
    - Implement getApplicationTitle() with fallback to default
    - Implement getApplicationLogo() with fallback to default
    - _Requirements: 2.3, 2.4, 2.7, 2.8, 2.9_
  
  - [ ]* 3.3 Write property test for branding persistence
    - **Property 8: Branding persistence round-trip**
    - **Validates: Requirements 2.9**

- [x] 4. Implement currency models and utilities
  - [x] 4.1 Create CurrencySettings model class
    - Add fields: symbol (default "$"), acronym (default "USD")
    - Add formatPrice(float) method
    - _Requirements: 5.1, 5.2, 5.6_
  
  - [x] 4.2 Update SettingsService for currency management
    - Add getCurrencySettings() with database loading and caching
    - Add saveCurrencySettings() with database persistence
    - _Requirements: 5.7_
  
  - [x] 4.3 Create CurrencyFormatter utility class
    - Add static initialize(SettingsService) method
    - Add static format(float) method
    - Add static getSymbol() and getAcronym() methods
    - _Requirements: 5.4, 5.5_
  
  - [ ]* 4.4 Write property test for currency persistence
    - **Property 23: Currency persistence round-trip**
    - **Validates: Requirements 5.7**

- [x] 5. Implement user account management
  - [x] 5.1 Create UserCredentials model class
    - Add fields: username, passwordHash
    - Add constructor and getters
    - _Requirements: 3.1, 3.2_
  
  - [x] 5.2 Update AuthenticationService with credential change methods
    - Implement changeUsername(currentPassword, newUsername) with validation
    - Implement changePassword(currentPassword, newPassword, confirmPassword) with validation
    - Add private hashPassword() and verifyPassword() helper methods
    - _Requirements: 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10_
  
  - [ ]* 5.3 Write property tests for credential validation
    - **Property 9: Password change requires current password**
    - **Property 10: Password confirmation mismatch rejection**
    - **Property 11: Incorrect current password rejection**
    - **Property 14: Empty username rejection**
    - **Property 15: Short password rejection**
    - **Validates: Requirements 3.3, 3.5, 3.6, 3.9, 3.10**
  
  - [ ]* 5.4 Write property test for credential persistence
    - **Property 13: Credential persistence round-trip**
    - **Validates: Requirements 3.8**

- [x] 6. Update SettingsView with new sections
  - [x] 6.1 Add store branding section to SettingsView
    - Add TextField for store name
    - Add Button for logo selection with FileChooser
    - Add ImageView for logo preview
    - _Requirements: 2.1, 2.2_
  
  - [x] 6.2 Add currency configuration section to SettingsView
    - Add ComboBox for currency symbol selection ($, €, £, ¥, ₹, Custom)
    - Add TextField for custom symbol (enabled when Custom selected)
    - Add TextField for currency acronym
    - _Requirements: 5.1, 5.2, 5.3_
  
  - [x] 6.3 Add user account management section to SettingsView
    - Add TextField for new username
    - Add Button "Change Username"
    - Add PasswordField for current password
    - Add PasswordField for new password
    - Add PasswordField for confirm new password
    - Add Button "Change Password"
    - _Requirements: 3.1, 3.2, 3.4_

- [x] 7. Update SettingsController with new functionality
  - [x] 7.1 Wire branding controls to BrandingService
    - Load current branding settings on view initialization
    - Handle logo file selection and image loading
    - Handle save button to persist branding settings
    - Show success/error messages
    - _Requirements: 2.9_
  
  - [x] 7.2 Wire currency controls to SettingsService
    - Load current currency settings on view initialization
    - Handle currency symbol selection (enable/disable custom field)
    - Handle save button to persist currency settings
    - Trigger currency update event for immediate UI refresh
    - Show success/error messages
    - _Requirements: 5.7, 5.8_
  
  - [x] 7.3 Wire account management controls to AuthenticationService
    - Handle "Change Username" button with validation
    - Handle "Change Password" button with validation
    - Show validation errors for empty username, short password, mismatched confirmation
    - Show success messages on successful changes
    - _Requirements: 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10_

- [x] 8. Checkpoint - Ensure settings functionality works
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Optimize BillingView layout
  - [x] 9.1 Restructure BillingView center section
    - Replace center VBox with HBox containing two sections
    - Left section: Command table (70% width, wrap in ScrollPane with max height 300px)
    - Right section: VBox with discount controls, payment controls, totals, and complete button (30% width)
    - Set HBox.setHgrow(commandsTable, Priority.ALWAYS) for horizontal expansion
    - Remove discount/payment controls from original bottom section
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
  
  - [ ]* 9.2 Write property tests for billing layout
    - **Property 1: Command table horizontal expansion**
    - **Property 3: Command table scrolling with many items**
    - **Validates: Requirements 1.1, 1.4**

- [x] 10. Fix BillsHistoryController date filter logic
  - [x] 10.1 Update filterBills() method with date validation
    - Add validation: start date must be <= end date
    - If start date is in future, return empty list
    - If end date is in future, cap it at today's date
    - Query database with validated dates
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  
  - [ ]* 10.2 Write property tests for date filtering
    - **Property 16: Future start date returns no bills**
    - **Property 17: Future end date normalization**
    - **Property 18: Invalid date range rejection**
    - **Property 19: Valid date range filtering**
    - **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5**

- [x] 11. Update application title and logo with branding
  - [x] 11.1 Integrate BrandingService into ChaOfficeApplication
    - Inject BrandingService into main application class
    - Call brandingService.getApplicationTitle() when setting stage title
    - Call brandingService.getApplicationLogo() when setting stage icon
    - _Requirements: 2.3, 2.4, 2.7, 2.8_
  
  - [ ]* 11.2 Write property tests for branding display
    - **Property 4: Store name in application title**
    - **Property 5: Store logo in application icon**
    - **Validates: Requirements 2.3, 2.4**

- [x] 12. Update all views to use CurrencyFormatter
  - [x] 12.1 Replace hardcoded "$" in BillingView
    - Update total labels to use CurrencyFormatter.format()
    - Update table cell factories to use CurrencyFormatter
    - _Requirements: 5.4_
  
  - [x] 12.2 Replace hardcoded "$" in BillsHistoryView
    - Update total column cell factory to use CurrencyFormatter.format()
    - _Requirements: 5.4_
  
  - [x] 12.3 Replace hardcoded "$" in PartsInventoryView
    - Update price column to use CurrencyFormatter.format()
    - _Requirements: 5.4_
  
  - [x] 12.4 Replace hardcoded "$" in AnalyticsView (if exists)
    - Update revenue displays to use CurrencyFormatter.format()
    - _Requirements: 5.4_
  
  - [ ]* 12.5 Write property test for currency display
    - **Property 21: Currency symbol display**
    - **Validates: Requirements 5.4**

- [x] 13. Update ReportsView internationalization
  - [x] 13.1 Replace hardcoded "Preset" label
    - Change "Preset:" to LocaleManager.getString("reports.preset") + ":"
    - _Requirements: 6.2_
  
  - [x] 13.2 Verify all other labels use LocaleManager
    - Audit ReportsView for any remaining hardcoded English strings
    - Replace with LocaleManager calls
    - _Requirements: 6.1, 6.4, 6.5_

- [x] 14. Update PDF report generation for internationalization and branding
  - [x] 14.1 Update SalesReportPDFGenerator
    - Inject BrandingService
    - Add branding header (logo and store name) if configured
    - Replace hardcoded English strings with LocaleManager.getString() calls
    - Use CurrencyFormatter for price formatting
    - _Requirements: 2.5, 2.6, 5.5, 7.1_
  
  - [x] 14.2 Update InventoryReportPDFGenerator
    - Inject BrandingService
    - Add branding header (logo and store name) if configured
    - Replace hardcoded English strings with LocaleManager.getString() calls
    - Use CurrencyFormatter for price formatting
    - _Requirements: 2.5, 2.6, 5.5, 7.1_
  
  - [ ]* 14.3 Write property tests for PDF branding
    - **Property 6: Store name in PDF documents**
    - **Property 7: Store logo in PDF documents**
    - **Validates: Requirements 2.5, 2.6**
  
  - [ ]* 14.4 Write property test for PDF localization
    - **Property 26: PDF report localization**
    - **Validates: Requirements 7.1**

- [x] 15. Update CSV report generation for internationalization
  - [x] 15.1 Update SalesReportCSVGenerator
    - Replace hardcoded English column headers with LocaleManager.getString() calls
    - Use CurrencyFormatter for price formatting
    - _Requirements: 5.5, 7.2_
  
  - [x] 15.2 Update InventoryReportCSVGenerator
    - Replace hardcoded English column headers with LocaleManager.getString() calls
    - Use CurrencyFormatter for price formatting
    - _Requirements: 5.5, 7.2_
  
  - [ ]* 15.3 Write property test for CSV localization
    - **Property 27: CSV report localization**
    - **Validates: Requirements 7.2**

- [x] 16. Update Excel report generation for internationalization
  - [x] 16.1 Update SalesReportExcelGenerator
    - Replace hardcoded English column headers with LocaleManager.getString() calls
    - Use CurrencyFormatter for price formatting
    - _Requirements: 5.5, 7.3_
  
  - [x] 16.2 Update InventoryReportExcelGenerator
    - Replace hardcoded English column headers with LocaleManager.getString() calls
    - Use CurrencyFormatter for price formatting
    - _Requirements: 5.5, 7.3_
  
  - [ ]* 16.3 Write property test for Excel localization
    - **Property 28: Excel report localization**
    - **Validates: Requirements 7.3**

- [x] 17. Implement currency change immediate update
  - [x] 17.1 Add currency change event system
    - Create CurrencyChangeEvent class
    - Add event listener registration in SettingsService
    - Fire event when currency settings are saved
    - _Requirements: 5.8_
  
  - [x] 17.2 Register currency change listeners in views
    - BillingView: Update all price displays on currency change
    - BillsHistoryView: Refresh table on currency change
    - PartsInventoryView: Refresh table on currency change
    - AnalyticsView: Update revenue displays on currency change
    - _Requirements: 5.8_
  
  - [ ]* 17.3 Write property test for immediate currency update
    - **Property 24: Currency change immediate update**
    - **Validates: Requirements 5.8**

- [x] 18. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- The implementation is organized to minimize disruption to existing functionality
- Database schema changes are applied first to support new features
- UI changes are applied after backend services are ready
- Internationalization is applied systematically across all report generators
