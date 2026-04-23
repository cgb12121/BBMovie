# Refactoring Plan: sso-service

The `sso-service` is the central Login Gateway and Authorization Server. It is the only service that signs tokens.

## Responsibilities
- Local Login flow orchestration.
- Social Login (OAuth2) via GitHub, Google, Facebook, etc.
- JWT Access Token generation (JWS signing).
- Session tracking via Refresh Tokens.
- RSA Key rotation and publishing JWKs (`/.well-known/jwks.json`).
- Session revocation management (Logout).

## Components to Migrate
- **Entities**: `JoseKey`, `RefreshToken`.
- **Controllers**: Portions of `AuthController` (`/login`, `/logout`, `/access-token`), `JwkController`, `SessionController`.
- **Services**: `SessionService`, `RefreshTokenService`, `JoseProvider`.
- **Security Logic**: `KeyRotation`, `KeyCache`, `OAuth2LoginSuccessHandler`, `CustomAuthorizationRequestResolver`.
- **NATS Producers**: `LogoutEventProducer`, `ABACEventProducer`.

## Communication with Other Services
- **`identity-service`**: SSO calls `identity-service` to verify password credentials.
- **`mfa-service`**: SSO calls `mfa-service` to verify 2nd-factor codes if enabled for the user:
  - `POST /internal/mfa/verify-totp`
  - `POST /internal/mfa/verify-otp`

## Database Migration
1. Keep `jwk_keys` and `refresh_token` tables in the `sso-service` database.

## Technical Challenges
- **OAuth2 Registration Logic**: The `OAuth2LoginSuccessHandler` currently auto-creates users. This logic must be moved to call `identity-service` to ensure the User store remains centralized.
- **Public Key Exposure**: The `/.well-known/jwks.json` endpoint is now critical for system-wide local token verification. It must be highly available.
- **Key Rotation Compatibility**: Rotation policy must preserve old keys long enough for in-flight tokens to expire, otherwise downstream local validation will fail.
