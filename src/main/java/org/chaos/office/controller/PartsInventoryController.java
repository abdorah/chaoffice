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
        
        // Use valueProperty listener instead of setOnAction to avoid triggering on programmatic setValue
        // This prevents infinite loop when loadCategories() sets the value
        view.getCategoryFilter().valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                handleCategoryFilterChange(newVal);
            }
        });
        
        view.getAddButton().setOnAction(e -> handleAdd());
        view.getEditButton().setOnAction(e -> handleEdit());
        view.getDeleteButton().setOnAction(e -> handleDelete());
        view.getImportButton().setOnAction(e -> handleImport());
        view.getDownloadTemplateButton().setOnAction(e -> handleDownloadTemplate());
    }
    
    private void loadParts() {
        var parts = partService.getAllParts();
        view.getPartsTable().setItems(FXCollections.observableArrayList(parts));
        
        // Only load categories if the filter is empty (first time initialization)
        if (view.getCategoryFilter().getItems().isEmpty()) {
            loadCategories();
        }
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
    
    /**
     * Handles category filter changes without reloading categories.
     * This prevents infinite loop that was caused by loadParts() -> loadCategories() -> setValue() -> handleCategoryFilter()
     */
    private void handleCategoryFilterChange(String selected) {
        String allCategoriesText = LocaleManager.getString("category.all");
        if (selected == null || selected.equals(allCategoriesText)) {
            // Show all parts without reloading categories
            var parts = partService.getAllParts();
            view.getPartsTable().setItems(FXCollections.observableArrayList(parts));
        } else {
            // Find category by name and filter parts
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
                LocaleManager.getString("parts.select.to.edit")
            );
        }
    }
    
    private void showPartDialog(Part existingPart) {
        Dialog<Part> dialog = new Dialog<>();
        dialog.setTitle(existingPart == null ? 
            LocaleManager.getString("parts.add") : 
            LocaleManager.getString("parts.edit"));
        dialog.setHeaderText(existingPart == null ? 
            LocaleManager.getString("dialog.header.add.part") : 
            LocaleManager.getString("dialog.header.edit.part"));
        
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
                            LocaleManager.getString("error.part.name.required"));
                        return null;
                    }
                    
                    if (!ValidationHelper.isNotEmpty(makerField.getText())) {
                        AlertHelper.showError(LocaleManager.getString("error.title"), 
                            LocaleManager.getString("error.part.maker.required"));
                        return null;
                    }
                    
                    if (!ValidationHelper.isNotEmpty(descriptionField.getText())) {
                        AlertHelper.showError(LocaleManager.getString("error.title"), 
                            LocaleManager.getString("error.part.description.required"));
                        return null;
                    }
                    
                    float price;
                    try {
                        price = Float.parseFloat(priceField.getText());
                        if (price < 0) {
                            AlertHelper.showError(LocaleManager.getString("error.title"), 
                                LocaleManager.getString("error.part.price.negative"));
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        AlertHelper.showError(LocaleManager.getString("error.title"), 
                            LocaleManager.getString("error.part.price.invalid"));
                        return null;
                    }
                    
                    int quantity;
                    try {
                        quantity = Integer.parseInt(quantityField.getText());
                        if (quantity < 0) {
                            AlertHelper.showError(LocaleManager.getString("error.title"), 
                                LocaleManager.getString("error.part.quantity.negative"));
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        AlertHelper.showError(LocaleManager.getString("error.title"), 
                            LocaleManager.getString("error.part.quantity.invalid"));
                        return null;
                    }
                    
                    if (categoryCombo.getValue() == null) {
                        AlertHelper.showError(LocaleManager.getString("error.title"), 
                            LocaleManager.getString("error.part.category.required"));
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
                        java.text.MessageFormat.format(LocaleManager.getString("error.part.save.failed"), e.getMessage()));
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
                        LocaleManager.getString("parts.added.successfully")
                    );
                } else {
                    partService.updatePart(part);
                    AlertHelper.showInfo(
                        LocaleManager.getString("success.title"),
                        LocaleManager.getString("parts.updated.successfully")
                    );
                }
                loadParts();
            } catch (Exception e) {
                AlertHelper.showError(
                    LocaleManager.getString("error.title"),
                    java.text.MessageFormat.format(LocaleManager.getString("parts.save.failed"), e.getMessage())
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
     * Handles the download template button action by exporting the entire
     * parts database to an Excel file.
     * Requirements: 8.3
     */
    private void handleDownloadTemplate() {
        // Create and configure FileChooser for saving
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(LocaleManager.getString("parts.export.database"));
        fileChooser.setInitialFileName("parts_database_export.xlsx");
        
        // Configure file type filter for Excel
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );
        
        // Show save dialog
        File saveFile = fileChooser.showSaveDialog(view.getScene().getWindow());
        
        // Export the database if a location was selected
        if (saveFile != null) {
            try {
                // Get all parts and categories from the database
                var allParts = partService.getAllParts();
                var allCategories = categoryService.getAllCategories();
                
                // Create Excel workbook
                org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                
                // ===== PARTS SHEET =====
                org.apache.poi.ss.usermodel.Sheet partsSheet = workbook.createSheet("Parts");
                
                // Create header row for parts
                org.apache.poi.ss.usermodel.Row partsHeaderRow = partsSheet.createRow(0);
                String[] partsHeaders = {
                    LocaleManager.getString("parts.name"),
                    LocaleManager.getString("parts.maker"),
                    LocaleManager.getString("parts.description"),
                    LocaleManager.getString("parts.price"),
                    LocaleManager.getString("parts.quantity"),
                    LocaleManager.getString("parts.category")
                };
                
                // Style for header
                org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                
                for (int i = 0; i < partsHeaders.length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = partsHeaderRow.createCell(i);
                    cell.setCellValue(partsHeaders[i]);
                    cell.setCellStyle(headerStyle);
                }
                
                // Add parts data rows (without ID column)
                int rowNum = 1;
                for (Part part : allParts) {
                    org.apache.poi.ss.usermodel.Row row = partsSheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(part.getName());
                    row.createCell(1).setCellValue(part.getMaker());
                    row.createCell(2).setCellValue(part.getDescription());
                    row.createCell(3).setCellValue(part.getPrice());
                    row.createCell(4).setCellValue(part.getQuantity());
                    row.createCell(5).setCellValue(part.getCategory() != null ? part.getCategory().getName() : "");
                }
                
                // Auto-size columns for parts
                for (int i = 0; i < partsHeaders.length; i++) {
                    partsSheet.autoSizeColumn(i);
                }
                
                // ===== CATEGORIES SHEET =====
                org.apache.poi.ss.usermodel.Sheet categoriesSheet = workbook.createSheet("Categories");
                
                // Create header row for categories
                org.apache.poi.ss.usermodel.Row categoriesHeaderRow = categoriesSheet.createRow(0);
                String[] categoriesHeaders = {
                    LocaleManager.getString("categories.name"),
                    LocaleManager.getString("categories.description")
                };
                
                for (int i = 0; i < categoriesHeaders.length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = categoriesHeaderRow.createCell(i);
                    cell.setCellValue(categoriesHeaders[i]);
                    cell.setCellStyle(headerStyle);
                }
                
                // Add categories data rows
                rowNum = 1;
                for (org.chaos.office.model.Category category : allCategories) {
                    org.apache.poi.ss.usermodel.Row row = categoriesSheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(category.getName());
                    row.createCell(1).setCellValue(category.getDescription());
                }
                
                // Auto-size columns for categories
                for (int i = 0; i < categoriesHeaders.length; i++) {
                    categoriesSheet.autoSizeColumn(i);
                }
                
                // Write to file
                try (java.io.FileOutputStream fileOut = new java.io.FileOutputStream(saveFile)) {
                    workbook.write(fileOut);
                }
                workbook.close();
                
                // Show success message
                AlertHelper.showInfo(
                    LocaleManager.getString("success.title"),
                    java.text.MessageFormat.format(LocaleManager.getString("parts.export.database.success"), saveFile.getAbsolutePath())
                );
                
            } catch (Exception e) {
                AlertHelper.showError(
                    LocaleManager.getString("error.title"),
                    java.text.MessageFormat.format(LocaleManager.getString("parts.export.database.failed"), e.getMessage())
                );
            }
        }
    }
    
    public Parent getView() {
        return view;
    }
}
