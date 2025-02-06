module chaoffice {
  requires agroal.api;
  requires arc;
  requires jakarta.cdi;
  requires jakarta.inject;
  requires javafx.controls;
  requires static lombok;
  requires org.jdbi.v3.core;
  requires quarkus.fx;
  requires quarkus.core;
  requires javafx.fxml;
  requires org.eclipse.jgit;
  requires org.apache.poi.poi;
  requires jakarta.json;
  requires org.apache.commons.lang3;
  requires resteasy.reactive.common;
  requires org.jooq;

  exports org.chaos.office;

  opens org.chaos.office to
      javafx.fxml;
}
