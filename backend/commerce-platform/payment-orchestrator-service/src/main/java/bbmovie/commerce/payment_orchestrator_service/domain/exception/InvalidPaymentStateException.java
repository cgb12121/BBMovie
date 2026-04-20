package bbmovie.commerce.payment_orchestrator_service.domain.exception;

import bbmovie.commerce.payment_orchestrator_service.domain.enums.PaymentStatus;
import bbmovie.commerce.payment_orchestrator_service.domain.model.PaymentTransition;

import java.util.Set;

public class InvalidPaymentStateException extends DomainException {

    private final PaymentStatus currentState;
    private final PaymentStatus attemptedState;
    private final Set<PaymentStatus> validTransitions;

    public InvalidPaymentStateException(PaymentStatus currentState, PaymentStatus attemptedState) {
        super(String.format(
                "Invalid payment state transition: current=%s, attempted=%s, valid=%s",
                currentState,
                attemptedState,
                PaymentTransition.getNextStates(currentState)
        ));
        this.currentState = currentState;
        this.attemptedState = attemptedState;
        this.validTransitions = PaymentTransition.getNextStates(currentState);
    }

    public PaymentStatus getCurrentState() {
        return currentState;
    }

    public PaymentStatus getAttemptedState() {
        return attemptedState;
    }

    public Set<PaymentStatus> getValidTransitions() {
        return validTransitions;
    }
}

