package org.chaos.office.model;

/**
 * Represents a single import error with context information.
 * Used during bulk import operations to track and report errors that occur
 * during file parsing, data validation, or database insertion.
 * 
 * Requirements: 4.7, 7.3
 */
public class ImportError {
    private int rowNumber;
    private String fieldName;
    private String errorMessage;
    private ErrorType errorType;
    
    /**
     * Error type enumeration for categorizing import errors
     */
    public enum ErrorType {
        /** Validation errors (e.g., empty required fields, invalid data formats) */
        VALIDATION,
        
        /** Parsing errors (e.g., malformed CSV/Excel, wrong number of columns) */
        PARSING,
        
        /** Database errors (e.g., insertion failures, constraint violations) */
        DATABASE
    }
    
    /**
     * Default constructor
     */
    public ImportError() {
    }
    
    /**
     * Constructor with all fields
     * 
     * @param rowNumber The row number where the error occurred (1-indexed)
     * @param fieldName The field that caused the error (nullable for non-field-specific errors)
     * @param errorMessage Description of the error
     * @param errorType The type of error (VALIDATION, PARSING, or DATABASE)
     */
    public ImportError(int rowNumber, String fieldName, String errorMessage, ErrorType errorType) {
        this.rowNumber = rowNumber;
        this.fieldName = fieldName;
        this.errorMessage = errorMessage;
        this.errorType = errorType;
    }
    
    /**
     * Constructor without field name (for errors not specific to a field)
     * 
     * @param rowNumber The row number where the error occurred (1-indexed)
     * @param errorMessage Description of the error
     * @param errorType The type of error (VALIDATION, PARSING, or DATABASE)
     */
    public ImportError(int rowNumber, String errorMessage, ErrorType errorType) {
        this(rowNumber, null, errorMessage, errorType);
    }
    
    // Getters and setters
    
    public int getRowNumber() {
        return rowNumber;
    }
    
    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public ErrorType getErrorType() {
        return errorType;
    }
    
    public void setErrorType(ErrorType errorType) {
        this.errorType = errorType;
    }
    
    /**
     * Returns a formatted error message for display to users
     * Format: "Row N: [Field] - Error message" or "Row N: Error message" if no field
     * 
     * @return Formatted error message string
     */
    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Row ").append(rowNumber).append(": ");
        
        if (fieldName != null && !fieldName.isEmpty()) {
            sb.append("[").append(fieldName).append("] - ");
        }
        
        sb.append(errorMessage);
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "ImportError{" +
                "rowNumber=" + rowNumber +
                ", fieldName='" + fieldName + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorType=" + errorType +
                '}';
    }
}
