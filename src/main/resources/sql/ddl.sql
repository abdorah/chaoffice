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
    name TEXT NOT NULL UNIQUE,
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
-- Includes POS system features: discounts, payment methods, and subtotal tracking
CREATE TABLE IF NOT EXISTS bills (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    totalprice REAL NOT NULL CHECK (totalprice >= 0),
    clientname TEXT NOT NULL,
    clientphone TEXT NOT NULL,
    date TEXT NOT NULL,
    subtotal REAL DEFAULT 0.0 CHECK (subtotal >= 0),
    discount_type TEXT DEFAULT 'none' CHECK (discount_type IN ('none', 'percentage', 'fixed')),
    discount_value REAL DEFAULT 0.0 CHECK (discount_value >= 0),
    payment_method TEXT DEFAULT 'cash' CHECK (payment_method IN ('cash', 'card', 'check'))
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

-- Insert sample parts
-- Engine parts
INSERT OR IGNORE INTO parts (name, maker_id, description, price, quantity, catid)
SELECT 'Oil Filter', m.id, 'High-quality oil filter for engine protection', 15.99, 50, c.id
FROM makers m, categories c
WHERE m.name = 'Bosch' AND c.name = 'Engine'
AND NOT EXISTS (SELECT 1 FROM parts WHERE name = 'Oil Filter');

INSERT OR IGNORE INTO parts (name, maker_id, description, price, quantity, catid)
SELECT 'Air Filter', m.id, 'Premium air filter for optimal engine performance', 22.50, 40, c.id
FROM makers m, categories c
WHERE m.name = 'Denso' AND c.name = 'Engine'
AND NOT EXISTS (SELECT 1 FROM parts WHERE name = 'Air Filter');

INSERT OR IGNORE INTO parts (name, maker_id, description, price, quantity, catid)
SELECT 'Spark Plug Set', m.id, 'Set of 4 high-performance spark plugs', 45.00, 30, c.id
FROM makers m, categories c
WHERE m.name = 'ACDelco' AND c.name = 'Engine'
AND NOT EXISTS (SELECT 1 FROM parts WHERE name = 'Spark Plug Set');

-- Brake parts
INSERT OR IGNORE INTO parts (name, maker_id, description, price, quantity, catid)
SELECT 'Brake Pads Front', m.id, 'Premium ceramic brake pads for front wheels', 89.99, 25, c.id
FROM makers m, categories c
WHERE m.name = 'Brembo' AND c.name = 'Brakes'
AND NOT EXISTS (SELECT 1 FROM parts WHERE name = 'Brake Pads Front');

INSERT OR IGNORE INTO parts (name, maker_id, description, price, quantity, catid)
SELECT 'Brake Pads Rear', m.id, 'Premium ceramic brake pads for rear wheels', 79.99, 25, c.id
FROM makers m, categories c
WHERE m.name = 'Brembo' AND c.name = 'Brakes'
AND NOT EXISTS (SELECT 1 FROM parts WHERE name = 'Brake Pads Rear');

INSERT OR IGNORE INTO parts (name, maker_id, description, price, quantity, catid)
SELECT 'Brake Disc Front', m.id, 'High-performance brake disc for front wheels', 125.00, 20, c.id
FROM makers m, categories c
WHERE m.name = 'Brembo' AND c.name = 'Brakes'
AND NOT EXISTS (SELECT 1 FROM parts WHERE name = 'Brake Disc Front');

-- Suspension parts
INSERT OR IGNORE INTO parts (name, maker_id, description, price, quantity, catid)
SELECT 'Shock Absorber Front', m.id, 'Heavy-duty shock absorber for front suspension', 150.00, 15, c.id
FROM makers m, categories c
WHERE m.name = 'Michelin' AND c.name = 'Suspension'
AND NOT EXISTS (SELECT 1 FROM parts WHERE name = 'Shock Absorber Front');

INSERT OR IGNORE INTO parts (name, maker_id, description, price, quantity, catid)
SELECT 'Shock Absorber Rear', m.id, 'Heavy-duty shock absorber for rear suspension', 140.00, 15, c.id
FROM makers m, categories c
WHERE m.name = 'Michelin' AND c.name = 'Suspension'
AND NOT EXISTS (SELECT 1 FROM parts WHERE name = 'Shock Absorber Rear');

-- Cooling System parts
INSERT OR IGNORE INTO parts (name, maker_id, description, price, quantity, catid)
SELECT 'Radiator', m.id, 'High-efficiency aluminum radiator', 250.00, 10, c.id
FROM makers m, categories c
WHERE m.name = 'Denso' AND c.name = 'Cooling System'
AND NOT EXISTS (SELECT 1 FROM parts WHERE name = 'Radiator');

INSERT OR IGNORE INTO parts (name, maker_id, description, price, quantity, catid)
SELECT 'Water Pump', m.id, 'Durable water pump for cooling system', 95.00, 20, c.id
FROM makers m, categories c
WHERE m.name = 'Bosch' AND c.name = 'Cooling System'
AND NOT EXISTS (SELECT 1 FROM parts WHERE name = 'Water Pump');

-- Electrical parts
INSERT OR IGNORE INTO parts (name, maker_id, description, price, quantity, catid)
SELECT 'Battery', m.id, '12V high-capacity car battery', 180.00, 12, c.id
FROM makers m, categories c
WHERE m.name = 'ACDelco' AND c.name = 'Electrical'
AND NOT EXISTS (SELECT 1 FROM parts WHERE name = 'Battery');

INSERT OR IGNORE INTO parts (name, maker_id, description, price, quantity, catid)
SELECT 'Alternator', m.id, 'High-output alternator for electrical system', 320.00, 8, c.id
FROM makers m, categories c
WHERE m.name = 'Bosch' AND c.name = 'Electrical'
AND NOT EXISTS (SELECT 1 FROM parts WHERE name = 'Alternator');


-- Branding settings table
-- Stores optional store name and logo for application customization
-- Single row table (id must always be 1)
CREATE TABLE IF NOT EXISTS branding_settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    store_name TEXT,
    logo_image BLOB,
    logo_format TEXT
);

-- Currency settings table
-- Stores currency symbol and acronym for price display customization
-- Single row table (id must always be 1)
CREATE TABLE IF NOT EXISTS currency_settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    symbol TEXT NOT NULL DEFAULT '$',
    acronym TEXT NOT NULL DEFAULT 'USD'
);

-- Create indexes for search optimization
-- These indexes improve performance for part search queries
CREATE INDEX IF NOT EXISTS idx_parts_name ON parts(name);
CREATE INDEX IF NOT EXISTS idx_parts_catid ON parts(catid);
CREATE INDEX IF NOT EXISTS idx_makers_name ON makers(name);
