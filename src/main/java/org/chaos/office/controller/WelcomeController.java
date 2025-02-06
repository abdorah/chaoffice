package org.chaos.office.controller;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

@ApplicationScoped
public class WelcomeController extends BaseController {

  @FXML
  public void initialize() {
    // Update the state of the UI stack
    BaseController.getViewStack().push(this);

    // Create UI components
    Button nextPageButton = new Button("Start");
    Label greetingLabel = new Label("Welcome to Chaos Office...");
    Label introductionLabel = new Label("Your success companion!");
    Label instructionLabel = new Label("Start your journey here, by clicking on the start button:");
    Region spacer = new Region();

    // Add CSS classes
    nextPageButton.getStyleClass().add("next-button");
    greetingLabel.getStyleClass().add("greeting-label");
    introductionLabel.getStyleClass().add("introduction-label");
    instructionLabel.getStyleClass().add("instruction-label");
    spacer.getStyleClass().add("spacer");

    // Set behavior of the UI elements
    nextPageButton.setOnAction(
        event -> {
          System.out.println("Welcome Welcome!");
        });

    // Arrange components in a layout
    VBox root =
        new VBox(greetingLabel, introductionLabel, instructionLabel, spacer, nextPageButton);
    root.getStyleClass().add("welcome-layout");

    // Reset up the scene and the root node
    super.getViewManager().getStage().getScene().setRoot(root);

    // Apply the CSS file
    String cssPath =
        Objects.requireNonNull(getClass().getResource("/style/welcome.css")).toExternalForm();
    super.getViewManager().getStage().getScene().getStylesheets().add(cssPath);
  }
}
