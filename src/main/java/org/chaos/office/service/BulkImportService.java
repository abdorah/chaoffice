package org.chaos.office.service;

import org.chaos.office.model.Category;
import org.chaos.office.model.ImportError;
import org.chaos.office.model.ImportResult;
import org.chaos.office.model.Part;
import org.chaos.office.model.PartImportData;
import org.chaos.office.model.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the bulk import process for parts from CSV or Excel files.
 * 
 * This service:
 * - Determines file type from extension
 * - Delegates parsing to appropriate parser (CSV or Excel)
 * - Validates each part using ImportValidator
 * - Converts valid parts to Part objects and saves via PartService
 * - Tracks success/failure counts and detailed errors
 * - Continues processing on individual failures (fail-safe)
 * 
 * Requirements: 2.4, 3.1, 3.2, 4.1-4.8, 5.1-5.5, 7.1-7.5
 */
public class BulkImportService {
    private static final Logger logger = LoggerFactory.getLogger(BulkImportService.class);
    
    private final PartService partService;
    private final CategoryService categoryService;
    private final CSVFileParser csvFileParser;
    private final ExcelFileParser excelFileParser;
    private final ImportValidator importValidator;
    
    /**
     * Constructor with all dependencies
     * 
     * @param partService Service for saving parts to database
     * @param categoryService Service for loading categories
     * @param csvFileParser Parser for CSV files
     * @param excelFileParser Parser for Excel files
     * @param importValidator Validator for part data
     */
    public BulkImportService(PartService partService, CategoryService categoryService,
                            CSVFileParser csvFileParser, ExcelFileParser excelFileParser,
                            ImportValidator importValidator) {
        this.partService = partService;
        this.categoryService = categoryService;
        this.csvFileParser = csvFileParser;
        this.excelFileParser = excelFileParser;
        this.importValidator = importValidator;
    }
    
    /**
     * Imports parts from a file (CSV or Excel).
     * 
     * Process:
     * 1. Determine file type from extension
     * 2. Select appropriate parser
     * 3. For Excel files: Check for Categories sheet and import categories first
     * 4. Parse file into List<PartImportData>
     * 5. Load all categories into Map<String, Category>
     * 6. For each PartImportData:
     *    - Validate using ImportValidator
     *    - If valid: convert to Part, save via PartService, increment success count
     *    - If invalid or save fails: record error, increment failure count
     * 7. Return ImportResult with counts and errors
     * 
     * @param file The file to import (.csv, .xlsx, or .xls)
     * @return ImportResult containing success/failure counts and error details
     */
    public ImportResult importFromFile(File file) {
        ImportResult result = new ImportResult();
        
        // Validate file exists
        if (file == null || !file.exists()) {
            result.addError(new ImportError(0, "File does not exist", ImportError.ErrorType.PARSING));
            result.incrementFailureCount();
            logger.error("Import failed: file does not exist");
            return result;
        }
        
        // Determine file type from extension (Requirement 2.4)
        String fileName = file.getName().toLowerCase();
        FileParser parser = null;
        boolean isExcelFile = false;
        
        if (fileName.endsWith(".csv")) {
            parser = csvFileParser;
            logger.info("Selected CSV parser for file: {}", file.getName());
        } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            parser = excelFileParser;
            isExcelFile = true;
            logger.info("Selected Excel parser for file: {}", file.getName());
        } else {
            // Unsupported file type (Requirement 2.4)
            result.addError(new ImportError(0, 
                "Unsupported file type. Please upload a .csv, .xlsx, or .xls file.", 
                ImportError.ErrorType.PARSING));
            result.incrementFailureCount();
            logger.error("Import failed: unsupported file type - {}", fileName);
            return result;
        }
        
        // For Excel files, check for Categories sheet and import categories first
        if (isExcelFile) {
            try {
                importCategoriesFromExcel(file);
            } catch (Exception e) {
                logger.warn("Failed to import categories from Excel file: {}", e.getMessage());
                // Continue with parts import even if categories import fails
            }
        }
        
