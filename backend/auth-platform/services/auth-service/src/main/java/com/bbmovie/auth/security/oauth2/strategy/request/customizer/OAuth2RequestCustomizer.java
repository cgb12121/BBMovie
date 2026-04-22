package com.bbmovie.auth.security.oauth2.strategy.request.customizer;

import java.util.Map;

/**
 * This interface provides a mechanism for customizing OAuth2 request parameters
 * for specific OAuth2 providers during the authorization process.
 * Implementations define provider-specific request customization logic.
 */
public interface OAuth2RequestCustomizer {
    /**
     * Retrieves the registration identifier associated with a specific OAuth2 client.
     * This identifier is typically used to categorize or select the customizations
     * to be applied during the authorization request process for the client.
     *
     * @return the registration ID as a String, which uniquely identifies the OAuth2 client
     */
    String getRegistrationId();
    /**
     * Customizes the provided request parameters for an OAuth2 authorization request.
     * This method allows the addition or modification of parameters to accommodate
     * provider-specific requirements or behaviors during the authorization process.
     *
     * @param parameters the map of parameters to be customized; keys represent
     *                   parameter names, and values represent their corresponding
     *                   values to be sent in the authorization request
     */
    void customize(Map<String, Object> parameters);
}
