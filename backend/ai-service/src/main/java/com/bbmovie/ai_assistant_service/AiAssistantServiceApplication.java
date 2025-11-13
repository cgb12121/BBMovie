package com.bbmovie.ai_assistant_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        scanBasePackages = "com.bbmovie.ai_assistant_service.global_config",
        exclude = {
                org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration.class,
                org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class
        }
)
public class AiAssistantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiAssistantServiceApplication.class, args);
    }
}
