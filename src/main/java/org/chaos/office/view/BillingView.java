package org.chaos.office.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.chaos.office.model.Command;
import org.chaos.office.model.Part;
import org.chaos.office.util.LocaleManager;

/**
 * View for creating bills/sales transactions.
 */
public class BillingView extends BorderPane {
    
    private final TextField clientNameField;
    private final TextField clientPhoneField;
    private final ComboBox<Part> partSelector;
    private final Spinner<Integer> quantitySpinner;
    private final Button addPartButton;
    private final TableView<Command> commandsTable;
    private final Label totalLabel;
    private final Button completeSaleButton;
    
    public BillingView() {
        // Top: Client information
        Label titleLabel = new Label(LocaleManager.getString("billing.title"));
        titleLabel.getStyleClass().add("title-label");
        
        GridPane clientGrid = new GridPane();
        clientGrid.setHgap(10);
        clientGrid.setVgap(10);
        clientGrid.setPadding(new Insets(20));
        
        Label clientNameLabel = new Label(LocaleManager.getString("billing.client.name"));
        clientNameField = new TextField();
        clientNameField.setPromptText(LocaleManager.getString("billing.client.name"));
        
        Label clientPhoneLabel = new Label(LocaleManager.getString("billing.client.phone"));
        clientPhoneField = new TextField();
        clientPhoneField.setPromptText(LocaleManager.getString("billing.client.phone"));
        
        clientGrid.add(clientNameLabel, 0, 0);
        clientGrid.add(clientNameField, 1, 0);
        clientGrid.add(clientPhoneLabel, 2, 0);
        clientGrid.add(clientPhoneField, 3, 0);
        
        VBox topBox = new VBox(10, titleLabel, clientGrid);
        topBox.setPadding(new Insets(20));
        setTop(topBox);
        
        // Center: Commands table
        commandsTable = new TableView<>();
        commandsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        TableColumn<Command, String> partNameCol = new TableColumn<>("Part Name");
        partNameCol.setCellValueFactory(new PropertyValueFactory<>("partName"));
        partNameCol.setPrefWidth(200);
        
        TableColumn<Command, Integer> quantityCol = new TableColumn<>(LocaleManager.getString("billing.part.quantity"));
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityCol.setPrefWidth(100);
        
        TableColumn<Command, Float> priceCol = new TableColumn<>(LocaleManager.getString("parts.price"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("priceConsidered"));
        priceCol.setPrefWidth(100);
        
        TableColumn<Command, Float> subtotalCol = new TableColumn<>("Subtotal");
        subtotalCol.setCellValueFactory(cellData -> {
            Command cmd = cellData.getValue();
            float subtotal = cmd.getQuantity() * cmd.getPriceConsidered();
            return new javafx.beans.property.SimpleFloatProperty(subtotal).asObject();
        });
        subtotalCol.setPrefWidth(100);
        
        commandsTable.getColumns().addAll(partNameCol, quantityCol, priceCol, subtotalCol);
        
        VBox centerBox = new VBox(10, commandsTable);
        centerBox.setPadding(new Insets(20));
        setCenter(centerBox);
        
        // Right: Part selection
        VBox rightBox = new VBox(15);
        rightBox.setPadding(new Insets(20));
        rightBox.setPrefWidth(300);
        
        Label selectPartLabel = new Label(LocaleManager.getString("billing.part.select"));
        selectPartLabel.getStyleClass().add("label-subtitle");
        
        partSelector = new ComboBox<>();
        partSelector.setPromptText(LocaleManager.getString("billing.part.select"));
        partSelector.setPrefWidth(250);
        
        Label quantityLabel = new Label(LocaleManager.getString("billing.part.quantity"));
        quantitySpinner = new Spinner<>(1, 1000, 1);
        quantitySpinner.setEditable(true);
        quantitySpinner.setPrefWidth(250);
        
        addPartButton = new Button(LocaleManager.getString("billing.part.add"));
        addPartButton.getStyleClass().add("primary-button");
        addPartButton.setPrefWidth(250);
        
        rightBox.getChildren().addAll(
            selectPartLabel,
            partSelector,
            quantityLabel,
            quantitySpinner,
            addPartButton
        );
        
        setRight(rightBox);
        
        // Bottom: Total and complete sale
        totalLabel = new Label(LocaleManager.getString("billing.total") + ": $0.00");
        totalLabel.getStyleClass().add("label-headline");
        
        completeSaleButton = new Button(LocaleManager.getString("billing.complete"));
        completeSaleButton.getStyleClass().add("success-button");
        completeSaleButton.setPrefWidth(200);
        
        HBox bottomBox = new HBox(20, totalLabel, completeSaleButton);
        bottomBox.setPadding(new Insets(20));
        bottomBox.setAlignment(Pos.CENTER_RIGHT);
        setBottom(bottomBox);
        
        // Apply styling
        getStyleClass().add("content-pane");
    }
    
    public TextField getClientNameField() {
        return clientNameField;
    }
    
    public TextField getClientPhoneField() {
        return clientPhoneField;
    }
    
    public ComboBox<Part> getPartSelector() {
        return partSelector;
    }
    
    public Spinner<Integer> getQuantitySpinner() {
        return quantitySpinner;
    }
    
    public Button getAddPartButton() {
        return addPartButton;
    }
    
    public TableView<Command> getCommandsTable() {
        return commandsTable;
    }
    
    public Label getTotalLabel() {
        return totalLabel;
    }
    
    public Button getCompleteSaleButton() {
        return completeSaleButton;
    }
    
    public void updateTotal(float total) {
        totalLabel.setText(String.format("%s: $%.2f", LocaleManager.getString("billing.total"), total));
    }
    
    public void clearForm() {
        clientNameField.clear();
        clientPhoneField.clear();
        commandsTable.getItems().clear();
        quantitySpinner.getValueFactory().setValue(1);
        partSelector.getSelectionModel().clearSelection();
        updateTotal(0);
    }
}
