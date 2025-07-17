package com.bbmovie.fileservice.service.validation;

import com.bbmovie.fileservice.exception.MalwareFileException;
import com.bbmovie.fileservice.service.validation.clamav.ClamAVService;
import com.bbmovie.fileservice.service.validation.tika.TikaValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

@Service
public class FileValidationService {

    private final TikaValidationService tikaService;
    private final ClamAVService clamAVService;

    @Autowired
    public FileValidationService(TikaValidationService tikaService, ClamAVService clamAVService) {
        this.tikaService = tikaService;
        this.clamAVService = clamAVService;
    }

    public Mono<Boolean> validateFile(Path filePath) {
        return tikaService.isValidContentType(filePath)
                .flatMap(tikaResult ->
                        clamAVService.scanFile(filePath)
                                .handle((scanResult, sink) -> {
                                    if (Boolean.TRUE.equals(scanResult)) {
                                        sink.next(tikaResult);
                                    } else {
                                        sink.error(new MalwareFileException("Malware file(s) detected!"));
                                    }
                                })
                );
    }
}