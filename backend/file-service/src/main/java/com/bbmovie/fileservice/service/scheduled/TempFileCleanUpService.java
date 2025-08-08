package com.bbmovie.fileservice.service.scheduled;

import com.bbmovie.fileservice.repository.TempFileRecordRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Log4j2
@Service
public class TempFileCleanUpService {

    private final TempFileRecordRepository tempFileRecordRepository;

    @Autowired
    public TempFileCleanUpService(TempFileRecordRepository tempFileRecordRepository) {
        this.tempFileRecordRepository = tempFileRecordRepository;
    }

    public Mono<Void> cleanupTempFile(Path tempPath) {
        Path path = tempPath.getFileName();
        if (path == null) {
            return Mono.empty();
        }
        String fileName = path.toString();
        return Mono.fromCallable(() -> {
            log.info("Attempt to deleting temp file: {}", fileName);
                    try {
                        Files.deleteIfExists(tempPath);
                        log.info("Temp file {} deleted.", tempPath);
                        return true;
                    } catch (IOException e) {
                        log.warn("Failed to delete temp file {}: {}", tempPath, e.getMessage());
                        return false;
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        return tempFileRecordRepository.findByFileName(fileName)
                                .flatMap(tempFileRecord -> {
                                    tempFileRecord.setRemoved(true);
                                    tempFileRecord.setRemovedAt(LocalDateTime.now());
                                    return tempFileRecordRepository.save(tempFileRecord).then();
                                })
                                .onErrorResume(err -> {
                                    log.warn("Failed to update TempFileRecord for {}: {}", fileName, err.getMessage());
                                    return Mono.empty();
                                });
                    }
                    return Mono.empty();
                }).then();
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldTempFiles() {
        tempFileRecordRepository.findAllByIsRemovedFalse()
                .flatMap(tempFileRecord -> cleanupTempFile(Paths.get(tempFileRecord.getTempDir())))
                .subscribe();
    }
}