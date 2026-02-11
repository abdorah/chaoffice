package org.chaos.office.service;

import javafx.scene.image.Image;
import org.chaos.office.model.BrandingSettings;
import org.chaos.office.util.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BrandingService.
 * Tests branding settings persistence, retrieval, and fallback behavior.
 */
class BrandingServiceTest {
    
    private BrandingService brandingService;
    
    @BeforeEach
    void setUp() throws Exception {
        brandingService = new BrandingService();
        DatabaseConnection.getInstance().initializeDatabase();
        cleanupTestData();
    }
    
    @AfterEach
    void tearDown() {
        try {
            cleanupTestData();
        } catch (SQLException e) {
            // Ignore cleanup errors
        }
    }
    
    private void cleanupTestData() throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM branding_settings WHERE id = 1")) {
            stmt.executeUpdate();
        }
    }
    
    @Test
    void testGetBrandingSettings_WhenNoSettingsExist_ReturnsEmptySettings() {
        BrandingSettings settings = brandingService.getBrandingSettings();
        
        assertNotNull(settings);
        assertFalse(settings.hasStoreName());
        assertFalse(settings.hasLogo());
    }
    
    @Test
    void testSaveAndRetrieveBrandingSettings() {
        BrandingSettings settings = new BrandingSettings();
        settings.setStoreName("Test Store");
        settings.setLogoImage(new byte[]{1, 2, 3, 4, 5});
        settings.setLogoFormat("PNG");
        
        brandingService.saveBrandingSettings(settings);
        
        BrandingSettings retrieved = brandingService.getBrandingSettings();
        
        assertNotNull(retrieved);
        assertEquals("Test Store", retrieved.getStoreName());
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, retrieved.getLogoImage());
        assertEquals("PNG", retrieved.getLogoFormat());
        assertTrue(retrieved.hasStoreName());
        assertTrue(retrieved.hasLogo());
    }
    
    @Test
    void testSaveBrandingSettings_WithNullSettings_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            brandingService.saveBrandingSettings(null);
        });
    }
    
    @Test
    void testGetApplicationTitle_WithStoreName_ReturnsStoreName() {
        BrandingSettings settings = new BrandingSettings();
        settings.setStoreName("My Custom Store");
        
        brandingService.saveBrandingSettings(settings);
        
        String title = brandingService.getApplicationTitle();
        
        assertEquals("My Custom Store", title);
    }
    
    @Test
    void testGetApplicationTitle_WithoutStoreName_ReturnsDefaultTitle() {
        String title = brandingService.getApplicationTitle();
        
        assertNotNull(title);
        assertFalse(title.isEmpty(), "Title should not be empty");
    }
    
    @Test
    void testGetApplicationLogo_WithoutLogo_ReturnsNull() {
        Image logo = brandingService.getApplicationLogo();
        
        assertNull(logo);
    }
    
    @Test
    void testBrandingSettingsCaching() {
        BrandingSettings settings = new BrandingSettings();
        settings.setStoreName("Cached Store");
        
        brandingService.saveBrandingSettings(settings);
        
        BrandingSettings first = brandingService.getBrandingSettings();
        BrandingSettings second = brandingService.getBrandingSettings();
        
        assertSame(first, second, "Should return cached instance");
    }
    
    @Test
    void testUpdateBrandingSettings() {
        BrandingSettings settings = new BrandingSettings();
        settings.setStoreName("Original Store");
        brandingService.saveBrandingSettings(settings);
        
        BrandingSettings updated = new BrandingSettings();
        updated.setStoreName("Updated Store");
        updated.setLogoImage(new byte[]{10, 20, 30});
        updated.setLogoFormat("JPG");
        
        brandingService.saveBrandingSettings(updated);
        
        BrandingSettings retrieved = brandingService.getBrandingSettings();
        
        assertEquals("Updated Store", retrieved.getStoreName());
        assertArrayEquals(new byte[]{10, 20, 30}, retrieved.getLogoImage());
        assertEquals("JPG", retrieved.getLogoFormat());
    }
}
