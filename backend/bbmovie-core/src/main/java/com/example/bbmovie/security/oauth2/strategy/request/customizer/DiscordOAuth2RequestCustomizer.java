package com.example.bbmovie.security.oauth2.strategy.request.customizer;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component("discord")
public class DiscordOAuth2RequestCustomizer implements OAuth2RequestCustomizer {
    @Override
    public String getRegistrationId() {
        return "discord";
    }

    @Override
    public void customize(Map<String, Object> parameters) {
        // Discord does not support customizing parameters to control login behavior
    }
}