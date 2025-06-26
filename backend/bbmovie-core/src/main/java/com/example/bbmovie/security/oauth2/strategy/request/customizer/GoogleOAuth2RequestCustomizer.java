package com.example.bbmovie.security.oauth2.strategy.request.customizer;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component("google")
public class GoogleOAuth2RequestCustomizer implements OAuth2RequestCustomizer {
    @Override
    public String getRegistrationId() {
        return "google";
    }

    @Override
    public void customize(Map<String, Object> parameters) {
        parameters.put("prompt", "consent");
    }
}