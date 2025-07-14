package com.bbmovie.fileservice.service.validation.clamav;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xyz.capybara.clamav.ClamavClient;
import xyz.capybara.clamav.commands.scan.result.ScanResult;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

@Log4j2
@Service
public class ClamAVService {

    private final ClamavClient clamAVClient;

    @Autowired
    public ClamAVService(ClamavClient clamAVClient) {
        this.clamAVClient = clamAVClient;
    }

    public Mono<Boolean> scanFile(Path filePath) {
        return Mono.fromCallable(() -> {
                try (InputStream is = Files.newInputStream(filePath)) {
                    ScanResult scanResult = clamAVClient.scan(is);
                    if (scanResult instanceof ScanResult.OK) {
                        return true;
                    }
                    if (scanResult instanceof ScanResult.VirusFound virusFound) {
                        Map<String, Collection<String>> viruses = virusFound.getFoundViruses();
                        log.error("Virus found in file: {}", viruses);
                        return false;
                    }

                    log.error("Unknown scan result: {}. Treat the result as malicious file.", scanResult);
                    return false;
                }
            })
            .subscribeOn(Schedulers.boundedElastic());
    }
}