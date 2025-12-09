package com.bbmovie.fileservice.repository;

import com.bbmovie.fileservice.entity.FileAsset;
import com.bbmovie.fileservice.entity.FileStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Repository
public interface FileAssetRepository extends R2dbcRepository<FileAsset, Long> {
    Flux<FileAsset> findByMovieId(String movieId);
    Flux<FileAsset> findByMovieIdAndQuality(String movieId, String quality);
    Flux<FileAsset> findAllBy(Pageable pageable);
    Flux<FileAsset> findByStatusAndCreatedAtBefore(FileStatus status, LocalDateTime cutoffTime);
}