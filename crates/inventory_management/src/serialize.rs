use crate::error::ExportError;
use crate::types::ProductRecord;
use std::fs::File;
use std::path::Path;

/// Write ProductRecords to a CSV file with header row.
/// Column order: name, reference, description, quantity, price_unit, status,
///               category_name, supplier_name, location_name
pub fn write_csv(records: &[ProductRecord], output_path: &str) -> Result<(), ExportError> {
    let file = File::create(Path::new(output_path)).map_err(|_| ExportError::NotWritable {
        path: output_path.to_string(),
    })?;
    let mut wtr = csv::WriterBuilder::new().from_writer(file);
    for record in records {
        wtr.serialize(record)
            .map_err(|e| ExportError::SerializationError {
                details: e.to_string(),
            })?;
    }
    wtr.flush()
        .map_err(|e| ExportError::SerializationError {
            details: e.to_string(),
        })?;
    Ok(())
}

/// Write ProductRecords to a JSON file as an array of objects.
pub fn write_json(records: &[ProductRecord], output_path: &str) -> Result<(), ExportError> {
    let file = File::create(Path::new(output_path)).map_err(|_| ExportError::NotWritable {
        path: output_path.to_string(),
    })?;
    serde_json::to_writer_pretty(file, records).map_err(|e| ExportError::SerializationError {
        details: e.to_string(),
    })?;
    Ok(())
}
