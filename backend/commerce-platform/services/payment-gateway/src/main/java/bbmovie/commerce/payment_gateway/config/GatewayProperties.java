package bbmovie.commerce.payment_gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {
    private final PaymentOrchestrator paymentOrchestrator = new PaymentOrchestrator();
    private final Promotion promotion = new Promotion();

    @Getter
    @Setter
    public static class PaymentOrchestrator {
        private String baseUrl;
    }

    @Getter
    @Setter
    public static class Promotion {
        private String baseUrl;
    }
}
