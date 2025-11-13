package com.bbmovie.auth.security.oauth2;

import com.bbmovie.auth.security.oauth2.strategy.request.customizer.OAuth2RequestCustomizer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CustomAuthorizationRequestResolver is a custom implementation of
 * {@link OAuth2AuthorizationRequestResolver} for resolving OAuth2
 * authorization requests. It provides PKCE-based handling for specific
 * client registrations and applies customizations to authorization requests
 * based on the registration ID.
 * <p>
 * This class uses a default OAuth2AuthorizationRequestResolver for
 * general handling and supports customization through the
 * {@link OAuth2RequestCustomizer} interface, allowing tailored behavior
 * for authorization requests across different client registrations.
 * <p>
 * Features:
 * <p>- Supports PKCE-based resolution for specific client registrations (e.g., "x (formal Twitter)").
 * <p>- Extensible with custom logic for additional authorization request parameters.
 * <p>- Maps client-specific customization logic through a dynamic registry.
 */
@Component
public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultResolver;
    private final OAuth2AuthorizationRequestResolver pkceResolver;
    private final Map<String, OAuth2RequestCustomizer> customizerMap;

    /**
     * Constructs a {@code CustomAuthorizationRequestResolver} that processes
     * OAuth2 authorization requests and applies custom logic for PKCE-based
     * flows or other customizations per client registration.
     *
     * @param repo The repository of client registrations, providing metadata
     *             about each OAuth2 client participating in the authorization process.
     * @param customizers A list of {@link OAuth2RequestCustomizer} objects that
     *                    define additional customization logic for authorization
     *                    requests based on the client registration ID.
     */
    public CustomAuthorizationRequestResolver(
            ClientRegistrationRepository repo,
            List<OAuth2RequestCustomizer> customizers
    ) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");

        DefaultOAuth2AuthorizationRequestResolver defaultPkceResolver = new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
        defaultPkceResolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        this.pkceResolver = defaultPkceResolver;

        this.customizerMap = new HashMap<>();
        for (OAuth2RequestCustomizer customizer : customizers) {
            this.customizerMap.put(customizer.getRegistrationId(), customizer);
        }
    }

    /**
     * Resolves an {@link OAuth2AuthorizationRequest} based on the provided HTTP request.
     * If the request URI indicates a specific client registration ID ('x'), this method
     * uses a PKCE-based resolver. Otherwise, a default resolver is used, and the
     * authorization request is potentially customized.
     *
     * @param request the {@link HttpServletRequest} containing the authorization request details
     * @return the resolved {@link OAuth2AuthorizationRequest}, customized if applicable,
     *         or null if unable to resolve
     */
    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String registrationId = uri.substring(uri.lastIndexOf("/") + 1);
        // Use PKCE resolver for X OAuth2
        if ("x".equalsIgnoreCase(registrationId)) {
            return pkceResolver.resolve(request);
        } else {
            OAuth2AuthorizationRequest req = defaultResolver.resolve(request);
            return customizeRequest(request, req);
        }
    }

    /**
     * Resolves an {@link OAuth2AuthorizationRequest} based on the provided HTTP request
     * and client registration ID. If the client registration ID matches a specific value ('x'),
     * the PKCE resolver is used; otherwise, a default resolver is used with potential
     * customization applied.
     *
     * @param request the {@link HttpServletRequest} containing the authorization request details
     * @param clientRegistrationId the identifier for the client registration used to resolve
     *        the authorization request
     * @return the resolved {@link OAuth2AuthorizationRequest}, potentially customized, or null if unable
     *         to resolve
     */
    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        // Use PKCE resolver for X OAuth2
        if ("x".equalsIgnoreCase(clientRegistrationId)) {
            return pkceResolver.resolve(request, clientRegistrationId);
        } else {
            OAuth2AuthorizationRequest req = defaultResolver.resolve(request, clientRegistrationId);
            return customizeRequest(request, req);
        }
    }

    /**
     * Customizes an {@link OAuth2AuthorizationRequest} by adding additional parameters based on
     * specific customizations for the provided registration ID derived from the request URI.
     *
     * @param request the HTTP request which contains the URI used to extract the registration ID
     * @param req the original {@link OAuth2AuthorizationRequest} to be customized;
     *            may be null, in which case null is returned
     * @return a new customized {@link OAuth2AuthorizationRequest} instance with additional parameters,
     *         or null if the input request is null
     */
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
