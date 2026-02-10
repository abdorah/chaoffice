package org.chaos.office.service;

import org.chaos.office.model.Bill;
import org.chaos.office.model.Command;
import org.chaos.office.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * BillService handles billing operations including bill creation, retrieval, and inventory management.
 * 
 * <p>This service is responsible for:
 * <ul>
 *   <li>Managing bills (create, read)</li>
 *   <li>Managing commands (bill line items)</li>
 *   <li>Validating inventory availability</li>
 *   <li>Reducing inventory quantities on sale completion</li>
 *   <li>Filtering bills by date range</li>
 * </ul>
 * 
 * <p>Requirements: 6.3, 6.6, 6.7, 6.9, 7.2
 */
public class BillService {
    private static final Logger logger = LoggerFactory.getLogger(BillService.class);
    
    /**
     * Retrieves all bills from the database.
     * 
     * @return List of all bills
     */
    public List<Bill> getAllBills() {
        List<Bill> bills = new ArrayList<>();
        String sql = "SELECT id, totalprice, clientname, clientphone, date, subtotal, discount_type, discount_value, payment_method FROM bills";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Bill bill = mapResultSetToBill(rs);
                bills.add(bill);
            }
            
            logger.info("Retrieved {} bills from database", bills.size());
        } catch (SQLException e) {
            logger.error("Error retrieving all bills", e);
        }
        
        return bills;
    }
    
    /**
     * Retrieves a bill by its ID.
     * 
     * @param id the bill ID
     * @return Optional containing the bill if found, empty otherwise
     */
    public Optional<Bill> getBillById(int id) {
        String sql = "SELECT id, totalprice, clientname, clientphone, date, subtotal, discount_type, discount_value, payment_method FROM bills WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Bill bill = mapResultSetToBill(rs);
                    logger.info("Retrieved bill with ID: {}", id);
                    return Optional.of(bill);
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving bill by ID: {}", id, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Saves a new bill with its commands to the database.
     * This operation is transactional - if any part fails, all changes are rolled back.
     * Validates inventory availability and reduces quantities on success.
     * 
     * @param bill the bill to save
     * @param commands the list of commands (line items) for the bill
     * @throws IllegalArgumentException if inventory is insufficient for any part or if discount/payment method values are invalid
     * @throws RuntimeException if the save operation fails
     */
    public void saveBill(Bill bill, List<Command> commands) {
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);
            
            // Validate discount and payment method values before save
            validateDiscountAndPaymentMethod(bill);
            
            // Validate inventory availability for all parts
            for (Command command : commands) {
                if (!hasInventory(conn, command.getPartId(), command.getQuantity())) {
                    throw new IllegalArgumentException(
                        "Insufficient inventory for part ID: " + command.getPartId());
                }
            }
            
            // Insert the bill with new POS properties
            String billSql = "INSERT INTO bills (totalprice, clientname, clientphone, date, subtotal, discount_type, discount_value, payment_method) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            int billId;
            
            try (PreparedStatement stmt = conn.prepareStatement(billSql)) {
                stmt.setFloat(1, bill.getTotalPrice());
                stmt.setString(2, bill.getClientName());
                stmt.setString(3, bill.getClientPhone());
                stmt.setString(4, bill.getDate().toString());
                stmt.setFloat(5, bill.getSubtotal());
                stmt.setString(6, bill.getDiscountType());
                stmt.setFloat(7, bill.getDiscountValue());
                stmt.setString(8, bill.getPaymentMethod());
                stmt.executeUpdate();
                
                try (PreparedStatement idStmt = conn.prepareStatement("SELECT last_insert_rowid()");
                     ResultSet rs = idStmt.executeQuery()) {
                    rs.next();
                    billId = rs.getInt(1);
                    bill.setId(billId);
                }
            }
            
            // Insert commands and reduce inventory
            String commandSql = "INSERT INTO commands (billid, partid, quantity, priceconsidered) VALUES (?, ?, ?, ?)";
            String updateInventorySql = "UPDATE parts SET quantity = quantity - ? WHERE id = ?";
            
            try (PreparedStatement commandStmt = conn.prepareStatement(commandSql);
                 PreparedStatement inventoryStmt = conn.prepareStatement(updateInventorySql)) {
                
                for (Command command : commands) {
                    // Insert command
                    commandStmt.setInt(1, billId);
                    commandStmt.setInt(2, command.getPartId());
                    commandStmt.setInt(3, command.getQuantity());
                    commandStmt.setFloat(4, command.getPriceConsidered());
                    commandStmt.executeUpdate();
                    
                    // Reduce inventory
                    inventoryStmt.setInt(1, command.getQuantity());
                    inventoryStmt.setInt(2, command.getPartId());
                    inventoryStmt.executeUpdate();
                }
            }
            
            conn.commit();
            logger.info("Saved bill with ID: {} and {} commands", billId, commands.size());
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.warn("Transaction rolled back due to error");
                } catch (SQLException rollbackEx) {
                    logger.error("Error rolling back transaction", rollbackEx);
                }
            }
            logger.error("Error saving bill", e);
            throw new RuntimeException("Failed to save bill", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.error("Error resetting auto-commit", e);
                }
            }
        }
    }
    
    /**
     * Validates discount and payment method values before saving.
     * 
     * @param bill the bill to validate
     * @throws IllegalArgumentException if discount or payment method values are invalid
     */
    private void validateDiscountAndPaymentMethod(Bill bill) {
        // Validate discount_type
        String discountType = bill.getDiscountType();
        if (!"none".equals(discountType) && !"percentage".equals(discountType) && !"fixed".equals(discountType)) {
            throw new IllegalArgumentException("Invalid discount_type: " + discountType + 
                ". Must be 'none', 'percentage', or 'fixed'.");
        }
        
        // Validate discount_value based on type
        float discountValue = bill.getDiscountValue();
        if ("percentage".equals(discountType)) {
            if (discountValue < 0 || discountValue > 100) {
                throw new IllegalArgumentException("Percentage discount must be between 0 and 100. Got: " + discountValue);
            }
        } else if ("fixed".equals(discountType)) {
            if (discountValue < 0) {
                throw new IllegalArgumentException("Fixed discount cannot be negative. Got: " + discountValue);
            }
            if (discountValue > bill.getSubtotal()) {
                throw new IllegalArgumentException("Fixed discount (" + discountValue + 
                    ") cannot exceed subtotal (" + bill.getSubtotal() + ")");
            }
        }
        
        // Validate payment_method
        String paymentMethod = bill.getPaymentMethod();
        if (!"cash".equals(paymentMethod) && !"card".equals(paymentMethod) && !"check".equals(paymentMethod)) {
            throw new IllegalArgumentException("Invalid payment_method: " + paymentMethod + 
                ". Must be 'cash', 'card', or 'check'.");
        }
    }
    
    /**
     * Retrieves all commands for a specific bill.
     * 
     * @param billId the bill ID
     * @return List of commands for the bill
     */
    public List<Command> getCommandsForBill(int billId) {
        List<Command> commands = new ArrayList<>();
        String sql = "SELECT billid, partid, quantity, priceconsidered FROM commands WHERE billid = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, billId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Command command = mapResultSetToCommand(rs);
                    commands.add(command);
                }
            }
            
            logger.info("Retrieved {} commands for bill ID: {}", commands.size(), billId);
        } catch (SQLException e) {
            logger.error("Error retrieving commands for bill ID: {}", billId, e);
        }
        
        return commands;
    }
    
    /**
     * Filters bills by date range (inclusive).
     * 
     * @param start the start date (inclusive)
     * @param end the end date (inclusive)
     * @return List of bills within the date range
     */
    public List<Bill> filterByDateRange(LocalDate start, LocalDate end) {
        List<Bill> bills = new ArrayList<>();
        String sql = "SELECT id, totalprice, clientname, clientphone, date, subtotal, discount_type, discount_value, payment_method FROM bills WHERE date >= ? AND date <= ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, start.toString());
            stmt.setString(2, end.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Bill bill = mapResultSetToBill(rs);
                    bills.add(bill);
                }
            }
            
            logger.info("Retrieved {} bills between {} and {}", bills.size(), start, end);
        } catch (SQLException e) {
            logger.error("Error filtering bills by date range", e);
        }
        
        return bills;
    }
    
    /**
     * Checks if sufficient inventory is available for a part.
     * 
     * @param conn the database connection
     * @param partId the part ID
     * @param requestedQuantity the requested quantity
     * @return true if sufficient inventory is available, false otherwise
     * @throws SQLException if a database error occurs
     */
    private boolean hasInventory(Connection conn, int partId, int requestedQuantity) throws SQLException {
        String sql = "SELECT quantity FROM parts WHERE id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, partId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int availableQuantity = rs.getInt("quantity");
                    return availableQuantity >= requestedQuantity;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Maps a ResultSet row to a Bill object.
     * Handles NULL values gracefully for backwards compatibility with existing bills.
     * 
     * @param rs the ResultSet
     * @return the Bill object
     * @throws SQLException if a database access error occurs
     */
    private Bill mapResultSetToBill(ResultSet rs) throws SQLException {
        Bill bill = new Bill();
        bill.setId(rs.getInt("id"));
        bill.setTotalPrice(rs.getFloat("totalprice"));
        bill.setClientName(rs.getString("clientname"));
        bill.setClientPhone(rs.getString("clientphone"));
        bill.setDate(LocalDate.parse(rs.getString("date")));
        
        // Handle new POS columns with NULL safety for backwards compatibility
        try {
            // Subtotal - default to 0.0 if NULL or column doesn't exist
            float subtotal = rs.getFloat("subtotal");
            if (rs.wasNull()) {
                subtotal = 0.0f;
            }
            bill.setSubtotal(subtotal);
            
            // Discount type - default to "none" if NULL or column doesn't exist
            String discountType = rs.getString("discount_type");
            if (discountType == null) {
                discountType = "none";
            }
            bill.setDiscountType(discountType);
            
            // Discount value - default to 0.0 if NULL or column doesn't exist
            float discountValue = rs.getFloat("discount_value");
            if (rs.wasNull()) {
                discountValue = 0.0f;
            }
            bill.setDiscountValue(discountValue);
            
            // Payment method - default to "cash" if NULL or column doesn't exist
            String paymentMethod = rs.getString("payment_method");
            if (paymentMethod == null) {
                paymentMethod = "cash";
            }
            bill.setPaymentMethod(paymentMethod);
            
        } catch (SQLException e) {
            // If columns don't exist (old schema), use default values
            logger.debug("POS columns not found in result set, using default values", e);
            bill.setSubtotal(0.0f);
            bill.setDiscountType("none");
            bill.setDiscountValue(0.0f);
            bill.setPaymentMethod("cash");
        }
        
        return bill;
    }
    
    /**
     * Maps a ResultSet row to a Command object.
     * 
     * @param rs the ResultSet
     * @return the Command object
     * @throws SQLException if a database access error occurs
     */
    private Command mapResultSetToCommand(ResultSet rs) throws SQLException {
        Command command = new Command();
        command.setBillId(rs.getInt("billid"));
        command.setPartId(rs.getInt("partid"));
        command.setQuantity(rs.getInt("quantity"));
        command.setPriceConsidered(rs.getFloat("priceconsidered"));
        
        return command;
    }
}
