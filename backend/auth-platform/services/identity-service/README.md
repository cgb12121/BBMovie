# identity-service

Identity domain extracted from legacy `auth-service`.

## Migration scope from monolith

- `/auth/register`
- `/auth/verify-email`
- `/auth/send-verification`
- `/auth/change-password`
- `/auth/forgot-password`
- `/auth/reset-password`
- `/auth/user-agent` (if kept in identity boundary)

## Current status

- Phase: migration-stage handlers implemented for register/verify/password/internal-verify endpoints
- Legacy `auth-service` remains active during cutover
- Feature flag gate: `cutover.identity.enabled` (default `false`)
- Contracts and sequencing:
  - `backend/auth-platform/docs/refactor/identity-service.md`
  - `backend/auth-platform/docs/refactor/internal-api-contracts.md`
  - `backend/auth-platform/docs/refactor/migration-runbook.md`
