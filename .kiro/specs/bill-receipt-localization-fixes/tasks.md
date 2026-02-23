# Bill Receipt Localization Fixes - Implementation Tasks

## Overview
This task list breaks down the implementation of localization fixes into actionable items organized by phase.

**Estimated Total Effort**: 4 days  
**Priority**: High  
**Dependencies**: None

---

## Phase 1: Foundation and Infrastructure

### Task 1: Create EnumLocalizer Utility Class
**Priority**: P0 (Critical)  
**Estimated Effort**: 2 hours  
**Assignee**: TBD

**Description**: Create a new utility class to centralize enum localization logic.

**Subtasks**:
- [x] 1.1 Create `EnumLocalizer.java` in `src/main/java/org/chaos/office/util/`
- [x] 1.2 Implement `getLocalizedStockStatus(StockStatus)` method
- [x] 1.3 Implement `getLocalizedPaymentMethod(PaymentMethod)` method
- [x] 1.4 Add null checks and return empty string for null values
- [x] 1.5 Add JavaDoc comments for all methods
- [x] 1.6 Add SLF4J logger for debugging

**Acceptance Criteria**:
- Class compiles without errors
- All methods have proper JavaDoc
- Null values handled gracefully
- Uses LocaleManager for string lookups

**Files to Create**:
- `src/main/java/org/chaos/office/util/EnumLocalizer.java`

---

### Task 2: Add Localization Keys to Resource Bundles
**Priority**: P0 (Critical)  
**Estimated Effort**: 1 hour  
**Assignee**: TBD

**Description**: Add all required localization keys for stock status and payment methods to all language files.

**Subtasks**:
- [x] 2.1 Add stock status keys to `messages_en_US.properties`
- [x] 2.2 Add payment method keys to `messages_en_US.properties`
- [x] 2.3 Add stock status keys to `messages_ar.properties`
- [x] 2.4 Add payment method keys to `messages_ar.properties`
- [x] 2.5 Add stock status keys to `messages_fr.properties`
- [x] 2.6 Add payment method keys to `messages_fr.properties`
- [x] 2.7 Verify all existing common button keys exist (ok, cancel, yes, no)

**Acceptance Criteria**:
- All 6 keys added to each of 3 files (18 total additions)
- Keys follow naming convention: `stock.status.*` and `payment.method.*`
- Translations are accurate and natural
- No duplicate keys

**Files to Modify**:
- `src/main/resources/messages/messages_en_US.properties`
- `src/main/resources/messages/messages_ar.properties`
- `src/main/resources/messages/messages_fr.properties`

**Keys to Add**:
```properties
# Stock Status
stock.status.normal=Normal / عادي / Normal
stock.status.low=Low Stock / مخزون منخفض / Stock faible
stock.status.out=Out of Stock / نفذت من المخزون / Rupture de stock

# Payment Methods
payment.method.cash=Cash / نقداً / Espèces
payment.method.card=Card / بطاقة / Carte
payment.method.check=Check / شيك / Chèque
```

---

### Task 3: Write Unit Tests for EnumLocalizer
**Priority**: P0 (Critical)  
**Estimated Effort**: 2 hours  
**Assignee**: TBD

**Description**: Create comprehensive unit tests for the EnumLocalizer class.

**Subtasks**:
- [x] 3.1 Create `EnumLocalizerTest.java` in `src/test/java/org/chaos/office/util/`
- [x] 3.2 Write test for stock status in English
- [x] 3.3 Write test for stock status in Arabic
- [x] 3.4 Write test for stock status in French
- [x] 3.5 Write test for payment methods in English
- [x] 3.6 Write test for payment methods in Arabic
- [x] 3.7 Write test for payment methods in French
- [x] 3.8 Write test for null enum values
- [x] 3.9 Run tests and verify all pass

**Acceptance Criteria**:
- All tests pass
- Test coverage > 90% for EnumLocalizer class
- Tests verify correct localization for all enum values
- Tests verify null handling

**Files to Create**:
- `src/test/java/org/chaos/office/util/EnumLocalizerTest.java`

---

## Phase 2: Fix Arabic Font Support

### Task 4: Update ReportService Font Selection
**Priority**: P0 (Critical)  
**Estimated Effort**: 3 hours  
**Assignee**: TBD

**Description**: Replace the incorrect Chinese font with proper Arabic font support.

