package org.chaos.office.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.chaos.office.util.CurrencyFormatter;
import org.chaos.office.util.LocaleManager;

import java.time.LocalDate;

/**
 * View for sales analytics with charts and statistics.
 */
public class SalesAnalyticsView extends BorderPane {
    
    private final DatePicker startDatePicker;
    private final DatePicker endDatePicker;
    private final Button refreshButton;
    private final LineChart<String, Number> salesChart;
    private final Label totalRevenueLabel;
    private final Label transactionCountLabel;
    
    public SalesAnalyticsView() {
        // Top: Title and date range selector
        Label titleLabel = new Label(LocaleManager.getString("analytics.title"));
        titleLabel.getStyleClass().add("title-label");
        
        Label startDateLabel = new Label(LocaleManager.getString("bills.filter.start"));
        startDatePicker = new DatePicker();
        startDatePicker.setValue(LocalDate.now().minusMonths(1));
        
        Label endDateLabel = new Label(LocaleManager.getString("bills.filter.end"));
        endDatePicker = new DatePicker();
        endDatePicker.setValue(LocalDate.now());
        
        refreshButton = new Button(LocaleManager.getString("common.refresh"));
        refreshButton.getStyleClass().add("primary-button");
        
        HBox dateBox = new HBox(10, startDateLabel, startDatePicker, endDateLabel, endDatePicker, refreshButton);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        dateBox.setPadding(new Insets(10));
        
        VBox topBox = new VBox(10, titleLabel, dateBox);
        topBox.setPadding(new Insets(20));
        setTop(topBox);
        
        // Center: Sales chart
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(LocaleManager.getString("analytics.chart.date"));
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(LocaleManager.getString("analytics.chart.sales") + " (" + CurrencyFormatter.getSymbol() + ")");
        
        salesChart = new LineChart<>(xAxis, yAxis);
        salesChart.setTitle(LocaleManager.getString("analytics.chart.title"));
        salesChart.setLegendVisible(false);
        
        VBox centerBox = new VBox(salesChart);
        centerBox.setPadding(new Insets(20));
        setCenter(centerBox);
        
        // Right: Summary statistics
        VBox statsBox = new VBox(20);
        statsBox.setPadding(new Insets(20));
        statsBox.setPrefWidth(250);
        statsBox.setAlignment(Pos.TOP_CENTER);
        
        Label statsTitle = new Label(LocaleManager.getString("analytics.summary"));
        statsTitle.getStyleClass().add("label-subtitle");
        
        totalRevenueLabel = new Label(LocaleManager.getString("analytics.revenue") + ": " + CurrencyFormatter.format(0));
        totalRevenueLabel.getStyleClass().add("label-headline");
        totalRevenueLabel.setWrapText(true);
        
        transactionCountLabel = new Label(LocaleManager.getString("analytics.transactions") + ": 0");
        transactionCountLabel.getStyleClass().add("label-headline");
        transactionCountLabel.setWrapText(true);
        
        Separator separator = new Separator();
        
        statsBox.getChildren().addAll(
            statsTitle,
            separator,
            totalRevenueLabel,
            transactionCountLabel
        );
        
        setRight(statsBox);
        
        // Apply styling
        getStyleClass().add("content-pane");
    }
    
    public DatePicker getStartDatePicker() {
        return startDatePicker;
    }
    
    public DatePicker getEndDatePicker() {
        return endDatePicker;
    }
    
    public Button getRefreshButton() {
        return refreshButton;
    }
    
    public LineChart<String, Number> getSalesChart() {
        return salesChart;
    }
    
    public Label getTotalRevenueLabel() {
        return totalRevenueLabel;
    }
    
    public Label getTransactionCountLabel() {
        return transactionCountLabel;
    }
    
    public void updateStatistics(double totalRevenue, int transactionCount) {
        totalRevenueLabel.setText(String.format("%s: %s", 
            LocaleManager.getString("analytics.revenue"), CurrencyFormatter.format((float) totalRevenue)));
        transactionCountLabel.setText(String.format("%s: %d", 
            LocaleManager.getString("analytics.transactions"), transactionCount));
    }
}
