package org.chaos.office.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * CurrencySettings model representing currency configuration for the application.
 * Stores currency symbol and acronym for price display and formatting.
 * Uses JavaFX properties for UI binding support.
 */
public class CurrencySettings {
    private final StringProperty symbol;
    private final StringProperty acronym;

    /**
     * Default constructor initializing with default USD currency.
     */
    public CurrencySettings() {
        this.symbol = new SimpleStringProperty("$");
        this.acronym = new SimpleStringProperty("USD");
    }

    /**
     * Constructor with specified currency settings.
     *
     * @param symbol  Currency symbol (e.g., "$", "€", "£")
     * @param acronym Currency acronym (e.g., "USD", "EUR", "GBP")
     */
    public CurrencySettings(String symbol, String acronym) {
        this.symbol = new SimpleStringProperty(symbol);
        this.acronym = new SimpleStringProperty(acronym);
    }

    // Symbol property methods
    public String getSymbol() {
        return symbol.get();
    }

    public void setSymbol(String symbol) {
        this.symbol.set(symbol);
    }

    public StringProperty symbolProperty() {
        return symbol;
    }

    // Acronym property methods
    public String getAcronym() {
        return acronym.get();
    }

    public void setAcronym(String acronym) {
        this.acronym.set(acronym);
    }

    public StringProperty acronymProperty() {
        return acronym;
    }

    /**
     * Formats a price value with the configured currency symbol.
     *
     * @param price The price value to format
     * @return Formatted price string with currency symbol prefix (e.g., "$10.50")
     */
    public String formatPrice(float price) {
        return String.format(java.util.Locale.US, "%s%.2f", getSymbol(), price);
    }

    @Override
    public String toString() {
        return "CurrencySettings{" +
                "symbol='" + getSymbol() + '\'' +
                ", acronym='" + getAcronym() + '\'' +
                '}';
    }
}
