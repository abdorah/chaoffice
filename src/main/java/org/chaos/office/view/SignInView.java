package org.chaos.office.view;

import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class SignInView extends BorderPane {

  public Label signInLabel() {
    Label label = new Label("Sign In");
    label.getStyleClass().add("sign-in-label");
    label.setFont(new Font("Arial", 24));
    label.setTextFill(Color.WHITE);
    return label;
  }

  public TextField idField() {
    TextField idField = new TextField();
    idField.getStyleClass().add("sign-in-id-field");
    idField.setFont(new Font("Arial", 24));
    idField.setText("username");
    return idField;
  }

  public PasswordField passwordField() {
    PasswordField passwordField = new PasswordField();
    passwordField.getStyleClass().add("sign-in-password-field");
    passwordField.setFont(new Font("Arial", 24));
    passwordField.setText("password");
    return passwordField;
  }

  public SignInView() {
    VBox vbox = new VBox();
    vbox.getChildren().add(signInLabel());
    vbox.getChildren().add(idField());
    vbox.getChildren().add(passwordField());
    this.setCenter(vbox);
  }
}
