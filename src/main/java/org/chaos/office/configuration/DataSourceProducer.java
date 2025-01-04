package org.chaos.office.configuration;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalDataSourceConfiguration;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import lombok.NoArgsConstructor;

@ApplicationScoped
@NoArgsConstructor
public class DataSourceProducer {

  // Map to hold dynamically created data sources by their names
  private final Map<String, AgroalDataSource> dataSourceMap = new HashMap<>();

  // Example of dynamic datasource creation method
  public void addDataSource(String name, String jdbcUrl, String username, String password)
      throws SQLException {
    AgroalDataSource dataSource = createDataSource(jdbcUrl, username, password);
    dataSourceMap.put(name, dataSource);
  }

  // Retrieve a DataSource by its name
  public AgroalDataSource getDataSource(String name) {
    return dataSourceMap.get(name);
  }

  // Producer for data sources
  @Produces
  @DefaultBean
  public AgroalDataSource defaultDataSource() {
    // Default behavior: return the first added datasource or throw an error
    if (!dataSourceMap.isEmpty()) {
      return dataSourceMap.values().iterator().next(); // Just return the first one for now
    }
    throw new IllegalStateException("No data sources available");
  }

  // Method to create a new AgroalDataSource
  private AgroalDataSource createDataSource(String database, String username, String password)
      throws SQLException {
    AgroalDataSourceConfigurationSupplier configurationSupplier =
        new AgroalDataSourceConfigurationSupplier();

    AgroalDataSourceConfiguration configuration =
        configurationSupplier
            .connectionPoolConfiguration(
                cp ->
                    cp.initialSize(5)
                        .minSize(5)
                        .maxSize(10)
                        .connectionFactoryConfiguration(
                            cf ->
                                cf.jdbcUrl(String.format("jdbc:sqlite:data/%s.db", database))
                                    .principal(new NamePrincipal(username))
                                    .credential(new SimplePassword(password))))
            .get();

    return AgroalDataSource.from(configuration);
  }
}
