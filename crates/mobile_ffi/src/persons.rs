//! Person CRUD FFI functions.

use crate::dtos::{FfiCreatePersonDto, FfiPersonDto, FfiUpdatePersonDto};
use crate::error::FfiError;
use crate::get_app_context;
use frontend::commands::person_commands;

/// Create a new person.
///
/// Converts the FFI DTO to the internal `CreatePersonDto`, delegates to
/// `person_commands::create_orphan_person`, and converts the result back.
#[uniffi::export]
pub fn mobile_create_person(dto: FfiCreatePersonDto) -> Result<FfiPersonDto, FfiError> {
    let ctx = get_app_context()?;
    let create_dto = dto.into();
    let person = person_commands::create_orphan_person(ctx, None, &create_dto)
        .map_err(FfiError::from)?;
    Ok(person.into())
}

/// Get a single person by ID.
///
/// Returns `None` if the person does not exist.
#[uniffi::export]
pub fn mobile_get_person(id: u64) -> Result<Option<FfiPersonDto>, FfiError> {
    let ctx = get_app_context()?;
    let person = person_commands::get_person(ctx, &id).map_err(FfiError::from)?;
    Ok(person.map(|p| p.into()))
}

/// Get all persons.
#[uniffi::export]
pub fn mobile_get_all_persons() -> Result<Vec<FfiPersonDto>, FfiError> {
    let ctx = get_app_context()?;
    let persons = person_commands::get_all_person(ctx).map_err(FfiError::from)?;
    Ok(persons.into_iter().map(|p| p.into()).collect())
}

/// Update an existing person.
///
/// Converts the FFI DTO to the internal `UpdatePersonDto`, delegates to
/// `person_commands::update_person`, and converts the result back.
#[uniffi::export]
pub fn mobile_update_person(dto: FfiUpdatePersonDto) -> Result<FfiPersonDto, FfiError> {
    let ctx = get_app_context()?;
    let update_dto = dto.into();
    let person =
        person_commands::update_person(ctx, None, &update_dto).map_err(FfiError::from)?;
    Ok(person.into())
}

/// Remove a person by ID.
#[uniffi::export]
pub fn mobile_remove_person(id: u64) -> Result<(), FfiError> {
    let ctx = get_app_context()?;
    person_commands::remove_person(ctx, None, &id).map_err(FfiError::from)?;
    Ok(())
}
