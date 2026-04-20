# Polish Fixes

## Issue 1: PDF reports not generated
Excel and CSV work but PDF fails silently. The `genpdf` crate likely has an issue with font loading or the PdfWriter implementation.
- [x] 1.1 Debug the PDF writer — check if genpdf requires a font file to be present
- [x] 1.2 Add a bundled default font or use genpdf's built-in font
- [x] 1.3 Test PDF generation and show error message if it fails

## Issue 2: UI settings not persisting (theme resets to white)
The settings save/load logic exists but isn't working. Likely the dark_mode property isn't being applied correctly on startup, or the file isn't being written.
- [x] 2.1 Debug: add logging to UiSettings::load and UiSettings::save to verify file I/O
- [x] 2.2 Verify the dark_mode property is being read and applied to Palette.color-scheme
- [x] 2.3 Ensure the on_close_requested handler actually saves before exiting

## Issue 3: Products table shows category/location IDs instead of names
The refresh_products_table function shows raw entity IDs for category and location columns. Should resolve IDs to names.
- [x] 3.1 In refresh_products_table, look up category name from the category ID
- [x] 3.2 In refresh_products_table, look up location name from the location ID
- [x] 3.3 In refresh_products_table, look up supplier name from the supplier ID

## Issue 4: Sync page needs database settings UI
The sync page shows status but has no way to configure the TursoDB URL and auth token.
- [x] 4.1 Add a "Settings" section to the Sync page with LineEdit fields for TursoDB URL and auth token
- [x] 4.2 Add auto-sync toggle and interval input
- [x] 4.3 Wire the configure-sync callback to save these settings

## Issue 5: Add ScrollView to all pages for small screens
Pages don't scroll when the window is too small, hiding content.
- [x] 5.1 Wrap ProductsPage content in ScrollView
- [x] 5.2 Wrap CategoriesPage content in ScrollView
- [x] 5.3 Wrap PersonsPage content in ScrollView
- [x] 5.4 Wrap DealsPage content in ScrollView
- [x] 5.5 Wrap LocationsPage content in ScrollView
- [x] 5.6 Wrap StockTrackingPage content in ScrollView
- [x] 5.7 Wrap BudgetPage content in ScrollView
- [x] 5.8 Wrap ReportsPage content in ScrollView
- [x] 5.9 Wrap UsersPage content in ScrollView
- [x] 5.10 Wrap SyncPage content in ScrollView

## Issue 6: Add logout button
There's no visible logout button in the UI. The user can't log out.
- [x] 6.1 Add a logout button to the sidebar bottom area (or top-right of the app shell)
- [x] 6.2 Wire it to AppState.logout callback
- [x] 6.3 Show current user name next to the logout button
