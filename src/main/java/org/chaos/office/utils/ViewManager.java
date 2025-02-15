package org.chaos.office.utils;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Objects;
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
    this.stage = new ViewManager(new StackPane(), 800, 600, "Chaos Office Application").getStage();
  }

  public Scene getScene() {
    return this.stage.getScene();
  }

  public void setScene(Scene scene) {
    this.stage.setScene(scene);
  }

  public void setRoot(Parent parent) {
    this.stage.getScene().setRoot(parent);
  }

  public void show() {
    this.stage.show();
  }

  public void show(Parent root) {
    this.setRoot(root);
    this.show();
  }

  public void applyCss(String fileName) {
    String cssPath =
        Objects.requireNonNull(getClass().getResource(String.format("/style/%s.css", fileName)))
            .toExternalForm();
    this.getStage().getScene().getStylesheets().add(cssPath);
  }
}
