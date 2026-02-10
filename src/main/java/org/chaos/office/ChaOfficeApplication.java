package org.chaos.office;

import javafx.application.Application;
import javafx.stage.Stage;
import org.chaos.office.controller.SignInController;
import org.chaos.office.service.DatabaseService;
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
            
            // Load saved language preference
            LocaleManager.setLocale(new Locale("en", "US"));
            
            // Set up primary stage
            primaryStage.setTitle(LocaleManager.getString("app.title"));
            primaryStage.setWidth(1200);
            primaryStage.setHeight(800);
            
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
