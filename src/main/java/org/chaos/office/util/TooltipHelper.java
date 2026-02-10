package org.chaos.office.util;

import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

/**
 * TooltipHelper utility class for managing error tooltips on form controls.
 * Provides methods to show and hide validation error tooltips with consistent styling.
 * 
 * <p>This class is responsible for:
 * <ul>
 *   <li>Displaying error tooltips on invalid input fields</li>
 *   <li>Removing error tooltips when validation passes</li>
 *   <li>Applying consistent error styling (red borders)</li>
 *   <li>Managing tooltip timing and behavior</li>
 * </ul>
 * 
 * <p>Requirements: 1.4, 2.2, 2.4
 */
public class TooltipHelper {
    
    private static final String ERROR_STYLE = "-fx-border-color: #F44336; -fx-border-width: 2px; -fx-border-radius: 3px;";
    private static final String NORMAL_STYLE = "";
    
    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with static methods only.
     */
    private TooltipHelper() {
        // Utility class - no instantiation
    }
    
    /**
     * Shows an error tooltip on a control and applies error styling.
     * 
     * @param control the control to show the error on
     * @param errorMessage the error message to display
     */
    public static void showError(Control control, String errorMessage) {
        if (control == null || errorMessage == null || errorMessage.isEmpty()) {
            return;
        }
        
        // Create and configure tooltip
        Tooltip tooltip = new Tooltip(errorMessage);
        tooltip.setShowDelay(Duration.millis(100));
        tooltip.setHideDelay(Duration.millis(5000));
        tooltip.setAutoHide(true);
        tooltip.setStyle("-fx-background-color: #F44336; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 12px; " +
                        "-fx-padding: 8px;");
        
        // Set tooltip on control
        control.setTooltip(tooltip);
        
        // Apply error styling
        control.setStyle(ERROR_STYLE);
        
        // Show tooltip immediately
        if (control.getScene() != null && control.getScene().getWindow() != null) {
            tooltip.show(control, 
                        control.localToScreen(0, 0).getX(),
                        control.localToScreen(0, 0).getY() + control.getHeight() + 5);
        }
    }
    
    /**
     * Clears the error tooltip and styling from a control.
     * 
     * @param control the control to clear the error from
     */
    public static void clearError(Control control) {
        if (control == null) {
            return;
        }
        
        // Remove tooltip
        Tooltip tooltip = control.getTooltip();
        if (tooltip != null) {
            tooltip.hide();
            control.setTooltip(null);
        }
        
        // Remove error styling
        control.setStyle(NORMAL_STYLE);
    }
    
    /**
     * Checks if a control currently has an error tooltip.
     * 
     * @param control the control to check
     * @return true if the control has an error tooltip, false otherwise
     */
    public static boolean hasError(Control control) {
        return control != null && control.getTooltip() != null;
    }
}
