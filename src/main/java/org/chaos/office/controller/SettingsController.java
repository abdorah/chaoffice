package org.chaos.office.controller;

import javafx.scene.Parent;
import org.chaos.office.util.AlertHelper;
import org.chaos.office.util.DatabaseConnection;
import org.chaos.office.util.LocaleManager;
import org.chaos.office.view.SettingsView;

import java.util.Locale;

/**
 * SettingsController - Manages application settings
 * Requirements: 12.2, 12.4, 12.5
 */
public class SettingsController {
    
    private final SettingsView view;
    
    public SettingsController() {
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
        
        // Set database path
        view.getDatabasePathField().setText(System.getProperty("user.home") + "/.chaoffice/chaoffice.db");
    }
    
    private void setupEventHandlers() {
        view.getSaveButton().setOnAction(e -> handleSave());
    }
    
    private void handleSave() {
        String selected = view.getLanguageComboBox().getValue();
        Locale locale;
        
        switch (selected) {
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
        
        AlertHelper.showInfo(
            LocaleManager.getString("settings.success.title"),
            LocaleManager.getString("settings.success.message")
        );
    }
    
    public Parent getView() {
        return view;
    }
}
