package org.chaos.office.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

@ApplicationScoped
public class WelcomeController extends BaseController {

  @Inject HomeController homeController;

  @FXML
  public void initialize() {
    // Update the state of the UI stack
    BaseController.getViewStack().push(this);

    // Create UI components
    Button nextPageButton = new Button("Start");
    Label greetingLabel = new Label("Welcome to Chaos Office...");
    Label introductionLabel = new Label("Your success companion!");
    Label instructionLabel = new Label("Start your journey here, by clicking on the start button:");
    Region region = new Region();

    // Set the style of the components
    // TODO: implement this after finishing the application logic.

    // Set behavior of the UI elements
    nextPageButton.setOnAction(
        event -> {
          System.out.println("Welcome Welcome!");
          homeController.initialize();
        });

    // Arrange components in a layout
    VBox root =
        new VBox(greetingLabel, introductionLabel, instructionLabel, region, nextPageButton);

    // Reset up the scene and the root node
    super.getView().getStage().getScene().setRoot(root);
  }
}
