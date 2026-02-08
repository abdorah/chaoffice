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

    /**
     * Default constructor initializing all properties.
     */
    public Bill() {
        this.id = new SimpleIntegerProperty();
        this.clientName = new SimpleStringProperty();
        this.clientPhone = new SimpleStringProperty();
        this.totalPrice = new SimpleFloatProperty();
        this.date = new SimpleObjectProperty<>();
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

    @Override
    public String toString() {
        return "Bill{" +
                "id=" + getId() +
                ", clientName='" + getClientName() + '\'' +
                ", clientPhone='" + getClientPhone() + '\'' +
                ", totalPrice=" + getTotalPrice() +
                ", date=" + getDate() +
                '}';
    }
}
