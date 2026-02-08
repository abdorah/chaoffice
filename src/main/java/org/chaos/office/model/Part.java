package org.chaos.office.model;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Part model representing an auto part in the inventory.
 * Uses JavaFX properties for UI binding support.
 */
public class Part {
    private final IntegerProperty id;
    private final StringProperty name;
    private final StringProperty maker;
    private final StringProperty description;
    private final FloatProperty price;
    private final IntegerProperty quantity;
    private final ObjectProperty<Category> category;

    /**
     * Default constructor initializing all properties.
     */
    public Part() {
        this.id = new SimpleIntegerProperty();
        this.name = new SimpleStringProperty();
        this.maker = new SimpleStringProperty();
        this.description = new SimpleStringProperty();
        this.price = new SimpleFloatProperty();
        this.quantity = new SimpleIntegerProperty();
        this.category = new SimpleObjectProperty<>();
    }

    /**
     * Constructor with all fields.
     *
     * @param id          Part ID
     * @param name        Part name
     * @param maker       Part maker/manufacturer
     * @param description Part description
     * @param price       Part price (must be non-negative)
     * @param quantity    Part quantity in stock (must be non-negative)
     * @param category    Part category
     */
    public Part(int id, String name, String maker, String description, float price, int quantity, Category category) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.maker = new SimpleStringProperty(maker);
        this.description = new SimpleStringProperty(description);
        this.price = new SimpleFloatProperty(price);
        this.quantity = new SimpleIntegerProperty(quantity);
        this.category = new SimpleObjectProperty<>(category);
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

    // Name property methods
    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    // Maker property methods
    public String getMaker() {
        return maker.get();
    }

    public void setMaker(String maker) {
        this.maker.set(maker);
    }

    public StringProperty makerProperty() {
        return maker;
    }

    // Description property methods
    public String getDescription() {
        return description.get();
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    // Price property methods
    public float getPrice() {
        return price.get();
    }

    public void setPrice(float price) {
        this.price.set(price);
    }

    public FloatProperty priceProperty() {
        return price;
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

    // Category property methods
    public Category getCategory() {
        return category.get();
    }

    public void setCategory(Category category) {
        this.category.set(category);
    }

    public ObjectProperty<Category> categoryProperty() {
        return category;
    }

    @Override
    public String toString() {
        return "Part{" +
                "id=" + getId() +
                ", name='" + getName() + '\'' +
                ", maker='" + getMaker() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", price=" + getPrice() +
                ", quantity=" + getQuantity() +
                ", category=" + (getCategory() != null ? getCategory().getName() : "null") +
                '}';
    }
}