**Subtasks**:
- [x] 4.1 Locate `getFontForLocale()` method in `ReportService.java`
- [x] 4.2 Replace font loading logic with fallback chain
- [x] 4.3 Add font paths for Windows (arial.ttf, arialuni.ttf)
- [x] 4.4 Add font paths for Linux (DejaVuSans.ttf)
- [x] 4.5 Add font paths for macOS (Arial Unicode.ttf)
- [x] 4.6 Use `BaseFont.IDENTITY_H` encoding for Unicode support
- [x] 4.7 Use `BaseFont.EMBEDDED` flag for font embedding
- [x] 4.8 Add debug logging for font selection
- [x] 4.9 Add warning logging for fallback to Helvetica
- [x] 4.10 Update JavaDoc comments

**Acceptance Criteria**:
- Font loading tries all paths in fallback chain
- Uses proper Unicode encoding (IDENTITY_H)
- Embeds fonts in PDF
- Logs font selection for debugging
- Falls back gracefully if no fonts available
- Code compiles without errors

**Files to Modify**:
- `src/main/java/org/chaos/office/service/ReportService.java`

---

### Task 5: Test Arabic PDF Generation
**Priority**: P0 (Critical)  
**Estimated Effort**: 2 hours  
**Assignee**: TBD

**Description**: Generate test PDFs in Arabic and verify text renders correctly.

**Subtasks**:
- [x] 5.1 Set application language to Arabic
- [x] 5.2 Create a test bill with Arabic client name
- [x] 5.3 Generate bill receipt PDF
- [x] 5.4 Open PDF in Adobe Reader and verify Arabic text renders
- [x] 5.5 Open PDF in Chrome and verify Arabic text renders
- [x] 5.6 Open PDF in Firefox and verify Arabic text renders
- [x] 5.7 Verify text is right-aligned (RTL)
- [x] 5.8 Verify Arabic characters are properly shaped
- [x] 5.9 Test on Windows (if available)
- [x] 5.10 Test on Linux (if available)
- [x] 5.11 Test on macOS (if available)

**Acceptance Criteria**:
- Arabic text renders correctly in all tested PDF viewers
- Text is right-aligned
- Arabic characters are properly connected
- No empty/blank text
- PDF file size is reasonable (not empty)

**Test Files to Generate**:
- `test_bill_arabic.pdf`
- `test_bill_english.pdf` (for comparison)
- `test_bill_french.pdf` (for comparison)

---

## Phase 3: Localize Dialog Buttons

### Task 6: Update AlertHelper for Localized Buttons
**Priority**: P1 (High)  
**Estimated Effort**: 3 hours  
**Assignee**: TBD

**Description**: Modify AlertHelper to use localized button types instead of default JavaFX buttons.

**Subtasks**:
- [x] 6.1 Add import for `javafx.scene.control.ButtonBar`
- [x] 6.2 Create `createLocalizedOkButton()` private method
- [x] 6.3 Create `createLocalizedCancelButton()` private method
- [x] 6.4 Create `createLocalizedYesButton()` private method
- [x] 6.5 Create `createLocalizedNoButton()` private method
- [x] 6.6 Update `showConfirmation()` to use localized buttons
- [x] 6.7 Update button result checking to use ButtonData instead of ButtonType
- [x] 6.8 Add JavaDoc comments for new methods
- [x] 6.9 Test compilation

**Acceptance Criteria**:
- All dialog methods use localized buttons
- Button functionality preserved (OK still means OK)
- Code compiles without errors
- No regression in dialog behavior

**Files to Modify**:
- `src/main/java/org/chaos/office/util/AlertHelper.java`

---

### Task 7: Test Localized Dialogs
**Priority**: P1 (High)  
**Estimated Effort**: 1.5 hours  
**Assignee**: TBD

**Description**: Manually test all dialogs in all three languages.

**Subtasks**:
- [x] 7.1 Set language to English, trigger delete part dialog
- [x] 7.2 Verify buttons show "OK" and "Cancel"
- [x] 7.3 Set language to Arabic, trigger delete part dialog
- [x] 7.4 Verify buttons show "موافق" and "إلغاء"
- [x] 7.5 Set language to French, trigger delete category dialog
- [x] 7.6 Verify buttons show "OK" and "Annuler"
- [x] 7.7 Test error dialogs in all languages
- [x] 7.8 Test info dialogs in all languages
- [x] 7.9 Verify button functionality (clicking OK/Cancel works)
- [x] 7.10 Take screenshots for documentation

