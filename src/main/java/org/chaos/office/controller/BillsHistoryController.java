package org.chaos.office.controller;

import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.chaos.office.model.Bill;
import org.chaos.office.model.Command;
import org.chaos.office.service.BillService;
import org.chaos.office.service.ReportService;
import org.chaos.office.util.AlertHelper;
import org.chaos.office.util.LocaleManager;
import org.chaos.office.view.BillsHistoryView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller for bills history.
 */
public class BillsHistoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(BillsHistoryController.class);
    private final BillsHistoryView view;
    private final BillService billService;
    private final ReportService reportService;
    private final Stage stage;
    
    public BillsHistoryController(Stage stage) {
        this.stage = stage;
        this.view = new BillsHistoryView();
        this.billService = new BillService();
        this.reportService = new ReportService();
        
        // Load initial bills
        loadBills();
        
        // Set up event handlers
        view.getFilterButton().setOnAction(e -> loadBills());
        view.getViewDetailsButton().setOnAction(e -> handleViewDetails());
        view.getGeneratePdfButton().setOnAction(e -> handleGeneratePdf());
    }
    
    public Parent getView() {
        return view;
    }
    
    /**
     * Loads bills based on date range filter.
     */
    private void loadBills() {
        LocalDate startDate = view.getStartDatePicker().getValue();
        LocalDate endDate = view.getEndDatePicker().getValue();
        
        if (startDate == null || endDate == null) {
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                "Please select both start and end dates"
            );
            return;
        }
        
        if (startDate.isAfter(endDate)) {
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                "Start date must be before end date"
            );
            return;
        }
        
        List<Bill> bills = billService.filterByDateRange(startDate, endDate);
        view.getBillsTable().setItems(FXCollections.observableArrayList(bills));
        
        logger.info("Loaded {} bills between {} and {}", bills.size(), startDate, endDate);
    }
    
    /**
     * Handles viewing bill details.
     */
    private void handleViewDetails() {
        Bill selectedBill = view.getBillsTable().getSelectionModel().getSelectedItem();
        if (selectedBill == null) {
            return;
        }
        
        // Get commands for this bill
        List<Command> commands = billService.getCommandsForBill(selectedBill.getId());
        
        // Create dialog to show details
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Bill Details - #" + selectedBill.getId());
        dialog.setHeaderText(String.format("Client: %s | Phone: %s | Date: %s",
            selectedBill.getClientName(),
            selectedBill.getClientPhone(),
            selectedBill.getDate()));
        
        // Create table for commands
        TableView<Command> commandsTable = new TableView<>();
        commandsTable.setItems(FXCollections.observableArrayList(commands));
        
        TableColumn<Command, String> partCol = new TableColumn<>("Part");
        partCol.setCellValueFactory(cellData -> {
            // Get part name from part ID
            return new javafx.beans.property.SimpleStringProperty("Part #" + cellData.getValue().getPartId());
        });
        
        TableColumn<Command, Integer> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(cellData -> cellData.getValue().quantityProperty().asObject());
        
        TableColumn<Command, Float> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(cellData -> cellData.getValue().priceConsideredProperty().asObject());
        
        TableColumn<Command, String> subtotalCol = new TableColumn<>("Subtotal");
        subtotalCol.setCellValueFactory(cellData -> {
            Command cmd = cellData.getValue();
            float subtotal = cmd.getQuantity() * cmd.getPriceConsidered();
            return new javafx.beans.property.SimpleStringProperty(String.format("$%.2f", subtotal));
        });
        
        commandsTable.getColumns().addAll(partCol, qtyCol, priceCol, subtotalCol);
        commandsTable.setPrefHeight(300);
        
        Label totalLabel = new Label(String.format("Total: $%.2f", selectedBill.getTotalPrice()));
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        GridPane content = new GridPane();
        content.setVgap(10);
        content.add(commandsTable, 0, 0);
        content.add(totalLabel, 0, 1);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        dialog.showAndWait();
        
        logger.info("Viewed details for bill #{}", selectedBill.getId());
    }
    
    /**
     * Handles generating PDF for selected bill.
     */
    private void handleGeneratePdf() {
        Bill selectedBill = view.getBillsTable().getSelectionModel().getSelectedItem();
        if (selectedBill == null) {
            return;
        }
        
        // Get commands for this bill
        List<Command> commands = billService.getCommandsForBill(selectedBill.getId());
        
        // Show file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF");
        fileChooser.setInitialFileName(String.format("bill_%d_%s.pdf", 
            selectedBill.getId(), 
            selectedBill.getDate().toString()));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                reportService.generateBillPDF(selectedBill, commands, file);
                
                AlertHelper.showInfo(
                    LocaleManager.getString("success.title"),
                    "PDF generated successfully: " + file.getName()
                );
                
                logger.info("Generated PDF for bill #{} at {}", selectedBill.getId(), file.getAbsolutePath());
            } catch (Exception e) {
                logger.error("Error generating PDF", e);
                AlertHelper.showError(
                    LocaleManager.getString("error.title"),
                    "Failed to generate PDF: " + e.getMessage()
                );
            }
        }
    }
}
