# Internal API Contracts

This document is the single source of truth for internal APIs between auth-platform services.

## Conventions
- Base path prefix: `/internal`
- Auth: service-to-service auth via mTLS or signed internal token (to be selected by platform team)
- Error contract:
  - `400` for validation errors
  - `401/403` for auth/authz failures
  - `404` for missing resources
  - `409` for state conflicts

## 1) identity-service

### `POST /internal/users/verify-credentials`
- Purpose: Validate email/password for login orchestration.
- Request:
```json
{
  "email": "user@example.com",
  "password": "raw-password"
}
```
- Response:
```json
{
  "valid": true,
  "userId": "uuid",
  "email": "user@example.com",
  "roles": ["USER"],
  "isEnabled": true,
  "isMfaEnabled": true
}
```

### `GET /internal/users/{userId}`
- Purpose: Fetch identity profile for downstream services (e.g. student-service).
- Response (example):
```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "displayedUsername": "bb_user",
  "isEnabled": true
}
```

## 2) mfa-service

### `POST /internal/mfa/verify-totp`
- Purpose: Verify TOTP code.
- Request:
```json
{
  "userId": "uuid",
  "code": "123456"
}
```
- Response:
```json
{
  "valid": true
}
```

### `POST /internal/mfa/generate-otp`
- Purpose: Generate OTP for SMS/email channel.
- Request:
```json
{
  "userId": "uuid",
  "type": "SMS",
  "destination": "+1234567890"
}
```
- Response:
```json
{
  "success": true
}
```

### `POST /internal/mfa/verify-otp`
- Purpose: Verify OTP code.
- Request:
```json
{
  "userId": "uuid",
  "code": "123456"
}
```
- Response:
```json
{
  "valid": true
}
```

## 3) sso-service

### `GET /.well-known/jwks.json`
- Purpose: Publish active and still-valid public keys for local verification.
- Requirements:
  - Must include current signing key.
  - Must keep previous keys until all unexpired tokens signed by them are expired.

### Token/Key Operational Contract
- Consumers cache JWKS for 5 minutes by default.
- Consumers force-refresh JWKS once when `kid` is unknown.
- If refresh fails, consumers may temporarily use last-known-good JWKS.

