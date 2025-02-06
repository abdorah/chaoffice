package org.chaos.office.utils;

import jakarta.enterprise.context.ApplicationScoped;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.Getter;

@Getter
@ApplicationScoped
public class ViewManager {
  Stage stage;

  private ViewManager(Parent root, int width, int height, String title) {
    this.stage = new Stage();
    Scene scene = new Scene(root, width, height);
    this.stage.setScene(scene);
    this.stage.setTitle(title);
  }

  public ViewManager() {
    this.stage = new ViewManager(new StackPane(), 300, 200, "Chaos Office Application").getStage();
  }
}
