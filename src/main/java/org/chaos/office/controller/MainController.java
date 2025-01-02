package org.chaos.office.controller;

import io.quarkiverse.fx.RunOnFxThread;
import io.quarkiverse.fx.views.FxView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@FxView
@RunOnFxThread
@ApplicationScoped
public class MainController {

    Stage stage;

    @Inject
    HomeController homeController;

    @Inject
    DatabaseController databaseController;

    @Produces
    public Stage stage() {
        return stage;
    }

    @FXML
    public void initialize() {
        // Create UI components
        Button homeButton = new Button("Home");
        homeButton.setOnAction(event -> {
            System.out.println("Home!");
            homeController.initialize();
        });

        Button databaseButton = new Button("Database");
        databaseButton.setOnAction(event -> {
            System.out.println("Database!");
            databaseController.initialize();
        });


        // Arrange components in a layout
        VBox root = new VBox(homeButton, databaseButton);
        stage = new Stage();
        // Set up the scene and stage
        Scene scene = new Scene(root, 300, 200);
        stage.setScene(scene);
        stage.setTitle("ChaOffice Application");
        stage.show();
    }
}