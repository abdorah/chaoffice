package org.chaos.office.service;

import org.chaos.office.event.CurrencyChangeEvent;
import org.chaos.office.event.CurrencyChangeListener;
import org.chaos.office.model.CurrencySettings;
import org.chaos.office.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * SettingsService manages application settings including currency configuration.
 * 
 * <p>This service is responsible for:
 * <ul>
 *   <li>Loading currency settings from the database</li>
 *   <li>Caching currency settings for performance</li>
 *   <li>Persisting currency settings to the database</li>
 *   <li>Notifying listeners when currency settings change</li>
 * </ul>
 * 
 * <p>Requirements: 5.7, 5.8
 */
public class SettingsService {
    private static final Logger logger = LoggerFactory.getLogger(SettingsService.class);
    
    private CurrencySettings cachedCurrencySettings;
    private final List<CurrencyChangeListener> currencyChangeListeners = new ArrayList<>();
    
    /**
     * Retrieves currency settings from the database or returns cached settings.
     * If no settings exist in the database, returns default CurrencySettings (USD, $).
     * 
     * @return CurrencySettings object, never null
     */
    public CurrencySettings getCurrencySettings() {
        if (cachedCurrencySettings != null) {
            logger.debug("Returning cached currency settings");
            return cachedCurrencySettings;
        }
        
        cachedCurrencySettings = loadCurrencySettingsFromDatabase();
        return cachedCurrencySettings;
    }
    
    /**
     * Registers a listener to be notified when currency settings change.
     * 
     * @param listener the listener to register (must not be null)
     * @throws IllegalArgumentException if listener is null
     */
    public void addCurrencyChangeListener(CurrencyChangeListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        currencyChangeListeners.add(listener);
        logger.debug("Registered currency change listener: {}", listener.getClass().getSimpleName());
    }
    
    /**
     * Removes a previously registered currency change listener.
     * 
     * @param listener the listener to remove
     */
    public void removeCurrencyChangeListener(CurrencyChangeListener listener) {
        currencyChangeListeners.remove(listener);
        logger.debug("Removed currency change listener: {}", listener.getClass().getSimpleName());
    }
    
    /**
     * Fires a currency change event to all registered listeners.
     * 
     * @param event the currency change event to fire
     */
    private void fireCurrencyChangeEvent(CurrencyChangeEvent event) {
        logger.info("Firing currency change event to {} listeners", currencyChangeListeners.size());
        for (CurrencyChangeListener listener : currencyChangeListeners) {
            try {
                listener.onCurrencyChanged(event);
            } catch (Exception e) {
                logger.error("Error notifying currency change listener: {}", 
                    listener.getClass().getSimpleName(), e);
            }
        }
    }
    
    /**
     * Saves currency settings to the database and updates the cache.
     * Uses INSERT OR REPLACE to handle both insert and update scenarios.
     * Fires a currency change event to notify registered listeners.
     * 
     * @param settings the currency settings to save (must not be null)
     * @throws IllegalArgumentException if settings is null
     * @throws RuntimeException if the save operation fails
     */
    public void saveCurrencySettings(CurrencySettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("Currency settings cannot be null");
        }
        
        // Store old settings for event
        CurrencySettings oldSettings = cachedCurrencySettings;
        
        String sql = "INSERT OR REPLACE INTO currency_settings (id, symbol, acronym) VALUES (1, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, settings.getSymbol());
            stmt.setString(2, settings.getAcronym());
            
            stmt.executeUpdate();
            
            // Update cache
            cachedCurrencySettings = settings;
            
            logger.info("Saved currency settings to database: {} {}", 
                settings.getSymbol(), settings.getAcronym());
            
            // Fire currency change event
            CurrencyChangeEvent event = new CurrencyChangeEvent(oldSettings, settings);
            fireCurrencyChangeEvent(event);
            
        } catch (SQLException e) {
            logger.error("Error saving currency settings to database", e);
            throw new RuntimeException("Failed to save currency settings", e);
        }
    }
    
    /**
     * Loads currency settings from the database.
     * If no settings exist, returns default CurrencySettings (USD, $).
     * 
     * @return CurrencySettings object, never null
     */
    private CurrencySettings loadCurrencySettingsFromDatabase() {
        String sql = "SELECT symbol, acronym FROM currency_settings WHERE id = 1";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                String symbol = rs.getString("symbol");
                String acronym = rs.getString("acronym");
                
                CurrencySettings settings = new CurrencySettings(symbol, acronym);
                logger.info("Loaded currency settings from database: {} {}", symbol, acronym);
                return settings;
            } else {
                // No settings exist, return defaults
                logger.info("No currency settings found, using defaults (USD, $)");
                return new CurrencySettings();
            }
            
        } catch (SQLException e) {
            logger.error("Error loading currency settings from database", e);
            // Return default settings on error
            return new CurrencySettings();
        }
    }
}
