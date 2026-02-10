package org.chaos.office.service;

import org.chaos.office.model.PartImportData;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Common interface for file parsing implementations.
 * Parsers extract part data from various file formats (CSV, Excel) and convert them
 * into PartImportData objects for validation and import processing.
 * 
 * Requirements: 3.1, 3.2
 */
public interface FileParser {
    /**
     * Parses a file and extracts part data from each row.
     * 
     * @param file The file to parse (CSV or Excel format)
     * @return List of PartImportData objects, one per data row (excluding headers)
     * @throws IOException If the file cannot be read or is corrupted
     */
    List<PartImportData> parse(File file) throws IOException;
}
