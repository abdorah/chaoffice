package org.chaos.office.service;

import org.chaos.office.model.Category;
import org.chaos.office.model.PartImportData;
import org.chaos.office.model.ValidationResult;

import java.util.Map;

/**
 * Validates PartImportData objects before database insertion.
 * Checks all required fields, validates data types and ranges,
 * and ensures category references exist.
 * 
 * Requirements: 4.1-4.8
 */
public class ImportValidator {
    
    /**
     * Validates a PartImportData object against validation rules.
     * 
     * Validation rules:
     * - Name must not be empty or whitespace-only (Requirement 4.1)
     * - Maker must not be empty or whitespace-only (Requirement 4.2)
     * - Description must not be empty or whitespace-only (Requirement 4.3)
     * - Price must be parseable as float and non-negative (Requirement 4.4)
     * - Quantity must be parseable as integer and non-negative (Requirement 4.5)
     * - Category name must exist in categoryMap (Requirement 4.6)
     * 
     * @param data The PartImportData to validate
     * @param categoryMap Map of category names to Category objects
     * @return ValidationResult with isValid flag and list of error messages
     */
    public ValidationResult validate(PartImportData data, Map<String, Category> categoryMap) {
        ValidationResult result = new ValidationResult();
        
        // Validate name (Requirement 4.1)
        if (data.getName() == null || data.getName().trim().isEmpty()) {
            result.addError("name is empty or whitespace-only");
        }
        
        // Validate maker (Requirement 4.2)
        if (data.getMaker() == null || data.getMaker().trim().isEmpty()) {
            result.addError("maker is empty or whitespace-only");
        }
        
        // Validate description (Requirement 4.3)
        if (data.getDescription() == null || data.getDescription().trim().isEmpty()) {
            result.addError("description is empty or whitespace-only");
        }
        
        // Validate price (Requirement 4.4)
        if (data.getPriceStr() == null || data.getPriceStr().trim().isEmpty()) {
            result.addError("price is empty");
        } else {
            try {
                float price = Float.parseFloat(data.getPriceStr().trim());
                if (price < 0) {
                    result.addError("price is negative");
                }
            } catch (NumberFormatException e) {
                result.addError("price cannot be parsed as a number");
            }
        }
        
        // Validate quantity (Requirement 4.5)
        if (data.getQuantityStr() == null || data.getQuantityStr().trim().isEmpty()) {
            result.addError("quantity is empty");
        } else {
            try {
                int quantity = Integer.parseInt(data.getQuantityStr().trim());
                if (quantity < 0) {
                    result.addError("quantity is negative");
                }
            } catch (NumberFormatException e) {
                result.addError("quantity cannot be parsed as an integer");
            }
        }
        
        // Validate category (Requirement 4.6, 4.8)
        if (data.getCategoryName() == null || data.getCategoryName().trim().isEmpty()) {
            result.addError("category is empty");
        } else if (categoryMap == null || !categoryMap.containsKey(data.getCategoryName())) {
            result.addError("category does not exist");
        }
        
        return result;
    }
}
