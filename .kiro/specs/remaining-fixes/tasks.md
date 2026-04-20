# Remaining Fixes

## Issue 1: Add User doesn't refresh / save
The Users page create-user callback fires but the user list doesn't refresh after creation.
- [x] 1.1 After create-user succeeds, call list_users and populate UsersPageAdapter.row-data
- [x] 1.2 Add a refresh_users_table helper function (like the other refresh_* functions)
- [x] 1.3 Wire the Users page table to UsersPageAdapter.row-data (verify binding)
- [x] 1.4 Auto-refresh users list after login (for Admin users)

## Issue 2: Sync page — verify it works
- [x] 2.1 Verify sync-to-remote and sync-from-remote callbacks update the UI status
- [x] 2.2 Show pending changes count on the Sync page (from ChangeTracker)
- [x] 2.3 Show "Online" status correctly (local-only mode should show "Local" not "Offline")

## Issue 3: Persist UI settings (window size, theme)
Save user preferences to a settings file so they persist across app restarts.
- [x] 3.1 Create a `ui_settings.toml` file format: window_width, window_height, dark_mode
- [x] 3.2 Load settings on app startup and apply to Window size and Palette.color-scheme
- [x] 3.3 Save settings on window close (capture current size and theme)
- [x] 3.4 Add `toml` crate dependency to slint_ui

## Issue 4: ComboBoxes not populated (categories empty when adding product)
The ComboBoxes for category/supplier/location are empty because populate_product_comboboxes is only called during refresh_products_table, which only runs after login. If no products exist yet, the refresh might not trigger the population.
- [x] 4.1 Call populate_product_comboboxes explicitly after login (not just inside refresh_products_table)
- [x] 4.2 Call populate_deal_comboboxes explicitly after login
- [x] 4.3 Ensure ComboBoxes refresh after creating a new category/person/location (so new items appear)

## Issue 5: Replace simple ComboBox with searchable selector for categories
A plain ComboBox doesn't scale for large lists. Replace with a filterable search pattern.
- [x] 5.1 Create a SearchableComboBox component: LineEdit + filtered ListView dropdown
- [x] 5.2 Replace category ComboBox in Products form with SearchableComboBox
- [x] 5.3 The component should filter items as the user types (case-insensitive substring match)

## Issue 6: Report generation — infinite loading, no output
The report callbacks only log the request but don't actually generate files. The progress indicator stays on forever.
- [x] 6.1 Implement actual report generation in the callbacks (call the reporting controller)
- [x] 6.2 Generate reports to a default output directory (e.g., `./reports/`)
- [x] 6.3 Create the output directory if it doesn't exist
- [x] 6.4 After generation, reset the `generating` flag to false
- [x] 6.5 Show the output file path to the user after successful generation
- [x] 6.6 Handle errors and show them to the user (reset generating flag on error too)
