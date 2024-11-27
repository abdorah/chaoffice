module org.chaos.office {
    requires java.desktop;
    requires transitive java.sql;
    requires org.xerial.sqlitejdbc;
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires transitive org.slf4j;
    
    exports org.chaos.office;
}
