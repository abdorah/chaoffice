package org.chaos.office.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.text.MessageFormat;
import org.chaos.office.model.BaseType;
import org.jdbi.v3.core.Jdbi;

@ApplicationScoped
public class DataService {

  @Inject DatabaseService databaseService;

  private static final MessageFormat CREATE_TABLE_STATEMENT =
      new MessageFormat("CREATE TABLE IF NOT EXISTS {0} ({1});");
  private static final MessageFormat ADD_COLUMN_STATEMENT =
      new MessageFormat("ALTER TABLE {0} ADD COLUMN {1};");
  private static final MessageFormat DELETE_COLUMN_STATEMENT =
      new MessageFormat("ALTER TABLE {0} DROP COLUMN {1};");
  private static final MessageFormat DELETE_TABLE_STATEMENT =
      new MessageFormat("DROP TABLE IF EXISTS {0};");
  private static final MessageFormat LOAD_DATA_STATEMENT =
      new MessageFormat(
          "LOAD DATA INFILE ''{0}'' INTO TABLE {1} FIELDS TERMINATED BY '','' ENCLOSED BY ''\"\''"
              + " LINES TERMINATED BY ''\n"
              + "'' IGNORE 1 ROWS;");
  private static final MessageFormat FILTER_STATEMENT =
      new MessageFormat("SELECT * FROM {0} WHERE {1};");
  private static final MessageFormat SORT_STATEMENT =
      new MessageFormat("SELECT * FROM {0} ORDER BY {1};");

  public void createTable(String dataSourceName, String tableName, BaseType columns) {
    Jdbi jdbi = databaseService.useDataSource(dataSourceName);
    String createStatement = CREATE_TABLE_STATEMENT.format(new Object[] {tableName, columns});
    jdbi.useHandle(handle -> handle.execute(createStatement));
  }

  public void addColumn(String dataSourceName, String tableName, BaseType column) {
    Jdbi jdbi = databaseService.useDataSource(dataSourceName);
    String addColumnStatement = ADD_COLUMN_STATEMENT.format(new Object[] {tableName, column});
    jdbi.useHandle(handle -> handle.execute(addColumnStatement));
  }

  public void deleteColumn(String dataSourceName, String tableName, String columnName) {
    Jdbi jdbi = databaseService.useDataSource(dataSourceName);
    String deleteColumnStatement =
        DELETE_COLUMN_STATEMENT.format(new Object[] {tableName, columnName});
    jdbi.useHandle(handle -> handle.execute(deleteColumnStatement));
  }

  public void deleteTable(String dataSourceName, String tableName) {
    Jdbi jdbi = databaseService.useDataSource(dataSourceName);
    String deleteTableStatement = DELETE_TABLE_STATEMENT.format(new Object[] {tableName});
    jdbi.useHandle(handle -> handle.execute(deleteTableStatement));
  }

  public void loadExcel(String dataSourceName, String tableName, String filePath) {
    loadFile(dataSourceName, tableName, filePath);
  }

  public void loadCSV(String dataSourceName, String tableName, String filePath) {
    loadFile(dataSourceName, tableName, filePath);
  }

  private void loadFile(String dataSourceName, String tableName, String filePath) {
    Jdbi jdbi = databaseService.useDataSource(dataSourceName);
    String loadDataStatement = LOAD_DATA_STATEMENT.format(new Object[] {filePath, tableName});
    jdbi.useHandle(handle -> handle.execute(loadDataStatement));
  }

  public void filter(String dataSourceName, String tableName, String condition) {
    Jdbi jdbi = databaseService.useDataSource(dataSourceName);
    String filterStatement = FILTER_STATEMENT.format(new Object[] {tableName, condition});
    jdbi.useHandle(handle -> handle.execute(filterStatement));
  }

  public void sort(String dataSourceName, String tableName, String orderBy) {
    Jdbi jdbi = databaseService.useDataSource(dataSourceName);
    String sortStatement = SORT_STATEMENT.format(new Object[] {tableName, orderBy});
    jdbi.useHandle(handle -> handle.execute(sortStatement));
  }

  public void convertToJson(String dataSourceName, String tableName) {
    // This would typically involve querying the data and converting it to JSON
    throw new UnsupportedOperationException("JSON conversion not implemented yet");
  }

  public void convertToTable(String dataSourceName, String tableName) {
    // This would typically involve parsing JSON and inserting it into a table
    throw new UnsupportedOperationException("Table conversion from JSON not implemented yet");
  }

  public void loadJson(String dataSourceName, String tableName, String filePath) {
    // JSON loading might require a different approach, possibly using a JSON parser
    throw new UnsupportedOperationException("JSON loading not implemented yet");
  }
}
