package org.chaos.office.util;

import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

/**
 * DialogHelper - Utility class for dialog operations
 * 
 * <p>Provides functionality to:
 * <ul>
 *   <li>Apply current theme to dialogs</li>
 *   <li>Ensure consistent dialog styling across the application</li>
 * </ul>
 */
public class DialogHelper {
    
    /**
     * Applies the current theme to a dialog.
     * This ensures dialogs match the application's selected theme.
     * 
     * @param dialog the dialog to apply the theme to
     */
    public static void applyTheme(Dialog<?> dialog) {
        if (dialog == null) {
            return;
        }
        
        DialogPane dialogPane = dialog.getDialogPane();
        if (dialogPane == null) {
            return;
        }
        
        // Clear any existing stylesheets
        dialogPane.getStylesheets().clear();
        
        // Get current theme
        String currentTheme = ThemeManager.getCurrentTheme();
        
        // Apply theme if not "none"
        if (!"none".equals(currentTheme)) {
            String cssPath = getThemePath(currentTheme);
            if (cssPath != null) {
                dialogPane.getStylesheets().add(cssPath);
            }
        }
    }
    
    /**
     * Gets the CSS file path for a theme.
     * 
     * @param themeId the theme ID
     * @return the CSS file path or null if not found
     */
    private static String getThemePath(String themeId) {
        try {
            String path = "/style/" + themeId + ".css";
            if (DialogHelper.class.getResource(path) != null) {
                return DialogHelper.class.getResource(path).toExternalForm();
            }
        } catch (Exception e) {
            // Theme not found, return null
        }
        return null;
    }
}
