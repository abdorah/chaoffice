package org.chaos.office.controller;

import io.quarkiverse.fx.views.FxView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.chaos.office.utils.BaseController;

/**
 * Main Stage loader. This class is the only view that will be loaded onto the Fx Thread.
 * Its role is to be the initial page that be shown, since the fx application is single threaded.
 * Nevertheless, it shows nothing it just calls other fx scenes on the stage created here.
 *
 * @author KOTBI Abderrahmane
 */
@FxView
@ApplicationScoped
public class MainController extends BaseController {

  @Inject WelcomeController welcomeController;

  @FXML
  public void initialize() {
    // Create UI components
    Button welcomeButton = new Button("Welcome");
    welcomeButton.setOnAction(
        event -> {
          System.out.println("Welcome!");
          welcomeController.initialize();
        });

    // Arrange components in a layout
    VBox root = new VBox(welcomeButton);
    // Set scene root node and show the scene
    super.getViewManager().show(root);
  }
}
