package org.chaos.office.controller;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.chaos.office.model.Category;
import org.chaos.office.model.Part;
import org.chaos.office.service.CategoryService;
import org.chaos.office.service.PartService;
import org.chaos.office.util.AlertHelper;
import org.chaos.office.util.LocaleManager;
import org.chaos.office.util.ValidationHelper;
import org.chaos.office.view.PartsInventoryView;

import java.util.ArrayList;
import java.util.Optional;

/**
 * PartsInventoryController - Manages parts inventory
 * Requirements: 4.2, 4.3, 4.5, 4.6, 4.7, 13.2, 13.4, 15.1
 */
public class PartsInventoryController {
    
    private final PartsInventoryView view;
    private final PartService partService;
    private final CategoryService categoryService;
    
    public PartsInventoryController() {
        this.view = new PartsInventoryView();
        this.partService = new PartService();
        this.categoryService = new CategoryService();
        
        setupEventHandlers();
        loadParts();
    }
    
    private void setupEventHandlers() {
        view.getSearchField().textProperty().addListener((obs, old, newVal) -> handleSearch(newVal));
        view.getCategoryFilter().setOnAction(e -> handleCategoryFilter());
        view.getAddButton().setOnAction(e -> handleAdd());
        view.getEditButton().setOnAction(e -> handleEdit());
        view.getDeleteButton().setOnAction(e -> handleDelete());
    }
    
    private void loadParts() {
        var parts = partService.getAllParts();
        view.getPartsTable().setItems(FXCollections.observableArrayList(parts));
        
        // Load categories into filter dropdown
        loadCategories();
    }
    
    private void loadCategories() {
        var categories = categoryService.getAllCategories();
        var categoryNames = new ArrayList<String>();
        categoryNames.add("All Categories"); // Add default option
        for (Category category : categories) {
            categoryNames.add(category.getName());
        }
        view.getCategoryFilter().setItems(FXCollections.observableArrayList(categoryNames));
        view.getCategoryFilter().setValue("All Categories");
    }
    
    private void handleCategoryFilter() {
        String selected = view.getCategoryFilter().getValue();
        if (selected == null || selected.equals("All Categories")) {
            loadParts();
        } else {
            // Find category by name
            var categories = categoryService.getAllCategories();
            for (Category category : categories) {
                if (category.getName().equals(selected)) {
                    var parts = partService.filterByCategory(category.getId());
                    view.getPartsTable().setItems(FXCollections.observableArrayList(parts));
                    break;
                }
            }
        }
    }
    
