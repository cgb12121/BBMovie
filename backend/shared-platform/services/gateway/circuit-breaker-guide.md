# API Gateway Guide: Circuit Breakers

This document provides a comprehensive guide to understanding and implementing the Circuit Breaker pattern in the Spring Cloud Gateway using Resilience4j.

## 1. The Core Concept: What is a Circuit Breaker?

Think of an electrical circuit breaker in your house. If a faulty appliance draws too much power, the breaker "trips" to cut off electricity, protecting the rest of your house from damage.

In software, a Circuit Breaker does the same thing: it stops a problem in one microservice (e.g., it's failing or slow) from causing a cascading failure across the entire system. It wraps network calls and monitors them for failures.

A Circuit Breaker has three states:

1.  **CLOSED (Normal):** Requests flow normally. The breaker counts failures. If the failure rate exceeds a threshold, it trips and moves to the **OPEN** state.

2.  **OPEN (Tripped):** For a configured timeout (e.g., 30 seconds), the breaker **immediately rejects all further requests** without even trying to call the failing service. This gives the service time to recover. After the timeout, it moves to **HALF-OPEN**.

3.  **HALF-OPEN (Trial):** The breaker allows a single, trial request through.
    *   If it **succeeds**, the service is considered recovered, and the breaker moves back to **CLOSED**.
    *   If it **fails**, the service is still unhealthy, and the breaker moves back to **OPEN** to begin the timeout again.

---

## 2. Implementation in the Gateway

The following configuration in `application.yml` enables a global circuit breaker for all routes.

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - name: CircuitBreaker
          args:
            name: myCircuitBreaker
            fallbackUri: forward:/service-fallback
```

### Configuration Breakdown

*   `default-filters`: Applies this filter to **every route** in the gateway.
*   `name: CircuitBreaker`: Uses the built-in Spring Cloud Gateway filter, which is powered by Resilience4j.
*   `name: myCircuitBreaker`: A crucial identifier. Because it's a default filter, **all routes will share this same breaker instance**. If `movie-service` trips the breaker, calls to `user-service` will also be blocked. For more granular control, apply this filter per-route with unique names.
*   `fallbackUri: forward:/service-fallback`: This is the fallback mechanism. When the circuit is OPEN, instead of just failing, the gateway will **internally forward the request** to an endpoint within the gateway application itself. The client is unaware this forwarding happened.

### The Fallback Controller

The `fallbackUri` points to a controller you must create within the gateway to handle these fallback requests.

```java
// In your gateway application
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import java.util.Map;

@RestController
public class FallbackController {

    @GetMapping("/service-fallback")
    public Mono<ResponseEntity<Map<String, String>>> serviceFallback() {
        Map<String, String> response = Map.of(
            "errorCode", "SERVICE_UNAVAILABLE",
            "message", "The service is temporarily unavailable. Please try again later."
        );
        // Return a 503 Service Unavailable status
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
}
```

---

## 3. Frontend Interaction

The frontend is responsible for interpreting the `503 Service Unavailable` status code and redirecting the user to an appropriate error page.

This is typically handled in a global HTTP interceptor.

**Conceptual JavaScript Example:**
```javascript
// Using a library like axios
axios.interceptors.response.use(
  (response) => response, // Pass through successful responses
  (error) => {
    // If the server sends a response with a status code
    if (error.response && error.response.status === 503) {
      // Redirect the user to a dedicated maintenance/error page
      window.location.href = '/503-service-unavailable';
    }
    return Promise.reject(error);
  }
);
```

---

## 4. Advanced Configuration

You can finely tune every aspect of the circuit breaker's behavior in `application.yml`.

```yaml
# ==================================================
# RESILIENCE4J CUSTOM CIRCUIT BREAKER CONFIGURATION
# ==================================================
resilience4j.circuitbreaker:
  # Define reusable configuration templates
  configs:
    default:
      registerHealthIndicator: true
      slidingWindowType: COUNT_BASED  # COUNT_BASED or TIME_BASED
      slidingWindowSize: 100          # The last 100 requests are considered
      minimumNumberOfCalls: 10        # Min requests before it can trip
      permittedNumberOfCallsInHalfOpenState: 3 # Allow 3 trial requests
      automaticTransitionFromOpenToHalfOpenEnabled: true
      waitDurationInOpenState: 10s    # Stay OPEN for 10 seconds
      failureRateThreshold: 50        # If 50% of requests fail, trip the circuit
      recordExceptions:
        - java.io.IOException
        - java.util.concurrent.TimeoutException
      ignoreExceptions:
        - com.bbmovie.gateway.exception.BusinessLogicException # Don't trip on these

  # Define specific instances of circuit breakers
  instances:
    # This name matches the 'name' arg in the gateway filter
    my-custom-breaker:
      baseConfig: default             # Inherit from the default config
      failureRateThreshold: 60        # Override: trip at 60% failure rate
      waitDurationInOpenState: 30s    # Override: stay open for 30 seconds

    # Another example for a different, more critical service
    payment-service-breaker:
      baseConfig: default
      failureRateThreshold: 25        # Trip much faster for payments
      slidingWindowSize: 50
```

### Key Properties Explained:

*   `slidingWindowType`: `COUNT_BASED` (last `N` calls) or `TIME_BASED` (calls in the last `N` seconds).
*   `slidingWindowSize`: The number of requests or seconds to consider for the failure rate.
*   `minimumNumberOfCalls`: How many calls must occur in the window before the breaker can trip. Prevents tripping on initial, isolated failures.
*   `failureRateThreshold`: The failure rate percentage that will cause the circuit to open.
*   `waitDurationInOpenState`: How long the circuit stays open before transitioning to half-open.
*   `permittedNumberOfCallsInHalfOpenState`: The number of "trial" requests to allow when in the half-open state.
*   `recordExceptions` / `ignoreExceptions`: Fine-grained control over which exceptions count as a failure.