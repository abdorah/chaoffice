//! Contact CRUD FFI functions.

use crate::dtos::{FfiContactDto, FfiCreateContactDto, FfiUpdateContactDto};
use crate::error::FfiError;
use crate::get_app_context;
use frontend::commands::contact_commands;

/// Create a new contact.
///
/// Converts the FFI DTO to the internal `CreateContactDto`, delegates to
/// `contact_commands::create_orphan_contact`, and converts the result back.
#[uniffi::export]
pub fn mobile_create_contact(dto: FfiCreateContactDto) -> Result<FfiContactDto, FfiError> {
    let ctx = get_app_context()?;
    let create_dto = dto.into();
    let contact = contact_commands::create_orphan_contact(ctx, None, &create_dto)
        .map_err(FfiError::from)?;
    Ok(contact.into())
}

/// Get a single contact by ID.
///
/// Returns `None` if the contact does not exist.
#[uniffi::export]
pub fn mobile_get_contact(id: i64) -> Result<Option<FfiContactDto>, FfiError> {
    let ctx = get_app_context()?;
    let contact = contact_commands::get_contact(ctx, &id).map_err(FfiError::from)?;
    Ok(contact.map(|c| c.into()))
}

/// Get all contacts.
#[uniffi::export]
pub fn mobile_get_all_contacts() -> Result<Vec<FfiContactDto>, FfiError> {
    let ctx = get_app_context()?;
    let contacts = contact_commands::get_all_contact(ctx).map_err(FfiError::from)?;
    Ok(contacts.into_iter().map(|c| c.into()).collect())
}

/// Update an existing contact.
///
/// Converts the FFI DTO to the internal `UpdateContactDto`, delegates to
/// `contact_commands::update_contact`, and converts the result back.
#[uniffi::export]
pub fn mobile_update_contact(dto: FfiUpdateContactDto) -> Result<FfiContactDto, FfiError> {
    let ctx = get_app_context()?;
    let update_dto = dto.into();
    let contact =
        contact_commands::update_contact(ctx, None, &update_dto).map_err(FfiError::from)?;
    Ok(contact.into())
}

/// Remove a contact by ID.
#[uniffi::export]
pub fn mobile_remove_contact(id: i64) -> Result<(), FfiError> {
    let ctx = get_app_context()?;
    contact_commands::remove_contact(ctx, None, &id).map_err(FfiError::from)?;
    Ok(())
}
