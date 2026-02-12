package org.chaos.office.reports.models;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Inventory report data model containing all metrics and analysis for an inventory report.
 * Extends ReportData with inventory-specific fields including stock threshold,
 * total inventory value, parts grouped by category, and stock alerts.
 */
public class InventoryReportData extends ReportData {
    private int stockThreshold;
    private BigDecimal totalInventoryValue;
    private Map<String, List<PartInventoryItem>> partsByCategory;
    private List<PartInventoryItem> lowStockParts;
    private List<PartInventoryItem> outOfStockParts;
    
    /**
     * Default constructor initializes collections and BigDecimal fields to zero
     */
    public InventoryReportData() {
        super("Inventory Report");
        this.stockThreshold = 10; // Default threshold
        this.totalInventoryValue = BigDecimal.ZERO;
        this.partsByCategory = new HashMap<>();
        this.lowStockParts = new ArrayList<>();
        this.outOfStockParts = new ArrayList<>();
    }
    
    /**
     * Constructor with stock threshold
     * @param stockThreshold The minimum quantity level that triggers a low stock alert
     */
    public InventoryReportData(int stockThreshold) {
        this();
        this.stockThreshold = stockThreshold;
    }
    
    @Override
    public String getReportTitle() {
        return org.chaos.office.util.LocaleManager.getString("report.inventory.title");
    }
    
    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put(org.chaos.office.util.LocaleManager.getString("report.stock.threshold"), stockThreshold + " " + org.chaos.office.util.LocaleManager.getString("report.units"));
        return params;
    }
    
    // Getters and setters
    
    public int getStockThreshold() {
        return stockThreshold;
    }
    
    public void setStockThreshold(int stockThreshold) {
        this.stockThreshold = stockThreshold;
    }
    
    public BigDecimal getTotalInventoryValue() {
        return totalInventoryValue;
    }
    
    public void setTotalInventoryValue(BigDecimal totalInventoryValue) {
        this.totalInventoryValue = totalInventoryValue;
    }
    
    public Map<String, List<PartInventoryItem>> getPartsByCategory() {
        return partsByCategory;
    }
    
    public void setPartsByCategory(Map<String, List<PartInventoryItem>> partsByCategory) {
        this.partsByCategory = partsByCategory;
    }
    
    public List<PartInventoryItem> getLowStockParts() {
        return lowStockParts;
    }
    
    public void setLowStockParts(List<PartInventoryItem> lowStockParts) {
        this.lowStockParts = lowStockParts;
    }
    
    public List<PartInventoryItem> getOutOfStockParts() {
        return outOfStockParts;
    }
    
    public void setOutOfStockParts(List<PartInventoryItem> outOfStockParts) {
        this.outOfStockParts = outOfStockParts;
    }
    
    @Override
    public String toString() {
        return "InventoryReportData{" +
                "stockThreshold=" + stockThreshold +
                ", totalInventoryValue=" + totalInventoryValue +
                ", partsByCategory=" + partsByCategory +
                ", lowStockParts=" + lowStockParts +
                ", outOfStockParts=" + outOfStockParts +
                '}';
    }
}
