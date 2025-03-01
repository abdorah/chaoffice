package org.chaos.office.controller;

import javafx.scene.Scene;
import org.chaos.office.view.DashboardView;
import org.chaos.office.view.DataTableView;

public class DashboardController extends Scene {
  private DashboardView dashboardView;

  public DashboardController() {
    super(new DashboardView());
    this.dashboardView = (DashboardView) getRoot();
    initializeView();
  }

  private void initializeView() {
    DataTableView dataTableView = new DataTableView();
    dashboardView.setCenter(dataTableView);
    dashboardView.addSidebarToggle(this::toggleSidebar);
  }

  private void toggleSidebar() {
    dashboardView.toggleSidebar();
  }
}
