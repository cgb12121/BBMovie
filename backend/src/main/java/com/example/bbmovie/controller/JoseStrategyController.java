package com.example.bbmovie.controller;

import com.example.bbmovie.security.jose.JoseProviderStrategy;
import com.example.bbmovie.security.jose.JoseProviderStrategyContext;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@RestController
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
@RequestMapping("/admin/jose")
public class JoseStrategyController {

    private final JoseProviderStrategyContext context;
    private final ApplicationContext applicationContext;


    @Autowired
    public JoseStrategyController(JoseProviderStrategyContext context, ApplicationContext applicationContext) {
        this.context = context;
        this.applicationContext = applicationContext;
    }

    @PostMapping("/switch/{strategy}")
    public ResponseEntity<String> switchStrategy(@PathVariable String strategy) {
        try {
            context.changeProvider(strategy);
            return ResponseEntity.ok("Switched to: " + strategy);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.badRequest().body("Unable to Switched to: " + strategy);
        }
    }

    @GetMapping("/active")
    public Map<String, String> currentStrategy() {
        String activeName = getStrategyName(context.getActiveProvider());
        String previousName = getStrategyName(context.getPreviousProvider());

        return Map.of(
                "active", activeName != null ? activeName : "null",
                "previous", previousName != null ? previousName : "null"
        );
    }

    private String getStrategyName(JoseProviderStrategy provider) {
        return context.getAll().entrySet().stream()
                .filter(e -> e.getValue() == provider)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
