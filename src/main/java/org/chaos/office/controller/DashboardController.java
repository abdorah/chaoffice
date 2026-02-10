package org.chaos.office.controller;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.chaos.office.util.SessionManager;
import org.chaos.office.util.ThemeManager;
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
        
        // Apply current theme
        ThemeManager.applyCurrentTheme(this);
        
        setupEventHandlers();
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
        dashboardView.getReportsButton().setOnAction(e -> showReports());
        dashboardView.getSettingsButton().setOnAction(e -> showSettings());
        
        // Show Parts Inventory by default
        showPartsInventory();
    }
    
    private void handleLogout() {
        SessionManager.getInstance().clearSession();
        SignInController loginController = new SignInController(stage);
        stage.setScene(loginController);
        
        // Apply theme to login scene
        ThemeManager.applyCurrentTheme(loginController);
    }
    
    private void showPartsInventory() {
        PartsInventoryController controller = new PartsInventoryController();
        dashboardView.getContentPane().getChildren().clear();
        dashboardView.getContentPane().getChildren().add(controller.getView());
    }
    
    private void showCategories() {
        CategoryManagementController controller = new CategoryManagementController(stage);
        dashboardView.getContentPane().getChildren().clear();
        dashboardView.getContentPane().getChildren().add(controller.getView());
    }
    
    private void showBilling() {
        BillingController controller = new BillingController(stage);
        dashboardView.getContentPane().getChildren().clear();
        dashboardView.getContentPane().getChildren().add(controller.getView());
    }
    
    private void showBillsHistory() {
        BillsHistoryController controller = new BillsHistoryController(stage);
        dashboardView.getContentPane().getChildren().clear();
        dashboardView.getContentPane().getChildren().add(controller.getView());
    }
    
    private void showSalesAnalytics() {
        SalesAnalyticsController controller = new SalesAnalyticsController(stage);
        dashboardView.getContentPane().getChildren().clear();
        dashboardView.getContentPane().getChildren().add(controller.getView());
    }
    
    private void showReports() {
        ReportsController controller = new ReportsController(stage);
        dashboardView.getContentPane().getChildren().clear();
        dashboardView.getContentPane().getChildren().add(controller.getView());
    }
    
    private void showSettings() {
        SettingsController controller = new SettingsController(stage);
        dashboardView.getContentPane().getChildren().clear();
        dashboardView.getContentPane().getChildren().add(controller.getView());
    }
    
    public void refreshUI() {
        // Refresh the dashboard view with updated language
        dashboardView.refreshLanguage();
        
        // Refresh the current content view
        // Re-trigger the current view to reload with new language
        showPartsInventory();
    }
}
