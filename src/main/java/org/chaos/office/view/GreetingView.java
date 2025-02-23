package org.chaos.office.view;

import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

public class GreetingView extends Pane {

  public Label greetingLabel(String text) {
    Label label = new Label(text);
    label.setStyle("-fx-font-weight: bold");
    label.setWrapText(true);
    label.setMinWidth(300);
    label.setMaxWidth(300);
    return label;
  }

  public GreetingView() {
    this.getChildren().add(greetingLabel("Hello World"));
  }
}
