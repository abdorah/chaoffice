package org.chaos.office.service;

import java.util.Objects;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneService {
  private static Stage primaryStage;

  public static void setPrimaryStage(Stage stage) {
    primaryStage = stage;
  }

  public static void switchScene(Scene scene) {
    if (primaryStage != null) {
      primaryStage.setScene(scene);
      primaryStage
          .getScene()
          .getStylesheets()
          .add(
              Objects.requireNonNull(SceneService.class.getResource("/style/main.css"))
                  .toExternalForm());
      primaryStage.show();
    } else {
      throw new IllegalStateException("Primary stage is not set.");
    }
  }
}
