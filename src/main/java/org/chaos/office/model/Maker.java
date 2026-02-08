package org.chaos.office.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Maker model representing a manufacturer or brand of auto parts.
 * Uses JavaFX properties for UI binding support.
 */
public class Maker {
    private final IntegerProperty id;
    private final StringProperty name;

    /**
     * Default constructor initializing all properties.
     */
    public Maker() {
        this.id = new SimpleIntegerProperty();
        this.name = new SimpleStringProperty();
    }

    /**
     * Constructor with all fields.
     *
     * @param id   Maker ID
     * @param name Maker name
     */
    public Maker(int id, String name) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
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

    @Override
    public String toString() {
        return "Maker{" +
                "id=" + getId() +
                ", name='" + getName() + '\'' +
                '}';
    }
}
