package com.bbmovie.transcodeworker.persistence.repository;

import com.bbmovie.transcodeworker.persistence.entity.ComplexityProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComplexityProfileRepository extends JpaRepository<ComplexityProfileEntity, String> {

    Optional<ComplexityProfileEntity> findByUploadIdAndAnalysisVersion(String uploadId, int analysisVersion);
}
