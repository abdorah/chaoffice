package org.chaos.office.service;

import org.chaos.office.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * SalesService provides sales analytics and reporting functionality.
 * 
 * <p>This service is responsible for:
 * <ul>
 *   <li>Calculating sales by date</li>
 *   <li>Computing total revenue</li>
 *   <li>Counting transactions</li>
 * </ul>
 * 
 * <p>Requirements: 7.2, 7.3, 7.4
 */
public class SalesService {
    private static final Logger logger = LoggerFactory.getLogger(SalesService.class);
    
    /**
     * Retrieves sales grouped by date within a date range.
     * 
     * @param start the start date (inclusive)
     * @param end the end date (inclusive)
     * @return Map of dates to total sales for that date
     */
    public Map<LocalDate, Double> getSalesByDate(LocalDate start, LocalDate end) {
        Map<LocalDate, Double> salesByDate = new HashMap<>();
        String sql = "SELECT date, SUM(totalprice) as total FROM bills WHERE date >= ? AND date <= ? GROUP BY date";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, start.toString());
            stmt.setString(2, end.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = LocalDate.parse(rs.getString("date"));
                    double total = rs.getDouble("total");
                    salesByDate.put(date, total);
                }
            }
            
            logger.info("Retrieved sales for {} dates between {} and {}", salesByDate.size(), start, end);
        } catch (SQLException e) {
            logger.error("Error retrieving sales by date", e);
        }
        
        return salesByDate;
    }
    
    /**
     * Calculates total revenue within a date range.
     * 
     * @param start the start date (inclusive)
     * @param end the end date (inclusive)
     * @return total revenue
     */
    public double getTotalRevenue(LocalDate start, LocalDate end) {
        String sql = "SELECT SUM(totalprice) as total FROM bills WHERE date >= ? AND date <= ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, start.toString());
            stmt.setString(2, end.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double total = rs.getDouble("total");
                    logger.info("Total revenue between {} and {}: {}", start, end, total);
                    return total;
                }
            }
        } catch (SQLException e) {
            logger.error("Error calculating total revenue", e);
        }
        
        return 0.0;
    }
    
    /**
     * Counts the number of transactions within a date range.
     * 
     * @param start the start date (inclusive)
     * @param end the end date (inclusive)
     * @return number of transactions
     */
    public int getTransactionCount(LocalDate start, LocalDate end) {
        String sql = "SELECT COUNT(*) as count FROM bills WHERE date >= ? AND date <= ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, start.toString());
            stmt.setString(2, end.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    logger.info("Transaction count between {} and {}: {}", start, end, count);
                    return count;
                }
            }
        } catch (SQLException e) {
            logger.error("Error counting transactions", e);
        }
        
        return 0;
    }
}
