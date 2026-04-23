# Refactoring Strategy: Overall System Architecture

This document describes the target architecture after refactoring the monolithic `auth-service` into a distributed microservices platform.

## Target Architecture

The monolith is split into four focused services based on Domain-Driven Design (DDD):

1. **`identity-service`**: The User Store and Account Management.
2. **`sso-service`**: The Authorization Server and Login Gateway.
3. **`mfa-service`**: The Verification and OTP Provider.
4. **`student-service`**: The Student Verification Business Logic.

## 1. Decentralized Token Verification Strategy

To prevent the `sso-service` from becoming a system bottleneck, we will implement **local JWT verification**:

1. **`sso-service`** acts as the JWS Issuer. It rotates RSA keys and publishes the Public JWKS at `/.well-known/jwks.json`.
2. **API Gateway** and **Internal Services** (Identity, Student, etc.) will use a shared auth module (see section 4).
3. Upon startup, these services will download the JWK set from the `sso-service`.
4. Every incoming request is verified **locally** without making a network call to the `sso-service`.

### JWKS Caching and Rotation Rules
- Cache JWKS with a short TTL (recommended: 5 minutes).
- On unknown `kid`, force a one-time immediate JWKS refresh before rejecting.
- Keep last-known-good JWKS in memory if `sso-service` is temporarily unavailable.
- `sso-service` key rotation overlap should guarantee old public keys remain exposed until all unexpired tokens signed by them are naturally expired.

## 2. Inter-Service Communication Patterns

### Synchronous (REST/gRPC)
Used for critical validation during the login flow:
- `sso-service` -> `identity-service`: `POST /internal/users/verify-credentials` (Validates password hash).
- `sso-service` -> `mfa-service`: `POST /internal/mfa/verify-totp` (Validates TOTP).
- `sso-service` -> `mfa-service`: `POST /internal/mfa/verify-otp` (Validates OTP).

### Asynchronous (NATS JetStream)
Used for state propagation and high-latency tasks:
- **`auth.logout`**: Published by `sso-service` when a session is revoked. All services subscribe to this to invalidate local caches.
- **`auth.user.registered`**: Published by `identity-service`. Other services (like `student-service`) use this to initialize their local state if needed.

## 3. Database Decoupling

Each service will have its own database (or schema) to ensure independent scalability and schema evolution:

| Service | Primary Tables |
| :--- | :--- |
| **`identity-service`** | `users` |
| **`sso-service`** | `jwk_keys`, `refresh_token` |
| **`mfa-service`** | `mfa_secrets` (extracted from `users`) |
| **`student-service`** | `student_profiles` |

### Handling Cross-Service Joins
The JPA `@OneToOne` between `User` and `StudentProfile` will be removed. The `student_profiles` table will simply store a `user_id` (UUID). When the `student-service` needs user details, it will fetch them via an internal REST call to the `identity-service`.

## 4. Shared Reusable Auth Module

To avoid duplicating auth logic in every downstream service, introduce a dedicated reusable module (e.g. `com.bbmovie:auth-security-common` or `auth-security-spring-boot-starter`) that provides:
- `JoseAuthenticationFilter`: Standard security filter for local JWT validation.
- JWKS client + cache + refresh policy (TTL, force-refresh on unknown `kid`, fallback to last-known-good set).
- Logout blacklist subscriber/client (consumes `auth.logout` and checks revoked `sid`/`jti`).
- `SecurityUtils`: Shared helpers for extracting `user_id` and `roles` from the `SecurityContext`.

Keep transport-agnostic DTOs in `com.bbmovie:common`, while auth enforcement and JWT/JWKS mechanics live in this dedicated module.

## 5. Related Design Docs
- `internal-api-contracts.md`
- `migration-runbook.md`
