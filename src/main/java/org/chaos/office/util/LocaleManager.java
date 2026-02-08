package org.chaos.office.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/**
 * LocaleManager manages application internationalization and localization.
 * Provides access to localized strings and manages the current application locale.
 * 
 * <p>This class is responsible for:
 * <ul>
 *   <li>Managing the current application locale</li>
 *   <li>Loading ResourceBundles for different languages</li>
 *   <li>Providing translated strings via getString()</li>
 *   <li>Persisting locale preferences</li>
 * </ul>
 * 
 * <p>Supported languages:
 * <ul>
 *   <li>English (en_US)</li>
 *   <li>French (fr)</li>
 *   <li>Arabic (ar)</li>
 * </ul>
 * 
 * <p>Requirements: 9.1, 9.2, 9.4
 */
public class LocaleManager {
    private static final Logger logger = LoggerFactory.getLogger(LocaleManager.class);
    
    // ResourceBundle base name
    private static final String BUNDLE_BASE_NAME = "messages.messages";
    
    // Preferences key for storing locale
    private static final String PREF_LOCALE_LANGUAGE = "locale.language";
    private static final String PREF_LOCALE_COUNTRY = "locale.country";
    
    // Supported locales
    public static final Locale LOCALE_ENGLISH = new Locale("en", "US");
    public static final Locale LOCALE_FRENCH = new Locale("fr", "");
    public static final Locale LOCALE_ARABIC = new Locale("ar", "");
    
    // Current locale and resource bundle
    private static Locale currentLocale;
    private static ResourceBundle bundle;
    
    // Preferences for persisting locale
    private static final Preferences prefs = Preferences.userNodeForPackage(LocaleManager.class);
    
    // Static initializer to load saved locale or default to English
    static {
        loadSavedLocale();
    }
    
    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with static methods only.
     */
    private LocaleManager() {
        // Utility class - no instantiation
    }
    
    /**
     * Sets the application locale and loads the corresponding ResourceBundle.
     * The locale preference is persisted for future application sessions.
     * 
     * @param locale the locale to set (must not be null)
     * @throws IllegalArgumentException if locale is null
     * @throws MissingResourceException if the ResourceBundle cannot be loaded
     */
    public static void setLocale(Locale locale) {
        if (locale == null) {
            throw new IllegalArgumentException("Locale cannot be null");
        }
        
        logger.info("Setting locale to: {} ({})", locale.getDisplayName(), locale);
        
        try {
            // Load the ResourceBundle for the specified locale
            bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale);
            currentLocale = locale;
            
            // Persist the locale preference
            saveLocale(locale);
            
            logger.info("Locale set successfully to: {}", locale);
            
        } catch (MissingResourceException e) {
            logger.error("Failed to load ResourceBundle for locale: {}", locale, e);
            throw new MissingResourceException(
                "ResourceBundle not found for locale: " + locale,
                BUNDLE_BASE_NAME,
                ""
            );
        }
    }
    
    /**
     * Gets a localized string for the specified key.
     * If the key is not found, returns the key itself as a fallback.
     * 
     * @param key the resource key (must not be null)
     * @return the localized string, or the key itself if not found
     * @throws IllegalArgumentException if key is null
     */
    public static String getString(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        
        // Ensure bundle is loaded
        if (bundle == null) {
            logger.warn("ResourceBundle not loaded, loading default locale");
            setLocale(LOCALE_ENGLISH);
        }
        
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            logger.warn("Missing resource key: {}", key);
            // Return the key itself as fallback
            return key;
        }
    }
    
    /**
     * Gets the current application locale.
     * 
     * @return the current locale, never null
     */
    public static Locale getCurrentLocale() {
        // Ensure locale is initialized
        if (currentLocale == null) {
            loadSavedLocale();
        }
        return currentLocale;
    }
    
    /**
     * Loads the saved locale from preferences.
     * If no saved locale exists, defaults to English.
     */
    private static void loadSavedLocale() {
        String language = prefs.get(PREF_LOCALE_LANGUAGE, null);
        String country = prefs.get(PREF_LOCALE_COUNTRY, null);
        
        Locale locale;
        if (language != null) {
            locale = new Locale(language, country != null ? country : "");
            logger.info("Loaded saved locale: {}", locale);
        } else {
            locale = LOCALE_ENGLISH;
            logger.info("No saved locale found, using default: {}", locale);
        }
        
        // Set the locale (this will load the ResourceBundle)
        try {
            setLocale(locale);
        } catch (MissingResourceException e) {
            // If the saved locale fails to load, fall back to English
            logger.warn("Failed to load saved locale, falling back to English", e);
            setLocale(LOCALE_ENGLISH);
        }
    }
    
    /**
     * Saves the locale to preferences for persistence across application sessions.
     * 
     * @param locale the locale to save
     */
    private static void saveLocale(Locale locale) {
        prefs.put(PREF_LOCALE_LANGUAGE, locale.getLanguage());
        prefs.put(PREF_LOCALE_COUNTRY, locale.getCountry());
        logger.debug("Locale preference saved: {} ({})", locale.getLanguage(), locale.getCountry());
    }
    
    /**
     * Checks if a locale is supported by the application.
     * 
     * @param locale the locale to check
     * @return true if the locale is supported, false otherwise
     */
    public static boolean isLocaleSupported(Locale locale) {
        if (locale == null) {
            return false;
        }
        
        String language = locale.getLanguage();
        return "en".equals(language) || "fr".equals(language) || "ar".equals(language);
    }
    
    /**
     * Gets an array of all supported locales.
     * 
     * @return array of supported locales
     */
    public static Locale[] getSupportedLocales() {
        return new Locale[] {
            LOCALE_ENGLISH,
            LOCALE_FRENCH,
            LOCALE_ARABIC
        };
    }
}
