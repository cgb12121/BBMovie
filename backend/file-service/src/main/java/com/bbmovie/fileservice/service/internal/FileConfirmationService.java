package com.bbmovie.fileservice.service.internal;

import com.bbmovie.fileservice.entity.FileAsset;
import com.bbmovie.fileservice.entity.FileStatus;
import com.bbmovie.fileservice.repository.FileAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileConfirmationService {

    private final FileAssetRepository fileAssetRepository;

    public Mono<Void> confirmFile(Long fileId) {
        return fileAssetRepository.findById(fileId)
                .flatMap(file -> {
                    if (file.getStatus() == FileStatus.CONFIRMED) {
                        return Mono.empty();
                    }
                    file.setStatus(FileStatus.CONFIRMED);
                    return fileAssetRepository.save(file).then();
                })
                .doOnSuccess(v -> log.info("Confirmed file usage: {}", fileId))
                .doOnError(e -> log.error("Failed to confirm file {}: {}", fileId, e.getMessage()));
    }
}
