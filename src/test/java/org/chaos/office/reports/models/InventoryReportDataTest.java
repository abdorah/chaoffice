package org.chaos.office.reports.models;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for the InventoryReportData class.
 * Tests initialization, getters/setters, and report-specific functionality.
 */
class InventoryReportDataTest {
    
    private InventoryReportData inventoryReportData;
    
    @BeforeEach
    void setUp() {
        inventoryReportData = new InventoryReportData();
    }
    
    @Test
    void testDefaultConstructor() {
        assertNotNull(inventoryReportData);
        assertEquals("Inventory Report", inventoryReportData.getReportType());
        assertNotNull(inventoryReportData.getGeneratedAt());
        assertEquals("ChaOffice Parts Inventory", inventoryReportData.getCompanyName());
        
        // Verify default values
        assertEquals(10, inventoryReportData.getStockThreshold());
        assertEquals(BigDecimal.ZERO, inventoryReportData.getTotalInventoryValue());
        assertNotNull(inventoryReportData.getPartsByCategory());
        assertTrue(inventoryReportData.getPartsByCategory().isEmpty());
        assertNotNull(inventoryReportData.getLowStockParts());
        assertTrue(inventoryReportData.getLowStockParts().isEmpty());
        assertNotNull(inventoryReportData.getOutOfStockParts());
        assertTrue(inventoryReportData.getOutOfStockParts().isEmpty());
    }
    
    @Test
    void testConstructorWithStockThreshold() {
        int customThreshold = 20;
        InventoryReportData report = new InventoryReportData(customThreshold);
        
        assertEquals(customThreshold, report.getStockThreshold());
        assertEquals("Inventory Report", report.getReportType());
        
        // Verify other fields are still initialized
        assertEquals(BigDecimal.ZERO, report.getTotalInventoryValue());
        assertNotNull(report.getPartsByCategory());
        assertNotNull(report.getLowStockParts());
        assertNotNull(report.getOutOfStockParts());
    }
    
    @Test
    void testGetReportTitle() {
        assertEquals("Inventory Report", inventoryReportData.getReportTitle());
    }
    
    @Test
    void testGetParameters() {
        inventoryReportData.setStockThreshold(15);
        
        Map<String, String> params = inventoryReportData.getParameters();
        
        assertNotNull(params);
        assertEquals(1, params.size());
        assertEquals("15 units", params.get("Stock Threshold"));
    }
    
    @Test
    void testGetParametersWithDefaultThreshold() {
        Map<String, String> params = inventoryReportData.getParameters();
        
        assertNotNull(params);
        assertEquals(1, params.size());
        assertEquals("10 units", params.get("Stock Threshold"));
    }
    
    @Test
    void testSetAndGetStockThreshold() {
        inventoryReportData.setStockThreshold(25);
        
        assertEquals(25, inventoryReportData.getStockThreshold());
    }
    
    @Test
    void testSetAndGetTotalInventoryValue() {
        BigDecimal totalValue = new BigDecimal("50000.00");
        inventoryReportData.setTotalInventoryValue(totalValue);
        
        assertEquals(totalValue, inventoryReportData.getTotalInventoryValue());
    }
    
    @Test
    void testSetAndGetPartsByCategory() {
        Map<String, List<PartInventoryItem>> partsByCategory = new HashMap<>();
        
        List<PartInventoryItem> brakesParts = new ArrayList<>();
        brakesParts.add(new PartInventoryItem("Brake Pad", "Brakes", 50, 
                                              new BigDecimal("25.00"), 
                                              new BigDecimal("1250.00"), 
                                              StockStatus.NORMAL));
        
        List<PartInventoryItem> filtersParts = new ArrayList<>();
        filtersParts.add(new PartInventoryItem("Oil Filter", "Filters", 30, 
                                               new BigDecimal("10.00"), 
                                               new BigDecimal("300.00"), 
                                               StockStatus.NORMAL));
        
        partsByCategory.put("Brakes", brakesParts);
        partsByCategory.put("Filters", filtersParts);
        
        inventoryReportData.setPartsByCategory(partsByCategory);
        
        Map<String, List<PartInventoryItem>> result = inventoryReportData.getPartsByCategory();
        assertEquals(2, result.size());
        assertTrue(result.containsKey("Brakes"));
        assertTrue(result.containsKey("Filters"));
        assertEquals(1, result.get("Brakes").size());
        assertEquals(1, result.get("Filters").size());
    }
    