        // Parse file into List<PartImportData> (Requirements 3.1, 3.2)
        List<PartImportData> importDataList;
        try {
            importDataList = parser.parse(file);
            logger.info("Parsed {} rows from file: {}", importDataList.size(), file.getName());
        } catch (IOException e) {
            // File read failure (Requirement 7.1)
            result.addError(new ImportError(0, 
                "Unable to read file. The file may be corrupted or inaccessible.", 
                ImportError.ErrorType.PARSING));
            result.incrementFailureCount();
            logger.error("Import failed: unable to read file - {}", file.getName(), e);
            return result;
        } catch (Exception e) {
            // Invalid file format (Requirement 7.2)
            result.addError(new ImportError(0, 
                "Invalid file format. Please ensure the file is a valid CSV or Excel file.", 
                ImportError.ErrorType.PARSING));
            result.incrementFailureCount();
            logger.error("Import failed: invalid file format - {}", file.getName(), e);
            return result;
        }
        
        // Load all categories into Map<String, Category> (Requirement 4.6)
        Map<String, Category> categoryMap = loadCategoryMap();
        logger.info("Loaded {} categories for validation", categoryMap.size());
        
        // Process each PartImportData (Requirements 5.1-5.5)
        for (PartImportData data : importDataList) {
            try {
                // Validate using ImportValidator (Requirements 4.1-4.8)
                ValidationResult validationResult = importValidator.validate(data, categoryMap);
                
                if (validationResult.isValid()) {
                    // Convert to Part object and save (Requirement 5.1)
                    Part part = convertToPart(data, categoryMap);
                    partService.savePart(part);
                    
                    // Increment success counter (Requirement 5.2)
                    result.incrementSuccessCount();
                    logger.debug("Successfully imported part from row {}: {}", data.getRowNumber(), data.getName());
                    
                } else {
                    // Record validation errors (Requirements 4.7, 5.3)
                    for (String errorMsg : validationResult.getErrors()) {
                        // Extract field name from error message if present
                        String fieldName = extractFieldName(errorMsg);
                        result.addError(new ImportError(
                            data.getRowNumber(),
                            fieldName,
                            errorMsg,
                            ImportError.ErrorType.VALIDATION
                        ));
                    }
                    result.incrementFailureCount();
                    logger.debug("Validation failed for row {}: {}", data.getRowNumber(), validationResult.getErrors());
                }
                
            } catch (Exception e) {
                // Database insertion failure (Requirements 5.3, 7.4)
                result.addError(new ImportError(
                    data.getRowNumber(),
                    "Database error - " + e.getMessage(),
                    ImportError.ErrorType.DATABASE
                ));
                result.incrementFailureCount();
                logger.error("Database error importing row {}: {}", data.getRowNumber(), e.getMessage(), e);
            }
            
            // Continue processing remaining parts (Requirement 5.4)
        }
        
        // Log final results (Requirement 7.5)
        logger.info("Import completed: {} successful, {} failed", 
            result.getSuccessCount(), result.getFailureCount());
        
