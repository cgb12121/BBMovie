package com.bbmovie.camundaengine.workflow;

import com.bbmovie.camundaengine.enums.VerificationOutcome;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component("notifyAuthServiceDelegate")
public class NotifyAuthServiceDelegate implements JavaDelegate {

    private final RestTemplate restTemplate;

    @Value("${auth.service.url:http://localhost:8080/api/student-program/internal/applications}")
    private String authServiceUrl;

    public NotifyAuthServiceDelegate() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void execute(DelegateExecution execution) {
        Object applicationId = execution.getVariable("applicationId");
        Object outcomeObj = execution.getVariable("verificationOutcome");
        String outcome = outcomeObj != null
                ? outcomeObj.toString()
                : VerificationOutcome.NEEDS_REVIEW.name();
        String scoreReasons = execution.getVariable("scoreReasons") != null
                ? execution.getVariable("scoreReasons").toString()
                : "";

        log.info("Notifying auth-service about application {}: outcome={}", applicationId, outcome);

        String url = String.format("%s/%s/finalize?status=%s&message=%s",
                authServiceUrl, applicationId, outcome, scoreReasons);

        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL is required");
        }

        try {
            restTemplate.postForLocation(url, null);
        } catch (Exception e) {
            log.error("Failed to notify auth-service", e);
            // In a real system, we might want to retry or raise an incident
        }
    }
}