    @Test
    void testSetAndGetLowStockParts() {
        List<PartInventoryItem> lowStockParts = new ArrayList<>();
        lowStockParts.add(new PartInventoryItem("Part A", "Category A", 5, 
                                                new BigDecimal("20.00"), 
                                                new BigDecimal("100.00"), 
                                                StockStatus.LOW_STOCK));
        lowStockParts.add(new PartInventoryItem("Part B", "Category B", 8, 
                                                new BigDecimal("15.00"), 
                                                new BigDecimal("120.00"), 
                                                StockStatus.LOW_STOCK));
        
        inventoryReportData.setLowStockParts(lowStockParts);
        
        List<PartInventoryItem> result = inventoryReportData.getLowStockParts();
        assertEquals(2, result.size());
        assertEquals("Part A", result.get(0).getPartName());
        assertEquals(StockStatus.LOW_STOCK, result.get(0).getStatus());
    }
    
    @Test
    void testSetAndGetOutOfStockParts() {
        List<PartInventoryItem> outOfStockParts = new ArrayList<>();
        outOfStockParts.add(new PartInventoryItem("Part C", "Category C", 0, 
                                                  new BigDecimal("30.00"), 
                                                  BigDecimal.ZERO, 
                                                  StockStatus.OUT_OF_STOCK));
        
        inventoryReportData.setOutOfStockParts(outOfStockParts);
        
        List<PartInventoryItem> result = inventoryReportData.getOutOfStockParts();
        assertEquals(1, result.size());
        assertEquals("Part C", result.get(0).getPartName());
        assertEquals(0, result.get(0).getQuantity());
        assertEquals(StockStatus.OUT_OF_STOCK, result.get(0).getStatus());
    }
    
    @Test
    void testToString() {
        inventoryReportData.setStockThreshold(15);
        inventoryReportData.setTotalInventoryValue(new BigDecimal("25000.00"));
        
        String result = inventoryReportData.toString();
        
        assertNotNull(result);
        assertTrue(result.contains("InventoryReportData"));
        assertTrue(result.contains("stockThreshold=15"));
        assertTrue(result.contains("25000.00"));
    }
    
    @Test
    void testEmptyInventoryScenario() {
        // Verify that a report with no inventory has empty collections
        assertTrue(inventoryReportData.getPartsByCategory().isEmpty());
        assertTrue(inventoryReportData.getLowStockParts().isEmpty());
        assertTrue(inventoryReportData.getOutOfStockParts().isEmpty());
        assertEquals(BigDecimal.ZERO, inventoryReportData.getTotalInventoryValue());
    }
    
    @Test
    void testSingleCategoryInventory() {
        Map<String, List<PartInventoryItem>> partsByCategory = new HashMap<>();
        
        List<PartInventoryItem> parts = new ArrayList<>();
        parts.add(new PartInventoryItem("Part A", "Category A", 100, 
                                        new BigDecimal("10.00"), 
                                        new BigDecimal("1000.00"), 
                                        StockStatus.NORMAL));
        
        partsByCategory.put("Category A", parts);
        inventoryReportData.setPartsByCategory(partsByCategory);
        
        assertEquals(1, inventoryReportData.getPartsByCategory().size());
        assertTrue(inventoryReportData.getPartsByCategory().containsKey("Category A"));
    }
    
    @Test
    void testMultipleCategoriesInventory() {
        Map<String, List<PartInventoryItem>> partsByCategory = new HashMap<>();
        
        for (int i = 1; i <= 5; i++) {
            List<PartInventoryItem> parts = new ArrayList<>();
            parts.add(new PartInventoryItem("Part " + i, "Category " + i, 50, 
                                            new BigDecimal("10.00"), 
                                            new BigDecimal("500.00"), 
                                            StockStatus.NORMAL));
            partsByCategory.put("Category " + i, parts);
        }
        
        inventoryReportData.setPartsByCategory(partsByCategory);
        
        assertEquals(5, inventoryReportData.getPartsByCategory().size());
    }
    
