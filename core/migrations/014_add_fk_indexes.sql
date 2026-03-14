-- Add indexes on all FK columns that currently lack them (Req 16.2)
CREATE INDEX IF NOT EXISTS idx_sales_customer_id ON sales(customer_id);
CREATE INDEX IF NOT EXISTS idx_sales_payment_wallet_id ON sales(payment_wallet_id);
CREATE INDEX IF NOT EXISTS idx_sale_line_items_sale_id ON sale_line_items(sale_id);
CREATE INDEX IF NOT EXISTS idx_production_logs_recipe_id ON production_logs(recipe_id);
CREATE INDEX IF NOT EXISTS idx_production_logs_chef_id ON production_logs(chef_id);
CREATE INDEX IF NOT EXISTS idx_production_logs_finished_good_id ON production_logs(finished_good_id);
CREATE INDEX IF NOT EXISTS idx_recipe_ingredients_recipe_id ON recipe_ingredients(recipe_id);
CREATE INDEX IF NOT EXISTS idx_recipe_ingredients_raw_material_id ON recipe_ingredients(raw_material_id);
CREATE INDEX IF NOT EXISTS idx_debt_records_customer_id ON debt_records(customer_id);
CREATE INDEX IF NOT EXISTS idx_debt_records_sale_id ON debt_records(sale_id);
CREATE INDEX IF NOT EXISTS idx_expenses_wallet_id ON expenses(wallet_id);
CREATE INDEX IF NOT EXISTS idx_expenses_recorded_by ON expenses(recorded_by);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_wallet_id ON wallet_transactions(wallet_id);
