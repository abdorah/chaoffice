package org.chaos.office.service;

import org.chaos.office.model.Category;
import org.chaos.office.model.Part;
import org.chaos.office.util.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PartService.
 */
class PartServiceTest {
    
    private PartService partService;
    private Category testCategory;
    
    @BeforeEach
    void setUp() throws Exception {
        partService = new PartService();
        
        // Initialize database
        DatabaseConnection.getInstance().initializeDatabase();
        
        // Clean up any existing test data first
        try {
            cleanupTestData();
        } catch (SQLException e) {
            // Ignore cleanup errors on first run
        }
        
        // Create test category
        testCategory = createTestCategory("Test Category", "Test Description");
    }
    
    @AfterEach
    void tearDown() {
        // Clean up test data
        try {
            cleanupTestData();
        } catch (SQLException e) {
            // Ignore cleanup errors
        }
    }
    
    @Test
    void testSaveAndRetrievePart() {
        Part part = new Part();
        part.setName("Test Part Unique " + System.currentTimeMillis());  // Make name unique
        part.setMaker("Test Maker");
        part.setDescription("Test Description");
        part.setPrice(100.0f);  // Use 100.0 to avoid floating point precision issues
        part.setQuantity(10);
        part.setCategory(testCategory);
        
        partService.savePart(part);
        
        List<Part> parts = partService.getAllParts();
        assertTrue(parts.size() > 0);
        
        Optional<Part> found = parts.stream()
                .filter(p -> part.getName().equals(p.getName()))
                .findFirst();
        
        assertTrue(found.isPresent());
        assertEquals("Test Maker", found.get().getMaker());
        assertEquals(100.0f, found.get().getPrice(), 0.01f);
        assertEquals(10, found.get().getQuantity());
    }
    
    @Test
    void testGetPartById() {
        Part part = new Part();
        part.setName("Test Part By ID");
        part.setMaker("Test Maker");
        part.setDescription("Test Description");
        part.setPrice(50.0f);
        part.setQuantity(5);
        part.setCategory(testCategory);
        
        partService.savePart(part);
        
        // Get the saved part to retrieve its ID
        List<Part> parts = partService.getAllParts();
        Optional<Part> savedPart = parts.stream()
                .filter(p -> "Test Part By ID".equals(p.getName()))
                .findFirst();
        
        assertTrue(savedPart.isPresent());
        int partId = savedPart.get().getId();
        
        // Retrieve by ID
        Optional<Part> retrieved = partService.getPartById(partId);
        assertTrue(retrieved.isPresent());
        assertEquals("Test Part By ID", retrieved.get().getName());
    }
    
    @Test
    void testUpdatePart() {
        Part part = new Part();
        part.setName("Original Name");
        part.setMaker("Original Maker");
        part.setDescription("Original Description");
        part.setPrice(100.0f);
        part.setQuantity(20);
        part.setCategory(testCategory);
        
        partService.savePart(part);
        
        // Get the saved part
        List<Part> parts = partService.getAllParts();
        Optional<Part> savedPart = parts.stream()
                .filter(p -> "Original Name".equals(p.getName()))
                .findFirst();
        
        assertTrue(savedPart.isPresent());
        Part toUpdate = savedPart.get();
        
        // Update the part
        toUpdate.setName("Updated Name");
        toUpdate.setPrice(150.0f);
        toUpdate.setQuantity(30);
        
        partService.updatePart(toUpdate);
        
        // Retrieve and verify
        Optional<Part> updated = partService.getPartById(toUpdate.getId());
        assertTrue(updated.isPresent());
        assertEquals("Updated Name", updated.get().getName());
        assertEquals(150.0f, updated.get().getPrice(), 0.01f);
        assertEquals(30, updated.get().getQuantity());
    }
    
    @Test
    void testDeletePart() {
        Part part = new Part();
        part.setName("Part To Delete");
        part.setMaker("Test Maker");
        part.setDescription("Test Description");
        part.setPrice(75.0f);
        part.setQuantity(15);
        part.setCategory(testCategory);
        
        partService.savePart(part);
        
        // Get the saved part
        List<Part> parts = partService.getAllParts();
        Optional<Part> savedPart = parts.stream()
                .filter(p -> "Part To Delete".equals(p.getName()))
                .findFirst();
        
        assertTrue(savedPart.isPresent());
        int partId = savedPart.get().getId();
        
        // Delete the part
        partService.deletePart(partId);
        
        // Verify deletion
        Optional<Part> deleted = partService.getPartById(partId);
        assertFalse(deleted.isPresent());
    }
    
    @Test
    void testSearchPartsByName() {
        Part part1 = createPart("Brake Pad", "Brembo", "Front brake pad", 50.0f, 10);
        Part part2 = createPart("Brake Disc", "Brembo", "Front brake disc", 100.0f, 5);
        Part part3 = createPart("Oil Filter", "Bosch", "Engine oil filter", 15.0f, 20);
        
        partService.savePart(part1);
        partService.savePart(part2);
        partService.savePart(part3);
        
        List<Part> results = partService.searchParts("brake");
        
        assertTrue(results.size() >= 2);
        assertTrue(results.stream().anyMatch(p -> p.getName().contains("Brake")));
    }
    
