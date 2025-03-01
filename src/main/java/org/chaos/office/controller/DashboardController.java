package org.chaos.office.controller;

import javafx.scene.Scene;
import org.chaos.office.service.ComponentService;
import org.chaos.office.view.DashboardView;
import org.chaos.office.view.DataTableView;

public class DashboardController extends Scene {

  public DashboardController() {
    super(new DashboardView());
    DashboardView dashboardView = ComponentService.getView(this);
    dashboardView.setCenter(new DataTableView());
  }
}
