package org.chaos.office.reports.services;

import org.chaos.office.model.Part;
import org.chaos.office.reports.models.InventoryReportData;
import org.chaos.office.reports.models.PartInventoryItem;
import org.chaos.office.reports.models.StockStatus;
import org.chaos.office.service.PartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates inventory reports with stock levels, value calculations, and alerts.
 * Integrates with PartService to retrieve current inventory data.
 */
public class InventoryReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(InventoryReportGenerator.class);
    private final PartService partService;
    
    /**
     * Constructor with PartService dependency
     * @param partService Service for retrieving part data
     */
    public InventoryReportGenerator(PartService partService) {
        this.partService = partService;
    }
    
    /**
     * Generates a complete inventory report with the specified stock threshold.
     * 
     * @param stockThreshold The minimum quantity level that triggers a low stock alert
     * @return InventoryReportData containing all inventory metrics and analysis
     */
    public InventoryReportData generate(int stockThreshold) {
        logger.info("Generating inventory report with stock threshold: {}", stockThreshold);
        
        InventoryReportData reportData = new InventoryReportData(stockThreshold);
        
        // Retrieve all parts from database
        List<Part> allParts = partService.getAllParts();
        logger.info("Retrieved {} parts from database", allParts.size());
        
        // Convert parts to inventory items
        List<PartInventoryItem> inventoryItems = allParts.stream()
                .map(part -> convertToInventoryItem(part, stockThreshold))
                .collect(Collectors.toList());
        
        // Calculate total inventory value
        BigDecimal totalValue = calculateTotalInventoryValue(inventoryItems);
        reportData.setTotalInventoryValue(totalValue);
        
        // Group parts by category
        Map<String, List<PartInventoryItem>> partsByCategory = groupByCategory(inventoryItems);
        reportData.setPartsByCategory(partsByCategory);
        
        // Identify low stock and out of stock parts
        List<PartInventoryItem> lowStockParts = identifyLowStockParts(inventoryItems, stockThreshold);
        List<PartInventoryItem> outOfStockParts = identifyOutOfStockParts(inventoryItems);
        
        reportData.setLowStockParts(lowStockParts);
        reportData.setOutOfStockParts(outOfStockParts);
        
        logger.info("Inventory report generated: Total value={}, Low stock={}, Out of stock={}", 
                    totalValue, lowStockParts.size(), outOfStockParts.size());
        
        return reportData;
    }
    
    /**
     * Converts a Part to a PartInventoryItem with calculated stock value and status.
     * 
     * @param part The part to convert
     * @param stockThreshold The threshold for low stock classification
     * @return PartInventoryItem with all fields populated
     */
    private PartInventoryItem convertToInventoryItem(Part part, int stockThreshold) {
        BigDecimal price = BigDecimal.valueOf(part.getPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal stockValue = calculateStockValue(part);
        StockStatus status = determineStockStatus(part.getQuantity(), stockThreshold);
        
        return new PartInventoryItem(
                part.getName(),
                part.getCategory().getName(),
                part.getQuantity(),
                price,
                stockValue,
                status
        );
    }
    
    /**
     * Calculates the stock value for an individual part (quantity × price).
     * 
     * @param part The part to calculate stock value for
     * @return Stock value with 2 decimal places
     */
    private BigDecimal calculateStockValue(Part part) {
        BigDecimal quantity = BigDecimal.valueOf(part.getQuantity());
        BigDecimal price = BigDecimal.valueOf(part.getPrice());
        return quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculates the total inventory value by summing all part stock values.
     * 
     * @param inventoryItems List of inventory items
     * @return Total inventory value with 2 decimal places
     */
    private BigDecimal calculateTotalInventoryValue(List<PartInventoryItem> inventoryItems) {
        return inventoryItems.stream()
                .map(PartInventoryItem::getStockValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Groups parts by category and sorts alphabetically within each category.
     * 
     * @param inventoryItems List of inventory items
     * @return Map of category name to sorted list of parts
     */
    private Map<String, List<PartInventoryItem>> groupByCategory(List<PartInventoryItem> inventoryItems) {
        Map<String, List<PartInventoryItem>> grouped = inventoryItems.stream()
                .collect(Collectors.groupingBy(PartInventoryItem::getCategory));
        
        // Sort parts alphabetically within each category
        grouped.forEach((category, parts) -> 
                parts.sort(Comparator.comparing(PartInventoryItem::getPartName)));
        
        return grouped;
    }
    
    /**
     * Identifies parts with low stock (0 < quantity <= threshold).
     * 
     * @param inventoryItems List of inventory items
     * @param threshold The stock threshold
     * @return List of low stock parts
     */
    private List<PartInventoryItem> identifyLowStockParts(List<PartInventoryItem> inventoryItems, int threshold) {
        return inventoryItems.stream()
                .filter(item -> item.getQuantity() > 0 && item.getQuantity() <= threshold)
                .collect(Collectors.toList());
    }
    
    /**
     * Identifies parts that are out of stock (quantity == 0).
     * 
     * @param inventoryItems List of inventory items
     * @return List of out of stock parts
     */
    private List<PartInventoryItem> identifyOutOfStockParts(List<PartInventoryItem> inventoryItems) {
        return inventoryItems.stream()
                .filter(item -> item.getQuantity() == 0)
                .collect(Collectors.toList());
    }
    
    /**
     * Determines the stock status based on quantity and threshold.
     * 
     * @param quantity The current quantity
     * @param threshold The stock threshold
     * @return StockStatus (OUT_OF_STOCK, LOW_STOCK, or NORMAL)
     */
    private StockStatus determineStockStatus(int quantity, int threshold) {
        if (quantity == 0) {
            return StockStatus.OUT_OF_STOCK;
        } else if (quantity <= threshold) {
            return StockStatus.LOW_STOCK;
        } else {
            return StockStatus.NORMAL;
        }
    }
}
