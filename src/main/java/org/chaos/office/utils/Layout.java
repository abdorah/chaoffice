package org.chaos.office.utils;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class Layout {
  private BorderPane mainLayout;
  private MenuBar menuBar;

  public Layout() {
    mainLayout = new BorderPane();
    setupMenuBar();
    mainLayout.setTop(menuBar);
    mainLayout.getStyleClass().add("main-layout");
  }

  private void setupMenuBar() {
    menuBar = new MenuBar();

    Menu fileMenu = new Menu("File");
    MenuItem newItem = new MenuItem("New");
    MenuItem openItem = new MenuItem("Open");
    MenuItem saveItem = new MenuItem("Save");
    fileMenu.getItems().addAll(newItem, openItem, saveItem);

    Menu helpMenu = new Menu("Help");
    MenuItem aboutItem = new MenuItem("About");
    helpMenu.getItems().add(aboutItem);

    menuBar.getMenus().addAll(fileMenu, helpMenu);
  }

  public BorderPane getLayout() {
    return mainLayout;
  }

  public void setContent(VBox content) {
    mainLayout.setCenter(content);
  }
}
