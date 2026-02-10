package org.chaos.office.service;

import org.apache.poi.ss.usermodel.*;
import org.chaos.office.model.PartImportData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses Excel files (.xlsx, .xls) into PartImportData objects using Apache POI.
 * Handles header row detection, different cell types (STRING, NUMERIC),
 * and proper workbook resource management.
 * 
 * Expected Excel format:
 * - 6 columns: name, maker, description, price, quantity, category
 * - Optional header row (detected if first cell is "name" or "Name")
 * - First sheet is used for data extraction
 * 
 * Requirements: 3.2, 3.3, 3.4, 3.6
 */
public class ExcelFileParser implements FileParser {
    
    private static final int EXPECTED_COLUMN_COUNT = 6;
    
    /**
     * Parses an Excel file and extracts part data from each row.
     * 
     * @param file The Excel file to parse (.xlsx or .xls)
     * @return List of PartImportData objects, one per data row (excluding headers)
     * @throws IOException If the file cannot be read or is not a valid Excel file
     */
    @Override
    public List<PartImportData> parse(File file) throws IOException {
        List<PartImportData> results = new ArrayList<>();
        
        try (Workbook workbook = WorkbookFactory.create(file)) {
            // Get the first sheet
            Sheet sheet = workbook.getSheetAt(0);
            
            if (sheet == null) {
                return results; // Empty file
            }
            
            boolean firstRowIsHeader = false;
            int startRow = 0;
            
            // Check if first row is a header
            Row firstRow = sheet.getRow(0);
            if (firstRow != null) {
                firstRowIsHeader = isHeaderRow(firstRow);
                if (firstRowIsHeader) {
                    startRow = 1; // Skip header row
                }
            }
            
            // Process each row
            for (int rowIndex = startRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                
                // Skip null or empty rows
                if (row == null || isEmptyRow(row)) {
                    continue;
                }
                
                // Extract cells from the row
                List<String> fields = extractFields(row);
                
                // Validate column count - skip malformed rows
                if (fields.size() != EXPECTED_COLUMN_COUNT) {
                    continue;
                }
                
                // Create PartImportData object
                // Row numbers are 1-indexed for user-friendly error reporting
                PartImportData data = new PartImportData(
                    rowIndex + 1,  // Convert to 1-indexed
                    fields.get(0), // name
                    fields.get(1), // maker
                    fields.get(2), // description
                    fields.get(3), // price
                    fields.get(4), // quantity
                    fields.get(5)  // category
                );
                
                results.add(data);
            }
        }
        
        return results;
    }
    
    /**
     * Determines if a row is a header row by checking if the first cell
     * contains "name" (case-insensitive).
     * 
     * @param row The first row of the Excel sheet
     * @return true if the row is a header row, false otherwise
     */
    private boolean isHeaderRow(Row row) {
        Cell firstCell = row.getCell(0);
        if (firstCell == null) {
            return false;
        }
        
        String cellValue = getCellValueAsString(firstCell).trim().toLowerCase();
        return cellValue.equals("name");
    }
    
    /**
     * Checks if a row is empty (all cells are null or blank).
     * 
     * @param row The row to check
     * @return true if the row is empty, false otherwise
     */
    private boolean isEmptyRow(Row row) {
        for (int i = 0; i < EXPECTED_COLUMN_COUNT; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(cell).trim();
                if (!value.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Extracts field values from a row, handling different cell types.
     * Only extracts fields up to the last non-null cell to detect malformed rows.
     * 
     * @param row The row to extract fields from
     * @return List of field values as strings
     */
    private List<String> extractFields(Row row) {
        List<String> fields = new ArrayList<>();
        
        // Get the actual number of cells in the row (up to last cell)
        int lastCellNum = row.getLastCellNum();
        
        // If the row has fewer cells than expected, it's malformed
        if (lastCellNum < EXPECTED_COLUMN_COUNT) {
            // Return only the cells that exist
            for (int i = 0; i < lastCellNum; i++) {
                Cell cell = row.getCell(i);
                String value = getCellValueAsString(cell);
                fields.add(value);
            }
            return fields;
        }
        
        // Extract all expected fields
        for (int i = 0; i < EXPECTED_COLUMN_COUNT; i++) {
            Cell cell = row.getCell(i);
            String value = getCellValueAsString(cell);
            fields.add(value);
        }
        
        return fields;
    }
    
    /**
     * Converts a cell value to a string, handling different cell types.
     * 
     * Handles:
     * - STRING cells: returns the string value
     * - NUMERIC cells: returns the numeric value as a string (without scientific notation)
     * - BOOLEAN cells: returns "true" or "false"
     * - FORMULA cells: evaluates the formula and returns the result
     * - BLANK/null cells: returns empty string
     * 
     * @param cell The cell to convert
     * @return The cell value as a string, or empty string if cell is null/blank
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
                
            case NUMERIC:
                // Check if it's a date (dates are stored as numeric in Excel)
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                // Return numeric value without scientific notation
                double numericValue = cell.getNumericCellValue();
                // If it's a whole number, return without decimal point
                if (numericValue == Math.floor(numericValue)) {
                    return String.valueOf((long) numericValue);
                }
                return String.valueOf(numericValue);
                
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
                
            case FORMULA:
                // Evaluate formula and return result
                try {
                    return getCellValueAsString(evaluateFormula(cell));
                } catch (Exception e) {
                    // If formula evaluation fails, return the formula string
                    return cell.getCellFormula();
                }
                
            case BLANK:
            case _NONE:
            case ERROR:
            default:
                return "";
        }
    }
    
    /**
     * Evaluates a formula cell and returns a cell with the result.
     * 
     * @param cell The formula cell to evaluate
     * @return A cell containing the evaluated result
     */
    private Cell evaluateFormula(Cell cell) {
        FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
        CellValue cellValue = evaluator.evaluate(cell);
        
        // Create a temporary cell to hold the evaluated value
        // We'll use the cell type from the evaluation result
        switch (cellValue.getCellType()) {
            case STRING:
                cell.setCellValue(cellValue.getStringValue());
                break;
            case NUMERIC:
                cell.setCellValue(cellValue.getNumberValue());
                break;
            case BOOLEAN:
                cell.setCellValue(cellValue.getBooleanValue());
                break;
            default:
                break;
        }
        
        return cell;
    }
}
