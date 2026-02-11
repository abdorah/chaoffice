package org.chaos.office.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * BrandingSettings model representing customizable store branding configuration.
 * Stores optional store name and logo image for application and document branding.
 * Uses JavaFX properties for UI binding support.
 */
public class BrandingSettings {
    private final StringProperty storeName;
    private final ObjectProperty<byte[]> logoImage;
    private final StringProperty logoFormat;

    /**
     * Default constructor initializing all properties.
     */
    public BrandingSettings() {
        this.storeName = new SimpleStringProperty();
        this.logoImage = new SimpleObjectProperty<>();
        this.logoFormat = new SimpleStringProperty();
    }

    /**
     * Constructor with all fields.
     *
     * @param storeName  Optional store name for branding
     * @param logoImage  Optional logo image as byte array
     * @param logoFormat Logo image format (e.g., "PNG", "JPG")
     */
    public BrandingSettings(String storeName, byte[] logoImage, String logoFormat) {
        this.storeName = new SimpleStringProperty(storeName);
        this.logoImage = new SimpleObjectProperty<>(logoImage);
        this.logoFormat = new SimpleStringProperty(logoFormat);
    }

    // StoreName property methods
    public String getStoreName() {
        return storeName.get();
    }

    public void setStoreName(String storeName) {
        this.storeName.set(storeName);
    }

    public StringProperty storeNameProperty() {
        return storeName;
    }

    // LogoImage property methods
    public byte[] getLogoImage() {
        return logoImage.get();
    }

    public void setLogoImage(byte[] logoImage) {
        this.logoImage.set(logoImage);
    }

    public ObjectProperty<byte[]> logoImageProperty() {
        return logoImage;
    }

    // LogoFormat property methods
    public String getLogoFormat() {
        return logoFormat.get();
    }

    public void setLogoFormat(String logoFormat) {
        this.logoFormat.set(logoFormat);
    }

    public StringProperty logoFormatProperty() {
        return logoFormat;
    }

    // Utility methods
    /**
     * Checks if a store name is configured.
     *
     * @return true if store name is not null and not empty after trimming
     */
    public boolean hasStoreName() {
        String name = getStoreName();
        return name != null && !name.trim().isEmpty();
    }

    /**
     * Checks if a logo image is configured.
     *
     * @return true if logo image is not null and has content
     */
    public boolean hasLogo() {
        byte[] image = getLogoImage();
        return image != null && image.length > 0;
    }

    @Override
    public String toString() {
        return "BrandingSettings{" +
                "storeName='" + getStoreName() + '\'' +
                ", logoImage=" + (getLogoImage() != null ? "byte[" + getLogoImage().length + "]" : "null") +
                ", logoFormat='" + getLogoFormat() + '\'' +
                '}';
    }
}
