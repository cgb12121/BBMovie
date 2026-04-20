package bbmovie.commerce.payment_orchestrator_service.adapter.outbound.client;

import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.client.SubscriptionClientPort;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionClientAdapter implements SubscriptionClientPort {
    @Override
    public void notifyPaymentUpdate(String orchestratorPaymentId) {
        // TODO: integrate with subscription-service via REST/Feign.
    }
}

