# Legacy Business Flows: auth-service

This document describes the critical business processes implemented within the monolithic `auth-service`.

## 1. User Registration & Email Verification

1. **Client** calls `POST /auth/register` with user details.
2. **`RegistrationService`** validates the request, hashes the password (randomly choosing between Argon2, BCrypt, or PBKDF2), and saves the `User` with `isEnabled=false`.
3. **`EmailVerifyTokenService`** generates a random UUID token and stores it in Redis (15-min TTL) mapping `token -> email`.
4. **`EmailEventProducer`** publishes an event to NATS topic `auth.registration`.
5. **(External Service)** consumes the NATS event and sends a Magic Link email to the user.
6. **User** clicks the link, calling `GET /auth/verify-email?token=...`.
7. **`RegistrationService`** validates the token in Redis, sets `user.isEnabled=true`, and deletes the token.

## 2. Authentication (Login) & Token Issuance

1. **Client** calls `POST /auth/login` with credentials.
2. **`SessionService`** validates the password.
3. **`JoseProvider`** creates a unique Session ID (`sid`) and a JWT ID (`jti`).
4. **`JoseProvider`** signs two tokens:
   - **Access Token**: Short-lived, contains core claims (ID, Email, Role) and the `sid`.
   - **Refresh Token**: Long-lived, contains the `sid`.
5. **`RefreshTokenService`** persists the Refresh Token and device metadata (User-Agent, IP) to the `refresh_token` table.
6. **Response** returns the tokens to the user.

## 3. Multi-Factor Authentication (MFA)

### Setup Flow
1. **User** calls `POST /api/mfa/setup`.
2. **`MfaService`** generates a TOTP secret and a QR Code data URI.
3. **`User`** entity is updated with the `totpSecret`, but `isMfaEnabled` remains `false`.
4. **OTP Code** is sent via NATS to the user's email for setup confirmation.
5. **User** calls `POST /api/mfa/verify` with the code.
6. **`MfaService`** validates the code and sets `user.isMfaEnabled=true`.

### Login Flow (MFA Interruption)
- Currently, the login flow expects a standard authentication. If MFA is enabled, the client is expected to handle the 2FA challenge (using the `X-TOTP-Code` header or separate verification steps).

## 4. Session Revocation (Logout)

### Current Device Logout
1. **User** calls `POST /auth/logout` with the Access Token.
2. **`SessionService`** extracts the `sid` from the token.
3. **`RefreshTokenService`** deletes the session from the database.
4. **`JoseProvider`** adds the `sid` to the Redis logout blacklist.
5. **`LogoutEventProducer`** publishes the revoked `sid` to NATS topic `auth.logout`.

### Remote Revocation
1. **User** calls `POST /device/v1/sessions/revoke` with a target `sid`.
2. **`SessionService`** ensures the target `sid` is not the current session.
3. **`RefreshTokenService`** deletes the target session and blacklists it in Redis/NATS.

## 5. Student Verification Workflow

1. **User** calls `POST /api/student-program/apply` with a document (PDF/Image).
2. **`StudentVerificationService`** creates a `StudentProfile` with `status=PENDING`.
3. **System** triggers an external Camunda process via REST.
4. **(Async)** The Camunda workflow (or an admin) processes the document.
5. **Admin** (or Camunda) calls `/api/student-program/internal/applications/{id}/finalize` to update the profile to `VERIFIED` or `REJECTED`.
