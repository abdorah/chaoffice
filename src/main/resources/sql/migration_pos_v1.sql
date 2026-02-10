-- Migration script for Point of Sale System enhancements
-- Version: 1.0
-- Description: Adds discount, payment method, and subtotal tracking to bills table
--              Adds search optimization indexes to parts table

-- Add new columns to bills table with default values for backwards compatibility
-- These columns support the POS system's discount and payment tracking features

-- Add subtotal column (sum of all items before discount)
ALTER TABLE bills ADD COLUMN subtotal REAL DEFAULT 0.0 CHECK (subtotal >= 0);

-- Add discount_type column with enum constraint
-- Valid values: 'none', 'percentage', 'fixed'
ALTER TABLE bills ADD COLUMN discount_type TEXT DEFAULT 'none' 
    CHECK (discount_type IN ('none', 'percentage', 'fixed'));

-- Add discount_value column (percentage or fixed amount)
ALTER TABLE bills ADD COLUMN discount_value REAL DEFAULT 0.0 CHECK (discount_value >= 0);

-- Add payment_method column with enum constraint
-- Valid values: 'cash', 'card', 'check'
ALTER TABLE bills ADD COLUMN payment_method TEXT DEFAULT 'cash' 
    CHECK (payment_method IN ('cash', 'card', 'check'));

-- Create indexes for search optimization
-- These indexes improve performance for part search queries

-- Index on parts.name for text search
CREATE INDEX IF NOT EXISTS idx_parts_name ON parts(name);

-- Index on parts.catid for category filtering
CREATE INDEX IF NOT EXISTS idx_parts_catid ON parts(catid);

-- Index on makers.name for maker search (optional but helpful)
CREATE INDEX IF NOT EXISTS idx_makers_name ON makers(name);
