package org.chaos.office.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the results of a bulk import operation.
 * Contains success/failure counts and detailed error information for reporting to users.
 * 
 * Requirements: 5.5, 6.3, 6.4
 */
public class ImportResult {
    private int successCount;
    private int failureCount;
    private List<ImportError> errors;
    
    /**
     * Default constructor - initializes with zero counts and empty error list
     */
    public ImportResult() {
        this.successCount = 0;
        this.failureCount = 0;
        this.errors = new ArrayList<>();
    }
    
    /**
     * Constructor with all fields
     * 
     * @param successCount Number of successfully imported parts
     * @param failureCount Number of failed imports
     * @param errors List of detailed error information
     */
    public ImportResult(int successCount, int failureCount, List<ImportError> errors) {
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.errors = errors != null ? errors : new ArrayList<>();
    }
    
    // Getters and setters
    
    public int getSuccessCount() {
        return successCount;
    }
    
    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }
    
    public int getFailureCount() {
        return failureCount;
    }
    
    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }
    
    public List<ImportError> getErrors() {
        return errors;
    }
    
    public void setErrors(List<ImportError> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
    }
    
    /**
     * Checks if any errors occurred during the import operation
     * 
     * @return true if there are any errors, false otherwise
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
    
    /**
     * Generates a human-readable summary of the import operation
     * 
     * @return A formatted summary string with success/failure counts and error details
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        
        // Add success/failure counts
        summary.append("Import Summary:\n");
        summary.append("Successfully imported: ").append(successCount).append(" part(s)\n");
        summary.append("Failed to import: ").append(failureCount).append(" part(s)\n");
        
        // Add error details if any
        if (hasErrors()) {
            summary.append("\nErrors:\n");
            for (ImportError error : errors) {
                summary.append("  - ").append(error.getFormattedMessage()).append("\n");
            }
        }
        
        return summary.toString();
    }
    
    /**
     * Adds an error to the error list
     * 
     * @param error The ImportError to add
     */
    public void addError(ImportError error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
    }
    
    /**
     * Increments the success count by 1
     */
    public void incrementSuccessCount() {
        this.successCount++;
    }
    
    /**
     * Increments the failure count by 1
     */
    public void incrementFailureCount() {
        this.failureCount++;
    }
    
    @Override
    public String toString() {
        return "ImportResult{" +
                "successCount=" + successCount +
                ", failureCount=" + failureCount +
                ", errors=" + (errors != null ? errors.size() : 0) + " error(s)" +
                '}';
    }
}
