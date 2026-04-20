package bbmovie.commerce.payment_orchestrator_service.application.port.outbound.client;

public interface LedgerClientPort {
    void postPaymentEntry(String orchestratorPaymentId);
}