**Acceptance Criteria**:
- All dialog buttons display in correct language
- Button functionality works correctly
- No visual glitches or layout issues
- Screenshots captured for all languages

---

### Task 8: Update AlertHelper Unit Tests
**Priority**: P2 (Medium)  
**Estimated Effort**: 1 hour  
**Assignee**: TBD

**Description**: Add tests for localized button creation.

**Subtasks**:
- [x] 8.1 Add test for English button creation
- [x] 8.2 Add test for Arabic button creation
- [x] 8.3 Add test for French button creation
- [x] 8.4 Verify tests don't require UI thread (use mocking if needed)
- [x] 8.5 Run tests and verify all pass

**Acceptance Criteria**:
- Tests verify button text is localized
- Tests pass consistently
- No UI thread dependencies

**Files to Modify**:
- `src/test/java/org/chaos/office/util/AlertHelperTest.java`

---

## Phase 4: Localize Enum Values in Exports

### Task 9: Update PDFExporter to Use EnumLocalizer
**Priority**: P1 (High)  
**Estimated Effort**: 1.5 hours  
**Assignee**: TBD

**Description**: Replace enum toString() calls with EnumLocalizer in PDFExporter.

**Subtasks**:
- [x] 9.1 Add import for `EnumLocalizer`
- [x] 9.2 Find `item.getStatus().toString()` call (around line 437)
- [x] 9.3 Replace with `EnumLocalizer.getLocalizedStockStatus(item.getStatus())`
- [x] 9.4 Find payment method `toString()` calls in `addSalesContent()`
- [x] 9.5 Replace with `EnumLocalizer.getLocalizedPaymentMethod()`
- [x] 9.6 Search for any other enum toString() calls
- [x] 9.7 Test compilation
- [x] 9.8 Generate test PDF and verify enum values are localized

**Acceptance Criteria**:
- All enum toString() calls replaced
- Code compiles without errors
- Test PDFs show localized enum values
- No regression in PDF layout

**Files to Modify**:
- `src/main/java/org/chaos/office/reports/services/PDFExporter.java`

---

### Task 10: Update ExcelExporter to Use EnumLocalizer
**Priority**: P1 (High)  
**Estimated Effort**: 1 hour  
**Assignee**: TBD

**Description**: Replace enum toString() calls with EnumLocalizer in ExcelExporter.

**Subtasks**:
- [x] 10.1 Add import for `EnumLocalizer`
- [x] 10.2 Find `item.getStatus().toString()` call (around line 250)
- [x] 10.3 Replace with `EnumLocalizer.getLocalizedStockStatus(item.getStatus())`
- [x] 10.4 Search for any payment method toString() calls
- [x] 10.5 Replace with `EnumLocalizer.getLocalizedPaymentMethod()`
- [x] 10.6 Test compilation
- [x] 10.7 Generate test Excel file and verify enum values

**Acceptance Criteria**:
- All enum toString() calls replaced
- Code compiles without errors
- Test Excel files show localized enum values

**Files to Modify**:
- `src/main/java/org/chaos/office/reports/services/ExcelExporter.java`

---

### Task 11: Update CSVExporter to Use EnumLocalizer
**Priority**: P1 (High)  
**Estimated Effort**: 1 hour  
**Assignee**: TBD

**Description**: Replace enum toString() calls with EnumLocalizer in CSVExporter.

**Subtasks**:
- [x] 11.1 Add import for `EnumLocalizer`
- [x] 11.2 Search for all `getStatus().toString()` calls
- [x] 11.3 Replace with `EnumLocalizer.getLocalizedStockStatus()`
- [x] 11.4 Search for payment method toString() calls
- [x] 11.5 Replace with `EnumLocalizer.getLocalizedPaymentMethod()`
- [x] 11.6 Test compilation
- [x] 11.7 Generate test CSV file and verify enum values

**Acceptance Criteria**:
- All enum toString() calls replaced
- Code compiles without errors
- Test CSV files show localized enum values

**Files to Modify**:
- `src/main/java/org/chaos/office/reports/services/CSVExporter.java`

---

### Task 12: Update ReportsController to Use EnumLocalizer
**Priority**: P1 (High)  
**Estimated Effort**: 0.5 hours  
**Assignee**: TBD

