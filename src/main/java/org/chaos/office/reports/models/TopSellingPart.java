package org.chaos.office.reports.models;

import java.math.BigDecimal;

/**
 * Helper class representing a top-selling part in a sales report.
 * Contains information about part name, quantity sold, and revenue generated.
 */
public class TopSellingPart {
    private String partName;
    private int quantitySold;
    private BigDecimal revenue;
    
    /**
     * Default constructor
     */
    public TopSellingPart() {
        this.revenue = BigDecimal.ZERO;
    }
    
    /**
     * Constructor with all fields
     * @param partName The name of the part
     * @param quantitySold The total quantity sold
     * @param revenue The total revenue generated from this part
     */
    public TopSellingPart(String partName, int quantitySold, BigDecimal revenue) {
        this.partName = partName;
        this.quantitySold = quantitySold;
        this.revenue = revenue != null ? revenue : BigDecimal.ZERO;
    }
    
    // Getters and setters
    
    public String getPartName() {
        return partName;
    }
    
    public void setPartName(String partName) {
        this.partName = partName;
    }
    
    public int getQuantitySold() {
        return quantitySold;
    }
    
    public void setQuantitySold(int quantitySold) {
        this.quantitySold = quantitySold;
    }
    
    public BigDecimal getRevenue() {
        return revenue;
    }
    
    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }
    
    @Override
    public String toString() {
        return "TopSellingPart{" +
                "partName='" + partName + '\'' +
                ", quantitySold=" + quantitySold +
                ", revenue=" + revenue +
                '}';
    }
}
