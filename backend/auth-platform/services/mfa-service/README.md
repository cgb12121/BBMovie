# mfa-service

MFA domain extracted from legacy `auth-service`.

## Migration scope from monolith

- `/api/mfa/setup`
- `/api/mfa/verify`
- Internal APIs for `sso-service`:
  - `POST /internal/mfa/verify-totp`
  - `POST /internal/mfa/generate-otp`
  - `POST /internal/mfa/verify-otp`

## Current status

- Phase: in progress (migration-stage handlers implemented for setup/verify/internal OTP+TOTP)
- Legacy `auth-service` remains active during cutover
- Feature flag gate: `cutover.mfa.enabled` (default `false`)
- Contracts and sequencing:
  - `backend/auth-platform/docs/refactor/mfa-service.md`
  - `backend/auth-platform/docs/refactor/internal-api-contracts.md`
  - `backend/auth-platform/docs/refactor/migration-runbook.md`