**Description**: Replace enum toString() calls with EnumLocalizer in UI controller.

**Subtasks**:
- [x] 12.1 Add import for `EnumLocalizer`
- [x] 12.2 Find `item.getStatus().toString()` call (around line 289)
- [x] 12.3 Replace with `EnumLocalizer.getLocalizedStockStatus(item.getStatus())`
- [x] 12.4 Test compilation
- [x] 12.5 Run application and verify UI shows localized enum values

**Acceptance Criteria**:
- Enum toString() call replaced
- Code compiles without errors
- UI displays localized enum values

**Files to Modify**:
- `src/main/java/org/chaos/office/controller/ReportsController.java`

---

### Task 13: Search for Remaining Enum toString() Calls
**Priority**: P1 (High)  
**Estimated Effort**: 1 hour  
**Assignee**: TBD

**Description**: Search entire codebase for any remaining enum toString() calls that need localization.

**Subtasks**:
- [x] 13.1 Search for `getStatus().toString()` in all Java files
- [x] 13.2 Search for `getPaymentMethod().toString()` in all Java files
- [x] 13.3 Review each occurrence and determine if it needs localization
- [x] 13.4 Update any remaining occurrences
- [x] 13.5 Document any intentional toString() usage (e.g., logging)

**Acceptance Criteria**:
- All user-facing enum displays are localized
- Logging and debugging toString() calls are documented
- No enum values displayed in English when other language selected

---

## Phase 5: Fix Test Failures

### Task 14: Fix BillServiceOptionalFieldsTest
**Priority**: P1 (High)  
**Estimated Effort**: 1 hour  
**Assignee**: TBD

**Description**: Update test setup to ensure parts have valid categories.

**Subtasks**:
- [x] 14.1 Locate `setUp()` method in `BillServiceOptionalFieldsTest.java`
- [x] 14.2 Update category retrieval to use `orElseThrow()`
- [x] 14.3 Add clear error message if no categories exist
- [x] 14.4 Ensure `testPart.setCategory(category)` is called
- [x] 14.5 Add logging for test part creation
- [x] 14.6 Run test `testSaveBillWithEmptyClientName` and verify it passes
- [x] 14.7 Run test `testSaveBillWithEmptyClientPhone` and verify it passes
- [x] 14.8 Run test `testSaveBillWithBothFieldsEmpty` and verify it passes
- [x] 14.9 Run all three tests together and verify they pass
- [x] 14.10 Run tests multiple times to ensure consistency

**Acceptance Criteria**:
- All three tests pass consistently
- Test setup fails fast with clear error if no categories
- Tests can run multiple times without conflicts
- Test cleanup works correctly

**Files to Modify**:
- `src/test/java/org/chaos/office/service/BillServiceOptionalFieldsTest.java`

---

## Phase 6: Integration Testing

### Task 15: Comprehensive PDF Testing
**Priority**: P1 (High)  
**Estimated Effort**: 2 hours  
**Assignee**: TBD

**Description**: Generate and verify PDFs in all languages with all features.

**Subtasks**:
- [x] 15.1 Generate bill receipt in English
- [x] 15.2 Generate bill receipt in Arabic
- [x] 15.3 Generate bill receipt in French
- [x] 15.4 Generate inventory report in English
- [x] 15.5 Generate inventory report in Arabic (verify stock status)
- [x] 15.6 Generate inventory report in French (verify stock status)
- [-] 15.7 Generate sales report in English
- [-] 15.8 Generate sales report in Arabic (verify payment methods)
- [-] 15.9 Generate sales report in French (verify payment methods)
- [-] 15.10 Open all PDFs in multiple viewers and verify rendering

**Acceptance Criteria**:
- All PDFs generate without errors
- All text is in correct language
- Arabic text renders correctly
- Enum values are localized
- No mixed-language text

**Test Artifacts**:
- 9 test PDF files (3 types × 3 languages)

---

### Task 16: Comprehensive Dialog Testing
**Priority**: P1 (High)  
**Estimated Effort**: 1 hour  
**Assignee**: TBD

**Description**: Test all dialogs in all languages.

**Subtasks**:
- [ ] 16.1 Test delete part confirmation in English
- [ ] 16.2 Test delete part confirmation in Arabic
- [ ] 16.3 Test delete part confirmation in French
- [ ] 16.4 Test delete category confirmation in all languages
- [ ] 16.5 Test error dialogs in all languages
- [ ] 16.6 Test info dialogs in all languages
- [ ] 16.7 Test out-of-stock alert in all languages
- [ ] 16.8 Verify button functionality in all cases

