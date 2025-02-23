package org.chaos.office.core.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ViewManager {
    private static ViewManager instance;
    private Stage primaryStage;
    private Scene mainScene;
    private Map<String, ViewConfig> viewConfigs;

    private ViewManager() {
        this.viewConfigs = new HashMap<>();
    }

    public static ViewManager getInstance() {
        if (instance == null) {
            instance = new ViewManager();
        }
        return instance;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public ViewBuilder createView(String viewName) {
        return new ViewBuilder(viewName);
    }

    public void loadView(String viewName) {
        ViewConfig config = viewConfigs.get(viewName);
        if (config == null) {
            throw new IllegalArgumentException("View not configured: " + viewName);
        }

        try {
            URL fxmlUrl = getClass().getResource(config.fxmlPath);
            if (fxmlUrl == null) {
                throw new IOException("Cannot find file: " + config.fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            if (mainScene == null) {
                mainScene = new Scene(root);
                primaryStage.setScene(mainScene);
            } else {
                mainScene.setRoot(root);
            }

            if (config.cssPath != null) {
                URL cssUrl = getClass().getResource(config.cssPath);
                if (cssUrl != null) {
                    mainScene.getStylesheets().clear();
                    mainScene.getStylesheets().add(cssUrl.toExternalForm());
                } else {
                    System.err.println("CSS file not found: " + config.cssPath);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading view: " + viewName + ". " + e.getMessage());
        }
    }

    public void loadViewIntoPane(String viewName, Pane targetPane) {
        ViewConfig config = viewConfigs.get(viewName);
        if (config == null) {
            throw new IllegalArgumentException("View not configured: " + viewName);
        }
    
        try {
            URL fxmlUrl = getClass().getResource(config.fxmlPath);
            if (fxmlUrl == null) {
                throw new IOException("Cannot find file: " + config.fxmlPath);
            }
    
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
    
            targetPane.getChildren().clear();
            targetPane.getChildren().add(root);
    
            if (config.cssPath != null) {
                URL cssUrl = getClass().getResource(config.cssPath);
                if (cssUrl != null) {
                    root.getStylesheets().clear();
                    root.getStylesheets().add(cssUrl.toExternalForm());
                } else {
                    System.err.println("CSS file not found: " + config.cssPath);
                }
            }
    
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading view: " + viewName + ". " + e.getMessage());
        }
    }

    public class ViewBuilder {
        private String viewName;
        private String fxmlPath;
        private String cssPath;

        private ViewBuilder(String viewName) {
            this.viewName = viewName;
        }

        public ViewBuilder withFxml(String fxmlPath) {
            this.fxmlPath = fxmlPath;
            return this;
        }

        public ViewBuilder withCss(String cssPath) {
            this.cssPath = cssPath;
            return this;
        }

        public void register() {
            viewConfigs.put(viewName, new ViewConfig(fxmlPath, cssPath));
        }
    }

    private static class ViewConfig {
        String fxmlPath;
        String cssPath;

        ViewConfig(String fxmlPath, String cssPath) {
            this.fxmlPath = fxmlPath;
            this.cssPath = cssPath;
        }
    }
}
