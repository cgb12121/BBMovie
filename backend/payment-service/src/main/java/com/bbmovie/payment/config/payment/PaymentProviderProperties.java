package com.bbmovie.payment.config.payment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.Map;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentProviderProperties {

    private Map<String, ProviderConfig> providers;

    @Getter
    @Setter
    public static class ProviderConfig {
        private boolean enabled;
        private String reason;
    }
}