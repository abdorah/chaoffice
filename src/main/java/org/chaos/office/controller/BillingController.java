package org.chaos.office.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.TableColumn;
import javafx.stage.Stage;
import org.chaos.office.model.Bill;
import org.chaos.office.model.Command;
import org.chaos.office.model.Part;
import org.chaos.office.service.BillService;
import org.chaos.office.service.PartService;
import org.chaos.office.util.AlertHelper;
import org.chaos.office.util.LocaleManager;
import org.chaos.office.util.ToastNotification;
import org.chaos.office.util.TooltipHelper;
import org.chaos.office.util.ValidationHelper;
import org.chaos.office.view.BillingView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Controller for billing/sales transactions.
 */
public class BillingController {
    
    private static final Logger logger = LoggerFactory.getLogger(BillingController.class);
    private final BillingView view;
    private final PartService partService;
    private final BillService billService;
    private final ObservableList<Command> commands;
    
    public BillingController(Stage stage) {
        this.view = new BillingView();
        this.partService = new PartService();
        this.billService = new BillService();
        this.commands = FXCollections.observableArrayList();
        
        // Bind table to commands list
        view.getCommandsTable().setItems(commands);
        
        // Set up part selection callback
        view.getPartSearchComponent().setOnPartSelected(this::handlePartSelected);
        
        // Set up event handlers
        view.getAddPartButton().setOnAction(e -> handleAddPart());
        view.getCompleteSaleButton().setOnAction(e -> handleCompleteSale());
        
        // Set up price edit handler on the price column
        @SuppressWarnings("unchecked")
        TableColumn<Command, Float> priceColumn = (TableColumn<Command, Float>) view.getCommandsTable().getColumns().get(2);
        priceColumn.setOnEditCommit(this::handlePriceEdit);
        
        // Set up discount change listeners
        view.getDiscountTypeGroup().selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            updateTotals();
        });
        view.getDiscountValueField().textProperty().addListener((obs, oldVal, newVal) -> {
            updateTotals();
        });
    }
    
    public Parent getView() {
        return view;
    }
    
    /**
     * Handles part selection from the search component.
     */
    private void handlePartSelected(Part part) {
        // This is called when a part is clicked in the search component
        // The actual adding is done by handleAddPart button
        logger.info("Part selected from search: {}", part.getName());
    }
    
    /**
     * Handles adding a part to the bill.
     * Validates stock availability and provides detailed error feedback.
     */
    private void handleAddPart() {
        Part selectedPart = view.getPartSearchComponent().getResultsView().getSelectionModel().getSelectedItem();
        if (selectedPart == null) {
            ToastNotification.showWarning(LocaleManager.getString("billing.select.part"));
            return;
        }
        
        // Check for zero-stock parts first (Requirement 12.5)
        if (selectedPart.getQuantity() <= 0) {
            AlertHelper.showError(
                LocaleManager.getString("parts.out.of.stock.title"),
                java.text.MessageFormat.format(LocaleManager.getString("parts.out.of.stock.message.short"), 
                    selectedPart.getName())
            );
            logger.warn("Attempted to add zero-stock part: {}", selectedPart.getName());
            return;
        }
        
        int quantity = view.getQuantitySpinner().getValue();
        
        // Validate quantity
        if (quantity <= 0) {
            ToastNotification.showError(LocaleManager.getString("billing.quantity.positive"));
            return;
        }
        
        // Check for insufficient stock (Requirement 9.2)
        if (quantity > selectedPart.getQuantity()) {
            AlertHelper.showError(
                LocaleManager.getString("error.insufficient.stock.title"),
                java.text.MessageFormat.format(
                    LocaleManager.getString("error.insufficient.stock.message"),
                    quantity, selectedPart.getName(), selectedPart.getQuantity())
            );
            logger.warn("Insufficient stock for part {}: requested {}, available {}", 
                       selectedPart.getName(), quantity, selectedPart.getQuantity());
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
                        LocaleManager.getString("error.insufficient.stock.title"),
                        java.text.MessageFormat.format(
                            LocaleManager.getString("error.insufficient.stock.update.message"),
                            quantity, selectedPart.getName(), cmd.getQuantity(), newQuantity, selectedPart.getQuantity())
                    );
                    logger.warn("Insufficient stock for part {}: current {}, requested {}, available {}", 
                               selectedPart.getName(), cmd.getQuantity(), newQuantity, selectedPart.getQuantity());
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
        updateTotals();
        
        // Reset quantity spinner
        view.getQuantitySpinner().getValueFactory().setValue(1);
        
        // Show success feedback
        ToastNotification.showSuccess(
            java.text.MessageFormat.format(LocaleManager.getString("billing.part.added"), 
                quantity, selectedPart.getName())
        );
        
        logger.info("Added part {} with quantity {} to bill", selectedPart.getName(), quantity);
    }
    
    /**
     * Handles price editing in the commands table.
     * Validates price input and provides visual error feedback.
     */
    private void handlePriceEdit(javafx.scene.control.TableColumn.CellEditEvent<Command, Float> event) {
        Command command = event.getRowValue();
        Float newPrice = event.getNewValue();
        
        // Validate price (Requirement 1.4)
        if (newPrice == null || newPrice <= 0) {
            AlertHelper.showError(
                "Invalid Price",
                String.format("Price must be greater than zero.\n\nPlease enter a valid positive number for '%s'.", 
                    command.getPartName())
            );
            logger.warn("Invalid price edit attempted for {}: {}", command.getPartName(), newPrice);
            
            // Revert to old value
            view.getCommandsTable().refresh();
            return;
        }
        
        // Check for unreasonably high prices (warning, not blocking)
        if (newPrice > 100000) {
            boolean confirmed = AlertHelper.showConfirmation(
                "Confirm High Price",
                String.format("The price $%.2f for '%s' is unusually high.\n\nDo you want to continue?", 
                    newPrice, command.getPartName())
            );
            
            if (!confirmed) {
                view.getCommandsTable().refresh();
                return;
            }
        }
        
        // Update price
        float oldPrice = command.getPriceConsidered();
        command.setPriceConsidered(newPrice);
        
        // Refresh table and update totals
        view.getCommandsTable().refresh();
        updateTotals();
        
        // Show success feedback
        ToastNotification.showSuccess(
            java.text.MessageFormat.format(LocaleManager.getString("billing.price.updated"), 
                command.getPartName(), oldPrice, newPrice)
        );
        
        logger.info("Updated price for {} from ${} to ${}", command.getPartName(), oldPrice, newPrice);
    }
    
    /**
     * Updates the total price display with discount calculation.
     * Provides real-time validation feedback with visual error indicators.
     */
    private void updateTotals() {
        // Calculate subtotal
        float subtotal = 0;
        for (Command cmd : commands) {
            subtotal += cmd.getQuantity() * cmd.getPriceConsidered();
        }
        
        // Calculate discount
        float discountAmount = 0;
        String discountType = view.getSelectedDiscountType();
        boolean hasDiscountError = false;
        
        if (!discountType.equals("none")) {
            String discountValueText = view.getDiscountValueField().getText().trim();
            if (!discountValueText.isEmpty()) {
                try {
                    float discountValue = Float.parseFloat(discountValueText);
                    
                    if (discountType.equals("percentage")) {
                        // Validate percentage discount (Requirement 2.2)
                        if (discountValue < 0 || discountValue > 100) {
                            TooltipHelper.showError(view.getDiscountValueField(), 
                                "Percentage must be between 0 and 100");
                            hasDiscountError = true;
                        } else {
                            TooltipHelper.clearError(view.getDiscountValueField());
                            discountAmount = subtotal * (discountValue / 100);
                        }
                    } else if (discountType.equals("fixed")) {
                        // Validate fixed discount (Requirement 2.4)
                        if (discountValue < 0) {
                            TooltipHelper.showError(view.getDiscountValueField(), 
                                "Discount cannot be negative");
                            hasDiscountError = true;
                        } else if (discountValue > subtotal) {
                            TooltipHelper.showError(view.getDiscountValueField(), 
                                String.format("Discount cannot exceed subtotal of $%.2f", subtotal));
                            hasDiscountError = true;
                        } else {
                            TooltipHelper.clearError(view.getDiscountValueField());
                            discountAmount = discountValue;
                        }
                    }
                } catch (NumberFormatException e) {
                    // Invalid number format
                    TooltipHelper.showError(view.getDiscountValueField(), 
                        "Please enter a valid number");
                    hasDiscountError = true;
                }
            } else {
                // Clear error if field is empty
                TooltipHelper.clearError(view.getDiscountValueField());
            }
        } else {
            // Clear error when discount type is "none"
            TooltipHelper.clearError(view.getDiscountValueField());
        }
        
        // Calculate final total
        float finalTotal = subtotal - discountAmount;
        
        // Update display
        view.updateTotals(subtotal, discountAmount, finalTotal);
        
        // Disable complete sale button if there are errors
        view.getCompleteSaleButton().setDisable(hasDiscountError || commands.isEmpty());
    }
    
    /**
     * Handles completing the sale.
     * Validates all inputs, checks stock availability, and provides comprehensive error feedback.
     */
    private void handleCompleteSale() {
        // Clear any previous error styling
        TooltipHelper.clearError(view.getClientNameField());
        TooltipHelper.clearError(view.getClientPhoneField());
        
        // Get client information (now optional)
        String clientName = view.getClientNameField().getText().trim();
        String clientPhone = view.getClientPhoneField().getText().trim();
        
        // Client name and phone are now optional - no validation needed
        
        if (commands.isEmpty()) {
            AlertHelper.showError(
                LocaleManager.getString("dialog.title.empty.bill"),
                LocaleManager.getString("error.billing.empty")
            );
            return;
        }
        
        // Calculate subtotal
        float subtotal = 0;
        for (Command cmd : commands) {
            subtotal += cmd.getQuantity() * cmd.getPriceConsidered();
        }
        
        // Get discount information
        String discountType = view.getSelectedDiscountType();
        float discountValue = 0;
        float discountAmount = 0;
        
        if (!discountType.equals("none")) {
            String discountValueText = view.getDiscountValueField().getText().trim();
            if (!discountValueText.isEmpty()) {
                try {
                    discountValue = Float.parseFloat(discountValueText);
                    
                    // Validate discount (Requirement 2.2, 2.4)
                    if (discountType.equals("percentage")) {
                        if (discountValue < 0 || discountValue > 100) {
                            TooltipHelper.showError(view.getDiscountValueField(), 
                                "Percentage must be between 0 and 100");
                            AlertHelper.showError(
                                LocaleManager.getString("dialog.title.invalid.discount"),
                                LocaleManager.getString("error.billing.discount.percentage.range")
                            );
                            return;
                        }
                        discountAmount = subtotal * (discountValue / 100);
                    } else if (discountType.equals("fixed")) {
                        if (discountValue < 0 || discountValue > subtotal) {
                            TooltipHelper.showError(view.getDiscountValueField(), 
                                String.format("Discount cannot exceed subtotal of $%.2f", subtotal));
                            AlertHelper.showError(
                                LocaleManager.getString("dialog.title.invalid.discount"),
                                java.text.MessageFormat.format(LocaleManager.getString("error.billing.discount.fixed.exceeds"), subtotal)
                            );
                            return;
                        }
                        discountAmount = discountValue;
                    }
                } catch (NumberFormatException e) {
                    TooltipHelper.showError(view.getDiscountValueField(), "Please enter a valid number");
                    AlertHelper.showError(
                        LocaleManager.getString("dialog.title.invalid.discount"),
                        LocaleManager.getString("error.billing.discount.invalid")
                    );
                    return;
                }
            }
        }
        
        // Calculate final total
        float finalTotal = subtotal - discountAmount;
        
        // Get payment method
        String paymentMethod = view.getSelectedPaymentMethod();
        
        // Create bill with POS fields
        Bill bill = new Bill();
        bill.setClientName(clientName);
        bill.setClientPhone(clientPhone);
        bill.setSubtotal(subtotal);
        bill.setDiscountType(discountType);
        bill.setDiscountValue(discountValue);
        bill.setTotalPrice(finalTotal);
        bill.setPaymentMethod(paymentMethod);
        bill.setDate(LocalDate.now());
        
        // Save bill with comprehensive error handling
        try {
            billService.saveBill(bill, new ArrayList<>(commands));
            
            // Show success notification
            ToastNotification.showSuccess(
                java.text.MessageFormat.format(LocaleManager.getString("billing.sale.completed"), 
                    finalTotal)
            );
            
            // Localize payment method
            String localizedPaymentMethod = LocaleManager.getString("payment.method." + paymentMethod.toLowerCase());
            
            // Show appropriate message based on whether client name is provided
            String successMessage;
            if (clientName == null || clientName.trim().isEmpty()) {
                successMessage = java.text.MessageFormat.format(
                    LocaleManager.getString("success.billing.completed.no.client"), 
                    finalTotal, localizedPaymentMethod
                );
            } else {
                successMessage = java.text.MessageFormat.format(
                    LocaleManager.getString("success.billing.completed"), 
                    clientName, finalTotal, localizedPaymentMethod
                );
            }
            
            AlertHelper.showInfo(
                LocaleManager.getString("dialog.title.sale.completed"),
                successMessage
            );
            
            // Clear form
            view.clearForm();
            commands.clear();
            
            // Clear any error styling
            TooltipHelper.clearError(view.getClientNameField());
            TooltipHelper.clearError(view.getClientPhoneField());
            TooltipHelper.clearError(view.getDiscountValueField());
            
            // Refresh part search to update stock quantities
            view.getPartSearchComponent().refresh();
            
            logger.info("Completed sale for client {} with total ${} (discount: {}, payment: {})", 
                clientName, finalTotal, discountType, paymentMethod);
                
        } catch (IllegalArgumentException e) {
            // Business logic errors (e.g., insufficient stock - Requirement 9.3, 9.4)
            logger.error("Validation error completing sale", e);
            
            AlertHelper.showError(
                LocaleManager.getString("dialog.title.cannot.complete.sale"),
                java.text.MessageFormat.format(LocaleManager.getString("error.billing.cannot.complete"), 
                    e.getMessage())
            );
            
            // Refresh part search to show updated stock
            view.getPartSearchComponent().refresh();
            
        } catch (RuntimeException e) {
            // Database and other runtime errors - provide user-friendly message
            logger.error("Error completing sale", e);
            
            String userMessage;
            String errorMsg = e.getMessage() != null ? e.getMessage() : "";
            
            if (errorMsg.contains("UNIQUE constraint") || errorMsg.contains("unique constraint")) {
                userMessage = LocaleManager.getString("error.billing.save.duplicate");
            } else if (errorMsg.contains("NOT NULL constraint") || errorMsg.contains("not null")) {
                userMessage = LocaleManager.getString("error.billing.save.missing");
            } else if (errorMsg.contains("CHECK constraint") || errorMsg.contains("check constraint")) {
                userMessage = LocaleManager.getString("error.billing.save.invalid");
            } else if (errorMsg.contains("database") || errorMsg.contains("Database")) {
                userMessage = java.text.MessageFormat.format(LocaleManager.getString("error.billing.save.database"), errorMsg);
            } else {
                userMessage = java.text.MessageFormat.format(LocaleManager.getString("error.billing.save.general"), errorMsg);
            }
            
            AlertHelper.showError(LocaleManager.getString("dialog.title.error.saving.bill"), userMessage);
            
        } catch (Exception e) {
            // Unexpected errors
            logger.error("Unexpected error completing sale", e);
            
            AlertHelper.showError(
                LocaleManager.getString("dialog.title.unexpected.error"),
                java.text.MessageFormat.format(LocaleManager.getString("error.billing.unexpected"), 
                    e.getMessage())
            );
        }
    }
}
