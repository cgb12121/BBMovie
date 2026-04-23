# Legacy Endpoint Migration Map

This map helps move traffic from legacy `auth-service` to split services without deleting the monolith first.

## Scope

- Legacy source: `backend/auth-platform/services/auth-service`
- Target services:
  - `backend/auth-platform/services/identity-service`
  - `backend/auth-platform/services/mfa-service`
  - `backend/auth-platform/services/sso-serivce`

## Endpoint ownership map

### Identity service

- `POST /auth/register`
- `GET /auth/verify-email`
- `POST /auth/send-verification`
- `POST /auth/change-password`
- `POST /auth/forgot-password`
- `POST /auth/reset-password`

### MFA service

- `POST /api/mfa/setup`
- `POST /api/mfa/verify`
- `POST /internal/mfa/verify-totp`
- `POST /internal/mfa/generate-otp`
- `POST /internal/mfa/verify-otp`

### SSO service

- `POST /auth/login`
- `POST /auth/logout`
- `POST /auth/access-token`
- `GET /auth/oauth2-callback`
- `GET /.well-known/jwks.json`
- `GET /admin/jwks/all`
- `GET /admin/jwks/active`

## Cutover guidance

1. Keep legacy `auth-service` active.
2. Implement real handlers in new services behind feature flags.
3. Mirror requests in shadow mode (optional) and compare response/latency.
4. Shift read traffic by endpoint group (identity -> mfa -> sso).
5. Keep rollback route to monolith until migration runbook criteria pass.
