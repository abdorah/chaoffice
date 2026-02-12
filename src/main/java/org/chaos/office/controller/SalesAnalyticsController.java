package org.chaos.office.controller;

import javafx.scene.Parent;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import org.chaos.office.service.SalesService;
import org.chaos.office.util.AlertHelper;
import org.chaos.office.util.LocaleManager;
import org.chaos.office.view.SalesAnalyticsView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * Controller for sales analytics.
 */
public class SalesAnalyticsController {
    
    private static final Logger logger = LoggerFactory.getLogger(SalesAnalyticsController.class);
    private final SalesAnalyticsView view;
    private final SalesService salesService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd");
    
    public SalesAnalyticsController(Stage stage) {
        this.view = new SalesAnalyticsView();
        this.salesService = new SalesService();
        
        // Load initial data
        loadSalesData();
        
        // Set up event handlers
        view.getRefreshButton().setOnAction(e -> loadSalesData());
    }
    
    public Parent getView() {
        return view;
    }
    
    /**
     * Loads sales data and updates chart and statistics.
     */
    private void loadSalesData() {
        LocalDate startDate = view.getStartDatePicker().getValue();
        LocalDate endDate = view.getEndDatePicker().getValue();
        
        if (startDate == null || endDate == null) {
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                "Please select both start and end dates"
            );
            return;
        }
        
        if (startDate.isAfter(endDate)) {
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                "Start date must be before end date"
            );
            return;
        }
        
        try {
            // Get sales data
            Map<LocalDate, Double> salesByDate = salesService.getSalesByDate(startDate, endDate);
            double totalRevenue = salesService.getTotalRevenue(startDate, endDate);
            int transactionCount = salesService.getTransactionCount(startDate, endDate);
            
            // Update chart
            updateChart(salesByDate);
            
            // Update statistics
            view.updateStatistics(totalRevenue, transactionCount);
            
            logger.info("Loaded sales data: {} transactions, ${} revenue", transactionCount, totalRevenue);
        } catch (Exception e) {
            logger.error("Error loading sales data", e);
            AlertHelper.showError(
                LocaleManager.getString("error.title"),
                LocaleManager.getString("error.load")
            );
        }
    }
    
    /**
     * Updates the sales chart with data.
     */
    private void updateChart(Map<LocalDate, Double> salesByDate) {
        view.getSalesChart().getData().clear();
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(LocaleManager.getString("analytics.chart.sales"));
        
        // Sort by date and add to chart
        TreeMap<LocalDate, Double> sortedSales = new TreeMap<>(salesByDate);
        
        for (Map.Entry<LocalDate, Double> entry : sortedSales.entrySet()) {
            String dateStr = entry.getKey().format(DATE_FORMATTER);
            series.getData().add(new XYChart.Data<>(dateStr, entry.getValue()));
        }
        
        view.getSalesChart().getData().add(series);
        
        logger.info("Updated chart with {} data points", sortedSales.size());
    }
}