**Acceptance Criteria**:
- All dialogs display correct language
- All buttons work correctly
- No visual glitches

---

### Task 17: Export Testing (Excel and CSV)
**Priority**: P2 (Medium)  
**Estimated Effort**: 1 hour  
**Assignee**: TBD

**Description**: Test Excel and CSV exports with localized enum values.

**Subtasks**:
- [ ] 17.1 Export inventory report to Excel in English
- [ ] 17.2 Export inventory report to Excel in Arabic
- [ ] 17.3 Export inventory report to Excel in French
- [ ] 17.4 Export sales report to CSV in English
- [ ] 17.5 Export sales report to CSV in Arabic
- [ ] 17.6 Export sales report to CSV in French
- [ ] 17.7 Open all files and verify enum values are localized
- [ ] 17.8 Verify no encoding issues with Arabic text

**Acceptance Criteria**:
- All exports complete successfully
- Enum values are localized
- Arabic text displays correctly in Excel/CSV
- No encoding issues

---

### Task 18: Run Full Test Suite
**Priority**: P1 (High)  
**Estimated Effort**: 0.5 hours  
**Assignee**: TBD

**Description**: Run all automated tests to ensure no regressions.

**Subtasks**:
- [x] 18.1 Run `mvn clean test`
- [x] 18.2 Verify all tests pass
- [ ] 18.3 Check test coverage report
- [ ] 18.4 Fix any failing tests
- [ ] 18.5 Re-run tests to confirm fixes

**Acceptance Criteria**:
- 100% test pass rate
- No new test failures
- Test coverage maintained or improved
- Build succeeds

---

## Phase 7: Documentation and Cleanup

### Task 19: Update Code Documentation
**Priority**: P2 (Medium)  
**Estimated Effort**: 1.5 hours  
**Assignee**: TBD

**Description**: Add/update JavaDoc comments for all modified code.

**Subtasks**:
- [ ] 19.1 Review `EnumLocalizer` JavaDoc
- [ ] 19.2 Review `AlertHelper` JavaDoc
- [ ] 19.3 Review `ReportService` JavaDoc
- [ ] 19.4 Add comments explaining font fallback logic
- [ ] 19.5 Add comments explaining button localization
- [ ] 19.6 Ensure all public methods have JavaDoc
- [ ] 19.7 Run JavaDoc generation and check for warnings

**Acceptance Criteria**:
- All public methods have JavaDoc
- JavaDoc is clear and accurate
- No JavaDoc warnings
- Code is well-commented

---

### Task 20: Create Release Notes
**Priority**: P2 (Medium)  
**Estimated Effort**: 1 hour  
**Assignee**: TBD

**Description**: Document all changes for release notes.

**Subtasks**:
- [ ] 20.1 List all bug fixes
- [ ] 20.2 List all new features
- [ ] 20.3 Document font requirements
- [ ] 20.4 Document known limitations
- [ ] 20.5 Add upgrade instructions (if any)
- [ ] 20.6 Review and finalize

**Acceptance Criteria**:
- Release notes are complete
- All changes documented
- Clear and user-friendly language

**Deliverable**:
- `RELEASE_NOTES.md` or similar

---

### Task 21: Update User Documentation
**Priority**: P3 (Low)  
**Estimated Effort**: 1 hour  
**Assignee**: TBD

**Description**: Update user-facing documentation with localization information.

**Subtasks**:
- [ ] 21.1 Document language selection process
- [ ] 21.2 Document font requirements for Arabic
- [ ] 21.3 Add troubleshooting section for PDF rendering issues
- [ ] 21.4 Update screenshots if needed
- [ ] 21.5 Review and finalize

**Acceptance Criteria**:
- Documentation is accurate
- Clear instructions for users
- Troubleshooting guide included

---

### Task 22: Code Review and Cleanup
**Priority**: P1 (High)  
**Estimated Effort**: 1 hour  
**Assignee**: TBD

**Description**: Final code review and cleanup before merge.

**Subtasks**:
- [ ] 22.1 Remove any commented-out code
- [ ] 22.2 Remove debug print statements
- [ ] 22.3 Verify consistent code style
- [ ] 22.4 Run code formatter
- [ ] 22.5 Check for unused imports
- [ ] 22.6 Verify no compiler warnings
- [ ] 22.7 Run static analysis (if available)
- [ ] 22.8 Address any code quality issues

