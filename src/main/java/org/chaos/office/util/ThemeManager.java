package org.chaos.office.util;

import javafx.scene.Scene;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * ThemeManager - Manages application theme/styling
 * 
 * <p>Provides functionality to:
 * <ul>
 *   <li>Load and apply CSS themes to scenes</li>
 *   <li>Save and retrieve theme preferences</li>
 *   <li>Support multiple themes (main, dark, ubuntu, none)</li>
 * </ul>
 */
public class ThemeManager {
    
    private static final Logger LOGGER = Logger.getLogger(ThemeManager.class.getName());
    private static final String SETTINGS_DIR = System.getProperty("user.home") + "/.chaoffice";
    private static final String SETTINGS_FILE = SETTINGS_DIR + "/settings.properties";
    private static final String THEME_KEY = "app.theme";
    private static final String DEFAULT_THEME = "main";
    
    private static String currentTheme = null;
    
    /**
     * Available themes in the application
     */
    public enum Theme {
        MAIN("main", "Main Theme"),
        DARK("dark", "Dark Theme"),
        UBUNTU("ubuntu", "Ubuntu Theme"),
        NONE("none", "No Theme");
        
        private final String id;
        private final String displayName;
        
        Theme(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
        
        public String getId() {
            return id;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static Theme fromId(String id) {
            for (Theme theme : values()) {
                if (theme.id.equals(id)) {
                    return theme;
                }
            }
            return MAIN;
        }
    }
    
    /**
     * Gets the current theme
     * 
     * @return the current theme ID
     */
    public static String getCurrentTheme() {
        if (currentTheme == null) {
            currentTheme = loadThemePreference();
        }
        return currentTheme;
    }
    
    /**
     * Sets and applies a theme to a scene
     * 
     * @param scene the scene to apply the theme to
     * @param themeId the theme ID (main, dark, ubuntu, none)
     */
    public static void setTheme(Scene scene, String themeId) {
        if (scene == null) {
            LOGGER.warning("Cannot apply theme to null scene");
            return;
        }
        
        // Remove all existing stylesheets
        scene.getStylesheets().clear();
        
        // Apply new theme
        String cssPath = getThemePath(themeId);
        if (cssPath != null) {
            scene.getStylesheets().add(cssPath);
            LOGGER.info("Applied theme: " + themeId);
        } else {
            LOGGER.warning("Theme not found: " + themeId + ", using default");
            String defaultPath = getThemePath(DEFAULT_THEME);
            if (defaultPath != null) {
                scene.getStylesheets().add(defaultPath);
            }
        }
        
        currentTheme = themeId;
        saveThemePreference(themeId);
    }
    
    /**
     * Applies the current saved theme to a scene
     * 
     * @param scene the scene to apply the theme to
     */
    public static void applyCurrentTheme(Scene scene) {
        String theme = getCurrentTheme();
        setTheme(scene, theme);
    }
    
    /**
     * Gets the CSS file path for a theme
     * 
     * @param themeId the theme ID
     * @return the CSS file path or null if not found
     */
    private static String getThemePath(String themeId) {
        try {
            String path = "/style/" + themeId + ".css";
            if (ThemeManager.class.getResource(path) != null) {
                return ThemeManager.class.getResource(path).toExternalForm();
            }
        } catch (Exception e) {
            LOGGER.warning("Error loading theme: " + themeId + " - " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Loads the saved theme preference from settings file
     * 
     * @return the saved theme ID or default theme
     */
    private static String loadThemePreference() {
        Properties props = new Properties();
        File settingsFile = new File(SETTINGS_FILE);
        
        if (settingsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(settingsFile)) {
                props.load(fis);
                String theme = props.getProperty(THEME_KEY, DEFAULT_THEME);
                LOGGER.info("Loaded theme preference: " + theme);
                return theme;
            } catch (IOException e) {
                LOGGER.warning("Failed to load theme preference: " + e.getMessage());
            }
        }
        
        return DEFAULT_THEME;
    }
    
    /**
     * Saves the theme preference to settings file
     * 
     * @param themeId the theme ID to save
     */
    private static void saveThemePreference(String themeId) {
        Properties props = new Properties();
        File settingsDir = new File(SETTINGS_DIR);
        File settingsFile = new File(SETTINGS_FILE);
        
        // Create settings directory if it doesn't exist
        if (!settingsDir.exists()) {
            settingsDir.mkdirs();
        }
        
        // Load existing properties
        if (settingsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(settingsFile)) {
                props.load(fis);
            } catch (IOException e) {
                LOGGER.warning("Failed to load existing settings: " + e.getMessage());
            }
        }
        
        // Update theme property
        props.setProperty(THEME_KEY, themeId);
        
        // Save properties
        try (FileOutputStream fos = new FileOutputStream(settingsFile)) {
            props.store(fos, "ChaOffice Application Settings");
            LOGGER.info("Saved theme preference: " + themeId);
        } catch (IOException e) {
            LOGGER.warning("Failed to save theme preference: " + e.getMessage());
        }
    }
    
    /**
     * Gets all available themes
     * 
     * @return array of all themes
     */
    public static Theme[] getAvailableThemes() {
        return Theme.values();
    }
}
