package org.chaos.office.service;

import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class ComponentService {
  private static Stage primaryStage;

  public static void setPrimaryStage(Stage stage) {
    primaryStage = stage;
  }

  public static <T extends Parent> T getView(Scene scene) {
    return (T) scene.getRoot();
  }

  public static void switchScene(Scene scene) {
    if (primaryStage != null) {
      primaryStage.setScene(scene);
      primaryStage
          .getScene()
          .getStylesheets()
          .add(
              Objects.requireNonNull(ComponentService.class.getResource("/style/main.css"))
                  .toExternalForm());
      primaryStage.show();
    } else {
      throw new IllegalStateException("Primary stage is not set.");
    }
  }

  public static <T extends javafx.scene.layout.Pane> T createBox(
      T box, double prefWidth, double padding) {
    if (prefWidth > 0) {
      box.setPrefWidth(prefWidth);
    }
    box.setPadding(new Insets(padding));
    return box;
  }

  public static Label createHeaderLabel(String text) {
    Label label = new Label(text);
    label.setFont(new Font("Arial", 16));
    label.setTextFill(Color.BLACK);
    label.getStyleClass().add("sidebar-button");
    return label;
  }

  public static Button createSwitchButton(
      String text, Scene targetScene, String fontName, int fontSize) {
    Button button = new Button(text);
    button.setFont(new Font(fontName, fontSize));
    button.setTextFill(Color.BLACK);
    button.getStyleClass().add("sidebar-button");
    button.setOnAction(e -> ComponentService.switchScene(targetScene));
    return button;
  }

  public static Button createOperationButton(
      String text, String fontName, int fontSize, Consumer<Node> action) {
    Button button = new Button(text);
    button.setFont(new Font(fontName, fontSize));
    button.setTextFill(Color.BLACK);
    button.getStyleClass().add("sidebar-button");
    button.setOnAction(e -> action.accept(button));
    return button;
  }
}
