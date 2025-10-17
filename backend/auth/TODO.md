# Auth Service Refactoring Plan

This document outlines the critical and recommended changes to improve the resilience, security, and quality of the `auth` service.

## 🚨 ASAP - Critical Fixes

These issues represent significant architectural flaws or bugs that should be addressed immediately.

### 1. Decouple NATS Connection from Application Startup

-   **Status**: should be done now

-   **Problem:** `NatsConfig.java` uses a blocking `Nats.connect()` call in its `@Bean` definition. If the NATS server is unavailable when the `auth` service starts, the entire application will freeze and fail to initialize. An authentication service must be able to start independently of the messaging system.
-   **Solution:** Refactor `NatsConfig.java` to use the same asynchronous `SmartLifecycle` pattern present in the other services (`search`, `payment`). The connection should be attempted in a background thread, and services that depend on it (like the event producers) must be event-driven, waiting for a `NatsConnectionEvent` before trying to use the connection.

### 2. Fix Stale JOSE Provider in `AuthServiceImpl`

-   **Status**: should be done now

-   **Problem:** The `AuthServiceImpl` constructor injects the `joseProviderStrategyContext.getActiveProvider()` and stores it in a `final` field. If an admin uses the `JoseStrategyController` to change the active strategy at runtime, `AuthServiceImpl` will continue using the old, stale provider for all new tokens it generates, causing authentication failures.
-   **Solution:**
    1.  Inject the `JoseProviderStrategyContext` itself into `AuthServiceImpl`, not the provider.
    2.  In any method that needs to generate a token (e.g., `login`), call `joseProviderStrategyContext.getActiveProvider()` at that moment to ensure you are always using the current, active provider.

### 3. Remove Blocking I/O from `IpAnonymityFilter`

-   **Status**: will be marked as deprecated to let the gateway handle this, removed from `SecurityConfig`. The code should stay there for a little longer to show its legacy to the project xd. 
-   **NOTE**: NEED DOUBLE-CHECK IF IT AFFECTS THE APP

-   **Problem:** The `IpAnonymityFilter` calls `anonymityCheckService.isAnonymous()`, which in turn uses a synchronous `RestTemplate` to make external network calls (`IpApiService`, `IpWhoLocationService`). This means **every single incoming API request** to your service can be blocked waiting for these slow network calls, which will severely degrade performance and exhaust your thread pool under load.
-   **Solution:**
    1.  Rewrite the anonymity provider services (`IpApiService`, etc.) to use the `CompletableFuture`.
    2.  The `isAnonymous` check should return a `CompletableFuture<Boolean>`.
    3.  The `IpAnonymityFilter` needs to be rewritten to work with this asynchronous result. This is complex in a `OncePerRequestFilter`. A more modern approach is to move this check into a `WebFilter` if possible, or handle the `CompletableFuture` result within the filter and subscribe to it to continue the chain.

---

## 💡 Optional Improvements & Refinements

These are not critical bugs, but addressing them will significantly improve the security and maintainability of the service.

### 1. Implement Refresh Token Rotation

-   **Problem:** The `RefreshTokenService` currently uses a "Multi-Use" pattern. If a refresh token is compromised, an attacker can use it repeatedly to generate new access tokens until it expires.
-   **Solution:** Implement refresh token rotation. When a refresh token is used, it should be immediately invalidated, and the response should include both a new access token *and* a new refresh token. This is the current security best practice.

### 2. Externalize Hardcoded URLs 

-   **Status**: Done.

-   **Problem:** `OAuth2LoginSuccessHandler` contains a hardcoded redirect URL: `http://localhost:3000`.
-   **Solution:** Move this URL into your `application.properties` or `application.yml` file so it can be easily changed for different environments (development, staging, production).

### 3. Refine NATS Stream Setup Logic (from email service? as I didn't find any on auth service or payment service)

-   **Status**: Should be Done now.

-   **Problem:** The `setupStream()` method in `NatsConfig` is called on every `CONNECTED` and `RECONNECTED` event. While it's idempotent (safe to run multiple times), it's inefficient.
-   **Solution:** Use an `AtomicBoolean` flag to ensure the stream setup logic is only executed once on the very first successful connection.

### 4. Clean Up Code and Configuration

-   **Status**: Jwt.io will be marked as deprecated, only nimbus will be used as it is enterprise-graded, compatible with built-in nimbus from spring.

-   **Simplify JOSE Strategies:** You have multiple implementations for JWS (`jwt.io`, `nimbus`). Consider standardizing on one (Nimbus is generally more feature-complete and robust) and removing the others to reduce complexity and code surface area.
-   **Remove `@SuppressWarnings("all")`:** In `JoseDebugController`, replace this with more specific suppressions or fix the underlying warnings.
