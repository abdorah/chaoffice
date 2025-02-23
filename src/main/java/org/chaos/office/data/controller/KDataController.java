package org.chaos.office.data.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class KDataController {

    @FXML
    private Label connectionStatus;

    @FXML
    private Label currentDatabase;

    @FXML
    private TreeTableView<?> databaseStructureView;

    @FXML
    private TreeView<?> databaseTree;

    @FXML
    private TextField exportDestinationPath;

    @FXML
    private ComboBox<?> exportTargetType;

    @FXML
    private TextField importSourcePath;

    @FXML
    private ComboBox<?> importSourceType;

    @FXML
    private TextArea queryEditor;

    @FXML
    private Label queryExecutionTime;

    @FXML
    private TableView<?> queryResultTable;

    @FXML
    private VBox sidebar;

    @FXML
    private HBox statusBar;

    @FXML
    private TableView<?> tableContentView;

    @FXML
    void closeDatabase(ActionEvent event) {

    }

    @FXML
    void copy(ActionEvent event) {

    }

    @FXML
    void createDatabase(ActionEvent event) {

    }

    @FXML
    void executeQuery(ActionEvent event) {

    }

    @FXML
    void exit(ActionEvent event) {

    }

    @FXML
    void exportData(ActionEvent event) {

    }

    @FXML
    void find(ActionEvent event) {

    }

    @FXML
    void importData(ActionEvent event) {

    }

    @FXML
    void newDatabase(ActionEvent event) {

    }

    @FXML
    void openDatabase(ActionEvent event) {

    }

    @FXML
    void paste(ActionEvent event) {

    }

    @FXML
    void performExport(ActionEvent event) {

    }

    @FXML
    void performImport(ActionEvent event) {

    }

    @FXML
    void refresh(ActionEvent event) {

    }

    @FXML
    void showAbout(ActionEvent event) {

    }

    @FXML
    void showDocumentation(ActionEvent event) {

    }

    @FXML
    void showPreferences(ActionEvent event) {

    }

    @FXML
    void toggleSidebar(ActionEvent event) {

    }
}
