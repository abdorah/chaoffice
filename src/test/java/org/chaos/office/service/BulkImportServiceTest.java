package org.chaos.office.service;

import org.chaos.office.model.Category;
import org.chaos.office.model.ImportError;
import org.chaos.office.model.ImportResult;
import org.chaos.office.model.Part;
import org.chaos.office.util.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BulkImportService.
 * Tests file type validation, parsing, validation, error handling, and fail-safe processing.
 * 
 * Requirements: 2.4, 3.1, 3.2, 4.1-4.8, 5.1-5.5, 7.1-7.5
 */
class BulkImportServiceTest {
    
    @TempDir
    Path tempDir;
    
    private BulkImportService bulkImportService;
    private PartService partService;
    private CategoryService categoryService;
    private CSVFileParser csvFileParser;
    private ExcelFileParser excelFileParser;
    private ImportValidator importValidator;
    private Category testCategory;
    
    @BeforeEach
    void setUp() throws Exception {
        // Initialize services
        partService = new PartService();
        categoryService = new CategoryService();
        csvFileParser = new CSVFileParser();
        excelFileParser = new ExcelFileParser();
        importValidator = new ImportValidator();
        
        bulkImportService = new BulkImportService(
            partService,
            categoryService,
            csvFileParser,
            excelFileParser,
            importValidator
        );
        
        // Initialize database
        DatabaseConnection.getInstance().initializeDatabase();
        
        // Clean up any existing test data
        try {
            cleanupTestData();
        } catch (SQLException e) {
            // Ignore cleanup errors on first run
        }
        
        // Create test category
        testCategory = createTestCategory("Electronics", "Electronic parts");
    }
    
    @AfterEach
    void tearDown() {
        // Clean up test data
        try {
            cleanupTestData();
        } catch (SQLException e) {
            // Ignore cleanup errors
        }
    }
    
    /**
     * Test: Unsupported file type should return error
     * Requirement: 2.4
     */
    @Test
    void testUnsupportedFileType() throws IOException {
        File txtFile = tempDir.resolve("test.txt").toFile();
        try (FileWriter writer = new FileWriter(txtFile)) {
            writer.write("some content");
        }
        
        ImportResult result = bulkImportService.importFromFile(txtFile);
        
        assertNotNull(result);
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.hasErrors());
        assertEquals(1, result.getErrors().size());
        
