package org.chaos.office.controller;

import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.chaos.office.model.BrandingSettings;
import org.chaos.office.model.CurrencySettings;
import org.chaos.office.service.AuthenticationService;
import org.chaos.office.service.BrandingService;
import org.chaos.office.service.SettingsService;
import org.chaos.office.util.AlertHelper;
import org.chaos.office.util.LocaleManager;
import org.chaos.office.util.ThemeManager;
import org.chaos.office.view.SettingsView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

/**
 * SettingsController - Manages application settings
 * Requirements: 12.2, 12.4, 12.5, 2.9, 5.7, 5.8, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10
 */
public class SettingsController {
    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);
    
    private final SettingsView view;
    private final Stage stage;
    private final BrandingService brandingService;
    private final SettingsService settingsService;
    private final AuthenticationService authenticationService;
    
    // Store selected logo data temporarily until save
    private byte[] selectedLogoData;
    private String selectedLogoFormat;
    
    public SettingsController() {
        this(null);
    }
    
    public SettingsController(Stage stage) {
        this.stage = stage;
        this.view = new SettingsView();
        this.brandingService = new BrandingService();
        this.settingsService = new SettingsService();
        this.authenticationService = new AuthenticationService();
        
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
        
        // Load branding settings
        loadBrandingSettings();
        
        // Load currency settings
        loadCurrencySettings();
    }
    
    /**
     * Loads current branding settings from the BrandingService and populates the view.
     * Requirements: 2.9
     */
    private void loadBrandingSettings() {
        try {
            BrandingSettings settings = brandingService.getBrandingSettings();
            
            // Load store name
            if (settings.hasStoreName()) {
                view.getStoreNameField().setText(settings.getStoreName());
            }
            
            // Load logo preview
            if (settings.hasLogo()) {
                Image logoImage = new Image(new ByteArrayInputStream(settings.getLogoImage()));
                view.getLogoPreview().setImage(logoImage);
                
                // Store current logo data
                selectedLogoData = settings.getLogoImage();
                selectedLogoFormat = settings.getLogoFormat();
            }
            
            logger.info("Loaded branding settings into view");
        } catch (Exception e) {
            logger.error("Error loading branding settings", e);
            AlertHelper.showError(
                LocaleManager.getString("settings.error.title"),
                "Failed to load branding settings: " + e.getMessage()
            );
        }
    }
    
    /**
     * Loads current currency settings from the SettingsService and populates the view.
     * Requirements: 5.7
     */
    private void loadCurrencySettings() {
        try {
            CurrencySettings settings = settingsService.getCurrencySettings();
            
            // Load currency symbol
            String symbol = settings.getSymbol();
            
            // Check if it's a standard symbol or custom
            if (symbol.equals("$") || symbol.equals("€") || symbol.equals("£") || 
                symbol.equals("¥") || symbol.equals("₹")) {
                view.getCurrencySymbolComboBox().setValue(symbol);
                view.getCustomSymbolField().setDisable(true);
            } else {
                // Custom symbol
                view.getCurrencySymbolComboBox().setValue("Custom");
                view.getCustomSymbolField().setText(symbol);
                view.getCustomSymbolField().setDisable(false);
            }
            
            // Load currency acronym
            view.getCurrencyAcronymField().setText(settings.getAcronym());
            
            logger.info("Loaded currency settings into view: {} {}", symbol, settings.getAcronym());
        } catch (Exception e) {
            logger.error("Error loading currency settings", e);
            AlertHelper.showError(
                LocaleManager.getString("settings.error.title"),
                "Failed to load currency settings: " + e.getMessage()
            );
        }
    }
    
    private void setupEventHandlers() {
        view.getSaveButton().setOnAction(e -> handleSave());
        view.getSelectLogoButton().setOnAction(e -> handleLogoSelection());
        view.getChangeUsernameButton().setOnAction(e -> handleChangeUsername());
        view.getChangePasswordButton().setOnAction(e -> handleChangePassword());
    }
    
    /**
     * Handles logo file selection using FileChooser.
     * Loads the selected image file and displays it in the preview.
     * Requirements: 2.2, 2.9
     */
    private void handleLogoSelection() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(LocaleManager.getString("settings.branding.selectLogo"));
        
        // Set file filters for image files
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
            new FileChooser.ExtensionFilter("PNG Files", "*.png"),
            new FileChooser.ExtensionFilter("JPEG Files", "*.jpg", "*.jpeg"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        File selectedFile = fileChooser.showOpenDialog(stage);
        
        if (selectedFile != null) {
            try {
                // Read the file into byte array
                selectedLogoData = Files.readAllBytes(selectedFile.toPath());
                
                // Determine format from file extension
                String fileName = selectedFile.getName().toLowerCase();
                if (fileName.endsWith(".png")) {
                    selectedLogoFormat = "PNG";
                } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    selectedLogoFormat = "JPG";
                } else if (fileName.endsWith(".gif")) {
                    selectedLogoFormat = "GIF";
                } else if (fileName.endsWith(".bmp")) {
                    selectedLogoFormat = "BMP";
                } else {
                    selectedLogoFormat = "UNKNOWN";
                }
                
                // Display preview
                Image logoImage = new Image(new ByteArrayInputStream(selectedLogoData));
                view.getLogoPreview().setImage(logoImage);
                
                logger.info("Logo file selected: {} ({})", selectedFile.getName(), selectedLogoFormat);
                
            } catch (IOException e) {
                logger.error("Error reading logo file", e);
                AlertHelper.showError(
                    LocaleManager.getString("settings.error.title"),
                    "Failed to load logo image: " + e.getMessage()
                );
            }
        }
    }
    
    private void handleSave() {
        try {
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
            
            // Save branding settings
            saveBrandingSettings();
            
            // Save currency settings
            saveCurrencySettings();
            
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
            
        } catch (Exception e) {
            logger.error("Error saving settings", e);
            AlertHelper.showError(
                LocaleManager.getString("settings.error.title"),
                "Failed to save settings: " + e.getMessage()
            );
        }
    }
    
    /**
     * Saves branding settings to the database via BrandingService.
     * Requirements: 2.9
     */
    private void saveBrandingSettings() {
        try {
            // Get store name from field (can be empty)
            String storeName = view.getStoreNameField().getText();
            if (storeName != null && storeName.trim().isEmpty()) {
                storeName = null; // Store null for empty strings
            }
            
            // Create branding settings object
            BrandingSettings settings = new BrandingSettings(
                storeName,
                selectedLogoData,
                selectedLogoFormat
            );
            
            // Save to database
            brandingService.saveBrandingSettings(settings);
            
            logger.info("Branding settings saved successfully");
            
        } catch (Exception e) {
            logger.error("Error saving branding settings", e);
            throw new RuntimeException("Failed to save branding settings", e);
        }
    }
    
    /**
     * Saves currency settings to the database via SettingsService.
     * Validates input and triggers currency update for immediate UI refresh.
     * Requirements: 5.7, 5.8
     */
    private void saveCurrencySettings() {
        try {
            // Get currency symbol
            String symbol;
            String selectedSymbol = view.getCurrencySymbolComboBox().getValue();
            
            if ("Custom".equals(selectedSymbol)) {
                // Use custom symbol from text field
                symbol = view.getCustomSymbolField().getText();
                
                // Validate custom symbol is not empty
                if (symbol == null || symbol.trim().isEmpty()) {
                    AlertHelper.showError(
                        LocaleManager.getString("settings.error.title"),
                        "Custom currency symbol cannot be empty"
                    );
                    throw new IllegalArgumentException("Custom currency symbol cannot be empty");
                }
            } else {
                // Use selected standard symbol
                symbol = selectedSymbol;
            }
            
            // Get currency acronym
            String acronym = view.getCurrencyAcronymField().getText();
            if (acronym == null || acronym.trim().isEmpty()) {
                acronym = "USD"; // Default to USD if empty
            }
            
            // Create currency settings object
            CurrencySettings settings = new CurrencySettings(symbol, acronym);
            
            // Save to database
            settingsService.saveCurrencySettings(settings);
            
            logger.info("Currency settings saved successfully: {} {}", symbol, acronym);
            
            // Note: The dashboard refresh in handleSave() will trigger immediate UI update
            // by recreating all views with the new currency settings via CurrencyFormatter
            
        } catch (Exception e) {
            logger.error("Error saving currency settings", e);
            throw new RuntimeException("Failed to save currency settings", e);
        }
    }
    
    /**
     * Handles the "Change Username" button click.
     * Validates input and calls AuthenticationService to change the username.
     * Requirements: 3.3, 3.7, 3.8, 3.9
     */
    private void handleChangeUsername() {
        logger.info("Handling username change request");
        
        // Get input values
        String newUsername = view.getNewUsernameField().getText();
        String currentPassword = view.getCurrentPasswordField().getText();
        
        // Validate new username is not empty
        if (newUsername == null || newUsername.trim().isEmpty()) {
            logger.warn("Username change failed: new username is empty");
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                "Username cannot be empty"
            );
            return;
        }
        
        // Validate current password is provided
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            logger.warn("Username change failed: current password not provided");
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                "Current password is required to change username"
            );
            return;
        }
        
        // Attempt to change username
        boolean success = authenticationService.changeUsername(currentPassword, newUsername);
        
        if (success) {
            logger.info("Username changed successfully");
            AlertHelper.showInfo(
                LocaleManager.getString("success.title"),
                "Username changed successfully"
            );
            
            // Clear the fields
            view.getNewUsernameField().clear();
            view.getCurrentPasswordField().clear();
        } else {
            logger.warn("Username change failed");
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                "Failed to change username. Please check your current password."
            );
        }
    }
    
    /**
     * Handles the "Change Password" button click.
     * Validates input and calls AuthenticationService to change the password.
     * Requirements: 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.10
     */
    private void handleChangePassword() {
        logger.info("Handling password change request");
        
        // Get input values
        String currentPassword = view.getCurrentPasswordField().getText();
        String newPassword = view.getNewPasswordField().getText();
        String confirmPassword = view.getConfirmPasswordField().getText();
        
        // Validate current password is provided
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            logger.warn("Password change failed: current password not provided");
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                "Current password is required"
            );
            return;
        }
        
        // Validate new password is not empty
        if (newPassword == null || newPassword.isEmpty()) {
            logger.warn("Password change failed: new password is empty");
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                "New password cannot be empty"
            );
            return;
        }
        
        // Validate new password length (minimum 6 characters)
        if (newPassword.length() < 6) {
            logger.warn("Password change failed: new password is too short");
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                "New password must be at least 6 characters long"
            );
            return;
        }
        
        // Validate password confirmation matches
        if (!newPassword.equals(confirmPassword)) {
            logger.warn("Password change failed: password confirmation does not match");
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                "Password confirmation does not match"
            );
            return;
        }
        
        // Attempt to change password
        boolean success = authenticationService.changePassword(currentPassword, newPassword, confirmPassword);
        
        if (success) {
            logger.info("Password changed successfully");
            AlertHelper.showInfo(
                LocaleManager.getString("success.title"),
                "Password changed successfully"
            );
            
            // Clear all password fields
            view.getCurrentPasswordField().clear();
            view.getNewPasswordField().clear();
            view.getConfirmPasswordField().clear();
        } else {
            logger.warn("Password change failed");
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                "Failed to change password. Please check your current password."
            );
        }
    }
    
    public Parent getView() {
        return view;
    }
}
