package org.chaos.office.core.controller;

import java.io.IOException;

import org.chaos.office.core.navigation.ViewManager;

import javafx.fxml.FXML;

public class WelcomeScreenController {

    @FXML
    private void handleStartButton() throws IOException {
        System.out.println("Start button");
        ViewManager.getInstance().loadView("SignInScreen");
    }
}
