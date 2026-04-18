# Requirements Document

## Introduction

This document specifies the core inventory management operations for the Inventory Management Application built with Rust, Qleany, and Slint UI. The feature covers four custom use cases in the `inventory_management` feature group: importing products from CSV/JSON files, exporting products to CSV/JSON files, checking stock levels against a threshold, and transferring stock between locations. Qleany generates the CRUD infrastructure (controllers, repositories, DTOs) for Product, Category, Location, and Person entities; this spec covers the custom business logic layered on top.

## Glossary

- **Import_Handler**: The use case handler responsible for parsing CSV or JSON files and creating Product entities in the database, running as a long operation on a background thread.
- **Export_Handler**: The use case handler responsible for serializing Product entities (with their related Category, Location, and Person data) to CSV or JSON files.
- **Stock_Checker**: The use case handler responsible for querying all Products and returning those whose quantity falls below a given threshold.
- **Transfer_Handler**: The use case handler responsible for moving a specified quantity of a product from one Location to another, with undo support via Qleany's snapshot mechanism.
- **Product**: An inventory item entity with name, reference, description, quantity, price_unit, status (Available/OutOfStock/Discontinued/Reserved), and relationships to Category, Person (supplier), and Location.
- **Category**: A hierarchical classification entity with name, description, self-referencing parent_category, and ordered subcategories.
- **Location**: A storage site entity with name, address, latitude, longitude, and capacity.
- **Person**: A business contact entity with name, role (Manager/Supplier), and a one-to-one Contact relationship.
- **ImportFormat**: An enum with values Csv and Json specifying the input file format for product import.
- **ExportFormat**: An enum with values Csv and Json specifying the output file format for product export.
- **StockMovement**: An audit entity recording stock changes with movement_type, quantity, product, from/to locations, and performing user.
- **RBAC_Engine**: The role-based access control enforcement layer from the `inventory_security_macros` crate that gates use case execution via `#[require_permission]`.

## Requirements

### Requirement 1: Import Products from File

**User Story:** As a manager, I want to import products from a CSV or JSON file, so that I can bulk-load inventory data without manual entry.

#### Acceptance Criteria

1. WHEN a user provides a valid file path and ImportFormat of Csv, THE Import_Handler SHALL parse the CSV file and create Product entities for each valid row.
2. WHEN a user provides a valid file path and ImportFormat of Json, THE Import_Handler SHALL parse the JSON file and create Product entities for each valid entry.
3. WHEN the import completes, THE Import_Handler SHALL return an ImportProductsReturnDto containing the imported_count (number of successfully created Products), skipped_count (number of rows that failed validation), and error_messages (one descriptive message per skipped row).
4. WHEN a CSV row or JSON entry contains a category name that matches an existing Category, THE Import_Handler SHALL associate the created Product with that Category.
5. WHEN a CSV row or JSON entry contains a category name that does not match any existing Category, THE Import_Handler SHALL skip that row, increment skipped_count, and add an error message identifying the unknown category.
6. WHEN a CSV row or JSON entry contains a supplier name that matches an existing Person with role Supplier, THE Import_Handler SHALL associate the created Product with that Person as supplier.
7. WHEN a CSV row or JSON entry contains a supplier name that does not match any existing Person with role Supplier, THE Import_Handler SHALL skip that row, increment skipped_count, and add an error message identifying the unknown supplier.
8. WHEN a CSV row or JSON entry contains a location name that matches an existing Location, THE Import_Handler SHALL associate the created Product with that Location.
9. WHEN a CSV row or JSON entry contains a location name that does not match any existing Location, THE Import_Handler SHALL skip that row, increment skipped_count, and add an error message identifying the unknown location.
10. WHEN a CSV row or JSON entry is missing required fields (name or reference), THE Import_Handler SHALL skip that row, increment skipped_count, and add an error message identifying the missing fields.
11. WHEN the file path does not exist or is not readable, THE Import_Handler SHALL return an error without creating any Products.
12. WHEN the file content does not match the specified ImportFormat (malformed CSV or invalid JSON), THE Import_Handler SHALL return an error without creating any Products.
13. WHEN a non-authorized user attempts to import products, THE RBAC_Engine SHALL reject the operation with an AccessDenied error (requires `import:*` permission).

### Requirement 2: Export Products to File

**User Story:** As a manager, I want to export products to a CSV or JSON file, so that I can share inventory data or create backups.

#### Acceptance Criteria

1. WHEN a user provides a valid output path and ExportFormat of Csv, THE Export_Handler SHALL serialize all Product entities with their associated Category name, Location name, and supplier Person name into a CSV file at the specified path.
2. WHEN a user provides a valid output path and ExportFormat of Json, THE Export_Handler SHALL serialize all Product entities with their associated Category name, Location name, and supplier Person name into a JSON file at the specified path.
3. WHEN the export completes, THE Export_Handler SHALL return an ExportProductsReturnDto containing the exported_count (number of Products written).
4. WHEN a Product has no associated Category, Location, or supplier, THE Export_Handler SHALL write empty strings for those fields in the output file.
5. WHEN the output path is not writable, THE Export_Handler SHALL return an error without creating a partial file.
6. WHEN a non-authorized user attempts to export products, THE RBAC_Engine SHALL reject the operation with an AccessDenied error (requires `export:*` permission).

