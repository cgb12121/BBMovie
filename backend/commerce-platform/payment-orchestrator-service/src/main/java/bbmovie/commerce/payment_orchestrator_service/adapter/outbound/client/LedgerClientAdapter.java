package bbmovie.commerce.payment_orchestrator_service.adapter.outbound.client;

import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.client.LedgerClientPort;
import org.springframework.stereotype.Component;

@Component
public class LedgerClientAdapter implements LedgerClientPort {
    @Override
    public void postPaymentEntry(String orchestratorPaymentId) {
        // TODO: integrate with billing-ledger-service via REST/Feign.
    }
}

