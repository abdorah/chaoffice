package org.chaos.office.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the CurrencySettings model.
 */
class CurrencySettingsTest {

    @Test
    void testDefaultConstructor() {
        CurrencySettings settings = new CurrencySettings();
        
        assertNotNull(settings);
        assertEquals("$", settings.getSymbol());
        assertEquals("USD", settings.getAcronym());
    }

    @Test
    void testParameterizedConstructor() {
        CurrencySettings settings = new CurrencySettings("€", "EUR");
        
        assertEquals("€", settings.getSymbol());
        assertEquals("EUR", settings.getAcronym());
    }

    @Test
    void testSettersAndGetters() {
        CurrencySettings settings = new CurrencySettings();
        
        settings.setSymbol("£");
        settings.setAcronym("GBP");
        
        assertEquals("£", settings.getSymbol());
        assertEquals("GBP", settings.getAcronym());
    }

    @Test
    void testPropertyAccessors() {
        CurrencySettings settings = new CurrencySettings("¥", "JPY");
        
        assertNotNull(settings.symbolProperty());
        assertNotNull(settings.acronymProperty());
        
        assertEquals("¥", settings.symbolProperty().get());
        assertEquals("JPY", settings.acronymProperty().get());
    }

    @Test
    void testPropertyBinding() {
        CurrencySettings settings = new CurrencySettings();
        
        // Test that properties can be bound
        settings.symbolProperty().set("₹");
        assertEquals("₹", settings.getSymbol());
        
        settings.acronymProperty().set("INR");
        assertEquals("INR", settings.getAcronym());
    }

    @Test
    void testFormatPriceWithDefaultCurrency() {
        CurrencySettings settings = new CurrencySettings();
        
        assertEquals("$10.50", settings.formatPrice(10.5f));
        assertEquals("$0.00", settings.formatPrice(0.0f));
        assertEquals("$99.99", settings.formatPrice(99.99f));
    }

    @Test
    void testFormatPriceWithEuro() {
        CurrencySettings settings = new CurrencySettings("€", "EUR");
        
        assertEquals("€10.50", settings.formatPrice(10.5f));
        assertEquals("€0.00", settings.formatPrice(0.0f));
        assertEquals("€99.99", settings.formatPrice(99.99f));
    }

    @Test
    void testFormatPriceWithPound() {
        CurrencySettings settings = new CurrencySettings("£", "GBP");
        
        assertEquals("£10.50", settings.formatPrice(10.5f));
        assertEquals("£0.00", settings.formatPrice(0.0f));
        assertEquals("£99.99", settings.formatPrice(99.99f));
    }

    @Test
    void testFormatPriceWithYen() {
        CurrencySettings settings = new CurrencySettings("¥", "JPY");
        
        assertEquals("¥10.50", settings.formatPrice(10.5f));
        assertEquals("¥0.00", settings.formatPrice(0.0f));
        assertEquals("¥99.99", settings.formatPrice(99.99f));
    }

    @Test
    void testFormatPriceRounding() {
        CurrencySettings settings = new CurrencySettings();
        
        // Test proper rounding to 2 decimal places
        assertEquals("$10.50", settings.formatPrice(10.5f));
        assertEquals("$10.51", settings.formatPrice(10.505f));
        assertEquals("$10.50", settings.formatPrice(10.504f));
    }

    @Test
    void testFormatPriceWithLargeValues() {
        CurrencySettings settings = new CurrencySettings();
        
        assertEquals("$1000.00", settings.formatPrice(1000.0f));
        assertEquals("$9999.99", settings.formatPrice(9999.99f));
    }

    @Test
    void testFormatPriceWithNegativeValues() {
        CurrencySettings settings = new CurrencySettings();
        
        assertEquals("$-10.50", settings.formatPrice(-10.5f));
        assertEquals("$-0.01", settings.formatPrice(-0.01f));
    }

    @Test
    void testFormatPriceWithCustomSymbol() {
        CurrencySettings settings = new CurrencySettings("₹", "INR");
        
        assertEquals("₹10.50", settings.formatPrice(10.5f));
        assertEquals("₹99.99", settings.formatPrice(99.99f));
    }

    @Test
    void testToString() {
        CurrencySettings settings = new CurrencySettings("€", "EUR");
        String result = settings.toString();
        
        assertTrue(result.contains("symbol='€'"));
        assertTrue(result.contains("acronym='EUR'"));
    }

    @Test
    void testEmptyStrings() {
        CurrencySettings settings = new CurrencySettings("", "");
        
        assertEquals("", settings.getSymbol());
        assertEquals("", settings.getAcronym());
        assertEquals("0.00", settings.formatPrice(0.0f));
    }

    @Test
    void testCommonCurrencies() {
        // Test various common currencies
        CurrencySettings usd = new CurrencySettings("$", "USD");
        CurrencySettings eur = new CurrencySettings("€", "EUR");
        CurrencySettings gbp = new CurrencySettings("£", "GBP");
        CurrencySettings jpy = new CurrencySettings("¥", "JPY");
        CurrencySettings inr = new CurrencySettings("₹", "INR");
        
        assertEquals("$", usd.getSymbol());
        assertEquals("€", eur.getSymbol());
        assertEquals("£", gbp.getSymbol());
        assertEquals("¥", jpy.getSymbol());
        assertEquals("₹", inr.getSymbol());
        
        assertEquals("USD", usd.getAcronym());
        assertEquals("EUR", eur.getAcronym());
        assertEquals("GBP", gbp.getAcronym());
        assertEquals("JPY", jpy.getAcronym());
        assertEquals("INR", inr.getAcronym());
    }
}
