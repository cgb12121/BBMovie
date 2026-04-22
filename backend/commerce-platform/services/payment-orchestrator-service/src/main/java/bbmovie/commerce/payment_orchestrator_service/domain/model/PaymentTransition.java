package bbmovie.commerce.payment_orchestrator_service.domain.model;

import bbmovie.commerce.payment_orchestrator_service.domain.enums.PaymentStatus;

import java.util.Map;
import java.util.Set;

public final class PaymentTransition {

    private static final Map<PaymentStatus, Set<PaymentStatus>> VALID_TRANSITIONS = Map.of(
            PaymentStatus.PENDING, Set.of(  PaymentStatus.SUCCEEDED, PaymentStatus.FAILED, PaymentStatus.CANCELLED, PaymentStatus.EXPIRED),
            PaymentStatus.SUCCEEDED, Set.of(PaymentStatus.REFUNDED),
            PaymentStatus.FAILED, Set.of(),
            PaymentStatus.CANCELLED, Set.of(),
            PaymentStatus.EXPIRED, Set.of(),
            PaymentStatus.REFUNDED, Set.of()
    );

    private PaymentTransition() {
    }

    public static boolean isValidTransition(PaymentStatus from, PaymentStatus to) {
        return VALID_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public static Set<PaymentStatus> getNextStates(PaymentStatus current) {
        return VALID_TRANSITIONS.getOrDefault(current, Set.of());
    }
}

