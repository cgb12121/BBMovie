package bbmovie.commerce.payment_orchestrator_service.application.port.outbound.client;

public interface SubscriptionClientPort {
    void notifyPaymentUpdate(String orchestratorPaymentId);
}

