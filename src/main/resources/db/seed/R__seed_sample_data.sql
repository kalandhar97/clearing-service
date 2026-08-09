-- ============================================================================
-- REPEATABLE seed migration (Flyway "R__" prefix).
--
-- Why repeatable rather than versioned:
--   * Repeatable migrations always run AFTER every versioned migration, so this
--     file can never be "out of order". A versioned seed applied only under the
--     local profile broke as soon as the service had already been started without
--     it - Flyway then refused with
--     "Detected resolved migration not applied to database: <n>".
--   * Flyway re-applies a repeatable migration whenever its checksum changes, so
--     editing the sample data below just works on the next start; no manual
--     flyway_schema_history surgery needed.
--
-- Every statement is therefore written to be IDEMPOTENT (ON CONFLICT DO NOTHING),
-- because this file runs again on every checksum change and must never fail with
-- a duplicate-key error.
--
-- Only loaded when the "local" Spring profile is active (see application.yml:
-- spring.flyway.locations = classpath:db/migration,classpath:db/seed).
-- ============================================================================

-- ---------------------------------------------------------------------
-- clearing_batch
-- ---------------------------------------------------------------------
INSERT INTO clearing_batch (
    id, reference, network, currency, region, settlement_date, format, status,
    transaction_count, total_amount_minor, cutoff_at, submission_attempts,
    next_attempt_at, ack_reference, last_error,
    created_at, validated_at, sent_at, acknowledged_at, completed_at, failed_at,
    updated_at, version
) VALUES
('11111111-1111-1111-1111-111111111111', 'BATCH-VISA-20260720-0001', 'VISA', 'USD', 'US', DATE '2026-07-20', 'ISO8583', 'COMPLETED',
    2, 15000, TIMESTAMPTZ '2026-07-20 20:00:00+00', 1,
    NULL, 'ACK-REF-0001', NULL,
    TIMESTAMPTZ '2026-07-20 10:00:00+00', TIMESTAMPTZ '2026-07-20 10:05:00+00', TIMESTAMPTZ '2026-07-20 10:10:00+00',
    TIMESTAMPTZ '2026-07-20 10:20:00+00', TIMESTAMPTZ '2026-07-20 10:25:00+00', NULL,
    TIMESTAMPTZ '2026-07-20 10:25:00+00', 0),
('11111111-1111-1111-1111-111111111112', 'BATCH-MC-20260721-0001', 'MASTERCARD', 'USD', 'US', DATE '2026-07-21', 'ISO8583', 'ACKNOWLEDGED',
    1, 5000, TIMESTAMPTZ '2026-07-21 20:00:00+00', 1,
    NULL, 'ACK-REF-0002', NULL,
    TIMESTAMPTZ '2026-07-21 11:00:00+00', TIMESTAMPTZ '2026-07-21 11:05:00+00', TIMESTAMPTZ '2026-07-21 11:10:00+00',
    TIMESTAMPTZ '2026-07-21 11:20:00+00', NULL, NULL,
    TIMESTAMPTZ '2026-07-21 11:20:00+00', 0),
('11111111-1111-1111-1111-111111111113', 'BATCH-ACH-20260722-0001', 'ACH', 'USD', 'US', DATE '2026-07-22', 'NACHA', 'SENT',
    1, 25000, TIMESTAMPTZ '2026-07-22 20:00:00+00', 1,
    TIMESTAMPTZ '2026-07-22 12:15:00+00', NULL, NULL,
    TIMESTAMPTZ '2026-07-22 12:00:00+00', TIMESTAMPTZ '2026-07-22 12:05:00+00', TIMESTAMPTZ '2026-07-22 12:10:00+00',
    NULL, NULL, NULL,
    TIMESTAMPTZ '2026-07-22 12:10:00+00', 0),
('11111111-1111-1111-1111-111111111114', 'BATCH-SEPA-20260723-0001', 'SEPA', 'EUR', 'EU', DATE '2026-07-23', 'ISO20022', 'VALIDATED',
    0, 0, TIMESTAMPTZ '2026-07-23 20:00:00+00', 0,
    NULL, NULL, NULL,
    TIMESTAMPTZ '2026-07-23 09:00:00+00', TIMESTAMPTZ '2026-07-23 09:05:00+00', NULL,
    NULL, NULL, NULL,
    TIMESTAMPTZ '2026-07-23 09:05:00+00', 0),
