package org.chaos.office.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the PartImportData model.
 * Tests initialization, getters/setters, and data integrity.
 */
class PartImportDataTest {

    @Test
    void testDefaultConstructor() {
        PartImportData data = new PartImportData();
        assertNotNull(data);
        assertEquals(0, data.getRowNumber());
        assertNull(data.getName());
        assertNull(data.getMaker());
        assertNull(data.getDescription());
        assertNull(data.getPriceStr());
        assertNull(data.getQuantityStr());
        assertNull(data.getCategoryName());
    }

    @Test
    void testParameterizedConstructor() {
        PartImportData data = new PartImportData(
            5,
            "Brake Pad",
            "Bosch",
            "High-performance brake pad",
            "49.99",
            "10",
            "Brakes"
        );
        
        assertEquals(5, data.getRowNumber());
        assertEquals("Brake Pad", data.getName());
        assertEquals("Bosch", data.getMaker());
        assertEquals("High-performance brake pad", data.getDescription());
        assertEquals("49.99", data.getPriceStr());
        assertEquals("10", data.getQuantityStr());
        assertEquals("Brakes", data.getCategoryName());
    }

    @Test
    void testSettersAndGetters() {
        PartImportData data = new PartImportData();
        
        data.setRowNumber(3);
        data.setName("Oil Filter");
        data.setMaker("Mann");
        data.setDescription("Premium oil filter");
        data.setPriceStr("15.50");
        data.setQuantityStr("25");
        data.setCategoryName("Engine");
        
        assertEquals(3, data.getRowNumber());
        assertEquals("Oil Filter", data.getName());
        assertEquals("Mann", data.getMaker());
        assertEquals("Premium oil filter", data.getDescription());
        assertEquals("15.50", data.getPriceStr());
        assertEquals("25", data.getQuantityStr());
        assertEquals("Engine", data.getCategoryName());
    }

    @Test
    void testToString() {
        PartImportData data = new PartImportData(
            2,
            "Spark Plug",
            "NGK",
            "Iridium spark plug",
            "12.99",
            "8",
            "Ignition"
        );
        
        String result = data.toString();
        
        assertTrue(result.contains("rowNumber=2"));
        assertTrue(result.contains("name='Spark Plug'"));
        assertTrue(result.contains("maker='NGK'"));
        assertTrue(result.contains("description='Iridium spark plug'"));
        assertTrue(result.contains("priceStr='12.99'"));
        assertTrue(result.contains("quantityStr='8'"));
        assertTrue(result.contains("categoryName='Ignition'"));
    }

    @Test
    void testEmptyStrings() {
        PartImportData data = new PartImportData(1, "", "", "", "", "", "");
        
        assertEquals("", data.getName());
        assertEquals("", data.getMaker());
        assertEquals("", data.getDescription());
        assertEquals("", data.getPriceStr());
        assertEquals("", data.getQuantityStr());
        assertEquals("", data.getCategoryName());
    }

    @Test
    void testNullValues() {
        PartImportData data = new PartImportData(1, null, null, null, null, null, null);
        
        assertNull(data.getName());
        assertNull(data.getMaker());
        assertNull(data.getDescription());
        assertNull(data.getPriceStr());
        assertNull(data.getQuantityStr());
        assertNull(data.getCategoryName());
    }

    @Test
    void testRowNumberZero() {
        PartImportData data = new PartImportData();
        data.setRowNumber(0);
        assertEquals(0, data.getRowNumber());
    }

    @Test
    void testRowNumberNegative() {
        // While negative row numbers don't make sense in practice,
        // the model should still accept them (validation happens elsewhere)
        PartImportData data = new PartImportData();
        data.setRowNumber(-1);
        assertEquals(-1, data.getRowNumber());
    }

    @Test
    void testLargeRowNumber() {
        PartImportData data = new PartImportData();
        data.setRowNumber(999999);
        assertEquals(999999, data.getRowNumber());
    }

    @Test
    void testPriceStringFormats() {
        PartImportData data = new PartImportData();
        
        // Test various price string formats (validation happens elsewhere)
        data.setPriceStr("10.50");
        assertEquals("10.50", data.getPriceStr());
        
        data.setPriceStr("100");
        assertEquals("100", data.getPriceStr());
        
        data.setPriceStr("0.99");
        assertEquals("0.99", data.getPriceStr());
        
        data.setPriceStr("invalid");
        assertEquals("invalid", data.getPriceStr());
    }

    @Test
    void testQuantityStringFormats() {
        PartImportData data = new PartImportData();
        
        // Test various quantity string formats (validation happens elsewhere)
        data.setQuantityStr("5");
        assertEquals("5", data.getQuantityStr());
        
        data.setQuantityStr("0");
        assertEquals("0", data.getQuantityStr());
        
        data.setQuantityStr("1000");
        assertEquals("1000", data.getQuantityStr());
        
        data.setQuantityStr("invalid");
        assertEquals("invalid", data.getQuantityStr());
    }

    @Test
    void testLongStrings() {
        PartImportData data = new PartImportData();
        
        String longName = "A".repeat(500);
        String longDescription = "B".repeat(1000);
        
        data.setName(longName);
        data.setDescription(longDescription);
        
        assertEquals(longName, data.getName());
        assertEquals(longDescription, data.getDescription());
    }

    @Test
    void testSpecialCharacters() {
        PartImportData data = new PartImportData(
            1,
            "Part with \"quotes\" and 'apostrophes'",
            "Maker & Co.",
            "Description with <tags> and symbols: @#$%",
            "19.99",
            "5",
            "Category/Subcategory"
        );
        
        assertEquals("Part with \"quotes\" and 'apostrophes'", data.getName());
        assertEquals("Maker & Co.", data.getMaker());
        assertEquals("Description with <tags> and symbols: @#$%", data.getDescription());
        assertEquals("Category/Subcategory", data.getCategoryName());
    }

    @Test
    void testWhitespaceHandling() {
        PartImportData data = new PartImportData(
            1,
            "  Name with spaces  ",
            "\tMaker with tab\t",
            "Description\nwith\nnewlines",
            " 10.50 ",
            " 5 ",
            "  Category  "
        );
        
        // Model should preserve whitespace (trimming happens during validation)
        assertEquals("  Name with spaces  ", data.getName());
        assertEquals("\tMaker with tab\t", data.getMaker());
        assertEquals("Description\nwith\nnewlines", data.getDescription());
        assertEquals(" 10.50 ", data.getPriceStr());
        assertEquals(" 5 ", data.getQuantityStr());
        assertEquals("  Category  ", data.getCategoryName());
    }

    @Test
    void testDataUpdate() {
        PartImportData data = new PartImportData(
            1,
            "Original Name",
            "Original Maker",
            "Original Description",
            "10.00",
            "5",
            "Original Category"
        );
        
        // Update all fields
        data.setRowNumber(2);
        data.setName("Updated Name");
        data.setMaker("Updated Maker");
        data.setDescription("Updated Description");
        data.setPriceStr("20.00");
        data.setQuantityStr("10");
        data.setCategoryName("Updated Category");
        
        assertEquals(2, data.getRowNumber());
        assertEquals("Updated Name", data.getName());
        assertEquals("Updated Maker", data.getMaker());
        assertEquals("Updated Description", data.getDescription());
        assertEquals("20.00", data.getPriceStr());
        assertEquals("10", data.getQuantityStr());
        assertEquals("Updated Category", data.getCategoryName());
    }
}
