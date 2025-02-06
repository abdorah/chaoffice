package org.chaos.office.model;

import java.util.function.Function;

public class Relation {
  Table sourceTable;
  Table targetTable;

  Column sourceColumn;
  Column targetColumn;

  Function<String, String> mappingFunction;
}