        return result;
    }
    
    /**
     * Imports categories from the "Categories" sheet in an Excel file.
     * Creates categories that don't already exist in the database.
     * 
     * @param file The Excel file containing the Categories sheet
     * @throws IOException If the file cannot be read
     */
    private void importCategoriesFromExcel(File file) throws IOException {
        try (org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(file)) {
            // Look for "Categories" sheet
            org.apache.poi.ss.usermodel.Sheet categoriesSheet = workbook.getSheet("Categories");
            
            if (categoriesSheet == null) {
                logger.debug("No Categories sheet found in Excel file");
                return;
            }
            
            logger.info("Found Categories sheet, importing categories...");
            
            // Get existing categories to avoid duplicates
            Map<String, Category> existingCategories = loadCategoryMap();
            
            // Skip header row (row 0) and process data rows
            for (int rowIndex = 1; rowIndex <= categoriesSheet.getLastRowNum(); rowIndex++) {
                org.apache.poi.ss.usermodel.Row row = categoriesSheet.getRow(rowIndex);
                
                if (row == null) {
                    continue;
                }
                
                // Get category name and description
                org.apache.poi.ss.usermodel.Cell nameCell = row.getCell(0);
                org.apache.poi.ss.usermodel.Cell descCell = row.getCell(1);
                
                if (nameCell == null) {
                    continue;
                }
                
                String categoryName = getCellValueAsString(nameCell).trim();
                String categoryDesc = descCell != null ? getCellValueAsString(descCell).trim() : "";
                
                // Skip empty rows or existing categories
                if (categoryName.isEmpty() || existingCategories.containsKey(categoryName)) {
                    continue;
                }
                
                // Create new category
                Category newCategory = new Category();
                newCategory.setName(categoryName);
                newCategory.setDescription(categoryDesc);
                
                try {
                    categoryService.saveCategory(newCategory);
                    logger.info("Imported category: {}", categoryName);
                } catch (Exception e) {
                    logger.warn("Failed to import category '{}': {}", categoryName, e.getMessage());
                }
            }
        }
    }
    
    /**
     * Converts a cell value to a string, handling different cell types.
     * 
     * @param cell The cell to convert
     * @return The cell value as a string, or empty string if cell is null/blank
     */
    private String getCellValueAsString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
                
            case NUMERIC:
                // Check if it's a date
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                // Return numeric value without scientific notation
                double numericValue = cell.getNumericCellValue();
                if (numericValue == Math.floor(numericValue)) {
                    return String.valueOf((long) numericValue);
                }
                return String.valueOf(numericValue);
                
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
                
            case BLANK:
            default:
                return "";
        }
    }
    
    /**
     * Loads all categories from CategoryService into a Map keyed by category name.
     * This allows efficient category lookup during validation.
     * 
     * @return Map of category names to Category objects
     */
    private Map<String, Category> loadCategoryMap() {
        Map<String, Category> categoryMap = new HashMap<>();
        List<Category> categories = categoryService.getAllCategories();
        
        for (Category category : categories) {
            categoryMap.put(category.getName(), category);
        }
        
        return categoryMap;
    }
    
    /**
     * Converts a validated PartImportData object to a Part object.
     * Parses string values to appropriate types and resolves category reference.
     * 
     * @param data The validated PartImportData
     * @param categoryMap Map of category names to Category objects
     * @return Part object ready for database insertion
     */
    private Part convertToPart(PartImportData data, Map<String, Category> categoryMap) {
        Part part = new Part();
        
        // Set string fields
        part.setName(data.getName().trim());
        part.setMaker(data.getMaker().trim());
        part.setDescription(data.getDescription().trim());
        
        // Parse and set numeric fields
        part.setPrice(Float.parseFloat(data.getPriceStr().trim()));
        part.setQuantity(Integer.parseInt(data.getQuantityStr().trim()));
        
        // Resolve category reference
        Category category = categoryMap.get(data.getCategoryName());
        part.setCategory(category);
        
        return part;
    }
    
    /**
     * Extracts the field name from a validation error message.
     * Error messages typically start with the field name followed by " is" or " cannot".
     * 
     * Examples:
     * - "name is empty or whitespace-only" -> "name"
     * - "price cannot be parsed as a number" -> "price"
     * - "category does not exist" -> "category"
     * 
     * @param errorMessage The validation error message
     * @return The field name, or null if not found
     */
    private String extractFieldName(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return null;
        }
        
        // Common patterns in validation error messages
        String[] patterns = {" is ", " cannot ", " does "};
        
        for (String pattern : patterns) {
            int index = errorMessage.indexOf(pattern);
            if (index > 0) {
                return errorMessage.substring(0, index).trim();
            }
        }
        
        return null;
    }
}
