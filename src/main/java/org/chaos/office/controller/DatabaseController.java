package org.chaos.office.controller;

import jakarta.enterprise.context.ApplicationScoped;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

@ApplicationScoped
public class DatabaseController extends BaseController {

  @FXML
  public void initialize() {
    // Update the state of the UI stack
    BaseController.getViewStack().push(this);

    // Create UI components
    Button button = new Button("Click Database");
    button.setOnAction(
        event -> {
          System.out.println("Welcome Database!");
          super.getView().getStage().toBack();
        });

    // Arrange components in a layout
    VBox root = new VBox(button);

    // Reset up the scene and the root node
    super.getView().getStage().getScene().setRoot(root);
  }
}
