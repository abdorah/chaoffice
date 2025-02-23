package org.chaos.office.core.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import org.chaos.office.core.navigation.ViewManager;

public class MainContentController {
    @FXML
    private StackPane rootComponent;
    
    public void initialize() {
        ViewManager viewManager = ViewManager.getInstance();
        viewManager.loadViewIntoPane("KData", rootComponent);
    }
}