package org.chaos.office.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the result of validating a PartImportData object.
 * Contains a validity flag and a list of validation error messages.
 * Used by ImportValidator to communicate validation outcomes.
 * 
 * Requirements: 4.1-4.8
 */
public class ValidationResult {
    private boolean isValid;
    private List<String> errors;
    
    /**
     * Default constructor - initializes as valid with empty error list
     */
    public ValidationResult() {
        this.isValid = true;
        this.errors = new ArrayList<>();
    }
    
    /**
     * Constructor with all fields
     * 
     * @param isValid Whether the validation passed
     * @param errors List of validation error messages
     */
    public ValidationResult(boolean isValid, List<String> errors) {
        this.isValid = isValid;
        this.errors = errors != null ? errors : new ArrayList<>();
    }
    
    // Getters and setters
    
    public boolean isValid() {
        return isValid;
    }
    
    public void setValid(boolean valid) {
        isValid = valid;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public void setErrors(List<String> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
    }
    
    /**
     * Adds an error message to the error list and sets isValid to false
     * 
     * @param error The error message to add
     */
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
        this.isValid = false;
    }
    
    @Override
    public String toString() {
        return "ValidationResult{" +
                "isValid=" + isValid +
                ", errors=" + (errors != null ? errors.size() : 0) + " error(s)" +
                '}';
    }
}
