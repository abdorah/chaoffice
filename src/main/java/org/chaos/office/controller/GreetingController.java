package org.chaos.office.controller;

import javafx.scene.Scene;
import org.chaos.office.view.GreetingView;

public class GreetingController extends Scene {

  public GreetingController() {
    super(new GreetingView());
  }
}
