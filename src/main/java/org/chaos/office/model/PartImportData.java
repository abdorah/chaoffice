package org.chaos.office.model;

/**
 * Intermediate representation of part data from imported files (CSV or Excel).
 * This model holds raw string data before validation and conversion to Part objects.
 * Used during the bulk import process to track row numbers for error reporting.
 * 
 * Requirements: 3.1, 3.2
 */
public class PartImportData {
    private int rowNumber;
    private String name;
    private String maker;
    private String description;
    private String priceStr;
    private String quantityStr;
    private String categoryName;
    
    /**
     * Default constructor
     */
    public PartImportData() {
    }
    
    /**
     * Constructor with all fields
     * 
     * @param rowNumber The row number in the source file (1-indexed, for error reporting)
     * @param name The part name
     * @param maker The maker/manufacturer name
     * @param description The part description
     * @param priceStr The price as a string (to be parsed and validated)
     * @param quantityStr The quantity as a string (to be parsed and validated)
     * @param categoryName The category name (to be resolved to a Category object)
     */
    public PartImportData(int rowNumber, String name, String maker, String description, 
                          String priceStr, String quantityStr, String categoryName) {
        this.rowNumber = rowNumber;
        this.name = name;
        this.maker = maker;
        this.description = description;
        this.priceStr = priceStr;
        this.quantityStr = quantityStr;
        this.categoryName = categoryName;
    }
    
    // Getters and setters
    
    public int getRowNumber() {
        return rowNumber;
    }
    
    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getMaker() {
        return maker;
    }
    
    public void setMaker(String maker) {
        this.maker = maker;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getPriceStr() {
        return priceStr;
    }
    
    public void setPriceStr(String priceStr) {
        this.priceStr = priceStr;
    }
    
    public String getQuantityStr() {
        return quantityStr;
    }
    
    public void setQuantityStr(String quantityStr) {
        this.quantityStr = quantityStr;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    @Override
    public String toString() {
        return "PartImportData{" +
                "rowNumber=" + rowNumber +
                ", name='" + name + '\'' +
                ", maker='" + maker + '\'' +
                ", description='" + description + '\'' +
                ", priceStr='" + priceStr + '\'' +
                ", quantityStr='" + quantityStr + '\'' +
                ", categoryName='" + categoryName + '\'' +
                '}';
    }
}
