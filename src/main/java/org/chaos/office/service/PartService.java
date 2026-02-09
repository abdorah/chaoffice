package org.chaos.office.service;

import org.chaos.office.model.Category;
import org.chaos.office.model.Part;
import org.chaos.office.util.DatabaseConnection;
import org.chaos.office.util.ValidationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PartService handles CRUD operations for auto parts inventory.
 * 
 * <p>This service is responsible for:
 * <ul>
 *   <li>Managing parts inventory (create, read, update, delete)</li>
 *   <li>Searching and filtering parts</li>
 *   <li>Validating part data before persistence</li>
 * </ul>
 * 
 * <p>Requirements: 4.2, 4.3, 4.5, 4.7, 4.8
 */
public class PartService {
    private static final Logger logger = LoggerFactory.getLogger(PartService.class);
    
    /**
     * Retrieves all parts from the database.
     * 
     * @return List of all parts
     */
    public List<Part> getAllParts() {
        List<Part> parts = new ArrayList<>();
        String sql = "SELECT p.id, p.name, m.name as maker_name, p.description, p.price, p.quantity, " +
                     "p.catid, c.name as category_name, c.description as category_desc " +
                     "FROM parts p " +
                     "JOIN makers m ON p.maker_id = m.id " +
                     "JOIN categories c ON p.catid = c.id";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
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
     * Retrieves a part by its ID.
     * 
     * @param id the part ID
     * @return Optional containing the part if found, empty otherwise
     */
    public Optional<Part> getPartById(int id) {
        String sql = "SELECT p.id, p.name, m.name as maker_name, p.description, p.price, p.quantity, " +
                     "p.catid, c.name as category_name, c.description as category_desc " +
                     "FROM parts p " +
                     "JOIN makers m ON p.maker_id = m.id " +
                     "JOIN categories c ON p.catid = c.id " +
                     "WHERE p.id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Part part = mapResultSetToPart(rs);
                    logger.info("Retrieved part with ID: {}", id);
                    return Optional.of(part);
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving part by ID: {}", id, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Saves a new part to the database.
     * Validates the part data before saving.
     * 
     * @param part the part to save
     * @throws IllegalArgumentException if validation fails
     */
    public void savePart(Part part) {
        // Validate part data
        validatePart(part);
        
        String sql = "INSERT INTO parts (name, maker_id, description, price, quantity, catid) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Get or create maker ID
            int makerId = getOrCreateMakerId(part.getMaker());
            
            stmt.setString(1, part.getName());
            stmt.setInt(2, makerId);
            stmt.setString(3, part.getDescription());
            stmt.setFloat(4, part.getPrice());
            stmt.setInt(5, part.getQuantity());
            stmt.setInt(6, part.getCategory().getId());
            
            stmt.executeUpdate();
            logger.info("Saved new part: {}", part.getName());
        } catch (SQLException e) {
            logger.error("Error saving part: {}", part.getName(), e);
            throw new RuntimeException("Failed to save part", e);
        }
    }
    
    /**
     * Updates an existing part in the database.
     * Validates the part data before updating.
     * 
     * @param part the part to update
     * @throws IllegalArgumentException if validation fails
     */
    public void updatePart(Part part) {
        // Validate part data
        validatePart(part);
        
        String sql = "UPDATE parts SET name = ?, maker_id = ?, description = ?, price = ?, quantity = ?, catid = ? " +
                     "WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Get or create maker ID
            int makerId = getOrCreateMakerId(part.getMaker());
            
            stmt.setString(1, part.getName());
            stmt.setInt(2, makerId);
            stmt.setString(3, part.getDescription());
            stmt.setFloat(4, part.getPrice());
            stmt.setInt(5, part.getQuantity());
            stmt.setInt(6, part.getCategory().getId());
            stmt.setInt(7, part.getId());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Updated part with ID: {}", part.getId());
            } else {
                logger.warn("No part found with ID: {}", part.getId());
            }
        } catch (SQLException e) {
            logger.error("Error updating part with ID: {}", part.getId(), e);
            throw new RuntimeException("Failed to update part", e);
        }
    }
    
    /**
     * Deletes a part from the database.
     * 
     * @param id the ID of the part to delete
     */
    public void deletePart(int id) {
        String sql = "DELETE FROM parts WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                logger.info("Deleted part with ID: {}", id);
            } else {
                logger.warn("No part found with ID: {}", id);
            }
        } catch (SQLException e) {
            logger.error("Error deleting part with ID: {}", id, e);
            throw new RuntimeException("Failed to delete part", e);
        }
    }
    
    /**
     * Searches for parts by name, maker, or description.
     * Search is case-insensitive and matches partial strings.
     * 
     * @param query the search query
     * @return List of matching parts
     */
    public List<Part> searchParts(String query) {
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
                     "OR LOWER(p.description) LIKE LOWER(?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + query + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Part part = mapResultSetToPart(rs);
                    parts.add(part);
                }
            }
            
            logger.info("Search for '{}' returned {} parts", query, parts.size());
        } catch (SQLException e) {
            logger.error("Error searching parts with query: {}", query, e);
        }
        
        return parts;
    }
    
    /**
     * Filters parts by category.
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
                     "WHERE p.catid = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, categoryId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Part part = mapResultSetToPart(rs);
                    parts.add(part);
                }
            }
            
            logger.info("Filter by category {} returned {} parts", categoryId, parts.size());
        } catch (SQLException e) {
            logger.error("Error filtering parts by category: {}", categoryId, e);
        }
        
        return parts;
    }
    
    /**
     * Validates part data.
     * 
     * @param part the part to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validatePart(Part part) {
        if (!ValidationHelper.isNotEmpty(part.getName())) {
            throw new IllegalArgumentException("Part name is required");
        }
        
        if (!ValidationHelper.isNotEmpty(part.getMaker())) {
            throw new IllegalArgumentException("Part maker is required");
        }
        
        if (!ValidationHelper.isNotEmpty(part.getDescription())) {
            throw new IllegalArgumentException("Part description is required");
        }
        
        if (part.getPrice() < 0) {
            throw new IllegalArgumentException("Part price must be non-negative");
        }
        
        if (part.getQuantity() < 0) {
            throw new IllegalArgumentException("Part quantity must be non-negative");
        }
        
        if (part.getCategory() == null) {
            throw new IllegalArgumentException("Part category is required");
        }
    }
    
    /**
     * Gets or creates a maker ID for the given maker name.
     * 
     * @param makerName the maker name
     * @return the maker ID
     */
    private int getOrCreateMakerId(String makerName) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        
        // First, try to find existing maker
        String selectSql = "SELECT id FROM makers WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, makerName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        
        // If not found, create new maker
        String insertSql = "INSERT INTO makers (name) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.setString(1, makerName);
            stmt.executeUpdate();
            
            // Get the last inserted ID
            try (PreparedStatement idStmt = conn.prepareStatement("SELECT last_insert_rowid()");
                 ResultSet rs = idStmt.executeQuery()) {
                if (rs.next()) {
                    int makerId = rs.getInt(1);
                    logger.info("Created new maker: {} with ID: {}", makerName, makerId);
                    return makerId;
                }
            }
        }
        
        throw new SQLException("Failed to get or create maker ID for: " + makerName);
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
