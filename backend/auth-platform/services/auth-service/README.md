# DEPRECATED: auth-service (monolith)

`auth-service` is kept temporarily for backward compatibility and rollback safety.

## Migration status

- **Do not add new business features here** unless urgent hotfix.
- New development should go to:
  - `backend/auth-platform/services/identity-service`
  - `backend/auth-platform/services/mfa-service`
  - `backend/auth-platform/services/sso-serivce`
  - `backend/community-platform/services/student-program-service` (student flow)

## Reference docs

- Legacy docs: `backend/auth-platform/docs/legacy/`
- Refactor docs: `backend/auth-platform/docs/refactor/`
- Contracts and runbook:
  - `backend/auth-platform/docs/refactor/internal-api-contracts.md`
  - `backend/auth-platform/docs/refactor/migration-runbook.md`

## Deprecation policy

- Keep old endpoints running during migration phases.
- Cut traffic per domain (`identity`, `mfa`, `sso`) only after verification in runbook.
- Final decommission happens after all consumers switch and rollback window expires.
