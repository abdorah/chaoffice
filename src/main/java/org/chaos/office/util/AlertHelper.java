package org.chaos.office.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * AlertHelper utility class for displaying consistent alerts and dialogs throughout the application.
 * Provides standardized methods for showing error messages, information messages, and confirmation dialogs.
 * 
 * <p>This class is responsible for:
 * <ul>
 *   <li>Displaying error alerts with consistent styling</li>
 *   <li>Displaying information alerts</li>
 *   <li>Displaying confirmation dialogs and capturing user response</li>
 *   <li>Applying Material Design 3 styling to all alerts</li>
 * </ul>
 * 
 * <p>Requirements: 15.1, 15.5
 */
public class AlertHelper {
    private static final Logger logger = LoggerFactory.getLogger(AlertHelper.class);
    
    // CSS class for Material Design 3 styling
    private static final String ALERT_STYLE_CLASS = "material-alert";
    
    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with static methods only.
     */
    private AlertHelper() {
        // Utility class - no instantiation
    }
    
    /**
     * Displays an error alert dialog with the specified title and message.
     * The alert is modal and blocks until the user dismisses it.
     * 
     * @param title the title of the error dialog
     * @param message the error message to display
     */
    public static void showError(String title, String message) {
        logger.error("Showing error alert - Title: {}, Message: {}", title, message);
        
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null); // Material Design 3 style - no header
        alert.setContentText(message);
        
        // Replace default button with localized OK button
        alert.getButtonTypes().setAll(createLocalizedOkButton());
        
        // Apply consistent styling
        applyAlertStyling(alert);
        
        alert.showAndWait();
    }
    
    /**
     * Displays an information alert dialog with the specified title and message.
     * The alert is modal and blocks until the user dismisses it.
     * 
     * @param title the title of the information dialog
     * @param message the information message to display
     */
    public static void showInfo(String title, String message) {
        logger.info("Showing info alert - Title: {}, Message: {}", title, message);
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null); // Material Design 3 style - no header
        alert.setContentText(message);
        
        // Replace default button with localized OK button
        alert.getButtonTypes().setAll(createLocalizedOkButton());
        
        // Apply consistent styling
        applyAlertStyling(alert);
        
        alert.showAndWait();
    }
    
    /**
     * Displays a confirmation dialog with the specified title and message.
     * The dialog presents OK and Cancel buttons to the user.
     * 
     * @param title the title of the confirmation dialog
     * @param message the confirmation message to display
     * @return true if the user clicked OK, false if the user clicked Cancel or closed the dialog
     */
    public static boolean showConfirmation(String title, String message) {
        logger.info("Showing confirmation alert - Title: {}, Message: {}", title, message);
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null); // Material Design 3 style - no header
        alert.setContentText(message);
        
        // Replace default buttons with localized ones
        alert.getButtonTypes().setAll(createLocalizedOkButton(), createLocalizedCancelButton());
        
        // Apply consistent styling
        applyAlertStyling(alert);
        
        Optional<ButtonType> result = alert.showAndWait();
        boolean confirmed = result.isPresent() && 
                           result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE;
        
        logger.info("Confirmation result: {}", confirmed ? "OK" : "Cancel");
        
        return confirmed;
    }
    
    /**
     * Creates a localized OK button type.
     * 
     * @return a ButtonType with localized OK text and OK_DONE button data
     */
    private static ButtonType createLocalizedOkButton() {
        return new ButtonType(LocaleManager.getString("common.ok"), ButtonBar.ButtonData.OK_DONE);
    }
    
    /**
     * Creates a localized Cancel button type.
     * 
     * @return a ButtonType with localized Cancel text and CANCEL_CLOSE button data
     */
    private static ButtonType createLocalizedCancelButton() {
        return new ButtonType(LocaleManager.getString("common.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
    }
    
    /**
     * Creates a localized Yes button type.
     * 
     * @return a ButtonType with localized Yes text and YES button data
     */
    private static ButtonType createLocalizedYesButton() {
        return new ButtonType(LocaleManager.getString("common.yes"), ButtonBar.ButtonData.YES);
    }
    
    /**
     * Creates a localized No button type.
     * 
     * @return a ButtonType with localized No text and NO button data
     */
    private static ButtonType createLocalizedNoButton() {
        return new ButtonType(LocaleManager.getString("common.no"), ButtonBar.ButtonData.NO);
    }
    
    /**
     * Applies consistent Material Design 3 styling to an alert dialog.
     * This method adds CSS classes and configures the alert appearance.
     * Uses the current application theme from ThemeManager.
     * 
     * @param alert the alert to style
     */
    private static void applyAlertStyling(Alert alert) {
        // Add CSS class for Material Design 3 styling
        alert.getDialogPane().getStyleClass().add(ALERT_STYLE_CLASS);
        
        // Apply the current theme stylesheet
        String currentTheme = ThemeManager.getCurrentTheme();
        if (!"none".equals(currentTheme)) {
            try {
                String stylesheet = AlertHelper.class.getResource("/style/" + currentTheme + ".css").toExternalForm();
                alert.getDialogPane().getStylesheets().add(stylesheet);
                logger.debug("Applied theme '{}' to alert dialog", currentTheme);
            } catch (Exception e) {
                // Stylesheet not found - try default theme
                try {
                    String defaultStylesheet = AlertHelper.class.getResource("/style/main.css").toExternalForm();
                    alert.getDialogPane().getStylesheets().add(defaultStylesheet);
                    logger.debug("Applied default theme to alert dialog");
                } catch (Exception ex) {
                    // No stylesheet available - continue without styling
                    logger.debug("Could not load stylesheet for alert: {}", ex.getMessage());
                }
            }
        }
    }
}
