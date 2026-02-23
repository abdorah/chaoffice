# Requirements Document: DGI E-Invoicing Integration

## Introduction

This document specifies the requirements for integrating Morocco's mandatory e-invoicing system with ChaOffice, a JavaFX-based parts inventory management system. The integration enables real-time invoice submission to the Direction Générale des Impôts (DGI) xHub platform for clearance, ensuring compliance with Morocco's 2026 e-invoicing mandate.

The system must generate invoices in UBL 2.1 XML format, submit them to DGI for clearance, handle offline scenarios through queueing, and provide clear status indicators to users while maintaining the existing billing workflow.

## Glossary

- **DGI**: Direction Générale des Impôts (Moroccan Tax Authority)
- **xHub**: DGI's e-invoicing clearance platform
- **UBL**: Universal Business Language (XML-based invoice format standard)
- **Clearance**: The process of DGI validating and approving an invoice
- **Invoice_Generator**: Component that converts bill data to UBL 2.1 XML format
- **Submission_Service**: Component that handles API communication with xHub
- **Queue_Manager**: Component that manages offline invoice queueing
- **Bill**: ChaOffice's internal invoice/billing record
- **Clearance_Code**: Unique identifier returned by DGI upon invoice approval
- **Tax_ID**: Moroccan business tax identification number (Identifiant Fiscal)
- **Configuration_Manager**: Component that stores and manages business and API settings
- **Audit_Logger**: Component that records all submission attempts and responses

## Requirements

### Requirement 1: UBL 2.1 XML Invoice Generation

**User Story:** As a shop owner, I want my bills to be automatically converted to the DGI-compliant format, so that they can be submitted for clearance without manual intervention.

#### Acceptance Criteria

1. WHEN a bill is created with all required fields, THE Invoice_Generator SHALL produce a valid UBL 2.1 XML document
2. WHEN generating UBL XML, THE Invoice_Generator SHALL include seller information from Configuration_Manager (Tax_ID, business name, address)
3. WHEN generating UBL XML, THE Invoice_Generator SHALL include buyer information from the bill (client name, phone)
4. WHEN generating UBL XML, THE Invoice_Generator SHALL include all line items from Command records with quantity and price
5. WHEN generating UBL XML, THE Invoice_Generator SHALL include totals (subtotal, discount, final total) matching the bill
6. WHEN generating UBL XML, THE Invoice_Generator SHALL include the bill date in ISO 8601 format
7. WHEN generating UBL XML, THE Invoice_Generator SHALL assign a sequential invoice number based on the bill ID
8. WHEN generating UBL XML, THE Invoice_Generator SHALL include payment method information
9. THE Invoice_Generator SHALL validate the generated XML against the UBL 2.1 schema before returning it

### Requirement 2: Real-Time Invoice Submission

**User Story:** As a shop owner, I want invoices to be submitted to DGI immediately after creation, so that I maintain compliance with real-time submission requirements.

#### Acceptance Criteria

1. WHEN a bill is successfully created, THE Submission_Service SHALL attempt to submit the UBL XML to xHub within 30 seconds
2. WHEN submitting to xHub, THE Submission_Service SHALL use HTTPS protocol for secure communication
3. WHEN submitting to xHub, THE Submission_Service SHALL include valid authentication credentials from Configuration_Manager
4. WHEN submitting to xHub, THE Submission_Service SHALL set appropriate HTTP headers including Content-Type for XML
5. WHEN xHub returns a success response, THE Submission_Service SHALL extract and store the Clearance_Code
6. WHEN xHub returns an error response, THE Submission_Service SHALL store the error details with the bill
7. WHEN network connectivity is unavailable, THE Submission_Service SHALL delegate to Queue_Manager for later retry

### Requirement 3: Clearance Status Management

**User Story:** As a shop owner, I need to see whether each invoice was approved or rejected by DGI, so that I can take corrective action if needed.

#### Acceptance Criteria

