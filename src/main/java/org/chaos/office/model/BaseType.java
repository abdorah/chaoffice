package org.chaos.office.model;

public record BaseType(String name, Type type, Constraint constraint) {

    @Override
    public String toString() {
        return "'" + name + "' " + type + " " + constraint;
    }
}
