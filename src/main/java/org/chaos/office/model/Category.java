package org.chaos.office.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Category model representing a classification grouping for auto parts.
 * Uses JavaFX properties for UI binding support.
 */
public class Category {
    private final IntegerProperty id;
    private final StringProperty name;
    private final StringProperty description;
    private final ObjectProperty<byte[]> image;

    /**
     * Default constructor initializing all properties.
     */
    public Category() {
        this.id = new SimpleIntegerProperty();
        this.name = new SimpleStringProperty();
        this.description = new SimpleStringProperty();
        this.image = new SimpleObjectProperty<>();
    }

    /**
     * Constructor with all fields.
     *
     * @param id          Category ID
     * @param name        Category name
     * @param description Category description
     * @param image       Category icon image as byte array
     */
    public Category(int id, String name, String description, byte[] image) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.description = new SimpleStringProperty(description);
        this.image = new SimpleObjectProperty<>(image);
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

    // Image property methods
    public byte[] getImage() {
        return image.get();
    }

    public void setImage(byte[] image) {
        this.image.set(image);
    }

    public ObjectProperty<byte[]> imageProperty() {
        return image;
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + getId() +
                ", name='" + getName() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", image=" + (getImage() != null ? "byte[" + getImage().length + "]" : "null") +
                '}';
    }
}