1. THE Bill SHALL store a clearance status field with values: PENDING, APPROVED, REJECTED, or QUEUED
2. WHEN a bill is first created, THE Bill SHALL have clearance status set to PENDING
3. WHEN xHub approves an invoice, THE Bill SHALL update clearance status to APPROVED and store the Clearance_Code
4. WHEN xHub rejects an invoice, THE Bill SHALL update clearance status to REJECTED and store the rejection reason
5. WHEN an invoice is queued for offline submission, THE Bill SHALL update clearance status to QUEUED
6. THE Bill SHALL store the submission timestamp for audit purposes
7. THE Bill SHALL store the xHub response payload for troubleshooting

### Requirement 4: Offline Queue Management

**User Story:** As a shop owner, I need the system to continue working when internet is unavailable, so that I can keep serving customers without interruption.

#### Acceptance Criteria

1. WHEN network connectivity fails during submission, THE Queue_Manager SHALL add the invoice to a persistent queue
2. WHEN an invoice is queued, THE Queue_Manager SHALL store the complete UBL XML and bill reference
3. WHEN network connectivity is restored, THE Queue_Manager SHALL automatically retry queued submissions in chronological order
4. WHEN a queued submission succeeds, THE Queue_Manager SHALL remove it from the queue and update the bill status
5. WHEN a queued submission fails after retry, THE Queue_Manager SHALL keep it in the queue for the next retry cycle
6. THE Queue_Manager SHALL attempt to process the queue every 5 minutes when items are present
7. THE Queue_Manager SHALL limit retry attempts to 10 times per invoice before marking it as failed

### Requirement 5: Business Configuration Management

**User Story:** As a shop owner, I need to configure my business tax information once, so that it's automatically included in all invoices.

#### Acceptance Criteria

1. THE Configuration_Manager SHALL store business Tax_ID as a required field
2. THE Configuration_Manager SHALL store business legal name as a required field
3. THE Configuration_Manager SHALL store business address (street, city, postal code) as required fields
4. THE Configuration_Manager SHALL store business phone number as an optional field
5. THE Configuration_Manager SHALL store business email as an optional field
6. THE Configuration_Manager SHALL validate Tax_ID format matches Moroccan standards (8 digits)
7. WHEN configuration is incomplete, THE Configuration_Manager SHALL prevent invoice submission and display a warning
8. THE Configuration_Manager SHALL persist all settings to SQLite database

### Requirement 6: Environment Configuration

**User Story:** As an admin, I need to test the integration in sandbox mode before going live, so that I can verify everything works without affecting production data.

#### Acceptance Criteria

1. THE Configuration_Manager SHALL support two environment modes: SANDBOX and PRODUCTION
2. WHEN in SANDBOX mode, THE Submission_Service SHALL use the DGI sandbox xHub endpoint URL
3. WHEN in PRODUCTION mode, THE Submission_Service SHALL use the DGI production xHub endpoint URL
4. THE Configuration_Manager SHALL store separate authentication credentials for each environment
5. THE Configuration_Manager SHALL display a clear visual indicator showing the current active environment
6. WHEN switching environments, THE Configuration_Manager SHALL require admin confirmation
7. THE Configuration_Manager SHALL default to SANDBOX mode on first installation

### Requirement 7: Invoice Export with Clearance Information

**User Story:** As a shop owner, I need to export and print invoices with the DGI clearance code, so that I can provide compliant documentation to customers.

#### Acceptance Criteria

1. WHEN exporting an APPROVED invoice, THE Bill SHALL include the Clearance_Code in the output
2. WHEN exporting an APPROVED invoice, THE Bill SHALL include the clearance timestamp
3. WHEN exporting a PENDING or QUEUED invoice, THE Bill SHALL display the current status clearly
4. WHEN exporting a REJECTED invoice, THE Bill SHALL display the rejection reason
5. THE Bill SHALL support export to PDF format with clearance information
6. THE Bill SHALL support printing with clearance information visible on the document

### Requirement 8: Audit Trail and Logging

**User Story:** As a business owner, I need a complete record of all invoice submissions and responses, so that I can demonstrate compliance during tax audits.

