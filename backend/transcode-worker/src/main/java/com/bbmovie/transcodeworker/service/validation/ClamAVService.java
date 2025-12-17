package com.bbmovie.transcodeworker.service.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import xyz.capybara.clamav.ClamavClient;
import xyz.capybara.clamav.commands.scan.result.ScanResult;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Log4j2
@Service
@RequiredArgsConstructor
public class ClamAVService {

    private final ClamavClient clamAVClient;

    @Value("${app.clamav.enabled:true}")
    private boolean enabled;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    public boolean scanFile(Path filePath) {
        if (!enabled) {
            log.warn("ClamAV is disabled. Skipping scan for: {}", filePath);
            return true;
        }

        log.info("Scan file: {}", filePath);
        try (InputStream is = Files.newInputStream(filePath)) {
            ScanResult scanResult = clamAVClient.scan(is);
            log.info("Scan result: {}", scanResult);
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
        } catch (Exception e) {
            log.error("ClamAV scan failed", e);
            if (isDevProfile()) {
                log.warn("ClamAV failed but we are in DEV profile. Allowing file to proceed (BYPASS).");
                return true;
            }
            throw new RuntimeException("ClamAV scan failed", e);
        }
    }

    private boolean isDevProfile() {
        return Set.of("dev", "default", "local", "docker").contains(activeProfile.toLowerCase());
    }
}
