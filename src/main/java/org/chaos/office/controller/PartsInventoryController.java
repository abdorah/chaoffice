package org.chaos.office.controller;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.chaos.office.model.Category;
import org.chaos.office.model.ImportResult;
import org.chaos.office.model.Part;
import org.chaos.office.service.BulkImportService;
import org.chaos.office.service.CSVFileParser;
import org.chaos.office.service.CategoryService;
import org.chaos.office.service.ExcelFileParser;
import org.chaos.office.service.ImportValidator;
import org.chaos.office.service.PartService;
import org.chaos.office.util.AlertHelper;
import org.chaos.office.util.DialogHelper;
import org.chaos.office.util.LocaleManager;
import org.chaos.office.util.ValidationHelper;
import org.chaos.office.view.ImportResultDialog;
import org.chaos.office.view.PartsInventoryView;

import java.io.File;
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
        view.getImportButton().setOnAction(e -> handleImport());
        view.getDownloadTemplateButton().setOnAction(e -> handleDownloadTemplate());
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
        categoryNames.add(LocaleManager.getString("category.all")); // Add default option
        for (Category category : categories) {
            categoryNames.add(category.getName());
        }
        view.getCategoryFilter().setItems(FXCollections.observableArrayList(categoryNames));
        view.getCategoryFilter().setValue(LocaleManager.getString("category.all"));
    }
    
    private void handleCategoryFilter() {
        String selected = view.getCategoryFilter().getValue();
        String allCategoriesText = LocaleManager.getString("category.all");
        if (selected == null || selected.equals(allCategoriesText)) {
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
        
        // Apply current theme to dialog
        DialogHelper.applyTheme(dialog);
        
        // Set button types
        ButtonType saveButtonType = new ButtonType(
            LocaleManager.getString("common.save"), 
            ButtonBar.ButtonData.OK_DONE
        );
        ButtonType cancelButtonType = new ButtonType(
            LocaleManager.getString("common.cancel"),
            ButtonBar.ButtonData.CANCEL_CLOSE
        );
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);
        
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
    
    /**
     * Handles the import button action by opening a file chooser dialog
     * and processing the selected file with progress indication.
     * Requirements: 2.2, 2.3, 2.5, 6.1, 6.2, 6.7
     */
    private void handleImport() {
        // Create and configure FileChooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Import");
        
        // Configure file type filters for CSV and Excel files
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Supported Files", "*.csv", "*.xlsx", "*.xls"),
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
        );
        
        // Show open dialog
        File selectedFile = fileChooser.showOpenDialog(view.getScene().getWindow());
        
        // Process the file if one was selected
        if (selectedFile != null) {
            processImportFile(selectedFile);
        }
    }
    
    /**
     * Processes the import file on a background thread with progress indication.
     * Requirements: 6.1, 6.2, 6.7
     * 
     * @param file The file to import
     */
    private void processImportFile(File file) {
        // Show progress indicator (Requirement 6.1)
        view.getProgressIndicator().setVisible(true);
        view.getProgressIndicator().setManaged(true);
        
        // Disable import button during processing (Requirement 6.2)
        view.getImportButton().setDisable(true);
        
        // Create background task for import processing
        Task<ImportResult> importTask = new Task<ImportResult>() {
            @Override
            protected ImportResult call() throws Exception {
                // Create BulkImportService with all dependencies
                BulkImportService bulkImportService = new BulkImportService(
                    partService,
                    categoryService,
                    new CSVFileParser(),
                    new ExcelFileParser(),
                    new ImportValidator()
                );
                
                // Run import on background thread
                return bulkImportService.importFromFile(file);
            }
        };
        
        // Handle task completion
        importTask.setOnSucceeded(event -> {
            // Hide progress indicator (Requirement 6.7)
            view.getProgressIndicator().setVisible(false);
            view.getProgressIndicator().setManaged(false);
            
            // Re-enable import button (Requirement 6.7)
            view.getImportButton().setDisable(false);
            
            // Get import result
            ImportResult result = importTask.getValue();
            
            // Show result dialog (Requirements 6.3, 6.4, 6.5)
            ImportResultDialog resultDialog = new ImportResultDialog(result);
            resultDialog.showAndWait();
            
            // Refresh parts table if any imports succeeded (Requirement 6.6)
            if (result.getSuccessCount() > 0) {
                loadParts();
            }
        });
        
        // Handle task failure
        importTask.setOnFailed(event -> {
            // Hide progress indicator
            view.getProgressIndicator().setVisible(false);
            view.getProgressIndicator().setManaged(false);
            
            // Re-enable import button
            view.getImportButton().setDisable(false);
            
            // Show error message
            Throwable exception = importTask.getException();
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                "Import failed: " + (exception != null ? exception.getMessage() : "Unknown error")
            );
        });
        
        // Start the background task
        Thread importThread = new Thread(importTask);
        importThread.setDaemon(true);
        importThread.start();
    }
    
    /**
     * Handles the download template button action by opening a file chooser
     * and saving the CSV template to the selected location.
     * Requirements: 8.3
     */
    private void handleDownloadTemplate() {
        // Create and configure FileChooser for saving
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save CSV Template");
        fileChooser.setInitialFileName("parts_import_template.csv");
        
        // Configure file type filter for CSV
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        
        // Show save dialog
        File saveFile = fileChooser.showSaveDialog(view.getScene().getWindow());
        
        // Save the template if a location was selected
        if (saveFile != null) {
            try {
                // Load the template from resources
                java.io.InputStream templateStream = getClass()
                    .getResourceAsStream("/templates/parts_import_template.csv");
                
                if (templateStream == null) {
                    AlertHelper.showError(
                        LocaleManager.getString("error.title"),
                        "Template file not found in application resources."
                    );
                    return;
                }
                
                // Copy the template to the selected file
                java.nio.file.Files.copy(
                    templateStream,
                    saveFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
                
                templateStream.close();
                
                // Show success message
                AlertHelper.showInfo(
                    LocaleManager.getString("success.title"),
                    "Template downloaded successfully to:\n" + saveFile.getAbsolutePath()
                );
                
            } catch (java.io.IOException e) {
                AlertHelper.showError(
                    LocaleManager.getString("error.title"),
                    "Failed to save template: " + e.getMessage()
                );
            }
        }
    }
    
    public Parent getView() {
        return view;
    }
}
