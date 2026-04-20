package bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.stripe;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StripeProperties {

    @Value("${app.payment.stripe.secret-key:}")
    private String secretKey;

    @Value("${app.payment.stripe.webhook-secret:}")
    private String webhookSecret;

    public String getSecretKey() {
        return secretKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }


    public static final class StripeConstrains {
        public static final String HEADER_STRIPE_SIGNATURE = "stripe-signature";
    }
}

