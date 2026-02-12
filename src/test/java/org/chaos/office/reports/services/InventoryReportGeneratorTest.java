package org.chaos.office.reports.services;

import org.chaos.office.reports.models.InventoryReportData;
import org.chaos.office.reports.models.PartInventoryItem;
import org.chaos.office.reports.models.StockStatus;
import org.chaos.office.service.PartService;
import org.chaos.office.util.LocaleManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for InventoryReportGenerator.
 * Tests the generator with real PartService instance.
 */
class InventoryReportGeneratorTest {
    
    private PartService partService;
    private InventoryReportGenerator generator;
    
    @BeforeEach
    void setUp() {
        // Set locale to English for consistent test results
        LocaleManager.setLocale(LocaleManager.LOCALE_ENGLISH);
        partService = new PartService();
        generator = new InventoryReportGenerator(partService);
    }
    
    @Test
    void testGenerateInventoryReport_WithDefaultThreshold() {
        // Arrange
        int stockThreshold = 10;
        
        // Act
        InventoryReportData report = generator.generate(stockThreshold);
        
        // Assert
        assertNotNull(report);
        assertEquals(stockThreshold, report.getStockThreshold());
        assertEquals("Inventory Report", report.getReportTitle());
        
        // Verify all fields are initialized
        assertNotNull(report.getTotalInventoryValue());
        assertNotNull(report.getPartsByCategory());
        assertNotNull(report.getLowStockParts());
        assertNotNull(report.getOutOfStockParts());
        
        // Total inventory value should be non-negative
        assertTrue(report.getTotalInventoryValue().compareTo(BigDecimal.ZERO) >= 0);
    }
    
    @Test
    void testGenerateInventoryReport_CustomThreshold() {
        // Arrange
        int customThreshold = 20;
        
        // Act
        InventoryReportData report = generator.generate(customThreshold);
        
        // Assert
        assertNotNull(report);
        assertEquals(customThreshold, report.getStockThreshold());
        
        // Verify parameters contain threshold
        Map<String, String> params = report.getParameters();
        assertTrue(params.containsKey("Stock Threshold"));
        assertEquals(customThreshold + " units", params.get("Stock Threshold"));
    }
    
    @Test
    void testGenerateInventoryReport_PartsByCategory() {
        // Arrange
        int stockThreshold = 10;
        
        // Act
        InventoryReportData report = generator.generate(stockThreshold);
        
        // Assert
        Map<String, List<PartInventoryItem>> partsByCategory = report.getPartsByCategory();
        assertNotNull(partsByCategory);
        
        // If there are parts, verify they are grouped by category
        if (!partsByCategory.isEmpty()) {
            for (Map.Entry<String, List<PartInventoryItem>> entry : partsByCategory.entrySet()) {
                String category = entry.getKey();
                List<PartInventoryItem> parts = entry.getValue();
                
                assertNotNull(category);
                assertNotNull(parts);
                assertFalse(parts.isEmpty());
                
                // Verify all parts in this group have the same category
                for (PartInventoryItem part : parts) {
                    assertEquals(category, part.getCategory());
                }
                
                // Verify parts are sorted alphabetically within category
                for (int i = 0; i < parts.size() - 1; i++) {
                    String currentName = parts.get(i).getPartName();
                    String nextName = parts.get(i + 1).getPartName();
                    assertTrue(currentName.compareTo(nextName) <= 0, 
                            "Parts should be sorted alphabetically: " + currentName + " vs " + nextName);
                }
            }
        }
    }
    
    @Test
    void testGenerateInventoryReport_StockValueCalculation() {
        // Arrange
        int stockThreshold = 10;
        
        // Act
        InventoryReportData report = generator.generate(stockThreshold);
        
        // Assert
        Map<String, List<PartInventoryItem>> partsByCategory = report.getPartsByCategory();
        
        // Verify stock value calculation for each part
        for (List<PartInventoryItem> parts : partsByCategory.values()) {
            for (PartInventoryItem part : parts) {
                // Stock value should equal quantity * price
                BigDecimal expectedValue = part.getPrice()
                        .multiply(BigDecimal.valueOf(part.getQuantity()))
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                assertEquals(expectedValue, part.getStockValue(), 
                        "Stock value mismatch for part: " + part.getPartName());
            }
        }
    }
    
    @Test
    void testGenerateInventoryReport_StockStatusClassification() {
        // Arrange
        int stockThreshold = 10;
        
        // Act
        InventoryReportData report = generator.generate(stockThreshold);
        
        // Assert
        List<PartInventoryItem> lowStockParts = report.getLowStockParts();
        List<PartInventoryItem> outOfStockParts = report.getOutOfStockParts();
        
        // Verify low stock parts have quantity > 0 and <= threshold
        for (PartInventoryItem part : lowStockParts) {
            assertTrue(part.getQuantity() > 0, 
                    "Low stock part should have quantity > 0: " + part.getPartName());
            assertTrue(part.getQuantity() <= stockThreshold, 
                    "Low stock part should have quantity <= threshold: " + part.getPartName());
            assertEquals(StockStatus.LOW_STOCK, part.getStatus());
        }
        
        // Verify out of stock parts have quantity == 0
        for (PartInventoryItem part : outOfStockParts) {
            assertEquals(0, part.getQuantity(), 
                    "Out of stock part should have quantity == 0: " + part.getPartName());
            assertEquals(StockStatus.OUT_OF_STOCK, part.getStatus());
        }
    }
    
    @Test
    void testGenerateInventoryReport_TotalValueCalculation() {
        // Arrange
        int stockThreshold = 10;
        
        // Act
        InventoryReportData report = generator.generate(stockThreshold);
        
        // Assert
        BigDecimal totalValue = report.getTotalInventoryValue();
        Map<String, List<PartInventoryItem>> partsByCategory = report.getPartsByCategory();
        
        // Calculate expected total by summing all part stock values
        BigDecimal expectedTotal = BigDecimal.ZERO;
        for (List<PartInventoryItem> parts : partsByCategory.values()) {
            for (PartInventoryItem part : parts) {
                expectedTotal = expectedTotal.add(part.getStockValue());
            }
        }
        expectedTotal = expectedTotal.setScale(2, BigDecimal.ROUND_HALF_UP);
        
        assertEquals(expectedTotal, totalValue, 
                "Total inventory value should equal sum of all part stock values");
    }
    
    @Test
    void testGenerateInventoryReport_InitializesAllFields() {
        // Arrange
        int stockThreshold = 15;
        
        // Act
        InventoryReportData report = generator.generate(stockThreshold);
        
        // Assert - verify all fields are initialized (not null)
        assertNotNull(report);
        assertNotNull(report.getReportTitle());
        assertNotNull(report.getParameters());
        assertNotNull(report.getTotalInventoryValue());
        assertNotNull(report.getPartsByCategory());
        assertNotNull(report.getLowStockParts());
        assertNotNull(report.getOutOfStockParts());
    }
}
