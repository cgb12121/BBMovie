package com.bbmovie.transcodeworker.repository;

import com.bbmovie.transcodeworker.entity.ComplexityProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComplexityProfileRepository extends JpaRepository<ComplexityProfileEntity, String> {
    Optional<ComplexityProfileEntity> findByUploadIdAndAnalysisVersion(String uploadId, int analysisVersion);
}
