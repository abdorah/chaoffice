-- Convert all REAL monetary columns to INTEGER cents (Req 1.5)
-- raw_materials.current_quantity stays REAL (physical quantity, not money)
-- finished_goods.current_quantity stays REAL (physical quantity)

-- finished_goods: unit_price REAL → INTEGER
CREATE TABLE finished_goods_new (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    current_quantity REAL NOT NULL DEFAULT 0.0 CHECK (current_quantity >= 0),
    unit_price INTEGER NOT NULL DEFAULT 0 CHECK (unit_price >= 0),
    last_updated TEXT NOT NULL,
    sync_status TEXT NOT NULL DEFAULT 'Synced',
    updated_at TEXT NOT NULL
);
INSERT INTO finished_goods_new
    SELECT id, name, current_quantity, CAST(ROUND(unit_price * 100) AS INTEGER), last_updated, sync_status, updated_at
    FROM finished_goods;
DROP TABLE finished_goods;
ALTER TABLE finished_goods_new RENAME TO finished_goods;

-- sales: total_amount, amount_paid REAL → INTEGER
CREATE TABLE sales_new (
    id TEXT PRIMARY KEY NOT NULL,
    customer_id TEXT NOT NULL REFERENCES customers(id),
    total_amount INTEGER NOT NULL CHECK (total_amount >= 0),
    amount_paid INTEGER NOT NULL CHECK (amount_paid >= 0),
    payment_wallet_id TEXT NOT NULL REFERENCES wallets(id),
    timestamp TEXT NOT NULL,
    sync_status TEXT NOT NULL DEFAULT 'Synced',
    updated_at TEXT NOT NULL
);
INSERT INTO sales_new
    SELECT id, customer_id, CAST(ROUND(total_amount * 100) AS INTEGER),
           CAST(ROUND(amount_paid * 100) AS INTEGER), payment_wallet_id, timestamp, sync_status, updated_at
    FROM sales;
DROP TABLE sales;
ALTER TABLE sales_new RENAME TO sales;
CREATE INDEX IF NOT EXISTS idx_sales_customer_id ON sales(customer_id);
CREATE INDEX IF NOT EXISTS idx_sales_payment_wallet_id ON sales(payment_wallet_id);

-- wallets: current_balance REAL → INTEGER
CREATE TABLE wallets_new (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    wallet_type TEXT NOT NULL CHECK (wallet_type IN ('Bank', 'Cash', 'Representative')),
    current_balance INTEGER NOT NULL DEFAULT 0,
    sync_status TEXT NOT NULL DEFAULT 'Synced',
    updated_at TEXT NOT NULL
);
INSERT INTO wallets_new
    SELECT id, name, wallet_type, CAST(ROUND(current_balance * 100) AS INTEGER), sync_status, updated_at
    FROM wallets;
DROP TABLE wallets;
ALTER TABLE wallets_new RENAME TO wallets;

-- wallet_transactions: amount REAL → INTEGER
CREATE TABLE wallet_transactions_new (
    id TEXT PRIMARY KEY NOT NULL,
    wallet_id TEXT NOT NULL REFERENCES wallets(id),
    amount INTEGER NOT NULL,
    description TEXT NOT NULL,
    related_entity_id TEXT,
    timestamp TEXT NOT NULL,
    sync_status TEXT NOT NULL DEFAULT 'Synced',
    updated_at TEXT NOT NULL
);
INSERT INTO wallet_transactions_new
    SELECT id, wallet_id, CAST(ROUND(amount * 100) AS INTEGER), description,
           related_entity_id, timestamp, sync_status, updated_at
    FROM wallet_transactions;
DROP TABLE wallet_transactions;
ALTER TABLE wallet_transactions_new RENAME TO wallet_transactions;
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_wallet_id ON wallet_transactions(wallet_id);

-- debt_records: original_amount, remaining_amount REAL → INTEGER
CREATE TABLE debt_records_new (
    id TEXT PRIMARY KEY NOT NULL,
    customer_id TEXT NOT NULL REFERENCES customers(id),
    sale_id TEXT NOT NULL REFERENCES sales(id),
    original_amount INTEGER NOT NULL CHECK (original_amount > 0),
    remaining_amount INTEGER NOT NULL CHECK (remaining_amount >= 0),
    sale_date TEXT NOT NULL,
    is_settled INTEGER NOT NULL DEFAULT 0,
    sync_status TEXT NOT NULL DEFAULT 'Synced',
    updated_at TEXT NOT NULL
);
INSERT INTO debt_records_new
    SELECT id, customer_id, sale_id, CAST(ROUND(original_amount * 100) AS INTEGER),
           CAST(ROUND(remaining_amount * 100) AS INTEGER), sale_date, is_settled, sync_status, updated_at
    FROM debt_records;
DROP TABLE debt_records;
ALTER TABLE debt_records_new RENAME TO debt_records;
CREATE INDEX IF NOT EXISTS idx_debt_records_customer_id ON debt_records(customer_id);
CREATE INDEX IF NOT EXISTS idx_debt_records_sale_id ON debt_records(sale_id);

-- expenses: amount REAL → INTEGER
CREATE TABLE expenses_new (
    id TEXT PRIMARY KEY NOT NULL,
    description TEXT NOT NULL,
    amount INTEGER NOT NULL CHECK (amount > 0),
    category TEXT NOT NULL CHECK (category IN ('Purchase', 'OperatingCost')),
    wallet_id TEXT NOT NULL REFERENCES wallets(id),
    recorded_by TEXT NOT NULL REFERENCES users(id),
    timestamp TEXT NOT NULL,
    sync_status TEXT NOT NULL DEFAULT 'Synced',
    updated_at TEXT NOT NULL
);
INSERT INTO expenses_new
    SELECT id, description, CAST(ROUND(amount * 100) AS INTEGER), category,
           wallet_id, recorded_by, timestamp, sync_status, updated_at
    FROM expenses;
DROP TABLE expenses;
ALTER TABLE expenses_new RENAME TO expenses;
CREATE INDEX IF NOT EXISTS idx_expenses_wallet_id ON expenses(wallet_id);
CREATE INDEX IF NOT EXISTS idx_expenses_recorded_by ON expenses(recorded_by);