package org.chaos.office.model;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

/**
 * Bill model representing a sales transaction record.
 * Uses JavaFX properties for UI binding support.
 */
public class Bill {
    private final IntegerProperty id;
    private final StringProperty clientName;
    private final StringProperty clientPhone;
    private final FloatProperty totalPrice;
    private final ObjectProperty<LocalDate> date;
    
    // POS properties
    private final FloatProperty subtotal;
    private final StringProperty discountType;  // "none", "percentage", "fixed"
    private final FloatProperty discountValue;
    private final StringProperty paymentMethod; // "cash", "card", "check"

    /**
     * Default constructor initializing all properties.
     */
    public Bill() {
        this.id = new SimpleIntegerProperty();
        this.clientName = new SimpleStringProperty();
        this.clientPhone = new SimpleStringProperty();
        this.totalPrice = new SimpleFloatProperty();
        this.date = new SimpleObjectProperty<>();
        
        // Initialize POS properties with defaults
        this.subtotal = new SimpleFloatProperty(0.0f);
        this.discountType = new SimpleStringProperty("none");
        this.discountValue = new SimpleFloatProperty(0.0f);
        this.paymentMethod = new SimpleStringProperty("cash");
        
        // Add listeners for automatic recalculation
        this.subtotal.addListener((obs, oldVal, newVal) -> calculateFinalTotal());
        this.discountType.addListener((obs, oldVal, newVal) -> calculateFinalTotal());
        this.discountValue.addListener((obs, oldVal, newVal) -> calculateFinalTotal());
    }

    /**
     * Constructor with all fields.
     *
     * @param id          Bill ID
     * @param clientName  Client's name
     * @param clientPhone Client's phone number
     * @param totalPrice  Total price of the bill (must be non-negative)
     * @param date        Date of the transaction
     */
    public Bill(int id, String clientName, String clientPhone, float totalPrice, LocalDate date) {
        this.id = new SimpleIntegerProperty(id);
        this.clientName = new SimpleStringProperty(clientName);
        this.clientPhone = new SimpleStringProperty(clientPhone);
        this.totalPrice = new SimpleFloatProperty(totalPrice);
        this.date = new SimpleObjectProperty<>(date);
        
        // Initialize POS properties with defaults
        this.subtotal = new SimpleFloatProperty(0.0f);
        this.discountType = new SimpleStringProperty("none");
        this.discountValue = new SimpleFloatProperty(0.0f);
        this.paymentMethod = new SimpleStringProperty("cash");
        
        // Add listeners for automatic recalculation
        this.subtotal.addListener((obs, oldVal, newVal) -> calculateFinalTotal());
        this.discountType.addListener((obs, oldVal, newVal) -> calculateFinalTotal());
        this.discountValue.addListener((obs, oldVal, newVal) -> calculateFinalTotal());
    }

    // ID property methods
    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    // ClientName property methods
    public String getClientName() {
        return clientName.get();
    }

    public void setClientName(String clientName) {
        this.clientName.set(clientName);
    }

    public StringProperty clientNameProperty() {
        return clientName;
    }

    // ClientPhone property methods
    public String getClientPhone() {
        return clientPhone.get();
    }

    public void setClientPhone(String clientPhone) {
        this.clientPhone.set(clientPhone);
    }

    public StringProperty clientPhoneProperty() {
        return clientPhone;
    }

    // TotalPrice property methods
    public float getTotalPrice() {
        return totalPrice.get();
    }

    public void setTotalPrice(float totalPrice) {
        this.totalPrice.set(totalPrice);
    }

    public FloatProperty totalPriceProperty() {
        return totalPrice;
    }

    // Date property methods
    public LocalDate getDate() {
        return date.get();
    }

    public void setDate(LocalDate date) {
        this.date.set(date);
    }

    public ObjectProperty<LocalDate> dateProperty() {
        return date;
    }

    // Subtotal property methods
    public float getSubtotal() {
        return subtotal.get();
    }

