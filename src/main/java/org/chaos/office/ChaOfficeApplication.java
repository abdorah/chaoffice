package org.chaos.office;

import javafx.application.Application;
import javafx.stage.Stage;
import org.chaos.office.controller.GreetingController;
import org.chaos.office.service.ComponentService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class ChaOfficeApplication extends Application {
    @Override
    public void start(Stage primaryStage) {
        // Création d'une WebView
        WebView webView = new WebView();

        // Chargement de l'URL de l'interface utilisateur de Pocketbase
        // Remplace 'http://localhost:8090/_/' par l'URL de ton instance Pocketbase
        webView.getEngine().load("http://localhost:8090/_/");

        // Création d'un conteneur pour la WebView
        StackPane root = new StackPane(webView);

        // Création de la scène
        Scene scene = new Scene(root, 800, 600); // Largeur et hauteur de la fenêtre

        // Configuration du titre de la fenêtre
        primaryStage.setTitle("Pocketbase UI dans JavaFX");

        // Ajout de la scène à la fenêtre
        primaryStage.setScene(scene);

        // Affichage de la fenêtre
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
