package com.example.bbmovie.controller.admin;

import com.example.bbmovie.security.jose.JoseProviderStrategy;
import com.example.bbmovie.security.jose.JoseProviderStrategyContext;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Log4j2
@RestController
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
@RequestMapping("/admin/jose")
public class JoseStrategyController {

    private final JoseProviderStrategyContext joseProviderStrategyContext;

    @Autowired
    public JoseStrategyController(JoseProviderStrategyContext joseProviderStrategyContext) {
        this.joseProviderStrategyContext = joseProviderStrategyContext;
    }

    @PostMapping("/switch")
    public ResponseEntity<String> switchStrategy(@RequestBody Map<String, String> strategyParam) {
        try {
            String strategy = strategyParam.get("strategy");
            joseProviderStrategyContext.changeProvider(strategy);
            return ResponseEntity.ok("Switched to: " + strategy);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.badRequest().body("Unable to Switched strategy");
        }
    }

    @GetMapping("/active")
    public Map<String, String> currentStrategy() {
        String activeName = getStrategyName(joseProviderStrategyContext.getActiveProvider());
        String previousName = getStrategyName(joseProviderStrategyContext.getPreviousProvider());

        return Map.of(
                "active", activeName != null ? activeName : "null",
                "previous", previousName != null ? previousName : "null"
        );
    }

    private String getStrategyName(JoseProviderStrategy provider) {
        return joseProviderStrategyContext.getAll().entrySet().stream()
                .filter(e -> e.getValue() == provider)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}