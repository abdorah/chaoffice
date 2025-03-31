package org.chaos.office.view;

import io.swagger.client.ApiException;
import io.swagger.client.api.DefaultApi;
import io.swagger.client.model.Category;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.List;

public class DataTableView extends TableView<Category> {

  public DataTableView() {
    TableColumn<Category, String> idColumn = new TableColumn<>("ID");
    idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

    TableColumn<Category, String> nameColumn = new TableColumn<>("Name");
    nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));


    this.getColumns().addAll(idColumn, nameColumn);

    // Sample data
    DefaultApi defaultApi = new DefaultApi();
    List<Category> categories = new ArrayList<>();
      try {
          categories.addAll(defaultApi.categoriesGet());
      } catch (ApiException e) {
          throw new RuntimeException(e);
      }

      ObservableList<Category> data =
        FXCollections.observableArrayList(categories);

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
