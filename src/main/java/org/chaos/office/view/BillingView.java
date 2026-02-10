package org.chaos.office.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.util.converter.FloatStringConverter;
import org.chaos.office.model.Command;
import org.chaos.office.util.LocaleManager;

/**
 * View for creating bills/sales transactions with POS features.
 * 
 * <p>This view provides:
 * <ul>
 *   <li>Client information input (name, phone)</li>
 *   <li>Part search and selection with PartSearchComponent</li>
 *   <li>Editable command table with price editing</li>
 *   <li>Discount controls (none, percentage, fixed amount)</li>
 *   <li>Payment method selection (cash, card, check)</li>
 *   <li>Enhanced total display (subtotal, discount, final total)</li>
 * </ul>
 * 
 * <p>Requirements: 1.1, 1.2, 2.1, 2.5, 3.2, 3.3, 11.2
 */
public class BillingView extends BorderPane {
    
    private final TextField clientNameField;
    private final TextField clientPhoneField;
    private final PartSearchComponent partSearchComponent;
    private final Spinner<Integer> quantitySpinner;
    private final Button addPartButton;
    private final TableView<Command> commandsTable;
    
    // Discount controls
    private final ToggleGroup discountTypeGroup;
    private final RadioButton discountNoneRadio;
    private final RadioButton discountPercentageRadio;
    private final RadioButton discountFixedRadio;
    private final TextField discountValueField;
    
    // Payment method controls
    private final ToggleGroup paymentMethodGroup;
    private final RadioButton paymentCashRadio;
    private final RadioButton paymentCardRadio;
    private final RadioButton paymentCheckRadio;
    
    // Total display labels
    private final Label subtotalLabel;
    private final Label discountLabel;
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
        
        // Center: Main content area with part search, commands table, and controls
        VBox centerBox = new VBox(15);
        centerBox.setPadding(new Insets(20));
        
        // Part Search Component
        Label searchLabel = new Label("Search and Select Parts");
        searchLabel.getStyleClass().add("label-subtitle");
        
        partSearchComponent = new PartSearchComponent();
        partSearchComponent.setPrefHeight(250);
        
        // Quantity controls for adding parts
        HBox quantityBox = new HBox(10);
        quantityBox.setAlignment(Pos.CENTER_LEFT);
        
        Label quantityLabel = new Label(LocaleManager.getString("billing.part.quantity"));
        quantitySpinner = new Spinner<>(1, 1000, 1);
        quantitySpinner.setEditable(true);
        quantitySpinner.setPrefWidth(100);
        
        addPartButton = new Button(LocaleManager.getString("billing.part.add"));
        addPartButton.getStyleClass().add("primary-button");
        
        quantityBox.getChildren().addAll(quantityLabel, quantitySpinner, addPartButton);
        
        // Commands table with editable price column
        commandsTable = new TableView<>();
        commandsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        commandsTable.setMinHeight(200);
        
        TableColumn<Command, String> partNameCol = new TableColumn<>("Part Name");
        partNameCol.setCellValueFactory(new PropertyValueFactory<>("partName"));
        partNameCol.setPrefWidth(200);
        
        TableColumn<Command, Integer> quantityCol = new TableColumn<>(LocaleManager.getString("billing.part.quantity"));
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityCol.setPrefWidth(100);
        