### Requirement 3: Import/Export Round-Trip Consistency

**User Story:** As a manager, I want exported data to be re-importable, so that I can reliably back up and restore inventory data.

#### Acceptance Criteria

1. FOR ALL sets of Product entities with valid Category, Location, and supplier associations, exporting to a format and then importing from the same file SHALL produce Product entities with equivalent name, reference, description, quantity, price_unit, and status values.
2. THE Export_Handler SHALL write a CSV header row or JSON schema that matches the field names expected by the Import_Handler.
3. THE Export_Handler SHALL format numeric fields (quantity, price_unit) and enum fields (status) in a manner that the Import_Handler parses without error.

### Requirement 4: Check Stock Levels

**User Story:** As any authorized user, I want to check which products are below a given stock threshold, so that I can identify items that need restocking.

#### Acceptance Criteria

1. WHEN a user provides a threshold value, THE Stock_Checker SHALL query all Product entities and return those whose quantity is strictly less than the threshold.
2. WHEN the Stock_Checker returns results, THE Stock_Checker SHALL provide parallel arrays of product_id, product_names, and quantities, where each index corresponds to the same Product.
3. WHEN no Products have a quantity below the threshold, THE Stock_Checker SHALL return empty arrays.
4. WHEN the threshold is zero or negative, THE Stock_Checker SHALL return empty arrays (no product has negative quantity).
5. WHEN a non-authorized user attempts to check stock levels, THE RBAC_Engine SHALL reject the operation with an AccessDenied error (requires `product:read` permission).

### Requirement 5: Transfer Stock Between Locations

**User Story:** As an operator, I want to transfer a quantity of a product from one location to another, so that I can redistribute inventory across warehouses.

#### Acceptance Criteria

1. WHEN a user provides a valid product_id, from_location_id, to_location_id, and a positive quantity, THE Transfer_Handler SHALL decrease the product quantity at the source location by the specified amount and increase the product quantity at the destination location by the same amount.
2. WHEN a transfer completes successfully, THE Transfer_Handler SHALL return a TransferStockReturnDto with success set to true, new_quantity_at_source, and new_quantity_at_dest.
3. WHEN the requested transfer quantity exceeds the available quantity at the source location, THE Transfer_Handler SHALL reject the transfer and return an error indicating insufficient stock.
4. WHEN the transfer quantity is zero or negative, THE Transfer_Handler SHALL reject the transfer and return a validation error.
5. WHEN the from_location_id equals the to_location_id, THE Transfer_Handler SHALL reject the transfer and return a validation error indicating source and destination must differ.
6. WHEN the product_id does not reference an existing Product, THE Transfer_Handler SHALL return an error indicating the product was not found.
7. WHEN the from_location_id or to_location_id does not reference an existing Location, THE Transfer_Handler SHALL return an error indicating the location was not found.
8. WHEN a transfer is executed, THE Transfer_Handler SHALL record a StockMovement entity with movement_type Transfer, the transferred quantity, the product, from_location, to_location, and the performing user.
9. WHEN a transfer is undone via Qleany's undo mechanism, THE Transfer_Handler SHALL restore the original quantities at both locations.
10. WHEN a non-authorized user attempts to transfer stock, THE RBAC_Engine SHALL reject the operation with an AccessDenied error (requires `stock:*` permission).

### Requirement 6: Import as Long Operation

**User Story:** As a user importing a large file, I want the import to run on a background thread with progress reporting, so that the UI remains responsive.

#### Acceptance Criteria

1. WHEN an import operation starts, THE Import_Handler SHALL execute on a background thread as a Qleany long_operation.
2. WHEN the import is in progress, THE Import_Handler SHALL report progress as a percentage based on rows processed versus total rows.
3. WHEN the import completes or fails, THE Import_Handler SHALL emit a completion signal that the UI can observe to update its state.

### Requirement 7: CSV and JSON File Format Specification

**User Story:** As a developer, I want clearly defined file formats for import and export, so that the system handles data consistently.

#### Acceptance Criteria

1. THE Export_Handler SHALL write CSV files with the following column order: name, reference, description, quantity, price_unit, status, category_name, supplier_name, location_name.
2. THE Import_Handler SHALL expect CSV files with a header row matching the column names: name, reference, description, quantity, price_unit, status, category_name, supplier_name, location_name.
3. THE Export_Handler SHALL write JSON files as an array of objects, each containing fields: name, reference, description, quantity, price_unit, status, category_name, supplier_name, location_name.
4. THE Import_Handler SHALL expect JSON files as an array of objects with the same field names as the export format.
5. THE Import_Handler SHALL parse the status field as one of the ProductStatus enum values (Available, OutOfStock, Discontinued, Reserved) and reject rows with unrecognized status values.
