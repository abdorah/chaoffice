package org.chaos.office.controller;

import com.lowagie.text.DocumentException;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.chaos.office.reports.models.*;
import org.chaos.office.reports.services.ReportService;
import org.chaos.office.util.AlertHelper;
import org.chaos.office.util.EnumLocalizer;
import org.chaos.office.util.LocaleManager;
import org.chaos.office.view.ReportsView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

/**
 * ReportsController - Controller for reports generation and export
 * Requirements: 10.4, 10.5, 6.1, 6.2, 6.3, 6.5, 9.1, 9.2, 9.4, 9.5
 */
public class ReportsController {
    private static final Logger logger = LoggerFactory.getLogger(ReportsController.class);
    
    private final ReportsView view;
    private final Stage stage;
    private final ReportService reportService;
    private ReportData currentReportData;
    
    public ReportsController(Stage stage) {
        this.stage = stage;
        this.view = new ReportsView();
        this.reportService = new ReportService();
        
        initialize();
    }
    
    private void initialize() {
        // Report type change handler
        view.getReportTypeComboBox().setOnAction(e -> onReportTypeChanged());
        
        // Date preset change handler
        view.getDatePresetComboBox().setOnAction(e -> onDatePresetSelected());
        
        // Preview button handler
        view.getPreviewButton().setOnAction(e -> onPreviewClicked());
        
        // Export button handlers
        view.getExportPDFButton().setOnAction(e -> onExportPDFClicked());
        view.getExportCSVButton().setOnAction(e -> onExportCSVClicked());
        view.getExportExcelButton().setOnAction(e -> onExportExcelClicked());
        
        // Parameter change handlers to clear preview
        view.getStartDatePicker().setOnAction(e -> clearPreview());
        view.getEndDatePicker().setOnAction(e -> clearPreview());
        view.getStockThresholdField().textProperty().addListener((obs, old, newVal) -> clearPreview());
    }
    
    private void onReportTypeChanged() {
        String reportType = view.getReportTypeComboBox().getValue();
        String salesReportType = LocaleManager.getString("reports.type.sales");
        String inventoryReportType = LocaleManager.getString("reports.type.inventory");
        
        if (salesReportType.equals(reportType)) {
            view.getDateRangeControls().setVisible(true);
            view.getDateRangeControls().setManaged(true);
            view.getStockThresholdControls().setVisible(false);
            view.getStockThresholdControls().setManaged(false);
        } else if (inventoryReportType.equals(reportType)) {
            view.getDateRangeControls().setVisible(false);
            view.getDateRangeControls().setManaged(false);
            view.getStockThresholdControls().setVisible(true);
            view.getStockThresholdControls().setManaged(true);
        }
        
        clearPreview();
    }
    
    private void onDatePresetSelected() {
        String preset = view.getDatePresetComboBox().getValue();
        LocalDate today = LocalDate.now();
        
        String customPreset = LocaleManager.getString("reports.preset.custom");
        String todayPreset = LocaleManager.getString("reports.preset.today");
        String weekPreset = LocaleManager.getString("reports.preset.week");
        String monthPreset = LocaleManager.getString("reports.preset.month");
        
        if (todayPreset.equals(preset)) {
            view.getStartDatePicker().setValue(today);
            view.getEndDatePicker().setValue(today);
        } else if (weekPreset.equals(preset)) {
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            view.getStartDatePicker().setValue(monday);
            view.getEndDatePicker().setValue(today);
        } else if (monthPreset.equals(preset)) {
            LocalDate firstDay = today.withDayOfMonth(1);
            view.getStartDatePicker().setValue(firstDay);
            view.getEndDatePicker().setValue(today);
        } else if (customPreset.equals(preset)) {
            // User can manually select dates
        }
    }
    
