# Legacy Folder Structure: auth-service

> Deprecated reference. Keep only for migration mapping and rollback context.

The `auth-service` is a monolithic Spring Boot application organized by functional packages. Below is a detailed breakdown of the package structure and the responsibilities of each directory.

## Package: `com.bbmovie.auth`

### `common/`
- **`ValidationHandler.java`**: Utility for processing Spring `BindingResult` errors and converting them into standardized `ApiResponse` objects.

### `config/`
- **`NatsConfig.java`**: Configuration for NATS JetStream connectivity, including a lifecycle manager for background connection attempts.
- **`OpenApiConfig.java`**: Swagger/OpenAPI 3.0 definitions for API documentation.
- **`RedisConfig.java`**: Configuration for Lettuce-based Redis connections, used for session blacklisting and caching.
- **`TotpConfig.java`**: Configuration for MFA/TOTP generation using the Sam Stevens TOTP library, including NTP time synchronization options.

### `constant/`
- **`AuthErrorMessages.java`**, **`UserErrorMessages.java`**: Centralized string constants for exception messages and API error responses.

### `controller/`
- **`AuthController.java`**: Entry point for local registration, login, logout, and password management.
- **`JwkController.java`**: Exposes the public RSA keys via the standard `/.well-known/jwks.json` endpoint.
- **`MfaController.java`**: Handles MFA setup (QR code generation) and TOTP verification.
- **`SessionController.java`**: Provides endpoints for managing logged-in devices and revoking sessions.
- **`StudentProgramController.java`**: Manages the student verification lifecycle.
- **`advice/`**: Contains `GlobalExceptionHandler.java` for centralized `@ControllerAdvice`.
- **`openapi/`**: Interface-based Swagger definitions to keep controllers clean of documentation annotations.

### `dto/`
- Contains Request/Response objects, Record-based DTOs, and event objects for internal and NATS-based communication.

### `entity/`
- **`User.java`**: The core identity entity (implements `UserDetails`).
- **`StudentProfile.java`**: Stores student-specific metadata (linked 1:1 to User).
- **`jose/JoseKey.java`**: Entity for persisted RSA keys used in JWS signing.
- **`jose/RefreshToken.java`**: Persisted refresh tokens for session persistence across devices.
- **`enumerate/`**: Enums for Roles, Permissions, AuthProviders, and Verification Statuses.

### `exception/`
- Custom runtime exceptions mapped to specific HTTP status codes (e.g., `EmailAlreadyExistsException`, `BadLoginException`).

### `repository/`
- Spring Data JPA repositories for all entities (MySQL backed).

### `security/`
- **`SecurityConfig.java`**: Main Spring Security filter chain configuration.
- **`jose/`**: Internal logic for RSA key rotation (`KeyRotation.java`), caching (`KeyCache.java`), and the authentication filter (`JoseAuthenticationFilter.java`).
- **`oauth2/`**: Custom handlers and resolvers for Google, GitHub, Facebook, and Discord social logins.
- **`spel/`**: SpEL-based security evaluators for complex `@PreAuthorize` checks.

### `service/`
- **`auth/`**: Core business logic for authentication, registration, and sessions.
- **`nats/`**: JetStream producers for broadcasting events (Email, SMS, Logout, ABAC changes).
- **`student/`**: Logic for student verification, including integration with the external Camunda engine.

### `utils/`
- Helper classes for device detection (`DeviceInfoUtils`), IP parsing, and referral code generation.
