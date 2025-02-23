package org.chaos.office.service;

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
      primaryStage.show();
    } else {
      throw new IllegalStateException("Primary stage is not set.");
    }
  }
}