    private void onPreviewClicked() {
        if (!validateInputs()) {
            return;
        }
        
        try {
            String reportType = view.getReportTypeComboBox().getValue();
            String salesReportType = LocaleManager.getString("reports.type.sales");
            String inventoryReportType = LocaleManager.getString("reports.type.inventory");
            
            if (salesReportType.equals(reportType)) {
                LocalDate startDate = view.getStartDatePicker().getValue();
                LocalDate endDate = view.getEndDatePicker().getValue();
                currentReportData = reportService.generateSalesReport(startDate, endDate);
                displaySalesPreview((SalesReportData) currentReportData);
            } else if (inventoryReportType.equals(reportType)) {
                int threshold = Integer.parseInt(view.getStockThresholdField().getText());
                currentReportData = reportService.generateInventoryReport(threshold);
                displayInventoryPreview((InventoryReportData) currentReportData);
            }
            
            // Enable export buttons
            view.getExportPDFButton().setDisable(false);
            view.getExportCSVButton().setDisable(false);
            view.getExportExcelButton().setDisable(false);
            
        } catch (Exception e) {
            logger.error("Error generating report preview", e);
            AlertHelper.showError(LocaleManager.getString("dialog.title.reports.error"), 
                java.text.MessageFormat.format(LocaleManager.getString("error.reports.generate.failed"), e.getMessage()));
        }
    }
    
    private boolean validateInputs() {
        String reportType = view.getReportTypeComboBox().getValue();
        String salesReportType = LocaleManager.getString("reports.type.sales");
        String inventoryReportType = LocaleManager.getString("reports.type.inventory");
        
        if (salesReportType.equals(reportType)) {
            LocalDate startDate = view.getStartDatePicker().getValue();
            LocalDate endDate = view.getEndDatePicker().getValue();
            
            if (startDate == null || endDate == null) {
                AlertHelper.showError(LocaleManager.getString("dialog.title.validation.error"), 
                    LocaleManager.getString("error.reports.date.range.required"));
                return false;
            }
            
            if (startDate.isAfter(endDate)) {
                AlertHelper.showError(LocaleManager.getString("dialog.title.validation.error"), 
                    LocaleManager.getString("error.reports.date.range.invalid"));
                return false;
            }
        } else if (inventoryReportType.equals(reportType)) {
            try {
                int threshold = Integer.parseInt(view.getStockThresholdField().getText());
                if (threshold < 0) {
                    AlertHelper.showError(LocaleManager.getString("dialog.title.validation.error"), 
                        LocaleManager.getString("error.reports.threshold.negative"));
                    return false;
                }
            } catch (NumberFormatException e) {
                AlertHelper.showError(LocaleManager.getString("dialog.title.validation.error"), 
                    LocaleManager.getString("error.reports.threshold.invalid"));
                return false;
            }
        }
        
        return true;
    }
    
