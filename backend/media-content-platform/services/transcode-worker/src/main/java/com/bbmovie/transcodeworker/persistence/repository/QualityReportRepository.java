package com.bbmovie.transcodeworker.persistence.repository;

import com.bbmovie.transcodeworker.persistence.entity.QualityReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QualityReportRepository extends JpaRepository<QualityReportEntity, String> {

    Optional<QualityReportEntity> findByUploadIdAndRenditionSuffixAndAnalysisVersion(
            String uploadId, String renditionSuffix, int analysisVersion);
}
