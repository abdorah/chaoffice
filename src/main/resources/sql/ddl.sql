-- Database initialization script for ChaOffice Parts Inventory System
-- This script creates all necessary tables for the application

-- Users table
-- Stores user authentication and profile information
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('admin', 'user', 'guest')),
    firstname TEXT,
    lastname TEXT
);

-- Makers table
-- Stores auto parts manufacturers/makers
CREATE TABLE IF NOT EXISTS makers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

-- Categories table
-- Stores part categories with optional icon images
CREATE TABLE IF NOT EXISTS categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    image BLOB
);

-- Parts table
-- Stores auto parts inventory with pricing and quantity
CREATE TABLE IF NOT EXISTS parts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    maker_id INTEGER NOT NULL,
    description TEXT NOT NULL,
    image BLOB,
    price REAL NOT NULL CHECK (price >= 0),
    quantity INTEGER NOT NULL CHECK (quantity >= 0),
    catid INTEGER NOT NULL,
    FOREIGN KEY (catid) REFERENCES categories(id),
    FOREIGN KEY (maker_id) REFERENCES makers(id)
);

-- Bills table
-- Stores sales transactions with client information
CREATE TABLE IF NOT EXISTS bills (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    totalprice REAL NOT NULL CHECK (totalprice >= 0),
    clientname TEXT NOT NULL,
    clientphone TEXT NOT NULL,
    date TEXT NOT NULL
);

-- Commands table (bill line items)
-- Stores individual parts sold within each bill
CREATE TABLE IF NOT EXISTS commands (
    billid INTEGER NOT NULL,
    partid INTEGER NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    priceconsidered REAL NOT NULL CHECK (priceconsidered >= 0),
    PRIMARY KEY (billid, partid),
    FOREIGN KEY (partid) REFERENCES parts(id),
    FOREIGN KEY (billid) REFERENCES bills(id)
);

-- Insert default admin user
-- Default credentials: username=admin, password=admin
-- Note: In production, passwords should be hashed
INSERT OR IGNORE INTO users (username, password, role, firstname, lastname)
VALUES ('admin', 'admin', 'admin', 'System', 'Administrator');

-- Insert sample makers
INSERT OR IGNORE INTO makers (name) VALUES ('Bosch');
INSERT OR IGNORE INTO makers (name) VALUES ('Denso');
INSERT OR IGNORE INTO makers (name) VALUES ('ACDelco');
INSERT OR IGNORE INTO makers (name) VALUES ('Brembo');
INSERT OR IGNORE INTO makers (name) VALUES ('Michelin');

-- Insert sample categories
INSERT OR IGNORE INTO categories (name, description) VALUES ('Engine', 'Engine components and parts');
INSERT OR IGNORE INTO categories (name, description) VALUES ('Suspension', 'Suspension system components');
INSERT OR IGNORE INTO categories (name, description) VALUES ('Brakes', 'Brake system components');
INSERT OR IGNORE INTO categories (name, description) VALUES ('Cooling System', 'Cooling and heating components');
INSERT OR IGNORE INTO categories (name, description) VALUES ('Electrical', 'Electrical system components');
