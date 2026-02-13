# Bill Receipt Localization Fixes - Requirements

## 1. Overview

This spec addresses critical localization issues in the ChaOffice application, specifically:
1. Bill receipts are generated in English even when the application language is set to Arabic or French
2. Arabic bill receipt titles appear empty/blank after PDF generation due to incorrect font selection
3. Dialog buttons (OK/Cancel) remain in English regardless of selected language
4. Enum values (stock status, payment methods) display in English in all reports and PDFs
5. Search interface has hardcoded English text (placeholder, labels)
6. Date range separator "to" is hardcoded in English in all reports
7. CSV exports show garbled Arabic text due to encoding issues
8. Category card text needs better clarity/readability
9. Test failures in `BillServiceOptionalFieldsTest` related to part creation with invalid category IDs

## 2. Problem Statement

### 2.1 Language Not Applied to Bill Receipts
**Current Behavior:** When a user generates a bill receipt PDF with the application language set to Arabic (or French), the PDF is still generated in English.

**Expected Behavior:** The bill receipt PDF should be generated in the currently selected language, matching the application UI language.

**Impact:** High - Users in Arabic and French-speaking markets cannot use the application effectively for customer-facing documents.

### 2.2 Empty Arabic Titles in PDF
**Current Behavior:** When generating a bill receipt in Arabic, the title and field labels appear empty or blank in the PDF.

**Root Cause:** The `ReportService.getFontForLocale()` method attempts to use `STSong-Light` font with `UniGB-UCS2-H` encoding, which is designed for Chinese characters, not Arabic. When this fails, it falls back to Helvetica, which doesn't support Arabic characters.

**Expected Behavior:** Arabic text should render correctly in PDFs with proper font support.

**Impact:** Critical - Makes the application unusable for Arabic-speaking users.

### 2.3 Dialog Buttons Not Localized
**Current Behavior:** Dialog buttons (OK, Cancel, Yes, No) in confirmation dialogs and alerts appear in English regardless of the selected application language.

**Examples:**
- Delete part confirmation dialog shows "OK" and "Cancel" buttons in English even when the application is in Arabic
- Category delete confirmation shows English buttons
- All `AlertHelper` dialogs use default JavaFX button types which are not localized

**Root Cause:** The `AlertHelper` class uses JavaFX's built-in `ButtonType.OK` and `ButtonType.CANCEL`, which are not automatically localized by JavaFX. JavaFX requires explicit button text customization for localization.

**Expected Behavior:** Dialog buttons should display in the currently selected language:
- English: "OK", "Cancel"
- Arabic: "موافق", "إلغاء"
- French: "OK", "Annuler"

**Impact:** High - Creates inconsistent user experience where the UI is in one language but dialogs are in English.

### 2.4 Enum Values Not Localized in PDFs
**Current Behavior:** Enum values like `StockStatus` are displayed using their English `toString()` representation in PDFs, even when the PDF is generated in Arabic or French.

**Examples:**
- In inventory reports, stock status shows "OUT_OF_STOCK", "LOW_STOCK", "NORMAL" in English
- In Arabic PDFs, these appear as English text: "Out of Stock" instead of "نفذت من المخزون"
- Payment method enums also display in English

**Root Cause:** The `PDFExporter` class uses `item.getStatus().toString()` which returns the enum's hardcoded English display name. The enum doesn't support localization.

**Code Location:**
```java
// In PDFExporter.java line 437
String statusText = item.getStatus().toString();
```

**Expected Behavior:** Enum values should be localized using resource bundles:
- English: "Out of Stock", "Low Stock", "Normal"
- Arabic: "نفذت من المخزون", "مخزون منخفض", "عادي"
- French: "Rupture de stock", "Stock faible", "Normal"

**Impact:** High - Professional reports should be fully localized for international customers.

### 2.5 Hardcoded English Text in PartSearchComponent
**Current Behavior:** The search field placeholder and labels in the PartSearchComponent are hardcoded in English.

**Examples:**
- Search field placeholder: "Search parts by name, maker, category..." (hardcoded)
- Search label: "Search:" (hardcoded)
- Category label: "Category:" (hardcoded)

