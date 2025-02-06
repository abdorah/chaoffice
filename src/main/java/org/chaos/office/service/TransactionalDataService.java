package org.chaos.office.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import org.chaos.office.utils.DataUtil;

@ApplicationScoped
public class TransactionalDataService {

  @Inject DataUtil dataUtil;

  public void createBase(String baseName) {
    dataUtil.createBase(baseName);
  }

  public void createTable(String baseName, String tableName, Map<String, String> fields) {
    org.chaos.office.model.Table.TableBuilder tableBuilder =
        org.chaos.office.model.Table.builder().name(tableName).base(baseName);
    for (Map.Entry<String, String> field : fields.entrySet()) {
      tableBuilder
          .field(field.getKey(), dataUtil.mapFieldType(field.getValue()))
          .build()
          .getTable();
    }
    dataUtil.createTable(dataUtil.createBase(baseName), tableBuilder.build().getTable());
  }
}