    private void displaySalesPreview(SalesReportData data) {
        VBox container = view.getPreviewContainer();
        container.getChildren().clear();
        
        // Summary section
        Label summaryTitle = new Label(LocaleManager.getString("report.summary.metrics"));
        summaryTitle.getStyleClass().add("label-title");
        
        GridPane summaryGrid = new GridPane();
        summaryGrid.setHgap(20);
        summaryGrid.setVgap(10);
        summaryGrid.setPadding(new Insets(10));
        
        addGridRow(summaryGrid, 0, LocaleManager.getString("report.total.revenue") + ":", formatCurrency(data.getTotalRevenue()));
        addGridRow(summaryGrid, 1, LocaleManager.getString("report.number.of.sales") + ":", String.valueOf(data.getSalesCount()));
        addGridRow(summaryGrid, 2, LocaleManager.getString("report.average.sale.value") + ":", formatCurrency(data.getAverageSaleValue()));
        
        // Payment method breakdown
        Label paymentTitle = new Label(LocaleManager.getString("report.payment.method.breakdown"));
        paymentTitle.getStyleClass().add("label-title");
        
        GridPane paymentGrid = new GridPane();
        paymentGrid.setHgap(20);
        paymentGrid.setVgap(10);
        paymentGrid.setPadding(new Insets(10));
        
        int row = 0;
        for (Map.Entry<PaymentMethod, BigDecimal> entry : data.getPaymentMethodBreakdown().entrySet()) {
            addGridRow(paymentGrid, row++, EnumLocalizer.getLocalizedPaymentMethod(entry.getKey()) + ":", formatCurrency(entry.getValue()));
        }
        
        // Discount analysis
        Label discountTitle = new Label(LocaleManager.getString("report.discount.analysis"));
        discountTitle.getStyleClass().add("label-title");
        
        GridPane discountGrid = new GridPane();
        discountGrid.setHgap(20);
        discountGrid.setVgap(10);
        discountGrid.setPadding(new Insets(10));
        
        addGridRow(discountGrid, 0, LocaleManager.getString("report.total.discounts.given") + ":", formatCurrency(data.getTotalDiscounts()));
        addGridRow(discountGrid, 1, LocaleManager.getString("report.average.discount.percentage") + ":", data.getAverageDiscountPercentage() + "%");
        
        // Top selling parts
        Label topPartsTitle = new Label(LocaleManager.getString("report.top.selling.parts"));
        topPartsTitle.getStyleClass().add("label-title");
        
        GridPane topPartsGrid = new GridPane();
        topPartsGrid.setHgap(20);
        topPartsGrid.setVgap(10);
        topPartsGrid.setPadding(new Insets(10));
        
        addGridRow(topPartsGrid, 0, LocaleManager.getString("report.column.part.name"), 
                   LocaleManager.getString("report.quantity.sold"), 
                   LocaleManager.getString("report.column.revenue"));
        int partRow = 1;
        for (TopSellingPart part : data.getTopSellingParts()) {
            addGridRow(topPartsGrid, partRow++, part.getPartName(), 
                      String.valueOf(part.getQuantitySold()), 
                      formatCurrency(part.getRevenue()));
        }
        
        container.getChildren().addAll(
            summaryTitle, summaryGrid,
            new Separator(),
            paymentTitle, paymentGrid,
            new Separator(),
            discountTitle, discountGrid,
            new Separator(),
            topPartsTitle, topPartsGrid
        );
    }
    
    private void displayInventoryPreview(InventoryReportData data) {
        VBox container = view.getPreviewContainer();
        container.getChildren().clear();
        
        // Summary section
        Label summaryTitle = new Label(LocaleManager.getString("report.summary.metrics"));
        summaryTitle.getStyleClass().add("label-title");
        
        GridPane summaryGrid = new GridPane();
        summaryGrid.setHgap(20);
        summaryGrid.setVgap(10);
        summaryGrid.setPadding(new Insets(10));
        
        addGridRow(summaryGrid, 0, LocaleManager.getString("report.total.inventory.value") + ":", formatCurrency(data.getTotalInventoryValue()));
        addGridRow(summaryGrid, 1, LocaleManager.getString("report.low.stock.items") + ":", String.valueOf(data.getLowStockParts().size()));
        addGridRow(summaryGrid, 2, LocaleManager.getString("report.out.of.stock.items") + ":", String.valueOf(data.getOutOfStockParts().size()));
        
        // Inventory details (show first 20 items)
        Label detailsTitle = new Label(LocaleManager.getString("report.inventory.details") + " (First 20 items)");
        detailsTitle.getStyleClass().add("label-title");
        
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(15);
        detailsGrid.setVgap(8);
        detailsGrid.setPadding(new Insets(10));
        
        addGridRow(detailsGrid, 0, LocaleManager.getString("report.column.category"), 
                   LocaleManager.getString("report.column.part.name"), 
                   LocaleManager.getString("report.column.quantity"), 
                   LocaleManager.getString("report.status"));
        
        int row = 1;
        int count = 0;
        for (Map.Entry<String, List<PartInventoryItem>> entry : data.getPartsByCategory().entrySet()) {
            for (PartInventoryItem item : entry.getValue()) {
                if (count++ >= 20) break;
                addGridRow(detailsGrid, row++, entry.getKey(), item.getPartName(), 
                          String.valueOf(item.getQuantity()), EnumLocalizer.getLocalizedStockStatus(item.getStatus()));
            }
            if (count >= 20) break;
        }
        
        Label noteLabel = new Label("Note: Full details available in exported files");
        noteLabel.setStyle("-fx-font-style: italic; -fx-text-fill: gray;");
        
        container.getChildren().addAll(
            summaryTitle, summaryGrid,
            new Separator(),
            detailsTitle, detailsGrid,
            noteLabel
        );
    }
    
