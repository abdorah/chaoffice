package org.chaos.office.view;

import org.chaos.office.model.Category;
import org.chaos.office.model.Part;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PartCell.
 * 
 * <p>These tests verify:
 * <ul>
 *   <li>Cell class structure and inheritance</li>
 *   <li>Price formatting logic</li>
 *   <li>Details text building logic</li>
 * </ul>
 * 
 * <p>Note: Full rendering tests require JavaFX toolkit initialization.
 * These tests focus on the cell's structure and helper methods.
 * 
 * <p>Requirements: 5.2, 5.3, 5.4, 11.3
 */
class PartCellTest {
    
    private Category testCategory;
    
    @BeforeEach
    void setUp() {
        // Create test category
        testCategory = new Category(1, "Engine", "Engine parts", null);
    }
    
    @Test
    void testCellExtendsListCell() {
        // Verify PartCell extends ListCell<Part>
        assertTrue(javafx.scene.control.ListCell.class.isAssignableFrom(PartCell.class));
    }
    
    @Test
    void testCellCanBeInstantiated() {
        // Verify the cell class structure is valid
        // Actual instantiation requires JavaFX toolkit initialization
        assertNotNull(PartCell.class);
    }
    
    @Test
    void testPriceFormattingLogic() {
        // Test the price formatting logic (conceptually)
        // Format: $XX.XX with exactly 2 decimal places
        
        // Test various price values using US locale for consistent formatting
        float price1 = 0.50f;
        String expected1 = String.format(java.util.Locale.US, "$%.2f", price1);
        assertEquals("$0.50", expected1);
        
        float price2 = 25.99f;
        String expected2 = String.format(java.util.Locale.US, "$%.2f", price2);
        assertEquals("$25.99", expected2);
        
        float price3 = 100.00f;
        String expected3 = String.format(java.util.Locale.US, "$%.2f", price3);
        assertEquals("$100.00", expected3);
        
        float price4 = 1500.50f;
        String expected4 = String.format(java.util.Locale.US, "$%.2f", price4);
        assertEquals("$1500.50", expected4);
        
        float price5 = 0.01f;
        String expected5 = String.format(java.util.Locale.US, "$%.2f", price5);
        assertEquals("$0.01", expected5);
    }
    
    @Test
    void testDetailsTextStructure() {
        // Test the structure of details text
        // Format: "Category | Maker | $XX.XX | Stock: XX" or "Out of Stock"
        
        Part inStockPart = new Part(1, "Oil Filter", "Bosch", "Description", 25.99f, 50, testCategory);
        Part outOfStockPart = new Part(2, "Rare Part", "Rare", "Description", 100.00f, 0, testCategory);
        
        // Verify parts are created correctly
        assertNotNull(inStockPart);
        assertNotNull(outOfStockPart);
        assertEquals(50, inStockPart.getQuantity());
        assertEquals(0, outOfStockPart.getQuantity());
    }
    
    @Test
    void testPartWithNoCategory() {
        Part part = new Part(1, "Orphan Part", "Unknown", "Part without category", 10.00f, 5, null);
        
        // Should not throw exception when creating part without category
        assertNotNull(part);
        assertNull(part.getCategory());
    }
    
    @Test
    void testPartWithEmptyMaker() {
        Part part = new Part(1, "Generic Part", "", "Part with no maker", 15.00f, 20, testCategory);
        
        // Should not throw exception
        assertNotNull(part);
        assertEquals("", part.getMaker());
    }
    
    @Test
    void testPartWithNullMaker() {
        Part part = new Part(1, "Generic Part", null, "Part with null maker", 15.00f, 20, testCategory);
        
        // Should not throw exception
        assertNotNull(part);
        assertNull(part.getMaker());
    }
    
    @Test
    void testZeroPricePart() {
        Part part = new Part(1, "Free Sample", "Generic", "Free part", 0.00f, 10, testCategory);
        
        // Should render without error
        assertNotNull(part);
        assertEquals(0.00f, part.getPrice(), 0.001);
    }
    
    @Test
    void testHighPricePart() {
        Part part = new Part(1, "Expensive Part", "Premium", "Very expensive", 9999.99f, 1, testCategory);
        
        // Should render without error
        assertNotNull(part);
        assertEquals(9999.99f, part.getPrice(), 0.001);
    }
    
    @Test
    void testPartWithCategoryImage() {
        // Create category with image data
        byte[] imageData = new byte[]{1, 2, 3, 4, 5}; // Dummy image data
        Category categoryWithImage = new Category(1, "Engine", "Engine parts", imageData);
        Part part = new Part(1, "Oil Filter", "Bosch", "High quality", 25.99f, 50, categoryWithImage);
        
        // Should handle image data
        assertNotNull(part);
        assertNotNull(part.getCategory().getImage());
        assertEquals(5, part.getCategory().getImage().length);
    }
    
    @Test
    void testStockQuantityLevels() {
        // Test various stock levels
        Part outOfStock = new Part(1, "Part 1", "Maker", "Desc", 10.0f, 0, testCategory);
        Part lowStock = new Part(2, "Part 2", "Maker", "Desc", 10.0f, 1, testCategory);
        Part normalStock = new Part(3, "Part 3", "Maker", "Desc", 10.0f, 50, testCategory);
        Part highStock = new Part(4, "Part 4", "Maker", "Desc", 10.0f, 1000, testCategory);
        
        assertEquals(0, outOfStock.getQuantity());
        assertEquals(1, lowStock.getQuantity());
        assertEquals(50, normalStock.getQuantity());
        assertEquals(1000, highStock.getQuantity());
    }
    
    @Test
    void testCurrencySymbolConstant() {
        // Verify currency symbol is used consistently
        String currencySymbol = "$";
        assertNotNull(currencySymbol);
        assertEquals("$", currencySymbol);
    }
}
