package org.chaos.office.service;

import org.chaos.office.model.CurrencySettings;
import org.chaos.office.util.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SettingsService.
 * Tests currency settings persistence, retrieval, and caching behavior.
 */
class SettingsServiceTest {
    
    private SettingsService settingsService;
    
    @BeforeEach
    void setUp() throws Exception {
        settingsService = new SettingsService();
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
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM currency_settings WHERE id = 1")) {
            stmt.executeUpdate();
        }
    }
    
    @Test
    void testGetCurrencySettings_WhenNoSettingsExist_ReturnsDefaultSettings() {
        CurrencySettings settings = settingsService.getCurrencySettings();
        
        assertNotNull(settings);
        assertEquals("$", settings.getSymbol());
        assertEquals("USD", settings.getAcronym());
    }
    
    @Test
    void testSaveAndRetrieveCurrencySettings() {
        CurrencySettings settings = new CurrencySettings("€", "EUR");
        
        settingsService.saveCurrencySettings(settings);
        
        CurrencySettings retrieved = settingsService.getCurrencySettings();
        
        assertNotNull(retrieved);
        assertEquals("€", retrieved.getSymbol());
        assertEquals("EUR", retrieved.getAcronym());
    }
    
    @Test
    void testSaveCurrencySettings_WithNullSettings_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            settingsService.saveCurrencySettings(null);
        });
    }
    
    @Test
    void testCurrencySettingsCaching() {
        CurrencySettings settings = new CurrencySettings("£", "GBP");
        
        settingsService.saveCurrencySettings(settings);
        
        CurrencySettings first = settingsService.getCurrencySettings();
        CurrencySettings second = settingsService.getCurrencySettings();
        
        assertSame(first, second, "Should return cached instance");
    }
    
    @Test
    void testUpdateCurrencySettings() {
        CurrencySettings original = new CurrencySettings("$", "USD");
        settingsService.saveCurrencySettings(original);
        
        CurrencySettings updated = new CurrencySettings("¥", "JPY");
        settingsService.saveCurrencySettings(updated);
        
        CurrencySettings retrieved = settingsService.getCurrencySettings();
        
        assertEquals("¥", retrieved.getSymbol());
        assertEquals("JPY", retrieved.getAcronym());
    }
    
    @Test
    void testSaveCurrencySettings_WithCustomSymbol() {
        CurrencySettings settings = new CurrencySettings("₹", "INR");
        
        settingsService.saveCurrencySettings(settings);
        
        CurrencySettings retrieved = settingsService.getCurrencySettings();
        
        assertEquals("₹", retrieved.getSymbol());
        assertEquals("INR", retrieved.getAcronym());
    }
    
    @Test
    void testFormatPrice_WithDifferentCurrencies() {
        // Test with Euro
        CurrencySettings euroSettings = new CurrencySettings("€", "EUR");
        settingsService.saveCurrencySettings(euroSettings);
        
        CurrencySettings retrieved = settingsService.getCurrencySettings();
        String formatted = retrieved.formatPrice(123.45f);
        
        assertEquals("€123.45", formatted);
        
        // Test with Pound
        CurrencySettings poundSettings = new CurrencySettings("£", "GBP");
        settingsService.saveCurrencySettings(poundSettings);
        
        retrieved = settingsService.getCurrencySettings();
        formatted = retrieved.formatPrice(99.99f);
        
        assertEquals("£99.99", formatted);
    }
}
