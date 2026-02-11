package org.chaos.office.service;

import javafx.scene.image.Image;
import org.chaos.office.model.BrandingSettings;
import org.chaos.office.util.DatabaseConnection;
import org.chaos.office.util.LocaleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * BrandingService manages store branding settings including store name and logo.
 * 
 * <p>This service is responsible for:
 * <ul>
 *   <li>Loading branding settings from the database</li>
 *   <li>Caching branding settings for performance</li>
 *   <li>Persisting branding settings to the database</li>
 *   <li>Providing application title with fallback to default</li>
 *   <li>Providing application logo with fallback to default</li>
 * </ul>
 * 
 * <p>Requirements: 2.3, 2.4, 2.7, 2.8, 2.9
 */
public class BrandingService {
    private static final Logger logger = LoggerFactory.getLogger(BrandingService.class);
    
    private BrandingSettings cachedSettings;
    
    /**
     * Retrieves branding settings from the database or returns cached settings.
     * If no settings exist in the database, returns an empty BrandingSettings object.
     * 
     * @return BrandingSettings object, never null
     */
    public BrandingSettings getBrandingSettings() {
        if (cachedSettings != null) {
            logger.debug("Returning cached branding settings");
            return cachedSettings;
        }
        
        String sql = "SELECT store_name, logo_image, logo_format FROM branding_settings WHERE id = 1";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                String storeName = rs.getString("store_name");
                byte[] logoImage = rs.getBytes("logo_image");
                String logoFormat = rs.getString("logo_format");
                
                cachedSettings = new BrandingSettings(storeName, logoImage, logoFormat);
                logger.info("Loaded branding settings from database");
            } else {
                // No settings exist, return empty settings
                cachedSettings = new BrandingSettings();
                logger.info("No branding settings found, using defaults");
            }
            
        } catch (SQLException e) {
            logger.error("Error loading branding settings from database", e);
            // Return empty settings on error
            cachedSettings = new BrandingSettings();
        }
        
        return cachedSettings;
    }
    
    /**
     * Saves branding settings to the database and updates the cache.
     * Uses INSERT OR REPLACE to handle both insert and update scenarios.
     * 
     * @param settings the branding settings to save (must not be null)
     * @throws IllegalArgumentException if settings is null
     * @throws RuntimeException if the save operation fails
     */
    public void saveBrandingSettings(BrandingSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("Branding settings cannot be null");
        }
        
        String sql = "INSERT OR REPLACE INTO branding_settings (id, store_name, logo_image, logo_format) VALUES (1, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, settings.getStoreName());
            stmt.setBytes(2, settings.getLogoImage());
            stmt.setString(3, settings.getLogoFormat());
            
            stmt.executeUpdate();
            
            // Update cache
            cachedSettings = settings;
            
            logger.info("Saved branding settings to database");
            
        } catch (SQLException e) {
            logger.error("Error saving branding settings to database", e);
            throw new RuntimeException("Failed to save branding settings", e);
        }
    }
    
    /**
     * Gets the application title with fallback to default.
     * If a store name is configured, returns the store name.
     * Otherwise, returns the default application title from LocaleManager.
     * 
     * @return the application title, never null
     */
    public String getApplicationTitle() {
        BrandingSettings settings = getBrandingSettings();
        
        if (settings.hasStoreName()) {
            logger.debug("Using custom store name as application title");
            return settings.getStoreName();
        }
        
        logger.debug("Using default application title");
        return LocaleManager.getString("app.title");
    }
    
    /**
     * Gets the application logo with fallback to default.
     * If a logo is configured, returns an Image created from the logo bytes.
     * Otherwise, returns the default application logo.
     * 
     * @return the application logo Image, or null if no logo is available
     */
    public Image getApplicationLogo() {
        BrandingSettings settings = getBrandingSettings();
        
        if (settings.hasLogo()) {
            try {
                logger.debug("Using custom logo as application logo");
                return new Image(new ByteArrayInputStream(settings.getLogoImage()));
            } catch (Exception e) {
                logger.error("Error loading custom logo image", e);
                return getDefaultLogo();
            }
        }
        
        logger.debug("Using default application logo");
        return getDefaultLogo();
    }
    
    /**
     * Gets the default application logo.
     * Currently returns null as no default logo resource is configured.
     * 
     * @return the default logo Image, or null if no default logo exists
     */
    private Image getDefaultLogo() {
        // No default logo resource configured in the application
        // Return null to indicate no logo should be displayed
        return null;
    }
}
