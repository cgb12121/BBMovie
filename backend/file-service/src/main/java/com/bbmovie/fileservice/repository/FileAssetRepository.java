package com.bbmovie.fileservice.repository;

import com.bbmovie.fileservice.entity.FileAsset;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface FileAssetRepository extends R2dbcRepository<FileAsset, Long> {
    Flux<FileAsset> findByMovieId(String movieId);
    Flux<FileAsset> findByMovieIdAndQuality(String movieId, String quality);

    @Modifying
    @Query("INSERT INTO file_assets (movie_id, entity_type, storage_provider, path_or_public_id, quality, mime_type, file_size, created_at, updated_at) " +
           "VALUES (:#{#asset.movieId}, :#{#asset.entityType.name()}, :#{#asset.storageProvider.name()}, :#{#asset.pathOrPublicId}, :#{#asset.quality}, :#{#asset.mimeType}, :#{#asset.fileSize}, NOW(), NOW())")
    Mono<Void> insertFileAsset(@Param("asset") FileAsset asset);

    @Modifying
    @Query("DELETE FROM file_assets WHERE id = :id")
    Mono<Void> deleteById(@Param("id") Long id);

    Flux<FileAsset> findAllBy(Pageable pageable);
}
