use crate::error::ImportError;
use crate::types::ProductRecord;
use std::fs::File;
use std::path::Path;

/// Parse a CSV file into ProductRecords.
/// Returns an error if the file is unreadable or structurally malformed.
pub fn parse_csv(file_path: &str) -> Result<Vec<ProductRecord>, ImportError> {
    let path = Path::new(file_path);
    if !path.exists() {
        return Err(ImportError::FileNotFound {
            path: file_path.to_string(),
        });
    }
    let file = File::open(path).map_err(|_| ImportError::FileNotReadable {
        path: file_path.to_string(),
    })?;
    let mut rdr = csv::ReaderBuilder::new()
        .has_headers(true)
        .from_reader(file);
    let mut records = Vec::new();
    for result in rdr.deserialize() {
        let record: ProductRecord = result.map_err(|e| ImportError::MalformedCsv {
            details: e.to_string(),
        })?;
        records.push(record);
    }
    Ok(records)
}

/// Parse a JSON file (array of objects) into ProductRecords.
/// Returns an error if the file is unreadable or not valid JSON.
pub fn parse_json(file_path: &str) -> Result<Vec<ProductRecord>, ImportError> {
    let path = Path::new(file_path);
    if !path.exists() {
        return Err(ImportError::FileNotFound {
            path: file_path.to_string(),
        });
    }
    let file = File::open(path).map_err(|_| ImportError::FileNotReadable {
        path: file_path.to_string(),
    })?;
    let records: Vec<ProductRecord> =
        serde_json::from_reader(file).map_err(|e| ImportError::InvalidJson {
            details: e.to_string(),
        })?;
    Ok(records)
}
