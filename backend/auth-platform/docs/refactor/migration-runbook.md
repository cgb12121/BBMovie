# Migration Runbook: auth-service Monolith to auth-platform Services

This runbook defines a safe, reversible migration path from monolith to microservices.

## Goals
- Zero data loss
- Controlled cutover with rollback points
- Observable behavior per phase

## Phase 0: Preparation
- Freeze schema changes in legacy `auth-service` for affected tables.
- Create dashboards/alerts:
  - login success rate
  - token issuance failures
  - JWKS fetch failures
  - MFA verification failures
- Add correlation IDs across service-to-service calls.

## Phase 1: Data Readiness
- Create target schemas/tables:
  - `identity-service`: `users`
  - `sso-service`: `jwk_keys`, `refresh_token`
  - `mfa-service`: `mfa_secrets`
  - `student-service`: `student_profiles`
- Backfill data from monolith to target services.
- Validate row counts and key constraints.

Rollback checkpoint: keep monolith as single writer.

## Phase 2: Dual-Write (Temporary)
- For mutable entities, write to monolith and target service in the same operation boundary (or outbox event + idempotent consumer).
- Track write-drift metrics and reconcile.

Rollback checkpoint: disable target writes and continue monolith-only writes.

## Phase 3: Read Switch by Domain
- Switch reads domain-by-domain:
  1. identity reads -> `identity-service`
  2. MFA reads -> `mfa-service`
  3. student reads -> `student-service`
  4. token/session reads -> `sso-service`
- Use feature flags per endpoint or traffic segment.
  - `cutover.identity.enabled`
  - `cutover.mfa.enabled`
  - `cutover.sso.enabled`

Rollback checkpoint: flip read flags back to monolith.

## Phase 4: Auth Flow Cutover
- Move login orchestration to `sso-service`.
- Ensure `sso-service` verifies credentials via `identity-service`.
- Ensure MFA checks call `mfa-service` canonical endpoints:
  - `/internal/mfa/verify-totp`
  - `/internal/mfa/verify-otp`
- Validate refresh/logout/session endpoints.

Rollback checkpoint: route auth entrypoints back to legacy service.

## Phase 5: Local JWT Verification Rollout
- Deploy shared auth module to gateway/downstream services.
- Enable local verification using JWKS from `sso-service`.
- Enforce:
  - JWKS TTL = 5 minutes
  - force-refresh on unknown `kid`
  - fallback to last-known-good JWKS
- Subscribe to `auth.logout` events for blacklist propagation.

Rollback checkpoint: temporary fallback to centralized introspection/legacy auth checks if required.

## Phase 6: Decommission Legacy Paths
- Disable legacy write paths.
- Archive old tables and remove legacy auth endpoints.
- Keep read-only snapshots for audit period.

## Idempotency Rules
- All event consumers must be idempotent.
- Use dedup keys:
  - event ID for NATS messages
  - `sid`/`jti` for revocation events

## Verification Checklist (Go/No-Go)
- [ ] p95 login latency within SLO
- [ ] no increase in auth failure rates
- [ ] JWKS refresh errors below threshold
- [ ] session revoke propagation within agreed SLA
- [ ] data parity checks pass for migrated tables

## Related
- `cutover-gates.md`

