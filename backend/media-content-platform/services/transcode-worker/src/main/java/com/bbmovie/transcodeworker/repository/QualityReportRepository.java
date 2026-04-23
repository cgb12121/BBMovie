package com.bbmovie.transcodeworker.repository;

import com.bbmovie.transcodeworker.entity.QualityReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QualityReportRepository extends JpaRepository<QualityReportEntity, String> {
    Optional<QualityReportEntity> findByUploadIdAndRenditionSuffixAndAnalysisVersion(
            String uploadId, String renditionSuffix, int analysisVersion);
}
