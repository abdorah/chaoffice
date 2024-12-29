module chaoffice {
    requires agroal.api;
    requires arc;
    requires jakarta.cdi;
    requires jakarta.inject;
    requires java.sql;
    requires javafx.controls;
    requires static lombok;
    requires org.jdbi.v3.core;
    requires quarkus.fx;
    requires quarkus.core;
    requires javafx.fxml;

    exports org.chaos.office;
    exports org.chaos.office.controller;

    opens org.chaos.office to javafx.fxml;
    opens org.chaos.office.controller to javafx.fxml;
}