package org.chaos.office.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.chaos.office.model.ImportError.ErrorType;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the ImportResult model.
 * Tests initialization, getters/setters, error handling, and summary generation.
 */
class ImportResultTest {

    @Test
    void testDefaultConstructor() {
        ImportResult result = new ImportResult();
        assertNotNull(result);
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().isEmpty());
        assertFalse(result.hasErrors());
    }

    @Test
    void testParameterizedConstructor() {
        List<ImportError> errors = Arrays.asList(
            new ImportError(1, "name", "Name cannot be empty", ErrorType.VALIDATION),
            new ImportError(2, "price", "Invalid price", ErrorType.VALIDATION)
        );
        
        ImportResult result = new ImportResult(5, 2, errors);
        
        assertEquals(5, result.getSuccessCount());
        assertEquals(2, result.getFailureCount());
        assertEquals(2, result.getErrors().size());
        assertTrue(result.hasErrors());
    }

    @Test
    void testParameterizedConstructorWithNullErrors() {
        ImportResult result = new ImportResult(3, 0, null);
        
        assertEquals(3, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().isEmpty());
        assertFalse(result.hasErrors());
    }

    @Test
    void testSettersAndGetters() {
        ImportResult result = new ImportResult();
        
        result.setSuccessCount(10);
        result.setFailureCount(3);
        
        List<ImportError> errors = Arrays.asList(
            new ImportError(1, "Error 1", ErrorType.PARSING),
            new ImportError(2, "Error 2", ErrorType.VALIDATION),
            new ImportError(3, "Error 3", ErrorType.DATABASE)
        );
        result.setErrors(errors);
        
        assertEquals(10, result.getSuccessCount());
        assertEquals(3, result.getFailureCount());
        assertEquals(3, result.getErrors().size());
        assertTrue(result.hasErrors());
    }

    @Test
    void testSetErrorsWithNull() {
        ImportResult result = new ImportResult();
        result.setErrors(null);
        
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().isEmpty());
        assertFalse(result.hasErrors());
    }

    @Test
    void testHasErrorsWithEmptyList() {
        ImportResult result = new ImportResult(5, 0, new ArrayList<>());
        assertFalse(result.hasErrors());
    }

    @Test
    void testHasErrorsWithErrors() {
        List<ImportError> errors = Arrays.asList(
            new ImportError(1, "Error", ErrorType.VALIDATION)
        );
        ImportResult result = new ImportResult(5, 1, errors);
        assertTrue(result.hasErrors());
    }

    @Test
    void testAddError() {
        ImportResult result = new ImportResult();
        assertFalse(result.hasErrors());
        
        ImportError error = new ImportError(1, "name", "Name is empty", ErrorType.VALIDATION);
        result.addError(error);
        
        assertTrue(result.hasErrors());
        assertEquals(1, result.getErrors().size());
        assertEquals(error, result.getErrors().get(0));
    }

    @Test
    void testAddMultipleErrors() {
        ImportResult result = new ImportResult();
        
        result.addError(new ImportError(1, "Error 1", ErrorType.VALIDATION));
        result.addError(new ImportError(2, "Error 2", ErrorType.PARSING));
        result.addError(new ImportError(3, "Error 3", ErrorType.DATABASE));
        
        assertEquals(3, result.getErrors().size());
        assertTrue(result.hasErrors());
    }

    @Test
    void testIncrementSuccessCount() {
        ImportResult result = new ImportResult();
        assertEquals(0, result.getSuccessCount());
        
        result.incrementSuccessCount();
        assertEquals(1, result.getSuccessCount());
        
        result.incrementSuccessCount();
        assertEquals(2, result.getSuccessCount());
        
        result.incrementSuccessCount();
        assertEquals(3, result.getSuccessCount());
    }

    @Test
    void testIncrementFailureCount() {
        ImportResult result = new ImportResult();
        assertEquals(0, result.getFailureCount());
        
        result.incrementFailureCount();
        assertEquals(1, result.getFailureCount());
        
        result.incrementFailureCount();
        assertEquals(2, result.getFailureCount());
        
        result.incrementFailureCount();
        assertEquals(3, result.getFailureCount());
    }

    @Test
    void testGetSummaryWithNoErrors() {
        ImportResult result = new ImportResult(10, 0, new ArrayList<>());
        
        String summary = result.getSummary();
        
        assertTrue(summary.contains("Successfully imported: 10 part(s)"));
        assertTrue(summary.contains("Failed to import: 0 part(s)"));
        assertFalse(summary.contains("Errors:"));
    }

    @Test
    void testGetSummaryWithErrors() {
        List<ImportError> errors = Arrays.asList(
            new ImportError(1, "name", "Name cannot be empty", ErrorType.VALIDATION),
            new ImportError(2, "price", "Invalid price", ErrorType.VALIDATION)
        );
        ImportResult result = new ImportResult(5, 2, errors);
        
        String summary = result.getSummary();
        
        assertTrue(summary.contains("Successfully imported: 5 part(s)"));
        assertTrue(summary.contains("Failed to import: 2 part(s)"));
        assertTrue(summary.contains("Errors:"));
        assertTrue(summary.contains("Row 1: [name] - Name cannot be empty"));
        assertTrue(summary.contains("Row 2: [price] - Invalid price"));
    }

    @Test
    void testGetSummaryWithAllFailures() {
        List<ImportError> errors = Arrays.asList(
            new ImportError(1, "Parsing error", ErrorType.PARSING),
            new ImportError(2, "Validation error", ErrorType.VALIDATION),
            new ImportError(3, "Database error", ErrorType.DATABASE)
        );
        ImportResult result = new ImportResult(0, 3, errors);
        
        String summary = result.getSummary();
        
        assertTrue(summary.contains("Successfully imported: 0 part(s)"));
        assertTrue(summary.contains("Failed to import: 3 part(s)"));
        assertTrue(summary.contains("Errors:"));
        assertTrue(summary.contains("Row 1:"));
        assertTrue(summary.contains("Row 2:"));
        assertTrue(summary.contains("Row 3:"));
    }

    @Test
    void testGetSummaryWithMixedResults() {
        List<ImportError> errors = Arrays.asList(
            new ImportError(3, "quantity", "Invalid quantity", ErrorType.VALIDATION),
            new ImportError(7, "category", "Category not found", ErrorType.VALIDATION)
        );
        ImportResult result = new ImportResult(8, 2, errors);
        
        String summary = result.getSummary();
        
        assertTrue(summary.contains("Successfully imported: 8 part(s)"));
        assertTrue(summary.contains("Failed to import: 2 part(s)"));
        assertTrue(summary.contains("Errors:"));
        assertTrue(summary.contains("Row 3:"));
        assertTrue(summary.contains("Row 7:"));
    }

    @Test
    void testToString() {
        List<ImportError> errors = Arrays.asList(
            new ImportError(1, "Error 1", ErrorType.VALIDATION),
            new ImportError(2, "Error 2", ErrorType.PARSING)
        );
        ImportResult result = new ImportResult(5, 2, errors);
        
        String str = result.toString();
        
        assertTrue(str.contains("successCount=5"));
        assertTrue(str.contains("failureCount=2"));
        assertTrue(str.contains("2 error(s)"));
    }

    @Test
    void testToStringWithNoErrors() {
        ImportResult result = new ImportResult(10, 0, new ArrayList<>());
        
        String str = result.toString();
        
        assertTrue(str.contains("successCount=10"));
        assertTrue(str.contains("failureCount=0"));
        assertTrue(str.contains("0 error(s)"));
    }

    @Test
    void testZeroCounts() {
        ImportResult result = new ImportResult(0, 0, new ArrayList<>());
        
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertFalse(result.hasErrors());
    }

    @Test
    void testLargeCounts() {
        ImportResult result = new ImportResult();
        result.setSuccessCount(999999);
        result.setFailureCount(888888);
        
        assertEquals(999999, result.getSuccessCount());
        assertEquals(888888, result.getFailureCount());
    }

    @Test
    void testNegativeCounts() {
        // While negative counts don't make sense in practice,
        // the model should still accept them
        ImportResult result = new ImportResult();
        result.setSuccessCount(-1);
        result.setFailureCount(-2);
        
        assertEquals(-1, result.getSuccessCount());
        assertEquals(-2, result.getFailureCount());
    }

    @Test
    void testManyErrors() {
        List<ImportError> errors = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            errors.add(new ImportError(i, "Error " + i, ErrorType.VALIDATION));
        }
        
        ImportResult result = new ImportResult(0, 100, errors);
        
        assertEquals(100, result.getErrors().size());
        assertTrue(result.hasErrors());
        
        String summary = result.getSummary();
        assertTrue(summary.contains("Failed to import: 100 part(s)"));
    }

    @Test
    void testErrorListModification() {
        ImportResult result = new ImportResult();
        
        result.addError(new ImportError(1, "Error 1", ErrorType.VALIDATION));
        assertEquals(1, result.getErrors().size());
        
        result.addError(new ImportError(2, "Error 2", ErrorType.PARSING));
        assertEquals(2, result.getErrors().size());
        
        // Get the list and verify it contains the errors
        List<ImportError> errors = result.getErrors();
        assertEquals(2, errors.size());
    }

    @Test
    void testSuccessOnlyResult() {
        ImportResult result = new ImportResult(50, 0, new ArrayList<>());
        
        assertEquals(50, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertFalse(result.hasErrors());
        
        String summary = result.getSummary();
        assertTrue(summary.contains("Successfully imported: 50 part(s)"));
        assertTrue(summary.contains("Failed to import: 0 part(s)"));
        assertFalse(summary.contains("Errors:"));
    }

    @Test
    void testFailureOnlyResult() {
        List<ImportError> errors = Arrays.asList(
            new ImportError(1, "Error 1", ErrorType.VALIDATION),
            new ImportError(2, "Error 2", ErrorType.VALIDATION),
            new ImportError(3, "Error 3", ErrorType.VALIDATION)
        );
        ImportResult result = new ImportResult(0, 3, errors);
        
        assertEquals(0, result.getSuccessCount());
        assertEquals(3, result.getFailureCount());
        assertTrue(result.hasErrors());
        
        String summary = result.getSummary();
        assertTrue(summary.contains("Successfully imported: 0 part(s)"));
        assertTrue(summary.contains("Failed to import: 3 part(s)"));
        assertTrue(summary.contains("Errors:"));
    }

    @Test
    void testIncrementOperations() {
        ImportResult result = new ImportResult();
        
        // Simulate processing 10 parts: 7 success, 3 failures
        for (int i = 0; i < 7; i++) {
            result.incrementSuccessCount();
        }
        
        for (int i = 0; i < 3; i++) {
            result.incrementFailureCount();
            result.addError(new ImportError(i + 1, "Error " + (i + 1), ErrorType.VALIDATION));
        }
        
        assertEquals(7, result.getSuccessCount());
        assertEquals(3, result.getFailureCount());
        assertEquals(3, result.getErrors().size());
        assertTrue(result.hasErrors());
    }

    @Test
    void testSummaryFormatting() {
        List<ImportError> errors = Arrays.asList(
            new ImportError(5, "name", "Name is required", ErrorType.VALIDATION)
        );
        ImportResult result = new ImportResult(9, 1, errors);
        
        String summary = result.getSummary();
        
        // Check that summary has proper structure
        assertTrue(summary.startsWith("Import Summary:"));
        assertTrue(summary.contains("\n"));
        assertTrue(summary.contains("Successfully imported:"));
        assertTrue(summary.contains("Failed to import:"));
        assertTrue(summary.contains("Errors:"));
        assertTrue(summary.contains("  - Row 5:"));
    }

    @Test
    void testDifferentErrorTypes() {
        List<ImportError> errors = Arrays.asList(
            new ImportError(1, "Parsing error", ErrorType.PARSING),
            new ImportError(2, "name", "Validation error", ErrorType.VALIDATION),
            new ImportError(3, "Database error", ErrorType.DATABASE)
        );
        ImportResult result = new ImportResult(7, 3, errors);
        
        assertTrue(result.hasErrors());
        assertEquals(3, result.getErrors().size());
        
        String summary = result.getSummary();
        assertTrue(summary.contains("Row 1:"));
        assertTrue(summary.contains("Row 2:"));
        assertTrue(summary.contains("Row 3:"));
    }

    @Test
    void testEmptyErrorList() {
        ImportResult result = new ImportResult(5, 0, new ArrayList<>());
        
        assertFalse(result.hasErrors());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void testAddErrorToNullList() {
        ImportResult result = new ImportResult();
        result.setErrors(null);
        
        // addError should handle null list by creating a new one
        result.addError(new ImportError(1, "Error", ErrorType.VALIDATION));
        
        assertNotNull(result.getErrors());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.hasErrors());
    }

    @Test
    void testComplexScenario() {
        // Simulate a real import scenario
        ImportResult result = new ImportResult();
        
        // Process 20 parts
        for (int i = 1; i <= 20; i++) {
            if (i % 5 == 0) {
                // Every 5th part fails
                result.incrementFailureCount();
                result.addError(new ImportError(i, "price", "Invalid price", ErrorType.VALIDATION));
            } else {
                // Others succeed
                result.incrementSuccessCount();
            }
        }
        
        assertEquals(16, result.getSuccessCount());
        assertEquals(4, result.getFailureCount());
        assertEquals(4, result.getErrors().size());
        assertTrue(result.hasErrors());
        
        String summary = result.getSummary();
        assertTrue(summary.contains("Successfully imported: 16 part(s)"));
        assertTrue(summary.contains("Failed to import: 4 part(s)"));
        assertTrue(summary.contains("Row 5:"));
        assertTrue(summary.contains("Row 10:"));
        assertTrue(summary.contains("Row 15:"));
        assertTrue(summary.contains("Row 20:"));
    }

    @Test
    void testSummaryWithLongErrorMessages() {
        String longMessage = "This is a very long error message that contains a lot of detail about what went wrong during the import process. ".repeat(3);
        List<ImportError> errors = Arrays.asList(
            new ImportError(1, "field", longMessage, ErrorType.VALIDATION)
        );
        ImportResult result = new ImportResult(0, 1, errors);
        
        String summary = result.getSummary();
        assertTrue(summary.contains(longMessage));
    }

    @Test
    void testSummaryWithSpecialCharacters() {
        List<ImportError> errors = Arrays.asList(
            new ImportError(1, "name", "Name contains invalid characters: @#$%", ErrorType.VALIDATION),
            new ImportError(2, "description", "Description has \"quotes\" and 'apostrophes'", ErrorType.VALIDATION)
        );
        ImportResult result = new ImportResult(0, 2, errors);
        
        String summary = result.getSummary();
        assertTrue(summary.contains("@#$%"));
        assertTrue(summary.contains("\"quotes\""));
        assertTrue(summary.contains("'apostrophes'"));
    }
}
