package org.chaos.office.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.chaos.office.model.PartImportData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExcelFileParser.
 * Tests Excel parsing correctness, header row detection, cell type handling, and error handling.
 * 
 * Requirements: 3.2, 3.3, 3.4, 3.6
 */
class ExcelFileParserTest {
    
    private ExcelFileParser parser;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        parser = new ExcelFileParser();
    }
    
    @Test
    void testParseValidExcelWithoutHeader() throws IOException {
        // Create a simple Excel file without header
        File excelFile = createExcelFile("test.xlsx", false,
            new String[]{"Brake Pad", "Bosch", "Front brake pad", "25.99", "10", "Brakes"},
            new String[]{"Oil Filter", "Mann", "Engine oil filter", "12.50", "20", "Filters"}
        );
        
        List<PartImportData> results = parser.parse(excelFile);
        
        assertEquals(2, results.size());
        
        // Verify first row
        PartImportData first = results.get(0);
        assertEquals(1, first.getRowNumber());
        assertEquals("Brake Pad", first.getName());
        assertEquals("Bosch", first.getMaker());
        assertEquals("Front brake pad", first.getDescription());
        assertEquals("25.99", first.getPriceStr());
        assertEquals("10", first.getQuantityStr());
        assertEquals("Brakes", first.getCategoryName());
        
        // Verify second row
        PartImportData second = results.get(1);
        assertEquals(2, second.getRowNumber());
        assertEquals("Oil Filter", second.getName());
        assertEquals("Mann", second.getMaker());
    }
    
    @Test
    void testParseValidExcelWithHeader() throws IOException {
        // Create an Excel file with header row
        File excelFile = createExcelFile("test_with_header.xlsx", true,
            new String[]{"Brake Pad", "Bosch", "Front brake pad", "25.99", "10", "Brakes"},
            new String[]{"Oil Filter", "Mann", "Engine oil filter", "12.50", "20", "Filters"}
        );
        
        List<PartImportData> results = parser.parse(excelFile);
        
        // Should skip header row
        assertEquals(2, results.size());
        
        // First data row should be row 2 (after header)
        PartImportData first = results.get(0);
        assertEquals(2, first.getRowNumber());
        assertEquals("Brake Pad", first.getName());
    }
    
    @Test
    void testParseWithHeaderCaseInsensitive() throws IOException {
        // Test that header detection is case-insensitive
        File excelFile = createExcelFileWithCustomHeader("test_header_case.xlsx",
            new String[]{"Name", "Maker", "Description", "Price", "Quantity", "Category"},
            new String[]{"Brake Pad", "Bosch", "Front brake pad", "25.99", "10", "Brakes"}
        );
        
        List<PartImportData> results = parser.parse(excelFile);
        
        // Should skip header row
        assertEquals(1, results.size());
        assertEquals(2, results.get(0).getRowNumber());
    }
    
    @Test
    void testParseWithNumericCells() throws IOException {
        // Create Excel file with numeric cells (not strings)
        File excelFile = createExcelFileWithNumericCells("test_numeric.xlsx");
        
        List<PartImportData> results = parser.parse(excelFile);
        
        assertEquals(1, results.size());
        
        // Verify numeric values are converted to strings
        PartImportData first = results.get(0);
        assertEquals("Brake Pad", first.getName());
        assertEquals("25.99", first.getPriceStr());
        assertEquals("10", first.getQuantityStr());
    }
    
    @Test
    void testParseWithEmptyFields() throws IOException {
        // Create Excel file with empty cells
        File excelFile = createExcelFile("test_empty_fields.xlsx", false,
            new String[]{"Brake Pad", "", "Front brake pad", "25.99", "", "Brakes"},
            new String[]{"", "Mann", "Engine oil filter", "12.50", "20", ""}
        );
        
        List<PartImportData> results = parser.parse(excelFile);
        
        assertEquals(2, results.size());
        
        // Verify empty fields are preserved as empty strings
        PartImportData first = results.get(0);
        assertEquals("", first.getMaker());
        assertEquals("", first.getQuantityStr());
        
        PartImportData second = results.get(1);
        assertEquals("", second.getName());
        assertEquals("", second.getCategoryName());
    }
    
    @Test
    void testParseSkipsMalformedRows() throws IOException {
        // Create Excel file with rows that have wrong number of columns
        File excelFile = createExcelFileWithMalformedRows("test_malformed.xlsx");
        
        List<PartImportData> results = parser.parse(excelFile);
        
        // Should only parse the valid rows (rows with exactly 6 columns)
        assertEquals(2, results.size());
        assertEquals("Brake Pad", results.get(0).getName());
        assertEquals("Air Filter", results.get(1).getName());
    }
    
    @Test
    void testParseEmptyFile() throws IOException {
        // Create an empty Excel file
        File excelFile = createEmptyExcelFile("test_empty.xlsx");
        
        List<PartImportData> results = parser.parse(excelFile);
        
        assertEquals(0, results.size());
    }
    
    @Test
    void testParseFileWithOnlyHeader() throws IOException {
        // Create Excel file with only header row
        File excelFile = createExcelFileWithCustomHeader("test_only_header.xlsx",
            new String[]{"name", "maker", "description", "price", "quantity", "category"}
        );
        
        List<PartImportData> results = parser.parse(excelFile);
        
        assertEquals(0, results.size());
    }
    
    @Test
    void testParseFileWithEmptyRows() throws IOException {
        // Create Excel file with empty rows (should be skipped)
        File excelFile = createExcelFileWithEmptyRows("test_empty_rows.xlsx");
        
        List<PartImportData> results = parser.parse(excelFile);
        
        // Should skip empty rows
        assertEquals(2, results.size());
        assertEquals("Brake Pad", results.get(0).getName());
        assertEquals("Oil Filter", results.get(1).getName());
    }
    
    @Test
    void testParseNonExistentFile() {
        // Test that IOException is thrown for non-existent file
        File nonExistentFile = new File(tempDir.toFile(), "non_existent.xlsx");
        
        assertThrows(IOException.class, () -> parser.parse(nonExistentFile));
    }
    
    @Test
    void testParseInvalidFile() throws IOException {
        // Create a non-Excel file with .xlsx extension
        File invalidFile = new File(tempDir.toFile(), "invalid.xlsx");
        try (FileOutputStream fos = new FileOutputStream(invalidFile)) {
            fos.write("This is not an Excel file".getBytes());
        }
        
        assertThrows(IOException.class, () -> parser.parse(invalidFile));
    }
    
    @Test
    void testParseWithMixedCellTypes() throws IOException {
        // Create Excel file with mixed cell types (string, numeric, boolean)
        File excelFile = createExcelFileWithMixedTypes("test_mixed_types.xlsx");
        
        List<PartImportData> results = parser.parse(excelFile);
        
        assertEquals(1, results.size());
        
        // Verify all cell types are converted to strings
        PartImportData first = results.get(0);
        assertEquals("Brake Pad", first.getName());
        assertEquals("Bosch", first.getMaker());
        assertEquals("Front brake pad", first.getDescription());
        assertEquals("25.99", first.getPriceStr());
        assertEquals("10", first.getQuantityStr());
        assertEquals("Brakes", first.getCategoryName());
    }
    
    @Test
    void testParseWithWholeNumbers() throws IOException {
        // Test that whole numbers don't have decimal points
        File excelFile = createExcelFileWithWholeNumbers("test_whole_numbers.xlsx");
        
        List<PartImportData> results = parser.parse(excelFile);
        
        assertEquals(1, results.size());
        
        // Verify whole numbers are formatted without decimal points
        PartImportData first = results.get(0);
        assertEquals("25", first.getPriceStr());
        assertEquals("10", first.getQuantityStr());
    }
    
    // Helper methods to create Excel files for testing
    
    private File createExcelFile(String filename, boolean withHeader, String[]... rows) throws IOException {
        File file = new File(tempDir.toFile(), filename);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Parts");
            
            int rowIndex = 0;
            
            // Add header if requested
            if (withHeader) {
                Row headerRow = sheet.createRow(rowIndex++);
                String[] headers = {"name", "maker", "description", "price", "quantity", "category"};
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                }
            }
            
            // Add data rows
            for (String[] rowData : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < rowData.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(rowData[i]);
                }
            }
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
        
        return file;
    }
    
    private File createExcelFileWithCustomHeader(String filename, String[] header, String[]... rows) throws IOException {
        File file = new File(tempDir.toFile(), filename);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Parts");
            
            int rowIndex = 0;
            
            // Add custom header
            Row headerRow = sheet.createRow(rowIndex++);
            for (int i = 0; i < header.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(header[i]);
            }
            
            // Add data rows
            for (String[] rowData : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < rowData.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(rowData[i]);
                }
            }
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
        
        return file;
    }
    
    private File createExcelFileWithNumericCells(String filename) throws IOException {
        File file = new File(tempDir.toFile(), filename);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Parts");
            
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("Brake Pad");
            row.createCell(1).setCellValue("Bosch");
            row.createCell(2).setCellValue("Front brake pad");
            row.createCell(3).setCellValue(25.99);  // Numeric cell
            row.createCell(4).setCellValue(10);     // Numeric cell
            row.createCell(5).setCellValue("Brakes");
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
        
        return file;
    }
    
    private File createExcelFileWithMalformedRows(String filename) throws IOException {
        File file = new File(tempDir.toFile(), filename);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Parts");
            
            // Valid row with 6 columns
            Row row1 = sheet.createRow(0);
            row1.createCell(0).setCellValue("Brake Pad");
            row1.createCell(1).setCellValue("Bosch");
            row1.createCell(2).setCellValue("Front brake pad");
            row1.createCell(3).setCellValue("25.99");
            row1.createCell(4).setCellValue("10");
            row1.createCell(5).setCellValue("Brakes");
            
            // Malformed row with only 3 columns
            Row row2 = sheet.createRow(1);
            row2.createCell(0).setCellValue("Oil Filter");
            row2.createCell(1).setCellValue("Mann");
            row2.createCell(2).setCellValue("Engine oil filter");
            
            // Valid row with 6 columns
            Row row3 = sheet.createRow(2);
            row3.createCell(0).setCellValue("Air Filter");
            row3.createCell(1).setCellValue("Mann");
            row3.createCell(2).setCellValue("Cabin air filter");
            row3.createCell(3).setCellValue("8.99");
            row3.createCell(4).setCellValue("15");
            row3.createCell(5).setCellValue("Filters");
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
        
        return file;
    }
    
    private File createEmptyExcelFile(String filename) throws IOException {
        File file = new File(tempDir.toFile(), filename);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("Parts");
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
        
        return file;
    }
    
    private File createExcelFileWithEmptyRows(String filename) throws IOException {
        File file = new File(tempDir.toFile(), filename);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Parts");
            
            // Valid row
            Row row1 = sheet.createRow(0);
            row1.createCell(0).setCellValue("Brake Pad");
            row1.createCell(1).setCellValue("Bosch");
            row1.createCell(2).setCellValue("Front brake pad");
            row1.createCell(3).setCellValue("25.99");
            row1.createCell(4).setCellValue("10");
            row1.createCell(5).setCellValue("Brakes");
            
            // Empty row (row 1)
            sheet.createRow(1);
            
            // Valid row
            Row row3 = sheet.createRow(2);
            row3.createCell(0).setCellValue("Oil Filter");
            row3.createCell(1).setCellValue("Mann");
            row3.createCell(2).setCellValue("Engine oil filter");
            row3.createCell(3).setCellValue("12.50");
            row3.createCell(4).setCellValue("20");
            row3.createCell(5).setCellValue("Filters");
            
            // Empty row (row 3)
            sheet.createRow(3);
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
        
        return file;
    }
    
    private File createExcelFileWithMixedTypes(String filename) throws IOException {
        File file = new File(tempDir.toFile(), filename);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Parts");
            
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("Brake Pad");      // String
            row.createCell(1).setCellValue("Bosch");          // String
            row.createCell(2).setCellValue("Front brake pad"); // String
            row.createCell(3).setCellValue(25.99);            // Numeric
            row.createCell(4).setCellValue(10);               // Numeric
            row.createCell(5).setCellValue("Brakes");         // String
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
        
        return file;
    }
    
    private File createExcelFileWithWholeNumbers(String filename) throws IOException {
        File file = new File(tempDir.toFile(), filename);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Parts");
            
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("Brake Pad");
            row.createCell(1).setCellValue("Bosch");
            row.createCell(2).setCellValue("Front brake pad");
            row.createCell(3).setCellValue(25.0);  // Whole number as double
            row.createCell(4).setCellValue(10.0);  // Whole number as double
            row.createCell(5).setCellValue("Brakes");
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
        
        return file;
    }
}