    public void setSubtotal(float subtotal) {
        this.subtotal.set(subtotal);
    }

    public FloatProperty subtotalProperty() {
        return subtotal;
    }

    // DiscountType property methods
    public String getDiscountType() {
        return discountType.get();
    }

    public void setDiscountType(String discountType) {
        this.discountType.set(discountType);
    }

    public StringProperty discountTypeProperty() {
        return discountType;
    }

    // DiscountValue property methods
    public float getDiscountValue() {
        return discountValue.get();
    }

    public void setDiscountValue(float discountValue) {
        this.discountValue.set(discountValue);
    }

    public FloatProperty discountValueProperty() {
        return discountValue;
    }

    // PaymentMethod property methods
    public String getPaymentMethod() {
        return paymentMethod.get();
    }

    public void setPaymentMethod(String paymentMethod) {
        if (!isValidPaymentMethod(paymentMethod)) {
            throw new IllegalArgumentException("Invalid payment method: " + paymentMethod + 
                ". Must be 'cash', 'card', or 'check'.");
        }
        this.paymentMethod.set(paymentMethod);
    }

    public StringProperty paymentMethodProperty() {
        return paymentMethod;
    }

    /**
     * Validates if the payment method is one of the allowed values.
     *
     * @param method Payment method to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidPaymentMethod(String method) {
        return "cash".equals(method) || "card".equals(method) || "check".equals(method);
    }

    /**
     * Applies a discount to the bill with validation.
     *
     * @param type  Discount type: "none", "percentage", or "fixed"
     * @param value Discount value (percentage 0-100 or fixed amount)
     * @throws IllegalArgumentException if discount type or value is invalid
     */
    public void applyDiscount(String type, float value) {
        // Validate discount type
        if (!"none".equals(type) && !"percentage".equals(type) && !"fixed".equals(type)) {
            throw new IllegalArgumentException("Invalid discount type: " + type + 
                ". Must be 'none', 'percentage', or 'fixed'.");
        }

        // Validate discount value based on type
        if ("percentage".equals(type)) {
            if (value < 0 || value > 100) {
                throw new IllegalArgumentException("Percentage discount must be between 0 and 100. Got: " + value);
            }
        } else if ("fixed".equals(type)) {
            if (value < 0) {
                throw new IllegalArgumentException("Fixed discount cannot be negative. Got: " + value);
            }
            if (value > getSubtotal()) {
                throw new IllegalArgumentException("Fixed discount (" + value + 
                    ") cannot exceed subtotal (" + getSubtotal() + ")");
            }
        } else if ("none".equals(type)) {
            value = 0.0f; // Ensure value is 0 for "none" type
        }

        // Apply the discount
        this.discountType.set(type);
        this.discountValue.set(value);
        // calculateFinalTotal() will be called automatically by the listener
    }

    /**
     * Calculates and updates the final total based on subtotal and discount.
     * This method is called automatically when subtotal, discountType, or discountValue changes.
     */
    public void calculateFinalTotal() {
        float currentSubtotal = getSubtotal();
        float finalTotal = currentSubtotal;

        String type = getDiscountType();
        float value = getDiscountValue();

        if ("percentage".equals(type)) {
            finalTotal = currentSubtotal * (1 - value / 100.0f);
        } else if ("fixed".equals(type)) {
            finalTotal = currentSubtotal - value;
        }

        // Ensure final total is never negative
        if (finalTotal < 0) {
            finalTotal = 0;
        }

        setTotalPrice(finalTotal);
    }

    @Override
    public String toString() {
        return "Bill{" +
                "id=" + getId() +
                ", clientName='" + getClientName() + '\'' +
                ", clientPhone='" + getClientPhone() + '\'' +
                ", subtotal=" + getSubtotal() +
                ", discountType='" + getDiscountType() + '\'' +
                ", discountValue=" + getDiscountValue() +
                ", totalPrice=" + getTotalPrice() +
                ", paymentMethod='" + getPaymentMethod() + '\'' +
                ", date=" + getDate() +
                '}';
    }
}
