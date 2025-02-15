package org.chaos.office.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.chaos.office.domain.Table;
import org.chaos.office.utils.BaseController;
import org.chaos.office.utils.Layout;

@ApplicationScoped
public class DatabaseManagerController extends BaseController {

  private final Table tableService;
  private final ObservableList<String> fieldTypes =
      FXCollections.observableArrayList("varchar", "int", "long", "double", "float", "boolean");
  private final VBox fieldsContainer;

  @Inject
  public DatabaseManagerController() {
    this.tableService = Table.table;
    this.fieldsContainer = new VBox(10);
  }

  @FXML
  public void initialize() {
    Layout layout = new Layout();
    VBox content = createDatabaseManagerContent();
    layout.setContent(content);
    // Apply the CSS file
    this.getViewManager().applyCss("database-manager");
    super.getViewManager().show(layout.getLayout());
  }

  private VBox createDatabaseManagerContent() {
    VBox content = new VBox(20);
    content.setPadding(new Insets(20));
    content.getStyleClass().add("database-manager");

    // Database Creation Section
    VBox databaseSection = createDatabaseSection();

    // Table Creation Section
    VBox tableSection = createTableSection();

    content.getChildren().addAll(new Label("Database Manager"), databaseSection, tableSection);

    return content;
  }

  private VBox createDatabaseSection() {
    VBox section = new VBox(10);
    section.getStyleClass().add("section");

    Label sectionTitle = new Label("Create New Database");
    sectionTitle.getStyleClass().add("section-title");

    TextField dbNameField = new TextField();
    dbNameField.setPromptText("Enter database name (example.db)");

    Button createDbButton = new Button("Create Database");
    createDbButton.setOnAction(e -> createDatabase(dbNameField.getText()));

    section.getChildren().addAll(sectionTitle, dbNameField, createDbButton);
    return section;
  }

  private VBox createTableSection() {
    VBox section = new VBox(10);
    section.getStyleClass().add("section");

    Label sectionTitle = new Label("Create New Table");
    sectionTitle.getStyleClass().add("section-title");

    // Table name input
    TextField tableNameField = new TextField();
    tableNameField.setPromptText("Enter table name");

    // Fields section
    Label fieldsLabel = new Label("Table Fields");
    Button addFieldButton = new Button("Add Field");
    addFieldButton.setOnAction(e -> addFieldRow());

    // Create table button
    Button createTableButton = new Button("Create Table");
    createTableButton.setOnAction(e -> createTable(tableNameField.getText()));

    section
        .getChildren()
        .addAll(
            sectionTitle,
            tableNameField,
            fieldsLabel,
            fieldsContainer,
            addFieldButton,
            createTableButton);

    return section;
  }

  private void addFieldRow() {
    HBox fieldRow = new HBox(10);

    TextField fieldNameInput = new TextField();
    fieldNameInput.setPromptText("Field name");

    ComboBox<String> fieldTypeCombo = new ComboBox<>(fieldTypes);
    fieldTypeCombo.setValue("varchar");

    Button removeButton = new Button("Remove");
    removeButton.setOnAction(e -> fieldsContainer.getChildren().remove(fieldRow));

    fieldRow.getChildren().addAll(fieldNameInput, fieldTypeCombo, removeButton);
    fieldsContainer.getChildren().add(fieldRow);
  }

  private void createDatabase(String dbName) {
    try {
      tableService.createBase(dbName);
      showAlert(Alert.AlertType.INFORMATION, "Success", "Database created successfully: " + dbName);
    } catch (Exception e) {
      showAlert(Alert.AlertType.ERROR, "Error", "Failed to create database: " + e.getMessage());
    }
  }

  private void createTable(String tableName) {
    try {
      Map<String, String> fields = new HashMap<>();

      for (javafx.scene.Node node : fieldsContainer.getChildren()) {
        if (node instanceof HBox fieldRow) {
          TextField nameField = (TextField) fieldRow.getChildren().get(0);
          ComboBox<String> typeCombo = (ComboBox<String>) fieldRow.getChildren().get(1);

          fields.put(nameField.getText(), typeCombo.getValue());
        }
      }

      // Assuming the database name is stored somewhere or passed as parameter
      String currentDbName = Table.table.getBase(); // This should be dynamic
      tableService.createTable(currentDbName, tableName, fields);

      showAlert(Alert.AlertType.INFORMATION, "Success", "Table created successfully: " + tableName);
    } catch (Exception e) {
      showAlert(Alert.AlertType.ERROR, "Error", "Failed to create table: " + e.getMessage());
    }
  }

  private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setContentText(content);
    alert.showAndWait();
  }
}
