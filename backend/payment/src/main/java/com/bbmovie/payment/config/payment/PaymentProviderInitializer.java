package com.bbmovie.payment.config.payment;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class PaymentProviderInitializer {

    private final PaymentProviderProperties properties;
    private final PaymentProviderRegistry registry;

    @Autowired
    public PaymentProviderInitializer(PaymentProviderProperties properties, PaymentProviderRegistry registry) {
        this.properties = properties;
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        properties.getProviders()
                .forEach((name, cfg) -> {
                    registry.setProviderStatus(name, cfg.isEnabled(), "System", "System", cfg.getReason());
                    log.info("Payment provider {} is {} by System on start up", name, cfg.isEnabled() ? "enabled" : "disabled");
                });
    }
}
