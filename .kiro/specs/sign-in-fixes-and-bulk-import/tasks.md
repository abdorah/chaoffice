# Implementation Plan: Sign-In Fixes and Bulk Import

## Overview

This implementation plan covers two distinct features:
1. A simple CSS fix to reduce the sign-in page title size from 24px to 18px
2. A comprehensive bulk import feature for uploading and processing CSV/Excel files containing part data

The implementation will leverage existing services (PartService, CategoryService) and the Apache POI library (already in dependencies) for Excel parsing. The approach emphasizes incremental development with early validation through property-based tests.

## Tasks

- [x] 1. Fix sign-in page title size
  - Update the `.title-label` CSS class in `src/main/resources/style/main.css` to use 18px font size
  - _Requirements: 1.1, 1.2_

- [ ] 2. Create data models for bulk import
  - [x] 2.1 Create PartImportData model
    - Add fields: rowNumber, name, maker, description, priceStr, quantityStr, categoryName
    - Add constructor and getters
    - _Requirements: 3.1, 3.2_
  
  - [x] 2.2 Create ImportError model
    - Add fields: rowNumber, fieldName, errorMessage, errorType
    - Create ErrorType enum (VALIDATION, PARSING, DATABASE)
    - Add constructor and getters
    - _Requirements: 4.7, 7.3_
  
  - [x] 2.3 Create ImportResult model
    - Add fields: successCount, failureCount, List<ImportError> errors
    - Add methods: hasErrors(), getSummary()
    - _Requirements: 5.5, 6.3, 6.4_
  
  - [x] 2.4 Create ValidationResult model
    - Add fields: isValid, List<String> errors
    - Add constructor and getters
    - _Requirements: 4.1-4.8_

- [ ] 3. Implement CSV file parser
  - [x] 3.1 Create CSVFileParser class
    - Implement parse(File file) method
    - Handle header row detection and skipping
    - Parse comma-separated values with quote handling
    - Extract 6 fields per row: name, maker, description, price, quantity, category
    - Create PartImportData objects with row numbers
    - Handle malformed rows by recording errors
    - _Requirements: 3.1, 3.3, 3.4, 3.6_
  
  - [ ]* 3.2 Write property test for CSV parsing correctness
    - **Property 2: CSV and Excel Parsing Correctness**
    - **Property 3: Header Row Skipping**
    - **Validates: Requirements 3.1, 3.3, 3.4**
  
  - [ ]* 3.3 Write property test for malformed row handling
    - **Property 4: Malformed Row Error Recording**
    - **Validates: Requirements 3.6**

- [ ] 4. Implement Excel file parser
  - [x] 4.1 Create ExcelFileParser class
    - Implement parse(File file) method using Apache POI
    - Open workbook with WorkbookFactory
    - Handle header row detection and skipping
    - Extract 6 cells per row: name, maker, description, price, quantity, category
    - Handle different cell types (STRING, NUMERIC)
    - Create PartImportData objects with row numbers
    - Close workbook properly
    - _Requirements: 3.2, 3.3, 3.4, 3.6_
  
  - [ ]* 4.2 Write property test for Excel parsing correctness
    - **Property 2: CSV and Excel Parsing Correctness**
    - **Validates: Requirements 3.2, 3.3, 3.4**
  
  - [ ]* 4.3 Write unit tests for Excel edge cases
    - Test missing cells (treat as empty string)
    - Test different cell types
    - Test corrupted Excel files
    - _Requirements: 3.2, 3.5_

- [ ] 5. Implement import validator
  - [x] 5.1 Create ImportValidator class
    - Implement validate(PartImportData data, Map<String, Category> categoryMap) method
    - Validate name is not empty or whitespace-only
    - Validate maker is not empty or whitespace-only
    - Validate description is not empty or whitespace-only
    - Validate price can be parsed as float and is non-negative
    - Validate quantity can be parsed as integer and is non-negative
    - Validate category name exists in categoryMap
    - Return ValidationResult with isValid flag and error list
    - _Requirements: 4.1-4.8_
  
  - [ ]* 5.2 Write property test for required field validation
    - **Property 5: Required Field Validation**
    - **Validates: Requirements 4.1, 4.2, 4.3**
  
  - [ ]* 5.3 Write property test for price validation
    - **Property 6: Price Validation**
    - **Validates: Requirements 4.4**
  
  - [ ]* 5.4 Write property test for quantity validation
    - **Property 7: Quantity Validation**
    - **Validates: Requirements 4.5**
  
  - [ ]* 5.5 Write property test for category validation
    - **Property 8: Category Existence Validation**
    - **Validates: Requirements 4.6**
  
  - [ ]* 5.6 Write property test for validation error recording
    - **Property 9: Validation Error Recording**
    - **Validates: Requirements 4.7**

