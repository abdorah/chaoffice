# Final Fixes

## Issue 1: Sync settings lost when switching pages
The Sync page LineEdit fields (TursoDB URL, auth token) lose their values when navigating away and back. Slint destroys and recreates conditional page components on each navigation.
- [x] 1.1 Move sync settings fields to a global (SyncPageAdapter) so they persist across page switches
- [x] 1.2 Add properties: turso-url, turso-token, auto-sync-enabled, sync-interval to the adapter
- [x] 1.3 Bind the Sync page LineEdits to the adapter properties with two-way bindings
- [x] 1.4 On "Save Settings" click, read from the adapter (not from destroyed LineEdits)
- [x] 1.5 On app startup, populate the adapter from the loaded SyncConfig

## Issue 2: Theme and UI settings reset every rerun
The dark mode toggle updates AppSettings.dark-mode but the value isn't being read back correctly on startup, or the file isn't being written with the correct value.
- [x] 2.1 Add explicit logging: print the value of AppSettings.dark-mode right before saving
- [x] 2.2 Verify ui_settings.toml content after toggling dark mode and closing
- [x] 2.3 On startup, after setting AppSettings.dark-mode, also set Palette.color-scheme directly via a Slint callback
- [x] 2.4 Add an AppState callback `apply-dark-mode()` that sets Palette.color-scheme from AppSettings.dark-mode
- [x] 2.5 Call this callback from Rust after loading settings

## Issue 3: Add ability to edit a row (inline editing)
Currently entities can only be created and deleted. Add the ability to select a row and edit its fields.
- [x] 3.1 Add an "Edit" button or double-click handler on table rows for Products
- [x] 3.2 When editing, populate the create form with the selected row's data
- [x] 3.3 Change the form title to "Edit Product" and the button to "Save"
- [x] 3.4 On save, call update_product instead of create_product
- [x] 3.5 Add an AppState callback for update-product
- [x] 3.6 Wire the update callback to the direct_access update controller
- [x] 3.7 Repeat for Categories (edit name, description)
- [x] 3.8 Repeat for Persons (edit name, role)
- [x] 3.9 Repeat for Locations (edit name, address, capacity)
- [x] 3.10 Repeat for Deals (edit title, description, frequency, status)
