package bbmovie.commerce.payment_orchestrator_service.application.port.outbound.payment;

import bbmovie.commerce.payment_orchestrator_service.application.command.CreatePaymentCommand;
import bbmovie.commerce.payment_orchestrator_service.application.port.result.PaymentResult;

public interface PaymentCreationPort extends PaymentProviderPort {
    PaymentResult createPayment(CreatePaymentCommand cmd);
}