**Root Cause:** The `PartSearchComponent` class uses hardcoded English strings instead of `LocaleManager.getString()`.

**Code Location:**
```java
// Line 87 in PartSearchComponent.java
this.searchField.setPromptText("Search parts by name, maker, category...");

// Line 124
Label searchLabel = new Label("Search:");

// Line 129
Label categoryLabel = new Label("Category:");
```

**Expected Behavior:** All text should come from resource bundles and change with the selected language.

**Impact:** High - Search functionality appears in English even when application is in Arabic or French.

### 2.6 Hardcoded "to" in Date Range Display
**Current Behavior:** The date range in reports shows "to" in English even when the report is in Arabic or French.

**Examples:**
- PDF shows: "نطاق التاريخ: 01-02-2026 to 2026-02-13" (Arabic label but English "to")
- Should show: "نطاق التاريخ: 01-02-2026 إلى 2026-02-13"

**Root Cause:** The `SalesReportData.getParameters()` method hardcodes " to " in the date range string.

**Code Location:**
```java
// Line 60 in SalesReportData.java
params.put(LocaleManager.getString("report.date.range"), startDate + " to " + endDate);
```

**Expected Behavior:** The word "to" should be localized:
- English: "to"
- Arabic: "إلى"
- French: "à"

**Impact:** High - Makes reports look unprofessional with mixed languages.

### 2.7 CSV Export Encoding Issues
**Current Behavior:** CSV exports in Arabic show garbled text with question marks (???).

**Example:**
```
??? ???????,????? ??????????????,2026-02-01 to 2026-02-13
```

**Root Cause:** CSV files are not being written with UTF-8 encoding, causing Arabic characters to be corrupted.

**Expected Behavior:** CSV files should use UTF-8 encoding with BOM to ensure Arabic text displays correctly in Excel and other applications.

**Impact:** High - CSV exports are unusable for Arabic users.

### 2.8 Category Card Text Clarity
**Current Behavior:** Category names on cards may not be clear/readable enough, especially for Arabic text.

**User Feedback:** "make the string of the card describing the category a little bit 'clearer' I don't know what change to the font must be made"

**Expected Behavior:** Category card text should be more prominent and readable, possibly with:
- Larger font size
- Bold font weight
- Better contrast
- Proper font for Arabic text

**Impact:** Medium - Affects usability but not functionality.

### 2.9 Branding Changes Require Application Restart
**Current Behavior:** When users change the store name or logo in settings, they must restart the application to see the changes reflected in the window title and icon.

**Expected Behavior:** Changes to store name and logo should take effect immediately:
- Window title should update to show new store name
- Application icon should update to show new logo
- No restart required

**Impact:** Medium - Affects user experience when customizing branding.

### 2.10 Test Failures in BillServiceOptionalFieldsTest
**Current Behavior:** Three tests fail because test parts are created with `category = null` or invalid category IDs (ID = 0), causing database constraint violations or unexpected behavior.

**Failing Tests:**
- `testSaveBillWithEmptyClientName`
- `testSaveBillWithEmptyClientPhone`
- `testSaveBillWithBothFieldsEmpty`

**Root Cause:** The test setup creates a part without ensuring it has a valid category from the database.

**Expected Behavior:** Tests should pass by creating test parts with valid categories.

**Impact:** Medium - Prevents CI/CD pipeline from passing and indicates potential production issues.

## 3. User Stories

### 3.1 Multi-Language Bill Receipts
**As a** business owner using ChaOffice in an Arabic-speaking country  
**I want** bill receipts to be generated in Arabic when my application language is set to Arabic  
**So that** my customers can read and understand their receipts

**Acceptance Criteria:**
- [ ] 1.1 When application language is set to Arabic, bill receipt PDFs are generated in Arabic
- [ ] 1.2 When application language is set to French, bill receipt PDFs are generated in French
- [ ] 1.3 When application language is set to English, bill receipt PDFs are generated in English
- [ ] 1.4 All text in the PDF (title, labels, values) respects the selected language
- [ ] 1.5 Currency formatting respects the locale settings

