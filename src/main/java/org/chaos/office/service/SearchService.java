package org.chaos.office.service;

import org.chaos.office.model.Category;
import org.chaos.office.model.Part;
import org.chaos.office.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * SearchService provides optimized part search functionality for the POS system.
 * 
 * <p>This service is responsible for:
 * <ul>
 *   <li>Real-time part search across multiple fields</li>
 *   <li>Category-based filtering</li>
 *   <li>Combined search and filter operations</li>
 *   <li>Case-insensitive search with indexed queries</li>
 * </ul>
 * 
 * <p>All search queries use database indexes on parts.name and parts.catid
 * for optimal performance with large datasets.
 * 
 * <p>Requirements: 4.1, 4.2, 4.6, 6.2, 6.3
 */
public class SearchService {
    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    
    /**
     * Searches for parts with optional category filtering.
     * This is the main search method that combines text search and category filtering.
     * 
     * @param query the search query (can be null or empty for no text filtering)
     * @param categoryId the category ID to filter by (can be null for no category filtering)
     * @return List of matching parts
     */
    public List<Part> searchParts(String query, Integer categoryId) {
        // If both query and categoryId are null/empty, return all parts
        if ((query == null || query.trim().isEmpty()) && categoryId == null) {
            return getAllParts();
        }
        
        // If only categoryId is provided, use category filter
        if (query == null || query.trim().isEmpty()) {
            return filterByCategory(categoryId);
        }
        
        // If only query is provided, use text search
        if (categoryId == null) {
            return searchByText(query);
        }
        
        // Both query and categoryId provided, use combined search
        return searchWithFilters(query, categoryId);
    }
    
    /**
     * Searches for parts by text across name, description, maker, and category fields.
     * Search is case-insensitive and matches partial strings.
     * 
     * @param query the search query
     * @return List of matching parts
     */
    public List<Part> searchByText(String query) {
        List<Part> parts = new ArrayList<>();
        
        if (query == null || query.trim().isEmpty()) {
            return getAllParts();
        }
        
        String sql = "SELECT p.id, p.name, m.name as maker_name, p.description, p.price, p.quantity, " +
                     "p.catid, c.name as category_name, c.description as category_desc " +
                     "FROM parts p " +
                     "JOIN makers m ON p.maker_id = m.id " +
                     "JOIN categories c ON p.catid = c.id " +
                     "WHERE LOWER(p.name) LIKE LOWER(?) " +
                     "OR LOWER(m.name) LIKE LOWER(?) " +
                     "OR LOWER(p.description) LIKE LOWER(?) " +
                     "OR LOWER(c.name) LIKE LOWER(?) " +
                     "ORDER BY p.name";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + query.trim() + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            stmt.setString(4, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Part part = mapResultSetToPart(rs);
                    parts.add(part);
                }
            }
            
            logger.info("Text search for '{}' returned {} parts", query, parts.size());
        } catch (SQLException e) {
            logger.error("Error searching parts by text with query: {}", query, e);
        }
        
        return parts;
    }
    
    /**
     * Filters parts by category only.
     * 
     * @param categoryId the category ID to filter by
     * @return List of parts in the specified category
     */
    public List<Part> filterByCategory(int categoryId) {
        List<Part> parts = new ArrayList<>();
        String sql = "SELECT p.id, p.name, m.name as maker_name, p.description, p.price, p.quantity, " +
                     "p.catid, c.name as category_name, c.description as category_desc " +
                     "FROM parts p " +
                     "JOIN makers m ON p.maker_id = m.id " +
                     "JOIN categories c ON p.catid = c.id " +
                     "WHERE p.catid = ? " +
                     "ORDER BY p.name";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, categoryId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Part part = mapResultSetToPart(rs);
                    parts.add(part);
                }
            }
            
            logger.info("Category filter for category {} returned {} parts", categoryId, parts.size());
        } catch (SQLException e) {
            logger.error("Error filtering parts by category: {}", categoryId, e);
        }
        
        return parts;
    }
    
    /**
     * Searches for parts with both text query and category filter applied.
     * Both conditions must be satisfied for a part to be included in results.
     * 
     * @param query the search query
     * @param categoryId the category ID to filter by
     * @return List of matching parts
     */
    public List<Part> searchWithFilters(String query, Integer categoryId) {
        List<Part> parts = new ArrayList<>();
        
        if (query == null || query.trim().isEmpty()) {
            return filterByCategory(categoryId);
        }
        
        String sql = "SELECT p.id, p.name, m.name as maker_name, p.description, p.price, p.quantity, " +
                     "p.catid, c.name as category_name, c.description as category_desc " +
                     "FROM parts p " +
                     "JOIN makers m ON p.maker_id = m.id " +
                     "JOIN categories c ON p.catid = c.id " +
                     "WHERE (LOWER(p.name) LIKE LOWER(?) " +
                     "OR LOWER(m.name) LIKE LOWER(?) " +
                     "OR LOWER(p.description) LIKE LOWER(?) " +
                     "OR LOWER(c.name) LIKE LOWER(?)) " +
                     "AND p.catid = ? " +
                     "ORDER BY p.name";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + query.trim() + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            stmt.setString(4, searchPattern);
            stmt.setInt(5, categoryId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Part part = mapResultSetToPart(rs);
                    parts.add(part);
                }
            }
            
            logger.info("Combined search for '{}' in category {} returned {} parts", 
                       query, categoryId, parts.size());
        } catch (SQLException e) {
            logger.error("Error searching parts with filters - query: {}, categoryId: {}", 
                        query, categoryId, e);
        }
        
        return parts;
    }
    
    /**
     * Retrieves all parts from the database.
     * Used when no search criteria are specified.
     * 
     * @return List of all parts
     */
    private List<Part> getAllParts() {
        List<Part> parts = new ArrayList<>();
        String sql = "SELECT p.id, p.name, m.name as maker_name, p.description, p.price, p.quantity, " +
                     "p.catid, c.name as category_name, c.description as category_desc " +
                     "FROM parts p " +
                     "JOIN makers m ON p.maker_id = m.id " +
                     "JOIN categories c ON p.catid = c.id " +
                     "ORDER BY p.name";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Part part = mapResultSetToPart(rs);
                parts.add(part);
            }
            
            logger.info("Retrieved {} parts from database", parts.size());
        } catch (SQLException e) {
            logger.error("Error retrieving all parts", e);
        }
        
        return parts;
    }
    
    /**
     * Maps a ResultSet row to a Part object.
     * 
     * @param rs the ResultSet
     * @return the Part object
     * @throws SQLException if a database access error occurs
     */
    private Part mapResultSetToPart(ResultSet rs) throws SQLException {
        // Create category
        Category category = new Category();
        category.setId(rs.getInt("catid"));
        category.setName(rs.getString("category_name"));
        category.setDescription(rs.getString("category_desc"));
        
        // Create part
        Part part = new Part();
        part.setId(rs.getInt("id"));
        part.setName(rs.getString("name"));
        part.setMaker(rs.getString("maker_name"));
        part.setDescription(rs.getString("description"));
        part.setPrice(rs.getFloat("price"));
        part.setQuantity(rs.getInt("quantity"));
        part.setCategory(category);
        
        return part;
    }
}
