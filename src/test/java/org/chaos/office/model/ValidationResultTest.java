package org.chaos.office.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the ValidationResult model.
 * Tests initialization, getters/setters, error handling, and data integrity.
 */
class ValidationResultTest {

    @Test
    void testDefaultConstructor() {
        ValidationResult result = new ValidationResult();
        assertNotNull(result);
        assertTrue(result.isValid());
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testParameterizedConstructorValid() {
        ValidationResult result = new ValidationResult(true, new ArrayList<>());
        
        assertTrue(result.isValid());
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testParameterizedConstructorInvalid() {
        List<String> errors = Arrays.asList(
            "Name cannot be empty",
            "Price must be a non-negative number"
        );
        
        ValidationResult result = new ValidationResult(false, errors);
        
        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());
        assertEquals("Name cannot be empty", result.getErrors().get(0));
        assertEquals("Price must be a non-negative number", result.getErrors().get(1));
    }

    @Test
    void testParameterizedConstructorWithNullErrors() {
        ValidationResult result = new ValidationResult(true, null);
        
        assertTrue(result.isValid());
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testSettersAndGetters() {
        ValidationResult result = new ValidationResult();
        
        result.setValid(false);
        List<String> errors = Arrays.asList(
            "Quantity must be a non-negative integer",
            "Category does not exist"
        );
        result.setErrors(errors);
        
        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());
        assertEquals("Quantity must be a non-negative integer", result.getErrors().get(0));
        assertEquals("Category does not exist", result.getErrors().get(1));
    }

    @Test
    void testSetErrorsWithNull() {
        ValidationResult result = new ValidationResult();
        result.setErrors(null);
        
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testAddError() {
        ValidationResult result = new ValidationResult();
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
        
        result.addError("Name cannot be empty");
        
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("Name cannot be empty", result.getErrors().get(0));
    }

    @Test
    void testAddMultipleErrors() {
        ValidationResult result = new ValidationResult();
        
        result.addError("Name cannot be empty");
        result.addError("Maker cannot be empty");
        result.addError("Description cannot be empty");
        
        assertFalse(result.isValid());
        assertEquals(3, result.getErrors().size());
        assertEquals("Name cannot be empty", result.getErrors().get(0));
        assertEquals("Maker cannot be empty", result.getErrors().get(1));
        assertEquals("Description cannot be empty", result.getErrors().get(2));
    }

    @Test
    void testAddErrorSetsValidToFalse() {
        ValidationResult result = new ValidationResult();
        assertTrue(result.isValid());
        
        result.addError("Some error");
        
        assertFalse(result.isValid());
    }

    @Test
    void testAddErrorToNullList() {
        ValidationResult result = new ValidationResult();
        result.setErrors(null);
        
        // addError should handle null list by creating a new one
        result.addError("Error message");
        
        assertNotNull(result.getErrors());
        assertEquals(1, result.getErrors().size());
        assertFalse(result.isValid());
    }

    @Test
    void testToString() {
        List<String> errors = Arrays.asList(
            "Error 1",
            "Error 2"
        );
        ValidationResult result = new ValidationResult(false, errors);
        
        String str = result.toString();
        
        assertTrue(str.contains("isValid=false"));
        assertTrue(str.contains("2 error(s)"));
    }

    @Test
    void testToStringWithNoErrors() {
        ValidationResult result = new ValidationResult(true, new ArrayList<>());
        
        String str = result.toString();
        
        assertTrue(str.contains("isValid=true"));
        assertTrue(str.contains("0 error(s)"));
    }

    @Test
    void testValidResultWithNoErrors() {
        ValidationResult result = new ValidationResult(true, new ArrayList<>());
        
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testInvalidResultWithErrors() {
        List<String> errors = Arrays.asList("Error 1", "Error 2");
        ValidationResult result = new ValidationResult(false, errors);
        
        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());
    }

    @Test
    void testEmptyErrorMessage() {
        ValidationResult result = new ValidationResult();
        result.addError("");
        
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("", result.getErrors().get(0));
    }

    @Test
    void testNullErrorMessage() {
        ValidationResult result = new ValidationResult();
        result.addError(null);
        
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertNull(result.getErrors().get(0));
    }

    @Test
    void testLongErrorMessage() {
        String longMessage = "A".repeat(1000);
        ValidationResult result = new ValidationResult();
        result.addError(longMessage);
        
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(longMessage, result.getErrors().get(0));
    }

    @Test
    void testSpecialCharactersInErrorMessage() {
        ValidationResult result = new ValidationResult();
        result.addError("Error with \"quotes\" and 'apostrophes' and <tags> and symbols: @#$%");
        
        assertFalse(result.isValid());
        assertEquals("Error with \"quotes\" and 'apostrophes' and <tags> and symbols: @#$%", 
                     result.getErrors().get(0));
    }

    @Test
    void testWhitespaceInErrorMessage() {
        ValidationResult result = new ValidationResult();
        result.addError("  Error message with spaces  ");
        
        assertFalse(result.isValid());
        // Model should preserve whitespace
        assertEquals("  Error message with spaces  ", result.getErrors().get(0));
    }

    @Test
    void testNewlineInErrorMessage() {
        ValidationResult result = new ValidationResult();
        result.addError("Error on line 1\nContinued on line 2");
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("\n"));
    }

    @Test
    void testManyErrors() {
        ValidationResult result = new ValidationResult();
        
        for (int i = 1; i <= 100; i++) {
            result.addError("Error " + i);
        }
        
        assertFalse(result.isValid());
        assertEquals(100, result.getErrors().size());
    }

    @Test
    void testErrorListModification() {
        ValidationResult result = new ValidationResult();
        
        result.addError("Error 1");
        assertEquals(1, result.getErrors().size());
        
        result.addError("Error 2");
        assertEquals(2, result.getErrors().size());
        
        // Get the list and verify it contains the errors
        List<String> errors = result.getErrors();
        assertEquals(2, errors.size());
        assertEquals("Error 1", errors.get(0));
        assertEquals("Error 2", errors.get(1));
    }

    @Test
    void testSetValidToTrue() {
        ValidationResult result = new ValidationResult(false, Arrays.asList("Error"));
        assertFalse(result.isValid());
        
        result.setValid(true);
        assertTrue(result.isValid());
        // Note: errors list is not cleared when setting valid to true
        assertEquals(1, result.getErrors().size());
    }

    @Test
    void testSetValidToFalse() {
        ValidationResult result = new ValidationResult(true, new ArrayList<>());
        assertTrue(result.isValid());
        
        result.setValid(false);
        assertFalse(result.isValid());
    }

    @Test
    void testMultipleValidationErrors() {
        ValidationResult result = new ValidationResult();
        
        result.addError("Name cannot be empty");
        result.addError("Maker cannot be empty");
        result.addError("Description cannot be empty");
        result.addError("Price must be a non-negative number");
        result.addError("Quantity must be a non-negative integer");
        result.addError("Category does not exist");
        
        assertFalse(result.isValid());
        assertEquals(6, result.getErrors().size());
    }

    @Test
    void testDuplicateErrors() {
        ValidationResult result = new ValidationResult();
        
        result.addError("Name cannot be empty");
        result.addError("Name cannot be empty");
        
        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());
        assertEquals("Name cannot be empty", result.getErrors().get(0));
        assertEquals("Name cannot be empty", result.getErrors().get(1));
    }

