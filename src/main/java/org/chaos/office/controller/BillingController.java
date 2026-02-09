package org.chaos.office.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.chaos.office.model.Bill;
import org.chaos.office.model.Command;
import org.chaos.office.model.Part;
import org.chaos.office.service.BillService;
import org.chaos.office.service.PartService;
import org.chaos.office.util.AlertHelper;
import org.chaos.office.util.LocaleManager;
import org.chaos.office.util.ValidationHelper;
import org.chaos.office.view.BillingView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for billing/sales transactions.
 */
public class BillingController extends Scene {
    
    private static final Logger logger = LoggerFactory.getLogger(BillingController.class);
    private final BillingView view;
    private final PartService partService;
    private final BillService billService;
    private final ObservableList<Command> commands;
    
    public BillingController(Stage stage) {
        super(new BillingView(), 1200, 800);
        this.view = (BillingView) getRoot();
        this.partService = new PartService();
        this.billService = new BillService();
        this.commands = FXCollections.observableArrayList();
        
        // Apply stylesheet
        getStylesheets().add(getClass().getResource("/style/main.css").toExternalForm());
        
        // Bind table to commands list
        view.getCommandsTable().setItems(commands);
        
        // Load parts into selector
        loadParts();
        
        // Set up event handlers
        view.getAddPartButton().setOnAction(e -> handleAddPart());
        view.getCompleteSaleButton().setOnAction(e -> handleCompleteSale());
    }
    
    /**
     * Loads all parts into the part selector.
     */
    private void loadParts() {
        List<Part> parts = partService.getAllParts();
        view.getPartSelector().setItems(FXCollections.observableArrayList(parts));
        
        // Set custom string converter to display part name
        view.getPartSelector().setConverter(new StringConverter<Part>() {
            @Override
            public String toString(Part part) {
                if (part == null) {
                    return null;
                }
                return String.format("%s - $%.2f (Stock: %d)", 
                    part.getName(), part.getPrice(), part.getQuantity());
            }
            
            @Override
            public Part fromString(String string) {
                return null;
            }
        });
        
        logger.info("Loaded {} parts into selector", parts.size());
    }
    
    /**
     * Handles adding a part to the bill.
     */
    private void handleAddPart() {
        Part selectedPart = view.getPartSelector().getValue();
        if (selectedPart == null) {
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                "Please select a part"
            );
            return;
        }
        
        int quantity = view.getQuantitySpinner().getValue();
        
        // Validate quantity
        if (quantity <= 0) {
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                "Quantity must be positive"
            );
            return;
        }
        
        if (quantity > selectedPart.getQuantity()) {
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                String.format(LocaleManager.getString("billing.error.insufficient"), 
                    selectedPart.getName())
            );
            return;
        }
        
        // Check if part already in commands
        boolean found = false;
        for (Command cmd : commands) {
            if (cmd.getPartId() == selectedPart.getId()) {
                // Update quantity
                int newQuantity = cmd.getQuantity() + quantity;
                if (newQuantity > selectedPart.getQuantity()) {
                    AlertHelper.showError(
                        LocaleManager.getString("error.title"),
                        String.format(LocaleManager.getString("billing.error.insufficient"), 
                            selectedPart.getName())
                    );
                    return;
                }
                cmd.setQuantity(newQuantity);
                found = true;
                break;
            }
        }
        
        if (!found) {
            // Add new command
            Command command = new Command();
            command.setPartId(selectedPart.getId());
            command.setPartName(selectedPart.getName());
            command.setQuantity(quantity);
            command.setPriceConsidered(selectedPart.getPrice());
            commands.add(command);
        }
        
        // Refresh table and update total
        view.getCommandsTable().refresh();
        updateTotal();
        
        // Reset quantity spinner
        view.getQuantitySpinner().getValueFactory().setValue(1);
        
        logger.info("Added part {} with quantity {} to bill", selectedPart.getName(), quantity);
    }
    
    /**
     * Updates the total price display.
     */
    private void updateTotal() {
        float total = 0;
        for (Command cmd : commands) {
            total += cmd.getQuantity() * cmd.getPriceConsidered();
        }
        view.updateTotal(total);
    }
    
    /**
     * Handles completing the sale.
     */
    private void handleCompleteSale() {
        // Validate client information
        String clientName = view.getClientNameField().getText().trim();
        String clientPhone = view.getClientPhoneField().getText().trim();
        
        if (!ValidationHelper.isNotEmpty(clientName)) {
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                "Client name is required"
            );
            return;
        }
        
        if (!ValidationHelper.isNotEmpty(clientPhone)) {
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                "Client phone is required"
            );
            return;
        }
        
        if (commands.isEmpty()) {
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                "Please add at least one part to the bill"
            );
            return;
        }
        
        // Calculate total
        float total = 0;
        for (Command cmd : commands) {
            total += cmd.getQuantity() * cmd.getPriceConsidered();
        }
        
        // Create bill
        Bill bill = new Bill();
        bill.setClientName(clientName);
        bill.setClientPhone(clientPhone);
        bill.setTotalPrice(total);
        bill.setDate(LocalDate.now());
        
        // Save bill
        try {
            billService.saveBill(bill, new ArrayList<>(commands));
            
            AlertHelper.showInfo(
                LocaleManager.getString("billing.success.title"),
                LocaleManager.getString("billing.success.message")
            );
            
            // Clear form
            view.clearForm();
            commands.clear();
            
            // Reload parts to update stock quantities
            loadParts();
            
            logger.info("Completed sale for client {} with total ${}", clientName, total);
        } catch (IllegalArgumentException e) {
            logger.error("Error completing sale", e);
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                e.getMessage()
            );
        } catch (Exception e) {
            logger.error("Error completing sale", e);
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                LocaleManager.getString("error.save")
            );
        }
    }
}
