package org.chaos.office.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@ApplicationScoped
public class HomeController {

    @Inject
    DatabaseController databaseController;

    @FXML
    public void initialize() {
        // Create UI components
        Button button = new Button("Click Me");
        button.setOnAction(event -> System.out.println("Welcome Home!"));

        // Arrange components in a layout
        VBox root = new VBox(button);

        Stage primaryStage = databaseController.stage();
        // Set up the scene and stage
        Scene scene = new Scene(root, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.setTitle("ChaOffice Application");
        primaryStage.show();
    }
}