package com.example.bbmovie.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultResolver;

    public CustomAuthorizationRequestResolver(ClientRegistrationRepository repo) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
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

        switch (registrationId) {
            case "google" -> extraParams.put("prompt", GooglePrompt.CONSENT.getOption());
            case "facebook" -> extraParams.put("auth_type", "rerequest");
            case "github" -> {}
        }

        String option = GooglePrompt.CONSENT.getOption();
        extraParams.put("prompt", option);

        return OAuth2AuthorizationRequest.from(req)
                .additionalParameters(extraParams)
                .build();
    }

    @Getter
    enum GooglePrompt {

        /**
         * 🔑 Prompt=consent
         * <p>
         * - What it does: Forces the provider to show the consent screen again.
         * <p>
         * - When to use it: When you want the user to re-authorize your app and re-grant permissions.
         * <p>
         * - Example use case: Your app requests new scopes (permissions), or you're testing user approval flow.
         * <p>
         * ✅ Shows: "This app wants access to your email/profile/etc..."
         */
        CONSENT("consent"),
        /**
         * 🔑 Prompt=login
         * <p>
         * - What it does: Forces the user to re-authenticate (log in again), even if they're already logged into the provider.
         * <p>
         * - When to use it: For high-security flows (e.g., banking), or when you don't want auto-login.
         * <p>
         * - Example use case: Logging in again to confirm identity for sensitive actions.
         * <p>
         * ✅ Shows: The provider's login form, even if the user is already signed in.
         */
        LOGIN("login"),

        /**
         * 🔑 Prompt=select_account
         * <p>
         * - What it does: Forces the provider to ask the user to choose an account.
         * <p>
         * - When to use it: When users have multiple Google or GitHub accounts, and you want to let them pick explicitly.
         * <p>
         * - Example use case: Apps where users might have multiple identities.
         * <p>
         * ✅ Shows: Account selection screen, even if only one account is signed in.
         */
        SELECT_ACCOUNT("select_account"),;


        private final String option;

        GooglePrompt(String option) {
            this.option = option;
        }
    }
}
