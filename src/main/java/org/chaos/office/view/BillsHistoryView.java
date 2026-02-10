package org.chaos.office.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.chaos.office.model.Bill;
import org.chaos.office.util.LocaleManager;

import java.time.LocalDate;

/**
 * View for displaying bills history.
 */
public class BillsHistoryView extends BorderPane {
    
    private final DatePicker startDatePicker;
    private final DatePicker endDatePicker;
    private final Button filterButton;
    private final TableView<Bill> billsTable;
    private final Button viewDetailsButton;
    private final Button generatePdfButton;
    
    public BillsHistoryView() {
        // Top: Title and date range filter
        Label titleLabel = new Label(LocaleManager.getString("bills.title"));
        titleLabel.getStyleClass().add("title-label");
        
        Label startDateLabel = new Label(LocaleManager.getString("bills.filter.start"));
        startDatePicker = new DatePicker();
        startDatePicker.setValue(LocalDate.now().minusMonths(1));
        
        Label endDateLabel = new Label(LocaleManager.getString("bills.filter.end"));
        endDatePicker = new DatePicker();
        endDatePicker.setValue(LocalDate.now());
        
        filterButton = new Button(LocaleManager.getString("common.filter"));
        filterButton.getStyleClass().add("primary-button");
        
        HBox filterBox = new HBox(10, startDateLabel, startDatePicker, endDateLabel, endDatePicker, filterButton);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        filterBox.setPadding(new Insets(10));
        
        VBox topBox = new VBox(10, titleLabel, filterBox);
        topBox.setPadding(new Insets(20));
        setTop(topBox);
        
        // Center: Bills table
        billsTable = new TableView<>();
        billsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        TableColumn<Bill, Integer> idCol = new TableColumn<>(LocaleManager.getString("bills.id"));
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(80);
        
        TableColumn<Bill, String> clientNameCol = new TableColumn<>(LocaleManager.getString("bills.client.name"));
        clientNameCol.setCellValueFactory(new PropertyValueFactory<>("clientName"));
        clientNameCol.setPrefWidth(200);
        
        TableColumn<Bill, String> clientPhoneCol = new TableColumn<>(LocaleManager.getString("bills.client.phone"));
        clientPhoneCol.setCellValueFactory(new PropertyValueFactory<>("clientPhone"));
        clientPhoneCol.setPrefWidth(150);
        
        TableColumn<Bill, Float> totalCol = new TableColumn<>(LocaleManager.getString("bills.total"));
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        totalCol.setPrefWidth(120);
        totalCol.setCellFactory(column -> new TableCell<Bill, Float>() {
            @Override
            protected void updateItem(Float item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", item));
                }
            }
        });
        
        TableColumn<Bill, LocalDate> dateCol = new TableColumn<>(LocaleManager.getString("bills.date"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setPrefWidth(120);
        
        billsTable.getColumns().addAll(idCol, clientNameCol, clientPhoneCol, totalCol, dateCol);
        
        // Make table grow to fill available space
        VBox.setVgrow(billsTable, Priority.ALWAYS);
        
        VBox centerBox = new VBox(billsTable);
        centerBox.setPadding(new Insets(20));
        setCenter(centerBox);
        
        // Bottom: Action buttons
        viewDetailsButton = new Button(LocaleManager.getString("bills.view"));
        viewDetailsButton.getStyleClass().add("secondary-button");
        viewDetailsButton.setDisable(true);
        
        generatePdfButton = new Button(LocaleManager.getString("bills.pdf"));
        generatePdfButton.getStyleClass().add("primary-button");
        generatePdfButton.setDisable(true);
        
        HBox buttonBox = new HBox(15, viewDetailsButton, generatePdfButton);
        buttonBox.setPadding(new Insets(20));
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        setBottom(buttonBox);
        
        // Enable buttons when a bill is selected
        billsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            viewDetailsButton.setDisable(!hasSelection);
            generatePdfButton.setDisable(!hasSelection);
        });
        
        // Apply styling
        getStyleClass().add("content-pane");
    }
    
    public DatePicker getStartDatePicker() {
        return startDatePicker;
    }
    
    public DatePicker getEndDatePicker() {
        return endDatePicker;
    }
    
    public Button getFilterButton() {
        return filterButton;
    }
    
    public TableView<Bill> getBillsTable() {
        return billsTable;
    }
    
    public Button getViewDetailsButton() {
        return viewDetailsButton;
    }
    
    public Button getGeneratePdfButton() {
        return generatePdfButton;
    }
}
