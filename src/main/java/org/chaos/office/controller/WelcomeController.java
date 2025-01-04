package org.chaos.office.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.chaos.office.utils.View;

@ApplicationScoped
public class WelcomeController {

  @Inject View view;

  @FXML
  public void initialize() {
    // Create UI components
    Button button = new Button("Click Welcome");
    button.setOnAction(event -> System.out.println("Welcome Welcome!"));

    // Arrange components in a layout
    VBox root = new VBox(button);

    // Reset up the scene and the root node
    view.getStage().getScene().setRoot(root);
  }
}
