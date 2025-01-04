package org.chaos.office.controller;

import jakarta.enterprise.context.ApplicationScoped;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

@ApplicationScoped
public class HomeController extends BaseController {

  @FXML
  public void initialize() {
    // Update the state of the UI stack
    BaseController.getViewStack().push(this);

    // Create UI components
    Button button = new Button("Click Home");
    button.setOnAction(
        event -> {
          System.out.println("Welcome Home!");
          super.getView().getStage().toBack();
        });
    Button goBack = new Button("Go Back");
    goBack.setOnAction(
        event -> {
          System.out.println("Back from Home!");
          BaseController.getViewStack().pop();
          BaseController.getViewStack().peek().initialize();
        });

    // Arrange components in a layout
    VBox root = new VBox(button, goBack);

    // Reset up the scene and the root node
    super.getView().getStage().getScene().setRoot(root);
  }
}
