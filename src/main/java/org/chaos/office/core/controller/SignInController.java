package org.chaos.office.core.controller;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import org.chaos.office.core.navigation.ViewManager;

public class SignInController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Text messageText;

    @FXML
    protected void handleSignIn() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageText.setText("Username and password cannot be empty");
        } else {
            // Here you would typically validate the credentials
            System.out.println("Sign in attempt: Username = " + username + ", Password = " + password);
            messageText.setText("Sign in successful!");
            ViewManager.getInstance().loadView("MainScreen"); 
        }
    }
}
