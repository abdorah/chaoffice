package org.chaos.office.util;

import org.chaos.office.model.CurrencySettings;
import org.chaos.office.service.SettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the CurrencyFormatter utility class.
 * 
 * Requirements: 5.4, 5.5
 */
class CurrencyFormatterTest {

    private SettingsService settingsService;

    @BeforeEach
    void setUp() throws Exception {
        // Create real SettingsService
        settingsService = new SettingsService();
        
        // Initialize database
        DatabaseConnection.getInstance().initializeDatabase();
        
        // Clean up any existing test data
        cleanupTestData();
    }

    @AfterEach
    void tearDown() {
        // Reset the static state by using reflection to set settingsService to null
        try {
            var field = CurrencyFormatter.class.getDeclaredField("settingsService");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
            // Ignore - this is just cleanup
        }
        
        // Clean up test data
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
    void testCurrencyFormatterCannotBeInstantiated() {
        assertThrows(InvocationTargetException.class, () -> {
            var constructor = CurrencyFormatter.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });
    }

    @Test
    void testInitializeWithValidService() {
        assertDoesNotThrow(() -> CurrencyFormatter.initialize(settingsService));
    }

    @Test
    void testInitializeWithNullServiceThrowsException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> CurrencyFormatter.initialize(null)
        );
        assertEquals("SettingsService cannot be null", exception.getMessage());
    }

    @Test
    void testFormatWithoutInitializationThrowsException() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> CurrencyFormatter.format(99.99f)
        );
        assertTrue(exception.getMessage().contains("not been initialized"));
    }

    @Test
    void testGetSymbolWithoutInitializationThrowsException() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> CurrencyFormatter.getSymbol()
        );
        assertTrue(exception.getMessage().contains("not been initialized"));
    }

    @Test
    void testGetAcronymWithoutInitializationThrowsException() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> CurrencyFormatter.getAcronym()
        );
        assertTrue(exception.getMessage().contains("not been initialized"));
    }

    @Test
    void testFormatWithDefaultUSDCurrency() {
        CurrencyFormatter.initialize(settingsService);
        
        String formatted = CurrencyFormatter.format(99.99f);
        
        assertEquals("$99.99", formatted);
    }

    @Test
    void testFormatWithEuroCurrency() {
        CurrencySettings euroSettings = new CurrencySettings("€", "EUR");
        settingsService.saveCurrencySettings(euroSettings);
        CurrencyFormatter.initialize(settingsService);
        
        String formatted = CurrencyFormatter.format(50.50f);
        
        assertEquals("€50.50", formatted);
    }

    @Test
    void testFormatWithPoundCurrency() {
        CurrencySettings poundSettings = new CurrencySettings("£", "GBP");
        settingsService.saveCurrencySettings(poundSettings);
        CurrencyFormatter.initialize(settingsService);
        
        String formatted = CurrencyFormatter.format(123.45f);
        
        assertEquals("£123.45", formatted);
    }

    @Test
    void testFormatWithYenCurrency() {
        CurrencySettings yenSettings = new CurrencySettings("¥", "JPY");
        settingsService.saveCurrencySettings(yenSettings);
        CurrencyFormatter.initialize(settingsService);
        
        String formatted = CurrencyFormatter.format(1000.00f);
        
        assertEquals("¥1000.00", formatted);
    }

    @Test
    void testFormatWithZeroPrice() {
        CurrencyFormatter.initialize(settingsService);
        
        String formatted = CurrencyFormatter.format(0.0f);
        
        assertEquals("$0.00", formatted);
    }

    @Test
    void testFormatWithNegativePrice() {
        CurrencyFormatter.initialize(settingsService);
        
        String formatted = CurrencyFormatter.format(-25.50f);
        
        assertEquals("$-25.50", formatted);
    }

    @Test
    void testFormatWithLargePrice() {
        CurrencyFormatter.initialize(settingsService);
        
        // Use a smaller large number to avoid float precision issues
        String formatted = CurrencyFormatter.format(99999.99f);
        
        assertEquals("$99999.99", formatted);
    }

    @Test
    void testFormatWithSmallPrice() {
        CurrencyFormatter.initialize(settingsService);
        
        String formatted = CurrencyFormatter.format(0.01f);
        
        assertEquals("$0.01", formatted);
    }

    @Test
    void testGetSymbolReturnsCorrectSymbol() {
        CurrencyFormatter.initialize(settingsService);
        
        String symbol = CurrencyFormatter.getSymbol();
        
        assertEquals("$", symbol);
    }

    @Test
    void testGetSymbolWithEuro() {
        CurrencySettings euroSettings = new CurrencySettings("€", "EUR");
        settingsService.saveCurrencySettings(euroSettings);
        CurrencyFormatter.initialize(settingsService);
        
        String symbol = CurrencyFormatter.getSymbol();
        
        assertEquals("€", symbol);
    }

    @Test
    void testGetAcronymReturnsCorrectAcronym() {
        CurrencyFormatter.initialize(settingsService);
        
        String acronym = CurrencyFormatter.getAcronym();
        
        assertEquals("USD", acronym);
    }

    @Test
    void testGetAcronymWithEuro() {
        CurrencySettings euroSettings = new CurrencySettings("€", "EUR");
        settingsService.saveCurrencySettings(euroSettings);
        CurrencyFormatter.initialize(settingsService);
        
        String acronym = CurrencyFormatter.getAcronym();
        
        assertEquals("EUR", acronym);
    }

    @Test
    void testMultipleCallsUseSameSettingsService() {
        CurrencyFormatter.initialize(settingsService);
        
        // Multiple calls should work without errors
        String formatted = CurrencyFormatter.format(10.0f);
        String symbol = CurrencyFormatter.getSymbol();
        String acronym = CurrencyFormatter.getAcronym();
        
        assertEquals("$10.00", formatted);
        assertEquals("$", symbol);
        assertEquals("USD", acronym);
    }

    @Test
    void testFormatWithCustomCurrencySymbol() {
        CurrencySettings rupeeSettings = new CurrencySettings("₹", "INR");
        settingsService.saveCurrencySettings(rupeeSettings);
        CurrencyFormatter.initialize(settingsService);
        
        String formatted = CurrencyFormatter.format(500.00f);
        
        assertEquals("₹500.00", formatted);
    }
}
