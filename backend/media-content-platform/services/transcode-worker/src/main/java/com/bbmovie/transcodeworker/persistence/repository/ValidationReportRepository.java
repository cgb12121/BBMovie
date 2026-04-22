package com.bbmovie.transcodeworker.persistence.repository;

import com.bbmovie.transcodeworker.persistence.entity.ValidationReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ValidationReportRepository extends JpaRepository<ValidationReportEntity, String> {

    Optional<ValidationReportEntity> findByUploadIdAndRenditionSuffixAndAnalysisVersion(
            String uploadId, String renditionSuffix, int analysisVersion);
}