**Acceptance Criteria**:
- Code is clean and consistent
- No commented-out code
- No compiler warnings
- Passes static analysis

---

### Task 23: Localize PartSearchComponent Text
**Priority**: P1 (High)  
**Estimated Effort**: 1 hour  
**Assignee**: TBD

**Description**: Replace hardcoded English text in PartSearchComponent with localized strings.

**Subtasks**:
- [x] 23.1 Add localization keys to all message properties files
- [x] 23.2 Replace hardcoded search placeholder (line 87)
- [x] 23.3 Replace hardcoded "Search:" label (line 124)
- [x] 23.4 Replace hardcoded "Category:" label (line 129)
- [x] 23.5 Test in all three languages
- [x] 23.6 Verify placeholder text updates when language changes

**Acceptance Criteria**:
- All text comes from resource bundles
- Text changes when language is changed
- No hardcoded English strings remain

**Files to Modify**:
- `src/main/java/org/chaos/office/view/PartSearchComponent.java`
- `src/main/resources/messages/messages_en_US.properties`
- `src/main/resources/messages/messages_ar.properties`
- `src/main/resources/messages/messages_fr.properties`

**Keys to Add**:
```properties
search.label=Search / بحث / Rechercher
search.category.label=Category / الفئة / Catégorie
search.parts.placeholder=Search parts... / البحث عن القطع... / Rechercher des pièces...
```

---

### Task 24: Localize Date Range Separator
**Priority**: P1 (High)  
**Estimated Effort**: 1.5 hours  
**Assignee**: TBD

**Description**: Replace hardcoded " to " in date ranges with localized separator.

**Subtasks**:
- [x] 24.1 Add `date.range.separator` key to all message properties files
- [x] 24.2 Update `SalesReportData.getParameters()` to use localized separator
- [x] 24.3 Search for other date range formatting code
- [x] 24.4 Test PDFs with date ranges in all languages
- [x] 24.5 Test Excel exports with date ranges
- [x] 24.6 Test CSV exports with date ranges

**Acceptance Criteria**:
- Date ranges use localized separator
- English shows "to", Arabic shows "إلى", French shows "à"
- No hardcoded " to " in date range displays

**Files to Modify**:
- `src/main/java/org/chaos/office/reports/models/SalesReportData.java`
- `src/main/resources/messages/messages_en_US.properties`
- `src/main/resources/messages/messages_ar.properties`
- `src/main/resources/messages/messages_fr.properties`

**Keys to Add**:
```properties
date.range.separator= to  / إلى / à 
```

**Code Change**:
```java
// OLD:
params.put(LocaleManager.getString("report.date.range"), startDate + " to " + endDate);

// NEW:
String separator = LocaleManager.getString("date.range.separator");
params.put(LocaleManager.getString("report.date.range"), startDate + separator + endDate);
```

---

### Task 25: Fix CSV UTF-8 Encoding
**Priority**: P0 (Critical)  
**Estimated Effort**: 2 hours  
**Assignee**: TBD

**Description**: Fix CSV export encoding to properly support Arabic text.

**Subtasks**:
- [x] 25.1 Locate CSV writing code in `CSVExporter.java`
- [x] 25.2 Change file writer to use UTF-8 encoding
- [x] 25.3 Add UTF-8 BOM (Byte Order Mark) for Excel compatibility
- [x] 25.4 Test CSV export in English
- [x] 25.5 Test CSV export in Arabic - verify no ??? characters
- [x] 25.6 Test CSV export in French
- [x] 25.7 Open CSV in Excel and verify Arabic displays correctly
- [x] 25.8 Open CSV in text editor and verify encoding

**Acceptance Criteria**:
- CSV files use UTF-8 encoding
- UTF-8 BOM is included
- Arabic text displays correctly in Excel
- No question marks or garbled characters

**Files to Modify**:
- `src/main/java/org/chaos/office/reports/services/CSVExporter.java`

**Code Pattern**:
```java
// Add UTF-8 BOM
FileOutputStream fos = new FileOutputStream(file);
fos.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF}); // UTF-8 BOM

// Use UTF-8 encoding
OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
CSVWriter csvWriter = new CSVWriter(writer);
```

---

