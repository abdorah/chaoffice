package org.chaos.office.service;

import java.util.Map;
import org.chaos.office.utils.DataUtil;

public class TransactionalDataService {

  public void createBase(String baseName) {
    DataUtil.createBase(baseName);
  }

  public void createTable(String baseName, String tableName, Map<String, String> fields) {
    org.chaos.office.model.Table.TableBuilder tableBuilder =
        org.chaos.office.model.Table.builder().name(tableName).base(baseName);
    for (Map.Entry<String, String> field : fields.entrySet()) {
      tableBuilder
          .field(field.getKey(), DataUtil.mapFieldType(field.getValue()))
          .build()
          .getTable();
    }
    DataUtil.createTable(DataUtil.createBase(baseName), tableBuilder.build().getTable());
  }
}
