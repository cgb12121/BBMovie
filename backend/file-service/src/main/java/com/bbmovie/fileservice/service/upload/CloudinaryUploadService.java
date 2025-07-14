package com.bbmovie.fileservice.service.upload;

import com.bbmovie.fileservice.entity.TempFileRecord;
import com.bbmovie.fileservice.repository.TempFileRecordRepository;
import com.bbmovie.fileservice.service.kafka.ReactiveFileUploadEventPublisher;
import com.bbmovie.fileservice.service.storage.FileStorageStrategyFactory;
import com.bbmovie.fileservice.service.storage.StorageStrategy;
import com.example.common.dtos.kafka.FileUploadEvent;
import com.example.common.dtos.kafka.UploadMetadata;
import com.example.common.enums.Storage;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.bbmovie.fileservice.utils.FileMetaDataUtils.sanitizeFilenameWithoutExtension;
import static com.bbmovie.fileservice.utils.FileUploadEventUtils.createEvent;
import static com.bbmovie.fileservice.utils.FileUploadEventUtils.createNewTempUploadEvent;

@Log4j2
@Service("cloudinaryUploadService")
public class CloudinaryUploadService implements FileUploadStrategyService {

    private final FileStorageStrategyFactory strategyFactory;
    private final TempFileRecordRepository tempFileRecordRepository;
    private final ReactiveFileUploadEventPublisher publisher;

    @Value("${app.upload.temp-dir}")
    private String tempDir;

    public CloudinaryUploadService(
            FileStorageStrategyFactory strategyFactory,
            TempFileRecordRepository tempFileRecordRepository,
            ReactiveFileUploadEventPublisher publisher
    ) {
        this.strategyFactory = strategyFactory;
        this.tempFileRecordRepository = tempFileRecordRepository;
        this.publisher = publisher;
    }

    @Override
    public Mono<ResponseEntity<String>> uploadFile(FilePart filePart, UploadMetadata metadata, Authentication auth) {
        String username = auth.getName();
        String originalFilename = filePart.filename();
        String extension = FilenameUtils.getExtension(originalFilename);
        String nameWithoutExt = FilenameUtils.removeExtension(originalFilename);
        String safeName = sanitizeFilenameWithoutExtension(nameWithoutExt) + extension;

        Path tempPath = Paths.get(tempDir, safeName);
        StorageStrategy strategy = strategyFactory.getStrategy(Storage.CLOUDINARY.name());

        return filePart.transferTo(tempPath)//Save temp file 1st time?
            .then(Mono.defer(() -> {
                TempFileRecord tempFileRecord = createNewTempUploadEvent(metadata, nameWithoutExt, extension, tempPath, username);
                return tempFileRecordRepository.saveTempFile(tempFileRecord);
            }))
            .then(strategy.store(tempPath.toFile(), safeName))// Save file to temp 2nd time?
            .flatMap(result -> {
                FileUploadEvent event = createEvent(safeName, username, metadata, result);
                return publisher.publish(event)
                        .thenReturn(ResponseEntity.ok("✅ Uploaded to cloud and published event."));
            });
    }

    @Override
    public Flux<String> uploadWithProgress(FilePart filePart, UploadMetadata metadata, Authentication auth) {
        return Flux.error(new UnsupportedOperationException("Cloud upload does not support progress tracking yet."));
    }
}