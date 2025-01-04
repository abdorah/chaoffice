package org.chaos.office.model;

public enum Constraint {
  PRIMARY_KEY("PRIMARY KEY"),
  FOREIGN_KEY("FOREIGN KEY"),
  NOT_NULL("NOT NULL");

  private final String value;

  private Constraint(String value) {
    this.value = value;
  }

  public String getConstraint() {
    return this.value;
  }
}
