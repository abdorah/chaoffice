package org.chaos.office.model;

import static org.junit.jupiter.api.Assertions.*;

import org.chaos.office.model.ImportError.ErrorType;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the ImportError model.
 * Tests initialization, getters/setters, error formatting, and data integrity.
 */
class ImportErrorTest {

    @Test
    void testDefaultConstructor() {
        ImportError error = new ImportError();
        assertNotNull(error);
        assertEquals(0, error.getRowNumber());
        assertNull(error.getFieldName());
        assertNull(error.getErrorMessage());
        assertNull(error.getErrorType());
    }

    @Test
    void testParameterizedConstructorWithFieldName() {
        ImportError error = new ImportError(
            5,
            "price",
            "Price must be a non-negative number",
            ErrorType.VALIDATION
        );
        
        assertEquals(5, error.getRowNumber());
        assertEquals("price", error.getFieldName());
        assertEquals("Price must be a non-negative number", error.getErrorMessage());
        assertEquals(ErrorType.VALIDATION, error.getErrorType());
    }

    @Test
    void testParameterizedConstructorWithoutFieldName() {
        ImportError error = new ImportError(
            3,
            "Expected 6 columns, found 4",
            ErrorType.PARSING
        );
        
        assertEquals(3, error.getRowNumber());
        assertNull(error.getFieldName());
        assertEquals("Expected 6 columns, found 4", error.getErrorMessage());
        assertEquals(ErrorType.PARSING, error.getErrorType());
    }

    @Test
    void testSettersAndGetters() {
        ImportError error = new ImportError();
        
        error.setRowNumber(10);
        error.setFieldName("quantity");
        error.setErrorMessage("Quantity must be a non-negative integer");
        error.setErrorType(ErrorType.VALIDATION);
        
        assertEquals(10, error.getRowNumber());
        assertEquals("quantity", error.getFieldName());
        assertEquals("Quantity must be a non-negative integer", error.getErrorMessage());
        assertEquals(ErrorType.VALIDATION, error.getErrorType());
    }

    @Test
    void testErrorTypeEnum() {
        // Test all error type values
        assertEquals(ErrorType.VALIDATION, ErrorType.valueOf("VALIDATION"));
        assertEquals(ErrorType.PARSING, ErrorType.valueOf("PARSING"));
        assertEquals(ErrorType.DATABASE, ErrorType.valueOf("DATABASE"));
        
        // Test enum values array
        ErrorType[] types = ErrorType.values();
        assertEquals(3, types.length);
        assertTrue(java.util.Arrays.asList(types).contains(ErrorType.VALIDATION));
        assertTrue(java.util.Arrays.asList(types).contains(ErrorType.PARSING));
        assertTrue(java.util.Arrays.asList(types).contains(ErrorType.DATABASE));
    }

    @Test
    void testFormattedMessageWithFieldName() {
        ImportError error = new ImportError(
            7,
            "name",
            "Name cannot be empty",
            ErrorType.VALIDATION
        );
        
        String formatted = error.getFormattedMessage();
        assertEquals("Row 7: [name] - Name cannot be empty", formatted);
    }

    @Test
    void testFormattedMessageWithoutFieldName() {
        ImportError error = new ImportError(
            2,
            "Malformed CSV row",
            ErrorType.PARSING
        );
        
        String formatted = error.getFormattedMessage();
        assertEquals("Row 2: Malformed CSV row", formatted);
    }

    @Test
    void testFormattedMessageWithNullFieldName() {
        ImportError error = new ImportError(
            4,
            null,
            "Database connection failed",
            ErrorType.DATABASE
        );
        
        String formatted = error.getFormattedMessage();
        assertEquals("Row 4: Database connection failed", formatted);
    }

    @Test
    void testFormattedMessageWithEmptyFieldName() {
        ImportError error = new ImportError(
            6,
            "",
            "Invalid data format",
            ErrorType.PARSING
        );
        
        String formatted = error.getFormattedMessage();
        assertEquals("Row 6: Invalid data format", formatted);
    }

    @Test
    void testToString() {
        ImportError error = new ImportError(
            8,
            "category",
            "Category does not exist",
            ErrorType.VALIDATION
        );
        
        String result = error.toString();
        
        assertTrue(result.contains("rowNumber=8"));
        assertTrue(result.contains("fieldName='category'"));
        assertTrue(result.contains("errorMessage='Category does not exist'"));
        assertTrue(result.contains("errorType=VALIDATION"));
    }

    @Test
    void testValidationErrorType() {
        ImportError error = new ImportError(
            1,
            "maker",
            "Maker cannot be empty",
            ErrorType.VALIDATION
        );
        
        assertEquals(ErrorType.VALIDATION, error.getErrorType());
    }

    @Test
    void testParsingErrorType() {
        ImportError error = new ImportError(
            15,
            "Wrong number of columns",
            ErrorType.PARSING
        );
        
        assertEquals(ErrorType.PARSING, error.getErrorType());
    }

    @Test
    void testDatabaseErrorType() {
        ImportError error = new ImportError(
            20,
            "Failed to insert part into database",
            ErrorType.DATABASE
        );
        
        assertEquals(ErrorType.DATABASE, error.getErrorType());
    }

