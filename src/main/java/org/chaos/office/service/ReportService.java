package org.chaos.office.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.chaos.office.model.Bill;
import org.chaos.office.model.Command;
import org.chaos.office.model.Part;
import org.chaos.office.util.LocaleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Locale;

/**
 * ReportService handles PDF report generation.
 * 
 * <p>This service is responsible for:
 * <ul>
 *   <li>Generating bill PDFs</li>
 *   <li>Formatting reports professionally</li>
 * </ul>
 * 
 * <p>Requirements: 8.2, 8.3
 */
public class ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    private final PartService partService;
    
    public ReportService() {
        this.partService = new PartService();
    }
    
    /**
     * Generates a PDF report for a bill.
     * 
     * @param bill the bill to generate a report for
     * @param commands the list of commands (line items) in the bill
     * @param outputFile the file to write the PDF to
     */
    public void generateBillPDF(Bill bill, List<Command> commands, File outputFile) {
        try {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputFile));
            
            document.open();
            
            // Get fonts that support Unicode (including Arabic)
            Font titleFont = getFontForLocale(18, Font.BOLD);
            Font labelFont = getFontForLocale(12, Font.BOLD);
            Font valueFont = getFontForLocale(12, Font.NORMAL);
            
            // Check if we're using RTL language
            boolean isRTL = isRTLLocale();
            if (isRTL) {
                writer.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
            }
            
            // Title
            Paragraph title = new Paragraph(LocaleManager.getString("bill.receipt.title"), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Bill information
            
            document.add(new Paragraph(LocaleManager.getString("bill.receipt.id") + ": " + bill.getId(), valueFont));
            document.add(new Paragraph(LocaleManager.getString("bill.receipt.date") + ": " + bill.getDate().toString(), valueFont));
            
            // Client name and phone are optional - keep their space even if empty
            String clientName = bill.getClientName();
            if (clientName == null || clientName.trim().isEmpty()) {
                document.add(new Paragraph(LocaleManager.getString("bill.receipt.client.name") + ": ", valueFont));
            } else {
                document.add(new Paragraph(LocaleManager.getString("bill.receipt.client.name") + ": " + clientName, valueFont));
            }
            
            String clientPhone = bill.getClientPhone();
            if (clientPhone == null || clientPhone.trim().isEmpty()) {
                document.add(new Paragraph(LocaleManager.getString("bill.receipt.client.phone") + ": ", valueFont));
            } else {
                document.add(new Paragraph(LocaleManager.getString("bill.receipt.client.phone") + ": " + clientPhone, valueFont));
            }
            
            document.add(new Paragraph(" "));
            
            // Items table
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setSpacingAfter(10);
            if (isRTL) {
                table.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
            }
            
            // Table headers
            Font headerFont = getFontForLocale(12, Font.BOLD);
            Font cellFont = getFontForLocale(12, Font.NORMAL);
            
            addTableHeader(table, LocaleManager.getString("bill.receipt.part.name"), headerFont);
            addTableHeader(table, LocaleManager.getString("bill.receipt.quantity"), headerFont);
            addTableHeader(table, LocaleManager.getString("bill.receipt.unit.price"), headerFont);
            addTableHeader(table, LocaleManager.getString("bill.receipt.subtotal"), headerFont);
            addTableHeader(table, LocaleManager.getString("bill.receipt.total"), headerFont);
            
            // Table rows
            for (Command command : commands) {
                Part part = partService.getPartById(command.getPartId()).orElse(null);
                String partName = part != null ? part.getName() : LocaleManager.getString("bill.receipt.unknown.part");
                
                float subtotal = command.getQuantity() * command.getPriceConsidered();
                
                addTableCell(table, partName, cellFont);
                addTableCell(table, String.valueOf(command.getQuantity()), cellFont);
                addTableCell(table, String.format("%.2f", command.getPriceConsidered()), cellFont);
                addTableCell(table, String.format("%.2f", subtotal), cellFont);
                addTableCell(table, String.format("%.2f", subtotal), cellFont);
            }
            
            document.add(table);
            
            // Total
            String totalText = LocaleManager.getString("bill.receipt.total");
            // Don't uppercase for RTL languages as it may break the text
            if (!isRTL) {
                totalText = totalText.toUpperCase();
            }
            Paragraph total = new Paragraph(totalText + ": " + String.format("%.2f", bill.getTotalPrice()), labelFont);
            total.setAlignment(Element.ALIGN_RIGHT);
            total.setSpacingBefore(10);
            document.add(total);
            
            document.close();
            
            logger.info("Generated PDF report for bill ID: {} at {}", bill.getId(), outputFile.getAbsolutePath());
            
        } catch (Exception e) {
            logger.error("Error generating PDF report for bill ID: {}", bill.getId(), e);
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }
    
    /**
     * Gets a font that supports the current locale, including Arabic and other Unicode characters.
     */
    private Font getFontForLocale(int size, int style) {
        try {
            // Try to use a Unicode font that supports Arabic
            BaseFont bf = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            return new Font(bf, size, style);
        } catch (Exception e) {
            logger.warn("Could not load Unicode font, falling back to default", e);
            // Fallback to default font
            return new Font(Font.HELVETICA, size, style);
        }
    }
    
    /**
     * Checks if the current locale uses right-to-left text direction.
     */
    private boolean isRTLLocale() {
        Locale currentLocale = LocaleManager.getCurrentLocale();
        String language = currentLocale.getLanguage();
        // Arabic, Hebrew, Persian, Urdu use RTL
        return "ar".equals(language) || "he".equals(language) || 
               "fa".equals(language) || "ur".equals(language);
    }
    
    private void addTableHeader(PdfPTable table, String text, Font font) {
        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(new java.awt.Color(200, 200, 200));
        header.setBorderWidth(1);
        header.setPhrase(new Phrase(text, font));
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(header);
    }
    
    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }
}
