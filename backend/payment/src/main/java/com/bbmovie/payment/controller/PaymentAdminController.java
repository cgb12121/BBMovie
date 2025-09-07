package com.bbmovie.payment.controller;

import com.bbmovie.payment.config.PaymentProviderRegistry;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequestMapping("/admin/payments/providers")
public class PaymentAdminController {

    private final PaymentProviderRegistry registry;

    @Autowired
    public PaymentAdminController(PaymentProviderRegistry registry) {
        this.registry = registry;
    }

    @PostMapping("/{provider}/enable")
    public void enable(@PathVariable String provider, @RequestParam String reason, Authentication authentication) {
        String username = authentication.getName();
        registry.setProviderStatus(provider, true, username, reason);
    }

    @PostMapping("/{provider}/disable")
    public void disable(@PathVariable String provider, @RequestParam String reason, Authentication authentication) {
        String username = authentication.getName();
        registry.setProviderStatus(provider, false, username, reason);
    }

    @GetMapping("/{provider}")
    public PaymentProviderRegistry.ProviderStatus status(@PathVariable String provider) {
        return registry.getStatus(provider);
    }
}