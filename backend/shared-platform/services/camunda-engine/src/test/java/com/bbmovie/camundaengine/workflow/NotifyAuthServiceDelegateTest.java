package com.bbmovie.camundaengine.workflow;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotifyAuthServiceDelegateTest {

    @Test
    @SuppressWarnings("null")
    void executeUsesDefaultOutcomeWhenMissing() {
        NotifyAuthServiceDelegate delegate = new NotifyAuthServiceDelegate();
        RestTemplate restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(delegate, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(delegate, "authServiceUrl", "http://auth/api/student-program/internal/applications");

        DelegateExecution execution = mock(DelegateExecution.class);
        UUID applicationId = UUID.randomUUID();
        when(execution.getVariable("applicationId")).thenReturn(applicationId);
        when(execution.getVariable("verificationOutcome")).thenReturn(null);
        when(execution.getVariable("scoreReasons")).thenReturn("manual check");

        delegate.execute(execution);

        String expectedUrl = "http://auth/api/student-program/internal/applications/" + applicationId +
                "/finalize?status=NEEDS_REVIEW&message=manual check";
        verify(restTemplate).postForLocation(eq(expectedUrl), eq(null));
    }

    @Test
    @SuppressWarnings("null")
    void executeSwallowsNotificationErrors() {
        NotifyAuthServiceDelegate delegate = new NotifyAuthServiceDelegate();
        RestTemplate restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(delegate, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(delegate, "authServiceUrl", "http://auth/api/student-program/internal/applications");

        DelegateExecution execution = mock(DelegateExecution.class);
        UUID applicationId = UUID.randomUUID();
        when(execution.getVariable("applicationId")).thenReturn(applicationId);
        when(execution.getVariable("verificationOutcome")).thenReturn("AUTO_APPROVE");
        when(execution.getVariable("scoreReasons")).thenReturn("");

        doThrow(new RuntimeException("network issue")).when(restTemplate).postForLocation(anyString(), eq(null));

        assertDoesNotThrow(() -> delegate.execute(execution));
    }
}
