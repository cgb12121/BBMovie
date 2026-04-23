# Refactoring Plan: mfa-service

The `mfa-service` will be a standalone provider for generating and validating verification codes (TOTP, SMS, Email).

## Responsibilities
- Generate TOTP secrets and QR Code URIs.
- Validate TOTP codes for authentication.
- Generate and validate One-Time Passwords (OTP) via SMS or Email.
- Manage the lifecycle of MFA enrollment for users.

## Components to Migrate
- **Controller**: `MfaController`.
- **Services**: `MfaService`, `TotpService`, `OtpService`.
- **NATS Producer**: `TotpProducer`.

## New Internal API (for `sso-service` and other services)
- **`POST /internal/mfa/verify-totp`**
  - **Input**: `{ userId, code }`
  - **Output**: `{ valid: true }`
- **`POST /internal/mfa/generate-otp`**
  - **Input**: `{ userId, type: 'SMS'|'EMAIL', destination }`
  - **Output**: `{ success: true }`
- **`POST /internal/mfa/verify-otp`**
  - **Input**: `{ userId, code }`
  - **Output**: `{ valid: true }`

These endpoints are the canonical contract and should be used everywhere (`overall-system.md`, `sso-service.md`, and client implementations).

## Database Migration
1. Move the `totp_secret` and MFA settings (currently in `users` table) into a new `mfa_secrets` table in the `mfa-service` database.

## Technical Challenges
- **Secret Isolation**: Moving secrets out of the `users` table improves security but requires a synchronized migration.
- **Service Reuse**: Ensure the API is generic enough that a future `payment-service` could call `mfa-service` to confirm a high-value transaction.
