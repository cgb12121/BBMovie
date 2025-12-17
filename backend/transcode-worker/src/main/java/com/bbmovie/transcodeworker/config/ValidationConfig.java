package com.bbmovie.transcodeworker.config;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.capybara.clamav.ClamavClient;

@Configuration
public class ValidationConfig {

    @Value("${clamav.host:localhost}")
    private String clamavHost;

    @Value("${clamav.port:3310}")
    private int clamavPort;

    @Bean
    public Tika tika() {
        return new Tika();
    }

    @Bean
    public ClamavClient clamavClient() {
        return new ClamavClient(clamavHost, clamavPort);
    }
}