### 3.2 Arabic Text Rendering in PDFs
**As a** business owner using ChaOffice in Arabic  
**I want** Arabic text to display correctly in PDF receipts  
**So that** the receipts are readable and professional

**Acceptance Criteria:**
- [ ] 2.1 Arabic title "إيصال الفاتورة" displays correctly in PDF
- [ ] 2.2 Arabic field labels (Bill ID, Date, Client Name, etc.) display correctly
- [ ] 2.3 Arabic text is right-aligned (RTL direction)
- [ ] 2.4 Arabic characters are properly shaped and connected
- [ ] 2.5 Mixed Arabic and English text (e.g., numbers, dates) displays correctly
- [ ] 2.6 Font fallback mechanism works when Arabic fonts are not available

### 3.3 Localized Dialog Buttons
**As a** user who has selected Arabic as my application language  
**I want** all dialog buttons (OK, Cancel, etc.) to appear in Arabic  
**So that** I have a consistent, fully localized experience

**Acceptance Criteria:**
- [ ] 3.1 Delete confirmation dialogs show localized button text
- [ ] 3.2 Error dialogs show localized button text
- [ ] 3.3 Information dialogs show localized button text
- [ ] 3.4 All confirmation dialogs show localized button text
- [ ] 3.5 Button text changes immediately when language is changed in settings
- [ ] 3.6 Button text is properly aligned for RTL languages (Arabic)

### 3.4 Localized Enum Values in Reports
**As a** business owner generating inventory reports in Arabic  
**I want** stock status values to appear in Arabic  
**So that** my reports are professional and fully localized

**Acceptance Criteria:**
- [ ] 4.1 Stock status "Out of Stock" appears as "نفذت من المخزون" in Arabic PDFs
- [ ] 4.2 Stock status "Low Stock" appears as "مخزون منخفض" in Arabic PDFs
- [ ] 4.3 Stock status "Normal" appears as "عادي" in Arabic PDFs
- [ ] 4.4 Payment method enums are localized in sales reports
- [ ] 4.5 All enum values in Excel exports are localized
- [ ] 4.6 All enum values in CSV exports are localized

### 3.5 Fully Localized Search Interface
**As a** user who has selected Arabic as my application language  
**I want** the search interface to be fully in Arabic  
**So that** I can easily search for parts without seeing English text

**Acceptance Criteria:**
- [ ] 5.1 Search field placeholder text is localized
- [ ] 5.2 "Search:" label is localized
- [ ] 5.3 "Category:" label is localized
- [ ] 5.4 All search-related text changes with language selection

### 3.6 Localized Date Range Separator
**As a** business owner generating reports in Arabic  
**I want** the date range to use Arabic "إلى" instead of English "to"  
**So that** my reports are fully professional and localized

**Acceptance Criteria:**
- [ ] 6.1 Date range in PDFs uses localized separator
- [ ] 6.2 Date range in Excel exports uses localized separator
- [ ] 6.3 Date range in CSV exports uses localized separator
- [ ] 6.4 English uses "to", Arabic uses "إلى", French uses "à"

### 3.7 Proper CSV Encoding for Arabic
**As a** user exporting reports to CSV in Arabic  
**I want** the CSV file to display Arabic text correctly  
**So that** I can open it in Excel without seeing garbled text

**Acceptance Criteria:**
- [ ] 7.1 CSV files are written with UTF-8 encoding
- [ ] 7.2 CSV files include UTF-8 BOM for Excel compatibility
- [ ] 7.3 Arabic text displays correctly when opened in Excel
- [ ] 7.4 Arabic text displays correctly in text editors
- [ ] 7.5 No question marks or garbled characters

### 3.8 Clear and Readable Category Cards
**As a** user viewing category cards  
**I want** category names to be clear and easy to read  
**So that** I can quickly identify categories, especially in Arabic

**Acceptance Criteria:**
- [ ] 8.1 Category names use appropriate font size
- [ ] 8.2 Category names have good contrast
- [ ] 8.3 Arabic text is clearly readable
- [ ] 8.4 Font weight/style makes text prominent

