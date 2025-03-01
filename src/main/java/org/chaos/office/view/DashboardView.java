package org.chaos.office.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.chaos.office.controller.GreetingController;
import org.chaos.office.controller.SignInController;
import org.chaos.office.service.ComponentService;

public class DashboardView extends BorderPane {

  public DashboardView() {
    this.setTop(createHeader());
    this.setLeft(createSidebar());
    this.setRight(null);
    this.setBottom(null);
  }

  private VBox createSidebar() {
    VBox sidebar = ComponentService.createBox(new VBox(15), 200, 20);
    sidebar.getStyleClass().add("sidebar");

    Button homeButton =
        ComponentService.createSwitchButton("Home", new GreetingController(), "Arial", 16);
    Button signInButton =
        ComponentService.createSwitchButton("Sign In", new SignInController(), "Arial", 16);
    Button logoutButton =
        ComponentService.createSwitchButton("Logout", new GreetingController(), "Arial", 16);

    sidebar.getChildren().addAll(homeButton, signInButton, logoutButton);
    sidebar.setAlignment(Pos.TOP_CENTER);
    return sidebar;
  }

  private HBox createHeader() {
    HBox header = ComponentService.createBox(new HBox(15), -1, 20);
    header.getStyleClass().add("header");
    header.setAlignment(Pos.TOP_LEFT);
    header.getChildren().add(ComponentService.createHeaderLabel("Chaos Office"));
    header
        .getChildren()
        .add(
            ComponentService.createOperationButton(
                "Toggle Side Bar",
                "Arial",
                20,
                button -> {
                  if (this.getLeft() == null) {
                    this.setLeft(createSidebar());
                  } else {
                    this.setLeft(null);
                  }
                }));
    header
        .getChildren()
        .add(ComponentService.createSwitchButton("Go Home", new GreetingController(), "Arial", 20));
    return header;
  }
}
