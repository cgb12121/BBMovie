# Payment Orchestrator Transaction Flow Diagrams

This document visualizes how a transaction moves through the current codebase, including:

- Internal methods being called
- Class-to-class dependencies
- Port/adapter boundaries

## 1) Checkout Transaction Flow (with Idempotency AOP)

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant PC as PaymentController
    participant IA as IdempotencyAspect
    participant IDS as IdempotencyService
    participant RC as RedisIdempotencyAdapter
    participant IR as IdempotencyRecordRepository
    participant COS as CheckoutOrchestrationService
    participant PAM as PaymentApiMapper
    participant PAY as Payment (Aggregate)
    participant PR as PaymentProviderRegistry
    participant CPP as PaymentCreationPort (Stripe/PayPal)
    participant PRA as PaymentRepositoryAdapter
    participant JPA as PaymentJpaRepository
    participant DEP as PaymentDomainEventPublisher
    participant EPA as CommerceEventPublisherAdapter
    participant Kafka as Kafka

    Client->>PC: POST /api/v1/payments/checkout\ncheckout(idempotencyKey, req)
    PC->>IA: checkout(...) intercepted by @Idempotent
    IA->>IDS: execute(CHECKOUT, key, req, CheckoutResponse.class, action)
    IDS->>RC: get(op, key, responseType)
    alt Cache hit
        RC-->>IDS: Optional<CheckoutResponse>
        IDS-->>IA: IdempotencyResult(isReplay=true, value)
        IA-->>PC: return cached CheckoutResponse
        PC-->>Client: 200 CheckoutResponse
    else Cache miss
        IDS->>IR: findByOperationAndIdempotencyKey(op, key)
        alt DB replay found
            IR-->>IDS: existing IdempotencyRecordEntity
            IDS-->>IA: replayFromRecord(...)
            IA-->>PC: return replay CheckoutResponse
            PC-->>Client: 200 CheckoutResponse
        else First execution
            IDS->>COS: action.get() -> checkout(key, req)
            COS->>PAM: toCreatePaymentCommand(req)
            COS->>PAY: Payment.create(cmd.amount(), mapMethod(req.provider()))
            COS->>PR: getRequiredCreationProvider(req.provider())
            PR-->>COS: PaymentCreationPort
            COS->>CPP: createPayment(cmd)
            CPP-->>COS: PaymentResult
            COS->>PAY: registerProviderPayment(providerPaymentId)
            alt providerResult.status == SUCCEEDED
                COS->>PAY: markSucceeded(providerPaymentId)
            else providerResult.status == FAILED or CANCELLED
                COS->>PAY: markFailed("Provider returned status...")
            end
            COS->>PRA: save(payment, provider)
            PRA->>JPA: save(PaymentEntity)
            JPA-->>PRA: PaymentEntity
            PRA-->>COS: Payment (rehydrated)
            COS->>DEP: publish(saved, provider)
            DEP->>PAY: pullDomainEvents()
            loop each PaymentDomainEvent
                DEP->>EPA: publish(eventType, paymentId, payload)
                EPA->>Kafka: send(topic, paymentId, json) [if enabled]
            end
            COS->>PAM: toCheckoutResponse(PaymentResult)
            COS-->>IDS: CheckoutResponse
            IDS->>IR: saveAndFlush(IdempotencyRecordEntity)
            IDS->>RC: put(op, key, response) [best effort]
            IDS-->>IA: IdempotencyResult(isReplay=false, value)
            IA-->>PC: CheckoutResponse
            PC-->>Client: 200 CheckoutResponse
        end
    end