### 3.9 Immediate Branding Updates
**As a** business owner customizing my store branding  
**I want** to see my changes immediately without restarting  
**So that** I can quickly preview and adjust my branding

**Acceptance Criteria:**
- [ ] 9.1 Window title updates immediately when store name is saved
- [ ] 9.2 Application icon updates immediately when logo is saved
- [ ] 9.3 No application restart required
- [ ] 9.4 Changes persist after application restart

### 3.10 Reliable Test Suite
**As a** developer working on ChaOffice  
**I want** all tests to pass consistently  
**So that** I can trust the test suite and deploy with confidence

**Acceptance Criteria:**
- [ ] 5.1 `BillServiceOptionalFieldsTest.testSaveBillWithEmptyClientName` passes
- [ ] 5.2 `BillServiceOptionalFieldsTest.testSaveBillWithEmptyClientPhone` passes
- [ ] 5.3 `BillServiceOptionalFieldsTest.testSaveBillWithBothFieldsEmpty` passes
- [ ] 5.4 Test parts are created with valid categories
- [ ] 5.5 Test cleanup properly removes all test data
- [ ] 5.6 Tests can run multiple times without conflicts

## 4. Technical Requirements

### 4.1 Font Support for Arabic
**Requirement:** The PDF generation system must use fonts that support Arabic Unicode characters.

**Constraints:**
- Must work on Windows, macOS, and Linux
- Should embed fonts in PDF for portability
- Must handle Arabic text shaping and ligatures
- Should support RTL (right-to-left) text direction

**Recommended Fonts:**
- Windows: `arial.ttf`, `arialuni.ttf`
- Linux: DejaVu Sans, Noto Sans Arabic
- macOS: Arial Unicode MS
- Fallback: Embedded open-source Arabic font

### 4.2 Locale-Aware PDF Generation
**Requirement:** The `ReportService.generateBillPDF()` method must respect the current application locale.

**Implementation Notes:**
- Use `LocaleManager.getCurrentLocale()` to determine language
- Use `LocaleManager.getString()` for all text labels
- Apply RTL direction for Arabic and Hebrew
- Use locale-specific date and number formatting

### 4.3 Localized Dialog Buttons
**Requirement:** All JavaFX dialogs must display buttons in the currently selected language.

**Implementation Notes:**
- Replace `ButtonType.OK` and `ButtonType.CANCEL` with custom localized button types
- Create custom `ButtonType` instances with localized text from resource bundles
- Update `AlertHelper` class to use localized button types
- Ensure button text updates when language changes

**Localization Keys Required:**
```properties
# Add to all messages_*.properties files
common.ok=OK / موافق / OK
common.cancel=Cancel / إلغاء / Annuler
common.yes=Yes / نعم / Oui
common.no=No / لا / Non
```

### 4.4 Localized Enum Values
**Requirement:** All enum values displayed in reports and PDFs must be localized.

**Implementation Notes:**
- Add localization keys for all enum display values
- Create utility method to get localized enum text
- Update `StockStatus` enum to support localization
- Update `PaymentMethod` enum to support localization
- Replace `toString()` calls with localized text lookups

**Localization Keys Required:**
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

### 4.5 Test Data Integrity
**Requirement:** All tests must create valid test data that satisfies database constraints.

**Implementation Notes:**
- Test parts must have valid category IDs
- Use existing categories from the database or create test categories
- Ensure proper cleanup in `@AfterAll` methods
- Use `@Order` annotation to control test execution order

## 5. Non-Functional Requirements

### 5.1 Performance
- PDF generation should complete within 2 seconds for typical bills (1-20 line items)
- Font loading should be cached to avoid repeated file I/O

### 5.2 Compatibility
- PDFs must be readable in Adobe Reader, Chrome PDF viewer, and mobile PDF apps
- Arabic text must render correctly across all PDF viewers

### 5.3 Maintainability
- Font selection logic should be centralized and reusable
- Locale-specific formatting should use existing `LocaleManager` utilities
- Test fixtures should be reusable across test classes

## 6. Out of Scope

