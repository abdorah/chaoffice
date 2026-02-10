package org.chaos.office.reports.services;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.chaos.office.reports.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Exports report data to Excel (.xlsx) format with professional formatting.
 * Creates formatted Excel workbooks with styled headers, tables, and data.
 */
public class ExcelExporter {
    private static final Logger logger = LoggerFactory.getLogger(ExcelExporter.class);
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Exports report data to an Excel file.
     * 
     * @param reportData The report data to export
     * @param filePath The destination file path
     * @throws IOException If file writing fails
     */
    public void export(ReportData reportData, String filePath) throws IOException {
        logger.info("Exporting report to Excel: {}", filePath);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            if (reportData instanceof SalesReportData) {
                writeSalesExcel((SalesReportData) reportData, workbook);
            } else if (reportData instanceof InventoryReportData) {
                writeInventoryExcel((InventoryReportData) reportData, workbook);
            }
            
            // Write to file
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
            
            logger.info("Excel export completed successfully: {}", filePath);
        }
    }
    
    /**
     * Writes sales report data to Excel workbook.
     */
    private void writeSalesExcel(SalesReportData data, Workbook workbook) {
        Sheet sheet = workbook.createSheet("Sales Report");
        
        // Create styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        CellStyle normalStyle = createNormalStyle(workbook);
        
        int rowNum = 0;
        
        // Report header
        rowNum = writeReportHeader(sheet, rowNum, "Sales Report", 
            "Date Range: " + data.getStartDate() + " to " + data.getEndDate(),
            "Generated: " + data.getGeneratedAt().format(DISPLAY_FORMATTER),
            titleStyle, normalStyle);
        
        rowNum++; // Blank row
        
        // Summary metrics section
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Summary Metrics");
        titleCell.setCellStyle(headerStyle);
        
        rowNum = writeLabelValueRow(sheet, rowNum, "Total Revenue", data.getTotalRevenue(), normalStyle, currencyStyle);
        rowNum = writeLabelValueRow(sheet, rowNum, "Number of Sales", data.getSalesCount(), normalStyle, normalStyle);
        rowNum = writeLabelValueRow(sheet, rowNum, "Average Sale Value", data.getAverageSaleValue(), normalStyle, currencyStyle);
        
        rowNum++; // Blank row
        
        // Payment method breakdown
        Row paymentTitleRow = sheet.createRow(rowNum++);
        Cell paymentTitleCell = paymentTitleRow.createCell(0);
        paymentTitleCell.setCellValue("Payment Method Breakdown");
        paymentTitleCell.setCellStyle(headerStyle);
        
        Row paymentHeaderRow = sheet.createRow(rowNum++);
        createHeaderCell(paymentHeaderRow, 0, "Payment Method", headerStyle);
        createHeaderCell(paymentHeaderRow, 1, "Total Revenue", headerStyle);
        
        for (Map.Entry<PaymentMethod, BigDecimal> entry : data.getPaymentMethodBreakdown().entrySet()) {
            Row row = sheet.createRow(rowNum++);
            Cell methodCell = row.createCell(0);
            methodCell.setCellValue(entry.getKey().toString());
            methodCell.setCellStyle(normalStyle);
            
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(entry.getValue().doubleValue());
            valueCell.setCellStyle(currencyStyle);
        }
        
        rowNum++; // Blank row
        
        // Discount analysis
        Row discountTitleRow = sheet.createRow(rowNum++);
        Cell discountTitleCell = discountTitleRow.createCell(0);
        discountTitleCell.setCellValue("Discount Analysis");
        discountTitleCell.setCellStyle(headerStyle);
        
        rowNum = writeLabelValueRow(sheet, rowNum, "Total Discounts Given", data.getTotalDiscounts(), normalStyle, currencyStyle);
        rowNum = writeLabelValueRow(sheet, rowNum, "Average Discount Percentage", 
            data.getAverageDiscountPercentage().doubleValue() + "%", normalStyle, normalStyle);
        
        rowNum++; // Blank row
        
        // Top selling parts
        Row topPartsTitleRow = sheet.createRow(rowNum++);
        Cell topPartsTitleCell = topPartsTitleRow.createCell(0);
        topPartsTitleCell.setCellValue("Top Selling Parts");
        topPartsTitleCell.setCellStyle(headerStyle);
        
        Row topPartsHeaderRow = sheet.createRow(rowNum++);
        createHeaderCell(topPartsHeaderRow, 0, "Part Name", headerStyle);
        createHeaderCell(topPartsHeaderRow, 1, "Quantity Sold", headerStyle);
        createHeaderCell(topPartsHeaderRow, 2, "Revenue", headerStyle);
        
        for (TopSellingPart part : data.getTopSellingParts()) {
            Row row = sheet.createRow(rowNum++);
            
            Cell nameCell = row.createCell(0);
            nameCell.setCellValue(part.getPartName());
            nameCell.setCellStyle(normalStyle);
            
            Cell qtyCell = row.createCell(1);
            qtyCell.setCellValue(part.getQuantitySold());
            qtyCell.setCellStyle(normalStyle);
            
            Cell revenueCell = row.createCell(2);
            revenueCell.setCellValue(part.getRevenue().doubleValue());
            revenueCell.setCellStyle(currencyStyle);
        }
        
        // Auto-size columns
        for (int i = 0; i < 3; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    /**
     * Writes inventory report data to Excel workbook.
     */
    private void writeInventoryExcel(InventoryReportData data, Workbook workbook) {
        Sheet sheet = workbook.createSheet("Inventory Report");
        
        // Create styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        CellStyle normalStyle = createNormalStyle(workbook);
        CellStyle lowStockStyle = createLowStockStyle(workbook);
        CellStyle outOfStockStyle = createOutOfStockStyle(workbook);
        
        int rowNum = 0;
        
        // Report header
        rowNum = writeReportHeader(sheet, rowNum, "Inventory Report",
            "Stock Threshold: " + data.getStockThreshold() + " units",
            "Generated: " + data.getGeneratedAt().format(DISPLAY_FORMATTER),
            titleStyle, normalStyle);
        
        rowNum++; // Blank row
        
        // Summary metrics
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Summary Metrics");
        titleCell.setCellStyle(headerStyle);
        
        rowNum = writeLabelValueRow(sheet, rowNum, "Total Inventory Value", data.getTotalInventoryValue(), normalStyle, currencyStyle);
        rowNum = writeLabelValueRow(sheet, rowNum, "Low Stock Items", data.getLowStockParts().size(), normalStyle, normalStyle);
        rowNum = writeLabelValueRow(sheet, rowNum, "Out of Stock Items", data.getOutOfStockParts().size(), normalStyle, normalStyle);
        
        rowNum++; // Blank row
        
        // Inventory details
        Row detailsTitleRow = sheet.createRow(rowNum++);
        Cell detailsTitleCell = detailsTitleRow.createCell(0);
        detailsTitleCell.setCellValue("Inventory Details");
        detailsTitleCell.setCellStyle(headerStyle);
        
        Row detailsHeaderRow = sheet.createRow(rowNum++);
        createHeaderCell(detailsHeaderRow, 0, "Category", headerStyle);
        createHeaderCell(detailsHeaderRow, 1, "Part Name", headerStyle);
        createHeaderCell(detailsHeaderRow, 2, "Quantity", headerStyle);
        createHeaderCell(detailsHeaderRow, 3, "Price", headerStyle);
        createHeaderCell(detailsHeaderRow, 4, "Stock Value", headerStyle);
        createHeaderCell(detailsHeaderRow, 5, "Status", headerStyle);
        
        for (Map.Entry<String, List<PartInventoryItem>> entry : data.getPartsByCategory().entrySet()) {
            for (PartInventoryItem item : entry.getValue()) {
                Row row = sheet.createRow(rowNum++);
                
                // Determine row style based on stock status
                CellStyle rowStyle = normalStyle;
                if (item.getStatus() == StockStatus.OUT_OF_STOCK) {
                    rowStyle = outOfStockStyle;
                } else if (item.getStatus() == StockStatus.LOW_STOCK) {
                    rowStyle = lowStockStyle;
                }
                
                Cell categoryCell = row.createCell(0);
                categoryCell.setCellValue(entry.getKey());
                categoryCell.setCellStyle(rowStyle);
                
                Cell nameCell = row.createCell(1);
                nameCell.setCellValue(item.getPartName());
                nameCell.setCellStyle(rowStyle);
                
                Cell qtyCell = row.createCell(2);
                qtyCell.setCellValue(item.getQuantity());
                qtyCell.setCellStyle(rowStyle);
                
                Cell priceCell = row.createCell(3);
                priceCell.setCellValue(item.getPrice().doubleValue());
                CellStyle priceCellStyle = workbook.createCellStyle();
                priceCellStyle.cloneStyleFrom(currencyStyle);
                if (item.getStatus() != StockStatus.NORMAL) {
                    priceCellStyle.setFillForegroundColor(rowStyle.getFillForegroundColor());
                    priceCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                }
                priceCell.setCellStyle(priceCellStyle);
                
                Cell valueCell = row.createCell(4);
                valueCell.setCellValue(item.getStockValue().doubleValue());
                CellStyle valueCellStyle = workbook.createCellStyle();
                valueCellStyle.cloneStyleFrom(currencyStyle);
                if (item.getStatus() != StockStatus.NORMAL) {
                    valueCellStyle.setFillForegroundColor(rowStyle.getFillForegroundColor());
                    valueCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                }
                valueCell.setCellStyle(valueCellStyle);
                
                Cell statusCell = row.createCell(5);
                statusCell.setCellValue(item.getStatus().toString());
                statusCell.setCellStyle(rowStyle);
            }
        }
        
        // Auto-size columns
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    /**
     * Writes report header section.
     */
    private int writeReportHeader(Sheet sheet, int startRow, String title, String param, String generated,
                                   CellStyle titleStyle, CellStyle normalStyle) {
        Row titleRow = sheet.createRow(startRow++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleStyle);
        
        Row paramRow = sheet.createRow(startRow++);
        Cell paramCell = paramRow.createCell(0);
        paramCell.setCellValue(param);
        paramCell.setCellStyle(normalStyle);
        
        Row genRow = sheet.createRow(startRow++);
        Cell genCell = genRow.createCell(0);
        genCell.setCellValue(generated);
        genCell.setCellStyle(normalStyle);
        
        return startRow;
    }
    
    /**
     * Writes a label-value row.
     */
    private int writeLabelValueRow(Sheet sheet, int rowNum, String label, Object value,
                                    CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowNum);
        
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        
        Cell valueCell = row.createCell(1);
        if (value instanceof BigDecimal) {
            valueCell.setCellValue(((BigDecimal) value).doubleValue());
        } else if (value instanceof Number) {
            valueCell.setCellValue(((Number) value).doubleValue());
        } else {
            valueCell.setCellValue(value.toString());
        }
        valueCell.setCellStyle(valueStyle);
        
        return rowNum + 1;
    }
    
    /**
     * Creates a header cell with style.
     */
    private void createHeaderCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
    
    /**
     * Creates title style (large, bold).
     */
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        return style;
    }
    
    /**
     * Creates header style (bold, gray background).
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
    
    /**
     * Creates currency style ($ format).
     */
    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("$#,##0.00"));
        return style;
    }
    
    /**
     * Creates normal style.
     */
    private CellStyle createNormalStyle(Workbook workbook) {
        return workbook.createCellStyle();
    }
    
    /**
     * Creates low stock style (yellow background).
     */
    private CellStyle createLowStockStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
    
    /**
     * Creates out of stock style (red background).
     */
    private CellStyle createOutOfStockStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
    
    /**
     * Generates a timestamped filename for Excel export.
     * 
     * @param reportTitle The report title
     * @return Filename in format: ReportTitle_YYYYMMDD_HHMMSS.xlsx
     */
    public String generateFilename(String reportTitle) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String sanitizedTitle = reportTitle.replaceAll("[^a-zA-Z0-9]", "_");
        return sanitizedTitle + "_" + timestamp + ".xlsx";
    }
}
