package org.chaos.office.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Standalone migration runner for executing database migrations.
 * This class can be run directly to apply migrations to the database.
 * 
 * <p>Usage: Run this class as a Java application to execute the POS migration.
 * 
 * <p>Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 8.1, 8.2
 */
public class MigrationRunner {
    private static final Logger logger = LoggerFactory.getLogger(MigrationRunner.class);
    
    public static void main(String[] args) {
        logger.info("=== ChaOffice Database Migration Runner ===");
        logger.info("Starting POS system migration...\n");
        
        try {
            // Get database connection
            DatabaseConnection dbConn = DatabaseConnection.getInstance();
            Connection connection = dbConn.getConnection();
            
            logger.info("Connected to database at: {}", dbConn.getDatabaseLocation());
            
            // Display current schema before migration
            logger.info("\n--- Current Bills Table Schema ---");
            List<String> schemaBefore = DatabaseMigration.getBillsTableSchema(connection);
            schemaBefore.forEach(logger::info);
            
            logger.info("\n--- Current Indexes ---");
            List<String> indexesBefore = DatabaseMigration.getAllIndexes(connection);
            indexesBefore.forEach(index -> logger.info("Index: {}", index));
            
            // Execute migration
            logger.info("\n--- Executing Migration ---");
            DatabaseMigration.executePOSMigration(connection);
            
            // Display schema after migration
            logger.info("\n--- Bills Table Schema After Migration ---");
            List<String> schemaAfter = DatabaseMigration.getBillsTableSchema(connection);
            schemaAfter.forEach(logger::info);
            
            logger.info("\n--- Indexes After Migration ---");
            List<String> indexesAfter = DatabaseMigration.getAllIndexes(connection);
            indexesAfter.forEach(index -> logger.info("Index: {}", index));
            
            // Verify migration
            logger.info("\n--- Verifying Migration ---");
            DatabaseMigration.verifyMigration(connection);
            
            logger.info("\n=== Migration Completed Successfully ===");
            logger.info("The following changes were applied:");
            logger.info("  ✓ Added 'subtotal' column to bills table");
            logger.info("  ✓ Added 'discount_type' column to bills table");
            logger.info("  ✓ Added 'discount_value' column to bills table");
            logger.info("  ✓ Added 'payment_method' column to bills table");
            logger.info("  ✓ Created index 'idx_parts_name' on parts.name");
            logger.info("  ✓ Created index 'idx_parts_catid' on parts.catid");
            logger.info("  ✓ Created index 'idx_makers_name' on makers.name");
            logger.info("  ✓ Verified constraint enforcement for discount_type");
            logger.info("  ✓ Verified constraint enforcement for payment_method");
            
            // Close connection
            dbConn.closeConnection();
            
        } catch (SQLException e) {
            logger.error("Database error during migration", e);
            System.exit(1);
        } catch (IOException e) {
            logger.error("IO error during migration", e);
            System.exit(1);
        }
    }
}
