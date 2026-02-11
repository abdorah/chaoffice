package org.chaos.office.event;

/**
 * Listener interface for currency change events.
 * Implement this interface to receive notifications when currency settings change.
 * 
 * <p>Requirements: 5.8
 */
@FunctionalInterface
public interface CurrencyChangeListener {
    /**
     * Called when currency settings have been changed.
     * 
     * @param event the currency change event containing old and new settings
     */
    void onCurrencyChanged(CurrencyChangeEvent event);
}
