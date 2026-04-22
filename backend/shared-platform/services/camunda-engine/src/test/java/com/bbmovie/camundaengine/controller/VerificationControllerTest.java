package com.bbmovie.camundaengine.controller;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerificationControllerTest {

    @Test
    void startVerificationConvertsApplicationIdAndStartsProcess() {
        RuntimeService runtimeService = mock(RuntimeService.class);
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getId()).thenReturn("proc-123");
        when(runtimeService.startProcessInstanceByKey(eq("studentVerificationProcess"), org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(processInstance);

        VerificationController controller = new VerificationController(runtimeService);

        Map<String, Object> variables = new HashMap<>();
        String applicationId = UUID.randomUUID().toString();
        variables.put("applicationId", applicationId);
        variables.put("fullName", "Test User");

        Map<String, String> result = controller.startVerification(variables);

        assertEquals("proc-123", result.get("processInstanceId"));
        assertEquals("STARTED", result.get("status"));
        assertTrue(variables.get("applicationId") instanceof UUID);
        verify(runtimeService).startProcessInstanceByKey("studentVerificationProcess", variables);
    }
}
