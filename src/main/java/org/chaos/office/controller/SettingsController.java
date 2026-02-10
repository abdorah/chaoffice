package org.chaos.office.controller;

import javafx.scene.Parent;
import javafx.stage.Stage;
import org.chaos.office.util.AlertHelper;
import org.chaos.office.util.LocaleManager;
import org.chaos.office.util.ThemeManager;
import org.chaos.office.view.SettingsView;

import java.util.Locale;

/**
 * SettingsController - Manages application settings
 * Requirements: 12.2, 12.4, 12.5
 */
public class SettingsController {
    
    private final SettingsView view;
    private final Stage stage;
    
    public SettingsController() {
        this(null);
    }
    
    public SettingsController(Stage stage) {
        this.stage = stage;
        this.view = new SettingsView();
        
        loadSettings();
        setupEventHandlers();
    }
    
    private void loadSettings() {
        // Set current language
        Locale current = LocaleManager.getCurrentLocale();
        if (current.getLanguage().equals("fr")) {
            view.getLanguageComboBox().setValue("Français");
        } else if (current.getLanguage().equals("ar")) {
            view.getLanguageComboBox().setValue("العربية");
        } else {
            view.getLanguageComboBox().setValue("English");
        }
        
        // Set current theme
        String currentTheme = ThemeManager.getCurrentTheme();
        ThemeManager.Theme theme = ThemeManager.Theme.fromId(currentTheme);
        view.getThemeComboBox().setValue(theme.getDisplayName());
        
        // Set database path
        view.getDatabasePathField().setText(System.getProperty("user.home") + "/.chaoffice/chaoffice.db");
    }
    
    private void setupEventHandlers() {
        view.getSaveButton().setOnAction(e -> handleSave());
    }
    
    private void handleSave() {
        // Handle language change
        String selectedLanguage = view.getLanguageComboBox().getValue();
        Locale locale;
        
        switch (selectedLanguage) {
            case "Français":
                locale = Locale.FRENCH;
                break;
            case "العربية":
                locale = new Locale("ar");
                break;
            default:
                locale = new Locale("en", "US");
        }
        
        LocaleManager.setLocale(locale);
        
        // Handle theme change
        String selectedTheme = view.getThemeComboBox().getValue();
        String themeId;
        
        switch (selectedTheme) {
            case "Dark Theme":
                themeId = "dark";
                break;
            case "Ubuntu Theme":
                themeId = "ubuntu";
                break;
            case "No Theme":
                themeId = "none";
                break;
            default:
                themeId = "main";
        }
        
        // Apply theme to current stage if available
        if (stage != null && stage.getScene() != null) {
            ThemeManager.setTheme(stage.getScene(), themeId);
        }
        
        // Refresh the entire dashboard if we have access to stage
        if (stage != null) {
            DashboardController newDashboard = new DashboardController(stage);
            stage.setScene(newDashboard);
            
            // Reapply theme to the new scene
            ThemeManager.setTheme(stage.getScene(), themeId);
        }
        
        AlertHelper.showInfo(
            LocaleManager.getString("settings.success.title"),
            LocaleManager.getString("settings.success.message")
        );
    }
    
    public Parent getView() {
        return view;
    }
}
