# Billing Ledger Service (Dev Event Injection)

Use this guide to generate fake payment events without running real checkout/refund flows in `payment-orchestrator-service`.

## Prerequisites

- Docker compose stack is up (Kafka container: `kafka_kraft`)
- Kafka topic used by commerce events: `commerce.payment.events.v1`

Start infrastructure if needed:

```powershell
docker compose -f backend/docker-compose.yml up -d kafka kafka-ui
```

## Event Shape

`payment-orchestrator-service` publishes JSON events in this shape:

```json
{
  "eventType": "PaymentSucceededV1",
  "paymentId": "019dab2c-50c7-7713-8512-5cf3ee6c6cc3",
  "payload": {
    "provider": "STRIPE",
    "status": "SUCCEEDED",
    "subscriptionId": "sub_001",
    "subscriptionCampaignId": "campaign_student_2026",
    "occurredAt": "2026-04-20T21:30:00Z"
  }
}
```

`billing-ledger-service` now promotes `subscriptionId` and `subscriptionCampaignId`
from the event payload into dedicated columns for easier support and reconciliation queries.

## Option A: Publish single events quickly

Open producer in Kafka container:

```powershell
docker exec -it kafka_kraft kafka-console-producer --bootstrap-server localhost:9092 --topic commerce.payment.events.v1
```

Then paste one JSON event per line and press Enter.

## Option B: Publish sample event set from file

Sample file:

- `backend/commerce-platform/billing-ledger-service/dev/payment-events.jsonl`

Publish it:

```powershell
Get-Content "backend/commerce-platform/billing-ledger-service/dev/payment-events.jsonl" | `
docker exec -i kafka_kraft kafka-console-producer --bootstrap-server localhost:9092 --topic commerce.payment.events.v1
```

## Verify messages

- Open Kafka UI: [http://localhost:8080](http://localhost:8080)
- Cluster: `local-kraft`
- Topic: `commerce.payment.events.v1`

## Recommended fake flow

Use the same `paymentId` for one lifecycle:

1. `PaymentInitiatedV1`
2. `PaymentSucceededV1` (or `PaymentFailedV1`)
3. `PaymentRefundedV1` (only for success branch)

This gives a realistic ledger scenario without calling payment providers.

## APIs

### Ledger timeline

```http
GET /api/v1/ledger/{paymentId}
```

Returns immutable ledger entries for one payment ordered by occurrence time.

### PDF export (no cron required)

```http
GET /api/v1/ledger/{paymentId}/pdf
```

Returns a downloadable PDF report for one payment timeline.

### Dashboard summary (real-time/semi-real-time)

```http
GET /api/v1/ledger/dashboard/summary?recentLimit=20
```

Returns:

- Total inbox events
- Total ledger entries
- Last 24h entries
- Aggregations by provider/status/entry type
- Recent entry list

### User billing history

```http
GET /api/v1/ledger/user/{userId}
```

Returns all ledger entries for one user (latest first).

### Subscription history

```http
GET /api/v1/ledger/subscription/{subscriptionId}
```

Returns all ledger entries linked to a subscription.

### Search & filter (support/admin)

```http
GET /api/v1/ledger/search?status=FAILED&provider=STRIPE&userId=user_123&from=2026-04-01T00:00:00Z&to=2026-04-30T23:59:59Z&limit=100
```

Supported filters:

- `provider`
- `status`
- `userId`
- `subscriptionId`
- `from` / `to` (ISO-8601)
- `limit` (max 500)

### Monthly CSV export (accounting)

```http
GET /api/v1/ledger/export/csv?month=2026-04
```

Returns downloadable CSV with billing/reconciliation columns.

## Retry + DLQ behavior

- Consumer retries failed records 2 times with 1 second backoff.
- If still failing, event is sent to:
  - `{original-topic}.dlq`
  - for example: `commerce.payment.events.v1.dlq`

You can inspect DLQ events in Kafka UI.

## Replay runbook

For DLQ replay, use shared scripts/runbook:

- `backend/commerce-platform/docs/DLQ_REPLAY_RUNBOOK.md`
- `backend/commerce-platform/deploy/scripts/replay-dlq.ps1`

