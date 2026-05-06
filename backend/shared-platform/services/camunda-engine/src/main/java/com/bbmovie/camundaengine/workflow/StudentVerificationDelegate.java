package com.bbmovie.camundaengine.workflow;

import com.bbmovie.camundaengine.dto.StudentVerificationContext;
import com.bbmovie.camundaengine.dto.UniversityMatch;
import com.bbmovie.camundaengine.entity.University;
import com.bbmovie.camundaengine.service.UniversityRegistryService;
import com.bbmovie.camundaengine.enums.VerificationOutcome;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Component("studentVerificationDelegate")
public class StudentVerificationDelegate implements JavaDelegate {

    private final UniversityRegistryService universityRegistryService;
    private final RestTemplate restTemplate;

    @Value("${drools.service.url:http://localhost:8081/api/rules/verify-student}")
    private String droolsServiceUrl;

    @Value("${rust.ai.service.url:http://localhost:8686/api/process-batch}")
    private String rustAiServiceUrl;

    @Autowired
    public StudentVerificationDelegate(UniversityRegistryService universityRegistryService) {
        this.universityRegistryService = universityRegistryService;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Starting student verification for execution {}", execution.getId());

        // 1. Prepare Context from Process Variables
        StudentVerificationContext context = new StudentVerificationContext();
        context.setApplicationId((java.util.UUID) execution.getVariable("applicationId"));
        context.setUserId((String) execution.getVariable("userId"));
        context.setFullName((String) execution.getVariable("fullName"));
        context.setUniversityName((String) execution.getVariable("universityName"));
        context.setUniversityDomain((String) execution.getVariable("universityDomain"));
        context.setUniversityEmail((String) execution.getVariable("universityEmail"));
        context.setGraduationYear((Integer) execution.getVariable("graduationYear"));

        // 2. Delegate to Rust AI Context Refinery for OCR/Extraction
        String documentBase64 = (String) execution.getVariable("documentContent");
        String documentName = (String) execution.getVariable("documentName");
        if (documentBase64 != null) {
            try {
                Map<String, Object> rustReq = new HashMap<>();
                Map<String, String> item = new HashMap<>();
                item.put("filename", documentName);
                item.put("base64_content", documentBase64);
                rustReq.put("requests", Collections.singletonList(item));

                log.info("Calling Rust AI Service for extraction: {}", documentName);

                @SuppressWarnings("null")
                JsonNode rustResp = restTemplate.postForObject(rustAiServiceUrl, rustReq, JsonNode.class);
                
                if (rustResp == null) {
                    throw new IllegalArgumentException("Rust AI Service returned null response");
                }

                if (rustResp != null && rustResp.get("success").asBoolean()) {
                    JsonNode resultItem = rustResp.get("data").get(0).get("result");
                    if (resultItem != null) {
                        String ocrText = "";
                        if (resultItem.has("ocr_text")) {
                            ocrText = resultItem.get("ocr_text").asText();
                        } else if (resultItem.has("text")) {
                            ocrText = resultItem.get("text").asText();
                        }
                        
                        context.setExtractedText(ocrText);
                        context.setDocumentType(documentName.toLowerCase().endsWith(".pdf") ? "pdf" : "image");
                        
                        if (ocrText.contains("ID:")) {
                            context.setExtractedStudentId("EXTRACTED_BY_RUST");
                        }
                        
                        if (resultItem.has("vision_description")) {
                            log.debug("Vision description: {}", resultItem.get("vision_description").asText());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Rust AI Service call failed", e);
            }
        }

        // 3. University Registry Match
        Optional<University> domainMatch = universityRegistryService.findByDomain(context.getUniversityDomain());
        if (domainMatch.isPresent()) {
            University uni = domainMatch.get();
            context.setUniversityMatch(UniversityMatch.builder()
                    .name(uni.getName())
                    .domain(uni.getDomains())
                    .country(uni.getCountry())
                    .confidence(1.0)
                    .matched(true)
                    .build());
        } else {
             Optional<University> nameMatch = universityRegistryService.bestMatchByName(context.getUniversityName());
             nameMatch.ifPresent(university -> context.setUniversityMatch(UniversityMatch.builder()
                     .name(university.getName())
                     .matched(true)
                     .confidence(0.7)
                     .build()));
        }

        // 4. Call Drools Service
        try {
            @SuppressWarnings("null")
            StudentVerificationContext result = restTemplate.postForObject(droolsServiceUrl, context, StudentVerificationContext.class);
            if (result != null) {
                log.info("Drools result: score={}, outcome={}", result.getScore(), result.getOutcome());
                execution.setVariable("verificationScore", result.getScore());
                execution.setVariable("verificationOutcome", result.getOutcome().toString());
                execution.setVariable("scoreReasons", result.getScoreReasons());
            }
        } catch (Exception e) {
            log.error("Failed to call Drools service", e);
            execution.setVariable("verificationOutcome", VerificationOutcome.NEEDS_REVIEW.name());
            execution.setVariable("error", "Drools service unreachable");
        }
    }
}
