package com.bbmovie.transcodeworker.repository;

import com.bbmovie.transcodeworker.entity.ValidationReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ValidationReportRepository extends JpaRepository<ValidationReportEntity, String> {
    Optional<ValidationReportEntity> findByUploadIdAndRenditionSuffixAndAnalysisVersion(
            String uploadId, String renditionSuffix, int analysisVersion);
}