- [x] 6. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 7. Implement bulk import service
  - [x] 7.1 Create BulkImportService class
    - Add dependencies: PartService, CategoryService, CSVFileParser, ExcelFileParser, ImportValidator
    - Implement importFromFile(File file) method
    - Determine file type from extension (.csv, .xlsx, .xls)
    - Select appropriate parser based on file type
    - Parse file into List<PartImportData>
    - Load all categories from CategoryService into Map<String, Category>
    - For each PartImportData: validate, convert to Part, save via PartService
    - Track success/failure counts
    - Record errors for failed validations and insertions
    - Continue processing on individual failures
    - Return ImportResult with counts and errors
    - _Requirements: 2.4, 3.1, 3.2, 4.1-4.8, 5.1-5.5, 7.1-7.5_
  
  - [ ]* 7.2 Write property test for file type validation
    - **Property 1: File Type Validation**
    - **Validates: Requirements 2.4**
  
  - [ ]* 7.3 Write property test for valid part insertion
    - **Property 10: Valid Part Insertion**
    - **Validates: Requirements 5.1**
  
  - [ ]* 7.4 Write property test for import result accuracy
    - **Property 11: Import Result Accuracy**
    - **Validates: Requirements 5.2, 5.5**
  
  - [ ]* 7.5 Write property test for fail-safe processing
    - **Property 12: Fail-Safe Processing**
    - **Validates: Requirements 5.3, 5.4**
  
  - [ ]* 7.6 Write property test for error logging
    - **Property 13: Error Logging**
    - **Validates: Requirements 7.5**
  
  - [ ]* 7.7 Write unit tests for error handling
    - Test unsupported file type error
    - Test file read failure error
    - Test invalid file format error
    - Test database insertion failure handling
    - _Requirements: 7.1, 7.2, 7.4_

- [x] 8. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. Add bulk import UI to PartsInventoryView
  - [x] 9.1 Add import button to PartsInventoryView
    - Create "Import from File" button with upload icon
    - Add button to toolbar or button bar
    - Wire button to import handler method
    - _Requirements: 2.1, 2.2_
  
  - [x] 9.2 Implement file chooser dialog
    - Configure FileChooser with file type filters (.csv, .xlsx, .xls)
    - Display selected filename to user
    - _Requirements: 2.2, 2.3, 2.5_
  
  - [x] 9.3 Implement import handler with progress indication
    - Show progress indicator when import starts
    - Disable import button during processing
    - Run BulkImportService.importFromFile() on background thread (JavaFX Task)
    - Hide progress indicator when complete
    - Re-enable import button when complete
    - _Requirements: 6.1, 6.2, 6.7_
  
  - [x] 9.4 Create import result dialog
    - Display success count and failure count
    - Show scrollable list of errors grouped by type
    - Format errors as "Row N: [Field] - Error message"
    - Add OK button to close dialog
    - _Requirements: 6.3, 6.4, 6.5_
  
  - [x] 9.5 Implement table refresh after successful import
    - Refresh parts table when successCount > 0
    - _Requirements: 6.6_
  
  - [ ]* 9.6 Write unit tests for UI integration
    - Test button state changes (enabled/disabled)
    - Test progress indicator visibility
    - Test dialog content matches ImportResult
    - Test table refresh after import
    - _Requirements: 2.1, 6.1, 6.2, 6.6, 6.7_

- [ ] 10. Add help and template features
  - [x] 10.1 Add help tooltip to import button
    - Create tooltip explaining expected file format
    - Document column order: name, maker, description, price, quantity, category
    - Specify that category names must match existing categories
    - _Requirements: 8.1, 8.2, 8.4_
  
  - [x] 10.2 Create example CSV template
    - Generate example CSV file with header row and sample data
    - Add "Download Template" button or menu item
    - Implement template download functionality
    - _Requirements: 8.3_

- [x] 11. Final checkpoint - Integration testing
  - Ensure all tests pass, ask the user if questions arise.
  - Manually test complete import workflow with various file types
  - Verify error handling and user feedback

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties (minimum 100 iterations each)
- Unit tests validate specific examples and edge cases
- The CSS fix (task 1) can be completed independently and immediately
- Bulk import tasks build incrementally: models → parsers → validator → service → UI
