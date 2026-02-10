package org.chaos.office.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.chaos.office.model.Bill;
import org.chaos.office.model.Command;
import org.chaos.office.model.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

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
            PdfWriter.getInstance(document, new FileOutputStream(outputFile));
            
            document.open();
            
            // Title
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("BILL RECEIPT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Bill information
            Font labelFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font valueFont = new Font(Font.HELVETICA, 12, Font.NORMAL);
            
            document.add(new Paragraph("Bill ID: " + bill.getId(), valueFont));
            document.add(new Paragraph("Date: " + bill.getDate().toString(), valueFont));
            
            // Client name and phone are optional - keep their space even if empty
            String clientName = bill.getClientName();
            if (clientName == null || clientName.trim().isEmpty()) {
                document.add(new Paragraph("Client Name: ", valueFont));
            } else {
                document.add(new Paragraph("Client Name: " + clientName, valueFont));
            }
            
            String clientPhone = bill.getClientPhone();
            if (clientPhone == null || clientPhone.trim().isEmpty()) {
                document.add(new Paragraph("Client Phone: ", valueFont));
            } else {
                document.add(new Paragraph("Client Phone: " + clientPhone, valueFont));
            }
            
            document.add(new Paragraph(" "));
            
            // Items table
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setSpacingAfter(10);
            
            // Table headers
            addTableHeader(table, "Part Name");
            addTableHeader(table, "Quantity");
            addTableHeader(table, "Unit Price");
            addTableHeader(table, "Subtotal");
            addTableHeader(table, "Total");
            
            // Table rows
            for (Command command : commands) {
                Part part = partService.getPartById(command.getPartId()).orElse(null);
                String partName = part != null ? part.getName() : "Unknown Part";
                
                float subtotal = command.getQuantity() * command.getPriceConsidered();
                
                addTableCell(table, partName);
                addTableCell(table, String.valueOf(command.getQuantity()));
                addTableCell(table, String.format("%.2f", command.getPriceConsidered()));
                addTableCell(table, String.format("%.2f", subtotal));
                addTableCell(table, String.format("%.2f", subtotal));
            }
            
            document.add(table);
            
            // Total
            Paragraph total = new Paragraph("TOTAL: " + String.format("%.2f", bill.getTotalPrice()), labelFont);
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
    
    private void addTableHeader(PdfPTable table, String text) {
        Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(new java.awt.Color(200, 200, 200));
        header.setBorderWidth(1);
        header.setPhrase(new Phrase(text, headerFont));
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(header);
    }
    
    private void addTableCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }
}
