package org.chaos.office.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;

/**
 * ReportsView - Reports generation and export UI
 * Requirements: 10.3, 10.4, 10.5, 10.6, 10.7
 */
public class ReportsView extends VBox {
    
    private final ComboBox<String> reportTypeComboBox;
    private final DatePicker startDatePicker;
    private final DatePicker endDatePicker;
    private final ComboBox<String> datePresetComboBox;
    private final TextField stockThresholdField;
    private final Button previewButton;
    private final Button exportPDFButton;
    private final Button exportCSVButton;
    private final Button exportExcelButton;
    private final ScrollPane previewScrollPane;
    private final VBox previewContainer;
    private final VBox dateRangeControls;
    private final VBox stockThresholdControls;
    
    public ReportsView() {
        setSpacing(16);
        setPadding(new Insets(16));
        
        // Title
        Label titleLabel = new Label("Reports");
        titleLabel.getStyleClass().add("label-headline");
        
        // Report type selection
        HBox reportTypeBox = new HBox(10);
        reportTypeBox.setAlignment(Pos.CENTER_LEFT);
        Label reportTypeLabel = new Label("Report Type:");
        reportTypeComboBox = new ComboBox<>();
        reportTypeComboBox.getItems().addAll("Sales Report", "Inventory Report");
        reportTypeComboBox.setValue("Sales Report");
        reportTypeComboBox.setPrefWidth(200);
        reportTypeBox.getChildren().addAll(reportTypeLabel, reportTypeComboBox);
        
        // Date range controls (for Sales Report)
        dateRangeControls = new VBox(10);
        dateRangeControls.setPadding(new Insets(10));
        dateRangeControls.getStyleClass().add("card");
        
        Label dateRangeLabel = new Label("Date Range:");
        dateRangeLabel.getStyleClass().add("label-title");
        
        HBox datePresetBox = new HBox(10);
        datePresetBox.setAlignment(Pos.CENTER_LEFT);
        Label presetLabel = new Label("Preset:");
        datePresetComboBox = new ComboBox<>();
        datePresetComboBox.getItems().addAll("Custom", "Today", "This Week", "This Month");
        datePresetComboBox.setValue("This Month");
        datePresetComboBox.setPrefWidth(150);
        datePresetBox.getChildren().addAll(presetLabel, datePresetComboBox);
        
        HBox datePickersBox = new HBox(10);
        datePickersBox.setAlignment(Pos.CENTER_LEFT);
        Label startLabel = new Label("Start Date:");
        startDatePicker = new DatePicker(LocalDate.now().withDayOfMonth(1));
        startDatePicker.setPrefWidth(150);
        Label endLabel = new Label("End Date:");
        endDatePicker = new DatePicker(LocalDate.now());
        endDatePicker.setPrefWidth(150);
        datePickersBox.getChildren().addAll(startLabel, startDatePicker, endLabel, endDatePicker);
        
        dateRangeControls.getChildren().addAll(dateRangeLabel, datePresetBox, datePickersBox);
        
        // Stock threshold controls (for Inventory Report)
        stockThresholdControls = new VBox(10);
        stockThresholdControls.setPadding(new Insets(10));
        stockThresholdControls.getStyleClass().add("card");
        stockThresholdControls.setVisible(false);
        stockThresholdControls.setManaged(false);
        
        Label thresholdLabel = new Label("Stock Threshold:");
        thresholdLabel.getStyleClass().add("label-title");
        
        HBox thresholdBox = new HBox(10);
        thresholdBox.setAlignment(Pos.CENTER_LEFT);
        Label thresholdDescLabel = new Label("Low stock alert threshold (units):");
        stockThresholdField = new TextField("10");
        stockThresholdField.setPrefWidth(100);
        thresholdBox.getChildren().addAll(thresholdDescLabel, stockThresholdField);
        
        stockThresholdControls.getChildren().addAll(thresholdLabel, thresholdBox);
        
        // Action buttons
        HBox actionButtonsBox = new HBox(10);
        actionButtonsBox.setAlignment(Pos.CENTER_LEFT);
        
        previewButton = new Button("Generate Preview");
        previewButton.getStyleClass().add("button-primary");
        
        exportPDFButton = new Button("Export PDF");
        exportPDFButton.setDisable(true);
        
        exportCSVButton = new Button("Export CSV");
        exportCSVButton.setDisable(true);
        
        exportExcelButton = new Button("Export Excel");
        exportExcelButton.setDisable(true);
        
        actionButtonsBox.getChildren().addAll(previewButton, exportPDFButton, exportCSVButton, exportExcelButton);
        
        // Preview container
        Label previewLabel = new Label("Preview:");
        previewLabel.getStyleClass().add("label-title");
        
        previewContainer = new VBox(10);
        previewContainer.setPadding(new Insets(10));
        previewContainer.getStyleClass().add("card");
        previewContainer.setMinHeight(300);
        
        previewScrollPane = new ScrollPane(previewContainer);
        previewScrollPane.setFitToWidth(true);
        previewScrollPane.setPrefHeight(400);
        VBox.setVgrow(previewScrollPane, Priority.ALWAYS);
        
        getChildren().addAll(
            titleLabel,
            reportTypeBox,
            dateRangeControls,
            stockThresholdControls,
            actionButtonsBox,
            previewLabel,
            previewScrollPane
        );
    }
    
    public ComboBox<String> getReportTypeComboBox() { return reportTypeComboBox; }
    public DatePicker getStartDatePicker() { return startDatePicker; }
    public DatePicker getEndDatePicker() { return endDatePicker; }
    public ComboBox<String> getDatePresetComboBox() { return datePresetComboBox; }
    public TextField getStockThresholdField() { return stockThresholdField; }
    public Button getPreviewButton() { return previewButton; }
    public Button getExportPDFButton() { return exportPDFButton; }
    public Button getExportCSVButton() { return exportCSVButton; }
    public Button getExportExcelButton() { return exportExcelButton; }
    public VBox getPreviewContainer() { return previewContainer; }
    public VBox getDateRangeControls() { return dateRangeControls; }
    public VBox getStockThresholdControls() { return stockThresholdControls; }
}
