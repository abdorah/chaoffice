package org.chaos.office.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.chaos.office.utils.BaseController;
import org.chaos.office.utils.Layout;

@ApplicationScoped
public class DashboardController extends BaseController {

  @Inject DatabaseManagerController databaseManagerController;

  @FXML
  public void initialize() {
    Layout layout = new Layout();

    // Create dashboard components
    VBox dashboardContent = new VBox(10);
    dashboardContent.setPadding(new Insets(20));
    dashboardContent.getStyleClass().add("dashboard-content");

    // Header
    Label welcomeLabel = new Label("Dashboard");
    welcomeLabel.getStyleClass().add("dashboard-header");

    // Quick Actions
    HBox quickActions = createQuickActions();

    // Recent Items
    VBox recentItems = createRecentItems();

    dashboardContent.getChildren().addAll(welcomeLabel, quickActions, recentItems);

    layout.setContent(dashboardContent);

    // Apply the CSS file
    this.getViewManager().applyCss("dashboard");
    super.getViewManager().show(layout.getLayout());
  }

  private HBox createQuickActions() {
    HBox actions = new HBox(10);
    actions.getStyleClass().add("quick-actions");

    Button newDocButton = new Button("New Document");
    Button newProjectButton = new Button("New Project");
    Button databaseManagerButton = new Button("Database Manager");
    Button settingsButton = new Button("Settings");

    databaseManagerButton.setOnAction(
        e -> {
          databaseManagerController.initialize();
        });

    actions
        .getChildren()
        .addAll(newDocButton, newProjectButton, databaseManagerButton, settingsButton);
    return actions;
  }

  private VBox createRecentItems() {
    VBox recentItems = new VBox(5);
    recentItems.getStyleClass().add("recent-items");

    Label recentHeader = new Label("Recent Items");
    recentHeader.getStyleClass().add("section-header");

    // Add some dummy recent items
    for (int i = 1; i <= 5; i++) {
      HBox item = new HBox(10);
      Label itemLabel = new Label("Item " + i);
      Label dateLabel = new Label("2025-02-15");
      item.getChildren().addAll(itemLabel, dateLabel);
      recentItems.getChildren().add(item);
    }

    return recentItems;
  }
}
