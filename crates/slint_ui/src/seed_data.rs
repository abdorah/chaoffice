//! Seed data module — populates the database with sample data for UI testing.
//! Only runs if the database has no existing products (i.e., fresh install).

use crate::AppContext;
use std::sync::Arc;

pub fn seed_if_empty(ctx: &Arc<AppContext>) {
    // Check if data already exists
    match frontend::commands::product_commands::get_all_product(ctx) {
        Ok(products) if !products.is_empty() => {
            log::info!("Seed: database already has {} products, skipping", products.len());
            return;
        }
        _ => {}
    }

    log::info!("Seed: populating database with sample data...");

    let now = chrono::Utc::now();

    // ── Categories ─────────────────────────────────────────────
    let category_names = [
        ("Electronics", "Computers, phones, tablets, and accessories"),
        ("Office Supplies", "Paper, pens, folders, and desk accessories"),
        ("Furniture", "Desks, chairs, shelves, and storage units"),
        ("Raw Materials", "Steel, wood, plastic, and other base materials"),
        ("Packaging", "Boxes, tape, bubble wrap, and shipping supplies"),
        ("Cleaning Supplies", "Detergents, mops, gloves, and sanitizers"),
        ("Safety Equipment", "Helmets, goggles, gloves, and first aid kits"),
        ("Tools", "Hand tools, power tools, and measurement instruments"),
    ];

    let mut category_ids = Vec::new();
    for (name, desc) in &category_names {
        let dto = frontend::direct_access::CreateCategoryDto {
            created_at: now, updated_at: now,
            name: name.to_string(), description: desc.to_string(),
            parent_category: None, subcategories: vec![],
        };
        match frontend::commands::category_commands::create_category(ctx, None, &dto, 1, -1) {
            Ok(c) => { category_ids.push(c.id); }
            Err(e) => { log::error!("Seed: category '{}': {}", name, e); }
        }
    }
    log::info!("Seed: created {} categories", category_ids.len());

    // ── Locations ──────────────────────────────────────────────
    let location_data = [
        ("Main Warehouse", "123 Industrial Blvd, Springfield", 40.7128, -74.0060, 5000),
        ("Downtown Store", "456 Main St, Springfield", 40.7580, -73.9855, 800),
        ("East Distribution Center", "789 Logistics Ave, Shelbyville", 41.8781, -87.6298, 12000),
        ("West Coast Hub", "321 Pacific Way, Capital City", 34.0522, -118.2437, 8000),
        ("North Storage Facility", "654 Arctic Rd, Ogdenville", 47.6062, -122.3321, 3000),
        ("South Retail Outlet", "987 Sunny Dr, North Haverbrook", 29.7604, -95.3698, 1200),
    ];

    let mut location_ids = Vec::new();
    for (name, addr, lat, lng, cap) in &location_data {
        let dto = frontend::direct_access::CreateLocationDto {
            created_at: now, updated_at: now,
            name: name.to_string(), address: addr.to_string(),
            latitude: *lat, longitude: *lng, capacity: *cap,
            manager: None,
        };
        match frontend::commands::location_commands::create_location(ctx, None, &dto, 1, -1) {
            Ok(l) => { location_ids.push(l.id); }
            Err(e) => { log::error!("Seed: location '{}': {}", name, e); }
        }
    }
    log::info!("Seed: created {} locations", location_ids.len());

    // ── Persons (Managers + Suppliers) ─────────────────────────
    let person_data = [
        ("Alice Johnson", frontend::direct_access::PersonRole::Manager),
        ("Bob Martinez", frontend::direct_access::PersonRole::Manager),
        ("Carol Chen", frontend::direct_access::PersonRole::Manager),
        ("TechParts Inc.", frontend::direct_access::PersonRole::Supplier),
        ("Global Office Co.", frontend::direct_access::PersonRole::Supplier),
        ("WoodWorks Ltd.", frontend::direct_access::PersonRole::Supplier),
        ("PackRight Solutions", frontend::direct_access::PersonRole::Supplier),
        ("CleanPro Industries", frontend::direct_access::PersonRole::Supplier),
        ("SafetyFirst Corp.", frontend::direct_access::PersonRole::Supplier),
        ("MetalForge Inc.", frontend::direct_access::PersonRole::Supplier),
    ];

    let mut person_ids = Vec::new();
    let mut supplier_ids = Vec::new();
    let mut manager_ids = Vec::new();
    for (name, role) in &person_data {
        let dto = frontend::direct_access::CreatePersonDto {
            created_at: now, updated_at: now,
            name: name.to_string(), role: role.clone(), contact: None,
        };
        match frontend::commands::person_commands::create_person(ctx, None, &dto, 1, -1) {
            Ok(p) => {
                person_ids.push(p.id);
                match role {
                    frontend::direct_access::PersonRole::Supplier => supplier_ids.push(p.id),
                    frontend::direct_access::PersonRole::Manager => manager_ids.push(p.id),
                }
            }
            Err(e) => { log::error!("Seed: person '{}': {}", name, e); }
        }
    }
    log::info!("Seed: created {} persons ({} managers, {} suppliers)",
        person_ids.len(), manager_ids.len(), supplier_ids.len());

    // ── Products ───────────────────────────────────────────────
    let product_data: Vec<(&str, &str, &str, i64, f64, usize, usize, usize)> = vec![
        // (name, reference, description, qty, price, cat_idx, supplier_idx, loc_idx)
        ("Laptop Pro 15", "LP-15-2024", "15-inch professional laptop with 32GB RAM", 45, 1299.99, 0, 0, 0),
        ("Wireless Mouse", "WM-100", "Ergonomic wireless mouse with USB-C receiver", 320, 29.99, 0, 0, 1),
        ("USB-C Hub 7-in-1", "UCH-7", "Multi-port USB-C hub with HDMI and ethernet", 150, 49.99, 0, 0, 0),
        ("Mechanical Keyboard", "MK-RGB", "RGB mechanical keyboard with Cherry MX switches", 85, 89.99, 0, 0, 2),
        ("27\" 4K Monitor", "MON-27-4K", "27-inch 4K IPS monitor with USB-C", 30, 449.99, 0, 0, 0),
        ("Webcam HD 1080p", "WC-1080", "Full HD webcam with built-in microphone", 200, 59.99, 0, 0, 1),
        ("A4 Copy Paper (5000)", "CP-A4-5K", "Premium A4 copy paper, 5000 sheets", 500, 34.99, 1, 1, 0),
        ("Ballpoint Pens (50)", "BP-50-BLK", "Black ballpoint pens, box of 50", 800, 12.99, 1, 1, 1),
        ("Manila Folders (100)", "MF-100", "Letter-size manila folders, pack of 100", 250, 19.99, 1, 1, 0),
        ("Sticky Notes (12-pack)", "SN-12", "3x3 inch sticky notes, assorted colors", 600, 8.99, 1, 1, 1),
        ("Whiteboard Markers (24)", "WBM-24", "Dry-erase markers, 24 assorted colors", 180, 15.99, 1, 1, 2),
        ("Standing Desk", "SD-ELEC", "Electric height-adjustable standing desk", 20, 599.99, 2, 2, 3),
        ("Ergonomic Office Chair", "EOC-PRO", "Mesh-back ergonomic chair with lumbar support", 35, 349.99, 2, 2, 3),
        ("Bookshelf 5-Tier", "BS-5T", "5-tier wooden bookshelf, walnut finish", 40, 129.99, 2, 2, 3),
        ("Filing Cabinet 3-Drawer", "FC-3D", "Metal 3-drawer filing cabinet with lock", 25, 179.99, 2, 2, 0),
        ("Steel Sheets (1mm)", "SS-1MM", "Cold-rolled steel sheets, 1mm thickness, per unit", 1000, 15.50, 3, 6, 2),
        ("Plywood Boards", "PW-18MM", "18mm plywood boards, 4x8 feet", 300, 42.00, 3, 2, 2),
        ("ABS Plastic Pellets (25kg)", "ABS-25", "ABS plastic pellets for injection molding", 150, 85.00, 3, 6, 2),
        ("Aluminum Rods (2m)", "AR-2M", "6061 aluminum rods, 2 meter length", 500, 22.50, 3, 6, 2),
        ("Corrugated Boxes (Large)", "CB-LG", "Large corrugated shipping boxes, 24x18x18", 2000, 2.50, 4, 3, 0),
        ("Bubble Wrap Roll (100m)", "BW-100", "Bubble wrap roll, 100 meters", 80, 35.00, 4, 3, 0),
        ("Packing Tape (36 rolls)", "PT-36", "Clear packing tape, 36 rolls", 120, 28.99, 4, 3, 0),
        ("Shipping Labels (1000)", "SL-1K", "Self-adhesive shipping labels, 1000 per roll", 300, 18.99, 4, 3, 1),
        ("All-Purpose Cleaner (5L)", "APC-5L", "Industrial all-purpose cleaner, 5 liters", 100, 24.99, 5, 4, 0),
        ("Nitrile Gloves (Box 100)", "NG-100", "Disposable nitrile gloves, box of 100", 400, 14.99, 5, 4, 0),
        ("Microfiber Cloths (50)", "MC-50", "Microfiber cleaning cloths, pack of 50", 200, 22.99, 5, 4, 1),
        ("Safety Goggles", "SG-CLEAR", "Clear anti-fog safety goggles", 150, 12.99, 6, 5, 0),
        ("Hard Hat (White)", "HH-WHT", "OSHA-compliant white hard hat", 75, 19.99, 6, 5, 2),
        ("First Aid Kit (50-person)", "FAK-50", "Comprehensive first aid kit for 50 persons", 30, 89.99, 6, 5, 0),
        ("High-Vis Vest", "HVV-ORG", "Orange high-visibility safety vest", 200, 9.99, 6, 5, 2),
        ("Cordless Drill", "CD-20V", "20V cordless drill with 2 batteries", 40, 129.99, 7, 0, 3),
        ("Digital Caliper", "DC-150", "150mm digital caliper, 0.01mm precision", 60, 34.99, 7, 0, 0),
        ("Socket Set (52-piece)", "SS-52", "52-piece chrome vanadium socket set", 45, 79.99, 7, 0, 3),
        ("Tape Measure (8m)", "TM-8M", "8-meter retractable tape measure", 150, 11.99, 7, 0, 1),
        ("Laser Level", "LL-360", "360-degree self-leveling laser level", 25, 149.99, 7, 0, 3),
        // Low-stock items to trigger dashboard alerts
        ("Thermal Printer", "TP-USB", "USB thermal receipt printer", 3, 199.99, 0, 0, 1),
        ("Label Maker", "LM-PRO", "Professional label maker with keyboard", 5, 69.99, 1, 1, 0),
        ("Fire Extinguisher", "FE-ABC", "ABC dry chemical fire extinguisher", 2, 45.99, 6, 5, 2),
        ("Pallet Jack", "PJ-5T", "5-ton hydraulic pallet jack", 1, 399.99, 7, 0, 3),
        ("Barcode Scanner", "BS-2D", "2D wireless barcode scanner", 4, 89.99, 0, 0, 0),
    ];

    let mut product_ids = Vec::new();
    for (name, reference, desc, qty, price, cat_idx, sup_idx, loc_idx) in &product_data {
        let cat_id = category_ids.get(*cat_idx).copied();
        let sup_id = supplier_ids.get(*sup_idx).copied();
        let loc_id = location_ids.get(*loc_idx).copied();
        let dto = frontend::direct_access::CreateProductDto {
            created_at: now, updated_at: now,
            name: name.to_string(), reference: reference.to_string(),
            description: desc.to_string(), quantity: *qty, price_unit: *price,
            status: frontend::direct_access::ProductStatus::Available,
            category: cat_id, supplier: sup_id, location: loc_id,
        };
        match frontend::commands::product_commands::create_product(ctx, None, &dto, 1, -1) {
            Ok(p) => { product_ids.push(p.id); }
            Err(e) => { log::error!("Seed: product '{}': {}", name, e); }
        }
    }
    log::info!("Seed: created {} products", product_ids.len());

    // ── Deals ──────────────────────────────────────────────────
    use chrono::Duration;
    let deal_data: Vec<(&str, &str, f64, f64, frontend::direct_access::DealFrequency, frontend::direct_access::DealStatus, i64, usize, usize, usize)> = vec![
        // (title, desc, unit_cost, total, freq, status, duration_days, product_idx, supplier_idx, manager_idx)
        ("Laptop Bulk Purchase Q1", "Quarterly laptop procurement for offices", 1150.00, 51750.00,
            frontend::direct_access::DealFrequency::Quarterly, frontend::direct_access::DealStatus::Active, 90, 0, 0, 0),
        ("Office Supplies Annual", "Annual office supplies contract", 0.0, 25000.00,
            frontend::direct_access::DealFrequency::Yearly, frontend::direct_access::DealStatus::Active, 365, 6, 1, 0),
        ("Steel Sheet Monthly", "Monthly steel sheet delivery", 15.00, 15500.00,
            frontend::direct_access::DealFrequency::Monthly, frontend::direct_access::DealStatus::Active, 365, 15, 6, 1),
        ("Packaging Supplies Weekly", "Weekly packaging materials restock", 0.0, 5200.00,
            frontend::direct_access::DealFrequency::Weekly, frontend::direct_access::DealStatus::Active, 365, 19, 3, 1),
        ("Furniture Refresh 2024", "One-time office furniture upgrade", 0.0, 45000.00,
            frontend::direct_access::DealFrequency::OneTime, frontend::direct_access::DealStatus::Completed, 60, 11, 2, 0),
        ("Cleaning Contract H2", "Second half cleaning supplies", 0.0, 8000.00,
            frontend::direct_access::DealFrequency::Monthly, frontend::direct_access::DealStatus::Active, 180, 23, 4, 2),
        ("Safety Equipment Restock", "Quarterly safety gear replenishment", 0.0, 12000.00,
            frontend::direct_access::DealFrequency::Quarterly, frontend::direct_access::DealStatus::Draft, 90, 26, 5, 2),
        ("Tool Upgrade Program", "Annual tool replacement program", 0.0, 18000.00,
            frontend::direct_access::DealFrequency::Yearly, frontend::direct_access::DealStatus::Draft, 365, 30, 0, 0),
        ("Monitor Procurement", "One-time monitor purchase for new hires", 400.00, 12000.00,
            frontend::direct_access::DealFrequency::OneTime, frontend::direct_access::DealStatus::Completed, 30, 4, 0, 1),
        ("Plastic Pellets Contract", "Monthly ABS pellets supply", 80.00, 12000.00,
            frontend::direct_access::DealFrequency::Monthly, frontend::direct_access::DealStatus::Cancelled, 365, 17, 6, 2),
    ];

    for (title, desc, uc, tv, freq, status, dur, prod_idx, sup_idx, mgr_idx) in &deal_data {
        let prod_id = product_ids.get(*prod_idx).copied();
        let sup_id = supplier_ids.get(*sup_idx).copied();
        let mgr_id = manager_ids.get(*mgr_idx).copied();
        let dto = frontend::direct_access::CreateDealDto {
            created_at: now, updated_at: now,
            title: title.to_string(), description: desc.to_string(),
            unit_cost: *uc, total_value: *tv,
            start_date: now - Duration::days(*dur),
            end_date: now + Duration::days(*dur),
            frequency: freq.clone(), status: status.clone(),
            product: prod_id, supplier: sup_id, manager: mgr_id,
        };
        match frontend::commands::deal_commands::create_deal(ctx, None, &dto, 1, -1) {
            Ok(d) => { log::debug!("Seed: deal '{}' id={}", title, d.id); }
            Err(e) => { log::error!("Seed: deal '{}': {}", title, e); }
        }
    }
    log::info!("Seed: created {} deals", deal_data.len());

    // ── Budget Entries ─────────────────────────────────────────
    let budget_data: Vec<(&str, f64, frontend::direct_access::BudgetEntryType, i64)> = vec![
        ("Laptop procurement batch", 51750.00, frontend::direct_access::BudgetEntryType::Purchase, 5),
        ("Office supplies restock", 3200.00, frontend::direct_access::BudgetEntryType::Purchase, 12),
        ("Steel sheets delivery", 7750.00, frontend::direct_access::BudgetEntryType::Purchase, 8),
        ("Packaging materials", 1300.00, frontend::direct_access::BudgetEntryType::Purchase, 3),
        ("Furniture order", 45000.00, frontend::direct_access::BudgetEntryType::Purchase, 45),
        ("Cleaning supplies", 2400.00, frontend::direct_access::BudgetEntryType::Purchase, 20),
        ("Safety equipment", 4500.00, frontend::direct_access::BudgetEntryType::Purchase, 30),
        ("Tool replacements", 6200.00, frontend::direct_access::BudgetEntryType::Purchase, 60),
        ("Product sales - electronics", 28500.00, frontend::direct_access::BudgetEntryType::Sale, 7),
        ("Product sales - office", 8900.00, frontend::direct_access::BudgetEntryType::Sale, 14),
        ("Product sales - tools", 5600.00, frontend::direct_access::BudgetEntryType::Sale, 21),
        ("Product sales - furniture", 18200.00, frontend::direct_access::BudgetEntryType::Sale, 35),
        ("Product sales - safety gear", 3800.00, frontend::direct_access::BudgetEntryType::Sale, 10),
        ("Warehouse electricity", 4200.00, frontend::direct_access::BudgetEntryType::Expense, 2),
        ("Shipping costs", 3100.00, frontend::direct_access::BudgetEntryType::Expense, 6),
        ("Insurance premium", 8500.00, frontend::direct_access::BudgetEntryType::Expense, 90),
        ("Equipment maintenance", 2800.00, frontend::direct_access::BudgetEntryType::Expense, 15),
        ("Staff training", 1500.00, frontend::direct_access::BudgetEntryType::Expense, 40),
        ("Rent - main warehouse", 12000.00, frontend::direct_access::BudgetEntryType::Expense, 1),
        ("Rent - downtown store", 5500.00, frontend::direct_access::BudgetEntryType::Expense, 1),
        ("Product sales - packaging", 2100.00, frontend::direct_access::BudgetEntryType::Sale, 25),
        ("Product sales - raw materials", 9400.00, frontend::direct_access::BudgetEntryType::Sale, 18),
        ("Bulk electronics sale", 42000.00, frontend::direct_access::BudgetEntryType::Sale, 50),
        ("Utilities - east DC", 3800.00, frontend::direct_access::BudgetEntryType::Expense, 1),
        ("Marketing materials", 2200.00, frontend::direct_access::BudgetEntryType::Expense, 55),
        // Older entries for multi-month trends
        ("Laptop procurement Q4", 38000.00, frontend::direct_access::BudgetEntryType::Purchase, 120),
        ("Office supplies Q4", 4800.00, frontend::direct_access::BudgetEntryType::Purchase, 105),
        ("Product sales Q4 electronics", 22000.00, frontend::direct_access::BudgetEntryType::Sale, 110),
        ("Product sales Q4 furniture", 15000.00, frontend::direct_access::BudgetEntryType::Sale, 95),
        ("Rent Q4 warehouse", 12000.00, frontend::direct_access::BudgetEntryType::Expense, 100),
        ("Shipping Q4", 2800.00, frontend::direct_access::BudgetEntryType::Expense, 115),
        ("Steel purchase Q4", 9200.00, frontend::direct_access::BudgetEntryType::Purchase, 130),
        ("Product sales Q3 tools", 7500.00, frontend::direct_access::BudgetEntryType::Sale, 150),
        ("Packaging purchase Q3", 3200.00, frontend::direct_access::BudgetEntryType::Purchase, 160),
        ("Rent Q3 warehouse", 12000.00, frontend::direct_access::BudgetEntryType::Expense, 140),
        ("Product sales Q3 raw materials", 11000.00, frontend::direct_access::BudgetEntryType::Sale, 170),
        ("Equipment Q3", 5500.00, frontend::direct_access::BudgetEntryType::Purchase, 180),
        ("Utilities Q3", 3600.00, frontend::direct_access::BudgetEntryType::Expense, 155),
    ];

    for (desc, amount, entry_type, days_ago) in &budget_data {
        let dto = frontend::direct_access::CreateBudgetEntryDto {
            created_at: now, updated_at: now,
            entry_type: entry_type.clone(), amount: *amount,
            description: desc.to_string(),
            entry_date: now - Duration::days(*days_ago),
            product: None, deal: None, recorded_by: None,
        };
        match frontend::commands::budget_entry_commands::create_budget_entry(ctx, &dto, 1, -1) {
            Ok(b) => { log::debug!("Seed: budget entry '{}' id={}", desc, b.id); }
            Err(e) => { log::error!("Seed: budget entry '{}': {}", desc, e); }
        }
    }
    log::info!("Seed: created {} budget entries", budget_data.len());

    // ── Stock Movements ───────────────────────────────────────
    use frontend::common::entities::MovementType;
    let movement_data: Vec<(usize, MovementType, i64, Option<usize>, Option<usize>, &str, i64)> = vec![
        // (product_idx, type, qty, from_loc_idx, to_loc_idx, note, days_ago)
        (0, MovementType::Inbound, 20, None, Some(0), "Initial laptop shipment", 28),
        (1, MovementType::Inbound, 100, None, Some(1), "Mouse restock delivery", 25),
        (2, MovementType::Inbound, 50, None, Some(0), "USB-C hubs received", 22),
        (6, MovementType::Inbound, 200, None, Some(0), "Paper delivery", 20),
        (15, MovementType::Inbound, 500, None, Some(2), "Steel sheets bulk delivery", 18),
        (19, MovementType::Inbound, 1000, None, Some(0), "Corrugated boxes shipment", 15),
        (0, MovementType::Outbound, 5, Some(0), None, "Laptops shipped to client", 12),
        (1, MovementType::Outbound, 30, Some(1), None, "Mouse order fulfilled", 10),
        (6, MovementType::Outbound, 80, Some(0), None, "Paper distributed to offices", 8),
        (15, MovementType::Outbound, 200, Some(2), None, "Steel used in production", 6),
        (19, MovementType::Outbound, 400, Some(0), None, "Boxes used for shipping", 4),
        (0, MovementType::Transfer, 10, Some(0), Some(1), "Laptops to downtown store", 3),
        (1, MovementType::Transfer, 50, Some(1), Some(3), "Mice to west coast hub", 2),
        (3, MovementType::Inbound, 30, None, Some(2), "Keyboards received", 14),
        (4, MovementType::Inbound, 15, None, Some(0), "Monitors delivered", 16),
        (11, MovementType::Inbound, 10, None, Some(3), "Standing desks arrived", 20),
        (12, MovementType::Inbound, 15, None, Some(3), "Office chairs delivered", 19),
        (23, MovementType::Inbound, 50, None, Some(0), "Cleaner restock", 11),
        (26, MovementType::Inbound, 75, None, Some(0), "Safety goggles shipment", 9),
        (30, MovementType::Inbound, 20, None, Some(3), "Cordless drills received", 7),
        (3, MovementType::Outbound, 10, Some(2), None, "Keyboards sold", 5),
        (4, MovementType::Outbound, 5, Some(0), None, "Monitors shipped", 3),
        (23, MovementType::Outbound, 20, Some(0), None, "Cleaner distributed", 2),
        (11, MovementType::Transfer, 5, Some(3), Some(5), "Desks to south outlet", 1),
        (26, MovementType::Return, 10, Some(0), None, "Defective goggles returned", 1),
    ];

    for (prod_idx, mt, qty, from_idx, to_idx, note, days_ago) in &movement_data {
        let prod_id = product_ids.get(*prod_idx).copied();
        let from_loc = from_idx.and_then(|i| location_ids.get(i).copied());
        let to_loc = to_idx.and_then(|i| location_ids.get(i).copied());
        let ts = now - Duration::days(*days_ago);
        let dto = frontend::direct_access::CreateStockMovementDto {
            created_at: ts, updated_at: ts,
            movement_type: mt.clone(), quantity: *qty,
            note: note.to_string(),
            product: prod_id, from_location: from_loc, to_location: to_loc,
            performed_by: None,
        };
        match frontend::commands::stock_movement_commands::create_stock_movement(ctx, &dto, 1, -1) {
            Ok(m) => { log::debug!("Seed: movement id={}", m.id); }
            Err(e) => { log::error!("Seed: movement '{}': {}", note, e); }
        }
    }
    log::info!("Seed: created {} stock movements", movement_data.len());

    // ── Additional Users ──────────────────────────────────────
    let user_data = [
        ("manager1", "Manager1Pass", "Alice Johnson", "Manager"),
        ("operator1", "Operator1Pass", "Dave Wilson", "Operator"),
        ("viewer1", "Viewer1Pass!", "Eve Brown", "Viewer"),
    ];

    for (username, password, display_name, role) in &user_data {
        let user_role = match *role {
            "Manager" => frontend::user_management::dtos::CreateUserRole::Manager,
            "Operator" => frontend::user_management::dtos::CreateUserRole::Operator,
            "Viewer" => frontend::user_management::dtos::CreateUserRole::Viewer,
            _ => frontend::user_management::dtos::CreateUserRole::Admin,
        };
        let dto = frontend::user_management::CreateUserDto {
            username: username.to_string(),
            password: password.to_string(),
            display_name: display_name.to_string(),
            role: user_role,
            person_id: 0,
        };
        match frontend::commands::user_management_commands::create_user(ctx, &dto) {
            Ok(r) => { log::debug!("Seed: user '{}' id={}", username, r.user_id); }
            Err(e) => { log::error!("Seed: user '{}': {}", username, e); }
        }
    }
    log::info!("Seed: created {} additional users", user_data.len());

    log::info!("Seed: database population complete!");
}
