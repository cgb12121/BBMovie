package com.bbmovie.auth.security.oauth2.strategy.request.customizer;

import java.util.Map;

public interface OAuth2RequestCustomizer {
    String getRegistrationId();
    void customize(Map<String, Object> parameters);
}
