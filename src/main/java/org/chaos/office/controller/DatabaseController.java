package org.chaos.office.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.sql.SQLException;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.chaos.office.service.DatabaseService;
import org.chaos.office.utils.Component;

@ApplicationScoped
public class DatabaseController extends BaseController {

  @Inject DatabaseService databaseService;
  @Inject Component component;

  @FXML
  public void initialize() {
    // Update the state of the UI stack
    BaseController.getViewStack().push(this);

    // Create UI components
    HBox nameGroup = component.buildTextInput("name");
    HBox usernameGroup = component.buildTextInput("username");
    HBox passwordGroup = component.buildPassword("password");
    HBox backUpOriginGroup = component.buildTextInput("base to backup");
    HBox backUpNameGroup = component.buildTextInput("back up database name");

    Button createDatabaseButton = new Button("Create Database");
    createDatabaseButton.setOnAction(
        event -> {
          System.out.println("Create Database");
          try {
            databaseService.initDataSource(
                component.extractTextField(nameGroup).getText(),
                component.extractTextField(usernameGroup).getText(),
                component.extractTextField(passwordGroup).getText());
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
        });

    Button saveButton = new Button("Create backup database");
    saveButton.setOnAction(
        event -> {
          System.out.println("Create Backup Database");
          try {
            databaseService.saveCheckPoint(
                component.extractTextField(backUpOriginGroup).getText(),
                component.extractTextField(backUpNameGroup).getText());
          } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
          }
        });

    // Arrange components in a layout
    VBox createDatabaseGroup =
        new VBox(nameGroup, usernameGroup, passwordGroup, createDatabaseButton);
    createDatabaseGroup.setPadding(new Insets(20, 20, 20, 20));
    createDatabaseGroup.setSpacing(15);
    VBox backUpDatabaseGroup = new VBox(backUpOriginGroup, backUpNameGroup, saveButton);
    backUpDatabaseGroup.setPadding(new Insets(20, 20, 20, 20));
    backUpDatabaseGroup.setSpacing(15);
    VBox root = new VBox(15); // Add spacing of 15px between elements
    root.setPadding(new Insets(20, 40, 20, 40)); // Add padding around the layout
    root.setPrefWidth(400); // Set a preferred width for better centering
    root.setFillWidth(true); // Ensure the button stretches to fit width
    root.getChildren().add(createDatabaseGroup);
    root.getChildren().add(backUpDatabaseGroup);
    // Reset up the scene and the root node
    super.getView().getStage().getScene().setRoot(root);
  }
}
