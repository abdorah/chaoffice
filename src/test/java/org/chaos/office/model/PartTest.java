package org.chaos.office.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Part model.
 */
class PartTest {

    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new Category(1, "Engine", "Engine parts", null);
    }

    @Test
    void testDefaultConstructor() {
        Part part = new Part();
        assertNotNull(part);
        assertEquals(0, part.getId());
        assertNull(part.getName());
        assertNull(part.getMaker());
        assertNull(part.getDescription());
        assertEquals(0.0f, part.getPrice());
        assertEquals(0, part.getQuantity());
        assertNull(part.getCategory());
    }

    @Test
    void testParameterizedConstructor() {
        Part part = new Part(1, "Oil Filter", "Bosch", "High quality oil filter", 25.99f, 50, testCategory);
        
        assertEquals(1, part.getId());
        assertEquals("Oil Filter", part.getName());
        assertEquals("Bosch", part.getMaker());
        assertEquals("High quality oil filter", part.getDescription());
        assertEquals(25.99f, part.getPrice(), 0.001);
        assertEquals(50, part.getQuantity());
        assertEquals(testCategory, part.getCategory());
    }

    @Test
    void testSettersAndGetters() {
        Part part = new Part();
        
        part.setId(42);
        part.setName("Spark Plug");
        part.setMaker("NGK");
        part.setDescription("Standard spark plug");
        part.setPrice(12.50f);
        part.setQuantity(100);
        part.setCategory(testCategory);
        
        assertEquals(42, part.getId());
        assertEquals("Spark Plug", part.getName());
        assertEquals("NGK", part.getMaker());
        assertEquals("Standard spark plug", part.getDescription());
        assertEquals(12.50f, part.getPrice(), 0.001);
        assertEquals(100, part.getQuantity());
        assertEquals(testCategory, part.getCategory());
    }

    @Test
    void testPropertyAccessors() {
        Part part = new Part(1, "Oil Filter", "Bosch", "High quality oil filter", 25.99f, 50, testCategory);
        
        assertNotNull(part.idProperty());
        assertNotNull(part.nameProperty());
        assertNotNull(part.makerProperty());
        assertNotNull(part.descriptionProperty());
        assertNotNull(part.priceProperty());
        assertNotNull(part.quantityProperty());
        assertNotNull(part.categoryProperty());
        
        assertEquals(1, part.idProperty().get());
        assertEquals("Oil Filter", part.nameProperty().get());
        assertEquals("Bosch", part.makerProperty().get());
        assertEquals("High quality oil filter", part.descriptionProperty().get());
        assertEquals(25.99f, part.priceProperty().get(), 0.001);
        assertEquals(50, part.quantityProperty().get());
        assertEquals(testCategory, part.categoryProperty().get());
    }

    @Test
    void testPropertyBinding() {
        Part part = new Part();
        
        // Test that properties can be bound
        part.idProperty().set(100);
        assertEquals(100, part.getId());
        
        part.nameProperty().set("Brake Pad");
        assertEquals("Brake Pad", part.getName());
        
        part.makerProperty().set("Brembo");
        assertEquals("Brembo", part.getMaker());
        
        part.descriptionProperty().set("Premium brake pad");
        assertEquals("Premium brake pad", part.getDescription());
        
        part.priceProperty().set(45.00f);
        assertEquals(45.00f, part.getPrice(), 0.001);
        
        part.quantityProperty().set(25);
        assertEquals(25, part.getQuantity());
        
        part.categoryProperty().set(testCategory);
        assertEquals(testCategory, part.getCategory());
    }

    @Test
    void testToString() {
        Part part = new Part(1, "Oil Filter", "Bosch", "High quality oil filter", 25.99f, 50, testCategory);
        String result = part.toString();
        
        assertTrue(result.contains("id=1"));
        assertTrue(result.contains("name='Oil Filter'"));
        assertTrue(result.contains("maker='Bosch'"));
        assertTrue(result.contains("description='High quality oil filter'"));
        assertTrue(result.contains("price=25.99"));
        assertTrue(result.contains("quantity=50"));
        assertTrue(result.contains("category=Engine"));
    }

    @Test
    void testToStringWithNullCategory() {
        Part part = new Part(1, "Oil Filter", "Bosch", "High quality oil filter", 25.99f, 50, null);
        String result = part.toString();
        
        assertTrue(result.contains("category=null"));
    }

    @Test
    void testPriceValues() {
        Part cheapPart = new Part(1, "Washer", "Generic", "Small washer", 0.50f, 1000, testCategory);
        Part expensivePart = new Part(2, "Turbocharger", "Garrett", "High performance turbo", 1500.00f, 5, testCategory);
        Part freePart = new Part(3, "Sample", "Free", "Free sample", 0.0f, 10, testCategory);
        
        assertEquals(0.50f, cheapPart.getPrice(), 0.001);
        assertEquals(1500.00f, expensivePart.getPrice(), 0.001);
        assertEquals(0.0f, freePart.getPrice(), 0.001);
    }

    @Test
    void testQuantityValues() {
        Part outOfStock = new Part(1, "Rare Part", "Rare", "Out of stock", 100.0f, 0, testCategory);
        Part inStock = new Part(2, "Common Part", "Common", "In stock", 10.0f, 500, testCategory);
        
        assertEquals(0, outOfStock.getQuantity());
        assertEquals(500, inStock.getQuantity());
    }

    @Test
    void testEmptyStrings() {
        Part part = new Part(1, "", "", "", 0.0f, 0, testCategory);
        
        assertEquals("", part.getName());
        assertEquals("", part.getMaker());
        assertEquals("", part.getDescription());
    }

    @Test
    void testCategoryChange() {
        Category category1 = new Category(1, "Engine", "Engine parts", null);
        Category category2 = new Category(2, "Suspension", "Suspension parts", null);
        
        Part part = new Part(1, "Oil Filter", "Bosch", "High quality oil filter", 25.99f, 50, category1);
        assertEquals(category1, part.getCategory());
        
        part.setCategory(category2);
        assertEquals(category2, part.getCategory());
    }

    @Test
    void testFloatPropertyPrecision() {
        Part part = new Part();
        
        // Test various price values
        part.setPrice(19.99f);
        assertEquals(19.99f, part.getPrice(), 0.001);
        
        part.setPrice(0.01f);
        assertEquals(0.01f, part.getPrice(), 0.001);
        
        part.setPrice(9999.99f);
        assertEquals(9999.99f, part.getPrice(), 0.001);
    }
}
