package org.chaos.office.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Category model.
 */
class CategoryTest {

    @Test
    void testDefaultConstructor() {
        Category category = new Category();
        assertNotNull(category);
        assertEquals(0, category.getId());
        assertNull(category.getName());
        assertNull(category.getDescription());
        assertNull(category.getImage());
    }

    @Test
    void testParameterizedConstructor() {
        byte[] imageData = new byte[]{1, 2, 3, 4, 5};
        Category category = new Category(1, "Engine", "Engine parts and components", imageData);
        
        assertEquals(1, category.getId());
        assertEquals("Engine", category.getName());
        assertEquals("Engine parts and components", category.getDescription());
        assertArrayEquals(imageData, category.getImage());
    }

    @Test
    void testSettersAndGetters() {
        Category category = new Category();
        byte[] imageData = new byte[]{10, 20, 30};
        
        category.setId(42);
        category.setName("Suspension");
        category.setDescription("Suspension system parts");
        category.setImage(imageData);
        
        assertEquals(42, category.getId());
        assertEquals("Suspension", category.getName());
        assertEquals("Suspension system parts", category.getDescription());
        assertArrayEquals(imageData, category.getImage());
    }

    @Test
    void testPropertyAccessors() {
        byte[] imageData = new byte[]{1, 2, 3};
        Category category = new Category(1, "Engine", "Engine parts", imageData);
        
        assertNotNull(category.idProperty());
        assertNotNull(category.nameProperty());
        assertNotNull(category.descriptionProperty());
        assertNotNull(category.imageProperty());
        
        assertEquals(1, category.idProperty().get());
        assertEquals("Engine", category.nameProperty().get());
        assertEquals("Engine parts", category.descriptionProperty().get());
        assertArrayEquals(imageData, category.imageProperty().get());
    }

    @Test
    void testPropertyBinding() {
        Category category = new Category();
        byte[] imageData = new byte[]{5, 10, 15};
        
        // Test that properties can be bound
        category.idProperty().set(100);
        assertEquals(100, category.getId());
        
        category.nameProperty().set("Brakes");
        assertEquals("Brakes", category.getName());
        
        category.descriptionProperty().set("Brake system components");
        assertEquals("Brake system components", category.getDescription());
        
        category.imageProperty().set(imageData);
        assertArrayEquals(imageData, category.getImage());
    }

    @Test
    void testToString() {
        byte[] imageData = new byte[]{1, 2, 3, 4, 5};
        Category category = new Category(1, "Engine", "Engine parts", imageData);
        String result = category.toString();
        
        assertTrue(result.contains("id=1"));
        assertTrue(result.contains("name='Engine'"));
        assertTrue(result.contains("description='Engine parts'"));
        assertTrue(result.contains("image=byte[5]"));
    }

    @Test
    void testToStringWithNullImage() {
        Category category = new Category(1, "Engine", "Engine parts", null);
        String result = category.toString();
        
        assertTrue(result.contains("image=null"));
    }

    @Test
    void testEmptyStrings() {
        Category category = new Category(1, "", "", null);
        
        assertEquals("", category.getName());
        assertEquals("", category.getDescription());
    }

    @Test
    void testImageDataIntegrity() {
        byte[] originalData = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        Category category = new Category(1, "Test", "Test category", originalData);
        
        byte[] retrievedData = category.getImage();
        assertArrayEquals(originalData, retrievedData);
        assertEquals(originalData.length, retrievedData.length);
    }

    @Test
    void testLargeImageData() {
        // Simulate a larger image (e.g., 1KB)
        byte[] largeImage = new byte[1024];
        for (int i = 0; i < largeImage.length; i++) {
            largeImage[i] = (byte) (i % 256);
        }
        
        Category category = new Category(1, "Test", "Test with large image", largeImage);
        assertArrayEquals(largeImage, category.getImage());
        assertEquals(1024, category.getImage().length);
    }

    @Test
    void testImageUpdate() {
        byte[] image1 = new byte[]{1, 2, 3};
        byte[] image2 = new byte[]{4, 5, 6, 7};
        
        Category category = new Category(1, "Test", "Test category", image1);
        assertArrayEquals(image1, category.getImage());
        
        category.setImage(image2);
        assertArrayEquals(image2, category.getImage());
    }
}
