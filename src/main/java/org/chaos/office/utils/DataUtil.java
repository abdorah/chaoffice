package org.chaos.office.utils;

import jakarta.enterprise.context.ApplicationScoped;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;

@ApplicationScoped
public class DataUtil {

  public Jdbi createBase(String baseName) {
    if (baseName.matches("\\w+\\.db")) {
      return Jdbi.create("jdbc:sqlite:data/" + baseName);
    }
    throw new RuntimeException("Invalid base name: " + baseName);
  }

  public void createTable(Jdbi base, Table table) {
    SQLDialect dialect = DSL.using(SQLDialect.SQLITE).configuration().dialect();
    DSLContext jooq = DSL.using(dialect);
    if (table.getName().matches("\\w+")) {
      try (Handle handle = base.open()) {
        String createTableSQL = jooq.createTable(table.getName()).columns(table.fields()).getSQL();
        handle.execute(createTableSQL);
      }
    } else {
      throw new RuntimeException("Invalid table name: " + table.getName());
    }
  }

  public Class<?> mapFieldType(String fieldType) {
    return switch (fieldType) {
      case "varchar" -> String.class;
      case "int" -> Integer.class;
      case "long" -> Long.class;
      case "double" -> Double.class;
      case "float" -> Float.class;
      case "boolean" -> Boolean.class;
      default -> throw new RuntimeException("Invalid field type: " + fieldType);
    };
  }
}
