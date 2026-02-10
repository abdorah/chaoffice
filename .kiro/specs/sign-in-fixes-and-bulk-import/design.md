# Design Document

## Overview

This design addresses two distinct features for the ChaOffice POS system:

1. **Sign-In Page Title Fix**: A simple CSS modification to reduce the title font size from 24px to 18px for better visual hierarchy
2. **Bulk Import Feature**: A comprehensive file upload and processing system that allows users to import multiple parts from CSV or Excel files

The bulk import feature will integrate with the existing PartService and CategoryService, leveraging Apache POI (already in dependencies) for Excel parsing and standard Java I/O for CSV parsing. The design emphasizes data validation, error handling, and clear user feedback throughout the import process.

## Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    PartsInventoryView                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Import Button → FileChooser → File Selection        │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  BulkImportService                           │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  • Orchestrates import process                       │  │
│  │  • Delegates to parsers and validators               │  │
│  │  • Generates import results                          │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
           │                    │                    │
           ▼                    ▼                    ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  CSVFileParser   │  │ ExcelFileParser  │  │ ImportValidator  │
│  • Parse CSV     │  │ • Parse Excel    │  │ • Validate data  │
│  • Extract rows  │  │ • Extract rows   │  │ • Check fields   │
└──────────────────┘  └──────────────────┘  └──────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              Existing Services (Integration)                 │
│  ┌──────────────────┐         ┌──────────────────┐         │
│  │   PartService    │         │ CategoryService  │         │
│  │  • savePart()    │         │ • getAllCategories() │     │
│  └──────────────────┘         └──────────────────┘         │
└─────────────────────────────────────────────────────────────┘
```

### Design Principles

1. **Separation of Concerns**: File parsing, validation, and persistence are handled by separate components
2. **Fail-Safe Processing**: Individual row failures do not stop the entire import
3. **Existing Service Integration**: Leverage PartService and CategoryService for database operations
4. **User Feedback**: Provide detailed progress and error reporting

## Components and Interfaces

### 1. CSS Modification (Sign-In Fix)

**File**: `src/main/resources/style/main.css`

**Change**:
```css
.title-label {
    -fx-font-size: 18px;  /* Changed from 24px */
    -fx-font-weight: bold;
    -fx-text-fill: #333333;
}
```

### 2. ImportResult Model

**Purpose**: Encapsulates the results of a bulk import operation

**Fields**:
- `int successCount`: Number of successfully imported parts
- `int failureCount`: Number of failed imports
- `List<ImportError> errors`: Detailed error information

**Methods**:
- `boolean hasErrors()`: Returns true if any errors occurred
- `String getSummary()`: Returns a human-readable summary

### 3. ImportError Model

**Purpose**: Represents a single import error with context

**Fields**:
- `int rowNumber`: The row number where the error occurred (1-indexed)
- `String fieldName`: The field that caused the error (optional)
- `String errorMessage`: Description of the error
- `ErrorType errorType`: Enum (VALIDATION, PARSING, DATABASE)

### 4. PartImportData Model

**Purpose**: Intermediate representation of part data from file

**Fields**:
- `int rowNumber`: Source row number for error reporting
- `String name`: Part name
- `String maker`: Maker/manufacturer name
- `String description`: Part description
- `String priceStr`: Price as string (to be parsed)
- `String quantityStr`: Quantity as string (to be parsed)
- `String categoryName`: Category name (to be resolved to Category object)

### 5. FileParser Interface

**Purpose**: Common interface for file parsing implementations

**Methods**:
```java
List<PartImportData> parse(File file) throws IOException
```

### 6. CSVFileParser Implementation

**Purpose**: Parses CSV files into PartImportData objects

**Algorithm**:
1. Open file with BufferedReader
2. Read first line and determine if it's a header (check if first cell is "name" or "Name")
3. For each subsequent line:
   - Split by comma (handle quoted fields)
   - Extract 6 fields: name, maker, description, price, quantity, category
   - Create PartImportData with row number
   - Add to result list
4. Handle parsing errors by creating ImportError entries

**Error Handling**:
- Malformed CSV (wrong number of columns) → record error, skip row
- File read errors → throw IOException
- Empty file → return empty list

### 7. ExcelFileParser Implementation

**Purpose**: Parses Excel files (.xlsx, .xls) using Apache POI

**Dependencies**: Apache POI (already in pom.xml)

**Algorithm**:
1. Open workbook using WorkbookFactory
2. Get first sheet
3. Determine if first row is header (check if first cell contains "name")
4. For each subsequent row:
   - Extract 6 cells: name, maker, description, price, quantity, category
   - Handle different cell types (STRING, NUMERIC)
   - Create PartImportData with row number
   - Add to result list
5. Close workbook

**Error Handling**:
- Invalid Excel file → throw IOException
- Missing cells → treat as empty string
- Wrong cell types → convert to string representation

### 8. ImportValidator

**Purpose**: Validates PartImportData before database insertion

**Methods**:
```java
ValidationResult validate(PartImportData data, Map<String, Category> categoryMap)
```

**ValidationResult**:
- `boolean isValid`: Whether validation passed
- `List<String> errors`: List of validation error messages

**Validation Rules** (from Requirements 4.1-4.8):
1. Name is not empty or whitespace-only
2. Maker is not empty or whitespace-only
3. Description is not empty or whitespace-only
4. Price can be parsed as float and is non-negative
5. Quantity can be parsed as integer and is non-negative
6. Category name exists in categoryMap

### 9. BulkImportService

**Purpose**: Orchestrates the entire import process

**Dependencies**:
- PartService (existing)
- CategoryService (existing)
- CSVFileParser
- ExcelFileParser
- ImportValidator

**Methods**:

```java
ImportResult importFromFile(File file)
```

**Algorithm**:
1. Determine file type from extension
2. Select appropriate parser (CSV or Excel)
3. Parse file into List<PartImportData>
4. Load all categories from CategoryService into Map<String, Category>
5. For each PartImportData:
   a. Validate using ImportValidator
   b. If valid:
      - Convert to Part object
      - Call PartService.savePart()
      - Increment success counter
   c. If invalid or save fails:
      - Record error in ImportError list
      - Increment failure counter
6. Return ImportResult with counts and errors

**Error Handling**:
- Unsupported file type → return ImportResult with single error
- File parsing failure → return ImportResult with parsing error
- Individual part save failures → record error, continue processing

### 10. PartsInventoryView Enhancement

**Purpose**: Add bulk import UI to existing parts inventory view

**New UI Elements**:
- Import Button: Labeled "Import from File" with upload icon
- FileChooser: Configured to accept .csv, .xlsx, .xls files
- Progress Indicator: Shown during import processing
- Result Dialog: Displays ImportResult summary

**Integration Points**:
- Add import button to toolbar/button bar
- Wire button to import handler method
- Disable import button during processing
- Refresh parts table after successful import

**Import Handler Flow**:
1. User clicks "Import from File" button
2. FileChooser opens with file type filters
3. User selects file
4. Show progress indicator, disable button
5. Call BulkImportService.importFromFile() on background thread
6. On completion, hide progress indicator
7. Show result dialog with summary
8. If successful imports > 0, refresh parts table
9. Re-enable import button

### 11. Import Result Dialog

**Purpose**: Display import results to user

**Content**:
- Success count: "Successfully imported X parts"
- Failure count: "Failed to import Y parts"
- Error details (if any):
  - Scrollable list of errors
  - Format: "Row N: [Field] - Error message"
  - Group by error type (Validation, Parsing, Database)

**Actions**:
- OK button to close dialog
- Optional: Export errors to file button

## Data Models

### Part (Existing)
```java
class Part {
    int id;
    String name;
    String maker;
    String description;
    float price;
    int quantity;
    Category category;
}
```

### Category (Existing)
```java
class Category {
    int id;
    String name;
    String description;
    byte[] image;
}
```

### PartImportData (New)
```java
class PartImportData {
    int rowNumber;
    String name;
    String maker;
    String description;
    String priceStr;
    String quantityStr;
    String categoryName;
}
```

### ImportResult (New)
```java
class ImportResult {
    int successCount;
    int failureCount;
    List<ImportError> errors;
    
