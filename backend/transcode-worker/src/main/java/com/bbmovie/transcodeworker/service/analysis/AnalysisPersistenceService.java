package com.bbmovie.transcodeworker.service.analysis;

import com.bbmovie.transcodeworker.persistence.entity.ComplexityProfileEntity;
import com.bbmovie.transcodeworker.persistence.entity.QualityReportEntity;
import com.bbmovie.transcodeworker.persistence.entity.ValidationReportEntity;
import com.bbmovie.transcodeworker.persistence.repository.ComplexityProfileRepository;
import com.bbmovie.transcodeworker.persistence.repository.QualityReportRepository;
import com.bbmovie.transcodeworker.persistence.repository.ValidationReportRepository;
import com.bbmovie.transcodeworker.service.complexity.dto.ComplexityProfile;
import com.bbmovie.transcodeworker.service.quality.dto.QualityReport;
import com.bbmovie.transcodeworker.service.validation.encode.dto.ValidationReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisPersistenceService {

    private final ObjectMapper objectMapper;
    private final ComplexityProfileRepository complexityProfileRepository;
    private final ValidationReportRepository validationReportRepository;
    private final QualityReportRepository qualityReportRepository;

    private static final int ANALYSIS_VERSION = 1;

    public void saveComplexityProfile(ComplexityProfile profile) {
        try {
            ComplexityProfileEntity entity = complexityProfileRepository
                    .findByUploadIdAndAnalysisVersion(profile.uploadId(), ANALYSIS_VERSION)
                    .orElseGet(ComplexityProfileEntity::new);

            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID().toString());
            }

            entity.setUploadId(profile.uploadId());
            entity.setAnalysisVersion(ANALYSIS_VERSION);
            entity.setContentClass(profile.contentClass());
            entity.setComplexityScore(profile.complexityScore());
            entity.setFeatureScoresJson(objectMapper.writeValueAsString(profile.featureScores()));
            entity.setRecipeHintsJson(objectMapper.writeValueAsString(profile.recipeHints()));

            entity.setCreatedAt(Instant.now());
            complexityProfileRepository.save(entity);
        } catch (Exception e) {
            log.warn("Failed to persist complexity profile for uploadId={}", profile.uploadId(), e);
        }
    }

    public void saveValidationReport(String uploadId, String renditionSuffix, ValidationReport report) {
        try {
            ValidationReportEntity entity = validationReportRepository
                    .findByUploadIdAndRenditionSuffixAndAnalysisVersion(uploadId, renditionSuffix, ANALYSIS_VERSION)
                    .orElseGet(ValidationReportEntity::new);

            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID().toString());
            }

            entity.setUploadId(uploadId);
            entity.setRenditionSuffix(renditionSuffix);
            entity.setAnalysisVersion(ANALYSIS_VERSION);
            entity.setStatus(report.status().name());
            entity.setViolationsJson(objectMapper.writeValueAsString(report.violations()));
            entity.setFfprobeArtifactUri(report.artifactUri());
            entity.setCreatedAt(Instant.now());

            validationReportRepository.save(entity);
        } catch (Exception e) {
            log.warn("Failed to persist validation report for uploadId={}, rendition={}", uploadId, renditionSuffix, e);
        }
    }

    public void saveQualityReport(String uploadId, QualityReport report) {
        try {
            QualityReportEntity entity = qualityReportRepository
                    .findByUploadIdAndRenditionSuffixAndAnalysisVersion(uploadId, report.renditionSuffix(), ANALYSIS_VERSION)
                    .orElseGet(QualityReportEntity::new);

            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID().toString());
            }

            entity.setUploadId(uploadId);
            entity.setRenditionSuffix(report.renditionSuffix());
            entity.setAnalysisVersion(ANALYSIS_VERSION);
            entity.setMetric(report.metric());
            entity.setScore(report.score());
            entity.setPsnrDb(report.psnrDb());
            entity.setSsimScore(report.ssimScore());
            entity.setArtifactUri(report.artifactUri());
            entity.setCreatedAt(Instant.now());
            
            qualityReportRepository.save(entity);
        } catch (Exception e) {
            log.warn("Failed to persist quality report for uploadId={}, rendition={}", uploadId, report.renditionSuffix(), e);
        }
    }

    public static List<String> singleViolation(String message) {
        return List.of(message);
    }
}
