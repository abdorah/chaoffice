package org.chaos.office.model;

import javafx.beans.property.*;

/**
 * Command model representing a line item within a bill.
 * Each command represents a specific part and quantity sold in a transaction.
 * Uses JavaFX properties for UI binding support.
 */
public class Command {
    private final IntegerProperty billId;
    private final IntegerProperty partId;
    private final IntegerProperty quantity;
    private final FloatProperty priceConsidered;
    private final StringProperty partName;

    /**
     * Default constructor initializing all properties.
     */
    public Command() {
        this.billId = new SimpleIntegerProperty();
        this.partId = new SimpleIntegerProperty();
        this.quantity = new SimpleIntegerProperty();
        this.priceConsidered = new SimpleFloatProperty();
        this.partName = new SimpleStringProperty();
    }

    /**
     * Constructor with all fields.
     *
     * @param billId          ID of the bill this command belongs to
     * @param partId          ID of the part being sold
     * @param quantity        Quantity of the part (must be positive)
     * @param priceConsidered Price of the part at the time of sale (must be non-negative)
     */
    public Command(int billId, int partId, int quantity, float priceConsidered) {
        this.billId = new SimpleIntegerProperty(billId);
        this.partId = new SimpleIntegerProperty(partId);
        this.quantity = new SimpleIntegerProperty(quantity);
        this.priceConsidered = new SimpleFloatProperty(priceConsidered);
        this.partName = new SimpleStringProperty();
    }

    // BillId property methods
    public int getBillId() {
        return billId.get();
    }

    public void setBillId(int billId) {
        this.billId.set(billId);
    }

    public IntegerProperty billIdProperty() {
        return billId;
    }

    // PartId property methods
    public int getPartId() {
        return partId.get();
    }

    public void setPartId(int partId) {
        this.partId.set(partId);
    }

    public IntegerProperty partIdProperty() {
        return partId;
    }

    // Quantity property methods
    public int getQuantity() {
        return quantity.get();
    }

    public void setQuantity(int quantity) {
        this.quantity.set(quantity);
    }

    public IntegerProperty quantityProperty() {
        return quantity;
    }

    // PriceConsidered property methods
    public float getPriceConsidered() {
        return priceConsidered.get();
    }

    public void setPriceConsidered(float priceConsidered) {
        this.priceConsidered.set(priceConsidered);
    }

    public FloatProperty priceConsideredProperty() {
        return priceConsidered;
    }

    // PartName property methods
    public String getPartName() {
        return partName.get();
    }

    public void setPartName(String partName) {
        this.partName.set(partName);
    }

    public StringProperty partNameProperty() {
        return partName;
    }

    @Override
    public String toString() {
        return "Command{" +
                "billId=" + getBillId() +
                ", partId=" + getPartId() +
                ", quantity=" + getQuantity() +
                ", priceConsidered=" + getPriceConsidered() +
                ", partName='" + getPartName() + '\'' +
                '}';
    }
}