    boolean hasErrors();
    String getSummary();
}
```

### ImportError (New)
```java
class ImportError {
    int rowNumber;
    String fieldName;  // nullable
    String errorMessage;
    ErrorType errorType;
}

enum ErrorType {
    VALIDATION,
    PARSING,
    DATABASE
}
```

### ValidationResult (New)
```java
class ValidationResult {
    boolean isValid;
    List<String> errors;
}
```

## Correctness Properties


A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.

### Property 1: File Type Validation

*For any* file with an unsupported extension (not .csv, .xlsx, or .xls), the system should reject the file and return an error result without attempting to parse it.

**Validates: Requirements 2.4**

### Property 2: CSV and Excel Parsing Correctness

*For any* valid CSV or Excel file with properly formatted part data, the parser should extract all rows and correctly map each column to the corresponding PartImportData field (name, maker, description, price, quantity, category) in the specified order.

**Validates: Requirements 3.1, 3.2, 3.3**

### Property 3: Header Row Skipping

*For any* file with a header row (first cell contains "name" or "Name"), the parser should skip the first row and begin data extraction from row 2, with row numbers in errors starting from 2.

**Validates: Requirements 3.4**

### Property 4: Malformed Row Error Recording

*For any* file containing rows with missing or malformed data, the parser should record an error for each malformed row including the row number and error details, and continue processing remaining rows.

**Validates: Requirements 3.6**

### Property 5: Required Field Validation

*For any* PartImportData with empty or whitespace-only values in the name, maker, or description fields, the validator should reject it and record a validation error identifying the specific empty field.

**Validates: Requirements 4.1, 4.2, 4.3**

### Property 6: Price Validation

*For any* PartImportData where the price string cannot be parsed as a float or parses to a negative value, the validator should reject it and record a validation error for the price field.

**Validates: Requirements 4.4**

### Property 7: Quantity Validation

*For any* PartImportData where the quantity string cannot be parsed as an integer or parses to a negative value, the validator should reject it and record a validation error for the quantity field.

**Validates: Requirements 4.5**

### Property 8: Category Existence Validation

*For any* PartImportData with a category name that does not match any existing category in the database, the validator should reject it and record a validation error for the category field.

**Validates: Requirements 4.6**

### Property 9: Validation Error Recording

*For any* PartImportData that fails validation, the validator should record an error containing the row number, the field name that failed, and a descriptive error message.

**Validates: Requirements 4.7**

### Property 10: Valid Part Insertion

*For any* PartImportData that passes validation, the system should convert it to a Part object and call PartService.savePart() to insert it into the database.

**Validates: Requirements 5.1**

### Property 11: Import Result Accuracy

*For any* bulk import operation, the ImportResult should have a success count equal to the number of parts successfully inserted and a failure count equal to the number of parts that failed validation or insertion, and the sum should equal the total number of data rows processed.

**Validates: Requirements 5.2, 5.5**

### Property 12: Fail-Safe Processing

*For any* bulk import operation where some parts fail validation or insertion, the system should continue processing all remaining parts and include all failures in the error list without stopping the import.

**Validates: Requirements 5.3, 5.4**

### Property 13: Error Logging

*For any* import operation that encounters errors (parsing, validation, or database), the system should log each error with sufficient detail for debugging (row number, error type, error message).

**Validates: Requirements 7.5**

## Error Handling

### File-Level Errors

**Unsupported File Type**:
- Detection: Check file extension before parsing
- Response: Return ImportResult with single error, errorType = PARSING
- User Message: "Unsupported file type. Please upload a .csv, .xlsx, or .xls file."

**File Read Failure**:
- Detection: IOException during file opening
- Response: Return ImportResult with single error, errorType = PARSING
- User Message: "Unable to read file. The file may be corrupted or inaccessible."
- Logging: Log full exception stack trace

**Invalid File Format**:
- Detection: Parser throws exception (e.g., invalid Excel structure)
- Response: Return ImportResult with single error, errorType = PARSING
- User Message: "Invalid file format. Please ensure the file is a valid CSV or Excel file."

### Row-Level Errors

**Malformed CSV Row**:
- Detection: Wrong number of columns after split
- Response: Add ImportError with rowNumber, errorType = PARSING
- Continue processing next row
- Error Message: "Row {N}: Expected 6 columns, found {M}"

**Missing Excel Cells**:
- Detection: Cell is null or empty
- Response: Treat as empty string, let validation catch it
- Validation will generate appropriate error

**Validation Failures**:
- Detection: ValidationResult.isValid == false
- Response: Add ImportError for each validation error with rowNumber, fieldName, errorType = VALIDATION
- Continue processing next row
- Error Message: "Row {N}: {fieldName} - {validation error}"

**Database Insertion Failure**:
- Detection: PartService.savePart() throws exception
- Response: Add ImportError with rowNumber, errorType = DATABASE
- Continue processing next row
- Error Message: "Row {N}: Database error - {exception message}"
- Logging: Log full exception stack trace

### UI Error Handling

**Background Thread Exceptions**:
- All import processing runs on background thread (JavaFX Task)
- Exceptions caught and converted to ImportResult errors
- UI thread only updates based on ImportResult

**Dialog Display**:
- Always show result dialog, even if all imports fail
- Group errors by type (Parsing, Validation, Database)
- Provide scrollable error list for large error counts
- Limit displayed errors to first 100, with message "and N more errors..."

## Testing Strategy

### Dual Testing Approach

The testing strategy employs both unit tests and property-based tests to ensure comprehensive coverage:

**Unit Tests**: Focus on specific examples, edge cases, and integration points
- CSS change verification (check that .title-label has 18px font-size)
- File chooser configuration (verify accepted extensions)
- UI component existence (import button, progress indicator)
- Specific error message formats
- Dialog display behavior
- Integration between components

**Property-Based Tests**: Verify universal properties across all inputs using jqwik (already in pom.xml)
- File parsing correctness with randomly generated CSV/Excel files
- Validation rules with randomly generated PartImportData
- Error recording with various invalid inputs
- Import result accuracy with mixed valid/invalid data
- Fail-safe processing with simulated failures

### Property-Based Testing Configuration

**Library**: jqwik (version 1.8.2, already in dependencies)

**Test Configuration**:
- Minimum 100 iterations per property test
- Each test tagged with comment referencing design property
- Tag format: `// Feature: sign-in-fixes-and-bulk-import, Property {N}: {property title}`

