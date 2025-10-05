package com.bbmovie.email.service.email;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class EmailServiceFactory {
    private final Map<String, EmailService> strategies;
    @Getter
    private final EmailService defaultStrategy;
    private final List<String> rotationOrder;

    @Value( "${app.email.rotation.enabled}")
    private boolean isRotationEnabled;

    @Autowired
    public EmailServiceFactory(
            Map<String, EmailService> strategyMap,
            @Value("${app.mail.default}") String defaultStrategyName,
            @Value("${app.mail.rotation-order}") String rotationOrder
    ) {
        this.strategies = strategyMap;
        this.defaultStrategy = strategyMap.getOrDefault(defaultStrategyName,
                strategyMap.values().stream().findFirst().orElseThrow(() ->
                        new IllegalArgumentException("No default email strategy found"))
        );
        if (!isRotationEnabled) {
            this.rotationOrder = List.of(defaultStrategyName);
        } else {
            this.rotationOrder = Arrays.asList(rotationOrder.split(","));
        }
    }

    public List<EmailService> getRotationStrategies() {
        return rotationOrder.stream()
                .map(strategies::get)
                .toList();
    }
}
