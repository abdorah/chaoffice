package org.chaos.office.view;

import static org.chaos.office.service.ComponentService.createBox;
import static org.chaos.office.service.ComponentService.createSwitchButton;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.chaos.office.controller.GreetingController;
import org.chaos.office.controller.SignInController;
import org.chaos.office.service.ComponentService;

public class DashboardView extends BorderPane {

  public DashboardView() {
    this.setTop(createHeader());
    this.setLeft(this.createSidebar());
    this.setRight(null);
    this.setBottom(null);
    DataTableView dataTable = new DataTableView();

    VBox centerContent = new VBox(10);
    centerContent.getChildren().add(dataTable);

    this.setCenter(centerContent);
  }

  private VBox createSidebar() {
    VBox sidebar = createBox(new VBox(15), 200, 20);
    sidebar.getStyleClass().add("sidebar");

    Button homeButton = createSwitchButton("Home", new GreetingController(), "Arial", 16);
    Button signInButton = createSwitchButton("Sign In", new SignInController(), "Arial", 16);
    Button logoutButton = createSwitchButton("Logout", new GreetingController(), "Arial", 16);

    sidebar.getChildren().addAll(homeButton, signInButton, logoutButton);
    sidebar.setAlignment(Pos.TOP_CENTER);
    return sidebar;
  }

  private HBox createHeader() {
    HBox header = ComponentService.createBox(new HBox(15), -1, 20);
    header.getStyleClass().add("header");
    header.setAlignment(Pos.TOP_LEFT);
    header.getChildren().add(createHeaderLabel("Chaos Office"));
    header.getChildren().add(createHeaderButton("Go Home", null));
    header
        .getChildren()
        .add(
            ComponentService.createOperationButton(
                "Toggle Side Bar",
                "Arial",
                20,
                (button) -> {
                  if (this.getLeft() == null) {
                    this.setLeft(this.createSidebar());
                  } else {
                    this.setLeft(null);
                  }
                }));
    return header;
  }

  private Button createHeaderButton(String text, Scene targetScene) {
    return ComponentService.createSwitchButton(text, targetScene, "Arial", 20);
  }

  private Label createHeaderLabel(String text) {
    Label label = new Label(text);
    label.setFont(new Font("Arial", 16));
    label.setTextFill(Color.BLACK);
    label.getStyleClass().add("sidebar-button");
    return label;
  }
}
