# Subscription Service

Projection-based subscription lifecycle service fed by payment events.

## Responsibilities

- Consume payment events from Kafka.
- Build and update user subscription projection.
- Keep ingestion idempotent with inbox deduplication.
- Provide query APIs for user history and active subscription lookup.

## Data model focus

- `plans`
- `campaigns`
- `user_subscriptions`
- `subscription_event_inbox`

`subscription-service` owns plan/campaign/subscription tables and resolves IDs internally.

## APIs

### User subscription history

```http
GET /api/v1/subscriptions/user/{userId}
```

### Subscription by id

```http
GET /api/v1/subscriptions/{subscriptionId}
```

### Active subscription by user

```http
GET /api/v1/subscriptions/user/{userId}/active
```

## Event ingestion

Consumes payment events from:

- `commerce.payment.events.v1`

Used events:

- `PaymentSucceededV1` (activate/extend)
- `PaymentRefundedV1` (cancel/revoke path)
- `PaymentStatusUpdatedV1` (status-driven updates)

DLQ topic pattern:

- `commerce.payment.events.v1.dlq`

## Local configuration

Important keys in `application.properties`:

- `server.port=8097`
- `spring.datasource.*` (MySQL)
- `spring.kafka.bootstrap-servers`
- `spring.kafka.consumer.group-id=subscription-service`
- `app.kafka.topic.payment-events=commerce.payment.events.v1`

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
