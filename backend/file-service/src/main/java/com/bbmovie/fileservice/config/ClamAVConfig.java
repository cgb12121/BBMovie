package com.bbmovie.fileservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.capybara.clamav.ClamavClient;

@Configuration
public class ClamAVConfig {
    @Value("${clamav.host:localhost}")
    private String host;
    
    @Value("${clamav.port:3310}")
    private int port;
    
    @Bean
    public ClamavClient clamAVClient() {
        return new ClamavClient(host, port);
    }
}