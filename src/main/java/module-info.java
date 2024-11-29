module org.chaos.office {
    requires java.desktop;
    requires transitive java.sql;
    requires org.xerial.sqlitejdbc;
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires transitive org.slf4j;
    
    opens org.chaos.office.core.controller to javafx.fxml;

    exports org.chaos.office.core.controller to javafx.fxml;
    exports org.chaos.office;
}
