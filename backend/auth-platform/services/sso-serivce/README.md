# sso-serivce

SSO/JWT issuer domain extracted from legacy `auth-service`.

## Migration scope from monolith

- `/auth/login`
- `/auth/logout`
- `/auth/access-token`
- `/auth/oauth2-callback`
- `/.well-known/jwks.json`
- admin JWKS endpoints

## Current status

- Phase: migration-stage handlers implemented for login/logout/refresh/jwks flows
- Legacy `auth-service` remains active during cutover
- Feature flag gate: `cutover.sso.enabled` (default `false`)
- Contracts and sequencing:
  - `backend/auth-platform/docs/refactor/sso-service.md`
  - `backend/auth-platform/docs/refactor/internal-api-contracts.md`
  - `backend/auth-platform/docs/refactor/migration-runbook.md`
