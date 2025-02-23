package org.chaos.office.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class GreetingView extends BorderPane {

  public Label greetingLabel(String text) {
    Label label = new Label(text);
    label.getStyleClass().add("greeting-label");
    label.setFont(new Font("Arial", 24));
    label.setTextFill(Color.WHITE);
    return label;
  }

  public Button greetingButton(String text) {
    Button button = new Button(text);
    button.getStyleClass().add("greeting-button");
    button.setOnAction(e -> System.out.println("Get Started Clicked"));

    return button;
  }

  public GreetingView() {

    HBox header = new HBox(greetingLabel("Welcome to Chaos Office"));
    header.setAlignment(Pos.CENTER);
    header.setPadding(new Insets(50, 0, 20, 0));
    header.setFillHeight(true);

    VBox centerBox = new VBox(20, greetingButton("Get Started"));
    centerBox.setAlignment(Pos.CENTER);

    this.setTop(header);
    this.setCenter(centerBox);

    this.setBackground(
        new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
  }
}