```

## 2) Refund Transaction Flow

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant RC as RefundController
    participant IA as IdempotencyAspect
    participant IDS as IdempotencyService
    participant ROS as RefundOrchestrationService
    participant PR as PaymentProviderRegistry
    participant RP as RefundPort (Stripe/PayPal)
    participant PREPO as PaymentRepositoryPort
    participant PAY as Payment (Aggregate)
    participant DEP as PaymentDomainEventPublisher
    participant EPA as CommerceEventPublisherAdapter

    Client->>RC: POST /api/v1/payments/refunds\nrefund(idempotencyKey, req)
    RC->>IA: refund(...) intercepted by @Idempotent
    IA->>IDS: execute(REFUND, key, req, RefundResponse.class, action)
    IDS->>ROS: action.get() -> refund(key, req)
    ROS->>PREPO: findByOrchestratorPaymentId(...)
    PREPO-->>ROS: Payment
    ROS->>PR: getRequiredRefundProvider(req.provider())
    PR-->>ROS: RefundPort
    ROS->>RP: refund(orchestratorPaymentId, providerPaymentId)
    RP-->>ROS: PaymentResult
    alt providerResult.status == REFUNDED
        ROS->>PAY: refund()
        ROS->>PREPO: save(payment, provider)
    end
    ROS->>DEP: publish(payment, provider)
    DEP->>PAY: pullDomainEvents()
    DEP->>EPA: publish(...)
    ROS-->>IDS: RefundResponse
    IDS-->>IA: IdempotencyResult(value)
    IA-->>RC: RefundResponse
    RC-->>Client: 200 RefundResponse
```

## 3) Webhook Transaction Flow

```mermaid
sequenceDiagram
    autonumber
    actor Provider as Stripe/PayPal
    participant WC as WebhookController
    participant WOS as WebhookOrchestrationService
    participant PR as PaymentProviderRegistry
    participant WP as WebhookPort (StripePaymentProvider/PaypalPaymentProvider)
    participant DEDUP as JpaWebhookDedupAdapter
    participant PREPO as PaymentRepositoryPort
    participant PAY as Payment (Aggregate)
    participant DEP as PaymentDomainEventPublisher
    participant EP as EventPublisherPort

    Provider->>WC: POST /api/v1/webhooks/{provider}
    WC->>WOS: handle(provider, rawBody, headers)
    WOS->>PR: getRequiredWebhookProvider(providerType)
    PR-->>WOS: WebhookPort
    WOS->>WP: verifyWebhook(payload)
    alt invalid signature
        WP-->>WOS: false
        WOS-->>WC: WebhookHandleResult(INVALID_SIGNATURE)
        WC-->>Provider: 401 INVALID_SIGNATURE
    else valid signature
        WP-->>WOS: true
        WOS->>WP: parseWebhook(payload)
        WP-->>WOS: ProviderWebhookEvent
        WOS->>DEDUP: recordIfFirst(provider, eventId, rawBody)
        alt duplicate
            DEDUP-->>WOS: false
            WOS-->>WC: DUPLICATE_IGNORED
            WC-->>Provider: 200 DUPLICATE_IGNORED
        else first event
            DEDUP-->>WOS: true
            WOS->>PREPO: findByProviderPaymentId(provider, providerPaymentId)
            alt Payment matched
                PREPO-->>WOS: Optional<Payment>
                alt normalizedStatus == SUCCEEDED and payment == PENDING
                    WOS->>PAY: markSucceeded(providerPaymentId)
                    WOS->>PREPO: save(payment, provider)
                else normalizedStatus == FAILED and payment == PENDING
                    WOS->>PAY: markFailed("Provider webhook status: FAILED")
                    WOS->>PREPO: save(payment, provider)
                else normalizedStatus == REFUNDED and payment == SUCCEEDED
                    WOS->>PAY: refund()
                    WOS->>PREPO: save(payment, provider)
                end
                WOS->>DEP: publish(payment, provider)
                alt domain events published > 0
                    WOS-->>WC: ACK:<status>
                    WC-->>Provider: 200 ACK
                else no domain event
                    WOS->>EP: publish(mappedEventType, aggregateKey, payload)
                    WOS-->>WC: ACK:<status>
                    WC-->>Provider: 200 ACK
                end
            else Payment not found
                PREPO-->>WOS: Optional.empty()
                WOS->>EP: publish(mappedEventType, providerPaymentId, payload)
                WOS-->>WC: ACK:<status>
                WC-->>Provider: 200 ACK
            end
        end
    end
```

## 4) Dependency Diagram (Classes and Ports)

