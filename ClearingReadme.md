# Clearing Service

## Overview

The Clearing Service is the platform's financial interchange gateway. It exchanges finalized transaction information
with external financial parties — card networks, clearing houses, correspondent banks, and regulatory bodies — after
transactions have been authorized and captured. Clearing is the bridge between the platform's internal transaction
records and the external financial infrastructure that ultimately moves money and settles obligations.

Unlike authorization (which checks funds) or settlement (which disburses to merchants), clearing is the **formal
exchange of transaction data** that enables net settlement calculations, interchange fee assessments, and regulatory
transparency. The service ensures that every cleared transaction is formatted correctly for its destination, tracked
through confirmation, and fully auditable for reconciliation and compliance purposes.

---

## Table of Contents

- [Responsibilities](#responsibilities)
- [Clearing Lifecycle](#clearing-lifecycle)
- [Core Functionalities](#core-functionalities)
    - [Clearing File Generation](#clearing-file-generation)
    - [Clearing Batch Management](#clearing-batch-management)
    - [Network-Specific Message Formatting](#network-specific-message-formatting)
    - [Outbound Clearing Submissions](#outbound-clearing-submissions)
    - [Inbound Clearing Acknowledgements](#inbound-clearing-acknowledgements)
    - [Clearing Status Tracking](#clearing-status-tracking)
    - [Clearing Corrections and Adjustments](#clearing-corrections-and-adjustments)
    - [Regulatory Reporting](#regulatory-reporting)
    - [File Validation](#file-validation)
    - [Retry and Resubmission](#retry-and-resubmission)
    - [Audit Trail](#audit-trail)
    - [Clearing Reports](#clearing-reports)
- [Clearing Participants & Rails](#clearing-participants--rails)
- [Owned Resources](#owned-resources)
- [Domain Events](#domain-events)
- [Integration Notes](#integration-notes)

---

## Responsibilities

| Concern                            | Description                                                                                                                                       |
|------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| **Interchange Data Exchange**      | Transmit finalized transaction data to card networks, clearing houses, and banks in the precise formats they require.                             |
| **Net Settlement Enablement**      | Provide the transaction-level detail that enables networks and clearing houses to compute net settlement positions between acquirers and issuers. |
| **Regulatory Transparency**        | Submit required transaction reports to regulators, central banks, and financial intelligence units under AML, PSD2, and local mandates.           |
| **Format Compliance**              | Ensure all outbound files adhere to network specifications (ISO 8583, ISO 20022, NACHA, etc.) with zero tolerance for structural errors.          |
| **Exception Resolution**           | Handle rejections, corrections, and adjustments from clearing participants with automated retry and manual escalation workflows.                  |
| **Audit & Reconciliation Support** | Maintain an immutable record of every cleared transaction, file submission, and acknowledgement for reconciliation and dispute evidence.          |

---

## Clearing Lifecycle

```
CAPTURED TRANSACTIONS
        │
        ▼
┌─────────────────────────┐
│ 1. AGGREGATION          │ ──► Group transactions by network,
│    & BATCHING           │     rail, currency, and cut-off time
└─────────────────────────┘
        │
        ▼
┌─────────────────────────┐
│ 2. FILE GENERATION      │ ──► Format transactions into network-
│    & VALIDATION         │     specific clearing files (ISO 8583,
│                         │     ISO 20022, NACHA, etc.)
└─────────────────────────┘
        │
        ▼
┌─────────────────────────┐
│ 3. SUBMISSION           │ ──► Transmit files to clearing
│    (OUTBOUND)           │     participants via SFTP, API,
│                         │     SWIFT, or network gateway
└─────────────────────────┘
        │
        ├──► ACCEPTED ──► 4. ACKNOWLEDGEMENT ──► CLOSED
        │
        ├──► REJECTED ──► 5. CORRECTION ──► RESUBMISSION
        │
        └──► PENDING ──► AWAITING ACKNOWLEDGEMENT
```

**Clearing States:**

| State            | Description                                                                            |
|------------------|----------------------------------------------------------------------------------------|
| **PENDING**      | Transaction queued for clearing; awaiting batch formation.                             |
| **BATCHED**      | Transaction assigned to a clearing batch; file generation in progress.                 |
| **GENERATED**    | Clearing file created and validated; ready for submission.                             |
| **SUBMITTED**    | File transmitted to clearing participant; awaiting acknowledgement.                    |
| **ACKNOWLEDGED** | Clearing participant confirmed receipt and structural validity of the file.            |
| **ACCEPTED**     | Clearing participant accepted all transactions for clearing and settlement.            |
| **REJECTED**     | Clearing participant rejected the file or specific transactions; corrections required. |
| **CORRECTED**    | Rejected file or transactions have been corrected and resubmitted.                     |
| **SETTLED**      | Transactions have been included in the net settlement cycle; funds movement initiated. |
| **FAILED**       | Unrecoverable error after max retries; requires manual intervention.                   |

---

## Core Functionalities

### Clearing File Generation

Transforms internal transaction records into the standardized file formats required by clearing participants.

**Supported File Formats:**

| Format               | Standard                                       | Used By                                 | Description                                               |
|----------------------|------------------------------------------------|-----------------------------------------|-----------------------------------------------------------|
| **ISO 8583**         | Financial transaction card-originated messages | Visa, Mastercard, most card networks    | Binary/text message format for authorization and clearing |
| **ISO 20022**        | Universal financial industry message scheme    | SEPA, SWIFT gpi, modern clearing houses | XML-based, rich data model for payments and securities    |
| **NACHA**            | National Automated Clearing House Association  | US ACH clearing                         | Fixed-width text format for batch debit/credit entries    |
| **BAI2**             | Bank Administration Institute                  | US banks                                | Fixed-width format for cash management and reconciliation |
| **MT940 / MT942**    | SWIFT Message Types                            | International banks                     | End-of-day and intra-day account statements               |
| **CAMT.053**         | Cash Management                                | SEPA, European banks                    | ISO 20022-based account reporting                         |
| **CSV / XML / JSON** | Custom                                         | Fintechs, modern APIs                   | Proprietary formats for API-based clearing partners       |

**File Content:**

| Data Element            | Description                                                                                    |
|-------------------------|------------------------------------------------------------------------------------------------|
| **Header Record**       | File identifier, creation timestamp, sender/receiver IDs, batch count                          |
| **Transaction Records** | Individual cleared transactions with PAN/token, amount, currency, MCC, authorization code, ARN |
| **Trailer Record**      | Batch totals, hash totals, record counts for integrity verification                            |
| **Control Records**     | File-level checksums, digital signatures for non-repudiation                                   |

**Generation Rules:**

- Transactions are grouped by clearing cycle cut-off time (e.g., 18:00 UTC for next-day clearing).
- Files are generated in the participant's preferred format and encoding.
- Sensitive data (PANs) is replaced with network tokens or truncated per PCI requirements.
- Files are digitally signed before transmission.

---

### Clearing Batch Management

Organizes transactions into batches optimized for clearing efficiency and participant requirements.

**Batching Dimensions:**

| Dimension            | Description                                          | Example                                  |
|----------------------|------------------------------------------------------|------------------------------------------|
| **Network**          | Separate batches per card network or clearing house  | Visa batch, Mastercard batch, ACH batch  |
| **Currency**         | Separate batches per settlement currency             | USD batch, EUR batch, GBP batch          |
| **Merchant**         | Merchant-specific batches for large-volume merchants | Merchant A daily batch                   |
| **Transaction Type** | Separate batches by transaction category             | Sales, refunds, chargebacks, adjustments |
| **Cut-Off Time**     | Batches aligned with participant processing windows  | Before 15:00 CET for same-day SEPA       |
| **Risk Profile**     | Separate high-risk transactions for enhanced review  | High-risk merchant batch                 |

**Batch Properties:**

| Property           | Description                                          |
|--------------------|------------------------------------------------------|
| `batchId`          | Unique identifier (e.g., `CLR-20260717-VISA-001`)    |
| `participantId`    | Clearing participant (network, bank, clearing house) |
| `fileFormat`       | ISO 8583, ISO 20022, NACHA, etc.                     |
| `transactionCount` | Number of transactions in the batch                  |
| `totalAmount`      | Sum of transaction amounts                           |
| `currency`         | Settlement currency                                  |
| `status`           | PENDING, GENERATED, SUBMITTED, ACCEPTED, REJECTED    |
| `cutOffTime`       | Deadline for submission to participant               |
| `createdAt`        | Batch creation timestamp                             |
| `submittedAt`      | File submission timestamp                            |

**Batch Lifecycle:**

1. **Formation** — Transactions captured since last batch are aggregated.
2. **Validation** — File format, data completeness, and integrity checks.
3. **Signing** — Digital signature applied for non-repudiation.
4. **Submission** — File transmitted to clearing participant.
5. **Acknowledgement** — Participant confirms receipt and validity.
6. **Settlement** — Batch included in net settlement cycle.
7. **Archival** — File retained for regulatory period (typically 7+ years).

---

### Network-Specific Message Formatting

Ensures every message conforms to the exact specifications of the target clearing network.

**Card Network Clearing Formats:**

| Network        | Format                                  | Key Fields                                                         | Submission Method    |
|----------------|-----------------------------------------|--------------------------------------------------------------------|----------------------|
| **Visa**       | BASE II, Visa DPS                       | TC 33 (sales), TC 34 (refunds), TC 35 (chargebacks), ARN, BIN      | Visa Online, VSS API |
| **Mastercard** | IPM (Integrated Product Messages), GCMS | DE 2 (PAN), DE 4 (amount), DE 22 (POS data), DE 37 (retrieval ref) | GCMS, MDES API       |
| **Amex**       | OptBlue, Amex Direct                    | Transaction type, charge amount, merchant ID, authorization code   | Amex API, SFTP       |
| **Discover**   | DFS (Discover Financial Services)       | Transaction identifier, amount, MCC, authorization code            | DFS API, SFTP        |
| **UnionPay**   | CUPS (China UnionPay System)            | Card number, transaction amount, merchant category, trace number   | CUPS gateway         |

**Bank Clearing Formats:**

| Rail          | Format             | Key Fields                                                  | Submission Method          |
|---------------|--------------------|-------------------------------------------------------------|----------------------------|
| **ACH (US)**  | NACHA              | Entry detail, trace number, RDFI, amount, transaction code  | FedACH, The Clearing House |
| **SEPA (EU)** | ISO 20022 pacs.008 | Instructed amount, creditor/debtor IBAN, BIC, end-to-end ID | STEP2, RT1                 |
| **FPS (UK)**  | ISO 20022 pain.001 | Amount, sort code, account number, reference                | Pay.UK API                 |
| **SWIFT**     | MT103 / pacs.008   | Sender/receiver BIC, amount, currency, charges              | SWIFT network              |
| **Fedwire**   | ISO 20022          | Amount, sender/receiver FI, beneficiary account             | Federal Reserve            |

**Formatting Rules:**

- Field lengths, data types, and encoding must match network specifications exactly.
- Amounts are formatted in minor currency units (cents) with no decimal points.
- Dates follow network-specific formats (YYMMDD, YYYYMMDD, ISO 8601).
- Character sets are restricted to ASCII or EBCDIC per network requirements.
- Truncation rules apply to free-text fields (e.g., merchant name max 25 chars for Visa).

---

### Outbound Clearing Submissions

Transmits generated clearing files to external participants through secure channels.

**Submission Channels:**

| Channel                | Protocol                   | Security                                | Use Case                                             |
|------------------------|----------------------------|-----------------------------------------|------------------------------------------------------|
| **SFTP / FTPS**        | SSH File Transfer Protocol | TLS 1.3, certificate auth, IP allowlist | Batch file exchange with banks and networks          |
| **REST API**           | HTTPS / JSON or XML        | OAuth 2.0, mTLS, API keys               | Real-time clearing with modern fintechs and networks |
| **SOAP API**           | HTTPS / XML                | WS-Security, X.509 certificates         | Legacy bank and network integrations                 |
| **SWIFT Network**      | SWIFTNet FIN / gpi         | SWIFT PKI, HSM signing                  | International wire and correspondent banking         |
| **Network Gateway**    | Proprietary binary         | Network-specific encryption             | Visa, Mastercard direct connections                  |
| **File Upload Portal** | HTTPS web upload           | MFA, session encryption                 | Manual or exception handling submissions             |

**Submission Flow:**

1. **Pre-Submission Check** — Verify batch is complete, validated, and signed.
2. **Channel Selection** — Choose optimal channel based on participant, urgency, and file size.
3. **Transmission** — Upload or send file via selected channel.
4. **Receipt Confirmation** — Capture participant's receipt acknowledgment (message ID, timestamp).
5. **Status Update** — Transition batch to `SUBMITTED` state.
6. **Timeout Monitoring** — If no acknowledgement within SLA, trigger retry or alert.

**Submission SLAs:**

| Participant     | Expected Acknowledgement | Retry Threshold | Escalation        |
|-----------------|--------------------------|-----------------|-------------------|
| Visa BASE II    | 2 hours                  | 30 minutes      | Operations team   |
| Mastercard GCMS | 2 hours                  | 30 minutes      | Operations team   |
| ACH (FedACH)    | 4 hours                  | 1 hour          | Operations team   |
| SEPA (STEP2)    | 1 hour                   | 15 minutes      | Operations team   |
| SWIFT           | 30 minutes               | 10 minutes      | Senior operations |

---

### Inbound Clearing Acknowledgements

Processes responses from clearing participants confirming receipt, validity, and acceptance of submitted files.

**Acknowledgement Types:**

| Type                              | Description                                                 | Action Required                                       |
|-----------------------------------|-------------------------------------------------------------|-------------------------------------------------------|
| **Receipt Acknowledgement (ACK)** | Participant received the file; structural validation passed | None; await acceptance                                |
| **Acceptance Confirmation (ACC)** | All transactions accepted for clearing and settlement       | Update status to `ACCEPTED`; proceed to settlement    |
| **Rejection Notice (REJ)**        | File or specific transactions rejected                      | Parse rejection reason; initiate correction workflow  |
| **Partial Acceptance**            | Some transactions accepted, others rejected                 | Split batch; resubmit rejected items after correction |
| **Settlement Confirmation**       | Transactions included in net settlement cycle               | Update status to `SETTLED`; notify Settlement Service |

**Rejection Reasons:**

| Code                  | Reason                                          | Resolution                                                     |
|-----------------------|-------------------------------------------------|----------------------------------------------------------------|
| `FORMAT_ERROR`        | File does not conform to specification          | Correct formatting and regenerate file                         |
| `INVALID_FIELD`       | Specific field contains invalid data            | Correct data element and resubmit                              |
| `DUPLICATE_BATCH`     | Batch ID already processed                      | Verify batch uniqueness; resubmit with new ID if genuinely new |
| `MISSING_TRANSACTION` | Expected transaction not found in file          | Investigate missing transaction; regenerate batch              |
| `AMOUNT_MISMATCH`     | Transaction amount does not match authorization | Verify capture amount; correct if error                        |
| `INVALID_ARN`         | Acquirer Reference Number not found             | Verify ARN mapping; correct or escalate                        |
| `NETWORK_TIMEOUT`     | Participant system timeout                      | Retry submission after brief delay                             |
| `AUTH_EXPIRED`        | Authorization expired before clearing           | Void transaction internally; do not resubmit                   |

---

### Clearing Status Tracking

Provides real-time visibility into the status of every clearing batch and individual transaction.

**Status Query Capabilities:**

| Query                                                 | Returns                                                               |
|-------------------------------------------------------|-----------------------------------------------------------------------|
| `GET /clearing/batches/{batchId}`                     | Full batch details, file metadata, submission history, current status |
| `GET /clearing/batches?status=SUBMITTED`              | All batches awaiting acknowledgement                                  |
| `GET /clearing/batches?participant=VISA`              | All batches for a specific network                                    |
| `GET /clearing/transactions/{transactionId}/clearing` | Clearing status for a specific transaction                            |
| `GET /clearing/batches/{batchId}/rejections`          | All rejected transactions with reason codes                           |

**Status Dashboard Metrics:**

| Metric                           | Target     | Description                                              |
|----------------------------------|------------|----------------------------------------------------------|
| **Submission Success Rate**      | > 99.5%    | Percentage of files accepted on first submission         |
| **Average Acknowledgement Time** | < 1 hour   | Mean time from submission to participant acknowledgement |
| **Rejection Rate**               | < 0.1%     | Percentage of transactions rejected by participants      |
| **Correction Resolution Time**   | < 4 hours  | Mean time from rejection to successful resubmission      |
| **Settlement Lag**               | < 24 hours | Time from capture to inclusion in settlement cycle       |

---

### Clearing Corrections and Adjustments

Manages the correction of rejected or erroneous clearing submissions.

**Correction Workflow:**

1. **Detection** — Receive rejection notice from participant or detect internal error.
2. **Classification** — Categorize rejection reason (format, data, duplicate, timeout).
3. **Root Cause Analysis** — Identify whether error is in source data, formatting, or transmission.
4. **Correction** — Fix the underlying issue (regenerate file, correct data, retry transmission).
5. **Resubmission** — Submit corrected file or transactions with original batch reference.
6. **Verification** — Confirm participant acceptance of corrected submission.
7. **Audit** — Log correction reason, original batch, corrected batch, and resolution time.

**Adjustment Types:**

| Type                     | Description                                            | Example                                   |
|--------------------------|--------------------------------------------------------|-------------------------------------------|
| **Data Correction**      | Fix incorrect field value                              | Wrong MCC, incorrect amount               |
| **Format Correction**    | Regenerate file in correct format                      | Missing trailer record, wrong encoding    |
| **Transaction Removal**  | Remove a transaction from the batch                    | Authorization expired before clearing     |
| **Transaction Addition** | Add a missing transaction to the batch                 | Transaction captured after batch cut-off  |
| **Batch Split**          | Divide a rejected batch into valid and invalid subsets | Partial rejection by participant          |
| **Batch Merge**          | Combine multiple small batches into one                | Optimization for participant requirements |

---

### Regulatory Reporting

Submits transaction-level and aggregate reports to regulatory bodies as required by jurisdiction.

**Regulatory Reporting Requirements:**

| Jurisdiction       | Regulation   | Report Type                       | Frequency       | Content                                          |
|--------------------|--------------|-----------------------------------|-----------------|--------------------------------------------------|
| **European Union** | PSD2 / RTS   | Transaction reporting             | Daily           | All SEPA transactions with SCA indicators        |
| **United States**  | BSA / FinCEN | CTR (Currency Transaction Report) | Daily           | Cash transactions > $10,000                      |
| **United States**  | BSA / FinCEN | SAR (Suspicious Activity Report)  | As needed       | Suspicious transaction patterns                  |
| **United Kingdom** | FCA          | Regulatory returns                | Monthly         | Payment volume, fraud rates, operational metrics |
| **India**          | RBI          | Payment system data               | Daily           | UPI, NEFT, RTGS transaction details              |
| **Singapore**      | MAS          | Payment data                      | Quarterly       | Transaction volume, merchant categories          |
| **Global**         | FATF         | Cross-border wire transfers       | Per transaction | Originator and beneficiary information           |

**Reporting Flow:**

1. **Data Aggregation** — Collect transactions matching regulatory criteria from the reporting period.
2. **Format Conversion** — Transform internal data into regulator-specified format (XML, CSV, XBRL, etc.).
3. **Validation** — Validate against regulator's schema and business rules.
4. **Approval** — Compliance officer reviews and approves the report.
5. **Submission** — Transmit to regulator via secure portal, API, or email.
6. **Acknowledgement** — Capture regulator's receipt confirmation.
7. **Archival** — Retain report and supporting data for regulatory period (typically 5–7 years).

---

### File Validation

Ensures every clearing file meets structural, data, and business rule requirements before submission.

**Validation Layers:**

| Layer                        | Checks                                                                                  | Failure Action                                  |
|------------------------------|-----------------------------------------------------------------------------------------|-------------------------------------------------|
| **Structural Validation**    | File format conformance, record lengths, field counts, header/trailer presence          | Reject file; do not submit                      |
| **Data Type Validation**     | Numeric fields contain numbers, date fields are valid dates, character sets are correct | Flag field; attempt auto-correction or reject   |
| **Business Rule Validation** | Amounts match captured amounts, ARNs are valid, MCCs are recognized                     | Flag transaction; quarantine for review         |
| **Referential Integrity**    | Transactions reference valid merchants, valid authorizations, valid tokens              | Flag transaction; investigate missing reference |
| **Checksum Validation**      | Hash totals, record counts, digital signatures match                                    | Reject file; regenerate                         |
| **Duplicate Detection**      | Batch ID and transaction IDs are unique                                                 | Reject duplicate; reference existing batch      |

**Validation Pipeline:**

```
File Upload / Generation
        │
        ▼
┌─────────────────┐
│ Structural      │ ──► FAIL → Reject, alert, regenerate
│ Validation      │
└─────────────────┘
        │ PASS
        ▼
┌─────────────────┐
│ Data Type       │ ──► FAIL → Auto-correct or flag for review
│ Validation      │
└─────────────────┘
        │ PASS
        ▼
┌─────────────────┐
│ Business Rule   │ ──► FAIL → Quarantine transaction, alert ops
│ Validation      │
└─────────────────┘
        │ PASS
        ▼
┌─────────────────┐
│ Referential     │ ──► FAIL → Investigate missing reference
│ Integrity       │
└─────────────────┘
        │ PASS
        ▼
┌─────────────────┐
│ Checksum &      │ ──► FAIL → Reject, regenerate
│ Signature       │
└─────────────────┘
        │ PASS
        ▼
   READY FOR SUBMISSION
```

---

### Retry and Resubmission

Handles transient failures and rejected submissions through automated retry logic.

**Retry Scenarios:**

| Scenario                   | Retry Strategy                     | Max Attempts | Backoff                              |
|----------------------------|------------------------------------|--------------|--------------------------------------|
| **Network timeout**        | Exponential backoff                | 5            | 1 min, 5 min, 15 min, 30 min, 60 min |
| **Participant busy**       | Fixed interval                     | 3            | 10 minutes                           |
| **File format rejection**  | No retry; regenerate and resubmit  | N/A          | N/A                                  |
| **Data rejection**         | No retry; correct and resubmit     | N/A          | N/A                                  |
| **Authentication failure** | No retry; alert security team      | N/A          | N/A                                  |
| **Duplicate detection**    | No retry; verify and update status | N/A          | N/A                                  |

**Resubmission Rules:**

- Resubmitted files must reference the original batch ID for traceability.
- Only corrected files or transactions are resubmitted; do not resubmit known-bad data.
- Resubmission count is tracked per batch; batches with > 3 resubmissions are escalated.
- Automatic resubmission is paused during participant maintenance windows.

---

### Audit Trail

Maintains a complete, immutable record of every clearing activity for compliance, reconciliation, and dispute
resolution.

**Audit Log Content:**

| Field           | Description                                                                             |
|-----------------|-----------------------------------------------------------------------------------------|
| `auditId`       | Unique identifier for the audit entry                                                   |
| `batchId`       | Reference to the clearing batch                                                         |
| `transactionId` | Reference to the specific transaction (if applicable)                                   |
| `action`        | CREATED, GENERATED, SUBMITTED, ACKNOWLEDGED, ACCEPTED, REJECTED, CORRECTED, RESUBMITTED |
| `actor`         | System service or user who performed the action                                         |
| `timestamp`     | UTC timestamp of the action                                                             |
| `participantId` | Clearing participant involved                                                           |
| `fileName`      | Name of the clearing file                                                               |
| `fileHash`      | Cryptographic hash of the file for integrity verification                               |
| `reasonCode`    | Reason for rejection or correction (if applicable)                                      |
| `beforeState`   | State before the action                                                                 |
| `afterState`    | State after the action                                                                  |

**Audit Retention:**

- Active audit logs: 2 years in hot storage (queryable in real-time)
- Archive audit logs: 7 years in cold storage (retrievable within 24 hours)
- Regulatory audit logs: 10 years in immutable storage (cryptographically signed)

---

### Clearing Reports

Generates operational, financial, and regulatory reports on clearing activity.

**Report Types:**

| Report                          | Audience                 | Content                                                                     |
|---------------------------------|--------------------------|-----------------------------------------------------------------------------|
| **Daily Clearing Summary**      | Operations               | Batches submitted, acceptance rate, rejection reasons, pending items        |
| **Network Performance Report**  | Operations / Management  | Per-network submission success rates, acknowledgement times, settlement lag |
| **Rejection Analysis**          | Operations / Engineering | Top rejection reasons, affected merchants, trend analysis                   |
| **Settlement Reconciliation**   | Finance                  | Cleared transactions vs. settled amounts, variance analysis                 |
| **Regulatory Submission Log**   | Compliance               | All regulatory reports submitted, acknowledgements, deadlines               |
| **Merchant Clearing Statement** | Merchant                 | Per-merchant clearing activity, fees, settlement timing                     |

**Report Formats:**

- JSON (API)
- CSV (download)
- PDF (merchant-facing statements)
- XBRL (regulatory filings)

---

## Clearing Participants & Rails

| Participant / Rail           | Region         | Clearing Type              | Settlement Timing |
|------------------------------|----------------|----------------------------|-------------------|
| **VisaNet**                  | Global         | Card network               | T+1 to T+2        |
| **Mastercard Network**       | Global         | Card network               | T+1 to T+2        |
| **Amex Network**             | Global         | Card network               | T+1 to T+3        |
| **Discover Network**         | Global         | Card network               | T+1 to T+2        |
| **UnionPay (CUPS)**          | China / Global | Card network               | T+1               |
| **FedACH**                   | US             | Bank clearing              | T+1 to T+2        |
| **The Clearing House (TCH)** | US             | Bank clearing              | Same day (RTP)    |
| **SEPA (STEP2)**             | EU             | Bank clearing              | T+1               |
| **SEPA Instant (RT1)**       | EU             | Bank clearing              | < 10 seconds      |
| **FPS (UK)**                 | UK             | Bank clearing              | < 15 seconds      |
| **BACS**                     | UK             | Bank clearing              | T+3               |
| **CHAPS**                    | UK             | Bank clearing              | Same day          |
| **SWIFT**                    | Global         | Correspondent banking      | T+1 to T+5        |
| **Fedwire**                  | US             | Real-time gross settlement | Same day          |
| **Target2**                  | EU             | Real-time gross settlement | Same day          |

---

## Owned Resources

The Clearing Service is the authoritative owner of the following data:

| Resource                          | Description                                                                                                                         |
|-----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| **Clearing Batches**              | Aggregated collections of transactions prepared for clearing, including batch metadata, participant routing, and lifecycle state.   |
| **Clearing Files**                | Generated clearing files in network-specific formats, including file content, checksums, digital signatures, and transmission logs. |
| **Clearing Status**               | Real-time and historical status of every batch and transaction through the clearing lifecycle.                                      |
| **Regulatory Submission Records** | All reports submitted to regulatory bodies, including content, timestamps, acknowledgements, and compliance status.                 |

> **Note:** Transaction data is owned by the Payment Service. Settlement execution is owned by the Settlement Service.
> Network token data is owned by the Tokenization Service. The Clearing Service formats and exchanges data but does not
> own the underlying business records.

---

## Domain Events

The Clearing Service publishes the following events for downstream consumers:

| Event                       | Trigger                                                                                                              |
|-----------------------------|----------------------------------------------------------------------------------------------------------------------|
| `ClearingBatchCreated`      | A new clearing batch is formed from captured transactions, ready for file generation.                                |
| `ClearingSubmitted`         | A clearing file is successfully transmitted to the external clearing participant.                                    |
| `ClearingAccepted`          | The clearing participant acknowledges receipt and accepts all transactions in the batch for clearing and settlement. |
| `ClearingRejected`          | The clearing participant rejects the file or specific transactions, requiring correction and resubmission.           |
| `RegulatoryReportSubmitted` | A regulatory report is successfully transmitted to the relevant regulatory body.                                     |

---

## Integration Notes

- **Payment Service**: Consumes captured transaction events to include transactions in the next clearing batch; receives
  transaction metadata (ARN, authorization codes, MCC) for file generation.
- **Settlement Service**: Receives `ClearingAccepted` events to schedule merchant settlements based on net settlement
  positions; coordinates settlement timing with clearing cycles.
- **Reconciliation Service**: Receives clearing file hashes, submission logs, and acknowledgement records for automated
  reconciliation against participant statements.
- **Tokenization Service**: Requests token-to-PAN mapping or network token data for clearing file generation (where
  network requires PAN or token-specific formatting).
- **Merchant Service**: Retrieves merchant configuration (MCC, merchant ID formats, settlement preferences) for clearing
  file content and routing.
- **Ledger Service**: Posts clearing-related journal entries (interchange accruals, settlement liabilities) upon batch
  acceptance.
- **Audit Service**: Subscribes to all clearing events for compliance logging, regulatory evidence, and forensic
  investigation.
- **Notification Service**: Alerts operations teams of rejections, delays, and SLA breaches; sends merchant
  notifications of clearing status.
- **Regulatory Bodies**: Direct integration for report submission via secure portals, APIs, or file exchange protocols.
- **Card Networks / Clearing Houses**: Direct integration via SFTP, API, SWIFT, or network gateways for file submission
  and acknowledgement processing.