The following are explicitly out of scope for this spec:
- Adding new languages beyond the existing English, French, and Arabic
- Redesigning the PDF layout or styling
- Adding custom font selection in settings
- Implementing PDF encryption or digital signatures
- Fixing other unrelated test failures (e.g., `BrandingServiceTest`)
- Localizing JavaFX system dialogs (file choosers, color pickers, etc.)
- Adding bi-directional text support beyond basic RTL

## 7. Dependencies

### 7.1 External Dependencies
- OpenPDF library (already in use)
- System fonts (Arial, DejaVu Sans, etc.)
- `LocaleManager` utility class
- `CurrencyFormatter` utility class

### 7.2 Internal Dependencies
- `ReportService` class
- `AlertHelper` class
- `BillService` class
- `PartService` class
- `CategoryService` class
- `StockStatus` enum
- `PaymentMethod` enum
- Message properties files (`messages_ar.properties`, etc.)

## 8. Risks and Mitigations

### 8.1 Font Availability Risk
**Risk:** Required Arabic fonts may not be available on all systems.

**Mitigation:** 
- Implement font fallback chain
- Consider embedding a free Arabic font (e.g., Noto Sans Arabic) in the application
- Provide clear error messages if no suitable font is found

### 8.2 PDF Viewer Compatibility Risk
**Risk:** Some PDF viewers may not render Arabic text correctly even with proper fonts.

**Mitigation:**
- Embed fonts in PDF (already done with `BaseFont.EMBEDDED`)
- Test with multiple PDF viewers (Adobe Reader, Chrome, Firefox, mobile apps)
- Document known limitations

### 8.3 Test Data Conflicts Risk
**Risk:** Tests may fail if database already contains conflicting data.

**Mitigation:**
- Use unique test data identifiers
- Implement robust cleanup in `@AfterAll`
- Consider using in-memory database for tests

## 9. Success Metrics

### 9.1 Functional Metrics
- [ ] 100% of bill receipts generated in correct language
- [ ] 100% of Arabic text rendered correctly in PDFs
- [ ] 100% of dialog buttons localized correctly
- [ ] 100% of enum values localized in reports
- [ ] 100% test pass rate (all 3 failing tests now pass)

### 9.2 Quality Metrics
- [ ] No regression in existing PDF generation functionality
- [ ] No new warnings or errors in logs
- [ ] Code coverage maintained or improved

### 9.3 User Satisfaction Metrics
- [ ] Arabic-speaking users can generate readable receipts
- [ ] French-speaking users can generate readable receipts
- [ ] No customer complaints about PDF language issues

## 10. Acceptance Testing

### 10.1 Manual Testing Checklist
- [ ] Generate bill receipt with language set to English - verify English text
- [ ] Generate bill receipt with language set to Arabic - verify Arabic text and RTL layout
- [ ] Generate bill receipt with language set to French - verify French text
- [ ] Delete a part with language set to Arabic - verify dialog buttons are in Arabic
- [ ] Delete a category with language set to French - verify dialog buttons are in French
- [ ] Generate inventory report in Arabic - verify stock status is in Arabic
- [ ] Generate sales report in French - verify payment methods are in French
- [ ] Open generated PDFs in Adobe Reader - verify text renders correctly
- [ ] Open generated PDFs in Chrome - verify text renders correctly
- [ ] Open generated PDFs on mobile device - verify text renders correctly
- [ ] Run all tests in `BillServiceOptionalFieldsTest` - verify all pass

### 10.2 Automated Testing Requirements
- [ ] Unit tests for font selection logic
- [ ] Unit tests for locale-aware text formatting
- [ ] Unit tests for localized button type creation
- [ ] Unit tests for enum localization utility methods
- [ ] Integration tests for PDF generation in each language
- [ ] Integration tests for dialog localization
- [ ] All existing tests continue to pass

## 11. Documentation Requirements

### 11.1 Code Documentation
- [ ] Add JavaDoc comments to font selection methods
- [ ] Document font fallback logic
- [ ] Add comments explaining RTL handling

### 11.2 User Documentation
- [ ] Update user manual with language selection instructions
- [ ] Document system font requirements
- [ ] Add troubleshooting section for PDF rendering issues

