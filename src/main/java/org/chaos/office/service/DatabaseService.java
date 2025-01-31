package org.chaos.office.service;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import org.chaos.office.configuration.DataSourceProducer;
import org.jdbi.v3.core.Jdbi;

@ApplicationScoped
public class DatabaseService {

  @Inject DataSourceProducer dataSourceProducer;

  public void initDataSource(String name, String user, String password) throws SQLException {
    // Add a primary datasource
    dataSourceProducer.addDataSource(name, user, password);

    // Retrieve a datasource by name
    AgroalDataSource dataSource = dataSourceProducer.getDataSource(name);

    // Use the data source
    Jdbi.create(dataSource);
  }

  public void saveCheckPoint(String dataSourceName, String backUpName)
      throws SQLException, IOException {
    // Get the path of database file
    File database =
        new File(
            dataSourceProducer
                .getDataSource(dataSourceName)
                .getConnection()
                .getMetaData()
                .getURL());

    // Clone the Database file
    Files.copy(database.toPath(), new File(database.getParentFile(), backUpName).toPath());
  }

  // use a default datasource if needed
  public Jdbi useDataSource(String name) {
    AgroalDataSource dataSource = dataSourceProducer.getDataSource(name);
    if (dataSource == null) {
      throw new IllegalArgumentException("Data source with name " + name + " not found");
    }
    return Jdbi.create(dataSource);
  }
}
