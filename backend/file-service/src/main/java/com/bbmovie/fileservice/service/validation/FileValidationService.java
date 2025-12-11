package com.bbmovie.fileservice.service.validation;

import com.bbmovie.fileservice.exception.MalwareFileException;
import com.bbmovie.fileservice.service.validation.clamav.ClamAVService;
import com.bbmovie.fileservice.service.validation.tika.TikaValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class FileValidationService {

    private final TikaValidationService tikaService;
    private final ClamAVService clamAVService;

    public Mono<Void> validateFile(Path filePath) {
        return tikaService.validateContentType(filePath) // This will now raise an error on unsupported types
                .then(clamAVService.scanFile(filePath))
                .flatMap(isClean -> {
                    if (Boolean.TRUE.equals(isClean)) {
                        return Mono.empty(); // Success
                    } else {
                        return Mono.error(new MalwareFileException("Malware file(s) detected!"));
                    }
                });
    }
}