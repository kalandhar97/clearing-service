# Clearing Service — API Testing Guide

## Dependencies — what to run before this service

**Other services:** None. `clearing-service` only talks to an external card-network
transport, and by default that's mocked in-process (`clearing.transport.mock=true`,
`CLEARING_TRANSPORT_MOCK`), so nothing else needs to be running.

**Infrastructure:** Postgres (`clearingservicedb`). Kafka for outbox event
publishing (optional for REST testing). Config Server is optional.

Base URL: http://localhost:8089

> **Auth note:** two independent gates. A **JWT bearer token** is now required by default
> (`security.jwt.enabled`, off in the `local` profile) — see [Authentication](#authentication) below.
> Separately, the `X-Api-Key` header is only required when `clearing.security.api-key-enabled=true`
> (property `CLEARING_API_KEY_ENABLED`, see `application.yml`). **The default is `false`**, so no API
> key is required in the default/local configuration. If enabled, send the configured key
> (`clearing.security.api-key` / `CLEARING_API_KEY`) as `X-Api-Key: <key>` on every request below.
> There is no dedicated `local` profile override of that flag, so it stays disabled locally unless you
> explicitly set `CLEARING_API_KEY_ENABLED=true`.

All monetary amounts are integer minor units (e.g. cents). All timestamps are ISO-8601 UTC instants.

The examples below assume the database has been seeded via `db/seed/V2__seed_sample_data.sql`, which is
only applied when the `local` Spring profile is active (`SPRING_PROFILES_ACTIVE=local`), since it is wired
in via a `local`-profile-only `spring.flyway.locations` override.

## Authentication

This service is now an OAuth2 **resource server**: every endpoint below requires
`Authorization: Bearer <accessToken>` by default. Tokens are RS256 JWTs issued by
`authentication-service` (port 8081) and validated locally against its JWKS at
`http://localhost:8081/.well-known/jwks.json` — signature, issuer, expiry, plus the `purpose`
claim, which must be `access` (refresh / step-up tokens are rejected). Claims map to authorities
as `scope` (space-delimited) -> `SCOPE_*`, and `principal_type` (`USER`, `MERCHANT`, `ADMIN`,
`SERVICE`) -> one `ROLE_*`. See `config/SecurityConfig.java`.

**Getting a token.** Log in against `authentication-service` on port 8081 — password login
(`POST http://localhost:8081/api/v1/auth/login`) or social login (Google / GitHub / Microsoft).
The token comes back as `tokens.accessToken`. See `authentication-service/API_TESTING.md` for the
full password/MFA and OAuth2 social-login flows.

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"<password>"}' \
  | jq -r '.tokens.accessToken')
