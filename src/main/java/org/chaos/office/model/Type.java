package org.chaos.office.model;

public enum Type {
  TEXT("TEXT"),
  JSON("JSON");

  private String value;

  private Type(String value) {
    this.value = value;
  }

  public String getType() {
    return this.value;
  }
}
