package com.bbmovie.payment.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Component
public class PaymentProviderRegistry {

    private final Map<String, ProviderStatus> providers = new ConcurrentHashMap<>();

    public void setProviderStatus(String provider, boolean enabled, String by, String reason) {
        providers.put(provider, new ProviderStatus(enabled, by, reason));
        log.info("Payment provider {} is now {} by {}", provider, enabled ? "enabled" : "disabled", by);
    }

    public ProviderStatus getStatus(String provider) {
        return providers.getOrDefault(provider, new ProviderStatus(false, "Provider not registered"));
    }

    public record ProviderStatus(boolean enabled, String by, String reason) {
        public ProviderStatus(boolean enabled) {
            this(enabled, "System","Enabled at: " + LocalDateTime.now());
        }

        public ProviderStatus(boolean enabled, String reason) {
            this(enabled, "System", reason);
        }

        public ProviderStatus(boolean enabled, String by, LocalDateTime enabledAt) {
            this(enabled, by, "Enabled at: " + enabledAt);
        }
    }
}