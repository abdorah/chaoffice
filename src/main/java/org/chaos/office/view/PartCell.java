package org.chaos.office.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.chaos.office.model.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

/**
 * PartCell is a custom ListCell renderer for displaying Part objects in a ListView.
 * 
 * <p>This cell displays:
 * <ul>
 *   <li>Part image or default placeholder icon</li>
 *   <li>Part name (bold, larger font)</li>
 *   <li>Category, maker, price, and stock information</li>
 *   <li>Distinct styling for out-of-stock parts</li>
 * </ul>
 * 
 * <p>The layout is optimized for quick visual scanning with proper spacing
 * and alignment.
 * 
 * <p>Requirements: 5.1, 5.2, 5.3, 5.4, 11.3
 */
public class PartCell extends ListCell<Part> {
    private static final Logger logger = LoggerFactory.getLogger(PartCell.class);
    private static final int IMAGE_SIZE = 50;
    private static final String CURRENCY_SYMBOL = "$";
    
    // UI Components (reused for performance)
    private final HBox container;
    private final ImageView imageView;
    private final VBox infoContainer;
    private final Label nameLabel;
    private final Label detailsLabel;
    
    /**
     * Creates a new PartCell with initialized UI components.
     */
    public PartCell() {
        // Initialize image view
        this.imageView = new ImageView();
        this.imageView.setFitWidth(IMAGE_SIZE);
        this.imageView.setFitHeight(IMAGE_SIZE);
        this.imageView.setPreserveRatio(true);
        
        // Initialize name label
        this.nameLabel = new Label();
        this.nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        this.nameLabel.setWrapText(false);
        
        // Initialize details label
        this.detailsLabel = new Label();
        this.detailsLabel.setFont(Font.font("System", 12));
        this.detailsLabel.setWrapText(true);
        
        // Initialize info container
        this.infoContainer = new VBox(5);
        this.infoContainer.getChildren().addAll(nameLabel, detailsLabel);
        this.infoContainer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoContainer, Priority.ALWAYS);
        
        // Initialize main container
        this.container = new HBox(10);
        this.container.setAlignment(Pos.CENTER_LEFT);
        this.container.setPadding(new Insets(8));
        this.container.getChildren().addAll(imageView, infoContainer);
    }
    
    @Override
    protected void updateItem(Part part, boolean empty) {
        super.updateItem(part, empty);
        
        if (empty || part == null) {
            setText(null);
            setGraphic(null);
            getStyleClass().removeAll("part-cell", "out-of-stock");
        } else {
            // Update image
            updateImage(part);
            
            // Update name
            nameLabel.setText(part.getName());
            
            // Update details
            String details = buildDetailsText(part);
            detailsLabel.setText(details);
            
            // Apply styling based on stock
            getStyleClass().removeAll("part-cell", "out-of-stock");
            getStyleClass().add("part-cell");
            
            if (part.getQuantity() <= 0) {
                getStyleClass().add("out-of-stock");
                nameLabel.setStyle("-fx-text-fill: #999999;");
                detailsLabel.setStyle("-fx-text-fill: #999999;");
            } else {
                nameLabel.setStyle("-fx-text-fill: #000000;");
                detailsLabel.setStyle("-fx-text-fill: #555555;");
            }
            
            setText(null);
            setGraphic(container);
        }
    }
    
    /**
     * Updates the image view with the part's image or a default placeholder.
     * 
     * @param part the part to display
     */
    private void updateImage(Part part) {
        try {
            // Check if category has an image
            if (part.getCategory() != null && part.getCategory().getImage() != null) {
                byte[] imageData = part.getCategory().getImage();
                Image image = new Image(new ByteArrayInputStream(imageData));
                imageView.setImage(image);
            } else {
                // Use default placeholder
                imageView.setImage(getDefaultPlaceholder());
            }
        } catch (Exception e) {
            logger.warn("Error loading image for part: {}", part.getName(), e);
            imageView.setImage(getDefaultPlaceholder());
        }
    }
    
    /**
     * Builds the details text showing category, maker, price, and stock.
     * 
     * @param part the part to display
     * @return formatted details string
     */
    private String buildDetailsText(Part part) {
        StringBuilder details = new StringBuilder();
        
        // Category
        if (part.getCategory() != null) {
            details.append(part.getCategory().getName());
        } else {
            details.append("Unknown Category");
        }
        
        details.append(" | ");
        
        // Maker
        if (part.getMaker() != null && !part.getMaker().isEmpty()) {
            details.append(part.getMaker());
        } else {
            details.append("Unknown Maker");
        }
        
        details.append(" | ");
        
        // Price (formatted with currency symbol and 2 decimal places)
        details.append(formatPrice(part.getPrice()));
        
        details.append(" | ");
        
        // Stock
        if (part.getQuantity() <= 0) {
            details.append(org.chaos.office.util.LocaleManager.getString("parts.out.of.stock"));
        } else {
            details.append(org.chaos.office.util.LocaleManager.getString("parts.stock")).append(": ").append(part.getQuantity());
        }
        
        return details.toString();
    }
    
    /**
     * Formats a price value with currency symbol and exactly 2 decimal places.
     * Uses US locale for consistent formatting.
     * 
     * @param price the price to format
     * @return formatted price string
     */
    private String formatPrice(float price) {
        return String.format(java.util.Locale.US, "%s%.2f", CURRENCY_SYMBOL, price);
    }
    
    /**
     * Gets the default placeholder image for parts without images.
     * 
     * @return default placeholder image
     */
    private Image getDefaultPlaceholder() {
        // Try to load a default icon from resources
        try {
            // First try to use ImageHelper if it has a default icon method
            // Otherwise create a simple placeholder
            return new Image(getClass().getResourceAsStream("/icons/default-part.png"));
        } catch (Exception e) {
            // If no default icon exists, return null (will show empty space)
            logger.debug("No default part icon found, using empty placeholder");
            return null;
        }
    }
}