    private void handleSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadParts();
        } else {
            var results = partService.searchParts(query);
            view.getPartsTable().setItems(FXCollections.observableArrayList(results));
        }
    }
    
    private void handleAdd() {
        showPartDialog(null);
    }
    
    private void handleEdit() {
        var selected = view.getPartsTable().getSelectionModel().getSelectedItem();
        if (selected != null) {
            showPartDialog(selected);
        } else {
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                "Please select a part to edit"
            );
        }
    }
    
    private void showPartDialog(Part existingPart) {
        Dialog<Part> dialog = new Dialog<>();
        dialog.setTitle(existingPart == null ? 
            LocaleManager.getString("parts.add") : 
            LocaleManager.getString("parts.edit"));
        dialog.setHeaderText(existingPart == null ? 
            "Add New Part" : 
            "Edit Part");
        
        // Set button types
        ButtonType saveButtonType = new ButtonType(
            LocaleManager.getString("common.save"), 
            ButtonBar.ButtonData.OK_DONE
        );
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField();
        nameField.setPromptText(LocaleManager.getString("parts.name"));
        
        TextField makerField = new TextField();
        makerField.setPromptText(LocaleManager.getString("parts.maker"));
        
        TextArea descriptionField = new TextArea();
        descriptionField.setPromptText(LocaleManager.getString("parts.description"));
        descriptionField.setPrefRowCount(3);
        
        TextField priceField = new TextField();
        priceField.setPromptText(LocaleManager.getString("parts.price"));
        
        TextField quantityField = new TextField();
        quantityField.setPromptText(LocaleManager.getString("parts.quantity"));
        
        ComboBox<Category> categoryCombo = new ComboBox<>();
        categoryCombo.setItems(FXCollections.observableArrayList(categoryService.getAllCategories()));
        categoryCombo.setPromptText(LocaleManager.getString("parts.category"));
        categoryCombo.setConverter(new javafx.util.StringConverter<Category>() {
            @Override
            public String toString(Category category) {
                return category != null ? category.getName() : "";
            }
            
            @Override
            public Category fromString(String string) {
                return null;
            }
        });
        
        // Populate fields if editing
        if (existingPart != null) {
            nameField.setText(existingPart.getName());
            makerField.setText(existingPart.getMaker());
            descriptionField.setText(existingPart.getDescription());
            priceField.setText(String.valueOf(existingPart.getPrice()));
            quantityField.setText(String.valueOf(existingPart.getQuantity()));
            categoryCombo.setValue(existingPart.getCategory());
        }
        
        grid.add(new Label(LocaleManager.getString("parts.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(LocaleManager.getString("parts.maker") + ":"), 0, 1);
        grid.add(makerField, 1, 1);
        grid.add(new Label(LocaleManager.getString("parts.description") + ":"), 0, 2);
        grid.add(descriptionField, 1, 2);
        grid.add(new Label(LocaleManager.getString("parts.price") + ":"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(new Label(LocaleManager.getString("parts.quantity") + ":"), 0, 4);
        grid.add(quantityField, 1, 4);
        grid.add(new Label(LocaleManager.getString("parts.category") + ":"), 0, 5);
        grid.add(categoryCombo, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        
        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    // Validate inputs
                    if (!ValidationHelper.isNotEmpty(nameField.getText())) {
                        AlertHelper.showError(LocaleManager.getString("error.title"), 
                            "Part name is required");
                        return null;
                    }
                    
                    if (!ValidationHelper.isNotEmpty(makerField.getText())) {
                        AlertHelper.showError(LocaleManager.getString("error.title"), 
                            "Maker is required");
                        return null;
                    }
                    
                    if (!ValidationHelper.isNotEmpty(descriptionField.getText())) {
                        AlertHelper.showError(LocaleManager.getString("error.title"), 
                            "Description is required");
                        return null;
                    }
                    
                    float price;
                    try {
                        price = Float.parseFloat(priceField.getText());
                        if (price < 0) {
                            AlertHelper.showError(LocaleManager.getString("error.title"), 
                                "Price must be non-negative");
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        AlertHelper.showError(LocaleManager.getString("error.title"), 
                            "Invalid price format");
                        return null;
                    }
                    
                    int quantity;
                    try {
                        quantity = Integer.parseInt(quantityField.getText());
                        if (quantity < 0) {
                            AlertHelper.showError(LocaleManager.getString("error.title"), 
                                "Quantity must be non-negative");
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        AlertHelper.showError(LocaleManager.getString("error.title"), 
                            "Invalid quantity format");
                        return null;
                    }
                    
                    if (categoryCombo.getValue() == null) {
                        AlertHelper.showError(LocaleManager.getString("error.title"), 
                            "Category is required");
                        return null;
                    }
                    
                    Part part = existingPart != null ? existingPart : new Part();
                    part.setName(nameField.getText());
                    part.setMaker(makerField.getText());
                    part.setDescription(descriptionField.getText());
                    part.setPrice(price);
                    part.setQuantity(quantity);
                    part.setCategory(categoryCombo.getValue());
                    
                    return part;
                } catch (Exception e) {
                    AlertHelper.showError(LocaleManager.getString("error.title"), 
                        "Error: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });
        
        Optional<Part> result = dialog.showAndWait();
        result.ifPresent(part -> {
            try {
                if (existingPart == null) {
                    partService.savePart(part);
                    AlertHelper.showInfo(
                        LocaleManager.getString("success.title"),
                        "Part added successfully"
                    );
                } else {
                    partService.updatePart(part);
                    AlertHelper.showInfo(
                        LocaleManager.getString("success.title"),
                        "Part updated successfully"
                    );
                }
                loadParts();
            } catch (Exception e) {
                AlertHelper.showError(
                    LocaleManager.getString("error.title"),
                    "Failed to save part: " + e.getMessage()
                );
            }
        });
    }
    
    private void handleDelete() {
        var selected = view.getPartsTable().getSelectionModel().getSelectedItem();
        if (selected != null) {
            boolean confirmed = AlertHelper.showConfirmation(
                LocaleManager.getString("parts.delete.confirm.title"),
                LocaleManager.getString("parts.delete.confirm.message")
            );
            if (confirmed) {
                partService.deletePart(selected.getId());
                loadParts();
            }
        }
    }
    
    public Parent getView() {
        return view;
    }
}
