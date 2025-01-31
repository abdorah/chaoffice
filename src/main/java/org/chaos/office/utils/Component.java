package org.chaos.office.utils;

import jakarta.enterprise.context.ApplicationScoped;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

@ApplicationScoped
public class Component {

  public HBox buildTextInput(String title) {
    Label label = new Label(title);
    TextField field = new TextField();
    HBox group = new HBox(label, field);
    group.setPadding(new Insets(10, 7, 10, 15));
    group.setSpacing(5);
    return group;
  }

  public HBox buildPassword(String title) {
    Label label = new Label(title);
    PasswordField field = new PasswordField();
    HBox group = new HBox(label, field);
    group.setPadding(new Insets(10, 7, 10, 15));
    group.setSpacing(5);
    return group;
  }

  public TextField extractTextField(HBox component) {
    return (TextField) component.getChildren().get(1);
  }
}
