package com.bbmovie.fileservice.service.internal;

import com.bbmovie.common.enums.Storage;
import com.bbmovie.common.dtos.nats.FileUploadResult;
import com.bbmovie.fileservice.entity.FileAsset;
import com.bbmovie.fileservice.entity.FileStatus;
import com.bbmovie.fileservice.repository.FileAssetRepository;
import com.bbmovie.fileservice.service.storage.FileStorageStrategyFactory;
import com.bbmovie.fileservice.service.validation.FileValidationService;
import com.bbmovie.fileservice.utils.FileTypeUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InternalFileService {

    @Value("${app.upload.temp-dir}")
    private String tempDir;

    @Value("${spring.upload-dir}")
    private String appUploadDir;

    private final FileAssetRepository fileAssetRepository;
    private final FileStorageStrategyFactory storageFactory;
    private final FileValidationService fileValidationService;

    /**
     * Uploads general files and stores them permanently in the file asset system.
     * @param fileParts Flux of FilePart objects to upload
     * @return Flux of Maps containing file information
     */
    public Flux<Map<String, Object>> uploadGeneralFiles(Flux<FilePart> fileParts) {
        return fileParts.flatMap(this::uploadSingleFile);
    }

    private Mono<Map<String, Object>> uploadSingleFile(FilePart filePart) {
        return Mono.fromCallable(() -> {
            File dir = new File(tempDir);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new RuntimeException("Failed to create temp directory: " + tempDir);
            }
            return dir;
        }).flatMap(dir -> {
            String originalFilename = filePart.filename();
            String extension = FilenameUtils.getExtension(originalFilename);
            String uniqueName = UUID.randomUUID() + "." + extension;
            Path tempPath = Paths.get(tempDir).resolve(uniqueName);

            return filePart.transferTo(tempPath)
                    .then(fileValidationService.validateFile(tempPath))
                    .then(Mono.defer(() -> {
                        String storageName = Storage.LOCAL.name();
                        return storageFactory.getStrategy(storageName)
                                .store(tempPath.toFile(), uniqueName)
                                .flatMap(uploadResult -> createFileAsset(uploadResult, originalFilename, extension, storageName));
                    }))
                    .doFinally(signal -> {
                        File f = tempPath.toFile();
                        if (f.exists()) {
                            if (!f.delete()) {
                                log.warn("Failed to delete temp file: {}", f.getAbsolutePath());
                            }
                        }
                    });
        });
    }

    private Mono<Map<String, Object>> createFileAsset(FileUploadResult uploadResult, String originalFilename, String extension, String storageName) {
        FileAsset asset = FileAsset.builder()
                .movieId("N/A")
                .entityType(FileTypeUtils.determineEntityType(extension))
                .storageProvider(Storage.valueOf(storageName))
                .pathOrPublicId(uploadResult.getPublicId())
                .quality("ORIGINAL")
                .mimeType(uploadResult.getContentType())
                .fileSize(uploadResult.getFileSize())
                .status(FileStatus.PENDING)
                .build();

        return fileAssetRepository.save(asset)
                .map(saved -> Map.of(
                        "id", saved.getId(),
                        "url", saved.getPathOrPublicId(),
                        "filename", originalFilename,
                        "status", saved.getStatus(),
                        "storage", saved.getStorageProvider().name()
                ));
    }

    public Mono<Resource> loadFileAssetAsResource(Long fileId) {
        return fileAssetRepository.findById(fileId)
                .flatMap(asset -> {
                    if (asset.getStorageProvider() == Storage.LOCAL) {
                        try {
                            // For LOCAL storage, pathOrPublicId contains the filename that was stored
                            // We need to resolve it against the upload directory
                            // The upload directory path needs to be injected
                            Path filePath = Paths.get(appUploadDir, asset.getPathOrPublicId()).normalize();
                            Resource resource = new UrlResource(filePath.toUri());
                            if (resource.exists() && resource.isReadable()) {
                                return Mono.just(resource);
                            } else {
                                return Mono.error(new RuntimeException("File not found or not readable: " + asset.getPathOrPublicId()));
                            }
                        } catch (MalformedURLException e) {
                            return Mono.error(e);
                        }
                    } else if (asset.getStorageProvider() == Storage.CLOUDINARY) {
                        // For cloud storage, pathOrPublicId contains the public ID
                        return Mono.error(new RuntimeException("Cloudinary storage not implemented for direct file serving"));
                    } else if (asset.getStorageProvider() == Storage.S3) {
                        return Mono.error(new RuntimeException("S3 storage provider not supported"));
                    }
                    return Mono.error(new RuntimeException("File not found or unsupported storage provider"));
                });
    }
}
