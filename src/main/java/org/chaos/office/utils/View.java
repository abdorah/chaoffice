package org.chaos.office.utils;

import jakarta.enterprise.context.ApplicationScoped;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@ApplicationScoped
public class View {
  Stage stage;

  private View(Parent root, int width, int height, String title) {
    this.stage = new Stage();
    Scene scene = new Scene(root, width, height);
    this.stage.setScene(scene);
    this.stage.setTitle(title);
  }

  public View() {
    this.stage = new View(new StackPane(), 300, 200, "Chaos Office Application").getStage();
  }
}
