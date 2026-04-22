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

/**
 * Service class for malware scanning using ClamAV.
 * Provides file scanning capabilities to detect malware in uploaded files before processing.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class ClamAVService {

    /** ClamAV client instance for performing malware scans */
    private final ClamavClient clamAVClient;

    /** Flag to enable or disable ClamAV scanning */
    @Value("${app.clamav.enabled:true}")
    private boolean enabled;

    /** Current active Spring profile */
    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    /**
     * Scans a file for malware using ClamAV.
     * This method performs a malware scan on the specified file and returns whether it's clean.
     * In development profiles, scanning can be bypassed if disabled for testing purposes.
     *
     * @param filePath the path to the file to scan
     * @return true if the file is clean (no malware detected), false otherwise
     */
    public boolean scanFile(Path filePath) {
        if (!enabled && isDevProfile()) {
            log.warn("ClamAV is disabled. Skipping scan for: {}", filePath);
            return true;
        }

        log.info("Scan file: {}", filePath);
        try (InputStream is = Files.newInputStream(filePath)) {
            ScanResult scanResult = clamAVClient.scan(is);
            log.trace("[File {}]Scan result: {}", filePath, scanResult);
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

    /**
     * Checks if the current active profile is a development profile.
     * This method determines if the application is running in a development environment
     * where certain security checks might be relaxed for testing purposes.
     *
     * @return true if the active profile is considered a development profile, false otherwise
     */
    private boolean isDevProfile() {
        return Set.of("dev", "default", "local", "docker").contains(activeProfile.toLowerCase());
    }
}