#### Acceptance Criteria

1. WHEN an invoice submission is attempted, THE Audit_Logger SHALL record the timestamp, bill ID, and submission status
2. WHEN xHub responds, THE Audit_Logger SHALL record the complete response payload
3. WHEN a submission fails, THE Audit_Logger SHALL record the error type and error message
4. THE Audit_Logger SHALL store all audit records in SQLite with no automatic deletion
5. THE Audit_Logger SHALL support querying audit records by date range
6. THE Audit_Logger SHALL support querying audit records by bill ID
7. THE Audit_Logger SHALL support exporting audit logs to CSV format

### Requirement 9: Invoice Amendment and Cancellation

**User Story:** As a shop owner, I need to cancel or amend invoices that were submitted incorrectly, so that I can correct mistakes and maintain accurate records.

#### Acceptance Criteria

1. WHEN an APPROVED invoice needs cancellation, THE Submission_Service SHALL submit a cancellation request to xHub
2. WHEN xHub approves a cancellation, THE Bill SHALL update status to CANCELLED and store the cancellation code
3. WHEN creating an amended invoice, THE Bill SHALL reference the original invoice ID
4. WHEN submitting an amended invoice, THE Submission_Service SHALL include the original Clearance_Code as a reference
5. THE Bill SHALL prevent modification of invoices with status APPROVED unless through the amendment process
6. THE Bill SHALL maintain a history of amendments linked to the original invoice

### Requirement 10: User Interface Integration

**User Story:** As a shop owner, I want to see clearance status directly in my billing interface, so that I can quickly identify any issues without navigating to separate screens.

#### Acceptance Criteria

1. WHEN viewing the bill list, THE UI SHALL display a clearance status indicator for each bill
2. WHEN a bill has status APPROVED, THE UI SHALL display a green checkmark icon
3. WHEN a bill has status REJECTED, THE UI SHALL display a red error icon
4. WHEN a bill has status PENDING, THE UI SHALL display a yellow pending icon
5. WHEN a bill has status QUEUED, THE UI SHALL display an orange queue icon
6. WHEN clicking on a status indicator, THE UI SHALL display detailed clearance information including timestamps and codes
7. WHEN viewing bill details, THE UI SHALL display the Clearance_Code prominently if available
8. WHEN a submission error occurs, THE UI SHALL display a user-friendly error message with suggested actions

### Requirement 11: Error Handling and Recovery

**User Story:** As a shop owner, I need the system to handle errors gracefully and provide clear guidance, so that I can resolve issues without technical expertise.

#### Acceptance Criteria

1. WHEN xHub returns a validation error, THE Submission_Service SHALL parse the error and display specific field issues
2. WHEN authentication fails, THE Submission_Service SHALL display a message prompting credential verification
3. WHEN xHub is unavailable (HTTP 5xx errors), THE Submission_Service SHALL automatically queue the invoice for retry
4. WHEN network timeout occurs, THE Submission_Service SHALL retry once immediately before queueing
5. WHEN XML generation fails, THE Invoice_Generator SHALL log the error and prevent submission
6. WHEN configuration is invalid, THE Configuration_Manager SHALL prevent bill creation and display configuration requirements
7. IF an invoice remains QUEUED for more than 24 hours, THEN THE UI SHALL display a prominent warning notification

### Requirement 12: Sequential Invoice Numbering

**User Story:** As a business owner, I need invoice numbers to be sequential and unique, so that I comply with tax regulations and maintain proper accounting records.

#### Acceptance Criteria

1. THE Invoice_Generator SHALL assign invoice numbers sequentially starting from 1
2. THE Invoice_Generator SHALL ensure no gaps in the invoice number sequence
3. THE Invoice_Generator SHALL ensure no duplicate invoice numbers are generated
4. WHEN the system is restarted, THE Invoice_Generator SHALL resume numbering from the last used number
5. THE Invoice_Generator SHALL store the current invoice counter in SQLite
6. THE Invoice_Generator SHALL use a transaction lock when assigning new invoice numbers to prevent race conditions
