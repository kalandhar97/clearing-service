-- =====================================================================
-- Clearing Service - initial schema
-- All monetary values are stored as integer minor units (e.g. cents).
-- All timestamps are stored as TIMESTAMPTZ (UTC).
-- =====================================================================

-- ---------------------------------------------------------------------
-- clearing_batch: aggregate root for a batch of transactions prepared
-- for a single clearing participant / settlement date.
-- Lifecycle: CREATED -> VALIDATED -> SENT -> ACKNOWLEDGED -> COMPLETED
--            (any state may transition to FAILED)
-- ---------------------------------------------------------------------
CREATE TABLE clearing_batch (
    id                  UUID         PRIMARY KEY,
    reference           VARCHAR(64)  NOT NULL,
    network             VARCHAR(32)  NOT NULL,
    currency            VARCHAR(3)   NOT NULL,
    region              VARCHAR(32),
    settlement_date     DATE         NOT NULL,
    format              VARCHAR(16)  NOT NULL,
    status              VARCHAR(24)  NOT NULL,
    transaction_count   INTEGER      NOT NULL DEFAULT 0,
    total_amount_minor  BIGINT       NOT NULL DEFAULT 0,
    cutoff_at           TIMESTAMPTZ,
    submission_attempts INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at     TIMESTAMPTZ,
    ack_reference       VARCHAR(128),
    last_error          VARCHAR(1024),
    created_at          TIMESTAMPTZ  NOT NULL,
    validated_at        TIMESTAMPTZ,
    sent_at             TIMESTAMPTZ,
    acknowledged_at     TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    failed_at           TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ  NOT NULL,
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_clearing_batch_reference UNIQUE (reference)
);

CREATE INDEX ix_clearing_batch_status        ON clearing_batch (status);
CREATE INDEX ix_clearing_batch_next_attempt  ON clearing_batch (status, next_attempt_at);
CREATE INDEX ix_clearing_batch_participant    ON clearing_batch (network, settlement_date);

-- ---------------------------------------------------------------------
-- clearing_transaction: an individual transaction eligible for clearing.
-- source_transaction_id is the idempotency key from the Payment Service.
-- ---------------------------------------------------------------------
CREATE TABLE clearing_transaction (
    id                    UUID        PRIMARY KEY,
    source_transaction_id VARCHAR(64) NOT NULL,
    merchant_id           VARCHAR(64) NOT NULL,
    network               VARCHAR(32) NOT NULL,
    transaction_type      VARCHAR(24) NOT NULL,
    currency              VARCHAR(3)  NOT NULL,
    amount_minor          BIGINT      NOT NULL,
    region                VARCHAR(32),
    settlement_date       DATE        NOT NULL,
    mcc                   VARCHAR(4),
    auth_code             VARCHAR(16),
    arn                   VARCHAR(32),
    pan_token             VARCHAR(64),
    captured_at           TIMESTAMPTZ,
    status                VARCHAR(24) NOT NULL,
    rejection_reason      VARCHAR(512),
    batch_id              UUID,
    created_at            TIMESTAMPTZ NOT NULL,
    updated_at            TIMESTAMPTZ NOT NULL,
    version               BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_clearing_txn_source UNIQUE (source_transaction_id),
    CONSTRAINT fk_clearing_txn_batch  FOREIGN KEY (batch_id) REFERENCES clearing_batch (id)
);

CREATE INDEX ix_clearing_txn_status   ON clearing_transaction (status);
CREATE INDEX ix_clearing_txn_batch    ON clearing_transaction (batch_id);
CREATE INDEX ix_clearing_txn_grouping ON clearing_transaction (status, network, currency, settlement_date);

-- ---------------------------------------------------------------------
-- clearing_file: the generated clearing file for a batch (one-to-one).
-- ---------------------------------------------------------------------
CREATE TABLE clearing_file (
    id                  UUID         PRIMARY KEY,
    batch_id            UUID         NOT NULL,
    format              VARCHAR(16)  NOT NULL,
    file_name           VARCHAR(255) NOT NULL,
    content_hash        VARCHAR(64)  NOT NULL,
    size_bytes          BIGINT       NOT NULL,
    record_count        INTEGER      NOT NULL,
    control_total_minor BIGINT       NOT NULL,
    storage_uri         VARCHAR(512) NOT NULL,
    signature           VARCHAR(512),
    generated_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_clearing_file_batch UNIQUE (batch_id),
    CONSTRAINT fk_clearing_file_batch FOREIGN KEY (batch_id) REFERENCES clearing_batch (id)
);

-- ---------------------------------------------------------------------
-- clearing_acknowledgement: inbound acks/rejections from participants.
-- ---------------------------------------------------------------------
CREATE TABLE clearing_acknowledgement (
    id            UUID         PRIMARY KEY,
    batch_id      UUID         NOT NULL,
    ack_reference VARCHAR(128),
    status        VARCHAR(24)  NOT NULL,
    reason_code   VARCHAR(64),
    message       VARCHAR(1024),
    raw_payload   TEXT,
    received_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_clearing_ack_batch FOREIGN KEY (batch_id) REFERENCES clearing_batch (id)
);

CREATE INDEX ix_clearing_ack_batch ON clearing_acknowledgement (batch_id);

-- ---------------------------------------------------------------------
-- outbox_event: transactional outbox for reliable event publication.
-- ---------------------------------------------------------------------
CREATE TABLE outbox_event (
    id             UUID         PRIMARY KEY,
    aggregate_type VARCHAR(64)  NOT NULL,
    aggregate_id   VARCHAR(64)  NOT NULL,
    event_type     VARCHAR(64)  NOT NULL,
    payload        TEXT         NOT NULL,
    status         VARCHAR(16)  NOT NULL,
    attempts       INTEGER      NOT NULL DEFAULT 0,
    last_error     VARCHAR(1024),
    created_at     TIMESTAMPTZ  NOT NULL,
    published_at   TIMESTAMPTZ
);

CREATE INDEX ix_outbox_status ON outbox_event (status, created_at);

-- ---------------------------------------------------------------------
-- audit_entry: immutable audit trail of clearing lifecycle actions.
-- ---------------------------------------------------------------------
CREATE TABLE audit_entry (
    id             UUID         PRIMARY KEY,
    batch_id       UUID,
    transaction_id UUID,
    action         VARCHAR(48)  NOT NULL,
    actor          VARCHAR(64)  NOT NULL,
    participant_id VARCHAR(64),
    file_name      VARCHAR(255),
    file_hash      VARCHAR(64),
    reason_code    VARCHAR(64),
    before_state   VARCHAR(24),
    after_state    VARCHAR(24),
    detail         TEXT,
    created_at     TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_audit_batch ON audit_entry (batch_id);
CREATE INDEX ix_audit_txn   ON audit_entry (transaction_id);
