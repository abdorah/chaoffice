module org.chaos.office {
  requires java.desktop;
  requires transitive java.sql;
  requires java.prefs;
  requires transitive javafx.controls;
  requires javafx.fxml;
  requires javafx.web;
  requires transitive org.slf4j;
  requires org.xerial.sqlitejdbc;
  requires com.github.librepdf.openpdf;

  opens org.chaos.office to
      javafx.fxml;
  opens org.chaos.office.view to
      javafx.base;
  opens org.chaos.office.model to
      javafx.base;

  exports org.chaos.office;
  exports org.chaos.office.model;
  exports org.chaos.office.util;
}
