package org.chaos.office.domain;

import java.util.*;
import lombok.Data;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jooq.*;
import org.jooq.impl.DSL;

@Data
public class Table {
  private String base;
  private String name;
  private List<Field<?>> fields;

  public static Table table = new Table();

  Table(final String base, final String name, final List<Field<?>> fields) {
    this.base = base;
    this.name = name;
    this.fields = fields;
  }

  Table() {}

  public static TableBuilder builder() {
    return new TableBuilder();
  }

  public static class TableBuilder {
    private String base;
    private String name;
    private ArrayList<Field<?>> fields;

    TableBuilder() {}

    public TableBuilder base(final String base) {
      this.base = base;
      return this;
    }

    public TableBuilder name(final String name) {
      this.name = name;
      return this;
    }

    public TableBuilder field(final Field<?> field) {
      if (this.fields == null) {
        this.fields = new ArrayList<>();
      }

      this.fields.add(field);
      return this;
    }

    public TableBuilder field(final String fieldName, final Class<?> fieldType) {
      if (this.fields == null) {
        this.fields = new ArrayList<>();
      }

      this.fields.add(DSL.field(fieldName, fieldType));
      return this;
    }

    public TableBuilder fields(final Collection<? extends Field<?>> fields) {
      if (fields == null) {
        throw new NullPointerException("fields cannot be null");
      } else {
        if (this.fields == null) {
          this.fields = new ArrayList<>();
        }

        this.fields.addAll(fields);
        return this;
      }
    }

    public TableBuilder clearFields() {
      if (this.fields != null) {
        this.fields.clear();
      }

      return this;
    }

    public Table build() {
      List<Field<?>> fields;
      switch (this.fields == null ? 0 : this.fields.size()) {
        case 0 -> fields = Collections.emptyList();
        case 1 -> fields = List.of(this.fields.get(0));
        default -> fields = List.copyOf(this.fields);
      }

      return new Table(this.base, this.name, fields);
    }

    public String toString() {
      return "Table.TableBuilder(base="
          + this.base
          + ", name="
          + this.name
          + ", fields="
          + this.fields
          + ")";
    }
  }

  public Jdbi createBase(String baseName) {
    if (baseName.matches("\\w+\\.db")) {
      table.setBase(baseName);
      return Jdbi.create("jdbc:sqlite:data/" + baseName).open().commit().getJdbi();
    }
    throw new RuntimeException("Invalid base name: " + baseName);
  }

  public void createTable(Jdbi base, Table table) {
    SQLDialect dialect = DSL.using(SQLDialect.SQLITE).configuration().dialect();
    DSLContext jooq = DSL.using(dialect);
    if (table.getName().matches("\\w+")) {
      try (Handle handle = base.open()) {
        String createTableSQL =
            jooq.createTable(table.getName()).columns(table.getFields()).getSQL();
        handle.execute(createTableSQL);
      }
    } else {
      throw new RuntimeException("Invalid table name: " + table.getName());
    }
  }

  public void createTable(String baseName, String tableName, Map<String, String> fields) {
    org.chaos.office.domain.Table.TableBuilder tableBuilder =
        org.chaos.office.domain.Table.builder().name(tableName).base(baseName);
    for (Map.Entry<String, String> field : fields.entrySet()) {
      tableBuilder.field(field.getKey(), mapFieldType(field.getValue())).build();
    }
    createTable(createBase(baseName), tableBuilder.build());
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