('11111111-1111-1111-1111-111111111115', 'BATCH-VISA-20260724-0002', 'VISA', 'USD', 'US', DATE '2026-07-24', 'ISO8583', 'FAILED',
    1, 7500, TIMESTAMPTZ '2026-07-24 20:00:00+00', 5,
    NULL, 'ACK-REF-0003', 'Participant rejected file: invalid control total',
    TIMESTAMPTZ '2026-07-24 13:00:00+00', TIMESTAMPTZ '2026-07-24 13:05:00+00', TIMESTAMPTZ '2026-07-24 13:10:00+00',
    TIMESTAMPTZ '2026-07-24 13:20:00+00', NULL, TIMESTAMPTZ '2026-07-24 13:25:00+00',
    TIMESTAMPTZ '2026-07-24 13:25:00+00', 0)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- clearing_transaction
-- ---------------------------------------------------------------------
INSERT INTO clearing_transaction (
    id, source_transaction_id, merchant_id, network, transaction_type, currency,
    amount_minor, region, settlement_date, mcc, auth_code, arn, pan_token,
    captured_at, status, rejection_reason, batch_id, created_at, updated_at, version
) VALUES
('22222222-2222-2222-2222-222222222221', 'SRC-TXN-0001', 'MERCH-0001', 'VISA', 'SALE', 'USD',
    10000, 'US', DATE '2026-07-20', '5411', 'AUTH01', 'ARN00000000001', 'PANTOK-0001',
    TIMESTAMPTZ '2026-07-20 09:00:00+00', 'CLEARED', NULL, '11111111-1111-1111-1111-111111111111',
    TIMESTAMPTZ '2026-07-20 09:01:00+00', TIMESTAMPTZ '2026-07-20 10:25:00+00', 0),
('22222222-2222-2222-2222-222222222222', 'SRC-TXN-0002', 'MERCH-0002', 'VISA', 'SALE', 'USD',
    5000, 'US', DATE '2026-07-20', '5812', 'AUTH02', 'ARN00000000002', 'PANTOK-0002',
    TIMESTAMPTZ '2026-07-20 09:10:00+00', 'CLEARED', NULL, '11111111-1111-1111-1111-111111111111',
    TIMESTAMPTZ '2026-07-20 09:11:00+00', TIMESTAMPTZ '2026-07-20 10:25:00+00', 0),
('22222222-2222-2222-2222-222222222223', 'SRC-TXN-0003', 'MERCH-0003', 'MASTERCARD', 'REFUND', 'USD',
    5000, 'US', DATE '2026-07-21', '5411', 'AUTH03', 'ARN00000000003', 'PANTOK-0003',
    TIMESTAMPTZ '2026-07-21 10:00:00+00', 'CLEARED', NULL, '11111111-1111-1111-1111-111111111112',
    TIMESTAMPTZ '2026-07-21 10:01:00+00', TIMESTAMPTZ '2026-07-21 11:20:00+00', 0),
('22222222-2222-2222-2222-222222222224', 'SRC-TXN-0004', 'MERCH-0004', 'ACH', 'SALE', 'USD',
    25000, 'US', DATE '2026-07-22', NULL, NULL, NULL, 'PANTOK-0004',
    TIMESTAMPTZ '2026-07-22 11:30:00+00', 'BATCHED', NULL, '11111111-1111-1111-1111-111111111113',
    TIMESTAMPTZ '2026-07-22 11:31:00+00', TIMESTAMPTZ '2026-07-22 12:10:00+00', 0),
('22222222-2222-2222-2222-222222222225', 'SRC-TXN-0005', 'MERCH-0005', 'SEPA', 'SALE', 'EUR',
    12000, 'EU', DATE '2026-07-25', NULL, NULL, NULL, 'PANTOK-0005',
    TIMESTAMPTZ '2026-07-25 08:00:00+00', 'PENDING', NULL, NULL,
    TIMESTAMPTZ '2026-07-25 08:01:00+00', TIMESTAMPTZ '2026-07-25 08:01:00+00', 0),
