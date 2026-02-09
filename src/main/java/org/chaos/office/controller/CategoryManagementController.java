package org.chaos.office.controller;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.chaos.office.model.Category;
import org.chaos.office.service.CategoryService;
import org.chaos.office.util.AlertHelper;
import org.chaos.office.util.ImageHelper;
import org.chaos.office.util.LocaleManager;
import org.chaos.office.view.CategoryManagementView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * Controller for category management.
 */
public class CategoryManagementController extends Scene {
    
    private static final Logger logger = LoggerFactory.getLogger(CategoryManagementController.class);
    private final CategoryManagementView view;
    private final CategoryService categoryService;
    private final Stage stage;
    
    public CategoryManagementController(Stage stage) {
        super(new CategoryManagementView(), 1200, 800);
        this.stage = stage;
        this.view = (CategoryManagementView) getRoot();
        this.categoryService = new CategoryService();
        
        // Apply stylesheet - COMMENTED OUT TO USE DEFAULT THEME
        // getStylesheets().add(getClass().getResource("/style/main.css").toExternalForm());
        
        // Load categories
        loadCategories();
        
        // Set up event handlers
        view.getAddButton().setOnAction(e -> handleAdd());
        view.getEditButton().setOnAction(e -> handleEdit());
        view.getDeleteButton().setOnAction(e -> handleDelete());
    }
    
    /**
     * Loads all categories and displays them as cards.
     */
    private void loadCategories() {
        view.getCategoryCardsPane().getChildren().clear();
        view.clearSelection();
        
        List<Category> categories = categoryService.getAllCategories();
        for (Category category : categories) {
            VBox card = view.createCategoryCard(category);
            view.getCategoryCardsPane().getChildren().add(card);
        }
        
        logger.info("Loaded {} categories", categories.size());
    }
    
    /**
     * Handles adding a new category.
     */
    private void handleAdd() {
        Dialog<Category> dialog = createCategoryDialog(null);
        dialog.showAndWait().ifPresent(category -> {
            try {
                categoryService.saveCategory(category);
                AlertHelper.showInfo(
                    LocaleManager.getString("success.title"),
                    LocaleManager.getString("category.added")
                );
                loadCategories();
            } catch (Exception e) {
                logger.error("Error adding category", e);
                AlertHelper.showError(
                    LocaleManager.getString("error.title"),
                    LocaleManager.getString("error.save")
                );
            }
        });
    }
    
    /**
     * Handles editing the selected category.
     */
    private void handleEdit() {
        Category selected = view.getSelectedCategory();
        if (selected == null) {
            return;
        }
        
        Dialog<Category> dialog = createCategoryDialog(selected);
        dialog.showAndWait().ifPresent(category -> {
            try {
                categoryService.updateCategory(category);
                AlertHelper.showInfo(
                    LocaleManager.getString("success.title"),
                    LocaleManager.getString("category.updated")
                );
                loadCategories();
            } catch (Exception e) {
                logger.error("Error updating category", e);
                AlertHelper.showError(
                    LocaleManager.getString("error.title"),
                    LocaleManager.getString("error.save")
                );
            }
        });
    }
    
    /**
     * Handles deleting the selected category.
     */
    private void handleDelete() {
        Category selected = view.getSelectedCategory();
        if (selected == null) {
            return;
        }
        
        // Check if category has parts
        if (categoryService.hasParts(selected.getId())) {
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                LocaleManager.getString("category.has.parts")
            );
            return;
        }
        
        // Confirm deletion
        boolean confirmed = AlertHelper.showConfirmation(
            LocaleManager.getString("confirm.title"),
            LocaleManager.getString("category.delete.confirm")
        );
        
        if (confirmed) {
            try {
                categoryService.deleteCategory(selected.getId());
                AlertHelper.showInfo(
                    LocaleManager.getString("success.title"),
                    LocaleManager.getString("category.deleted")
                );
                loadCategories();
            } catch (Exception e) {
                logger.error("Error deleting category", e);
                AlertHelper.showError(
                    LocaleManager.getString("error.title"),
                    LocaleManager.getString("error.delete")
                );
            }
        }
    }
    
    /**
     * Creates a dialog for adding or editing a category.
     */
    private Dialog<Category> createCategoryDialog(Category existing) {
        Dialog<Category> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? 
            LocaleManager.getString("category.add") : 
            LocaleManager.getString("category.edit"));
        
        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        
        TextField nameField = new TextField();
        if (existing != null) {
            nameField.setText(existing.getName());
        }
        
        TextArea descField = new TextArea();
        descField.setPrefRowCount(3);
        if (existing != null) {
            descField.setText(existing.getDescription());
        }
        
        Button imageButton = new Button(LocaleManager.getString("category.select.image"));
        Label imageLabel = new Label(LocaleManager.getString("category.no.image"));
        
        final byte[][] selectedImage = {existing != null ? existing.getImage() : null};
        
        imageButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(LocaleManager.getString("category.select.image"));
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
            );
            
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                try {
                    selectedImage[0] = ImageHelper.fileToByteArray(file);
                    imageLabel.setText(file.getName());
                } catch (Exception ex) {
                    logger.error("Error loading image", ex);
                    AlertHelper.showError(
                        LocaleManager.getString("error.title"),
                        LocaleManager.getString("error.image.load")
                    );
                }
            }
        });
        
        grid.add(new Label(LocaleManager.getString("category.name")), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(LocaleManager.getString("category.description")), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label(LocaleManager.getString("category.image")), 0, 2);
        grid.add(imageButton, 1, 2);
        grid.add(imageLabel, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Convert result
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                if (nameField.getText().trim().isEmpty()) {
                    AlertHelper.showError(
                        LocaleManager.getString("error.title"),
                        LocaleManager.getString("error.required.fields")
                    );
                    return null;
                }
                
                Category category = existing != null ? existing : new Category();
                category.setName(nameField.getText().trim());
                category.setDescription(descField.getText().trim());
                category.setImage(selectedImage[0]);
                
                return category;
            }
            return null;
        });
        
        return dialog;
    }
}