        // Editable price column using TextFieldTableCell
        TableColumn<Command, Float> priceCol = new TableColumn<>(LocaleManager.getString("parts.price"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("priceConsidered"));
        priceCol.setCellFactory(TextFieldTableCell.forTableColumn(new FloatStringConverter()));
        priceCol.setEditable(true);
        priceCol.setPrefWidth(100);
        
        TableColumn<Command, Float> subtotalCol = new TableColumn<>("Subtotal");
        subtotalCol.setCellValueFactory(cellData -> {
            Command cmd = cellData.getValue();
            float subtotal = cmd.getQuantity() * cmd.getPriceConsidered();
            return new javafx.beans.property.SimpleFloatProperty(subtotal).asObject();
        });
        subtotalCol.setPrefWidth(100);
        
        commandsTable.getColumns().addAll(partNameCol, quantityCol, priceCol, subtotalCol);
        commandsTable.setEditable(true);
        
        // Discount controls section
        Label discountLabel = new Label("Discount");
        discountLabel.getStyleClass().add("label-subtitle");
        
        discountTypeGroup = new ToggleGroup();
        
        discountNoneRadio = new RadioButton("None");
        discountNoneRadio.setToggleGroup(discountTypeGroup);
        discountNoneRadio.setSelected(true);
        
        discountPercentageRadio = new RadioButton("Percentage");
        discountPercentageRadio.setToggleGroup(discountTypeGroup);
        
        discountFixedRadio = new RadioButton("Fixed Amount");
        discountFixedRadio.setToggleGroup(discountTypeGroup);
        
        discountValueField = new TextField();
        discountValueField.setPromptText("0.00");
        discountValueField.setPrefWidth(100);
        discountValueField.setDisable(true); // Disabled when "None" is selected
        
        HBox discountBox = new HBox(15);
        discountBox.setAlignment(Pos.CENTER_LEFT);
        discountBox.getChildren().addAll(
            discountNoneRadio,
            discountPercentageRadio,
            discountFixedRadio,
            discountValueField
        );
        
        // Payment method controls section
        Label paymentLabel = new Label("Payment Method");
        paymentLabel.getStyleClass().add("label-subtitle");
        
        paymentMethodGroup = new ToggleGroup();
        
        paymentCashRadio = new RadioButton("Cash");
        paymentCashRadio.setToggleGroup(paymentMethodGroup);
        paymentCashRadio.setSelected(true);
        
        paymentCardRadio = new RadioButton("Card");
        paymentCardRadio.setToggleGroup(paymentMethodGroup);
        
        paymentCheckRadio = new RadioButton("Check");
        paymentCheckRadio.setToggleGroup(paymentMethodGroup);
        
        HBox paymentBox = new HBox(15);
        paymentBox.setAlignment(Pos.CENTER_LEFT);
        paymentBox.getChildren().addAll(
            paymentCashRadio,
            paymentCardRadio,
            paymentCheckRadio
        );
        
        // Add all components to center box
        centerBox.getChildren().addAll(
            searchLabel,
            partSearchComponent,
            quantityBox,
            new Separator(),
            commandsTable,
            new Separator(),
            discountLabel,
            discountBox,
            paymentLabel,
            paymentBox
        );
        
        VBox.setVgrow(commandsTable, Priority.ALWAYS);
        setCenter(centerBox);
        
        // Bottom: Enhanced total display and complete sale button
        VBox totalsBox = new VBox(5);
        totalsBox.setAlignment(Pos.CENTER_RIGHT);
        
        subtotalLabel = new Label("Subtotal: $0.00");
        subtotalLabel.getStyleClass().add("label-subtitle");
        
        this.discountLabel = new Label("Discount: -$0.00");
        this.discountLabel.getStyleClass().add("label-subtitle");
        
        Separator totalSeparator = new Separator();
        totalSeparator.setPrefWidth(200);
        
        totalLabel = new Label(LocaleManager.getString("billing.total") + ": $0.00");
        totalLabel.getStyleClass().add("label-headline");
        
        totalsBox.getChildren().addAll(
            subtotalLabel,
            this.discountLabel,
            totalSeparator,
            totalLabel
        );
        
        completeSaleButton = new Button(LocaleManager.getString("billing.complete"));
        completeSaleButton.getStyleClass().add("success-button");
        completeSaleButton.setPrefWidth(200);
        completeSaleButton.setPrefHeight(40);
        completeSaleButton.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        HBox bottomBox = new HBox(20, totalsBox, completeSaleButton);
        bottomBox.setPadding(new Insets(20));
        bottomBox.setAlignment(Pos.CENTER_RIGHT);
        bottomBox.setMinHeight(100);
        setBottom(bottomBox);
        
        // Setup discount type change listener to enable/disable value field
        discountTypeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == discountNoneRadio) {
                discountValueField.setDisable(true);
                discountValueField.clear();
            } else {
                discountValueField.setDisable(false);
            }
        });
        
        // Apply styling
        getStyleClass().add("content-pane");
    }
    
    public TextField getClientNameField() {
        return clientNameField;
    }
    
    public TextField getClientPhoneField() {
        return clientPhoneField;
    }
    
    public PartSearchComponent getPartSearchComponent() {
        return partSearchComponent;
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
    
    // Discount control getters
    public ToggleGroup getDiscountTypeGroup() {
        return discountTypeGroup;
    }
    
    public RadioButton getDiscountNoneRadio() {
        return discountNoneRadio;
    }
    
    public RadioButton getDiscountPercentageRadio() {
        return discountPercentageRadio;
    }
    
    public RadioButton getDiscountFixedRadio() {
        return discountFixedRadio;
    }
    
    public TextField getDiscountValueField() {
        return discountValueField;
    }
    
    // Payment method control getters
    public ToggleGroup getPaymentMethodGroup() {
        return paymentMethodGroup;
    }
    
    public RadioButton getPaymentCashRadio() {
        return paymentCashRadio;
    }
    
    public RadioButton getPaymentCardRadio() {
        return paymentCardRadio;
    }
    
    public RadioButton getPaymentCheckRadio() {
        return paymentCheckRadio;
    }
    
    // Total display label getters
    public Label getSubtotalLabel() {
        return subtotalLabel;
    }
    
    public Label getDiscountLabel() {
        return discountLabel;
    }
    
    public Label getTotalLabel() {
        return totalLabel;
    }
    
    public Button getCompleteSaleButton() {
        return completeSaleButton;
    }
    
    /**
     * Updates the total display with subtotal, discount, and final total.
     * 
     * @param subtotal the subtotal before discount
     * @param discountAmount the discount amount
     * @param total the final total after discount
     */
    public void updateTotals(float subtotal, float discountAmount, float total) {
        subtotalLabel.setText(String.format("Subtotal: $%.2f", subtotal));
        discountLabel.setText(String.format("Discount: -$%.2f", discountAmount));
        totalLabel.setText(String.format("%s: $%.2f", LocaleManager.getString("billing.total"), total));
    }
    
    /**
     * Updates the total display (legacy method for backwards compatibility).
     * 
     * @param total the final total
     */
    public void updateTotal(float total) {
        updateTotals(total, 0, total);
    }
    
    /**
     * Clears the form and resets all fields to default values.
     */
    public void clearForm() {
        clientNameField.clear();
        clientPhoneField.clear();
        commandsTable.getItems().clear();
        quantitySpinner.getValueFactory().setValue(1);
        partSearchComponent.clear();
        
        // Reset discount controls
        discountNoneRadio.setSelected(true);
        discountValueField.clear();
        discountValueField.setDisable(true);
        
        // Reset payment method to cash
        paymentCashRadio.setSelected(true);
        
        // Reset totals
        updateTotals(0, 0, 0);
    }
    
    /**
     * Gets the selected discount type.
     * 
     * @return "none", "percentage", or "fixed"
     */
    public String getSelectedDiscountType() {
        Toggle selected = discountTypeGroup.getSelectedToggle();
        if (selected == discountPercentageRadio) {
            return "percentage";
        } else if (selected == discountFixedRadio) {
            return "fixed";
        } else {
            return "none";
        }
    }
    
    /**
     * Gets the selected payment method.
     * 
     * @return "cash", "card", or "check"
     */
    public String getSelectedPaymentMethod() {
        Toggle selected = paymentMethodGroup.getSelectedToggle();
        if (selected == paymentCardRadio) {
            return "card";
        } else if (selected == paymentCheckRadio) {
            return "check";
        } else {
            return "cash";
        }
    }
}
