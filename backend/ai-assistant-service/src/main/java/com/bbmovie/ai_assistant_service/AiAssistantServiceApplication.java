package com.bbmovie.ai_assistant_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        scanBasePackages = "com.bbmovie.ai_assistant_service.global_config"
)
public class AiAssistantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiAssistantServiceApplication.class, args);
    }
}
