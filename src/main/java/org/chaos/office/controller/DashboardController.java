package org.chaos.office.controller;

import javafx.scene.Scene;
import org.chaos.office.view.DashboardView;

public class DashboardController extends Scene {

  public DashboardController() {
    super(new DashboardView());
    this.setRoot(super.getRoot());
  }
}
