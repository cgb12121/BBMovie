package com.bbmovie.auth.security.oauth2.strategy.request.customizer;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component("github")
public class GitHubOAuth2RequestCustomizer implements OAuth2RequestCustomizer {
    @Override
    public String getRegistrationId() {
        return "github";
    }

    @Override
    public void customize(Map<String, Object> parameters) {
        // GitHub does not support customizing parameters to control login behavior
    }
}
