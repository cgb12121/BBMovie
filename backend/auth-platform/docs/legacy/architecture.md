# Legacy Architecture: auth-service

The current `auth-service` is a monolithic security and identity provider built with **Spring Boot 3.x** and **Java 21**. It serves as the centralized authority for authentication and user authorization across the BBMovie platform.

## Technology Stack

- **Framework**: Spring Boot 3.5.3
- **Security**: Spring Security (Stateful Session Management for OAuth2 flows, Stateless JWT for API access).
- **Identity**: Spring Data JPA with MySQL.
- **Tokens**: JWS (JSON Web Signature) using RSA-256 (via `nimbus-jose-jwt`).
- **MFA**: TOTP (Time-based One-Time Password) via Sam Stevens TOTP library.
- **Messaging**: NATS JetStream for event-driven asynchronous tasks (Email, SMS, Revocation).
- **Service Discovery**: Netflix Eureka Client.
- **Caching/Blacklisting**: Redis (Lettuce) for session tracking and JWT revocation.
- **Workflow**: Integration with Camunda Engine for student verification.

## Core Architectural Components

### 1. Identity Provider (IdP)
The service maintains the primary User store in MySQL. It handles the full lifecycle of a user account: registration, password hashing (Argon2 by default), email verification, and profile metadata.

### 2. Authorization Server (JWS Issuer)
The service acts as a self-contained Authorization Server. It:
- Generates RSA Key pairs.
- Automatically rotates keys every 7 days (`KeyRotation.java`).
- Signs Access Tokens and Refresh Tokens.
- Publishes public keys at `/.well-known/jwks.json`.

### 3. Session & Device Management
Unlike basic stateless JWT systems, this monolith tracks active sessions in the `refresh_token` table. This allows the system to:
- Detect logins from new devices.
- List all devices currently logged into an account.
- Revoke specific device sessions (Logout from "Other Devices").
- Broadcast revocation events via NATS to ensure tokens are blocked system-wide.

### 4. Distributed Events (NATS)
The monolith heavily relies on NATS JetStream to decouple high-latency tasks from the request-response cycle:
- **`auth.registration`**: Triggers verification emails.
- **`auth.forgot_password`**: Triggers reset emails.
- **`auth.logout`**: Informs the system to blacklist a specific Session ID (SID).
- **`auth.abac`**: Forces a token refresh when user roles or statuses change.

## External Integrations

| System | Integration Method | Purpose |
| :--- | :--- | :--- |
| **MySQL** | JPA / JDBC | Persistent storage for Users, Profiles, and Tokens. |
| **Redis** | Spring Data Redis | High-speed blacklisting of revoked tokens. |
| **NATS** | JetStream | Event broadcasting for async workflows. |
| **Camunda** | REST API | Orchestration of the Student Verification process. |
| **Eureka** | Spring Cloud | Registration for service discovery. |
| **OAuth2** | Spring Security | Social Login (Google, GitHub, Facebook, etc.). |
