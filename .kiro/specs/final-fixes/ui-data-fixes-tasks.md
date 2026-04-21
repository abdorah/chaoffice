# UI Data Fixes — Empty Sections

## Issue 1: Stock Tracking — No seed data for stock movements
- [x] 1.1 Add stock movement seed data (25 movements: Inbound, Outbound, Transfer, Return)
- [x] 1.2 Movements span last 28 days so KPIs show non-zero values

## Issue 2: Dashboard — Stock Alerts never populated
- [x] 2.1 Added refresh_dashboard_alerts() that computes low-stock products (qty < 10)
- [x] 2.2 Populates AppState.stock-alerts and AppState.low-stock-count
- [x] 2.3 Added 5 low-stock products in seed data (qty 1-5) to trigger alerts

## Issue 3: Budget Trends tab — trend paths never computed
- [x] 3.1 In load-summary callback, compute trend SVG paths from monthly sales/expenses data
- [x] 3.2 Budget seed data now spans 6 months (180 days) for multi-month trends

## Issue 4: Budget Projection — flat lines
- [x] 4.1 Seed data includes active deals with meaningful recurring costs
- [x] 4.2 More budget entries across months provide better historical averages for projection

## Issue 5: Auto-load data on page navigation
- [x] 5.1 After login: invoke_load_summary() on StockTrackingAdapter
- [x] 5.2 After login: invoke_load_summary("","") and invoke_load_projection(6) on BudgetPageAdapter
- [x] 5.3 After login: refresh_dashboard_alerts() for stock alerts