**Example Property Test Structure**:
```java
@Property
// Feature: sign-in-fixes-and-bulk-import, Property 5: Required Field Validation
void emptyFieldsAreRejected(@ForAll("partDataWithEmptyFields") PartImportData data) {
    ValidationResult result = validator.validate(data, categoryMap);
    assertFalse(result.isValid());
    assertTrue(result.getErrors().stream()
        .anyMatch(e -> e.contains("name") || e.contains("maker") || e.contains("description")));
}
```

### Test Data Generators

**For Property Tests**:
- CSV file generator: Creates valid/invalid CSV content with configurable rows
- Excel file generator: Creates valid/invalid Excel files using Apache POI
- PartImportData generator: Generates random part data with various validity states
- Category map generator: Creates maps of valid categories for validation

**For Unit Tests**:
- Fixed example CSV files (valid, with headers, malformed)
- Fixed example Excel files (valid, with headers, malformed)
- Predefined PartImportData instances for edge cases

### Test Coverage Goals

**Component Coverage**:
- CSVFileParser: 100% line coverage
- ExcelFileParser: 100% line coverage
- ImportValidator: 100% line coverage
- BulkImportService: 90%+ line coverage (UI integration may be mocked)

**Property Coverage**:
- Each correctness property has exactly one property-based test
- Each property test runs minimum 100 iterations
- Properties cover all validation rules and error handling paths

### Integration Testing

**Database Integration**:
- Use in-memory SQLite database for tests
- Populate with test categories before import tests
- Verify parts are actually inserted via PartService

**UI Integration**:
- Mock BulkImportService for UI tests
- Verify button states (enabled/disabled)
- Verify dialog content matches ImportResult
- Verify table refresh after successful import

### Manual Testing Checklist

1. Import valid CSV file with 10 parts → all succeed
2. Import valid Excel file with 10 parts → all succeed
3. Import CSV with header row → header skipped, data imported
4. Import file with mix of valid/invalid parts → partial success, errors shown
5. Import file with all invalid parts → all fail, detailed errors shown
6. Import file with unsupported extension → error message shown
7. Import corrupted file → error message shown
8. Verify progress indicator shows during import
9. Verify import button disabled during import
10. Verify table refreshes after successful import
11. Verify error dialog shows row numbers and field names
12. Verify CSS change: sign-in title is 18px
