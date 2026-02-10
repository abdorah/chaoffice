package org.chaos.office.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.chaos.office.model.Part;
import org.chaos.office.util.LocaleManager;

/**
 * PartsInventoryView - Parts inventory management UI
 * Requirements: 1.2, 1.3, 4.1, 13.1
 */
public class PartsInventoryView extends BorderPane {
    
    private final TextField searchField;
    private final ComboBox<String> categoryFilter;
    private final TableView<Part> partsTable;
    private final Button addButton;
    private final Button editButton;
    private final Button deleteButton;
    private final Button importButton;
    private final Button downloadTemplateButton;
    private final ProgressIndicator progressIndicator;
    
    public PartsInventoryView() {
        setPadding(new Insets(16));
        
        // Top search bar
        HBox topBar = createTopBar();
        setTop(topBar);
        
        // Center table - make it grow to fill available space
        partsTable = createPartsTable();
        VBox.setVgrow(partsTable, Priority.ALWAYS);
        setCenter(partsTable);
        
        // Bottom buttons
        HBox bottomBar = createBottomBar();
        setBottom(bottomBar);
        
        // Initialize components
        searchField = (TextField) ((HBox) topBar.getChildren().get(0)).getChildren().get(0);
        categoryFilter = (ComboBox<String>) ((HBox) topBar.getChildren().get(1)).getChildren().get(0);
        addButton = (Button) bottomBar.getChildren().get(0);
        editButton = (Button) bottomBar.getChildren().get(1);
        deleteButton = (Button) bottomBar.getChildren().get(2);
        importButton = (Button) bottomBar.getChildren().get(3);
        downloadTemplateButton = (Button) bottomBar.getChildren().get(4);
        progressIndicator = (ProgressIndicator) bottomBar.getChildren().get(5);
    }
    
    private HBox createTopBar() {
        HBox topBar = new HBox(16);
        topBar.setPadding(new Insets(0, 0, 16, 0));
        
        // Search field
        HBox searchBox = new HBox(8);
        TextField search = new TextField();
        search.setPromptText(LocaleManager.getString("parts.search.placeholder"));
        search.setPrefWidth(300);
        searchBox.getChildren().add(search);
        
        // Category filter
        HBox filterBox = new HBox(8);
        ComboBox<String> filter = new ComboBox<>();
        filter.setPromptText(LocaleManager.getString("parts.filter.category"));
        filter.setPrefWidth(200);
        filterBox.getChildren().add(filter);
        
        topBar.getChildren().addAll(searchBox, filterBox);
        return topBar;
    }
    
