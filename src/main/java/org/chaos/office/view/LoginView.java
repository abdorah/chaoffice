package org.chaos.office.view;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.chaos.office.util.LocaleManager;

import java.util.Locale;

/**
 * LoginView - Pure Java UI for login screen
 * Requirements: 1.2, 1.3, 3.1, 9.1
 */
public class LoginView extends VBox {
    
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final Button loginButton;
    private final ComboBox<String> languageComboBox;
    
    public LoginView() {
        // Configure layout
        setAlignment(Pos.CENTER);
        setSpacing(16);
        getStyleClass().add("padding-xl");
        
        // Title
        Label titleLabel = new Label(LocaleManager.getString("app.title"));
        titleLabel.getStyleClass().add("label-title");
        
        // Username field
        Label usernameLabel = new Label(LocaleManager.getString("login.username"));
        usernameField = new TextField();
        usernameField.setPromptText(LocaleManager.getString("login.username"));
        usernameField.setPrefWidth(300);
        
        // Password field
        Label passwordLabel = new Label(LocaleManager.getString("login.password"));
        passwordField = new PasswordField();
        passwordField.setPromptText(LocaleManager.getString("login.password"));
        passwordField.setPrefWidth(300);
        
        // Language selector
        Label languageLabel = new Label(LocaleManager.getString("login.language"));
        languageComboBox = new ComboBox<>();
        languageComboBox.getItems().addAll("English", "Français", "العربية");
        
        // Set current language based on locale
        Locale currentLocale = LocaleManager.getCurrentLocale();
        if (currentLocale.getLanguage().equals("fr")) {
            languageComboBox.setValue("Français");
        } else if (currentLocale.getLanguage().equals("ar")) {
            languageComboBox.setValue("العربية");
        } else {
            languageComboBox.setValue("English");
        }
        
        languageComboBox.setPrefWidth(300);
        
        // Login button
        loginButton = new Button(LocaleManager.getString("login.button"));
        loginButton.setPrefWidth(300);
        loginButton.setDefaultButton(true);
        
        // Add all components
        getChildren().addAll(
            titleLabel,
            usernameLabel, usernameField,
            passwordLabel, passwordField,
            languageLabel, languageComboBox,
            loginButton
        );
    }
    
    public TextField getUsernameField() {
        return usernameField;
    }
    
    public PasswordField getPasswordField() {
        return passwordField;
    }
    
    public Button getLoginButton() {
        return loginButton;
    }
    
    public ComboBox<String> getLanguageComboBox() {
        return languageComboBox;
    }
}
