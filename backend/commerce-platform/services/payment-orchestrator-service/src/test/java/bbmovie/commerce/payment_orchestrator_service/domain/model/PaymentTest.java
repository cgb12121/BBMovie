package bbmovie.commerce.payment_orchestrator_service.domain.model;

import bbmovie.commerce.payment_orchestrator_service.domain.exception.InvalidPaymentStateException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentTest {

    @Test
    void should_create_with_payment_created_event() {
        Payment payment = Payment.create(new Money(BigDecimal.TEN, "USD"), PaymentMethod.CREDIT_CARD);

        assertEquals(1, payment.pullDomainEvents().size());
        assertEquals(0, payment.pullDomainEvents().size());
    }

    @Test
    void should_not_refund_when_not_succeeded() {
        Payment payment = Payment.create(new Money(BigDecimal.TEN, "USD"), PaymentMethod.E_WALLET);

        assertThrows(InvalidPaymentStateException.class, payment::refund);
    }

    @Test
    void should_allow_refund_after_success() {
        Payment payment = Payment.create(new Money(BigDecimal.TEN, "USD"), PaymentMethod.DEBIT_CARD);
        payment.pullDomainEvents(); // clear created event

        payment.markSucceeded(new ProviderPaymentId("pi_123"));
        payment.refund();

        assertEquals(2, payment.pullDomainEvents().size());
    }

    @Test
    void should_allow_cancel_from_pending() {
        Payment payment = Payment.create(new Money(BigDecimal.ONE, "USD"), PaymentMethod.E_WALLET);
        payment.pullDomainEvents(); // clear created event

        payment.cancel("user_request");

        assertEquals(payment.status(), bbmovie.commerce.payment_orchestrator_service.domain.enums.PaymentStatus.CANCELLED);
        assertEquals(1, payment.pullDomainEvents().size());
    }

    @Test
    void should_allow_timeout_from_pending() {
        Payment payment = Payment.create(new Money(BigDecimal.ONE, "USD"), PaymentMethod.E_WALLET);
        payment.pullDomainEvents(); // clear created event

        payment.timeout("gateway_timeout");

        assertEquals(payment.status(), bbmovie.commerce.payment_orchestrator_service.domain.enums.PaymentStatus.EXPIRED);
        assertEquals(1, payment.pullDomainEvents().size());
    }

    @Test
    void should_reject_invalid_transition_with_valid_transitions_context() {
        Payment payment = Payment.create(new Money(BigDecimal.ONE, "USD"), PaymentMethod.CREDIT_CARD);
        payment.markFailed("declined");

        InvalidPaymentStateException ex = assertThrows(InvalidPaymentStateException.class, payment::refund);
        assertTrue(ex.getValidTransitions().isEmpty());
    }
}

