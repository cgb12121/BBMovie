package bbmovie.ai_platform.agentic_ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.ObjectMapper;

@Configuration
public class JacksonConfiguration {
     @Bean
     public ObjectMapper objectMapper() {
          return new ObjectMapper();
     }
}
