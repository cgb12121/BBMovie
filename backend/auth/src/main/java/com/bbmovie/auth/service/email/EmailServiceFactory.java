package com.bbmovie.auth.service.email;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class EmailServiceFactory {

    private final Map<String, EmailService> strategies;
    private final List<String> strategyNamesInOrder;
    @Getter
    private final EmailService defaultStrategy;
    private int currentRotationIndex = 0;

    @Autowired
    public EmailServiceFactory(
            Map<String, EmailService> strategyMap,
            @Value("${app.mail.default}") String defaultStrategyName
    ) {
        this.strategies = strategyMap;
        this.strategyNamesInOrder = new ArrayList<>(strategyMap.keySet());
        this.defaultStrategy = strategyMap.get(defaultStrategyName);
    }

    public EmailService getStrategy(String strategyName) {
        return strategies.get(strategyName);
    }

    public EmailService rotateStrategy() {
        if (strategyNamesInOrder.isEmpty()) return null;

        String name = strategyNamesInOrder.get(currentRotationIndex);
        currentRotationIndex = (currentRotationIndex + 1) % strategyNamesInOrder.size();
        return strategies.get(name);
    }
}
