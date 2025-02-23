package org.chaos.office;

import java.util.Objects;
import javafx.application.Application;
import javafx.stage.Stage;
import org.chaos.office.controller.GreetingController;
import org.chaos.office.view.GreetingView;

public class ChaOfficeApplication extends Application {

  @Override
  public void start(Stage primaryStage) {
    primaryStage.setTitle("Chaos Office");
    primaryStage.setScene(new GreetingController());
    primaryStage.getScene().setRoot(new GreetingView());
    primaryStage.setHeight(768);
    primaryStage.setWidth(1024);
    primaryStage
        .getScene()
        .getStylesheets()
        .add(Objects.requireNonNull(getClass().getResource("/style/main.css")).toExternalForm());
    primaryStage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
