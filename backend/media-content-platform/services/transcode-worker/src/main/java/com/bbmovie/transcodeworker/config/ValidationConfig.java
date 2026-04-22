package com.bbmovie.transcodeworker.config;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.capybara.clamav.ClamavClient;

/**
 * Configuration class for validation services.
 * Provides beans for file content detection (Apache Tika) and malware scanning (ClamAV).
 */
@Configuration
public class ValidationConfig {

    @Value("${clamav.host:localhost}")
    private String clamavHost;

    @Value("${clamav.port:3310}")
    private int clamavPort;

    /**
     * Creates and returns a Tika bean instance for content type detection.
     *
     * @return Tika instance for detecting file content types
     */
    @Bean
    public Tika tika() {
        return new Tika();
    }

    /**
     * Creates and returns a ClamAV client bean instance for malware scanning.
     *
     * @return ClamavClient instance configured with the specified host and port
     */
    @Bean
    public ClamavClient clamavClient() {
        return new ClamavClient(clamavHost, clamavPort);
    }
}