    @Test
    void testComplexValidationScenario() {
        // Simulate validating a PartImportData with multiple issues
        ValidationResult result = new ValidationResult();
        
        // Check name
        String name = "";
        if (name == null || name.trim().isEmpty()) {
            result.addError("Name cannot be empty");
        }
        
        // Check price
        String priceStr = "invalid";
        try {
            float price = Float.parseFloat(priceStr);
            if (price < 0) {
                result.addError("Price must be non-negative");
            }
        } catch (NumberFormatException e) {
            result.addError("Price must be a valid number");
        }
        
        // Check quantity
        String quantityStr = "-5";
        try {
            int quantity = Integer.parseInt(quantityStr);
            if (quantity < 0) {
                result.addError("Quantity must be non-negative");
            }
        } catch (NumberFormatException e) {
            result.addError("Quantity must be a valid integer");
        }
        
        assertFalse(result.isValid());
        assertEquals(3, result.getErrors().size());
        assertTrue(result.getErrors().contains("Name cannot be empty"));
        assertTrue(result.getErrors().contains("Price must be a valid number"));
        assertTrue(result.getErrors().contains("Quantity must be non-negative"));
    }

    @Test
    void testValidationPassesWithNoErrors() {
        // Simulate validating a valid PartImportData
        ValidationResult result = new ValidationResult();
        
        // All validations pass, no errors added
        String name = "Brake Pad";
        String maker = "Bosch";
        String description = "High-performance brake pad";
        String priceStr = "49.99";
        String quantityStr = "10";
        
        if (name == null || name.trim().isEmpty()) {
            result.addError("Name cannot be empty");
        }
        if (maker == null || maker.trim().isEmpty()) {
            result.addError("Maker cannot be empty");
        }
        if (description == null || description.trim().isEmpty()) {
            result.addError("Description cannot be empty");
        }
        
        try {
            float price = Float.parseFloat(priceStr);
            if (price < 0) {
                result.addError("Price must be non-negative");
            }
        } catch (NumberFormatException e) {
            result.addError("Price must be a valid number");
        }
        
        try {
            int quantity = Integer.parseInt(quantityStr);
            if (quantity < 0) {
                result.addError("Quantity must be non-negative");
            }
        } catch (NumberFormatException e) {
            result.addError("Quantity must be a valid integer");
        }
        
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testErrorListImmutability() {
        List<String> originalErrors = new ArrayList<>(Arrays.asList("Error 1", "Error 2"));
        ValidationResult result = new ValidationResult(false, originalErrors);
        
        // Modify the original list
        originalErrors.add("Error 3");
        
        // ValidationResult should have its own copy (or the original reference)
        // This test documents the current behavior
        assertEquals(3, result.getErrors().size());
    }

    @Test
    void testSetErrorsReplacesExistingErrors() {
        ValidationResult result = new ValidationResult();
        result.addError("Error 1");
        result.addError("Error 2");
        
        assertEquals(2, result.getErrors().size());
        
        List<String> newErrors = Arrays.asList("New Error 1", "New Error 2", "New Error 3");
        result.setErrors(newErrors);
        
        assertEquals(3, result.getErrors().size());
        assertEquals("New Error 1", result.getErrors().get(0));
        assertEquals("New Error 2", result.getErrors().get(1));
        assertEquals("New Error 3", result.getErrors().get(2));
    }

    @Test
    void testConsistentStateAfterMultipleOperations() {
        ValidationResult result = new ValidationResult();
        
        // Start valid
        assertTrue(result.isValid());
        
        // Add error - becomes invalid
        result.addError("Error 1");
        assertFalse(result.isValid());
        
        // Add more errors - stays invalid
        result.addError("Error 2");
        result.addError("Error 3");
        assertFalse(result.isValid());
        assertEquals(3, result.getErrors().size());
        
        // Manually set to valid (unusual but possible)
        result.setValid(true);
        assertTrue(result.isValid());
        // Errors still present
        assertEquals(3, result.getErrors().size());
        
        // Add another error - becomes invalid again
        result.addError("Error 4");
        assertFalse(result.isValid());
        assertEquals(4, result.getErrors().size());
    }

    @Test
    void testEmptyErrorList() {
        ValidationResult result = new ValidationResult(true, new ArrayList<>());
        
        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void testSingleError() {
        ValidationResult result = new ValidationResult();
        result.addError("Single error message");
        
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("Single error message", result.getErrors().get(0));
    }

    @Test
    void testErrorMessagesWithDifferentFormats() {
        ValidationResult result = new ValidationResult();
        
        result.addError("Simple error");
        result.addError("Error with field: name");
        result.addError("Error with details: expected 6 columns, found 4");
        result.addError("Error with number: row 5");
        
        assertFalse(result.isValid());
        assertEquals(4, result.getErrors().size());
    }

    @Test
    void testValidationResultForAllRequiredFields() {
        // Test scenario covering requirements 4.1-4.3 (required fields)
        ValidationResult result = new ValidationResult();
        
        result.addError("Name cannot be empty");
        result.addError("Maker cannot be empty");
        result.addError("Description cannot be empty");
        
        assertFalse(result.isValid());
        assertEquals(3, result.getErrors().size());
    }

    @Test
    void testValidationResultForNumericFields() {
        // Test scenario covering requirements 4.4-4.5 (numeric fields)
        ValidationResult result = new ValidationResult();
        
        result.addError("Price must be a non-negative number");
        result.addError("Quantity must be a non-negative integer");
        
        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());
    }

    @Test
    void testValidationResultForCategoryField() {
        // Test scenario covering requirements 4.6, 4.8 (category validation)
        ValidationResult result = new ValidationResult();
        
        result.addError("Category 'NonExistent' does not exist in database");
        
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
    }

    @Test
    void testValidationResultForAllValidations() {
        // Test scenario covering all requirements 4.1-4.8
        ValidationResult result = new ValidationResult();
        
        result.addError("Name cannot be empty");
        result.addError("Maker cannot be empty");
        result.addError("Description cannot be empty");
        result.addError("Price must be a non-negative number");
        result.addError("Quantity must be a non-negative integer");
        result.addError("Category does not exist");
        
        assertFalse(result.isValid());
        assertEquals(6, result.getErrors().size());
    }
}