```mermaid
classDiagram
    direction LR

    class PaymentController {
      +checkout(String, CheckoutRequest) CheckoutResponse
    }
    class RefundController {
      +refund(String, RefundRequest) RefundResponse
    }
    class WebhookController {
      +handle(String, String, HttpServletRequest) ResponseEntity~String~
    }

    class CheckoutUseCase
    class RefundUseCase
    class WebhookUseCase

    class CheckoutOrchestrationService {
      +checkout(String, CheckoutRequest) CheckoutResponse
      -mapMethod(ProviderType) PaymentMethod
    }
    class RefundOrchestrationService {
      +refund(String, RefundRequest) RefundResponse
    }
    class WebhookOrchestrationService {
      +handle(String, String, Map~String,String~) WebhookHandleResult
    }

    class IdempotencyAspect {
      +applyIdempotency(ProceedingJoinPoint, Idempotent) Object
    }
    class IdempotencyService {
      +execute(...)
      -replayFromRecord(...)
      -saveResult(...)
    }

    class PaymentProviderRegistry {
      +getRequiredCreationProvider(ProviderType) PaymentCreationPort
      +getRequiredRefundProvider(ProviderType) RefundPort
      +getRequiredWebhookProvider(ProviderType) WebhookPort
    }

    class Payment {
      +create(Money, PaymentMethod) Payment
      +registerProviderPayment(ProviderPaymentId)
      +markSucceeded(ProviderPaymentId)
      +markFailed(String)
      +cancel(String)
      +timeout(String)
      +refund()
      +pullDomainEvents() List~PaymentDomainEvent~
    }

    class PaymentDomainEventPublisher {
      +publish(Payment, ProviderType) int
    }
    class PaymentRepositoryPort
    class EventPublisherPort
    class WebhookDedupPort
    class IdempotencyPort
    class PaymentCreationPort
    class RefundPort
    class WebhookPort

    class PaymentRepositoryAdapter
    class CommerceEventPublisherAdapter
    class JpaWebhookDedupAdapter
    class RedisIdempotencyAdapter
    class StripePaymentProvider
    class PaypalPaymentProvider

    PaymentController --> CheckoutUseCase
    RefundController --> RefundUseCase
    WebhookController --> WebhookUseCase

    CheckoutOrchestrationService ..|> CheckoutUseCase
    RefundOrchestrationService ..|> RefundUseCase
    WebhookOrchestrationService ..|> WebhookUseCase

    IdempotencyAspect --> IdempotencyService
    IdempotencyService --> IdempotencyPort

    CheckoutOrchestrationService --> PaymentProviderRegistry
    CheckoutOrchestrationService --> PaymentRepositoryPort
    CheckoutOrchestrationService --> PaymentDomainEventPublisher
    CheckoutOrchestrationService --> Payment

    RefundOrchestrationService --> PaymentProviderRegistry
    RefundOrchestrationService --> PaymentRepositoryPort
    RefundOrchestrationService --> PaymentDomainEventPublisher
    RefundOrchestrationService --> Payment

    WebhookOrchestrationService --> PaymentProviderRegistry
    WebhookOrchestrationService --> WebhookDedupPort
    WebhookOrchestrationService --> PaymentRepositoryPort
    WebhookOrchestrationService --> PaymentDomainEventPublisher
    WebhookOrchestrationService --> EventPublisherPort
    WebhookOrchestrationService --> Payment

    PaymentDomainEventPublisher --> EventPublisherPort

    PaymentProviderRegistry --> PaymentCreationPort
    PaymentProviderRegistry --> RefundPort
    PaymentProviderRegistry --> WebhookPort

    PaymentRepositoryAdapter ..|> PaymentRepositoryPort
    CommerceEventPublisherAdapter ..|> EventPublisherPort
    JpaWebhookDedupAdapter ..|> WebhookDedupPort
    RedisIdempotencyAdapter ..|> IdempotencyPort
    StripePaymentProvider ..|> PaymentCreationPort
    StripePaymentProvider ..|> RefundPort
    StripePaymentProvider ..|> WebhookPort
    PaypalPaymentProvider ..|> PaymentCreationPort
    PaypalPaymentProvider ..|> RefundPort
    PaypalPaymentProvider ..|> WebhookPort
```

## 5) Domain State Machine (Current Implementation)

```mermaid
stateDiagram-v2
    [*] --> PENDING : Payment.create()
    PENDING --> SUCCEEDED : markSucceeded(providerPaymentId)
    PENDING --> FAILED : markFailed(reason)
    PENDING --> CANCELLED : cancel(reason)
    PENDING --> EXPIRED : timeout(reason)
    SUCCEEDED --> REFUNDED : refund()

    FAILED --> [*]
    CANCELLED --> [*]
    EXPIRED --> [*]
    REFUNDED --> [*]
```

