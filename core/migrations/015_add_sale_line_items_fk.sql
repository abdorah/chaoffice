-- Recreate sale_line_items with FK on finished_good_id (Req 16.1)
-- SQLite doesn't support ALTER TABLE ADD CONSTRAINT, so we recreate the table
CREATE TABLE sale_line_items_new (
    id TEXT PRIMARY KEY NOT NULL,
    sale_id TEXT NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    finished_good_id TEXT NOT NULL REFERENCES finished_goods(id),
    finished_good_name TEXT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price INTEGER NOT NULL CHECK (unit_price >= 0)
);
INSERT INTO sale_line_items_new SELECT * FROM sale_line_items;
DROP TABLE sale_line_items;
ALTER TABLE sale_line_items_new RENAME TO sale_line_items;
CREATE INDEX IF NOT EXISTS idx_sale_line_items_sale_id ON sale_line_items(sale_id);
CREATE INDEX IF NOT EXISTS idx_sale_line_items_fg_id ON sale_line_items(finished_good_id);
