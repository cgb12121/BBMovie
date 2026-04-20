package bbmovie.commerce.payment_orchestrator_service.adapter.outbound.provider.paypal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaypalProperties {

    @Value("${app.payment.paypal.client-id:}")
    private String clientId;

    @Value("${app.payment.paypal.client-secret:}")
    private String clientSecret;

    @Value("${app.payment.paypal.base-url:https://api-m.sandbox.paypal.com}")
    private String baseUrl;

    @Value("${app.payment.paypal.webhook-id:}")
    private String webhookId;

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getWebhookId() {
        return webhookId;
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    public static final class PaypalConstants {
        public static final String CAPTURE_INTENT = "CAPTURE";
        public static final String SHIPPING_PREFERENCE = "NO_SHIPPING";
        public static final String USER_ACTION_PAY_NOW = "PAY_NOW";
        public static final String PREFER_RETURN_REPRESENTATION = "return=representation";
        public static final String VERIFICATION_STATUS_SUCCESS = "SUCCESS";
        public static final String APPROVE_REL = "approve";

        public static final String TRANSMISSION_ID_HEADER = "paypal-transmission-id";
        public static final String TRANSMISSION_TIME_HEADER = "paypal-transmission-time";
        public static final String TRANSMISSION_SIG_HEADER = "paypal-transmission-sig";
        public static final String CERT_URL_HEADER = "paypal-cert-url";
        public static final String AUTH_ALGO_HEADER = "paypal-auth-algo";

        public static final String PROVIDER_STATUS_CREATED = "CREATED";
        public static final String PAYPAL_STATUS_COMPLETED = "COMPLETED";
    }
}

