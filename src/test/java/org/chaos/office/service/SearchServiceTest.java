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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SearchService.
 * Tests search functionality across multiple fields with various query patterns.
 */
class SearchServiceTest {
    
    private SearchService searchService;
    private Category testCategory1;
    private Category testCategory2;
    private PartService partService;
    
    @BeforeEach
    void setUp() throws Exception {
        searchService = new SearchService();
        partService = new PartService();
        
        // Initialize database
        DatabaseConnection.getInstance().initializeDatabase();
        
        // Clean up any existing test data first
        try {
            cleanupTestData();
        } catch (SQLException e) {
            // Ignore cleanup errors on first run
        }
        
        // Create test categories with unique names
        testCategory1 = createTestCategory("Electronics", "Electronic parts");
        testCategory2 = createTestCategory("Mechanical", "Mechanical parts");
        
        // Create test parts for search testing
        createTestParts();
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
    void testSearchByText_MatchesPartName() {
        List<Part> results = searchService.searchByText("Brake");
        
        assertNotNull(results);
        assertTrue(results.size() >= 1);
        assertTrue(results.stream().anyMatch(p -> p.getName().contains("Brake")));
    }
    
    @Test
    void testSearchByText_MatchesMakerName() {
        List<Part> results = searchService.searchByText("Bosch");
        
        assertNotNull(results);
        assertTrue(results.size() >= 1);
        assertTrue(results.stream().anyMatch(p -> "Bosch".equals(p.getMaker())));
    }
    
    @Test
    void testSearchByText_MatchesDescription() {
        List<Part> results = searchService.searchByText("premium");
        
        assertNotNull(results);
        assertTrue(results.size() >= 1);
        assertTrue(results.stream().anyMatch(p -> p.getDescription().toLowerCase().contains("premium")));
    }
    
    @Test
    void testSearchByText_MatchesCategoryName() {
        List<Part> results = searchService.searchByText("Electronics");
        
        assertNotNull(results);
        assertTrue(results.size() >= 1);
        assertTrue(results.stream().anyMatch(p -> "Electronics".equals(p.getCategory().getName())));
    }
    
    @Test
    void testSearchByText_CaseInsensitive() {
        List<Part> resultsLower = searchService.searchByText("brake");
        List<Part> resultsUpper = searchService.searchByText("BRAKE");
        List<Part> resultsMixed = searchService.searchByText("BrAkE");
        
        assertNotNull(resultsLower);
        assertNotNull(resultsUpper);
        assertNotNull(resultsMixed);
        
        // All should return the same results
        assertEquals(resultsLower.size(), resultsUpper.size());
        assertEquals(resultsLower.size(), resultsMixed.size());
    }
    
    @Test
    void testSearchByText_EmptyQuery() {
        List<Part> results = searchService.searchByText("");
        
        assertNotNull(results);
        // Should return all parts
        assertTrue(results.size() > 0);
    }
    
    @Test
    void testSearchByText_NullQuery() {
        List<Part> results = searchService.searchByText(null);
        
        assertNotNull(results);
        // Should return all parts
        assertTrue(results.size() > 0);
    }
    
    @Test
    void testSearchByText_WhitespaceQuery() {
        List<Part> results = searchService.searchByText("   ");
        
        assertNotNull(results);
        // Should return all parts
        assertTrue(results.size() > 0);
    }
    
    @Test
    void testSearchByText_NoMatches() {
        List<Part> results = searchService.searchByText("NonExistentPartXYZ123");
        
        assertNotNull(results);
        assertEquals(0, results.size());
    }
    
    @Test
    void testSearchByText_PartialMatch() {
        List<Part> results = searchService.searchByText("Pad");
        
        assertNotNull(results);
        assertTrue(results.size() >= 1);
        assertTrue(results.stream().anyMatch(p -> p.getName().contains("Pad")));
    }
    
    @Test
    void testSearchByText_SpecialCharacters() {
        // Create a part with special characters
        Part specialPart = createPart("Part-With-Dashes", "Test-Maker", "Description", 50.0f, 10, testCategory1);
        partService.savePart(specialPart);
        
        List<Part> results = searchService.searchByText("Dashes");
        
        assertNotNull(results);
        assertTrue(results.size() >= 1);
    }
    
    @Test
    void testFilterByCategory_ReturnsOnlyMatchingCategory() {
        List<Part> results = searchService.filterByCategory(testCategory1.getId());
        
        assertNotNull(results);
        assertTrue(results.size() > 0);
        assertTrue(results.stream().allMatch(p -> p.getCategory().getId() == testCategory1.getId()));
    }
    
    @Test
    void testFilterByCategory_DifferentCategories() {
        List<Part> results1 = searchService.filterByCategory(testCategory1.getId());
        List<Part> results2 = searchService.filterByCategory(testCategory2.getId());
        
        assertNotNull(results1);
        assertNotNull(results2);
        
        // Results should be different
        assertTrue(results1.size() > 0);
        assertTrue(results2.size() > 0);
        
        // Verify no overlap in categories
        assertTrue(results1.stream().allMatch(p -> p.getCategory().getId() == testCategory1.getId()));
        assertTrue(results2.stream().allMatch(p -> p.getCategory().getId() == testCategory2.getId()));
    }
    
    @Test
    void testFilterByCategory_NonExistentCategory() {
        List<Part> results = searchService.filterByCategory(99999);
        
        assertNotNull(results);
        assertEquals(0, results.size());
    }
    
    @Test
    void testSearchWithFilters_BothQueryAndCategory() {
        List<Part> results = searchService.searchWithFilters("Brake", testCategory1.getId());
        
        assertNotNull(results);
        // Should only return parts that match both the query AND the category
        assertTrue(results.stream().allMatch(p -> p.getCategory().getId() == testCategory1.getId()));
        assertTrue(results.stream().anyMatch(p -> 
            p.getName().contains("Brake") || 
            p.getDescription().toLowerCase().contains("brake") ||
            p.getMaker().toLowerCase().contains("brake")));
    }
    
    @Test
    void testSearchWithFilters_QueryMatchesButWrongCategory() {
        // Search for "Brake" in category2, but Brake Pad is in category1
        List<Part> results = searchService.searchWithFilters("Brake", testCategory2.getId());
        
        assertNotNull(results);
        // Should not return Brake Pad since it's in the wrong category
        assertFalse(results.stream().anyMatch(p -> "Brake Pad".equals(p.getName())));
    }
    
    @Test
    void testSearchWithFilters_EmptyQuery() {
        List<Part> results = searchService.searchWithFilters("", testCategory1.getId());
        
        assertNotNull(results);
        // Should return all parts in the category
        assertTrue(results.stream().allMatch(p -> p.getCategory().getId() == testCategory1.getId()));
    }
    
    @Test
    void testSearchWithFilters_NullQuery() {
        List<Part> results = searchService.searchWithFilters(null, testCategory1.getId());
        
        assertNotNull(results);
        // Should return all parts in the category
        assertTrue(results.stream().allMatch(p -> p.getCategory().getId() == testCategory1.getId()));
    }
    
    @Test
    void testSearchWithFilters_CaseInsensitive() {
        List<Part> resultsLower = searchService.searchWithFilters("brake", testCategory1.getId());
        List<Part> resultsUpper = searchService.searchWithFilters("BRAKE", testCategory1.getId());
        
        assertNotNull(resultsLower);
        assertNotNull(resultsUpper);
        assertEquals(resultsLower.size(), resultsUpper.size());
    }
    
    @Test
    void testSearchParts_NullQueryNullCategory() {
        List<Part> results = searchService.searchParts(null, null);
        
        assertNotNull(results);
        // Should return all parts
        assertTrue(results.size() > 0);
    }
    
    @Test
    void testSearchParts_EmptyQueryNullCategory() {
        List<Part> results = searchService.searchParts("", null);
        
        assertNotNull(results);
        // Should return all parts
        assertTrue(results.size() > 0);
    }
    
    @Test
    void testSearchParts_QueryOnlyNullCategory() {
        List<Part> results = searchService.searchParts("Brake", null);
        
        assertNotNull(results);
        assertTrue(results.size() >= 1);
        assertTrue(results.stream().anyMatch(p -> p.getName().contains("Brake")));
    }
    
    @Test
    void testSearchParts_NullQueryWithCategory() {
        List<Part> results = searchService.searchParts(null, testCategory1.getId());
        
        assertNotNull(results);
        assertTrue(results.stream().allMatch(p -> p.getCategory().getId() == testCategory1.getId()));
    }
    
    @Test
    void testSearchParts_EmptyQueryWithCategory() {
        List<Part> results = searchService.searchParts("", testCategory1.getId());
        
        assertNotNull(results);
        assertTrue(results.stream().allMatch(p -> p.getCategory().getId() == testCategory1.getId()));
    }
    
    @Test
    void testSearchParts_BothQueryAndCategory() {
        List<Part> results = searchService.searchParts("Brake", testCategory1.getId());
        
        assertNotNull(results);
        assertTrue(results.stream().allMatch(p -> p.getCategory().getId() == testCategory1.getId()));
        assertTrue(results.stream().anyMatch(p -> 
            p.getName().contains("Brake") || 
            p.getDescription().toLowerCase().contains("brake")));
    }
    
    @Test
    void testSearchResults_AreSorted() {
        List<Part> results = searchService.searchByText("");
        
        assertNotNull(results);
        assertTrue(results.size() > 1);
        
        // Verify results are sorted by name
        for (int i = 0; i < results.size() - 1; i++) {
            String name1 = results.get(i).getName();
            String name2 = results.get(i + 1).getName();
            assertTrue(name1.compareTo(name2) <= 0, 
                "Results should be sorted by name: " + name1 + " should come before " + name2);
        }
    }
    
    // Helper methods
    
    private void createTestParts() {
        // Create parts in category 1 (Electronics)
        Part part1 = createPart("Brake Pad", "Bosch", "Premium brake pad", 50.0f, 10, testCategory1);
        Part part2 = createPart("Oil Filter", "Mann", "High quality oil filter", 15.0f, 20, testCategory1);
        Part part3 = createPart("Spark Plug", "NGK", "Standard spark plug", 8.0f, 50, testCategory1);
        
        // Create parts in category 2 (Mechanical)
        Part part4 = createPart("Air Filter", "Bosch", "Engine air filter", 25.0f, 15, testCategory2);
        Part part5 = createPart("Fuel Pump", "Denso", "Electric fuel pump", 120.0f, 5, testCategory2);
        
        partService.savePart(part1);
        partService.savePart(part2);
        partService.savePart(part3);
        partService.savePart(part4);
        partService.savePart(part5);
    }
    
    private Part createPart(String name, String maker, String description, float price, int quantity, Category category) {
        Part part = new Part();
        part.setName(name);
        part.setMaker(maker);
        part.setDescription(description);
        part.setPrice(price);
        part.setQuantity(quantity);
        part.setCategory(category);
        return part;
    }
    
    private Category createTestCategory(String name, String description) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            // First check if category already exists
            String selectSql = "SELECT id, name, description FROM categories WHERE name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setString(1, name);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Category category = new Category();
                        category.setId(rs.getInt("id"));
                        category.setName(rs.getString("name"));
                        category.setDescription(rs.getString("description"));
                        return category;
                    }
                }
            }
            
            // If not exists, create new category
            String insertSql = "INSERT INTO categories (name, description) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
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
        }
        
        throw new SQLException("Failed to create test category");
    }
    
    private void cleanupTestData() throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            // Delete test parts
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM parts WHERE name IN ('Brake Pad', 'Oil Filter', 'Spark Plug', 'Air Filter', 'Fuel Pump', 'Part-With-Dashes')")) {
                stmt.executeUpdate();
            }
            
            // Delete test categories
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM categories WHERE name IN ('Electronics', 'Mechanical')")) {
                stmt.executeUpdate();
            }
            
            // Delete test makers
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM makers WHERE name IN ('Bosch', 'Mann', 'NGK', 'Denso', 'Test-Maker')")) {
                stmt.executeUpdate();
            }
        }
    }
}
