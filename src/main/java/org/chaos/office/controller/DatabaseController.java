package org.chaos.office.controller;

import io.quarkiverse.fx.views.FxView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@ApplicationScoped
@FxView
public class DatabaseController {

    @Inject
    HomeController homeController;

    Stage stage;

    @Produces
    public Stage stage() {
        return stage;
    }

    @FXML
    public void initialize() {
        // Create UI components
        Button button = new Button("Click Me");
        button.setOnAction(event -> System.out.println("Hello World!"));

        Button switchButton = new Button("Click Me");
        // homeController = new HomeController();
        switchButton.setOnAction(event -> homeController.initialize());

        // Arrange components in a layout
        VBox root = new VBox(button, switchButton);
        stage = new Stage();
        // Set up the scene and stage
        Scene scene = new Scene(root, 300, 200);
        stage.setScene(scene);
        stage.setTitle("ChaOffice Application");
        stage.show();
    }
}