## 12. Rollout Plan

### 12.1 Phase 1: Fix Test Failures (Priority: High)
- Fix `BillServiceOptionalFieldsTest` by ensuring valid test data
- Verify all tests pass

### 12.2 Phase 2: Fix Arabic Font Support (Priority: Critical)
- Implement proper Arabic font selection in `ReportService`
- Test Arabic PDF generation
- Verify text renders correctly

### 12.3 Phase 3: Localize Dialog Buttons (Priority: High)
- Update `AlertHelper` to use localized button types
- Add localization keys for button text
- Test all dialogs in all languages

### 12.4 Phase 4: Localize Enum Values (Priority: High)
- Add localization keys for all enum values
- Create utility methods for enum localization
- Update PDF exporters to use localized enum text
- Update Excel and CSV exporters

### 12.5 Phase 5: Verification and Testing (Priority: High)
- Test all three languages (English, French, Arabic)
- Test on multiple operating systems
- Test with multiple PDF viewers
- Verify all dialogs are localized

### 12.6 Phase 6: Documentation and Release (Priority: Medium)
- Update documentation
- Create release notes
- Deploy to production

## 13. Related Issues

- Issue from previous conversation: "the bill receipt pdf is in english, even if the language is set to arabic"
- Issue from previous conversation: "the bill in arabic titles are empty after generation"
- Issue from previous conversation: "pop ups that don't fully support language change, like the button ok/cancel in delete a part confirmation pop up"
- Issue from previous conversation: "the out of stock enum written directly in the arabic pdf"
- Issue from previous conversation: Test failures in `BillServiceOptionalFieldsTest`

## 14. References

- OpenPDF Documentation: https://github.com/LibrePDF/OpenPDF
- Unicode Arabic Support: https://unicode.org/charts/PDF/U0600.pdf
- Java Locale Documentation: https://docs.oracle.com/javase/8/docs/api/java/util/Locale.html
- JavaFX ButtonType Documentation: https://openjfx.io/javadoc/17/javafx.controls/javafx/scene/control/ButtonType.html
- Existing implementation in `PDFExporter.java` (reports module) - uses proper Arabic font support

## 15. Required Localization Keys

The following keys need to be verified/added to all message properties files:

### 15.1 Stock Status Keys
```properties
# English (messages_en_US.properties)
stock.status.normal=Normal
stock.status.low=Low Stock
stock.status.out=Out of Stock

# Arabic (messages_ar.properties)
stock.status.normal=عادي
stock.status.low=مخزون منخفض
stock.status.out=نفذت من المخزون

# French (messages_fr.properties)
stock.status.normal=Normal
stock.status.low=Stock faible
stock.status.out=Rupture de stock
```

### 15.2 Payment Method Keys
```properties
# English (messages_en_US.properties)
payment.method.cash=Cash
payment.method.card=Card
payment.method.check=Check

# Arabic (messages_ar.properties)
payment.method.cash=نقداً
payment.method.card=بطاقة
payment.method.check=شيك

# French (messages_fr.properties)
payment.method.cash=Espèces
payment.method.card=Carte
payment.method.check=Chèque
```

### 15.3 Search Interface Keys
```properties
# English (messages_en_US.properties)
search.label=Search
search.category.label=Category
search.parts.placeholder=Search parts by name, maker, category...

# Arabic (messages_ar.properties)
search.label=بحث
search.category.label=الفئة
search.parts.placeholder=البحث عن القطع بالاسم أو الصانع أو الفئة...

# French (messages_fr.properties)
search.label=Rechercher
search.category.label=Catégorie
search.parts.placeholder=Rechercher des pièces par nom, fabricant, catégorie...
```

### 15.4 Date Range Separator Key
```properties
# English (messages_en_US.properties)
date.range.separator= to 

# Arabic (messages_ar.properties)
date.range.separator= إلى 

# French (messages_fr.properties)
date.range.separator= à 
```

### 15.5 Common Button Keys (Already Exist)
These keys already exist in the properties files and should be used for dialog buttons:
- `common.ok`
- `common.cancel`
- `common.yes`
- `common.no`
