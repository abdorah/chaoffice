-- Create debt_payments and debt_payment_allocations tables (Req 14.1, 14.2)
CREATE TABLE IF NOT EXISTS debt_payments (
    id TEXT PRIMARY KEY NOT NULL,
    customer_id TEXT NOT NULL REFERENCES customers(id),
    amount INTEGER NOT NULL CHECK (amount > 0),
    wallet_id TEXT NOT NULL REFERENCES wallets(id),
    timestamp TEXT NOT NULL,
    sync_status TEXT NOT NULL DEFAULT 'Synced',
    updated_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_debt_payments_customer_id ON debt_payments(customer_id);
CREATE INDEX IF NOT EXISTS idx_debt_payments_wallet_id ON debt_payments(wallet_id);

CREATE TABLE IF NOT EXISTS debt_payment_allocations (
    id TEXT PRIMARY KEY NOT NULL,
    payment_id TEXT NOT NULL REFERENCES debt_payments(id) ON DELETE CASCADE,
    debt_record_id TEXT NOT NULL REFERENCES debt_records(id),
    amount_applied INTEGER NOT NULL CHECK (amount_applied > 0)
);
CREATE INDEX IF NOT EXISTS idx_dpa_payment_id ON debt_payment_allocations(payment_id);
CREATE INDEX IF NOT EXISTS idx_dpa_debt_record_id ON debt_payment_allocations(debt_record_id);
