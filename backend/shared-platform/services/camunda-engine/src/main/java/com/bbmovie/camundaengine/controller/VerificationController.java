package com.bbmovie.camundaengine.controller;

import com.bbmovie.camundaengine.controller.openapi.VerificationControllerOpenApi;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/verification")
public class VerificationController implements VerificationControllerOpenApi {

    private final RuntimeService runtimeService;

    @Autowired
    public VerificationController(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @PostMapping("/start")
    public Map<String, String> startVerification(@RequestBody Map<String, Object> variables) {
        // Ensure applicationId is a UUID
        if (variables.containsKey("applicationId") && variables.get("applicationId") instanceof String) {
            variables.put("applicationId", UUID.fromString((String) variables.get("applicationId")));
        }

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("studentVerificationProcess", variables);

        Map<String, String> response = new HashMap<>();
        response.put("processInstanceId", instance.getId());
        response.put("status", "STARTED");
        return response;
    }
}