```

**Testing without a token.** `security.jwt.enabled` (env `SECURITY_JWT_ENABLED`) defaults to
`true`. The `local` profile document in `application.yml` sets it to `false`, which swaps in a
permit-all chain, so with `SPRING_PROFILES_ACTIVE=local` — the profile the seeded-data examples
below already assume — the plain `curl` commands in this guide work as-is.
Never set it to `false` outside a developer machine or an ephemeral CI container.

**Always public** (no token, in either mode): `/actuator/health/**`, `/actuator/info`,
`/actuator/prometheus`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/error`.

**Two independent gates.** The optional `X-Api-Key` filter
(`clearing.security.api-key-enabled` / `CLEARING_API_KEY_ENABLED`, **default `false`**) is kept **in
addition to** the JWT chain, for machine-to-machine network partners with no platform identity. It is
off by default and there is no `local`-profile override, so it stays off locally unless you set
`CLEARING_API_KEY_ENABLED=true`. When on, send `X-Api-Key: <clearing.security.api-key>` as well as the
bearer token — turning the JWT toggle off does **not** disable the API-key filter.

**The same call, both ways:**

```bash
# with the `local` profile (security.jwt.enabled=false) — works as written
curl http://localhost:8089/api/v1/clearing/batches/11111111-1111-1111-1111-111111111111

# with the toggle on (the default) — token required
curl http://localhost:8089/api/v1/clearing/batches/11111111-1111-1111-1111-111111111111 \
  -H "Authorization: Bearer $TOKEN"
```

---

## Clearing Batches

Controller: `ClearingBatchController` — base path `/api/v1/clearing/batches`

### GET /api/v1/clearing/batches

List clearing batches, optionally filtered by status/network. Paginated, sorted by `createdAt` descending.

Query params:

- `status` (optional) — one of `CREATED`, `VALIDATED`, `SENT`, `ACKNOWLEDGED`, `COMPLETED`, `FAILED`
- `network` (optional) — one of `VISA`, `MASTERCARD`, `AMEX`, `DISCOVER`, `UNIONPAY`, `ACH`, `SEPA`, `FPS`, `SWIFT`,
  `FEDWIRE`
- `page` (optional, default `0`)
- `size` (optional, default `50`, max `200`)

Auth: Bearer JWT (see [Authentication](#authentication)); `X-Api-Key` additionally required only if enabled.

Response body (`PageResponse<ClearingBatchResponse>`):

```json
{
  "content": [
    {
      "id": "11111111-1111-1111-1111-111111111111",
      "reference": "BATCH-VISA-20260720-0001",
      "network": "VISA",
      "currency": "USD",
      "region": "US",
      "settlementDate": "2026-07-20",
      "format": "ISO8583",
      "status": "COMPLETED",
      "transactionCount": 2,
      "totalAmountMinor": 15000,
      "cutoffAt": "2026-07-20T20:00:00Z",
      "submissionAttempts": 1,
      "nextAttemptAt": null,
      "ackReference": "ACK-REF-0001",
      "lastError": null,
      "createdAt": "2026-07-20T10:00:00Z",
      "validatedAt": "2026-07-20T10:05:00Z",
      "sentAt": "2026-07-20T10:10:00Z",
      "acknowledgedAt": "2026-07-20T10:20:00Z",
      "completedAt": "2026-07-20T10:25:00Z",
      "failedAt": null,
      "updatedAt": "2026-07-20T10:25:00Z"
    }
  ],
  "page": 0,
  "size": 50,
  "totalElements": 5,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

curl:

```bash
curl "http://localhost:8089/api/v1/clearing/batches?status=COMPLETED&network=VISA&page=0&size=50"
```

### GET /api/v1/clearing/batches/{id}

Get a batch by id.

Auth: Bearer JWT (see [Authentication](#authentication)); `X-Api-Key` additionally required only if enabled.

Response body: same shape as one element of `content` above.

curl:

```bash
curl "http://localhost:8089/api/v1/clearing/batches/11111111-1111-1111-1111-111111111111"
```

### GET /api/v1/clearing/batches/reference/{reference}

Get a batch by its unique reference string.

Auth: Bearer JWT (see [Authentication](#authentication)); `X-Api-Key` additionally required only if enabled.

curl:

```bash
curl "http://localhost:8089/api/v1/clearing/batches/reference/BATCH-VISA-20260720-0001"
```

### GET /api/v1/clearing/batches/{id}/file

Get the generated clearing file metadata for a batch.

Auth: Bearer JWT (see [Authentication](#authentication)); `X-Api-Key` additionally required only if enabled.

Response body (`ClearingFileResponse`):

```json
{
  "id": "44444444-4444-4444-4444-444444444441",
  "batchId": "11111111-1111-1111-1111-111111111111",
  "format": "ISO8583",
  "fileName": "BATCH-VISA-20260720-0001.iso8583",
  "contentHash": "a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f9",
  "sizeBytes": 4096,
  "recordCount": 2,
  "controlTotalMinor": 15000,
  "storageUri": "file:///clearing-files/BATCH-VISA-20260720-0001.iso8583",
  "signature": "SIG-0001",
  "generatedAt": "2026-07-20T10:10:00Z"
}
```

curl:

```bash
curl "http://localhost:8089/api/v1/clearing/batches/11111111-1111-1111-1111-111111111111/file"
```

### GET /api/v1/clearing/batches/{id}/transactions

List the transactions in a batch.

Auth: Bearer JWT (see [Authentication](#authentication)); `X-Api-Key` additionally required only if enabled.

Response body: `List<ClearingTransactionResponse>` (see shape under Clearing Transactions below).

curl:

```bash
curl "http://localhost:8089/api/v1/clearing/batches/11111111-1111-1111-1111-111111111111/transactions"
```

### GET /api/v1/clearing/batches/{id}/rejections

List rejected transactions in a batch.

Auth: Bearer JWT (see [Authentication](#authentication)); `X-Api-Key` additionally required only if enabled.

curl:

```bash
curl "http://localhost:8089/api/v1/clearing/batches/11111111-1111-1111-1111-111111111115/rejections"
```

### GET /api/v1/clearing/batches/{id}/audit

Get the audit trail for a batch (404 if the batch does not exist).

Auth: Bearer JWT (see [Authentication](#authentication)); `X-Api-Key` additionally required only if enabled.

Response body (`List<AuditEntryResponse>`):

```json
[
  {
    "id": "55555555-5555-5555-5555-555555555551",
    "batchId": "11111111-1111-1111-1111-111111111111",
    "transactionId": null,
    "action": "BATCH_VALIDATED",
    "actor": "system",
    "participantId": "VISA",
    "fileName": null,
    "fileHash": null,
    "reasonCode": null,
    "beforeState": "CREATED",
    "afterState": "VALIDATED",
    "detail": "Batch validated prior to submission",
    "createdAt": "2026-07-20T10:05:00Z"
  }
]
```

curl:

```bash
curl "http://localhost:8089/api/v1/clearing/batches/11111111-1111-1111-1111-111111111111/audit"
```

### POST /api/v1/clearing/batches/form

Trigger formation of clearing batches from pending transactions.

Auth: Bearer JWT (see [Authentication](#authentication)); `X-Api-Key` additionally required only if enabled.

Request body: none.

Response body (`BatchFormationResponse`):

```json
{
  "batchesCreated": 1,
  "transactionsBatched": 3,
  "batches": [
    {
      "id": "11111111-1111-1111-1111-111111111112",
      "reference": "BATCH-MC-20260721-0001",
      "network": "MASTERCARD",
      "currency": "USD",
      "region": "US",
      "settlementDate": "2026-07-21",
      "format": "ISO8583",
      "status": "CREATED",
      "transactionCount": 3,
      "totalAmountMinor": 15000,
      "cutoffAt": null,
      "submissionAttempts": 0,
      "nextAttemptAt": null,
      "ackReference": null,
      "lastError": null,
      "createdAt": "2026-07-25T09:00:00Z",
      "validatedAt": null,
      "sentAt": null,
      "acknowledgedAt": null,
      "completedAt": null,
      "failedAt": null,
      "updatedAt": "2026-07-25T09:00:00Z"
    }
  ]
}
```

curl:

```bash
curl -X POST "http://localhost:8089/api/v1/clearing/batches/form"
```

### POST /api/v1/clearing/batches/{id}/submit

Manually submit a `VALIDATED` batch to the external clearing application.

Auth: Bearer JWT (see [Authentication](#authentication)); `X-Api-Key` additionally required only if enabled.

Request body: none.

Response body: `ClearingBatchResponse` (same shape as GET by id), with `status` moved toward `SENT`.

curl:

```bash
curl -X POST "http://localhost:8089/api/v1/clearing/batches/11111111-1111-1111-1111-111111111114/submit"
```

---

## Clearing Transactions

Controller: `ClearingTransactionController` — base path `/api/v1/clearing/transactions`

### POST /api/v1/clearing/transactions

Ingest a transaction for clearing. **Idempotent on `sourceTransactionId`** — re-posting the same
`sourceTransactionId` returns the existing transaction rather than creating a duplicate.

Auth: Bearer JWT (see [Authentication](#authentication)); `X-Api-Key` additionally required only if enabled.

Request body (`IngestTransactionRequest`):

```json
{
  "sourceTransactionId": "SRC-TXN-1001",
  "merchantId": "MERCH-0010",
  "network": "VISA",
  "transactionType": "SALE",
  "currency": "USD",
  "amountMinor": 9999,
  "region": "US",
  "settlementDate": "2026-07-26",
  "mcc": "5411",
  "authCode": "AUTH99",
  "arn": "ARN00000099999",
  "panToken": "PANTOK-1001",
  "capturedAt": "2026-07-25T18:30:00Z"
}
```

Validation: `sourceTransactionId` (required, max 64), `merchantId` (required, max 64), `network` (required
enum), `transactionType` (required enum), `currency` (required, exactly 3 chars), `amountMinor` (required,
positive), `region` (optional, max 32), `settlementDate` (required date), `mcc` (optional, max 4),
`authCode` (optional, max 16), `arn` (optional, max 32), `panToken` (optional, max 64), `capturedAt`
(optional instant).

Response: `201 Created` with body (`ClearingTransactionResponse`):

```json
{
  "id": "22222222-2222-2222-2222-222222222227",
  "sourceTransactionId": "SRC-TXN-1001",
  "merchantId": "MERCH-0010",
  "network": "VISA",
  "transactionType": "SALE",
  "currency": "USD",
  "amountMinor": 9999,
  "region": "US",
  "settlementDate": "2026-07-26",
  "mcc": "5411",
  "authCode": "AUTH99",
  "arn": "ARN00000099999",
  "panToken": "PANTOK-1001",
  "capturedAt": "2026-07-25T18:30:00Z",
  "status": "PENDING",
  "rejectionReason": null,
  "batchId": null,
  "createdAt": "2026-07-25T18:30:05Z",
  "updatedAt": "2026-07-25T18:30:05Z"
}
```

curl:

```bash
curl -X POST "http://localhost:8089/api/v1/clearing/transactions" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceTransactionId": "SRC-TXN-1001",
    "merchantId": "MERCH-0010",
    "network": "VISA",
    "transactionType": "SALE",
    "currency": "USD",
    "amountMinor": 9999,
    "region": "US",
    "settlementDate": "2026-07-26",
    "mcc": "5411",
    "authCode": "AUTH99",
    "arn": "ARN00000099999",
    "panToken": "PANTOK-1001",
    "capturedAt": "2026-07-25T18:30:00Z"
  }'
```

### GET /api/v1/clearing/transactions/{id}

Get a transaction by id.

Auth: Bearer JWT (see [Authentication](#authentication)); `X-Api-Key` additionally required only if enabled.

curl:

```bash
curl "http://localhost:8089/api/v1/clearing/transactions/22222222-2222-2222-2222-222222222221"
```

### GET /api/v1/clearing/transactions/by-source/{sourceTransactionId}

Get a transaction by its source transaction id (the idempotency key used on ingest).

Auth: Bearer JWT (see [Authentication](#authentication)); `X-Api-Key` additionally required only if enabled.

curl:

```bash
curl "http://localhost:8089/api/v1/clearing/transactions/by-source/SRC-TXN-0001"
```

### GET /api/v1/clearing/transactions

List transactions, optionally filtered by status. Paginated, sorted by `createdAt` descending.

Query params:

- `status` (optional) — one of `PENDING`, `BATCHED`, `CLEARED`, `REJECTED`, `FAILED`
- `page` (optional, default `0`)
- `size` (optional, default `50`, max `200`)

Auth: Bearer JWT (see [Authentication](#authentication)); `X-Api-Key` additionally required only if enabled.

Response body (`PageResponse<ClearingTransactionResponse>`):

```json
{
  "content": [
    {
      "id": "22222222-2222-2222-2222-222222222225",
      "sourceTransactionId": "SRC-TXN-0005",
      "merchantId": "MERCH-0005",
      "network": "SEPA",
      "transactionType": "SALE",
      "currency": "EUR",
      "amountMinor": 12000,
      "region": "EU",
      "settlementDate": "2026-07-25",
      "mcc": null,
      "authCode": null,
      "arn": null,
      "panToken": "PANTOK-0005",
      "capturedAt": "2026-07-25T08:00:00Z",
      "status": "PENDING",
      "rejectionReason": null,
      "batchId": null,
      "createdAt": "2026-07-25T08:01:00Z",
      "updatedAt": "2026-07-25T08:01:00Z"
    }
  ],
  "page": 0,
  "size": 50,
  "totalElements": 6,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

curl:

```bash
curl "http://localhost:8089/api/v1/clearing/transactions?status=PENDING&page=0&size=50"
```

### POST /api/v1/clearing/transactions/{id}/cancel

Cancel a transaction that has not yet been cleared.

Auth: Bearer JWT (see [Authentication](#authentication)); `X-Api-Key` additionally required only if enabled.

Request body: none.

Response body: `ClearingTransactionResponse` reflecting the cancelled status.

curl:

```bash
curl -X POST "http://localhost:8089/api/v1/clearing/transactions/22222222-2222-2222-2222-222222222225/cancel"
```

---

## Acknowledgements

Controller: `AcknowledgementController` — base path `/api/v1/clearing/batches/{batchId}/acknowledgements`

### POST /api/v1/clearing/batches/{batchId}/acknowledgements

Record an acknowledgement (`ACKNOWLEDGED`/`ACCEPTED`/`REJECTED`/`PARTIAL`) for a submitted batch. For
`PARTIAL` acknowledgements, `rejectedSourceTransactionIds` identifies the transactions not accepted.

Auth: Bearer JWT (see [Authentication](#authentication)); `X-Api-Key` additionally required only if enabled.

Request body (`AcknowledgementRequest`):

```json
{
  "status": "PARTIAL",
  "ackReference": "ACK-REF-0004",
  "reasonCode": "E1003",
  "message": "Two transactions failed downstream validation",
  "rawPayload": "{\"raw\":\"participant-native-payload\"}",
  "rejectedSourceTransactionIds": [
    "SRC-TXN-0006"
  ]
}
```

Validation: `status` is required (enum `ACKNOWLEDGED`, `ACCEPTED`, `REJECTED`, `PARTIAL`); all other
fields are optional.

Response body (`AcknowledgementResponse`):

```json
{
  "id": "33333333-3333-3333-3333-333333333334",
  "batchId": "11111111-1111-1111-1111-111111111113",
  "ackReference": "ACK-REF-0004",
  "status": "PARTIAL",
  "reasonCode": "E1003",
  "message": "Two transactions failed downstream validation",
  "receivedAt": "2026-07-25T18:45:00Z",
  "resultingBatchStatus": "ACKNOWLEDGED"
}
```

curl:

```bash
curl -X POST "http://localhost:8089/api/v1/clearing/batches/11111111-1111-1111-1111-111111111113/acknowledgements" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "PARTIAL",
    "ackReference": "ACK-REF-0004",
    "reasonCode": "E1003",
    "message": "Two transactions failed downstream validation",
    "rawPayload": "{\"raw\":\"participant-native-payload\"}",
    "rejectedSourceTransactionIds": ["SRC-TXN-0006"]
  }'
```

### GET /api/v1/clearing/batches/{batchId}/acknowledgements

List acknowledgements received for a batch.

Auth: Bearer JWT (see [Authentication](#authentication)); `X-Api-Key` additionally required only if enabled.

Response body (`List<AcknowledgementResponse>`):

```json
[
  {
    "id": "33333333-3333-3333-3333-333333333331",
    "batchId": "11111111-1111-1111-1111-111111111111",
    "ackReference": "ACK-REF-0001",
    "status": "ACCEPTED",
    "reasonCode": null,
    "message": "All transactions accepted",
    "receivedAt": "2026-07-20T10:20:00Z",
    "resultingBatchStatus": null
  }
]
```

Note: `resultingBatchStatus` is `null` on the list endpoint (only populated by the `process` response of
the POST above — see `AcknowledgementController.list`, which maps with a `null` resulting status).

curl:

```bash
curl "http://localhost:8089/api/v1/clearing/batches/11111111-1111-1111-1111-111111111111/acknowledgements"
```

---

## Seed data reference (local profile only)

Seeded via `src/main/resources/db/seed/V2__seed_sample_data.sql`, applied only when
`SPRING_PROFILES_ACTIVE=local` (adds `classpath:db/seed` to `spring.flyway.locations`).

| Table                    | ids                                               |
|--------------------------|---------------------------------------------------|
| clearing_batch           | `11111111-1111-1111-1111-111111111111` … `...115` |
| clearing_transaction     | `22222222-2222-2222-2222-222222222221` … `...226` |
| clearing_file            | `44444444-4444-4444-4444-444444444441`, `...442`  |
| clearing_acknowledgement | `33333333-3333-3333-3333-333333333331` … `...333` |
| audit_entry              | `55555555-5555-5555-5555-555555555551` … `...554` |
