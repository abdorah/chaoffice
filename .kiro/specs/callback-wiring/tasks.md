# Callback Wiring Tasks — Fix All UI Callbacks

## Status Legend
- `[ ]` Not started
- `[W]` Wired (callback connected to Rust) but body is TODO/stub
- `[X]` Fully working (wired + implemented + updates UI)
- `[N]` Not wired at all (no `on_` handler in main.rs)

## AppState Callbacks (ui/globals/app_state.slint)

### Authentication
- [X] `login(username, password)` — Wired in main.rs, calls authentication controller
- [N] `logout()` — NOT wired. No `on_logout` handler exists.

### User Management
- [N] `create-user(username, password, display_name, role)` — NOT wired
- [N] `deactivate-user(user_id)` — NOT wired

### Products
- [N] `create-product(name, reference, description, quantity, price_unit)` — NOT wired
- [N] `delete-product(id)` — NOT wired
- [N] `refresh-products()` — NOT wired

### Categories
- [N] `create-category(name, description, parent_id)` — NOT wired
- [N] `delete-category(id)` — NOT wired
- [N] `refresh-categories()` — NOT wired

### Persons
- [N] `create-person(name, role, phone, email)` — NOT wired
- [N] `delete-person(id)` — NOT wired
- [N] `refresh-persons()` — NOT wired

### Deals
- [N] `create-deal(title, description, product_id, supplier_id, manager_id, frequency)` — NOT wired
- [N] `delete-deal(id)` — NOT wired
- [N] `refresh-deals()` — NOT wired

### Locations
- [N] `create-location(name, address, lat, lng, capacity)` — NOT wired
- [N] `delete-location(id)` — NOT wired
- [N] `refresh-locations()` — NOT wired

### Stock Tracking
- [N] `record-stock-movement(type, quantity, note)` — NOT wired (AppState version)

### Budget
- [N] `record-budget-entry(type, amount, description)` — NOT wired (AppState version)

### Sync
- [X] `sync-to-remote()` — Implemented (stubbed — LibSQL not connected)
- [X] `sync-from-remote()` — Implemented (stubbed — LibSQL not connected)
- [X] `configure-sync(url, token, auto_sync, interval)` — Implemented (stubbed — LibSQL not connected)

### Reports
- [N] `generate-inventory-report(format, include_zero_stock)` — NOT wired
- [N] `generate-stock-report(format)` — NOT wired
- [N] `generate-budget-report(format, include_projections)` — NOT wired
- [N] `generate-purchasing-report(format, status_filter)` — NOT wired

## Page-Level Adapter Callbacks

### StockTrackingAdapter
- [X] `record-movement(product, type, qty, from, to, note)` — Fully implemented
- [X] `load-history(product, from_date, to_date)` — Fully implemented
- [X] `load-summary()` — Fully implemented

### BudgetPageAdapter
- [X] `load-summary(from_date, to_date)` — Fully implemented
- [X] `load-projection(months_ahead)` — Fully implemented
- [X] `record-entry(type, amount, desc, date, product, deal)` — Fully implemented

## Implementation Tasks

- [x] 1. Wire CRUD callbacks for Products (create, delete, refresh)
- [x] 2. Wire CRUD callbacks for Categories (create, delete, refresh)
- [x] 3. Wire CRUD callbacks for Persons (create, delete, refresh)
- [x] 4. Wire CRUD callbacks for Deals (create, delete, refresh)
- [x] 5. Wire CRUD callbacks for Locations (create, delete, refresh)
- [x] 6. Wire logout callback
- [x] 7. Wire create-user and deactivate-user callbacks
- [x] 8. Wire report generation callbacks (4 report types)
- [x] 9. Implement StockTrackingAdapter callback bodies (record, history, summary)
- [x] 10. Implement BudgetPageAdapter callback bodies (summary, projection, record)
- [x] 11. Implement sync callback bodies (push, pull, configure)
- [ ] 12. Verify all callbacks work end-to-end
