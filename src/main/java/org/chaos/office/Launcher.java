package org.chaos.office;

/**
 * Launcher class for jpackage distribution.
 * Required because JavaFX Application subclass cannot be the
 * main entry point when running from a non-modular fat JAR.
 */
public class Launcher {
  public static void main(String[] args) {
    ChaOfficeApplication.main(args);
  }
}
