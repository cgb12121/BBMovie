# Entitlement Service

Projection + decision service for access control, fed by payment lifecycle events.

## Responsibilities

- Consume payment events and project entitlement records.
- Serve access-check decisions to internal callers (streaming/workflow).
- Cache decisions in Redis for low-latency repeated checks.
- Support replay/backfill, explain endpoint, and manual overrides with audit.

## APIs

### Check entitlement

```http
POST /api/v1/entitlements/check
X-Internal-Api-Key: <key>
Authorization: Bearer <jwt>
```

### Batch check

```http
POST /api/v1/entitlements/check-batch
```

### Explain decision trace

```http
POST /api/v1/entitlements/explain
```

### User entitlement view

```http
GET /api/v1/entitlements/user/{userId}
```

### Manual override (admin)

```http
POST /api/v1/entitlements/admin/overrides/grant
POST /api/v1/entitlements/admin/overrides/revoke
```

### Replay trigger (admin)

```http
POST /api/v1/entitlements/admin/replay
```

## Security model

- Resource server JWT validation via JWKS (`jose.jwk.endpoint`).
- Dev fallback key set at `src/main/resources/jwk-dev.json`.
- Internal caller key check for `/check` via `X-Internal-Api-Key`.

## Event ingestion

Consumes payment events from:

- `commerce.payment.events.v1`

DLQ topic pattern:

- `commerce.payment.events.v1.dlq`

## Local configuration

Important keys in `application.properties`:

- `server.port=8098`
- `spring.datasource.*` (MySQL)
- `spring.data.redis.*`
- `spring.kafka.bootstrap-servers`
- `entitlement.security.internal-api-key`
- `entitlement.replay.enabled`
- `jose.jwk.endpoint`

## Reason codes (current)

- `ACTIVE_ENTITLEMENT`
- `NO_ACTIVE_ENTITLEMENT`
- `PLAN_PACKAGE_MISMATCH`

## Run and test

```powershell
mvn spring-boot:run
```

```powershell
mvn test
```

## Replay runbook

Use shared runbook and scripts:

- `backend/commerce-platform/docs/DLQ_REPLAY_RUNBOOK.md`
- `backend/commerce-platform/deploy/scripts/replay-dlq.ps1`
