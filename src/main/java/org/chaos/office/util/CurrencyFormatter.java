package org.chaos.office.util;

import org.chaos.office.model.CurrencySettings;
import org.chaos.office.service.SettingsService;

/**
 * CurrencyFormatter is a utility class that provides static methods for formatting
 * currency values throughout the application.
 * 
 * <p>This class acts as a facade to the SettingsService, providing convenient
 * static methods for currency formatting without requiring direct access to the
 * SettingsService instance in every view and controller.
 * 
 * <p>Usage:
 * <pre>
 * // Initialize once at application startup
 * CurrencyFormatter.initialize(settingsService);
 * 
 * // Use throughout the application
 * String formattedPrice = CurrencyFormatter.format(99.99f);  // "$99.99"
 * String symbol = CurrencyFormatter.getSymbol();              // "$"
 * String acronym = CurrencyFormatter.getAcronym();            // "USD"
 * </pre>
 * 
 * <p>Requirements: 5.4, 5.5
 */
public class CurrencyFormatter {
    private static SettingsService settingsService;
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private CurrencyFormatter() {
        throw new UnsupportedOperationException("CurrencyFormatter is a utility class and cannot be instantiated");
    }
    
    /**
     * Initializes the CurrencyFormatter with a SettingsService instance.
     * This method must be called once at application startup before using
     * any other methods in this class.
     * 
     * @param service the SettingsService instance to use for currency settings
     * @throws IllegalArgumentException if service is null
     */
    public static void initialize(SettingsService service) {
        if (service == null) {
            throw new IllegalArgumentException("SettingsService cannot be null");
        }
        settingsService = service;
    }
    
    /**
     * Formats a price value with the configured currency symbol.
     * 
     * @param price the price value to format
     * @return formatted price string with currency symbol prefix (e.g., "$99.99")
     * @throws IllegalStateException if the formatter has not been initialized
     */
    public static String format(float price) {
        ensureInitialized();
        CurrencySettings settings = settingsService.getCurrencySettings();
        return settings.formatPrice(price);
    }
    
    /**
     * Returns the configured currency symbol.
     * 
     * @return the currency symbol (e.g., "$", "€", "£")
     * @throws IllegalStateException if the formatter has not been initialized
     */
    public static String getSymbol() {
        ensureInitialized();
        CurrencySettings settings = settingsService.getCurrencySettings();
        return settings.getSymbol();
    }
    
    /**
     * Returns the configured currency acronym.
     * 
     * @return the currency acronym (e.g., "USD", "EUR", "GBP")
     * @throws IllegalStateException if the formatter has not been initialized
     */
    public static String getAcronym() {
        ensureInitialized();
        CurrencySettings settings = settingsService.getCurrencySettings();
        return settings.getAcronym();
    }
    
    /**
     * Ensures that the formatter has been initialized before use.
     * 
     * @throws IllegalStateException if the formatter has not been initialized
     */
    private static void ensureInitialized() {
        if (settingsService == null) {
            throw new IllegalStateException(
                "CurrencyFormatter has not been initialized. Call initialize(SettingsService) first.");
        }
    }
}
