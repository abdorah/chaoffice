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
        
        TableColumn<Part, Integer> idCol = new TableColumn<>(LocaleManager.getString("parts.id"));
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);
        
        TableColumn<Part, String> nameCol = new TableColumn<>(LocaleManager.getString("parts.name"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(150);
        
        TableColumn<Part, String> makerCol = new TableColumn<>(LocaleManager.getString("parts.maker"));
        makerCol.setCellValueFactory(new PropertyValueFactory<>("maker"));
        makerCol.setPrefWidth(120);
        
        TableColumn<Part, String> descCol = new TableColumn<>(LocaleManager.getString("parts.description"));
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(200);
        
        TableColumn<Part, Float> priceCol = new TableColumn<>(LocaleManager.getString("parts.price"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(80);
        
        TableColumn<Part, Integer> qtyCol = new TableColumn<>(LocaleManager.getString("parts.quantity"));
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setPrefWidth(80);
        
        table.getColumns().addAll(idCol, nameCol, makerCol, descCol, priceCol, qtyCol);
        return table;
    }
    
    private HBox createBottomBar() {
        HBox bottomBar = new HBox(8);
        bottomBar.setPadding(new Insets(16, 0, 0, 0));
        
        Button add = new Button(LocaleManager.getString("parts.add"));
        Button edit = new Button(LocaleManager.getString("parts.edit"));
        Button delete = new Button(LocaleManager.getString("parts.delete"));
        
        bottomBar.getChildren().addAll(add, edit, delete);
        return bottomBar;
    }
    
    public TextField getSearchField() { return searchField; }
    public ComboBox<String> getCategoryFilter() { return categoryFilter; }
    public TableView<Part> getPartsTable() { return partsTable; }
    public Button getAddButton() { return addButton; }
    public Button getEditButton() { return editButton; }
    public Button getDeleteButton() { return deleteButton; }
}
