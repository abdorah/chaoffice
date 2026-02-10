package org.chaos.office.service;

import org.chaos.office.model.Category;
import org.chaos.office.model.PartImportData;
import org.chaos.office.model.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ImportValidator.
 * Tests validation rules for name, maker, description, price, quantity, and category.
 * 
 * Requirements: 4.1-4.8
 */
class ImportValidatorTest {
    
    private ImportValidator validator;
    private Map<String, Category> categoryMap;
    
    @BeforeEach
    void setUp() {
        validator = new ImportValidator();
        
        // Create a map of valid categories for testing
        categoryMap = new HashMap<>();
        categoryMap.put("Brakes", new Category(1, "Brakes", "Brake components", null));
        categoryMap.put("Filters", new Category(2, "Filters", "Filter components", null));
        categoryMap.put("Engine", new Category(3, "Engine", "Engine parts", null));
    }
    
    // ========== Valid Data Tests ==========
    
    @Test
    void testValidateValidData() {
        // Test with completely valid data
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", "Front brake pad", "25.99", "10", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }
    
    @Test
    void testValidateWithZeroPrice() {
        // Test that zero price is valid (non-negative)
        PartImportData data = new PartImportData(
            1, "Free Sample", "Generic", "Free sample part", "0", "5", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }
    
    @Test
    void testValidateWithZeroQuantity() {
        // Test that zero quantity is valid (non-negative)
        PartImportData data = new PartImportData(
            1, "Out of Stock", "Generic", "Currently unavailable", "10.00", "0", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }
    
    @Test
    void testValidateWithLargeNumbers() {
        // Test with large valid numbers
        PartImportData data = new PartImportData(
            1, "Expensive Part", "Premium", "High-end component", "9999.99", "999999", "Engine"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }
    
    @Test
    void testValidateWithDecimalPrice() {
        // Test various decimal price formats
        PartImportData data1 = new PartImportData(
            1, "Part 1", "Maker", "Description", "10.5", "5", "Brakes"
        );
        PartImportData data2 = new PartImportData(
            2, "Part 2", "Maker", "Description", "10.50", "5", "Brakes"
        );
        PartImportData data3 = new PartImportData(
            3, "Part 3", "Maker", "Description", "10", "5", "Brakes"
        );
        
        assertTrue(validator.validate(data1, categoryMap).isValid());
        assertTrue(validator.validate(data2, categoryMap).isValid());
        assertTrue(validator.validate(data3, categoryMap).isValid());
    }
    
    // ========== Name Validation Tests (Requirement 4.1) ==========
    
    @Test
    void testValidateEmptyName() {
        PartImportData data = new PartImportData(
            1, "", "Bosch", "Front brake pad", "25.99", "10", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("name") && e.contains("empty")));
    }
    
    @Test
    void testValidateNullName() {
        PartImportData data = new PartImportData(
            1, null, "Bosch", "Front brake pad", "25.99", "10", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("name") && e.contains("empty")));
    }
    
    @Test
    void testValidateWhitespaceName() {
        PartImportData data = new PartImportData(
            1, "   ", "Bosch", "Front brake pad", "25.99", "10", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("name") && e.contains("whitespace")));
    }
    
    @Test
    void testValidateNameWithLeadingTrailingWhitespace() {
        // Name with leading/trailing whitespace but non-empty content should be valid
        PartImportData data = new PartImportData(
            1, "  Brake Pad  ", "Bosch", "Front brake pad", "25.99", "10", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertTrue(result.isValid());
    }
    
    // ========== Maker Validation Tests (Requirement 4.2) ==========
    
    @Test
    void testValidateEmptyMaker() {
        PartImportData data = new PartImportData(
            1, "Brake Pad", "", "Front brake pad", "25.99", "10", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("maker") && e.contains("empty")));
    }
    
    @Test
    void testValidateNullMaker() {
        PartImportData data = new PartImportData(
            1, "Brake Pad", null, "Front brake pad", "25.99", "10", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("maker") && e.contains("empty")));
    }
    
