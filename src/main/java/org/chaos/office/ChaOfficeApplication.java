package org.chaos.office;

import java.net.URISyntaxException;

import org.chaos.office.core.navigation.ViewManager;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class ChaOfficeApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws URISyntaxException {
        ViewManager viewManager = ViewManager.getInstance();
        viewManager.setPrimaryStage(primaryStage);

        viewManager.createView("SideBar")
                .withFxml("/components/SideBar.fxml")
                .withCss("/styles/main.css")
                .register();

        viewManager.createView("Header")
                .withFxml("/components/Header.fxml")
                .withCss("/styles/main.css")
                .register();

        viewManager.createView("MainContent")
                .withFxml("/components/MainContent.fxml")
                .withCss("/styles/main.css")
                .register();

        viewManager.createView("MainScreen")
                .withFxml("/views/MainScreen.fxml")
                .withCss("/styles/main.css")
                .register();

        viewManager.createView("WelcomeScreen")
                .withFxml("/views/WelcomeScreen.fxml")
                .withCss("/styles/main.css")
                .register();

        primaryStage.setTitle("ChaOffice");

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        primaryStage.setWidth(screenBounds.getWidth() * 0.8);
        primaryStage.setHeight(screenBounds.getHeight() * 0.8);

        primaryStage.setMinWidth(300);
        primaryStage.setMinHeight(600);

        Font font = Font.loadFont(getClass().getResource("/fonts/Ubuntu-BoldItalic.ttf").toURI().toString(), 0);

        System.out.println(font.getName());
        primaryStage.centerOnScreen();

        viewManager.loadView("WelcomeScreen");

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
