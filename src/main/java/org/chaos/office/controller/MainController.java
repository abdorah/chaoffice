package org.chaos.office.controller;

import io.quarkiverse.fx.RunOnFxThread;
import io.quarkiverse.fx.views.FxView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.chaos.office.utils.View;

/**
 * Main Stage loader. This class is the only view that will be loaded onto the Fx Thread.
 * Its role is to be the initial page that be shown, since the fx application is single threaded.
 * Nevertheless, it shows nothing it just calls other fx scenes on the stage created here.
 *
 * @author KOTBI Abderrahmane
 */
@FxView
@RunOnFxThread
@ApplicationScoped
public class MainController {

  @Inject View view;

  @Inject HomeController homeController;

  @Inject DatabaseController databaseController;

  @FXML
  public void initialize() {
    // Create UI components
    Button homeButton = new Button("Home");
    homeButton.setOnAction(
        event -> {
          System.out.println("Home!");
          homeController.initialize();
        });

    Button databaseButton = new Button("Database");
    databaseButton.setOnAction(
        event -> {
          System.out.println("Database!");
          databaseController.initialize();
        });

    // Arrange components in a layout
    VBox root = new VBox(homeButton, databaseButton);

    // Set scene root node and show the scene
    view.getStage().getScene().setRoot(root);
    view.getStage().show();
  }
}
