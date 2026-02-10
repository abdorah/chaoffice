package org.chaos.office.reports.models;

import java.math.BigDecimal;

/**
 * Helper class representing a part in an inventory report.
 * Contains information about part name, category, quantity, price, stock value, and status.
 */
public class PartInventoryItem {
    private String partName;
    private String category;
    private int quantity;
    private BigDecimal price;
    private BigDecimal stockValue;
    private StockStatus status;
    
    /**
     * Default constructor
     */
    public PartInventoryItem() {
        this.price = BigDecimal.ZERO;
        this.stockValue = BigDecimal.ZERO;
        this.status = StockStatus.NORMAL;
    }
    
    /**
     * Constructor with all fields
     * @param partName The name of the part
     * @param category The category of the part
     * @param quantity The current quantity in stock
     * @param price The price per unit
     * @param stockValue The total stock value (quantity * price)
     * @param status The stock status (NORMAL, LOW_STOCK, OUT_OF_STOCK)
     */
    public PartInventoryItem(String partName, String category, int quantity, 
                            BigDecimal price, BigDecimal stockValue, StockStatus status) {
        this.partName = partName;
        this.category = category;
        this.quantity = quantity;
        this.price = price != null ? price : BigDecimal.ZERO;
        this.stockValue = stockValue != null ? stockValue : BigDecimal.ZERO;
        this.status = status != null ? status : StockStatus.NORMAL;
    }
    
    // Getters and setters
    
    public String getPartName() {
        return partName;
    }
    
    public void setPartName(String partName) {
        this.partName = partName;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public BigDecimal getStockValue() {
        return stockValue;
    }
    
    public void setStockValue(BigDecimal stockValue) {
        this.stockValue = stockValue;
    }
    
    public StockStatus getStatus() {
        return status;
    }
    
    public void setStatus(StockStatus status) {
        this.status = status;
    }
    
    @Override
    public String toString() {
        return "PartInventoryItem{" +
                "partName='" + partName + '\'' +
                ", category='" + category + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", stockValue=" + stockValue +
                ", status=" + status +
                '}';
    }
}
