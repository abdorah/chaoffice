package org.chaos.office.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.MissingResourceException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LocaleManager utility class.
 * Tests locale management, ResourceBundle loading, and string retrieval.
 * 
 * Requirements: 9.1, 9.2, 9.4
 */
class LocaleManagerTest {

    @BeforeEach
    void setUp() {
        // Reset to English before each test
        LocaleManager.setLocale(LocaleManager.LOCALE_ENGLISH);
    }

    @Test
    void testSetLocaleEnglish() {
        // Act
        LocaleManager.setLocale(LocaleManager.LOCALE_ENGLISH);
        
        // Assert
        assertEquals(LocaleManager.LOCALE_ENGLISH, LocaleManager.getCurrentLocale());
        assertEquals("en", LocaleManager.getCurrentLocale().getLanguage());
        assertEquals("US", LocaleManager.getCurrentLocale().getCountry());
    }

    @Test
    void testSetLocaleFrench() {
        // Act
        LocaleManager.setLocale(LocaleManager.LOCALE_FRENCH);
        
        // Assert
        assertEquals(LocaleManager.LOCALE_FRENCH, LocaleManager.getCurrentLocale());
        assertEquals("fr", LocaleManager.getCurrentLocale().getLanguage());
    }

    @Test
    void testSetLocaleArabic() {
        // Act
        LocaleManager.setLocale(LocaleManager.LOCALE_ARABIC);
        
        // Assert
        assertEquals(LocaleManager.LOCALE_ARABIC, LocaleManager.getCurrentLocale());
        assertEquals("ar", LocaleManager.getCurrentLocale().getLanguage());
    }

