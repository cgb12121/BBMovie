package com.bbmovie.mediauploadservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "BBMovie Media Upload API",
                description = "Upload, chunk management and media file management APIs",
                version = "1.0.0",
                contact = @Contact(name = "BBMovie Team", email = "support@bbmovie.com")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Development server"),
                @Server(url = "https://api.bbmovie.com", description = "Production server")
        }
)
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@Configuration
public class OpenApiConfig {
}