        ImportError error = result.getErrors().get(0);
        assertEquals(ImportError.ErrorType.PARSING, error.getErrorType());
        assertTrue(error.getErrorMessage().contains("Unsupported file type"));
    }
    
    /**
     * Test: Non-existent file should return error
     * Requirement: 7.1
     */
    @Test
    void testNonExistentFile() {
        File nonExistentFile = new File("nonexistent.csv");
        
        ImportResult result = bulkImportService.importFromFile(nonExistentFile);
        
        assertNotNull(result);
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.hasErrors());
        assertEquals(ImportError.ErrorType.PARSING, result.getErrors().get(0).getErrorType());
    }
    
    /**
     * Test: Null file should return error
     * Requirement: 7.1
     */
    @Test
    void testNullFile() {
        ImportResult result = bulkImportService.importFromFile(null);
        
        assertNotNull(result);
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.hasErrors());
    }
    
    /**
     * Test: Valid CSV file with all valid parts should import successfully
     * Requirements: 3.1, 5.1, 5.2
     */
    @Test
    void testImportValidCsvFile() throws IOException {
        File csvFile = createValidCsvFile();
        
        ImportResult result = bulkImportService.importFromFile(csvFile);
        
        assertNotNull(result);
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertFalse(result.hasErrors());
        
        // Verify parts were actually saved to database
        List<Part> parts = partService.getAllParts();
        assertTrue(parts.stream().anyMatch(p -> "Test Part 1".equals(p.getName())));
        assertTrue(parts.stream().anyMatch(p -> "Test Part 2".equals(p.getName())));
    }
    
    /**
     * Test: Valid Excel file with all valid parts should import successfully
     * Requirements: 3.2, 5.1, 5.2
     */
    @Test
    void testImportValidExcelFile() throws IOException {
        File excelFile = createValidExcelFile();
        
        ImportResult result = bulkImportService.importFromFile(excelFile);
        
        assertNotNull(result);
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertFalse(result.hasErrors());
        
        // Verify parts were actually saved to database
        List<Part> parts = partService.getAllParts();
        assertTrue(parts.stream().anyMatch(p -> "Excel Part 1".equals(p.getName())));
        assertTrue(parts.stream().anyMatch(p -> "Excel Part 2".equals(p.getName())));
    }
    
    /**
     * Test: CSV file with header row should skip header
     * Requirement: 3.4
     */
    @Test
    void testImportCsvWithHeader() throws IOException {
        File csvFile = createCsvFileWithHeader();
        
        ImportResult result = bulkImportService.importFromFile(csvFile);
        
        assertNotNull(result);
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        
        // Verify header was not imported as a part
        List<Part> parts = partService.getAllParts();
        assertFalse(parts.stream().anyMatch(p -> "name".equalsIgnoreCase(p.getName())));
    }
    
    /**
     * Test: File with validation errors should record errors and continue processing
     * Requirements: 4.1-4.8, 5.3, 5.4
     */
    @Test
    void testImportWithValidationErrors() throws IOException {
        File csvFile = createCsvFileWithValidationErrors();
        
        ImportResult result = bulkImportService.importFromFile(csvFile);
        
        assertNotNull(result);
        assertEquals(1, result.getSuccessCount()); // One valid part
        assertTrue(result.getFailureCount() > 0); // At least one invalid part
        assertTrue(result.hasErrors());
        
        // Verify validation errors are recorded
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.getErrorType() == ImportError.ErrorType.VALIDATION));
    }
    
    /**
     * Test: Empty name should fail validation
     * Requirement: 4.1
     */
    @Test
    void testImportWithEmptyName() throws IOException {
        File csvFile = tempDir.resolve("empty_name.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write(",TestMaker,Description,10.0,5,Electronics\n");
        }
        
        ImportResult result = bulkImportService.importFromFile(csvFile);
        
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.hasErrors());
        
        ImportError error = result.getErrors().get(0);
        assertEquals(ImportError.ErrorType.VALIDATION, error.getErrorType());
        assertTrue(error.getErrorMessage().contains("name"));
    }
    
    /**
     * Test: Empty maker should fail validation
     * Requirement: 4.2
     */
    @Test
    void testImportWithEmptyMaker() throws IOException {
        File csvFile = tempDir.resolve("empty_maker.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("TestPart,,Description,10.0,5,Electronics\n");
        }
        
        ImportResult result = bulkImportService.importFromFile(csvFile);
        
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.hasErrors());
        
        ImportError error = result.getErrors().get(0);
        assertEquals(ImportError.ErrorType.VALIDATION, error.getErrorType());
        assertTrue(error.getErrorMessage().contains("maker"));
    }
    
    /**
     * Test: Invalid price should fail validation
     * Requirement: 4.4
     */
    @Test
    void testImportWithInvalidPrice() throws IOException {
        File csvFile = tempDir.resolve("invalid_price.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("TestPart,TestMaker,Description,invalid,5,Electronics\n");
        }
        
        ImportResult result = bulkImportService.importFromFile(csvFile);
        
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.hasErrors());
        
        ImportError error = result.getErrors().get(0);
        assertEquals(ImportError.ErrorType.VALIDATION, error.getErrorType());
        assertTrue(error.getErrorMessage().contains("price"));
    }
    
    /**
     * Test: Negative price should fail validation
     * Requirement: 4.4
     */
    @Test
    void testImportWithNegativePrice() throws IOException {
        File csvFile = tempDir.resolve("negative_price.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("TestPart,TestMaker,Description,-10.0,5,Electronics\n");
        }
        
        ImportResult result = bulkImportService.importFromFile(csvFile);
        
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.hasErrors());
        
        ImportError error = result.getErrors().get(0);
        assertEquals(ImportError.ErrorType.VALIDATION, error.getErrorType());
        assertTrue(error.getErrorMessage().contains("price"));
    }
    
    /**
     * Test: Invalid quantity should fail validation
     * Requirement: 4.5
     */
    @Test
    void testImportWithInvalidQuantity() throws IOException {
        File csvFile = tempDir.resolve("invalid_quantity.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("TestPart,TestMaker,Description,10.0,invalid,Electronics\n");
        }
        
        ImportResult result = bulkImportService.importFromFile(csvFile);
        
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.hasErrors());
        
        ImportError error = result.getErrors().get(0);
        assertEquals(ImportError.ErrorType.VALIDATION, error.getErrorType());
        assertTrue(error.getErrorMessage().contains("quantity"));
    }
    
    /**
     * Test: Non-existent category should fail validation
     * Requirement: 4.6, 4.8
     */
    @Test
    void testImportWithNonExistentCategory() throws IOException {
        File csvFile = tempDir.resolve("invalid_category.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("TestPart,TestMaker,Description,10.0,5,NonExistentCategory\n");
        }
        
        ImportResult result = bulkImportService.importFromFile(csvFile);
        
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.hasErrors());
        
        ImportError error = result.getErrors().get(0);
        assertEquals(ImportError.ErrorType.VALIDATION, error.getErrorType());
        assertTrue(error.getErrorMessage().contains("category"));
    }
    
    /**
     * Test: Mixed valid and invalid parts should process all and report correctly
     * Requirements: 5.3, 5.4, 5.5
     */
    @Test
    void testFailSafeProcessing() throws IOException {
        File csvFile = tempDir.resolve("mixed_valid_invalid.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Valid Part 1,Maker1,Description1,10.0,5,Electronics\n");
            writer.write(",Maker2,Description2,20.0,10,Electronics\n"); // Empty name
            writer.write("Valid Part 2,Maker3,Description3,30.0,15,Electronics\n");
            writer.write("Invalid Part,Maker4,Description4,-5.0,20,Electronics\n"); // Negative price
            writer.write("Valid Part 3,Maker5,Description5,40.0,25,Electronics\n");
        }
        
        ImportResult result = bulkImportService.importFromFile(csvFile);
        
        assertNotNull(result);
        assertEquals(3, result.getSuccessCount()); // 3 valid parts
        assertEquals(2, result.getFailureCount()); // 2 invalid parts
        assertEquals(5, result.getSuccessCount() + result.getFailureCount()); // Total rows processed
        assertTrue(result.hasErrors());
        assertEquals(2, result.getErrors().size());
    }
    
    /**
     * Test: Import result should have accurate counts
     * Requirement: 5.5
     */
    @Test
    void testImportResultAccuracy() throws IOException {
        File csvFile = tempDir.resolve("accuracy_test.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Part1,Maker1,Desc1,10.0,5,Electronics\n");
            writer.write("Part2,Maker2,Desc2,20.0,10,Electronics\n");
            writer.write("Part3,Maker3,Desc3,30.0,15,Electronics\n");
        }
        
        ImportResult result = bulkImportService.importFromFile(csvFile);
        
        assertEquals(3, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(3, result.getSuccessCount() + result.getFailureCount());
    }
    
    /**
     * Test: Error messages should include row numbers
     * Requirement: 4.7, 7.3
     */
    @Test
    void testErrorMessagesIncludeRowNumbers() throws IOException {
        File csvFile = tempDir.resolve("row_numbers.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Valid Part,Maker,Desc,10.0,5,Electronics\n"); // Row 1
            writer.write(",Maker,Desc,10.0,5,Electronics\n"); // Row 2 - error
            writer.write("Another Valid,Maker,Desc,10.0,5,Electronics\n"); // Row 3
        }
        
        ImportResult result = bulkImportService.importFromFile(csvFile);
        
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.hasErrors());
        
        ImportError error = result.getErrors().get(0);
        assertEquals(2, error.getRowNumber());
    }
    
    /**
     * Test: getSummary should return formatted summary
     * Requirement: 5.5
     */
    @Test
    void testGetSummary() throws IOException {
        File csvFile = tempDir.resolve("summary_test.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Valid Part,Maker,Desc,10.0,5,Electronics\n");
            writer.write(",Maker,Desc,10.0,5,Electronics\n"); // Invalid
        }
        
        ImportResult result = bulkImportService.importFromFile(csvFile);
        
        String summary = result.getSummary();
        assertNotNull(summary);
        assertTrue(summary.contains("Successfully imported: 1"));
        assertTrue(summary.contains("Failed to import: 1"));
        assertTrue(summary.contains("Errors:"));
    }
    
    /**
     * Test: CSV file extension should be recognized (case-insensitive)
     * Requirement: 2.4
     */
    @Test
    void testCsvFileExtensionCaseInsensitive() throws IOException {
        File csvFile = tempDir.resolve("test.CSV").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("TestPart,TestMaker,Description,10.0,5,Electronics\n");
        }
        
        ImportResult result = bulkImportService.importFromFile(csvFile);
        
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
    }
    
    /**
     * Test: Excel file extensions should be recognized (.xlsx and .xls)
     * Requirement: 2.4
     */
    @Test
    void testExcelFileExtensions() throws IOException {
        // Test .xlsx
        File xlsxFile = createValidExcelFile();
        ImportResult xlsxResult = bulkImportService.importFromFile(xlsxFile);
        assertTrue(xlsxResult.getSuccessCount() > 0);
        
        // Note: .xls files require the same Apache POI library, so they should work the same way
    }
    
    // Helper methods
    
    private File createValidCsvFile() throws IOException {
        File csvFile = tempDir.resolve("valid.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Test Part 1,Test Maker 1,Test Description 1,10.50,5,Electronics\n");
            writer.write("Test Part 2,Test Maker 2,Test Description 2,20.75,10,Electronics\n");
        }
        return csvFile;
    }
    
    private File createCsvFileWithHeader() throws IOException {
        File csvFile = tempDir.resolve("with_header.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("name,maker,description,price,quantity,category\n");
            writer.write("Header Test 1,Maker1,Desc1,10.0,5,Electronics\n");
            writer.write("Header Test 2,Maker2,Desc2,20.0,10,Electronics\n");
        }
        return csvFile;
    }
    
    private File createCsvFileWithValidationErrors() throws IOException {
        File csvFile = tempDir.resolve("validation_errors.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Valid Part,ValidMaker,ValidDesc,10.0,5,Electronics\n");
            writer.write(",EmptyName,Desc,10.0,5,Electronics\n"); // Empty name
            writer.write("InvalidPrice,Maker,Desc,abc,5,Electronics\n"); // Invalid price
        }
        return csvFile;
    }
    
    private File createValidExcelFile() throws IOException {
        // Create a simple Excel file using Apache POI
        File excelFile = tempDir.resolve("valid.xlsx").toFile();
        
        org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Parts");
        
        // Create data rows
        org.apache.poi.ss.usermodel.Row row1 = sheet.createRow(0);
        row1.createCell(0).setCellValue("Excel Part 1");
        row1.createCell(1).setCellValue("Excel Maker 1");
        row1.createCell(2).setCellValue("Excel Description 1");
        row1.createCell(3).setCellValue(15.50);
        row1.createCell(4).setCellValue(8);
        row1.createCell(5).setCellValue("Electronics");
        
        org.apache.poi.ss.usermodel.Row row2 = sheet.createRow(1);
        row2.createCell(0).setCellValue("Excel Part 2");
        row2.createCell(1).setCellValue("Excel Maker 2");
        row2.createCell(2).setCellValue("Excel Description 2");
        row2.createCell(3).setCellValue(25.75);
        row2.createCell(4).setCellValue(12);
        row2.createCell(5).setCellValue("Electronics");
        
        // Write to file
        try (java.io.FileOutputStream fileOut = new java.io.FileOutputStream(excelFile)) {
            workbook.write(fileOut);
        }
        workbook.close();
        
        return excelFile;
    }
    
    private Category createTestCategory(String name, String description) throws SQLException {
        // First check if category already exists
        String checkSql = "SELECT id FROM categories WHERE name = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, name);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    Category category = new Category();
                    category.setId(rs.getInt("id"));
                    category.setName(name);
                    category.setDescription(description);
                    return category;
                }
            }
        }
        
        // If not exists, create it
        String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.executeUpdate();
            
            // Get the last inserted ID
            try (PreparedStatement idStmt = conn.prepareStatement("SELECT last_insert_rowid()");
                 ResultSet rs = idStmt.executeQuery()) {
                if (rs.next()) {
                    Category category = new Category();
                    category.setId(rs.getInt(1));
                    category.setName(name);
                    category.setDescription(description);
                    return category;
                }
            }
        }
        
        throw new SQLException("Failed to create test category");
    }
    
    private void cleanupTestData() throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            // Delete test parts
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM parts WHERE name LIKE 'Test%' OR name LIKE 'Valid%' OR name LIKE 'Invalid%' OR name LIKE 'Excel%' OR name LIKE 'Header%' OR name LIKE 'Part%' OR name LIKE 'Another%'")) {
                stmt.executeUpdate();
            }
            
            // Delete test makers
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM makers WHERE name LIKE 'Test%' OR name LIKE 'Maker%' OR name LIKE 'Excel%' OR name LIKE 'Valid%' OR name LIKE 'Empty%'")) {
                stmt.executeUpdate();
            }
        }
    }
}