    @Test
    void testValidateWhitespaceMaker() {
        PartImportData data = new PartImportData(
            1, "Brake Pad", "   ", "Front brake pad", "25.99", "10", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("maker") && e.contains("whitespace")));
    }
    
    // ========== Description Validation Tests (Requirement 4.3) ==========
    
    @Test
    void testValidateEmptyDescription() {
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", "", "25.99", "10", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("description") && e.contains("empty")));
    }
    
    @Test
    void testValidateNullDescription() {
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", null, "25.99", "10", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("description") && e.contains("empty")));
    }
    
    @Test
    void testValidateWhitespaceDescription() {
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", "   ", "25.99", "10", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("description") && e.contains("whitespace")));
    }
    
    // ========== Price Validation Tests (Requirement 4.4) ==========
    
    @Test
    void testValidateNegativePrice() {
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", "Front brake pad", "-25.99", "10", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("price") && e.contains("negative")));
    }
    
    @Test
    void testValidateInvalidPriceFormat() {
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", "Front brake pad", "abc", "10", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("price") && e.contains("parsed")));
    }
    
    @Test
    void testValidateEmptyPrice() {
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", "Front brake pad", "", "10", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("price") && e.contains("empty")));
    }
    
    @Test
    void testValidateNullPrice() {
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", "Front brake pad", null, "10", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("price") && e.contains("empty")));
    }
    
    @Test
    void testValidateWhitespacePrice() {
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", "Front brake pad", "   ", "10", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("price")));
    }
    
    @Test
    void testValidatePriceWithWhitespace() {
        // Price with leading/trailing whitespace should be valid (trimmed before parsing)
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", "Front brake pad", "  25.99  ", "10", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertTrue(result.isValid());
    }
    
    @Test
    void testValidatePriceWithInvalidCharacters() {
        PartImportData data1 = new PartImportData(
            1, "Part", "Maker", "Desc", "$25.99", "10", "Brakes"
        );
        PartImportData data2 = new PartImportData(
            2, "Part", "Maker", "Desc", "25.99$", "10", "Brakes"
        );
        PartImportData data3 = new PartImportData(
            3, "Part", "Maker", "Desc", "25,99", "10", "Brakes"
        );
        
        assertFalse(validator.validate(data1, categoryMap).isValid());
        assertFalse(validator.validate(data2, categoryMap).isValid());
        assertFalse(validator.validate(data3, categoryMap).isValid());
    }
    
    // ========== Quantity Validation Tests (Requirement 4.5) ==========
    
    @Test
    void testValidateNegativeQuantity() {
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", "Front brake pad", "25.99", "-10", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("quantity") && e.contains("negative")));
    }
    
    @Test
    void testValidateInvalidQuantityFormat() {
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", "Front brake pad", "25.99", "abc", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("quantity") && e.contains("integer")));
    }
    
    @Test
    void testValidateDecimalQuantity() {
        // Quantity must be an integer, not a decimal
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", "Front brake pad", "25.99", "10.5", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("quantity") && e.contains("integer")));
    }
    
    @Test
    void testValidateEmptyQuantity() {
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", "Front brake pad", "25.99", "", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("quantity") && e.contains("empty")));
    }
    
    @Test
    void testValidateNullQuantity() {
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", "Front brake pad", "25.99", null, "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("quantity") && e.contains("empty")));
    }
    
    @Test
    void testValidateQuantityWithWhitespace() {
        // Quantity with leading/trailing whitespace should be valid (trimmed before parsing)
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", "Front brake pad", "25.99", "  10  ", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertTrue(result.isValid());
    }
    
    // ========== Category Validation Tests (Requirements 4.6, 4.8) ==========
    
    @Test
    void testValidateNonExistentCategory() {
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", "Front brake pad", "25.99", "10", "NonExistentCategory"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("category") && e.contains("not exist")));
    }
    
    @Test
    void testValidateEmptyCategory() {
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", "Front brake pad", "25.99", "10", ""
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("category") && e.contains("empty")));
    }
    
    @Test
    void testValidateNullCategory() {
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", "Front brake pad", "25.99", "10", null
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("category") && e.contains("empty")));
    }
    
    @Test
    void testValidateNullCategoryMap() {
        // If categoryMap is null, category validation should fail
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", "Front brake pad", "25.99", "10", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, null);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("category") && e.contains("not exist")));
    }
    
    @Test
    void testValidateEmptyCategoryMap() {
        // If categoryMap is empty, all categories should fail validation
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", "Front brake pad", "25.99", "10", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, new HashMap<>());
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("category") && e.contains("not exist")));
    }
    
    @Test
    void testValidateCategoryCaseSensitive() {
        // Category names should be case-sensitive
        PartImportData data = new PartImportData(
            1, "Brake Pad", "Bosch", "Front brake pad", "25.99", "10", "brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("category") && e.contains("not exist")));
    }
    
    // ========== Multiple Errors Tests (Requirement 4.7) ==========
    
    @Test
    void testValidateMultipleErrors() {
        // Test that all validation errors are collected
        PartImportData data = new PartImportData(
            1, "", "", "", "invalid", "invalid", "InvalidCategory"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertEquals(6, result.getErrors().size());
        
        // Verify all expected errors are present
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("name")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("maker")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("description")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("price")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("quantity")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("category")));
    }
    
    @Test
    void testValidatePartialErrors() {
        // Test with some valid and some invalid fields
        PartImportData data = new PartImportData(
            1, "Brake Pad", "", "Front brake pad", "-10", "abc", "Brakes"
        );
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertEquals(3, result.getErrors().size());
        
        // Verify specific errors
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("maker")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("price")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("quantity")));
        
        // Verify no errors for valid fields
        assertFalse(result.getErrors().stream().anyMatch(e -> e.contains("name")));
        assertFalse(result.getErrors().stream().anyMatch(e -> e.contains("description")));
        assertFalse(result.getErrors().stream().anyMatch(e -> e.contains("category")));
    }
    
    @Test
    void testValidateAllFieldsNull() {
        // Test with all null fields
        PartImportData data = new PartImportData(1, null, null, null, null, null, null);
        
        ValidationResult result = validator.validate(data, categoryMap);
        
        assertFalse(result.isValid());
        assertEquals(6, result.getErrors().size());
    }
}
