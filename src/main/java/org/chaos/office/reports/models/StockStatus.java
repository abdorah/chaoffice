package org.chaos.office.reports.models;

/**
 * Stock status enumeration for inventory reports
 */
public enum StockStatus {
    NORMAL("Normal"),
    LOW_STOCK("Low Stock"),
    OUT_OF_STOCK("Out of Stock");
    
    private final String displayName;
    
    StockStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
