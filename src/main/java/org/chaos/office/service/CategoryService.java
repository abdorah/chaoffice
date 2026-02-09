package org.chaos.office.service;

import org.chaos.office.model.Category;
import org.chaos.office.util.DatabaseConnection;
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
 * CategoryService handles CRUD operations for part categories.
 * 
 * <p>This service is responsible for:
 * <ul>
 *   <li>Managing categories (create, read, update, delete)</li>
 *   <li>Checking for dependent parts before deletion</li>
 *   <li>Managing category icons (images)</li>
 * </ul>
 * 
 * <p>Requirements: 5.3, 5.5
 */
public class CategoryService {
    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);
    
    /**
     * Retrieves all categories from the database.
     * 
     * @return List of all categories
     */
    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT id, name, description, image FROM categories";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Category category = mapResultSetToCategory(rs);
                categories.add(category);
            }
            
            logger.info("Retrieved {} categories from database", categories.size());
        } catch (SQLException e) {
            logger.error("Error retrieving all categories", e);
        }
        
        return categories;
    }
    
    /**
     * Retrieves a category by its ID.
     * 
     * @param id the category ID
     * @return Optional containing the category if found, empty otherwise
     */
    public Optional<Category> getCategoryById(int id) {
        String sql = "SELECT id, name, description, image FROM categories WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Category category = mapResultSetToCategory(rs);
                    logger.info("Retrieved category with ID: {}", id);
                    return Optional.of(category);
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving category by ID: {}", id, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Saves a new category to the database.
     * 
     * @param category the category to save
     */
    public void saveCategory(Category category) {
        String sql = "INSERT INTO categories (name, description, image) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getDescription());
            stmt.setBytes(3, category.getImage());
            
            stmt.executeUpdate();
            logger.info("Saved new category: {}", category.getName());
        } catch (SQLException e) {
            logger.error("Error saving category: {}", category.getName(), e);
            throw new RuntimeException("Failed to save category", e);
        }
    }
    
    /**
     * Updates an existing category in the database.
     * 
     * @param category the category to update
     */
    public void updateCategory(Category category) {
        String sql = "UPDATE categories SET name = ?, description = ?, image = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getDescription());
            stmt.setBytes(3, category.getImage());
            stmt.setInt(4, category.getId());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Updated category with ID: {}", category.getId());
            } else {
                logger.warn("No category found with ID: {}", category.getId());
            }
        } catch (SQLException e) {
            logger.error("Error updating category with ID: {}", category.getId(), e);
            throw new RuntimeException("Failed to update category", e);
        }
    }
    
    /**
     * Deletes a category from the database.
     * Prevents deletion if the category has parts assigned to it.
     * 
     * @param id the ID of the category to delete
     * @throws IllegalStateException if the category has parts assigned to it
     */
    public void deleteCategory(int id) {
        // Check if category has parts
        if (hasParts(id)) {
            logger.warn("Cannot delete category with ID {} - has parts assigned", id);
            throw new IllegalStateException("Cannot delete category with parts assigned to it");
        }
        
        String sql = "DELETE FROM categories WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                logger.info("Deleted category with ID: {}", id);
            } else {
                logger.warn("No category found with ID: {}", id);
            }
        } catch (SQLException e) {
            logger.error("Error deleting category with ID: {}", id, e);
            throw new RuntimeException("Failed to delete category", e);
        }
    }
    
    /**
     * Checks if a category has parts assigned to it.
     * 
     * @param categoryId the category ID to check
     * @return true if the category has parts, false otherwise
     */
    public boolean hasParts(int categoryId) {
        String sql = "SELECT COUNT(*) FROM parts WHERE catid = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, categoryId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    logger.debug("Category {} has {} parts", categoryId, count);
                    return count > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("Error checking if category has parts: {}", categoryId, e);
        }
        
        return false;
    }
    
    /**
     * Maps a ResultSet row to a Category object.
     * 
     * @param rs the ResultSet
     * @return the Category object
     * @throws SQLException if a database access error occurs
     */
    private Category mapResultSetToCategory(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setId(rs.getInt("id"));
        category.setName(rs.getString("name"));
        category.setDescription(rs.getString("description"));
        category.setImage(rs.getBytes("image"));
        
        return category;
    }
}
