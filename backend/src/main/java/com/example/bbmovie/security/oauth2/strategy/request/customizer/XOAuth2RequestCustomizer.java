package com.example.bbmovie.security.oauth2.strategy.request.customizer;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

@Component("x")
public class XOAuth2RequestCustomizer implements OAuth2RequestCustomizer {

    @Override
    public String getRegistrationId() {
        return "x";
    }

    @Override
    public void customize(Map<String, Object> parameters) {

    }
}
