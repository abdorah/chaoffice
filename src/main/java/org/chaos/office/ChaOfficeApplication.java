package org.chaos.office;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.chaos.office.controller.SignInController;
import org.chaos.office.service.BrandingService;
import org.chaos.office.service.DatabaseService;
import org.chaos.office.service.SettingsService;
import org.chaos.office.util.CurrencyFormatter;
import org.chaos.office.util.DatabaseConnection;
import org.chaos.office.util.LocaleManager;
import org.chaos.office.util.ThemeManager;

import java.util.Locale;

/**
 * ChaOfficeApplication - Main application entry point
 * Requirements: 9.5, 10.3, 10.4
 */
public class ChaOfficeApplication extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize database
            DatabaseService databaseService = new DatabaseService();
            databaseService.initializeDatabase();
            
            // Initialize settings service and currency formatter
            SettingsService settingsService = new SettingsService();
            CurrencyFormatter.initialize(settingsService);
            
            // Initialize branding service
            BrandingService brandingService = new BrandingService();
            
            // Load saved language preference (LocaleManager will load from preferences)
            // Don't set a specific locale - let LocaleManager load the saved preference
            LocaleManager.getCurrentLocale(); // This triggers loading saved locale
            
            // Set up primary stage with branding
            primaryStage.setTitle(brandingService.getApplicationTitle());
            
            // Set application logo if available
            Image logo = brandingService.getApplicationLogo();
            if (logo != null) {
                primaryStage.getIcons().add(logo);
            }
            
            primaryStage.setWidth(1200);
            primaryStage.setHeight(800);
            
            // Set minimum window size to ensure usability
            primaryStage.setMinWidth(1024);
            primaryStage.setMinHeight(768);
            
            // Create and show login scene
            SignInController loginController = new SignInController(primaryStage);
            primaryStage.setScene(loginController);
            
            // Apply saved theme
            ThemeManager.applyCurrentTheme(primaryStage.getScene());
            
            // Handle window close
            primaryStage.setOnCloseRequest(e -> {
                DatabaseConnection.getInstance().closeConnection();
            });
            
            primaryStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start application: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