    @Test
    void testAllPartsOutOfStock() {
        List<PartInventoryItem> outOfStockParts = new ArrayList<>();
        
        for (int i = 1; i <= 3; i++) {
            outOfStockParts.add(new PartInventoryItem("Part " + i, "Category A", 0, 
                                                      new BigDecimal("10.00"), 
                                                      BigDecimal.ZERO, 
                                                      StockStatus.OUT_OF_STOCK));
        }
        
        inventoryReportData.setOutOfStockParts(outOfStockParts);
        
        assertEquals(3, inventoryReportData.getOutOfStockParts().size());
        assertTrue(inventoryReportData.getLowStockParts().isEmpty());
    }
    
    @Test
    void testAllPartsLowStock() {
        List<PartInventoryItem> lowStockParts = new ArrayList<>();
        
        for (int i = 1; i <= 4; i++) {
            lowStockParts.add(new PartInventoryItem("Part " + i, "Category A", 5, 
                                                    new BigDecimal("10.00"), 
                                                    new BigDecimal("50.00"), 
                                                    StockStatus.LOW_STOCK));
        }
        
        inventoryReportData.setLowStockParts(lowStockParts);
        
        assertEquals(4, inventoryReportData.getLowStockParts().size());
        assertTrue(inventoryReportData.getOutOfStockParts().isEmpty());
    }
    
    @Test
    void testMixedStockStatuses() {
        // Set up parts by category with mixed statuses
        Map<String, List<PartInventoryItem>> partsByCategory = new HashMap<>();
        List<PartInventoryItem> parts = new ArrayList<>();
        
        parts.add(new PartInventoryItem("Part A", "Category A", 100, 
                                        new BigDecimal("10.00"), 
                                        new BigDecimal("1000.00"), 
                                        StockStatus.NORMAL));
        parts.add(new PartInventoryItem("Part B", "Category A", 5, 
                                        new BigDecimal("10.00"), 
                                        new BigDecimal("50.00"), 
                                        StockStatus.LOW_STOCK));
        parts.add(new PartInventoryItem("Part C", "Category A", 0, 
                                        new BigDecimal("10.00"), 
                                        BigDecimal.ZERO, 
                                        StockStatus.OUT_OF_STOCK));
        
        partsByCategory.put("Category A", parts);
        inventoryReportData.setPartsByCategory(partsByCategory);
        
        // Set up low stock and out of stock lists
        List<PartInventoryItem> lowStock = new ArrayList<>();
        lowStock.add(parts.get(1));
        inventoryReportData.setLowStockParts(lowStock);
        
        List<PartInventoryItem> outOfStock = new ArrayList<>();
        outOfStock.add(parts.get(2));
        inventoryReportData.setOutOfStockParts(outOfStock);
        
        assertEquals(3, inventoryReportData.getPartsByCategory().get("Category A").size());
        assertEquals(1, inventoryReportData.getLowStockParts().size());
        assertEquals(1, inventoryReportData.getOutOfStockParts().size());
    }
    
    @Test
    void testCustomStockThresholdInParameters() {
        inventoryReportData.setStockThreshold(5);
        
        Map<String, String> params = inventoryReportData.getParameters();
        assertEquals("5 units", params.get("Stock Threshold"));
    }
    
    @Test
    void testZeroStockThreshold() {
        inventoryReportData.setStockThreshold(0);
        
        assertEquals(0, inventoryReportData.getStockThreshold());
        
        Map<String, String> params = inventoryReportData.getParameters();
        assertEquals("0 units", params.get("Stock Threshold"));
    }
    
    @Test
    void testLargeInventoryValue() {
        BigDecimal largeValue = new BigDecimal("999999.99");
        inventoryReportData.setTotalInventoryValue(largeValue);
        
        assertEquals(largeValue, inventoryReportData.getTotalInventoryValue());
    }
    
    @Test
    void testEmptyCategory() {
        Map<String, List<PartInventoryItem>> partsByCategory = new HashMap<>();
        partsByCategory.put("Empty Category", new ArrayList<>());
        
        inventoryReportData.setPartsByCategory(partsByCategory);
        
        assertTrue(inventoryReportData.getPartsByCategory().containsKey("Empty Category"));
        assertTrue(inventoryReportData.getPartsByCategory().get("Empty Category").isEmpty());
    }
}
