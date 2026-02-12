package org.chaos.office.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.chaos.office.model.Category;
import org.chaos.office.util.LocaleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

/**
 * View for category management displaying categories as cards.
 */
public class CategoryManagementView extends BorderPane {
    
    private static final Logger logger = LoggerFactory.getLogger(CategoryManagementView.class);
    private final FlowPane categoryCardsPane;
    private final Button addButton;
    private final Button editButton;
    private final Button deleteButton;
    private Category selectedCategory;
    
    public CategoryManagementView() {
        // Top: Title
        Label titleLabel = new Label(LocaleManager.getString("categories.title"));
        titleLabel.getStyleClass().add("title-label");
        HBox topBox = new HBox(titleLabel);
        topBox.setPadding(new Insets(20));
        topBox.setAlignment(Pos.CENTER_LEFT);
        setTop(topBox);
        
        // Center: Category cards in FlowPane
        categoryCardsPane = new FlowPane();
        categoryCardsPane.setHgap(20);
        categoryCardsPane.setVgap(20);
        categoryCardsPane.setPadding(new Insets(20));
        categoryCardsPane.setAlignment(Pos.TOP_LEFT);
        
        ScrollPane scrollPane = new ScrollPane(categoryCardsPane);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");
        setCenter(scrollPane);
        
        // Bottom: Action buttons
        addButton = new Button(LocaleManager.getString("button.add"));
        addButton.getStyleClass().add("primary-button");
        
        editButton = new Button(LocaleManager.getString("button.edit"));
        editButton.getStyleClass().add("secondary-button");
        editButton.setDisable(true);
        
        deleteButton = new Button(LocaleManager.getString("button.delete"));
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.setDisable(true);
        
        HBox buttonBox = new HBox(15, addButton, editButton, deleteButton);
        buttonBox.setPadding(new Insets(20));
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        setBottom(buttonBox);
        
        // Apply styling
        getStyleClass().add("content-pane");
    }
    
    /**
     * Creates a card for a category.
     */
    public VBox createCategoryCard(Category category) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(15));
        card.setPrefWidth(200);
        card.setAlignment(Pos.TOP_CENTER);
        
        // Image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(150);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);
        
        if (category.getImage() != null && category.getImage().length > 0) {
            try {
                logger.debug("Creating card for category '{}' with image size: {} bytes", 
                    category.getName(), category.getImage().length);
                Image image = new Image(new ByteArrayInputStream(category.getImage()));
                imageView.setImage(image);
                logger.debug("Successfully set image for category '{}'", category.getName());
            } catch (Exception e) {
                logger.error("Failed to load image for category '{}': {}", category.getName(), e.getMessage());
                // Use placeholder if image fails to load
                setPlaceholderIcon(imageView);
            }
        } else {
            logger.debug("Category '{}' has no image, using placeholder", category.getName());
            // No image - use placeholder
            setPlaceholderIcon(imageView);
        }
        
        // Name
        Label nameLabel = new Label(category.getName());
        nameLabel.getStyleClass().add("card-title");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(180);
        
        // Description
        Label descLabel = new Label(category.getDescription());
        descLabel.getStyleClass().add("card-description");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(180);
        descLabel.setMaxHeight(60);
        
        card.getChildren().addAll(imageView, nameLabel, descLabel);
        
        // Click handler for selection
        card.setOnMouseClicked(e -> {
            selectCategory(category, card);
        });
        
        return card;
    }
    
    /**
     * Selects a category card.
     */
    private void selectCategory(Category category, VBox card) {
        // Remove selection from all cards
        categoryCardsPane.getChildren().forEach(node -> {
            node.getStyleClass().remove("card-selected");
        });
        
        // Add selection to clicked card
        card.getStyleClass().add("card-selected");
        selectedCategory = category;
        
        // Enable edit and delete buttons
        editButton.setDisable(false);
        deleteButton.setDisable(false);
    }
    
    public FlowPane getCategoryCardsPane() {
        return categoryCardsPane;
    }
    
    public Button getAddButton() {
        return addButton;
    }
    
    public Button getEditButton() {
        return editButton;
    }
    
    public Button getDeleteButton() {
        return deleteButton;
    }
    
    public Category getSelectedCategory() {
        return selectedCategory;
    }
    
    public void clearSelection() {
        selectedCategory = null;
        editButton.setDisable(true);
        deleteButton.setDisable(true);
        categoryCardsPane.getChildren().forEach(node -> {
            node.getStyleClass().remove("card-selected");
        });
    }
    
    /**
     * Sets a placeholder icon for categories without images.
     */
    private void setPlaceholderIcon(ImageView imageView) {
        // Create a simple colored rectangle as placeholder
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(150, 150);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Draw a light gray background
        gc.setFill(javafx.scene.paint.Color.rgb(240, 240, 240));
        gc.fillRect(0, 0, 150, 150);
        
        // Draw a folder icon using simple shapes
        gc.setFill(javafx.scene.paint.Color.rgb(100, 100, 100));
        
        // Folder tab
        gc.fillRoundRect(30, 50, 40, 15, 5, 5);
        
        // Folder body
        gc.fillRoundRect(25, 60, 100, 60, 10, 10);
        
        // Convert canvas to image
        javafx.scene.image.WritableImage writableImage = new javafx.scene.image.WritableImage(150, 150);
        canvas.snapshot(null, writableImage);
        imageView.setImage(writableImage);
    }
}