    @Test
    void testSearchPartsByMaker() {
        Part part1 = createPart("Part A", "Bosch", "Description A", 50.0f, 10);
        Part part2 = createPart("Part B", "Bosch", "Description B", 60.0f, 15);
        Part part3 = createPart("Part C", "Denso", "Description C", 70.0f, 20);
        
        partService.savePart(part1);
        partService.savePart(part2);
        partService.savePart(part3);
        
        List<Part> results = partService.searchParts("bosch");
        
        assertTrue(results.size() >= 2);
        assertTrue(results.stream().allMatch(p -> "Bosch".equalsIgnoreCase(p.getMaker())));
    }
    
    @Test
    void testSearchPartsWithEmptyQuery() {
        List<Part> results = partService.searchParts("");
        assertNotNull(results);
        // Should return all parts
    }
    
    @Test
    void testFilterByCategory() {
        Part part1 = createPart("Part in Category", "Maker1", "Description", 50.0f, 10);
        partService.savePart(part1);
        
        List<Part> results = partService.filterByCategory(testCategory.getId());
        
        assertTrue(results.size() > 0);
        assertTrue(results.stream().allMatch(p -> p.getCategory().getId() == testCategory.getId()));
    }
    
    @Test
    void testValidatePartWithMissingName() {
        Part part = new Part();
        part.setName("");
        part.setMaker("Test Maker");
        part.setDescription("Test Description");
        part.setPrice(50.0f);
        part.setQuantity(10);
        part.setCategory(testCategory);
        
        assertThrows(IllegalArgumentException.class, () -> partService.savePart(part));
    }
    
    @Test
    void testValidatePartWithMissingMaker() {
        Part part = new Part();
        part.setName("Test Part");
        part.setMaker("");
        part.setDescription("Test Description");
        part.setPrice(50.0f);
        part.setQuantity(10);
        part.setCategory(testCategory);
        
        assertThrows(IllegalArgumentException.class, () -> partService.savePart(part));
    }
    
    @Test
    void testValidatePartWithNegativePrice() {
        Part part = new Part();
        part.setName("Test Part");
        part.setMaker("Test Maker");
        part.setDescription("Test Description");
        part.setPrice(-10.0f);
        part.setQuantity(10);
        part.setCategory(testCategory);
        
        assertThrows(IllegalArgumentException.class, () -> partService.savePart(part));
    }
    
    @Test
    void testValidatePartWithNegativeQuantity() {
        Part part = new Part();
        part.setName("Test Part");
        part.setMaker("Test Maker");
        part.setDescription("Test Description");
        part.setPrice(50.0f);
        part.setQuantity(-5);
        part.setCategory(testCategory);
        
        assertThrows(IllegalArgumentException.class, () -> partService.savePart(part));
    }
    
    @Test
    void testValidatePartWithNullCategory() {
        Part part = new Part();
        part.setName("Test Part");
        part.setMaker("Test Maker");
        part.setDescription("Test Description");
        part.setPrice(50.0f);
        part.setQuantity(10);
        part.setCategory(null);
        
        assertThrows(IllegalArgumentException.class, () -> partService.savePart(part));
    }
    
    // Helper methods
    
    private Part createPart(String name, String maker, String description, float price, int quantity) {
        Part part = new Part();
        part.setName(name);
        part.setMaker(maker);
        part.setDescription(description);
        part.setPrice(price);
        part.setQuantity(quantity);
        part.setCategory(testCategory);
        return part;
    }
    
    private Category createTestCategory(String name, String description) throws SQLException {
        // First check if category already exists
        String checkSql = "SELECT id FROM categories WHERE name = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, name);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    Category category = new Category();
                    category.setId(rs.getInt("id"));
                    category.setName(name);
                    category.setDescription(description);
                    return category;
                }
            }
        }
        
        // If not exists, create it
        String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.executeUpdate();
            
            // Get the last inserted ID
            try (PreparedStatement idStmt = conn.prepareStatement("SELECT last_insert_rowid()");
                 ResultSet rs = idStmt.executeQuery()) {
                if (rs.next()) {
                    Category category = new Category();
                    category.setId(rs.getInt(1));
                    category.setName(name);
                    category.setDescription(description);
                    return category;
                }
            }
        }
        
        throw new SQLException("Failed to create test category");
    }
    
    private void cleanupTestData() throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            // Delete test parts
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM parts WHERE name LIKE 'Test%' OR name LIKE '%Test%' OR name LIKE 'Part%' OR name LIKE 'Brake%' OR name LIKE 'Oil%' OR name LIKE 'Original%' OR name LIKE 'Updated%'")) {
                stmt.executeUpdate();
            }
            
            // Delete test categories
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM categories WHERE name LIKE 'Test%'")) {
                stmt.executeUpdate();
            }
            
            // Delete test makers
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM makers WHERE name LIKE 'Test%' OR name = 'Maker1'")) {
                stmt.executeUpdate();
            }
        }
    }
}
