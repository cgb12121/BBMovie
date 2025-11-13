package com.bbmovie.payment.controller;

import com.bbmovie.payment.config.payment.PaymentProviderRegistry;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

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
        String principal = authentication.getPrincipal().toString();
        registry.setProviderStatus(provider, true, username, principal, reason);
    }

    @PostMapping("/{provider}/disable")
    public void disable(@PathVariable String provider, @RequestParam String reason, Authentication authentication) {
        String username = authentication.getName();
        String principal = authentication.getPrincipal().toString();
        registry.setProviderStatus(provider, false, username, principal, reason);
    }

    @GetMapping("/{provider}")
    public PaymentProviderRegistry.ProviderStatus status(@PathVariable String provider, Authentication authentication) {
        String username = authentication.getName();
        String principal = authentication.getPrincipal().toString();
        log.info("[{}] {} requested status of provider {}", LocalDateTime.now(), username, principal);
        return registry.getStatus(provider);
    }
}