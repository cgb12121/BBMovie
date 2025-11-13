package com.bbmovie.auth.security.oauth2.strategy.request.customizer;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component("facebook")
public class FacebookOAuth2RequestCustomizer implements OAuth2RequestCustomizer {
    @Override
    public String getRegistrationId() {
        return "facebook";
    }

    @Override
    public void customize(Map<String, Object> parameters) {
        parameters.put("auth_type", "rerequest");
    }
}