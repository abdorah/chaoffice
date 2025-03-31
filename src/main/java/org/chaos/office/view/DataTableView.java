package org.chaos.office.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class DataTableView extends TableView<DataTableView.DataItem> {

  public DataTableView() {
    TableColumn<DataItem, String> idColumn = new TableColumn<>("ID");
    idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

    TableColumn<DataItem, String> nameColumn = new TableColumn<>("Name");
    nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

    TableColumn<DataItem, String> descriptionColumn = new TableColumn<>("Description");
    descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

    this.getColumns().addAll(idColumn, nameColumn, descriptionColumn);

    // Sample data
    ObservableList<DataItem> data =
        FXCollections.observableArrayList(
            new DataItem("1", "Item 1", "Description 1"),
            new DataItem("2", "Item 2", "Description 2"),
            new DataItem("3", "Item 3", "Description 3"));

    this.setItems(data);
  }

  public static class DataItem {
    private final String id;
    private final String name;
    private final String description;

    public DataItem(String id, String name, String description) {
      this.id = id;
      this.name = name;
      this.description = description;
    }

    public String getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }
  }
}
