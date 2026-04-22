package bbmovie.commerce.payment_orchestrator_service.domain.model;

import bbmovie.commerce.payment_orchestrator_service.domain.enums.PaymentStatus;
import bbmovie.commerce.payment_orchestrator_service.domain.event.PaymentCreatedEvent;
import bbmovie.commerce.payment_orchestrator_service.domain.event.PaymentCancelledEvent;
import bbmovie.commerce.payment_orchestrator_service.domain.event.PaymentDomainEvent;
import bbmovie.commerce.payment_orchestrator_service.domain.event.PaymentExpiredEvent;
import bbmovie.commerce.payment_orchestrator_service.domain.event.PaymentFailedEvent;
import bbmovie.commerce.payment_orchestrator_service.domain.event.PaymentRefundedEvent;
import bbmovie.commerce.payment_orchestrator_service.domain.event.PaymentSucceededEvent;
import bbmovie.commerce.payment_orchestrator_service.domain.exception.InvalidPaymentStateException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Payment {
    private final OrchestratorPaymentId id;
    private final Money amount;
    private final PaymentMethod method;
    private PaymentStatus status;
    private ProviderPaymentId providerPaymentId;
    private final String userId;
    private final String userEmail;
    private final String purpose;
    private final Map<String, String> metadata;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<PaymentDomainEvent> domainEvents = new ArrayList<>();

    private Payment(
            OrchestratorPaymentId id,
            Money amount,
            PaymentMethod method,
            String userId,
            String userEmail,
            String purpose,
            Map<String, String> metadata,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.method = Objects.requireNonNull(method, "method");
        this.status = PaymentStatus.PENDING;
        this.userId = userId;
        this.userEmail = userEmail;
        this.purpose = purpose;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        addDomainEvent(new PaymentCreatedEvent(this.id, this.createdAt));
    }

    public static Payment create(Money amount, PaymentMethod method) {
        return create(amount, method, null, null, null, Map.of());
    }

    public static Payment create(
            Money amount,
            PaymentMethod method,
            String userId,
            String userEmail,
            String purpose,
            Map<String, String> metadata
    ) {
        Instant now = Instant.now();
        return new Payment(OrchestratorPaymentId.newId(), amount, method, userId, userEmail, purpose, metadata, now, now);
    }

    public static Payment rehydrate(
            OrchestratorPaymentId id,
            Money amount,
            PaymentMethod method,
            PaymentStatus status,
            ProviderPaymentId providerPaymentId,
            String userId,
            String userEmail,
            String purpose,
            Map<String, String> metadata,
            Instant createdAt,
            Instant updatedAt
    ) {
        Payment payment = new Payment(id, amount, method, userId, userEmail, purpose, metadata, createdAt, updatedAt);
        payment.status = Objects.requireNonNull(status, "status");
        payment.providerPaymentId = providerPaymentId;
        payment.domainEvents.clear();
        return payment;
    }

    public OrchestratorPaymentId id() {
        return id;
    }

    public Money amount() {
        return amount;
    }

    public PaymentMethod method() {
        return method;
    }

    public PaymentStatus status() {
        return status;
    }

    public ProviderPaymentId providerPaymentId() {
        return providerPaymentId;
    }

    public String userId() {
        return userId;
    }

    public String userEmail() {
        return userEmail;
    }

    public String purpose() {
        return purpose;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void registerProviderPayment(ProviderPaymentId providerId) {
        ensureCanTransitionTo(PaymentStatus.PENDING);
        this.providerPaymentId = Objects.requireNonNull(providerId, "providerId");
        this.updatedAt = Instant.now();
    }

    public void markSucceeded(ProviderPaymentId providerId) {
        ensureCanTransitionTo(PaymentStatus.SUCCEEDED);
        this.providerPaymentId = Objects.requireNonNull(providerId, "providerId");
        this.status = PaymentStatus.SUCCEEDED;
        this.updatedAt = Instant.now();
        addDomainEvent(new PaymentSucceededEvent(this.id, this.providerPaymentId, this.updatedAt));
    }

    public void markFailed(String reason) {
        ensureCanTransitionTo(PaymentStatus.FAILED);
        this.status = PaymentStatus.FAILED;
        this.updatedAt = Instant.now();
        addDomainEvent(new PaymentFailedEvent(this.id, reason, this.updatedAt));
    }

    public void cancel(String reason) {
        ensureCanTransitionTo(PaymentStatus.CANCELLED);
        this.status = PaymentStatus.CANCELLED;
        this.updatedAt = Instant.now();
        addDomainEvent(new PaymentCancelledEvent(this.id, reason, this.updatedAt));
    }

    public void timeout(String reason) {
        ensureCanTransitionTo(PaymentStatus.EXPIRED);
        this.status = PaymentStatus.EXPIRED;
        this.updatedAt = Instant.now();
        addDomainEvent(new PaymentExpiredEvent(this.id, reason, this.updatedAt));
    }

    public void refund() {
        ensureCanTransitionTo(PaymentStatus.REFUNDED);
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = Instant.now();
        addDomainEvent(new PaymentRefundedEvent(this.id, this.updatedAt));
    }

    private void ensureCanTransitionTo(PaymentStatus nextStatus) {
        if (nextStatus == PaymentStatus.PENDING) {
            if (this.status != PaymentStatus.PENDING) {
                throw new InvalidPaymentStateException(this.status, nextStatus);
            }
            return;
        }
        if (!PaymentTransition.isValidTransition(this.status, nextStatus)) {
            throw new InvalidPaymentStateException(this.status, nextStatus);
        }
    }

    private void addDomainEvent(PaymentDomainEvent event) {
        domainEvents.add(event);
    }

    public List<PaymentDomainEvent> pullDomainEvents() {
        List<PaymentDomainEvent> out = List.copyOf(domainEvents);
        domainEvents.clear();
        return out;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Payment payment)) {
            return false;
        }
        return Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

