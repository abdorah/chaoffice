package org.chaos.office.controller;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.chaos.office.util.LocaleManager;
import org.chaos.office.util.SessionManager;
import org.chaos.office.view.DashboardView;

/**
 * DashboardController - Main dashboard controller
 * Requirements: 14.1, 14.2, 14.7, 3.6
 */
public class DashboardController extends Scene {
    
    private final DashboardView dashboardView;
    private final Stage stage;
    
    public DashboardController(Stage stage) {
        super(new DashboardView(), 1200, 800);
        this.stage = stage;
        this.dashboardView = (DashboardView) getRoot();
        
        // Apply stylesheet
        getStylesheets().add(getClass().getResource("/style/main.css").toExternalForm());
        
        setupEventHandlers();
        showWelcomeMessage();
    }
    
    private void setupEventHandlers() {
        // Logout button
        dashboardView.getLogoutButton().setOnAction(e -> handleLogout());
        
        // Navigation buttons
        dashboardView.getPartsButton().setOnAction(e -> showPartsInventory());
        dashboardView.getCategoriesButton().setOnAction(e -> showCategories());
        dashboardView.getBillingButton().setOnAction(e -> showBilling());
        dashboardView.getBillsButton().setOnAction(e -> showBillsHistory());
        dashboardView.getAnalyticsButton().setOnAction(e -> showSalesAnalytics());
        dashboardView.getSettingsButton().setOnAction(e -> showSettings());
    }
    
    private void handleLogout() {
        SessionManager.getInstance().clearSession();
        SignInController loginController = new SignInController(stage);
        stage.setScene(loginController);
    }
    
    private void showPartsInventory() {
        PartsInventoryController controller = new PartsInventoryController();
        dashboardView.getContentPane().getChildren().clear();
        dashboardView.getContentPane().getChildren().add(controller.getView());
    }
    
    private void showCategories() {
        CategoryManagementController controller = new CategoryManagementController(stage);
        dashboardView.getContentPane().getChildren().clear();
        dashboardView.getContentPane().getChildren().add(controller.getRoot());
    }
    
    private void showBilling() {
        BillingController controller = new BillingController(stage);
        dashboardView.getContentPane().getChildren().clear();
        dashboardView.getContentPane().getChildren().add(controller.getRoot());
    }
    
    private void showBillsHistory() {
        BillsHistoryController controller = new BillsHistoryController(stage);
        dashboardView.getContentPane().getChildren().clear();
        dashboardView.getContentPane().getChildren().add(controller.getRoot());
    }
    
    private void showSalesAnalytics() {
        SalesAnalyticsController controller = new SalesAnalyticsController(stage);
        dashboardView.getContentPane().getChildren().clear();
        dashboardView.getContentPane().getChildren().add(controller.getRoot());
    }
    
    private void showSettings() {
        SettingsController controller = new SettingsController();
        dashboardView.getContentPane().getChildren().clear();
        dashboardView.getContentPane().getChildren().add(controller.getView());
    }
    
    private void showWelcomeMessage() {
        Label welcomeLabel = new Label(LocaleManager.getString("app.welcome"));
        welcomeLabel.getStyleClass().add("label-title");
        dashboardView.getContentPane().getChildren().clear();
        dashboardView.getContentPane().getChildren().add(welcomeLabel);
    }
    
    private void showPlaceholder(String section) {
        Label placeholder = new Label(section + " - Coming Soon");
        placeholder.getStyleClass().add("label-headline");
        dashboardView.getContentPane().getChildren().clear();
        dashboardView.getContentPane().getChildren().add(placeholder);
    }
}
