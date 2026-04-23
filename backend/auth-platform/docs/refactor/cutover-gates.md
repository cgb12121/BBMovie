# Cutover Gates and Rollback Controls

This document defines endpoint-group feature flags, acceptance gates, and rollback triggers for auth migration.

## Feature Flags

- `cutover.identity.enabled` (default: `false`)
  - Service: `identity-service`
  - Scope: `/auth/register`, `/auth/verify-email`, `/auth/send-verification`, `/auth/change-password`, `/auth/forgot-password`, `/auth/reset-password`, `/auth/internal/users/verify-credentials`
- `cutover.mfa.enabled` (default: `false`)
  - Service: `mfa-service`
  - Scope: `/api/mfa/*`, `/internal/mfa/*`
- `cutover.sso.enabled` (default: `false`)
  - Service: `sso-serivce`
  - Scope: `/auth/login`, `/auth/logout`, `/auth/access-token`, `/auth/oauth2-callback`, `/.well-known/jwks.json`, `/admin/jwks/*`

When a flag is disabled, endpoint handlers return `503` with a message instructing traffic to legacy `auth-service`.

## Gate Checklist By Domain

### Identity gate
- Registration and email verification parity validated.
- Forgot/reset/change-password semantics match legacy responses.
- Internal verify-credentials endpoint stable for SSO callers.

### MFA gate
- Setup + verify flow parity validated.
- Internal TOTP/OTP endpoints return deterministic results.
- OTP replay and expiry behavior confirmed.

### SSO gate
- Login success/failure rates stable versus baseline.
- Access token refresh and logout/revoke behavior stable.
- JWKS endpoint healthy and key mismatch handling verified.

## Rollback Triggers

- Auth failure rate spike above baseline threshold.
- 401/403 spikes after endpoint group cutover.
- JWKS/unknown-`kid` verification failures above threshold.
- Refresh-token not-found/revoked anomalies.
- MFA verification error spike or OTP replay acceptance.

## Rollback Actions

1. Flip the affected cutover flag to `false`.
2. Route impacted endpoint group back to legacy `auth-service`.
3. Keep new service running for diagnostics, but stop serving that endpoint group.
4. Open incident timeline with request IDs and compare payload parity.