    @Test
    void testRowNumberZero() {
        ImportError error = new ImportError();
        error.setRowNumber(0);
        assertEquals(0, error.getRowNumber());
    }

    @Test
    void testRowNumberNegative() {
        // While negative row numbers don't make sense in practice,
        // the model should still accept them
        ImportError error = new ImportError();
        error.setRowNumber(-1);
        assertEquals(-1, error.getRowNumber());
    }

    @Test
    void testLargeRowNumber() {
        ImportError error = new ImportError();
        error.setRowNumber(999999);
        assertEquals(999999, error.getRowNumber());
    }

    @Test
    void testNullErrorMessage() {
        ImportError error = new ImportError(1, "field", null, ErrorType.VALIDATION);
        assertNull(error.getErrorMessage());
    }

    @Test
    void testEmptyErrorMessage() {
        ImportError error = new ImportError(1, "field", "", ErrorType.VALIDATION);
        assertEquals("", error.getErrorMessage());
    }

    @Test
    void testLongErrorMessage() {
        String longMessage = "A".repeat(1000);
        ImportError error = new ImportError(1, "field", longMessage, ErrorType.VALIDATION);
        assertEquals(longMessage, error.getErrorMessage());
    }

    @Test
    void testSpecialCharactersInFieldName() {
        ImportError error = new ImportError(
            1,
            "field_name-123",
            "Error message",
            ErrorType.VALIDATION
        );
        assertEquals("field_name-123", error.getFieldName());
    }

    @Test
    void testSpecialCharactersInErrorMessage() {
        ImportError error = new ImportError(
            1,
            "field",
            "Error with \"quotes\" and 'apostrophes' and <tags> and symbols: @#$%",
            ErrorType.VALIDATION
        );
        assertEquals("Error with \"quotes\" and 'apostrophes' and <tags> and symbols: @#$%", 
                     error.getErrorMessage());
    }

    @Test
    void testWhitespaceInFieldName() {
        ImportError error = new ImportError(
            1,
            "  field name  ",
            "Error message",
            ErrorType.VALIDATION
        );
        // Model should preserve whitespace
        assertEquals("  field name  ", error.getFieldName());
    }

    @Test
    void testWhitespaceInErrorMessage() {
        ImportError error = new ImportError(
            1,
            "field",
            "  Error message with spaces  ",
            ErrorType.VALIDATION
        );
        // Model should preserve whitespace
        assertEquals("  Error message with spaces  ", error.getErrorMessage());
    }

    @Test
    void testDataUpdate() {
        ImportError error = new ImportError(
            1,
            "originalField",
            "Original error message",
            ErrorType.VALIDATION
        );
        
        // Update all fields
        error.setRowNumber(2);
        error.setFieldName("updatedField");
        error.setErrorMessage("Updated error message");
        error.setErrorType(ErrorType.DATABASE);
        
        assertEquals(2, error.getRowNumber());
        assertEquals("updatedField", error.getFieldName());
        assertEquals("Updated error message", error.getErrorMessage());
        assertEquals(ErrorType.DATABASE, error.getErrorType());
    }

    @Test
    void testMultipleErrorsWithDifferentTypes() {
        ImportError validationError = new ImportError(1, "name", "Name is empty", ErrorType.VALIDATION);
        ImportError parsingError = new ImportError(2, "Malformed row", ErrorType.PARSING);
        ImportError databaseError = new ImportError(3, "Insert failed", ErrorType.DATABASE);
        
        assertEquals(ErrorType.VALIDATION, validationError.getErrorType());
        assertEquals(ErrorType.PARSING, parsingError.getErrorType());
        assertEquals(ErrorType.DATABASE, databaseError.getErrorType());
    }

    @Test
    void testFormattedMessageConsistency() {
        ImportError error1 = new ImportError(5, "price", "Invalid price", ErrorType.VALIDATION);
        ImportError error2 = new ImportError(5, "price", "Invalid price", ErrorType.VALIDATION);
        
        assertEquals(error1.getFormattedMessage(), error2.getFormattedMessage());
    }

    @Test
    void testConstructorWithNullFieldName() {
        ImportError error = new ImportError(1, null, "Error message", ErrorType.VALIDATION);
        assertNull(error.getFieldName());
        assertEquals("Row 1: Error message", error.getFormattedMessage());
    }

    @Test
    void testNewlineInErrorMessage() {
        ImportError error = new ImportError(
            1,
            "field",
            "Error on line 1\nContinued on line 2",
            ErrorType.VALIDATION
        );
        assertTrue(error.getErrorMessage().contains("\n"));
    }

    @Test
    void testFormattedMessageWithComplexScenario() {
        ImportError error = new ImportError(
            123,
            "categoryName",
            "Category 'Auto Parts/Brakes' does not exist in database",
            ErrorType.VALIDATION
        );
        
        String formatted = error.getFormattedMessage();
        assertEquals("Row 123: [categoryName] - Category 'Auto Parts/Brakes' does not exist in database", 
                     formatted);
        assertTrue(formatted.startsWith("Row 123:"));
        assertTrue(formatted.contains("[categoryName]"));
        assertTrue(formatted.contains("does not exist"));
    }
}