    @Test
    void testSetLocaleNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            LocaleManager.setLocale(null);
        });
    }

    @Test
    void testGetStringEnglish() {
        // Arrange
        LocaleManager.setLocale(LocaleManager.LOCALE_ENGLISH);
        
        // Act
        String title = LocaleManager.getString("app.title");
        String welcome = LocaleManager.getString("app.welcome");
        
        // Assert
        assertNotNull(title);
        assertNotNull(welcome);
        assertTrue(title.contains("ChaOffice"));
        assertTrue(welcome.contains("Welcome"));
    }

    @Test
    void testGetStringFrench() {
        // Arrange
        LocaleManager.setLocale(LocaleManager.LOCALE_FRENCH);
        
        // Act
        String title = LocaleManager.getString("app.title");
        String welcome = LocaleManager.getString("app.welcome");
        
        // Assert
        assertNotNull(title);
        assertNotNull(welcome);
        assertTrue(title.contains("ChaOffice"));
        assertTrue(welcome.contains("Bienvenue"));
    }

    @Test
    void testGetStringArabic() {
        // Arrange
        LocaleManager.setLocale(LocaleManager.LOCALE_ARABIC);
        
        // Act
        String title = LocaleManager.getString("app.title");
        String welcome = LocaleManager.getString("app.welcome");
        
        // Assert
        assertNotNull(title);
        assertNotNull(welcome);
        assertTrue(title.contains("تشاأوفيس"));
        assertTrue(welcome.contains("مرحباً"));
    }

    @Test
    void testGetStringMissingKey() {
        // Arrange
        LocaleManager.setLocale(LocaleManager.LOCALE_ENGLISH);
        String missingKey = "this.key.does.not.exist";
        
        // Act
        String result = LocaleManager.getString(missingKey);
        
        // Assert - should return the key itself as fallback
        assertEquals(missingKey, result);
    }

    @Test
    void testGetStringNullKey() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            LocaleManager.getString(null);
        });
    }

    @Test
    void testGetCurrentLocaleNotNull() {
        // Act
        Locale locale = LocaleManager.getCurrentLocale();
        
        // Assert
        assertNotNull(locale);
    }

    @Test
    void testLocaleSwitching() {
        // Test switching between locales
        LocaleManager.setLocale(LocaleManager.LOCALE_ENGLISH);
        String englishOk = LocaleManager.getString("common.ok");
        assertEquals("OK", englishOk);
        
        LocaleManager.setLocale(LocaleManager.LOCALE_FRENCH);
        String frenchOk = LocaleManager.getString("common.ok");
        assertEquals("OK", frenchOk); // OK is same in both languages
        
        LocaleManager.setLocale(LocaleManager.LOCALE_ARABIC);
        String arabicOk = LocaleManager.getString("common.ok");
        assertEquals("موافق", arabicOk);
    }

    @Test
    void testCommonStringsExist() {
        // Arrange
        LocaleManager.setLocale(LocaleManager.LOCALE_ENGLISH);
        
        // Act & Assert - verify common strings exist
        assertNotNull(LocaleManager.getString("common.ok"));
        assertNotNull(LocaleManager.getString("common.cancel"));
        assertNotNull(LocaleManager.getString("common.save"));
        assertNotNull(LocaleManager.getString("common.delete"));
        assertNotNull(LocaleManager.getString("common.edit"));
        assertNotNull(LocaleManager.getString("common.add"));
    }

    @Test
    void testLoginStringsExist() {
        // Arrange
        LocaleManager.setLocale(LocaleManager.LOCALE_ENGLISH);
        
        // Act & Assert - verify login strings exist
        assertNotNull(LocaleManager.getString("login.title"));
        assertNotNull(LocaleManager.getString("login.username"));
        assertNotNull(LocaleManager.getString("login.password"));
        assertNotNull(LocaleManager.getString("login.button"));
    }

    @Test
    void testIsLocaleSupportedEnglish() {
        assertTrue(LocaleManager.isLocaleSupported(LocaleManager.LOCALE_ENGLISH));
    }

    @Test
    void testIsLocaleSupportedFrench() {
        assertTrue(LocaleManager.isLocaleSupported(LocaleManager.LOCALE_FRENCH));
    }

    @Test
    void testIsLocaleSupportedArabic() {
        assertTrue(LocaleManager.isLocaleSupported(LocaleManager.LOCALE_ARABIC));
    }

    @Test
    void testIsLocaleSupportedUnsupported() {
        Locale german = new Locale("de", "DE");
        assertFalse(LocaleManager.isLocaleSupported(german));
    }

    @Test
    void testIsLocaleSupportedNull() {
        assertFalse(LocaleManager.isLocaleSupported(null));
    }

    @Test
    void testGetSupportedLocales() {
        // Act
        Locale[] locales = LocaleManager.getSupportedLocales();
        
        // Assert
        assertNotNull(locales);
        assertEquals(3, locales.length);
        assertTrue(containsLocale(locales, LocaleManager.LOCALE_ENGLISH));
        assertTrue(containsLocale(locales, LocaleManager.LOCALE_FRENCH));
        assertTrue(containsLocale(locales, LocaleManager.LOCALE_ARABIC));
    }

    @Test
    void testAllLanguagesHaveAppTitle() {
        // Test that all supported languages have the app.title key
        for (Locale locale : LocaleManager.getSupportedLocales()) {
            LocaleManager.setLocale(locale);
            String title = LocaleManager.getString("app.title");
            assertNotNull(title);
            assertFalse(title.isEmpty());
            assertTrue(title.contains("ChaOffice") || title.contains("تشاأوفيس"));
        }
    }

    @Test
    void testAllLanguagesHaveCommonStrings() {
        // Test that all supported languages have common strings
        String[] commonKeys = {
            "common.ok", "common.cancel", "common.save", 
            "common.delete", "common.edit", "common.add"
        };
        
        for (Locale locale : LocaleManager.getSupportedLocales()) {
            LocaleManager.setLocale(locale);
            for (String key : commonKeys) {
                String value = LocaleManager.getString(key);
                assertNotNull(value, "Missing key: " + key + " for locale: " + locale);
                assertFalse(value.isEmpty(), "Empty value for key: " + key + " for locale: " + locale);
            }
        }
    }

    /**
     * Helper method to check if an array contains a specific locale.
     */
    private boolean containsLocale(Locale[] locales, Locale target) {
        for (Locale locale : locales) {
            if (locale.equals(target)) {
                return true;
            }
        }
        return false;
    }
}
