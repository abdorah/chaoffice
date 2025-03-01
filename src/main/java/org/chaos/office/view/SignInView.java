package org.chaos.office.view;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.chaos.office.controller.DashboardController;
import org.chaos.office.service.ComponentService;

public class SignInView extends BorderPane {

  public Label signInLabel() {
    Label label = new Label("Sign In");
    label.getStyleClass().add("sign-in-label");
    label.setFont(new Font("Arial", 24));
    label.setTextFill(Color.BLACK);
    return label;
  }

  public TextField idField() {
    TextField idField = new TextField();
    idField.getStyleClass().add("sign-in-id-field");
    idField.setFont(new Font("Arial", 24));
    idField.setPromptText("Username");
    return idField;
  }

  public PasswordField passwordField() {
    PasswordField passwordField = new PasswordField();
    passwordField.getStyleClass().add("sign-in-password-field");
    passwordField.setFont(new Font("Arial", 24));
    passwordField.setPromptText("password");
    return passwordField;
  }

  public Button signInButton() {
    Button button = new Button("Sign in");
    button.getStyleClass().add("sing-in-button");
    button.setOnAction(
        e -> {
          System.out.println("Sign In Clicked");
          ComponentService.switchScene(new DashboardController());
        });
    return button;
  }

  public SignInView() {
    VBox vbox = new VBox();
    vbox.setSpacing(10);
    vbox.getChildren().add(signInLabel());
    vbox.getChildren().add(idField());
    vbox.getChildren().add(passwordField());
    vbox.getChildren().add(signInButton());
    this.setCenter(vbox);
  }
}
