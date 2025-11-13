package com.bbmovie.payment.config.payment;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Component
public class PaymentProviderRegistry {

    private final Map<String, ProviderStatus> providers = new ConcurrentHashMap<>();

    public void setProviderStatus(String provider, boolean enabled, String by, String principle, String reason) {
        providers.put(provider, new ProviderStatus(enabled, by, reason));
        log.info("[{}] Payment provider {} is now {} by {} [{}]",
                LocalDateTime.now(), provider, enabled ? "enabled" : "disabled", by, principle);
    }

    public ProviderStatus getStatus(String provider) {
        return providers.getOrDefault(provider, new ProviderStatus(false, "Provider not registered"));
    }

    public record ProviderStatus(boolean enabled, String by, String reason) {
        public ProviderStatus(boolean enabled, String reason) {
            this(enabled, "System", reason);
        }
    }
}