('22222222-2222-2222-2222-222222222226', 'SRC-TXN-0006', 'MERCH-0006', 'VISA', 'SALE', 'USD',
    7500, 'US', DATE '2026-07-24', '5411', 'AUTH06', 'ARN00000000006', 'PANTOK-0006',
    TIMESTAMPTZ '2026-07-24 12:00:00+00', 'REJECTED', 'Invalid control total on submitted file', '11111111-1111-1111-1111-111111111115',
    TIMESTAMPTZ '2026-07-24 12:01:00+00', TIMESTAMPTZ '2026-07-24 13:25:00+00', 0)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- clearing_file
-- ---------------------------------------------------------------------
INSERT INTO clearing_file (
    id, batch_id, format, file_name, content_hash, size_bytes, record_count,
    control_total_minor, storage_uri, signature, generated_at
) VALUES
('44444444-4444-4444-4444-444444444441', '11111111-1111-1111-1111-111111111111', 'ISO8583', 'BATCH-VISA-20260720-0001.iso8583',
    'a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f9', 4096, 2,
    15000, 'file:///clearing-files/BATCH-VISA-20260720-0001.iso8583', 'SIG-0001', TIMESTAMPTZ '2026-07-20 10:10:00+00'),
('44444444-4444-4444-4444-444444444442', '11111111-1111-1111-1111-111111111112', 'ISO8583', 'BATCH-MC-20260721-0001.iso8583',
    'b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f9a1', 2048, 1,
    5000, 'file:///clearing-files/BATCH-MC-20260721-0001.iso8583', 'SIG-0002', TIMESTAMPTZ '2026-07-21 11:10:00+00')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- clearing_acknowledgement
-- ---------------------------------------------------------------------
INSERT INTO clearing_acknowledgement (
    id, batch_id, ack_reference, status, reason_code, message, raw_payload, received_at
) VALUES
('33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111111', 'ACK-REF-0001', 'ACCEPTED', NULL,
    'All transactions accepted', '{"result":"ACCEPTED"}', TIMESTAMPTZ '2026-07-20 10:20:00+00'),
('33333333-3333-3333-3333-333333333332', '11111111-1111-1111-1111-111111111112', 'ACK-REF-0002', 'ACKNOWLEDGED', NULL,
    'File receipt confirmed', '{"result":"ACKNOWLEDGED"}', TIMESTAMPTZ '2026-07-21 11:20:00+00'),
('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111115', 'ACK-REF-0003', 'REJECTED', 'E4021',
    'File rejected: invalid control total', '{"result":"REJECTED","reason":"E4021"}', TIMESTAMPTZ '2026-07-24 13:20:00+00')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- audit_entry
-- ---------------------------------------------------------------------
INSERT INTO audit_entry (
    id, batch_id, transaction_id, action, actor, participant_id, file_name, file_hash,
    reason_code, before_state, after_state, detail, created_at
) VALUES
('55555555-5555-5555-5555-555555555551', '11111111-1111-1111-1111-111111111111', NULL, 'BATCH_VALIDATED', 'system', 'VISA',
    NULL, NULL, NULL, 'CREATED', 'VALIDATED', 'Batch validated prior to submission', TIMESTAMPTZ '2026-07-20 10:05:00+00'),
('55555555-5555-5555-5555-555555555552', '11111111-1111-1111-1111-111111111111', NULL, 'BATCH_SUBMITTED', 'system', 'VISA',
    'BATCH-VISA-20260720-0001.iso8583', 'a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f9',
    NULL, 'VALIDATED', 'SENT', 'Clearing file transmitted to participant', TIMESTAMPTZ '2026-07-20 10:10:00+00'),
('55555555-5555-5555-5555-555555555553', '11111111-1111-1111-1111-111111111111', NULL, 'BATCH_ACKNOWLEDGED', 'participant', 'VISA',
    NULL, NULL, NULL, 'SENT', 'ACKNOWLEDGED', 'Participant accepted all transactions', TIMESTAMPTZ '2026-07-20 10:20:00+00'),
('55555555-5555-5555-5555-555555555554', '11111111-1111-1111-1111-111111111115', NULL, 'BATCH_FAILED', 'participant', 'VISA',
    NULL, NULL, 'E4021', 'ACKNOWLEDGED', 'FAILED', 'Participant rejected file due to invalid control total', TIMESTAMPTZ '2026-07-24 13:25:00+00')
ON CONFLICT DO NOTHING;
