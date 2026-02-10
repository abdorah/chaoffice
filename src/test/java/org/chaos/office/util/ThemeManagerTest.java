package org.chaos.office.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ThemeManager
 */
class ThemeManagerTest {
    
    @Test
    void testGetAvailableThemes() {
        ThemeManager.Theme[] themes = ThemeManager.getAvailableThemes();
        
        assertNotNull(themes);
        assertEquals(4, themes.length);
        assertEquals("main", themes[0].getId());
        assertEquals("dark", themes[1].getId());
        assertEquals("ubuntu", themes[2].getId());
        assertEquals("none", themes[3].getId());
    }
    
    @Test
    void testThemeFromId() {
        assertEquals(ThemeManager.Theme.MAIN, ThemeManager.Theme.fromId("main"));
        assertEquals(ThemeManager.Theme.DARK, ThemeManager.Theme.fromId("dark"));
        assertEquals(ThemeManager.Theme.UBUNTU, ThemeManager.Theme.fromId("ubuntu"));
        assertEquals(ThemeManager.Theme.NONE, ThemeManager.Theme.fromId("none"));
        
        // Invalid ID should return MAIN
        assertEquals(ThemeManager.Theme.MAIN, ThemeManager.Theme.fromId("invalid"));
    }
    
    @Test
    void testThemeDisplayNames() {
        assertEquals("Main Theme", ThemeManager.Theme.MAIN.getDisplayName());
        assertEquals("Dark Theme", ThemeManager.Theme.DARK.getDisplayName());
        assertEquals("Ubuntu Theme", ThemeManager.Theme.UBUNTU.getDisplayName());
        assertEquals("No Theme", ThemeManager.Theme.NONE.getDisplayName());
    }
    
    @Test
    void testGetCurrentTheme() {
        String currentTheme = ThemeManager.getCurrentTheme();
        assertNotNull(currentTheme);
        // Should be one of the valid themes
        assertTrue(currentTheme.equals("main") || 
                   currentTheme.equals("dark") || 
                   currentTheme.equals("ubuntu") || 
                   currentTheme.equals("none"));
    }
    
    @Test
    void testSetThemeWithNullScene() {
        // Should not throw exception
        assertDoesNotThrow(() -> ThemeManager.setTheme(null, "main"));
    }
}
