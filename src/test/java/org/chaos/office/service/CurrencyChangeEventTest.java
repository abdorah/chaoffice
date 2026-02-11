package org.chaos.office.service;

import org.chaos.office.event.CurrencyChangeEvent;
import org.chaos.office.event.CurrencyChangeListener;
import org.chaos.office.model.CurrencySettings;
import org.chaos.office.util.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the currency change event system.
 * Validates that listeners are properly notified when currency settings change.
 * 
 * Requirements: 5.8
 */
class CurrencyChangeEventTest {
    
    private SettingsService settingsService;
    
    @BeforeEach
    void setUp() throws Exception {
        DatabaseConnection.getInstance().initializeDatabase();
        settingsService = new SettingsService();
    }
    
    @AfterEach
    void tearDown() throws Exception {
        DatabaseConnection.getInstance().closeConnection();
    }
    
    @Test
    void testCurrencyChangeEventFired() {
        // Given: A listener that tracks if it was called
        AtomicBoolean listenerCalled = new AtomicBoolean(false);
        AtomicReference<CurrencyChangeEvent> receivedEvent = new AtomicReference<>();
        
        CurrencyChangeListener listener = event -> {
            listenerCalled.set(true);
            receivedEvent.set(event);
        };
        
        settingsService.addCurrencyChangeListener(listener);
        
        // When: Currency settings are saved
        CurrencySettings newSettings = new CurrencySettings("€", "EUR");
        settingsService.saveCurrencySettings(newSettings);
        
        // Then: Listener should be called with the event
        assertTrue(listenerCalled.get(), "Listener should have been called");
        assertNotNull(receivedEvent.get(), "Event should not be null");
        assertEquals("€", receivedEvent.get().getNewSettings().getSymbol());
        assertEquals("EUR", receivedEvent.get().getNewSettings().getAcronym());
    }
    
    @Test
    void testMultipleListeners() {
        // Given: Multiple listeners
        AtomicBoolean listener1Called = new AtomicBoolean(false);
        AtomicBoolean listener2Called = new AtomicBoolean(false);
        
        settingsService.addCurrencyChangeListener(event -> listener1Called.set(true));
        settingsService.addCurrencyChangeListener(event -> listener2Called.set(true));
        
        // When: Currency settings are saved
        CurrencySettings newSettings = new CurrencySettings("£", "GBP");
        settingsService.saveCurrencySettings(newSettings);
        
        // Then: Both listeners should be called
        assertTrue(listener1Called.get(), "Listener 1 should have been called");
        assertTrue(listener2Called.get(), "Listener 2 should have been called");
    }
    
    @Test
    void testRemoveListener() {
        // Given: A listener that is registered then removed
        AtomicBoolean listenerCalled = new AtomicBoolean(false);
        CurrencyChangeListener listener = event -> listenerCalled.set(true);
        
        settingsService.addCurrencyChangeListener(listener);
        settingsService.removeCurrencyChangeListener(listener);
        
        // When: Currency settings are saved
        CurrencySettings newSettings = new CurrencySettings("¥", "JPY");
        settingsService.saveCurrencySettings(newSettings);
        
        // Then: Listener should not be called
        assertFalse(listenerCalled.get(), "Removed listener should not be called");
    }
    
    @Test
    void testEventContainsOldAndNewSettings() {
        // Given: Initial currency settings
        CurrencySettings initialSettings = new CurrencySettings("$", "USD");
        settingsService.saveCurrencySettings(initialSettings);
        
        AtomicReference<CurrencyChangeEvent> receivedEvent = new AtomicReference<>();
        settingsService.addCurrencyChangeListener(receivedEvent::set);
        
        // When: Currency settings are changed
        CurrencySettings newSettings = new CurrencySettings("€", "EUR");
        settingsService.saveCurrencySettings(newSettings);
        
        // Then: Event should contain both old and new settings
        assertNotNull(receivedEvent.get());
        assertNotNull(receivedEvent.get().getOldSettings());
        assertEquals("$", receivedEvent.get().getOldSettings().getSymbol());
        assertEquals("USD", receivedEvent.get().getOldSettings().getAcronym());
        assertEquals("€", receivedEvent.get().getNewSettings().getSymbol());
        assertEquals("EUR", receivedEvent.get().getNewSettings().getAcronym());
    }
    
    @Test
    void testHasSymbolChanged() {
        // Given: Initial settings
        CurrencySettings initialSettings = new CurrencySettings("$", "USD");
        settingsService.saveCurrencySettings(initialSettings);
        
        AtomicReference<CurrencyChangeEvent> receivedEvent = new AtomicReference<>();
        settingsService.addCurrencyChangeListener(receivedEvent::set);
        
        // When: Only symbol changes
        CurrencySettings newSettings = new CurrencySettings("€", "USD");
        settingsService.saveCurrencySettings(newSettings);
        
        // Then: hasSymbolChanged should return true
        assertTrue(receivedEvent.get().hasSymbolChanged());
        assertFalse(receivedEvent.get().hasAcronymChanged());
    }
    
    @Test
    void testHasAcronymChanged() {
        // Given: Initial settings
        CurrencySettings initialSettings = new CurrencySettings("$", "USD");
        settingsService.saveCurrencySettings(initialSettings);
        
        AtomicReference<CurrencyChangeEvent> receivedEvent = new AtomicReference<>();
        settingsService.addCurrencyChangeListener(receivedEvent::set);
        
        // When: Only acronym changes
        CurrencySettings newSettings = new CurrencySettings("$", "CAD");
        settingsService.saveCurrencySettings(newSettings);
        
        // Then: hasAcronymChanged should return true
        assertFalse(receivedEvent.get().hasSymbolChanged());
        assertTrue(receivedEvent.get().hasAcronymChanged());
    }
    
    @Test
    void testAddNullListenerThrowsException() {
        // When/Then: Adding null listener should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            settingsService.addCurrencyChangeListener(null);
        });
    }
}
