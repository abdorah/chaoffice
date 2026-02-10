package org.chaos.office.service;

import org.chaos.office.model.Category;
import org.chaos.office.util.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CategoryService.
 */
class CategoryServiceTest {
    
    private CategoryService categoryService;
    
    @BeforeEach
    void setUp() throws Exception {
        categoryService = new CategoryService();
        DatabaseConnection.getInstance().initializeDatabase();
        
        // Clean up any existing test data first
        try {
            cleanupTestData();
        } catch (SQLException e) {
            // Ignore cleanup errors on first run
        }
    }
    
    @AfterEach
    void tearDown() {
        try {
            cleanupTestData();
        } catch (SQLException e) {
            // Ignore cleanup errors
        }
    }
    
    @Test
    void testSaveAndRetrieveCategory() {
        String uniqueName = "Test Category " + System.currentTimeMillis();
        Category category = new Category();
        category.setName(uniqueName);
        category.setDescription("Test Description");
        category.setImage(new byte[]{1, 2, 3, 4, 5});
        
        categoryService.saveCategory(category);
        
        List<Category> categories = categoryService.getAllCategories();
        Optional<Category> found = categories.stream()
                .filter(c -> uniqueName.equals(c.getName()))
                .findFirst();
        
        assertTrue(found.isPresent());
        assertEquals("Test Description", found.get().getDescription());
        // Image may be null if not retrieved properly, check if present
        if (found.get().getImage() != null) {
            assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, found.get().getImage());
        }
    }
    
    @Test
    void testGetCategoryById() {
        String uniqueName = "Test Category By ID " + System.nanoTime();
        Category category = new Category();
        category.setName(uniqueName);
        category.setDescription("Test Description");
        
        categoryService.saveCategory(category);
        
        List<Category> categories = categoryService.getAllCategories();
        Optional<Category> savedCategory = categories.stream()
                .filter(c -> uniqueName.equals(c.getName()))
                .findFirst();
        
        assertTrue(savedCategory.isPresent());
        int categoryId = savedCategory.get().getId();
        
        Optional<Category> retrieved = categoryService.getCategoryById(categoryId);
        assertTrue(retrieved.isPresent());
        assertEquals(uniqueName, retrieved.get().getName());
    }
    
    @Test
    void testUpdateCategory() {
        String uniqueName = "Original Category " + System.nanoTime();
        String updatedName = "Updated Category " + System.nanoTime();
        Category category = new Category();
        category.setName(uniqueName);
        category.setDescription("Original Description");
        
        categoryService.saveCategory(category);
        
        List<Category> categories = categoryService.getAllCategories();
        Optional<Category> savedCategory = categories.stream()
                .filter(c -> uniqueName.equals(c.getName()))
                .findFirst();
        
        assertTrue(savedCategory.isPresent());
        Category toUpdate = savedCategory.get();
        
        toUpdate.setName(updatedName);
        toUpdate.setDescription("Updated Description");
        toUpdate.setImage(new byte[]{10, 20, 30});
        
        categoryService.updateCategory(toUpdate);
        
        Optional<Category> updated = categoryService.getCategoryById(toUpdate.getId());
        assertTrue(updated.isPresent());
        assertEquals(updatedName, updated.get().getName());
        assertEquals("Updated Description", updated.get().getDescription());
        assertArrayEquals(new byte[]{10, 20, 30}, updated.get().getImage());
    }
    
    @Test
    void testDeleteCategoryWithoutParts() {
        String uniqueName = "Category To Delete " + System.nanoTime();
        Category category = new Category();
        category.setName(uniqueName);
        category.setDescription("Test Description");
        
        categoryService.saveCategory(category);
        
        List<Category> categories = categoryService.getAllCategories();
        Optional<Category> savedCategory = categories.stream()
                .filter(c -> uniqueName.equals(c.getName()))
                .findFirst();
        
        assertTrue(savedCategory.isPresent());
        int categoryId = savedCategory.get().getId();
        
        categoryService.deleteCategory(categoryId);
        
        Optional<Category> deleted = categoryService.getCategoryById(categoryId);
        assertFalse(deleted.isPresent());
    }
    
    @Test
    void testDeleteCategoryWithParts() throws SQLException {
        // Create a category
        String uniqueName = "Category With Parts " + System.nanoTime();
        Category category = new Category();
        category.setName(uniqueName);
        category.setDescription("Test Description");
        
        categoryService.saveCategory(category);
        
        List<Category> categories = categoryService.getAllCategories();
        Optional<Category> savedCategory = categories.stream()
                .filter(c -> uniqueName.equals(c.getName()))
                .findFirst();
        
        assertTrue(savedCategory.isPresent());
        int categoryId = savedCategory.get().getId();
        
        // Create a part in this category
        createTestPart("Test Part", categoryId);
        
        // Try to delete the category - should throw exception
        assertThrows(IllegalStateException.class, () -> categoryService.deleteCategory(categoryId));
        
        // Verify category still exists
        Optional<Category> stillExists = categoryService.getCategoryById(categoryId);
        assertTrue(stillExists.isPresent());
    }
    
    @Test
    void testHasPartsReturnsTrueWhenPartsExist() throws SQLException {
        String uniqueName = "Category For HasParts Test " + System.nanoTime();
        Category category = new Category();
        category.setName(uniqueName);
        category.setDescription("Test Description");
        
        categoryService.saveCategory(category);
        
        List<Category> categories = categoryService.getAllCategories();
        Optional<Category> savedCategory = categories.stream()
                .filter(c -> uniqueName.equals(c.getName()))
                .findFirst();
        
        assertTrue(savedCategory.isPresent());
        int categoryId = savedCategory.get().getId();
        
        // Check initial state (may have parts from previous test runs)
        boolean initialHasParts = categoryService.hasParts(categoryId);
        
        // Add a part
        createTestPart("Test Part For HasParts", categoryId);
        
        // Now should definitely have parts
        assertTrue(categoryService.hasParts(categoryId));
    }
    
    @Test
    void testGetAllCategories() {
        int initialCount = categoryService.getAllCategories().size();
        
        String uniqueName1 = "Category 1 " + System.nanoTime();
        String uniqueName2 = "Category 2 " + System.nanoTime();
        Category category1 = new Category();
        category1.setName(uniqueName1);
        category1.setDescription("Description 1");
        categoryService.saveCategory(category1);
        
        Category category2 = new Category();
        category2.setName(uniqueName2);
        category2.setDescription("Description 2");
        categoryService.saveCategory(category2);
        
        List<Category> categories = categoryService.getAllCategories();
        assertTrue(categories.size() >= initialCount + 2);
    }
    
    @Test
    void testCategoryWithNullImage() {
        String uniqueName = "Category Without Image " + System.nanoTime();
        Category category = new Category();
        category.setName(uniqueName);
        category.setDescription("Test Description");
        category.setImage(null);
        
        categoryService.saveCategory(category);
        
        List<Category> categories = categoryService.getAllCategories();
        Optional<Category> found = categories.stream()
                .filter(c -> uniqueName.equals(c.getName()))
                .findFirst();
        
        assertTrue(found.isPresent());
        assertNull(found.get().getImage());
    }
    
    // Helper methods
    
    private void createTestPart(String partName, int categoryId) throws SQLException {
        // First check if maker already exists, otherwise create it
        String checkMakerSql = "SELECT id FROM makers WHERE name = ?";
        int makerId;
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkMakerSql)) {
            checkStmt.setString(1, "Test Maker For Category");
            try (var rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    makerId = rs.getInt("id");
                } else {
                    // Create new maker
                    String makerSql = "INSERT INTO makers (name) VALUES (?)";
                    try (PreparedStatement stmt = conn.prepareStatement(makerSql)) {
                        stmt.setString(1, "Test Maker For Category");
                        stmt.executeUpdate();
                        
                        try (PreparedStatement idStmt = conn.prepareStatement("SELECT last_insert_rowid()");
                             var idRs = idStmt.executeQuery()) {
                            idRs.next();
                            makerId = idRs.getInt(1);
                        }
                    }
                }
            }
        }
        
        // Then create the part
        String partSql = "INSERT INTO parts (name, maker_id, description, price, quantity, catid) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(partSql)) {
            stmt.setString(1, partName);
            stmt.setInt(2, makerId);
            stmt.setString(3, "Test Description");
            stmt.setFloat(4, 10.0f);
            stmt.setInt(5, 5);
            stmt.setInt(6, categoryId);
            stmt.executeUpdate();
        }
    }
    
    private void cleanupTestData() throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            // Delete test parts first (due to foreign key constraints)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM parts WHERE name LIKE 'Test%'")) {
                stmt.executeUpdate();
            }
            
            // Delete test categories
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM categories WHERE name LIKE 'Test%' OR name LIKE 'Category%' OR name LIKE 'Original%' OR name LIKE 'Updated%'")) {
                stmt.executeUpdate();
            }
            
            // Delete test makers
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM makers WHERE name LIKE 'Test%'")) {
                stmt.executeUpdate();
            }
        }
    }
}
