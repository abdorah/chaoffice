module org.chaos.office {
  requires java.desktop;
  requires transitive javafx.controls;
  requires javafx.fxml;
  requires javafx.web;
  requires transitive org.slf4j;

  opens org.chaos.office to
      javafx.fxml;

  exports org.chaos.office;
}
