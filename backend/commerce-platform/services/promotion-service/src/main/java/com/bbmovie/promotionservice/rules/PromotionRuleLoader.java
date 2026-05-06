package com.bbmovie.promotionservice.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionRuleLoader {
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final AtomicReference<List<PromotionRule>> cachedRules = new AtomicReference<>(Collections.emptyList());
    private volatile String checksum = "";

    @Value("${promotion.rules.resource:classpath:promotions-rules.json}")
    private String ruleResource;

    @PostConstruct
    public void initialLoad() {
        reloadIfChanged();
    }

    @Scheduled(fixedDelayString = "${promotion.rules.reload-ms:30000}")
    public void reloadIfChanged() {
        try {
            if (ruleResource == null) {
                throw new IllegalStateException("Promotion rules resource is not set");
            }
            Resource resource = resourceLoader.getResource(ruleResource);
            if (!resource.exists()) {
                log.warn("Promotion rules resource does not exist: {}", ruleResource);
                return;
            }
            byte[] data;
            try (InputStream in = resource.getInputStream()) {
                data = in.readAllBytes();
            }
            String nextChecksum = sha256(data);
            if (nextChecksum.equals(checksum)) {
                return;
            }
            PromotionRuleSet ruleSet = objectMapper.readValue(data, PromotionRuleSet.class);
            List<PromotionRule> rules = ruleSet.getRules() == null ? Collections.emptyList() : List.copyOf(ruleSet.getRules());
            cachedRules.set(rules);
            checksum = nextChecksum;
            log.info("Promotion rules reloaded. count={}", rules.size());
        } catch (Exception ex) {
            log.error("Failed to reload promotion rules", ex);
        }
    }

    public List<PromotionRule> currentRules() {
        return cachedRules.get();
    }

    private String sha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        return HexFormat.of().formatHex(hash);
    }
}
