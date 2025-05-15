package com.example.bbmovie.security.oauth2;

import com.example.bbmovie.security.oauth2.strategy.request.customizer.OAuth2RequestCustomizer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultResolver;
    private final Map<String, OAuth2RequestCustomizer> customizerMap;

    public CustomAuthorizationRequestResolver(
            ClientRegistrationRepository repo,
            List<OAuth2RequestCustomizer> customizers
    ) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
        this.customizerMap = new HashMap<>();
        for (OAuth2RequestCustomizer customizer : customizers) {
            this.customizerMap.put(customizer.getRegistrationId(), customizer);
        }
    }

    @Bean(value = "CustomAuthorizationRequestResolver")
    public OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver(
            ClientRegistrationRepository repo,
            List<OAuth2RequestCustomizer> customizers
    ) {
        return new CustomAuthorizationRequestResolver(repo, customizers);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest req = defaultResolver.resolve(request);
        return customizeRequest(request, req);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest req = defaultResolver.resolve(request, clientRegistrationId);
        return customizeRequest(request, req);
    }

    private OAuth2AuthorizationRequest customizeRequest(HttpServletRequest request, OAuth2AuthorizationRequest req) {
        if (req == null) return null;

        Map<String, Object> extraParams = new HashMap<>(req.getAdditionalParameters());
        String uri = request.getRequestURI();
        String registrationId = uri.substring(uri.lastIndexOf("/") + 1);

        OAuth2RequestCustomizer customizer = customizerMap.get(registrationId);
        if (customizer != null) {
            customizer.customize(extraParams);
        }

        return OAuth2AuthorizationRequest.from(req)
                .additionalParameters(extraParams)
                .build();
    }
}
