package org.chaos.office.controller;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.chaos.office.service.AuthenticationService;
import org.chaos.office.util.AlertHelper;
import org.chaos.office.util.LocaleManager;
import org.chaos.office.util.SessionManager;
import org.chaos.office.view.LoginView;

import java.util.Locale;

/**
 * SignInController - Manages login screen
 * Requirements: 3.2, 3.3, 9.2
 */
public class SignInController extends Scene {
    
    private final LoginView loginView;
    private final AuthenticationService authService;
    private final Stage stage;
    
    public SignInController(Stage stage) {
        super(new LoginView(), 800, 600);
        this.stage = stage;
        this.loginView = (LoginView) getRoot();
        this.authService = new AuthenticationService();
        
        // Apply stylesheet
        getStylesheets().add(getClass().getResource("/style/main.css").toExternalForm());
        
        setupEventHandlers();
    }
    
    private void setupEventHandlers() {
        // Login button handler
        loginView.getLoginButton().setOnAction(e -> handleLogin());
        
        // Enter key on password field
        loginView.getPasswordField().setOnAction(e -> handleLogin());
        
        // Language selection handler
        loginView.getLanguageComboBox().setOnAction(e -> handleLanguageChange());
    }
    
    private void handleLogin() {
        String username = loginView.getUsernameField().getText();
        String password = loginView.getPasswordField().getText();
        
        var userOptional = authService.authenticate(username, password);
        
        if (userOptional.isPresent()) {
            // Set session
            SessionManager.getInstance().setCurrentUser(userOptional.get());
            
            // Navigate to dashboard
            DashboardController dashboard = new DashboardController(stage);
            stage.setScene(dashboard);
        } else {
            // Show error
            AlertHelper.showError(
                LocaleManager.getString("login.error.title"),
                LocaleManager.getString("login.error.message")
            );
        }
    }
    
    private void handleLanguageChange() {
        String selected = loginView.getLanguageComboBox().getValue();
        if (selected == null) {
            return;
        }
        
        Locale locale;
        
        // Match language selection to locale
        if (selected.equals("Français") || selected.startsWith("Fr")) {
            locale = Locale.FRENCH;
        } else if (selected.equals("العربية") || selected.contains("العربية")) {
            locale = new Locale("ar");
        } else {
            // Default to English for "English" or any other value
            locale = new Locale("en", "US");
        }
        
        LocaleManager.setLocale(locale);
        
        // Refresh view
        SignInController newController = new SignInController(stage);
        stage.setScene(newController);
    }
}
