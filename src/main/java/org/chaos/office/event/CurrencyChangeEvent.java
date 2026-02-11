package org.chaos.office.event;

import org.chaos.office.model.CurrencySettings;

/**
 * Event fired when currency settings are changed.
 * This event allows views to respond to currency changes and update their displays.
 * 
 * <p>Requirements: 5.8
 */
public class CurrencyChangeEvent {
    private final CurrencySettings oldSettings;
    private final CurrencySettings newSettings;
    
    /**
     * Creates a new currency change event.
     * 
     * @param oldSettings the previous currency settings (may be null if no previous settings)
     * @param newSettings the new currency settings (must not be null)
     */
    public CurrencyChangeEvent(CurrencySettings oldSettings, CurrencySettings newSettings) {
        if (newSettings == null) {
            throw new IllegalArgumentException("New currency settings cannot be null");
        }
        this.oldSettings = oldSettings;
        this.newSettings = newSettings;
    }
    
    /**
     * Gets the previous currency settings.
     * 
     * @return the old currency settings, or null if there were no previous settings
     */
    public CurrencySettings getOldSettings() {
        return oldSettings;
    }
    
    /**
     * Gets the new currency settings.
     * 
     * @return the new currency settings, never null
     */
    public CurrencySettings getNewSettings() {
        return newSettings;
    }
    
    /**
     * Checks if the currency symbol has changed.
     * 
     * @return true if the symbol changed, false otherwise
     */
    public boolean hasSymbolChanged() {
        if (oldSettings == null) {
            return true;
        }
        return !oldSettings.getSymbol().equals(newSettings.getSymbol());
    }
    
    /**
     * Checks if the currency acronym has changed.
     * 
     * @return true if the acronym changed, false otherwise
     */
    public boolean hasAcronymChanged() {
        if (oldSettings == null) {
            return true;
        }
        return !oldSettings.getAcronym().equals(newSettings.getAcronym());
    }
}
