package org.chaos.office.util;

/**
 * ValidationHelper utility class for input validation throughout the application.
 * Provides standardized validation methods for common input patterns.
 * 
 * <p>This class is responsible for:
 * <ul>
 *   <li>Validating required fields are not empty</li>
 *   <li>Validating numeric values are positive</li>
 *   <li>Validating phone number formats</li>
 * </ul>
 * 
 * <p>Requirements: 15.2, 15.3
 */
public class ValidationHelper {
    
    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with static methods only.
     */
    private ValidationHelper() {
        // Utility class - no instantiation
    }
    
    /**
     * Validates that a string is not null and not empty (after trimming whitespace).
     * 
     * @param value the string to validate
     * @return true if the string is not null and not empty, false otherwise
     */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    /**
     * Validates that a string represents a positive number (greater than zero).
     * 
     * @param value the string to validate
     * @return true if the string represents a positive number, false otherwise
     */
    public static boolean isPositiveNumber(String value) {
        if (!isNotEmpty(value)) {
            return false;
        }
        
        try {
            double number = Double.parseDouble(value.trim());
            return number > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Validates that a string represents a valid phone number.
     * Accepts various formats including:
     * - Digits only (e.g., "1234567890")
     * - With spaces (e.g., "123 456 7890")
     * - With dashes (e.g., "123-456-7890")
     * - With parentheses (e.g., "(123) 456-7890")
     * - With plus sign for international (e.g., "+1 123 456 7890")
     * 
     * @param phone the phone number string to validate
     * @return true if the phone number is valid, false otherwise
     */
    public static boolean isValidPhone(String phone) {
        if (!isNotEmpty(phone)) {
            return false;
        }
        
        // Remove all non-digit characters except plus sign
        String digitsOnly = phone.replaceAll("[^0-9+]", "");
        
        // Check if it starts with + (international format)
        if (digitsOnly.startsWith("+")) {
            digitsOnly = digitsOnly.substring(1);
        }
        
        // Valid phone number should have at least 7 digits and at most 15 digits
        return digitsOnly.length() >= 7 && digitsOnly.length() <= 15;
    }
}
