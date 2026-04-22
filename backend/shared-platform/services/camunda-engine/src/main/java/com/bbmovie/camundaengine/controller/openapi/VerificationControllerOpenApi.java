package com.bbmovie.camundaengine.controller.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@Tag(name = "Verification Workflow", description = "Camunda verification process APIs")
public interface VerificationControllerOpenApi {
    @Operation(summary = "Start verification workflow")
    Map<String, String> startVerification(@RequestBody Map<String, Object> variables);
}

