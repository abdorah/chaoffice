package org.chaos.office.model;

public enum Type {
  TEXT("TEXT"),
  JSON("JSON"),
  XML("XML"),
  CSV("CSV"),
  EXCEL("EXCEL");

  private final String value;

  private Type(String value) {
    this.value = value;
  }

  public String getType() {
    return this.value;
  }
}
