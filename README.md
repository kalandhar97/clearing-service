# Clearing Service

Batches captured transactions into network clearing files (ISO 8583, ISO 20022, NACHA, CSV), submits them to an external
clearing application, and tracks acknowledgements through to settlement handoff.

## Role in the platform

- Ingests individual captured transactions (`POST /api/v1/clearing/transactions`), idempotent on `sourceTransactionId` (
  the Payment Service's transaction id), and holds them `PENDING` until batched.
- On a cron (`clearing.batching.formation-cron`, default every 10 minutes), groups pending transactions by
  network/currency/settlement date into a `ClearingBatch`, validates them, and renders a clearing file via a
  format-specific strategy (ISO8583 / ISO20022 / NACHA / CSV).
- On a second cron (`clearing.submission.sweep-cron`, default every minute), submits due/retryable batches to the
  external clearing application through a pluggable `ClearingTransport`, with bounded retry and exponential backoff (
  `clearing.submission.max-attempts`, `backoff-initial-ms`, `backoff-multiplier`).
- Records inbound acknowledgements/rejections per batch (`POST /api/v1/clearing/batches/{batchId}/acknowledgements`) and
  drives the batch state machine `CREATED -> VALIDATED -> SENT -> ACKNOWLEDGED -> COMPLETED`, with any non-terminal
  state able to fail out to `FAILED`.
- Maintains an immutable audit trail (`audit_entry`) of batch/transaction lifecycle actions, queryable per batch.
- Publishes domain events (`ClearingBatchCreated`, `ClearingSubmitted`, `ClearingAccepted`, `ClearingRejected`) to Kafka
  via a transactional outbox, published by a dedicated sweep (`clearing.outbox.publish-cron`, default every 5 seconds).

## Tech stack

- Java 21, Spring Boot 3.3.2, Gradle.
- Default port **8089** (`server.port`, overridable via `SERVER_PORT`).
- Datastore: PostgreSQL (`spring-boot-starter-data-jpa` + `postgresql` driver), schema owned entirely by **Flyway** (
  `ddl-auto: none`, `flyway.enabled: true`, single migration `V1__init.sql`).
- Messaging: Spring Kafka, used only as a producer (transactional outbox pattern; idempotent producer config with
  `acks=all`, `retries=5`, `enable.idempotence=true`).
- Outbound HTTP: `spring-boot-starter-webflux` (`WebClient`) for the real clearing transport.
- Ops: Actuator + Micrometer Prometheus registry, springdoc-openapi (Swagger UI at `/swagger-ui.html`).
- No security starter; an optional shared-secret `ApiKeyFilter` gates the REST API when
  `clearing.security.api-key-enabled=true` (disabled by default) — auth otherwise relies on the gateway.

## API surface

All endpoints are under `/api/v1/clearing`, fronted by `gateway-service` (route `Path=/api/v1/clearing/**`, with a
`clearingCircuitBreaker`, rate limiter, and 2 retries).

- **`ClearingTransactionController`** — `/api/v1/clearing/transactions`
    - `POST /` — ingest a transaction for clearing (idempotent on `sourceTransactionId`).
    - `GET /{id}`, `GET /by-source/{sourceTransactionId}` — lookups.
    - `GET /` — paginated list, optional `status` filter.
    - `POST /{id}/cancel` — cancel a transaction not yet cleared.
- **`ClearingBatchController`** — `/api/v1/clearing/batches`
    - `GET /`, `GET /{id}`, `GET /reference/{reference}` — batch lookups.
    - `GET /{id}/file` — the generated clearing file metadata.
    - `GET /{id}/transactions`, `GET /{id}/rejections`, `GET /{id}/audit` — batch drill-down.
    - `POST /form` — manually trigger batch formation (normally cron-driven).
    - `POST /{id}/submit` — manually force submission of a validated batch.
- **`AcknowledgementController`** — `/api/v1/clearing/batches/{batchId}/acknowledgements`
    - `POST /` — record an inbound ack/reject from the clearing network.
    - `GET /` — list acknowledgements for a batch.

Note: `controller/` (FileRecordController, NetworkCalendarController, NetworkFileController, OutboundBatchController)
and the parallel `entity/` package (FileRecord, NetworkCalendar, NetworkFile, OutboundBatch, Presentment) are **empty
legacy stub files** — comments state they were "superseded by the restructured clearing domain" and "could not be
deleted on this mount." They contain no code, are not wired into Spring, and should be treated as dead weight, not
active surface.

## Data model

Flyway `V1__init.sql` defines (all monetary amounts as integer minor units, all timestamps `TIMESTAMPTZ` UTC):

- **`clearing_batch`** (`ClearingBatch`, aggregate root) — `reference` (unique), `network`, `currency`, `region`,
  `settlement_date`, `format`, `status`, `transaction_count`, `total_amount_minor`, `cutoff_at`, `submission_attempts`,
  `next_attempt_at`, `ack_reference`, `last_error`, lifecycle timestamps (`validated_at`/`sent_at`/`acknowledged_at`/
  `completed_at`/`failed_at`), optimistic-lock `version`. State transitions are enforced in code (
  `BatchStatus.canTransitionTo`), not just documented.
- **`clearing_transaction`** (`ClearingTransaction`) — `source_transaction_id` (unique idempotency key), `merchant_id`,
  `network`, `transaction_type`, `currency`, `amount_minor`, `region`, `settlement_date`, `mcc`, `auth_code`, `arn`,
  `pan_token`, `captured_at`, `status` (`PENDING -> BATCHED -> CLEARED`, or `REJECTED`/`FAILED`), `rejection_reason`,
  `batch_id` (FK, nullable — cleared on `returnToPending()` if a batch fails validation).
- **`clearing_file`** (`ClearingFile`) — one-to-one with a batch: `format`, `file_name`, `content_hash`, `size_bytes`,
  `record_count`, `control_total_minor`, `storage_uri`, optional `signature`.
- **`clearing_acknowledgement`** (`ClearingAcknowledgement`) — inbound ack/rejection per batch: `ack_reference`,
  `status`, `reason_code`, `message`, `raw_payload`.
- **`outbox_event`** (`OutboxEvent`) — transactional outbox rows (`aggregate_type`, `aggregate_id`, `event_type`,
  `payload`, `status`, `attempts`, `last_error`) polled and relayed to Kafka.
- **`audit_entry`** (`AuditEntry`) — immutable action log keyed by batch/transaction with actor, participant, file hash,
  reason code, before/after state.

`Network` enum (VISA, MASTERCARD, AMEX, DISCOVER, UNIONPAY, ACH, SEPA, FPS, SWIFT, FEDWIRE) each carries a
`defaultFormat()` mapping to `ClearingFormat` (card networks → ISO8583, ACH → NACHA, SEPA/FPS/SWIFT/FEDWIRE → ISO20022).

## Inter-service integration

- **Inbound**: `gateway-service` routes `/api/v1/clearing/**` to `clearing-service` (env `CLEARING_SERVICE_URI`, default
  `http://clearing-service:8080` in the gateway's own config — actual container port is 8089 per this service's
  `server.port`), with a dedicated `clearingCircuitBreaker`, request-rate limiter, and retry filter. No other service in
  the repo was found calling clearing-service directly in Java code (e.g. payment-service has no clearing client) —
  transaction ingestion into clearing appears to be gateway-fronted/external rather than a direct service-to-service
  call in the current codebase.
- **Outbound — clearing files**: submitted via the `ClearingTransport` abstraction (see Design notes) to an external
  clearing application at `clearing.transport.base-url` + `submit-path` (defaults
  `http://localhost:9099/clearing/submissions`), or short-circuited by a mock when `clearing.transport.mock=true` (the
  default).
- **Outbound — Kafka**: publishes to topic `clearing.events.v1` (`clearing.events.topic`) via the transactional outbox:
  `ClearingBatchCreated`, `ClearingSubmitted`, `ClearingAccepted`, `ClearingRejected` (event/aggregate-type constants in
  `ClearingEvents`). No inbound Kafka consumers were found in this service.
- **Storage**: generated clearing files are written to local disk under `clearing.storage.directory` (default
  `./clearing-files`) via `LocalFileStorage`.

## Running locally

```
./gradlew :clearing-service:bootRun
```

Or via the bundled `docker-compose.yml` (Postgres + single-node Kafka + the service), which sets
`CLEARING_TRANSPORT_MOCK=true` explicitly.

Key environment variables (all optional, defaults shown):

| Variable                                                                        | Default                                                               |
|---------------------------------------------------------------------------------|-----------------------------------------------------------------------|
| `SERVER_PORT`                                                                   | `8089`                                                                |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`                                        | `jdbc:postgresql://localhost:5432/clearing` / `clearing` / `clearing` |
| `KAFKA_BOOTSTRAP_SERVERS`                                                       | `localhost:9092`                                                      |
| `CLEARING_TRANSPORT_MOCK`                                                       | `true`                                                                |
| `CLEARING_TRANSPORT_BASE_URL` / `CLEARING_TRANSPORT_SUBMIT_PATH`                | `http://localhost:9099` / `/clearing/submissions`                     |
| `CLEARING_TRANSPORT_API_KEY`                                                    | *(empty)*                                                             |
| `CLEARING_STORAGE_DIR`                                                          | `./clearing-files`                                                    |
| `CLEARING_MAX_BATCH_SIZE`                                                       | `5000`                                                                |
| `CLEARING_FORMATION_CRON` / `CLEARING_SUBMISSION_CRON` / `CLEARING_OUTBOX_CRON` | every 10 min / every 1 min / every 5 sec                              |
| `CLEARING_API_KEY_ENABLED` / `CLEARING_API_KEY`                                 | `false` / *(empty)*                                                   |

**Switching mock vs. real clearing transport**: set `CLEARING_TRANSPORT_MOCK=false` (and `CLEARING_TRANSPORT_BASE_URL`/
`CLEARING_TRANSPORT_API_KEY` as needed) to enable `RestClearingTransport` and disable `MockClearingTransport` — this is
a Spring `@ConditionalOnProperty` toggle, not a profile, so exactly one `ClearingTransport` bean is ever active. With
mock on (default), every submission is accepted immediately with a synthetic `MOCK-<uuid>` receipt and no network I/O,
so the full pipeline runs end-to-end without a live downstream endpoint.

Swagger UI: `http://localhost:8089/swagger-ui.html`. Health/metrics: `/actuator/health`, `/actuator/prometheus`.

## Design notes

- **Formatter strategy pattern**: `ClearingMessageFormatter` is an interface (`format()`, `fileExtension()`,
  `format(batch, transactions)`) with four Spring-managed implementations — `Iso8583Formatter`, `Iso20022Formatter`,
  `NachaFormatter`, `CsvFormatter` — auto-discovered into a `FormatterRegistry` keyed by `ClearingFormat` enum. Adding a
  network format is a matter of adding one new `@Component`; no branching logic elsewhere needs to change. The Javadoc
  on `ClearingMessageFormatter` is explicit that these are *structurally valid but simplified* representations (correct
  header/detail/trailer with record counts and control totals) — full certified wire-format encoding is deliberately
  left to the external clearing application, an intentional architectural boundary rather than an oversight.
- **Transport abstraction with a first-class mock**: `ClearingTransport` has exactly two implementations selected by
  `@ConditionalOnProperty(clearing.transport.mock)` — `MockClearingTransport` (default, matchIfMissing=true) and
  `RestClearingTransport` (WebClient-based, real HTTP). This makes the mock a legitimate default rather than a test
  double bolted on later, letting the whole batching/submission/ack/outbox pipeline be exercised locally and in CI with
  zero external dependencies.
- **Batch state machine with enforced transitions**: `BatchStatus.canTransitionTo` is checked inside the entity itself (
  `ClearingBatch.transitionTo`), throwing `IllegalStateException` on illegal moves (e.g. `SENT` never reverts to
  `CREATED`). Notably a batch only reaches `SENT` once transmission *actually succeeds*; a transient submission failure
  leaves it at `VALIDATED` with `next_attempt_at` set for the retry sweep, rather than optimistically marking it sent.
- **Idempotency and durable retry throughout**: transaction ingestion is idempotent on `source_transaction_id` (unique
  DB constraint), batch submission retries are durable and backed off (`submission_attempts`, `next_attempt_at`,
  exponential backoff) rather than relying on in-memory retry, and outbound Kafka events go through a transactional
  outbox table so event publication survives crashes between DB commit and broker send — three different
  idempotency/reliability concerns (ingest, submit, publish) each solved with the same "durable state + scheduled sweep"
  pattern instead of ad hoc in-request retries.
