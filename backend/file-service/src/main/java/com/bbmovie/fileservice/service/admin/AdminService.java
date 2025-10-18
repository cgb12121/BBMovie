package com.bbmovie.fileservice.service.admin;

import com.bbmovie.fileservice.dto.CloudinaryUsageRequest;
import com.bbmovie.fileservice.dto.DiskUsageResponse;
import com.bbmovie.fileservice.entity.FileAsset;
import com.bbmovie.fileservice.repository.FileAssetRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.api.ApiResponse;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
public class AdminService {

    private final FileAssetRepository fileAssetRepository;
    private final Cloudinary cloudinary;

    @Value("${spring.upload-dir}")
    private String uploadDir;

    @Autowired
    public AdminService(FileAssetRepository fileAssetRepository, Cloudinary cloudinary) {
        this.fileAssetRepository = fileAssetRepository;
        this.cloudinary = cloudinary;
    }

    public Flux<FileAsset> listAllFiles(Pageable pageable) {
        return fileAssetRepository.findAllBy(pageable);
    }

    public Mono<ApiResponse> cloudinaryUsage(CloudinaryUsageRequest request) {
        return Mono.fromCallable(() -> {
            try {
                return cloudinary.api().usage(ObjectUtils.asMap("date", request.getDateRequested()));
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch usage from Cloudinary", e);
            }
        });
    }

    public Mono<DiskUsageResponse> getDiskUsage() {
        return Mono.fromCallable(() -> {
            try {
                FileStore store = Files.getFileStore(Paths.get(uploadDir));
                return DiskUsageResponse.builder()
                        .path(uploadDir)
                        .totalSpaceBytes(store.getTotalSpace())
                        .usableSpaceBytes(store.getUsableSpace())
                        .freeSpaceBytes(store.getUnallocatedSpace())
                        .totalSpace(String.format("%.2f GB", store.getTotalSpace() / 1073741824.0))
                        .usableSpace(String.format("%.2f GB", store.getUsableSpace() / 1073741824.0))
                        .freeSpace(String.format("%.2f GB", store.getUnallocatedSpace() / 1073741824.0))
                        .build();
            } catch (IOException e) {
                throw new RuntimeException("Could not read disk space information", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<ApiResponse> listCloudinaryResources() {
        return Mono.fromCallable(() -> {
            try {
                return cloudinary.api().resources(ObjectUtils.emptyMap());
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch resources from Cloudinary", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteFileAsset(Long id) {
        return fileAssetRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("FileAsset not found with id: " + id)))
                .flatMap(asset -> {
                    Mono<Void> deletePhysicalFile = switch (asset.getStorageProvider()) {
                        case LOCAL -> Mono.fromRunnable(() -> {
                            try {
                                Files.deleteIfExists(Paths.get(uploadDir, asset.getPathOrPublicId()));
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to delete local file", e);
                            }
                        }).subscribeOn(Schedulers.boundedElastic()).then();
                        case CLOUDINARY -> Mono.fromRunnable(() -> {
                            try {
                                cloudinary.api().deleteResources(List.of(asset.getPathOrPublicId()), ObjectUtils.emptyMap());
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to delete Cloudinary resource", e);
                            }
                        }).subscribeOn(Schedulers.boundedElastic()).then();
                        default -> Mono.error(new RuntimeException("Unsupported storage provider"));
                    };
                    return deletePhysicalFile.then(fileAssetRepository.deleteById(asset.getId()));
                });
    }

    public Mono<ApiResponse> deleteCloudinaryResource(String publicId) {
        return Mono.fromCallable(() -> {
            try {
                return cloudinary.api().deleteResources(List.of(publicId), ObjectUtils.emptyMap());
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete Cloudinary resource", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
