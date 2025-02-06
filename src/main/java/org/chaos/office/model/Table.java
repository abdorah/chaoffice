package org.chaos.office.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.Singular;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Data
public class Table {
  private String base;
  private String name;
  @Singular private List<Field<?>> fields;

  Table(final String base, final String name, final List<Field<?>> fields) {
    this.base = base;
    this.name = name;
    this.fields = fields;
  }

  public static TableBuilder builder() {
    return new TableBuilder();
  }

  public org.jooq.Table<?> getTable() {
    org.jooq.Table table = DSL.table(this.name);
    for (Field<?> field : this.fields) {
      table.field(field);
    }
    return table;
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
        this.fields = new ArrayList();
      }

      this.fields.add(field);
      return this;
    }

    public TableBuilder field(final String fieldName, final Class<?> fieldType) {
      if (this.fields == null) {
        this.fields = new ArrayList();
      }

      this.fields.add(DSL.field(fieldName, fieldType));
      return this;
    }

    public TableBuilder fields(final Collection<? extends Field<?>> fields) {
      if (fields == null) {
        throw new NullPointerException("fields cannot be null");
      } else {
        if (this.fields == null) {
          this.fields = new ArrayList();
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
        case 1 -> fields = Collections.singletonList((Field) this.fields.get(0));
        default -> fields = Collections.unmodifiableList(new ArrayList(this.fields));
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
}
