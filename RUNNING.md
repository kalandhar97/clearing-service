# Clearing Service — Build & Run

Production Spring Boot 3.3 / Java 17 service that prepares, validates, batches,
and transmits clearing files to the external clearing application, tracks
acknowledgements, and publishes domain events.

See `ClearingReadme.md` for the full domain specification.

## Architecture at a glance

```
Payment Service ──ingest──► [PENDING txns]
                                  │  (scheduled formation / POST /batches/form)
                                  ▼
                     validate ─► batch ─► generate file (ISO 8583 / ISO 20022 / NACHA / CSV)
                                  │        hash + control totals, stored via ClearingFileStorage
                                  ▼
                            [VALIDATED batch]
                                  │  (scheduled submission sweep, durable retry/backoff)
                                  ▼
                     REST/WebClient ─► External Clearing Application
                                  │
                                  ▼
                            [SENT batch]  ◄── inbound acknowledgement (webhook)
                                  │
                    ACCEPTED / PARTIAL / REJECTED
                                  ▼
                     COMPLETED / FAILED  ──► domain events via transactional outbox ─► Kafka
```

Batch lifecycle: `CREATED → VALIDATED → SENT → ACKNOWLEDGED → COMPLETED`, with any
non-terminal state able to move to `FAILED`. The state machine is enforced in
`ClearingBatch.transitionTo`.

Reliability building blocks:

- **Idempotent ingestion** keyed on `sourceTransactionId` (unique constraint).
- **Transactional outbox** (`outbox_event`) drained to Kafka by `OutboxPublisher`,
  so events commit atomically with state changes and survive restarts.
- **Durable submission retry**: attempt count and `next_attempt_at` are persisted
  on the batch; the scheduler re-drives due batches with exponential backoff.
- **Optimistic locking** (`@Version`) on batches and transactions.
- **Immutable audit trail** (`audit_entry`) for every lifecycle action.

## Prerequisites

- JDK 17
- Gradle 8.x (or use the Docker build)
- PostgreSQL 16 and Kafka (provided by `docker-compose.yml`)

## Run locally

```bash
# 1. Start Postgres + Kafka
docker compose up -d postgres kafka

# 2. Run the service (uses the mock transport by default, so no external
#    clearing endpoint is required)
gradle bootRun
```

Or run everything (service included) in containers:

```bash
docker compose up --build
```

- API base: `http://localhost:8080/api/v1/clearing`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
- Metrics (Prometheus): `http://localhost:8080/actuator/prometheus`

## Quick smoke test

```bash
# Ingest a transaction
curl -X POST http://localhost:8080/api/v1/clearing/transactions \
  -H 'Content-Type: application/json' \
  -d '{"sourceTransactionId":"txn-1","merchantId":"M-100","network":"VISA",
       "transactionType":"SALE","currency":"USD","amountMinor":1999,
       "settlementDate":"2026-07-20","mcc":"5411","arn":"A123"}'

# Form batches from pending transactions
curl -X POST http://localhost:8080/api/v1/clearing/batches/form

# Inspect batches (grab an {id})
curl http://localhost:8080/api/v1/clearing/batches

# Submit a VALIDATED batch immediately (also runs automatically on a schedule)
curl -X POST http://localhost:8080/api/v1/clearing/batches/{id}/submit

# Record an acknowledgement from the participant
curl -X POST http://localhost:8080/api/v1/clearing/batches/{id}/acknowledgements \
  -H 'Content-Type: application/json' \
  -d '{"status":"ACCEPTED","ackReference":"RCV-9"}'
```

## Configuration (env vars)

| Variable                                                    | Default              | Purpose                                 |
|-------------------------------------------------------------|----------------------|-----------------------------------------|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`                    | local Postgres       | Datasource                              |
| `KAFKA_BOOTSTRAP_SERVERS`                                   | `localhost:9092`     | Kafka brokers                           |
| `CLEARING_TRANSPORT_MOCK`                                   | `true`               | `false` enables the real REST transport |
| `CLEARING_TRANSPORT_BASE_URL` / `_SUBMIT_PATH` / `_API_KEY` | —                    | External clearing endpoint              |
| `CLEARING_STORAGE_DIR`                                      | `./clearing-files`   | Where generated files are written       |
| `CLEARING_SUBMISSION_MAX_ATTEMPTS`                          | `5`                  | Retry cap before a batch fails          |
| `CLEARING_EVENTS_TOPIC`                                     | `clearing.events.v1` | Kafka topic for domain events           |
| `CLEARING_API_KEY_ENABLED` / `CLEARING_API_KEY`             | `false`              | Optional API-key gate                   |

Switch to the real transport with `CLEARING_TRANSPORT_MOCK=false` and set the
base URL / submit path / API key for the external clearing application.

## Schema

Flyway owns the schema (`src/main/resources/db/migration/V1__init.sql`) and runs
on startup. `spring.jpa.hibernate.ddl-auto` is `none`; set it to `validate` in CI
against a migrated database to detect entity/schema drift.

## Notes

- **Message formats** (ISO 8583, ISO 20022 pacs.008, NACHA, CSV) are rendered as
  structured, self-consistent files with header/detail/trailer and control
  totals. They are simplified representations — certified wire encoding is owned
  by the external clearing application, per the service boundary.
- **Regulatory reporting** (README §Regulatory Reporting) is a documented
  extension point and is not implemented in this build.
- **Legacy files**: the earlier scaffolding under `entity/`, `controller/`,
  `repository/`, and the old CRUD `service/*` classes could not be deleted on the
  current mount, so they were emptied to no-op files. They are safe to delete:
  `git rm` the `entity/` and `controller/` folders and the
  `FileRecord*/NetworkFile*/NetworkCalendar*/OutboundBatch*/Presentment*`
  classes under `repository/` and `service/`.

```
