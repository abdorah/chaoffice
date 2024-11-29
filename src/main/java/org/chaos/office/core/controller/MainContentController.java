package org.chaos.office.core.controller;

import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;

public class MainContentController {
    @FXML
    private WebView webView;

    public void initialize() {
        WebEngine webEngine = webView.getEngine();
        webEngine.load(getClass().getResource("/html/content.html").toExternalForm());
    }
}
