module org.chaos.office {
  requires java.desktop;
  requires transitive javafx.controls;
  requires javafx.fxml;
  requires javafx.web;
  requires transitive org.slf4j;
  requires java.compiler;
  requires okio;
  requires okhttp3;
  requires com.google.gson;
  requires gson.fire;
  requires java.sql;
  requires okhttp3.logging;
  requires swagger.annotations;
  requires annotations;

  opens org.chaos.office to
      javafx.fxml;
  opens org.chaos.office.view to
      javafx.base;

  exports org.chaos.office;
}
