package com.bbmovie.camundaengine.workflow;

import com.bbmovie.camundaengine.dto.StudentVerificationContext;
import com.bbmovie.camundaengine.entity.University;
import com.bbmovie.camundaengine.repository.UniversityRepository;
import com.bbmovie.camundaengine.enums.VerificationOutcome;
import com.bbmovie.camundaengine.service.UniversityRegistryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentVerificationDelegateTest {

    @Test
    void executeSetsVerificationVariablesOnSuccessfulDroolsCall() {
        UniversityRepository universityRepository = mock(UniversityRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        UniversityRegistryService universityRegistryService = new UniversityRegistryService(universityRepository, entityManager);
        StudentVerificationDelegate delegate = new StudentVerificationDelegate(universityRegistryService);
        RestTemplate restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(delegate, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(delegate, "droolsServiceUrl", "http://drools/api/rules/verify-student");
        ReflectionTestUtils.setField(delegate, "rustAiServiceUrl", "http://rust/api/process-batch");

        University university = University.builder()
                .name("BB University")
                .domains("bb.edu")
                .country("VN")
                .build();
        when(universityRepository.findByDomainsContainingIgnoreCase("bb.edu")).thenReturn(Optional.of(university));

        Map<String, Object> rustResponse = Map.of(
                "success", true,
                "data", List.of(Map.of("result", Map.of(
                        "ocr_text", "ID: 12345",
                        "vision_description", "text detected"
                )))
        );
        ObjectMapper mapper = new ObjectMapper();
        when(restTemplate.postForObject(eq("http://rust/api/process-batch"), any(), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(mapper.valueToTree(rustResponse));

        StudentVerificationContext droolsResult = new StudentVerificationContext();
        droolsResult.setScore(90);
        droolsResult.setOutcome(VerificationOutcome.AUTO_APPROVE);
        droolsResult.setScoreReasons(List.of("High confidence"));
        when(restTemplate.postForObject(eq("http://drools/api/rules/verify-student"), any(), eq(StudentVerificationContext.class)))
                .thenReturn(droolsResult);

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getId()).thenReturn("exec-1");
        when(execution.getVariable("applicationId")).thenReturn(UUID.randomUUID());
        when(execution.getVariable("userId")).thenReturn("user-1");
        when(execution.getVariable("fullName")).thenReturn("Test User");
        when(execution.getVariable("universityName")).thenReturn("BB University");
        when(execution.getVariable("universityDomain")).thenReturn("bb.edu");
        when(execution.getVariable("universityEmail")).thenReturn("student@bb.edu");
        when(execution.getVariable("graduationYear")).thenReturn(2028);
        when(execution.getVariable("documentContent")).thenReturn("BASE64");
        when(execution.getVariable("documentName")).thenReturn("card.pdf");

        assertDoesNotThrow(() -> delegate.execute(execution));
        verify(execution).setVariable("verificationScore", 90);
        verify(execution).setVariable("verificationOutcome", "AUTO_APPROVE");
        verify(execution).setVariable("scoreReasons", List.of("High confidence"));
    }

    @Test
    void executeFallsBackToNeedsReviewWhenDroolsUnavailable() {
        UniversityRepository universityRepository = mock(UniversityRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        UniversityRegistryService universityRegistryService = new UniversityRegistryService(universityRepository, entityManager);
        StudentVerificationDelegate delegate = new StudentVerificationDelegate(universityRegistryService);
        RestTemplate restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(delegate, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(delegate, "droolsServiceUrl", "http://drools/api/rules/verify-student");

        when(universityRepository.findByDomainsContainingIgnoreCase("none.edu")).thenReturn(Optional.empty());
        when(universityRepository.findByNameContainingIgnoreCase("Unknown University")).thenReturn(Optional.empty());
        when(restTemplate.postForObject(eq("http://drools/api/rules/verify-student"), any(), eq(StudentVerificationContext.class)))
                .thenThrow(new RuntimeException("service down"));

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getId()).thenReturn("exec-2");
        when(execution.getVariable("applicationId")).thenReturn(UUID.randomUUID());
        when(execution.getVariable("userId")).thenReturn("user-2");
        when(execution.getVariable("fullName")).thenReturn("Unknown");
        when(execution.getVariable("universityName")).thenReturn("Unknown University");
        when(execution.getVariable("universityDomain")).thenReturn("none.edu");
        when(execution.getVariable("universityEmail")).thenReturn("unknown@none.edu");
        when(execution.getVariable("graduationYear")).thenReturn(2027);
        when(execution.getVariable("documentContent")).thenReturn(null);
        when(execution.getVariable("documentName")).thenReturn("card.jpg");

        assertDoesNotThrow(() -> delegate.execute(execution));
        verify(execution).setVariable("verificationOutcome", VerificationOutcome.NEEDS_REVIEW.name());
        verify(execution).setVariable("error", "Drools service unreachable");
    }
}