    private TableView<Part> createPartsTable() {
        TableView<Part> table = new TableView<>();
        
        // Set column resize policy to fill available width
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        TableColumn<Part, Integer> idCol = new TableColumn<>(LocaleManager.getString("parts.id"));
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setMinWidth(50);
        idCol.setMaxWidth(80);
        
        TableColumn<Part, String> nameCol = new TableColumn<>(LocaleManager.getString("parts.name"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setMinWidth(150);
        
        TableColumn<Part, String> makerCol = new TableColumn<>(LocaleManager.getString("parts.maker"));
        makerCol.setCellValueFactory(new PropertyValueFactory<>("maker"));
        makerCol.setMinWidth(120);
        
        TableColumn<Part, String> descCol = new TableColumn<>(LocaleManager.getString("parts.description"));
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setMinWidth(200);
        
        TableColumn<Part, Float> priceCol = new TableColumn<>(LocaleManager.getString("parts.price"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setMinWidth(80);
        priceCol.setMaxWidth(120);
        
        TableColumn<Part, Integer> qtyCol = new TableColumn<>(LocaleManager.getString("parts.quantity"));
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setMinWidth(80);
        qtyCol.setMaxWidth(120);
        
        table.getColumns().addAll(idCol, nameCol, makerCol, descCol, priceCol, qtyCol);
        return table;
    }
    
    private HBox createBottomBar() {
        HBox bottomBar = new HBox(8);
        bottomBar.setPadding(new Insets(16, 0, 0, 0));
        
        Button add = new Button(LocaleManager.getString("parts.add"));
        Button edit = new Button(LocaleManager.getString("parts.edit"));
        Button delete = new Button(LocaleManager.getString("parts.delete"));
        Button importBtn = new Button(LocaleManager.getString("parts.import"));
        Button downloadTemplate = new Button(LocaleManager.getString("parts.download.template"));
        
        // Add upload icon to import button
        importBtn.setGraphic(createUploadIcon());
        
        // Add download icon to template button
        downloadTemplate.setGraphic(createDownloadIcon());
        
        // Add help tooltip explaining file format
        Tooltip importTooltip = new Tooltip(
            "Expected file format:\n\n" +
            "Columns (in order):\n" +
            "1. Name\n" +
            "2. Maker\n" +
            "3. Description\n" +
            "4. Price\n" +
            "5. Quantity\n" +
            "6. Category\n\n" +
            "Note: Category names must match existing categories exactly.\n" +
            "Supported formats: CSV (.csv), Excel (.xlsx, .xls)"
        );
        importTooltip.setShowDelay(javafx.util.Duration.millis(300));
        importBtn.setTooltip(importTooltip);
        
        // Add tooltip to download template button
        Tooltip templateTooltip = new Tooltip(
            "Download a CSV template file with example data\n" +
            "to help you format your import file correctly."
        );
        templateTooltip.setShowDelay(javafx.util.Duration.millis(300));
        downloadTemplate.setTooltip(templateTooltip);
        
        // Create progress indicator (initially hidden)
        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(24, 24);
        progress.setVisible(false);
        progress.setManaged(false); // Don't take up space when hidden
        
        bottomBar.getChildren().addAll(add, edit, delete, importBtn, downloadTemplate, progress);
        return bottomBar;
    }
    
    /**
     * Creates a simple upload icon using JavaFX shapes
     * @return An ImageView containing the upload icon graphic
     */
    private javafx.scene.image.ImageView createUploadIcon() {
        // Create a simple upload icon using Canvas
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(16, 16);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Set color for the icon
        gc.setFill(javafx.scene.paint.Color.rgb(60, 60, 60));
        gc.setStroke(javafx.scene.paint.Color.rgb(60, 60, 60));
        gc.setLineWidth(1.5);
        
        // Draw upload arrow (up arrow)
        // Arrow shaft
        gc.strokeLine(8, 12, 8, 4);
        
        // Arrow head
        gc.strokeLine(8, 4, 5, 7);
        gc.strokeLine(8, 4, 11, 7);
        
        // Base line
        gc.strokeLine(3, 14, 13, 14);
        
        // Convert canvas to image and return as ImageView
        javafx.scene.image.WritableImage image = new javafx.scene.image.WritableImage(16, 16);
        canvas.snapshot(null, image);
        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
        
        return imageView;
    }
    
    /**
     * Creates a simple download icon using JavaFX shapes
     * @return An ImageView containing the download icon graphic
     */
    private javafx.scene.image.ImageView createDownloadIcon() {
        // Create a simple download icon using Canvas
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(16, 16);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Set color for the icon
        gc.setFill(javafx.scene.paint.Color.rgb(60, 60, 60));
        gc.setStroke(javafx.scene.paint.Color.rgb(60, 60, 60));
        gc.setLineWidth(1.5);
        
        // Draw download arrow (down arrow)
        // Arrow shaft
        gc.strokeLine(8, 4, 8, 12);
        
        // Arrow head
        gc.strokeLine(8, 12, 5, 9);
        gc.strokeLine(8, 12, 11, 9);
        
        // Base line
        gc.strokeLine(3, 14, 13, 14);
        
        // Convert canvas to image and return as ImageView
        javafx.scene.image.WritableImage image = new javafx.scene.image.WritableImage(16, 16);
        canvas.snapshot(null, image);
        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
        
        return imageView;
    }
    
    public TextField getSearchField() { return searchField; }
    public ComboBox<String> getCategoryFilter() { return categoryFilter; }
    public TableView<Part> getPartsTable() { return partsTable; }
    public Button getAddButton() { return addButton; }
    public Button getEditButton() { return editButton; }
    public Button getDeleteButton() { return deleteButton; }
    public Button getImportButton() { return importButton; }
    public Button getDownloadTemplateButton() { return downloadTemplateButton; }
    public ProgressIndicator getProgressIndicator() { return progressIndicator; }
}
