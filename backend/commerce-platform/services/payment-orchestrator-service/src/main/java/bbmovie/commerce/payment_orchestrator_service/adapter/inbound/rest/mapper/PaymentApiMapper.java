package bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.mapper;

import bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.dto.CheckoutRequest;
import bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.dto.CheckoutResponse;
import bbmovie.commerce.payment_orchestrator_service.adapter.inbound.rest.dto.RefundResponse;
import bbmovie.commerce.payment_orchestrator_service.application.command.CreatePaymentCommand;
import bbmovie.commerce.payment_orchestrator_service.application.port.result.PaymentResult;
import bbmovie.commerce.payment_orchestrator_service.domain.model.Money;

public final class PaymentApiMapper {

    private PaymentApiMapper() {
    }

    public static CreatePaymentCommand toCreatePaymentCommand(CheckoutRequest req) {
        return new CreatePaymentCommand(
                req.userId(),
                req.userEmail(),
                req.provider(),
                new Money(req.amount(), req.currency()),
                req.purpose(),
                req.metadata()
        );
    }

    public static CheckoutResponse toCheckoutResponse(PaymentResult r) {
        return new CheckoutResponse(
                r.orchestratorPaymentId().value().toString(),
                r.provider(),
                r.providerPaymentId().value(),
                r.status(),
                r.paymentUrl(),
                r.clientSecret(),
                r.providerMetadata()
        );
    }

    public static RefundResponse toRefundResponse(PaymentResult r) {
        return new RefundResponse(
                r.orchestratorPaymentId().value().toString(),
                r.provider(),
                r.providerPaymentId().value(),
                r.status(),
                r.providerMetadata()
        );
    }
}

