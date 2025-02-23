package org.chaos.office.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.chaos.office.controller.GreetingController;
import org.chaos.office.controller.SignInController;
import org.chaos.office.service.SceneService;

public class DashboardView extends BorderPane {

  private VBox createSidebar() {
    VBox sidebar = new VBox(15);
    sidebar.setPadding(new Insets(20));
    sidebar.getStyleClass().add("sidebar");
    sidebar.setPrefWidth(200);

    Button homeButton = createSidebarButton("Home", new GreetingController());
    Button signInButton = createSidebarButton("Sign In", new SignInController());
    Button logoutButton = createSidebarButton("Logout", new GreetingController());

    sidebar.getChildren().addAll(homeButton, signInButton, logoutButton);
    sidebar.setAlignment(Pos.TOP_CENTER);
    return sidebar;
  }

  private Button createSidebarButton(String text, Scene targetScene) {
    Button button = new Button(text);
    button.setFont(new Font("Arial", 16));
    button.setTextFill(Color.WHITE);
    button.getStyleClass().add("sidebar-button");
    button.setOnAction(e -> SceneService.switchScene(targetScene));
    return button;
  }

  public DashboardView() {
    this.setLeft(createSidebar());
  }
}
