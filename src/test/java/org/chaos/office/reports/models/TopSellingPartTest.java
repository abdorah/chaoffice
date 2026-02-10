package org.chaos.office.reports.models;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * Unit tests for the TopSellingPart class.
 * Tests initialization, getters/setters, and edge cases.
 */
class TopSellingPartTest {
    
    private TopSellingPart topSellingPart;
    
    @BeforeEach
    void setUp() {
        topSellingPart = new TopSellingPart();
    }
    
    @Test
    void testDefaultConstructor() {
        assertNotNull(topSellingPart);
        assertNull(topSellingPart.getPartName());
        assertEquals(0, topSellingPart.getQuantitySold());
        assertEquals(BigDecimal.ZERO, topSellingPart.getRevenue());
    }
    
    @Test
    void testConstructorWithAllFields() {
        String partName = "Engine Oil Filter";
        int quantitySold = 150;
        BigDecimal revenue = new BigDecimal("7500.00");
        
        TopSellingPart part = new TopSellingPart(partName, quantitySold, revenue);
        
        assertEquals(partName, part.getPartName());
        assertEquals(quantitySold, part.getQuantitySold());
        assertEquals(revenue, part.getRevenue());
    }
    
    @Test
    void testConstructorWithNullRevenue() {
        TopSellingPart part = new TopSellingPart("Part A", 100, null);
        
        assertEquals("Part A", part.getPartName());
        assertEquals(100, part.getQuantitySold());
        assertEquals(BigDecimal.ZERO, part.getRevenue());
    }
    
    @Test
    void testSetAndGetPartName() {
        String partName = "Brake Pads";
        topSellingPart.setPartName(partName);
        
        assertEquals(partName, topSellingPart.getPartName());
    }
    
    @Test
    void testSetAndGetQuantitySold() {
        topSellingPart.setQuantitySold(250);
        
        assertEquals(250, topSellingPart.getQuantitySold());
    }
    
    @Test
    void testSetAndGetRevenue() {
        BigDecimal revenue = new BigDecimal("12500.50");
        topSellingPart.setRevenue(revenue);
        
        assertEquals(revenue, topSellingPart.getRevenue());
    }
    
    @Test
    void testToString() {
        topSellingPart.setPartName("Spark Plugs");
        topSellingPart.setQuantitySold(200);
        topSellingPart.setRevenue(new BigDecimal("4000.00"));
        
        String result = topSellingPart.toString();
        
        assertNotNull(result);
        assertTrue(result.contains("TopSellingPart"));
        assertTrue(result.contains("Spark Plugs"));
        assertTrue(result.contains("200"));
        assertTrue(result.contains("4000.00"));
    }
    
    @Test
    void testZeroQuantitySold() {
        topSellingPart.setPartName("Rare Part");
        topSellingPart.setQuantitySold(0);
        topSellingPart.setRevenue(BigDecimal.ZERO);
        
        assertEquals(0, topSellingPart.getQuantitySold());
        assertEquals(BigDecimal.ZERO, topSellingPart.getRevenue());
    }
    
    @Test
    void testLargeQuantitySold() {
        topSellingPart.setQuantitySold(10000);
        
        assertEquals(10000, topSellingPart.getQuantitySold());
    }
    
    @Test
    void testLargeRevenue() {
        BigDecimal largeRevenue = new BigDecimal("999999.99");
        topSellingPart.setRevenue(largeRevenue);
        
        assertEquals(largeRevenue, topSellingPart.getRevenue());
    }
    
    @Test
    void testNullPartName() {
        topSellingPart.setPartName(null);
        
        assertNull(topSellingPart.getPartName());
    }
    
    @Test
    void testEmptyPartName() {
        topSellingPart.setPartName("");
        
        assertEquals("", topSellingPart.getPartName());
    }
    
    @Test
    void testPartNameWithSpecialCharacters() {
        String specialName = "Part #123 (Premium)";
        topSellingPart.setPartName(specialName);
        
        assertEquals(specialName, topSellingPart.getPartName());
    }
    
    @Test
    void testRevenueWithDecimals() {
        BigDecimal revenue = new BigDecimal("1234.56");
        topSellingPart.setRevenue(revenue);
        
        assertEquals(revenue, topSellingPart.getRevenue());
        assertEquals(0, revenue.compareTo(topSellingPart.getRevenue()));
    }
    
    @Test
    void testMultipleInstances() {
        TopSellingPart part1 = new TopSellingPart("Part A", 100, new BigDecimal("5000.00"));
        TopSellingPart part2 = new TopSellingPart("Part B", 75, new BigDecimal("3750.00"));
        
        assertNotEquals(part1.getPartName(), part2.getPartName());
        assertNotEquals(part1.getQuantitySold(), part2.getQuantitySold());
        assertNotEquals(part1.getRevenue(), part2.getRevenue());
    }
    
    @Test
    void testUpdateValues() {
        topSellingPart.setPartName("Initial Part");
        topSellingPart.setQuantitySold(50);
        topSellingPart.setRevenue(new BigDecimal("2500.00"));
        
        // Update values
        topSellingPart.setPartName("Updated Part");
        topSellingPart.setQuantitySold(100);
        topSellingPart.setRevenue(new BigDecimal("5000.00"));
        
        assertEquals("Updated Part", topSellingPart.getPartName());
        assertEquals(100, topSellingPart.getQuantitySold());
        assertEquals(new BigDecimal("5000.00"), topSellingPart.getRevenue());
    }
}
