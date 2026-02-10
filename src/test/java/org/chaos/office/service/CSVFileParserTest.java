package org.chaos.office.service;

import org.chaos.office.model.PartImportData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CSVFileParser.
 * Tests CSV parsing correctness, header row detection, quote handling, and error handling.
 * 
 * Requirements: 3.1, 3.3, 3.4, 3.6
 */
class CSVFileParserTest {
    
    private CSVFileParser parser;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        parser = new CSVFileParser();
    }
    
    @Test
    void testParseValidCsvWithoutHeader() throws IOException {
        // Create a simple CSV file without header
        File csvFile = createCsvFile("test.csv",
            "Brake Pad,Bosch,Front brake pad,25.99,10,Brakes\n" +
            "Oil Filter,Mann,Engine oil filter,12.50,20,Filters\n"
        );
        
        List<PartImportData> results = parser.parse(csvFile);
        
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
    void testParseValidCsvWithHeader() throws IOException {
        // Create a CSV file with header row
        File csvFile = createCsvFile("test_with_header.csv",
            "name,maker,description,price,quantity,category\n" +
            "Brake Pad,Bosch,Front brake pad,25.99,10,Brakes\n" +
            "Oil Filter,Mann,Engine oil filter,12.50,20,Filters\n"
        );
        
        List<PartImportData> results = parser.parse(csvFile);
        
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
        File csvFile = createCsvFile("test_header_case.csv",
            "Name,Maker,Description,Price,Quantity,Category\n" +
            "Brake Pad,Bosch,Front brake pad,25.99,10,Brakes\n"
        );
        
        List<PartImportData> results = parser.parse(csvFile);
        
        // Should skip header row
        assertEquals(1, results.size());
        assertEquals(2, results.get(0).getRowNumber());
    }
    
    @Test
    void testParseWithQuotedFields() throws IOException {
        // Test CSV with quoted fields containing commas
        File csvFile = createCsvFile("test_quoted.csv",
            "\"Brake Pad, Premium\",Bosch,\"High-quality, durable brake pad\",25.99,10,Brakes\n" +
            "Oil Filter,Mann,\"Standard filter, fits most engines\",12.50,20,Filters\n"
        );
        
        List<PartImportData> results = parser.parse(csvFile);
        
        assertEquals(2, results.size());
        
        // Verify quoted fields are parsed correctly
        PartImportData first = results.get(0);
        assertEquals("Brake Pad, Premium", first.getName());
        assertEquals("High-quality, durable brake pad", first.getDescription());
        
        PartImportData second = results.get(1);
        assertEquals("Standard filter, fits most engines", second.getDescription());
    }
    
    @Test
    void testParseWithEscapedQuotes() throws IOException {
        // Test CSV with escaped quotes (double quotes)
        File csvFile = createCsvFile("test_escaped_quotes.csv",
            "\"Brake Pad \"\"Premium\"\"\",Bosch,\"Description with \"\"quotes\"\"\",25.99,10,Brakes\n"
        );
        
        List<PartImportData> results = parser.parse(csvFile);
        
        assertEquals(1, results.size());
        
        // Verify escaped quotes are handled correctly
        PartImportData first = results.get(0);
        assertEquals("Brake Pad \"Premium\"", first.getName());
        assertEquals("Description with \"quotes\"", first.getDescription());
    }
    
    @Test
    void testParseMixedQuotedAndUnquoted() throws IOException {
        // Test CSV with mix of quoted and unquoted fields
        File csvFile = createCsvFile("test_mixed.csv",
            "Brake Pad,\"Bosch, Germany\",Front brake pad,25.99,10,Brakes\n" +
            "\"Oil Filter\",Mann,Engine oil filter,\"12.50\",20,\"Filters\"\n"
        );
        
        List<PartImportData> results = parser.parse(csvFile);
        
        assertEquals(2, results.size());
        
        PartImportData first = results.get(0);
        assertEquals("Brake Pad", first.getName());
        assertEquals("Bosch, Germany", first.getMaker());
        
        PartImportData second = results.get(1);
        assertEquals("Oil Filter", second.getName());
        assertEquals("12.50", second.getPriceStr());
        assertEquals("Filters", second.getCategoryName());
    }
    
    @Test
    void testParseWithEmptyFields() throws IOException {
        // Test CSV with empty fields
        File csvFile = createCsvFile("test_empty_fields.csv",
            "Brake Pad,,Front brake pad,25.99,,Brakes\n" +
            ",Mann,Engine oil filter,12.50,20,\n"
        );
        
        List<PartImportData> results = parser.parse(csvFile);
        
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
        // Test that rows with wrong number of columns are skipped
        File csvFile = createCsvFile("test_malformed.csv",
            "Brake Pad,Bosch,Front brake pad,25.99,10,Brakes\n" +
            "Oil Filter,Mann,Engine oil filter\n" +  // Only 3 columns - should be skipped
            "Air Filter,Mann,Cabin air filter,8.99,15,Filters\n" +
            "Spark Plug,NGK\n" +  // Only 2 columns - should be skipped
            "Wiper Blade,Bosch,Front wiper blade,15.00,5,Wipers\n"
        );
        
        List<PartImportData> results = parser.parse(csvFile);
        
        // Should only parse the 3 valid rows
        assertEquals(3, results.size());
        assertEquals("Brake Pad", results.get(0).getName());
        assertEquals("Air Filter", results.get(1).getName());
        assertEquals("Wiper Blade", results.get(2).getName());
    }
    
    @Test
    void testParseEmptyFile() throws IOException {
        // Test parsing an empty file
        File csvFile = createCsvFile("test_empty.csv", "");
        
        List<PartImportData> results = parser.parse(csvFile);
        
        assertEquals(0, results.size());
    }
    
    @Test
    void testParseFileWithOnlyHeader() throws IOException {
        // Test file with only header row
        File csvFile = createCsvFile("test_only_header.csv",
            "name,maker,description,price,quantity,category\n"
        );
        
        List<PartImportData> results = parser.parse(csvFile);
        
        assertEquals(0, results.size());
    }
    
    @Test
    void testParseFileWithEmptyLines() throws IOException {
        // Test file with empty lines (should be skipped)
        File csvFile = createCsvFile("test_empty_lines.csv",
            "Brake Pad,Bosch,Front brake pad,25.99,10,Brakes\n" +
            "\n" +
            "Oil Filter,Mann,Engine oil filter,12.50,20,Filters\n" +
            "\n" +
            "\n" +
            "Air Filter,Mann,Cabin air filter,8.99,15,Filters\n"
        );
        
        List<PartImportData> results = parser.parse(csvFile);
        
        assertEquals(3, results.size());
        assertEquals("Brake Pad", results.get(0).getName());
        assertEquals("Oil Filter", results.get(1).getName());
        assertEquals("Air Filter", results.get(2).getName());
    }
    
    @Test
    void testParseFileWithWhitespace() throws IOException {
        // Test that whitespace is preserved (not trimmed)
        File csvFile = createCsvFile("test_whitespace.csv",
            " Brake Pad ,Bosch, Front brake pad ,25.99,10,Brakes\n"
        );
        
        List<PartImportData> results = parser.parse(csvFile);
        
        assertEquals(1, results.size());
        
        // Whitespace should be preserved
        PartImportData first = results.get(0);
        assertEquals(" Brake Pad ", first.getName());
        assertEquals(" Front brake pad ", first.getDescription());
    }
    
    @Test
    void testParseNonExistentFile() {
        // Test that IOException is thrown for non-existent file
        File nonExistentFile = new File(tempDir.toFile(), "non_existent.csv");
        
        assertThrows(IOException.class, () -> parser.parse(nonExistentFile));
    }
    
    @Test
    void testParseWithAllFieldTypes() throws IOException {
        // Test with various data types in string format
        File csvFile = createCsvFile("test_field_types.csv",
            "Part Name,Maker Name,Description text,99.99,100,Category Name\n" +
            "Another Part,Another Maker,Another description,0.01,0,Another Category\n" +
            "Third Part,Third Maker,Third description,1000.50,999999,Third Category\n"
        );
        
        List<PartImportData> results = parser.parse(csvFile);
        
        assertEquals(3, results.size());
        
        // Verify all fields are captured as strings
        PartImportData first = results.get(0);
        assertEquals("99.99", first.getPriceStr());
        assertEquals("100", first.getQuantityStr());
        
        PartImportData second = results.get(1);
        assertEquals("0.01", second.getPriceStr());
        assertEquals("0", second.getQuantityStr());
        
        PartImportData third = results.get(2);
        assertEquals("1000.50", third.getPriceStr());
        assertEquals("999999", third.getQuantityStr());
    }
    
    // Helper method to create CSV files for testing
    private File createCsvFile(String filename, String content) throws IOException {
        File file = new File(tempDir.toFile(), filename);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
        return file;
    }
}