    private void addGridRow(GridPane grid, int row, String label, String value) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("label-bold");
        Label valueNode = new Label(value);
        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }
    
    private void addGridRow(GridPane grid, int row, String col1, String col2, String col3) {
        grid.add(new Label(col1), 0, row);
        grid.add(new Label(col2), 1, row);
        grid.add(new Label(col3), 2, row);
    }
    
    private void addGridRow(GridPane grid, int row, String col1, String col2, String col3, String col4) {
        grid.add(new Label(col1), 0, row);
        grid.add(new Label(col2), 1, row);
        grid.add(new Label(col3), 2, row);
        grid.add(new Label(col4), 3, row);
    }
    
    private void clearPreview() {
        view.getPreviewContainer().getChildren().clear();
        view.getExportPDFButton().setDisable(true);
        view.getExportCSVButton().setDisable(true);
        view.getExportExcelButton().setDisable(true);
        currentReportData = null;
    }
    
    private void onExportPDFClicked() {
        if (currentReportData == null) {
            AlertHelper.showError(LocaleManager.getString("dialog.title.reports.error"), 
                LocaleManager.getString("error.reports.no.preview"));
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Report");
        fileChooser.setInitialFileName(reportService.generatePDFFilename(currentReportData.getReportTitle()));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                reportService.exportToPDF(currentReportData, file.getAbsolutePath());
                AlertHelper.showInfo(
                    LocaleManager.getString("success.title"), 
                    LocaleManager.getString("reports.export.success") + ":\n" + file.getAbsolutePath()
                );
            } catch (IOException | DocumentException e) {
                logger.error("Error exporting PDF", e);
                AlertHelper.showError(
                    LocaleManager.getString("error.title"), 
                    LocaleManager.getString("reports.export.error") + ": " + e.getMessage()
                );
            }
        }
    }
    
    private void onExportCSVClicked() {
        if (currentReportData == null) {
            AlertHelper.showError(
                LocaleManager.getString("error.title"), 
                LocaleManager.getString("reports.no.report.error")
            );
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save CSV Report");
        fileChooser.setInitialFileName(reportService.generateCSVFilename(currentReportData.getReportTitle()));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                reportService.exportToCSV(currentReportData, file.getAbsolutePath());
                AlertHelper.showInfo(
                    LocaleManager.getString("success.title"), 
                    LocaleManager.getString("reports.export.success") + ":\n" + file.getAbsolutePath()
                );
            } catch (IOException e) {
                logger.error("Error exporting CSV", e);
                AlertHelper.showError(
                    LocaleManager.getString("error.title"), 
                    LocaleManager.getString("reports.export.error") + ": " + e.getMessage()
                );
            }
        }
    }
    
    private void onExportExcelClicked() {
        if (currentReportData == null) {
            AlertHelper.showError(
                LocaleManager.getString("error.title"), 
                LocaleManager.getString("reports.no.report.error")
            );
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Excel Report");
        fileChooser.setInitialFileName(reportService.generateExcelFilename(currentReportData.getReportTitle()));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                reportService.exportToExcel(currentReportData, file.getAbsolutePath());
                AlertHelper.showInfo(
                    LocaleManager.getString("success.title"), 
                    LocaleManager.getString("reports.export.success") + ":\n" + file.getAbsolutePath()
                );
            } catch (IOException e) {
                logger.error("Error exporting Excel", e);
                AlertHelper.showError(
                    LocaleManager.getString("error.title"), 
                    LocaleManager.getString("reports.export.error") + ": " + e.getMessage()
                );
            }
        }
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "$0.00";
        }
        return String.format("$%,.2f", amount);
    }
    
    public ReportsView getView() {
        return view;
    }
}
