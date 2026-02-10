package org.chaos.office.reports.models;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * Unit tests for the PartInventoryItem class.
 * Tests initialization, getters/setters, and field handling.
 */
class PartInventoryItemTest {
    
    private PartInventoryItem partInventoryItem;
    
    @BeforeEach
    void setUp() {
        partInventoryItem = new PartInventoryItem();
    }
    
    @Test
    void testDefaultConstructor() {
        assertNotNull(partInventoryItem);
        assertNull(partInventoryItem.getPartName());
        assertNull(partInventoryItem.getCategory());
        assertEquals(0, partInventoryItem.getQuantity());
        assertEquals(BigDecimal.ZERO, partInventoryItem.getPrice());
        assertEquals(BigDecimal.ZERO, partInventoryItem.getStockValue());
        assertEquals(StockStatus.NORMAL, partInventoryItem.getStatus());
    }
    
    @Test
    void testConstructorWithAllFields() {
        String partName = "Brake Pad";
        String category = "Brakes";
        int quantity = 50;
        BigDecimal price = new BigDecimal("25.99");
        BigDecimal stockValue = new BigDecimal("1299.50");
        StockStatus status = StockStatus.NORMAL;
        
        PartInventoryItem item = new PartInventoryItem(partName, category, quantity, 
                                                        price, stockValue, status);
        
        assertEquals(partName, item.getPartName());
        assertEquals(category, item.getCategory());
        assertEquals(quantity, item.getQuantity());
        assertEquals(price, item.getPrice());
        assertEquals(stockValue, item.getStockValue());
        assertEquals(status, item.getStatus());
    }
    
    @Test
    void testConstructorWithNullBigDecimalFields() {
        PartInventoryItem item = new PartInventoryItem("Part A", "Category A", 10, 
                                                        null, null, StockStatus.NORMAL);
        
        assertEquals(BigDecimal.ZERO, item.getPrice());
        assertEquals(BigDecimal.ZERO, item.getStockValue());
    }
    
    @Test
    void testConstructorWithNullStatus() {
        PartInventoryItem item = new PartInventoryItem("Part A", "Category A", 10, 
                                                        BigDecimal.TEN, BigDecimal.TEN, null);
        
        assertEquals(StockStatus.NORMAL, item.getStatus());
    }
    
    @Test
    void testSetAndGetPartName() {
        String partName = "Oil Filter";
        partInventoryItem.setPartName(partName);
        
        assertEquals(partName, partInventoryItem.getPartName());
    }
    
    @Test
    void testSetAndGetCategory() {
        String category = "Filters";
        partInventoryItem.setCategory(category);
        
        assertEquals(category, partInventoryItem.getCategory());
    }
    
    @Test
    void testSetAndGetQuantity() {
        partInventoryItem.setQuantity(100);
        
        assertEquals(100, partInventoryItem.getQuantity());
    }
    
    @Test
    void testSetAndGetPrice() {
        BigDecimal price = new BigDecimal("49.99");
        partInventoryItem.setPrice(price);
        
        assertEquals(price, partInventoryItem.getPrice());
    }
    
    @Test
    void testSetAndGetStockValue() {
        BigDecimal stockValue = new BigDecimal("4999.00");
        partInventoryItem.setStockValue(stockValue);
        
        assertEquals(stockValue, partInventoryItem.getStockValue());
    }
    
    @Test
    void testSetAndGetStatus() {
        partInventoryItem.setStatus(StockStatus.LOW_STOCK);
        
        assertEquals(StockStatus.LOW_STOCK, partInventoryItem.getStatus());
    }
    
    @Test
    void testLowStockStatus() {
        PartInventoryItem item = new PartInventoryItem("Part A", "Category A", 5, 
                                                        new BigDecimal("10.00"), 
                                                        new BigDecimal("50.00"), 
                                                        StockStatus.LOW_STOCK);
        
        assertEquals(StockStatus.LOW_STOCK, item.getStatus());
        assertTrue(item.getQuantity() > 0);
    }
    
    @Test
    void testOutOfStockStatus() {
        PartInventoryItem item = new PartInventoryItem("Part B", "Category B", 0, 
                                                        new BigDecimal("15.00"), 
                                                        BigDecimal.ZERO, 
                                                        StockStatus.OUT_OF_STOCK);
        
        assertEquals(StockStatus.OUT_OF_STOCK, item.getStatus());
        assertEquals(0, item.getQuantity());
        assertEquals(BigDecimal.ZERO, item.getStockValue());
    }
    
    @Test
    void testNormalStockStatus() {
        PartInventoryItem item = new PartInventoryItem("Part C", "Category C", 100, 
                                                        new BigDecimal("20.00"), 
                                                        new BigDecimal("2000.00"), 
                                                        StockStatus.NORMAL);
        
        assertEquals(StockStatus.NORMAL, item.getStatus());
        assertTrue(item.getQuantity() > 0);
    }
    
    @Test
    void testToString() {
        partInventoryItem.setPartName("Spark Plug");
        partInventoryItem.setCategory("Ignition");
        partInventoryItem.setQuantity(25);
        partInventoryItem.setPrice(new BigDecimal("8.99"));
        partInventoryItem.setStockValue(new BigDecimal("224.75"));
        partInventoryItem.setStatus(StockStatus.NORMAL);
        
        String result = partInventoryItem.toString();
        
        assertNotNull(result);
        assertTrue(result.contains("PartInventoryItem"));
        assertTrue(result.contains("Spark Plug"));
        assertTrue(result.contains("Ignition"));
        assertTrue(result.contains("quantity=25"));
        assertTrue(result.contains("8.99"));
        assertTrue(result.contains("224.75"));
        assertTrue(result.contains("NORMAL"));
    }
    
    @Test
    void testZeroQuantityWithNormalStatus() {
        // Edge case: quantity is zero but status is not OUT_OF_STOCK
        // This tests that the class allows this state (validation happens elsewhere)
        partInventoryItem.setQuantity(0);
        partInventoryItem.setStatus(StockStatus.NORMAL);
        
        assertEquals(0, partInventoryItem.getQuantity());
        assertEquals(StockStatus.NORMAL, partInventoryItem.getStatus());
    }
    
    @Test
    void testNegativeQuantity() {
        // Edge case: negative quantity (validation happens elsewhere)
        partInventoryItem.setQuantity(-5);
        
        assertEquals(-5, partInventoryItem.getQuantity());
    }
    
    @Test
    void testNullPartName() {
        partInventoryItem.setPartName(null);
        
        assertNull(partInventoryItem.getPartName());
    }
    
    @Test
    void testNullCategory() {
        partInventoryItem.setCategory(null);
        
        assertNull(partInventoryItem.getCategory());
    }
    
    @Test
    void testStockValueCalculation() {
        // Test that stock value can be set independently
        // (actual calculation happens in the generator)
        int quantity = 50;
        BigDecimal price = new BigDecimal("10.00");
        BigDecimal expectedStockValue = new BigDecimal("500.00");
        
        partInventoryItem.setQuantity(quantity);
        partInventoryItem.setPrice(price);
        partInventoryItem.setStockValue(expectedStockValue);
        
        assertEquals(expectedStockValue, partInventoryItem.getStockValue());
    }
}
