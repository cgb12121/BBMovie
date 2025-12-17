package com.bbmovie.mediauploadservice.service;

import com.bbmovie.mediauploadservice.dto.MediaFileFilterRequest;
import com.bbmovie.mediauploadservice.entity.MediaFile;
import com.bbmovie.mediauploadservice.enums.MediaStatus;
import com.bbmovie.mediauploadservice.repository.MediaFileRepository;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.bbmovie.common.entity.JoseConstraint.JosePayload.SUB;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaManagementService {

    private final MediaFileRepository mediaFileRepository;
    private final MinioClient minioClient;

    public Page<MediaFile> listMediaFiles(MediaFileFilterRequest filter, Pageable pageable) {
        Specification<MediaFile> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getPurpose() != null) {
                predicates.add(cb.equal(root.get("purpose"), filter.getPurpose()));
            }

            if (filter.getUserId() != null && !filter.getUserId().isBlank()) {
                predicates.add(cb.equal(root.get("userId"), filter.getUserId()));
            }

            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getFromDate().atStartOfDay()));
            }

            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getToDate().atTime(23, 59, 59)));
            }

            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                String searchLike = "%" + filter.getSearch().toLowerCase() + "%";
                Predicate filenameLike = cb.like(cb.lower(root.get("originalFilename")), searchLike);
                Predicate uploadIdLike = cb.like(cb.lower(root.get("uploadId")), searchLike);
                predicates.add(cb.or(filenameLike, uploadIdLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return mediaFileRepository.findAll(spec, pageable);
    }

    @Transactional
    public void deleteMediaFile(String uploadId) {
        MediaFile file = mediaFileRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new IllegalArgumentException("File not found with uploadId: " + uploadId));
        deleteFileInternal(file);
    }

    @Transactional
    public void deleteMediaFile(String uploadId, Jwt jwt) {
        String userId = jwt.getClaim(SUB);
        MediaFile file = mediaFileRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new IllegalArgumentException("File not found with uploadId: " + uploadId));

        if (!file.getUserId().equals(userId)) {
            throw new AccessDeniedException("You are not authorized to delete this file.");
        }

        deleteFileInternal(file);
    }

    private void deleteFileInternal(MediaFile file) {
        // Delete from MinIO
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(file.getBucket())
                            .object(file.getObjectKey())
                            .build()
            );
            log.info("Deleted file from storage: {}", file.getObjectKey());
        } catch (Exception e) {
            log.error("Failed to delete file from storage: {}", file.getObjectKey(), e);
        }

        file.setStatus(MediaStatus.DELETED);
        mediaFileRepository.save(file);
    }
}
