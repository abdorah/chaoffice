package org.chaos.office.view;

import static org.chaos.office.service.ComponentService.createBox;
import static org.chaos.office.service.ComponentService.createButton;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.chaos.office.controller.DashboardController;
import org.chaos.office.controller.GreetingController;
import org.chaos.office.controller.SignInController;
import org.chaos.office.service.ComponentService;

public class DashboardView extends BorderPane {

  public DashboardView() {
    this.setLeft(createSidebar());
    this.setTop(createHeader());
  }

  private VBox createSidebar() {
    VBox sidebar = createBox(new VBox(15), 200, 20);
    sidebar.getStyleClass().add("sidebar");

    Button homeButton = createButton("Home", new GreetingController(), "Arial", 16);
    Button signInButton = createButton("Sign In", new SignInController(), "Arial", 16);
    Button logoutButton = createButton("Logout", new GreetingController(), "Arial", 16);

    sidebar.getChildren().addAll(homeButton, signInButton, logoutButton);
    sidebar.setAlignment(Pos.TOP_CENTER);
    return sidebar;
  }

  private HBox createHeader() {
    HBox header = ComponentService.createBox(new HBox(15), -1, 20);
    header.getStyleClass().add("header");
    header.setAlignment(Pos.TOP_CENTER);
    header.getChildren().add(createHeaderLabel("Chaos Office"));
    header.getChildren().add(createHeaderButton("Go Home", null));
    return header;
  }

  private Button createHeaderButton(String text, Scene targetScene) {
    return ComponentService.createButton(text, targetScene, "Arial", 20);
  }

  private Label createHeaderLabel(String text) {
    Label label = new Label(text);
    label.setFont(new Font("Arial", 16));
    label.setTextFill(Color.BLACK);
    label.getStyleClass().add("sidebar-button");
    return label;
  }
}
