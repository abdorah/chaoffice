package org.chaos.office.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@ApplicationScoped
public class DatabaseController {

    @Inject
    MainController  mainController;

    @FXML
    public void initialize() {
        Button button = new Button("Click Database");
        button.setOnAction(event -> System.out.println("Welcome Database!"));

        // Arrange components in a layout
        VBox root = new VBox(button);

        Stage primaryStage = mainController.stage();
        // Set up the scene and stage
        Scene scene = new Scene(root, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.setTitle("ChaOffice Application");
        primaryStage.show();
    }
}