### Task 26: Improve Category Card Text Clarity
**Priority**: P2 (Medium)  
**Estimated Effort**: 1 hour  
**Assignee**: TBD

**Description**: Improve the visual clarity of category names on category cards.

**Subtasks**:
- [x] 26.1 Locate category card rendering code
- [x] 26.2 Increase font size for category names
- [x] 26.3 Make category names bold
- [x] 26.4 Ensure good contrast with background
- [ ] 26.5 Test with English text
- [ ] 26.6 Test with Arabic text (ensure proper font)
- [ ] 26.7 Test with French text
- [ ] 26.8 Get user feedback on readability

**Acceptance Criteria**:
- Category names are clearly readable
- Arabic text displays with proper font
- Good visual hierarchy on cards
- User confirms improved clarity

**Files to Modify**:
- `src/main/java/org/chaos/office/view/CategoryManagementView.java` (or related view)
- CSS files (if styling is in CSS)

**Suggested Changes**:
- Font size: 14pt → 16pt or 18pt
- Font weight: normal → bold
- Consider using the same Unicode font as PDFs for Arabic

---

### Task 27: Implement Immediate Branding Updates
**Priority**: P2 (Medium)  
**Estimated Effort**: 2 hours  
**Assignee**: TBD

**Description**: Make window title and icon update immediately when branding settings are saved.

**Subtasks**:
- [x] 27.1 Find where window title is set on application startup
- [x] 27.2 Create method to update window title dynamically
- [x] 27.3 Create method to update window icon dynamically
- [x] 27.4 Call update methods when branding settings are saved
- [ ] 27.5 Test changing store name - verify title updates immediately
- [ ] 27.6 Test changing logo - verify icon updates immediately
- [ ] 27.7 Test that changes persist after restart
- [x] 27.8 Handle edge cases (empty name, invalid logo)

**Acceptance Criteria**:
- Window title updates without restart
- Window icon updates without restart
- Changes are visible immediately
- No errors when updating

**Files to Modify**:
- `src/main/java/org/chaos/office/ChaOfficeApplication.java` (or main application class)
- `src/main/java/org/chaos/office/controller/SettingsController.java`
- `src/main/java/org/chaos/office/service/BrandingService.java`

**Implementation Approach**:
```java
// In SettingsController or BrandingService
private void updateApplicationBranding() {
    Stage primaryStage = getPrimaryStage(); // Get reference to main window
    
    // Update title
    String storeName = brandingService.getBrandingSettings().getStoreName();
    if (storeName != null && !storeName.isEmpty()) {
        primaryStage.setTitle(storeName);
    } else {
        primaryStage.setTitle(LocaleManager.getString("app.title"));
    }
    
    // Update icon
    Image logo = brandingService.getApplicationLogo();
    if (logo != null) {
        primaryStage.getIcons().clear();
        primaryStage.getIcons().add(logo);
    }
}
```

---

## Summary

**Total Tasks**: 27 (was 26)  
**Total Subtasks**: 238+ (was 230+)  
**Estimated Total Effort**: 39.5 hours (was 37.5 hours) - approximately 5 days

**Critical Path**:
1. Phase 1 (Foundation) → Phase 2 (Font Fix) → Phase 6 (Testing)
2. Phase 1 (Foundation) → Phase 3 (Dialogs) → Phase 6 (Testing)
3. Phase 1 (Foundation) → Phase 4 (Enums) → Phase 6 (Testing)
4. Phase 5 (Tests) can run in parallel with other phases
5. Tasks 23-27 should be done in Phase 4 or Phase 6

**New Dependencies**:
- Task 23 depends on Task 2 (Resource bundles)
- Task 24 depends on Task 2 (Resource bundles)
- Task 25 is independent (can be done anytime)
- Task 26 is independent (can be done anytime)
- **NEW** Task 27 is independent (can be done anytime)

**Risk Areas**:
- Task 4-5: Font availability on different systems
- Task 15: Manual PDF verification is time-consuming
- Task 14: May uncover other test issues
- Task 25: CSV encoding may affect Excel compatibility on different systems
- Task 26: Subjective - may need multiple iterations based on user feedback
- **NEW** Task 27: Need reference to primary Stage, may require architecture changes

---

**Status**: Ready for Implementation  
**Last Updated**: 2026-02-13  
**Version**: 1.2 (Added Task 27 for immediate branding updates)
