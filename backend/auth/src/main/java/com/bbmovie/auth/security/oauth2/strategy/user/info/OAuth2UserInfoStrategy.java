package com.bbmovie.auth.security.oauth2.strategy.user.info;

import com.bbmovie.auth.entity.enumerate.AuthProvider;

import java.util.Map;

/**
 * This interface defines a strategy for retrieving information about an OAuth2 user
 * based on the attributes provided by an OAuth2 provider. Different implementations
 * of this interface may provide specific logic for handling attributes from various providers.
 * <p>
 * The attribute map typically contains data obtained during the OAuth2 authentication process,
 * where each key-value pair represents a user attribute from the provider.
 */
@SuppressWarnings("unused")
public interface OAuth2UserInfoStrategy {
    /**
     * Retrieves the key used to identify the email attribute within the provided attributes map.
     *
     * @param attributes a map containing user attributes, typically derived from an OAuth2 response
     * @return the key used to locate the email attribute in the attribute map
     */
    String getEmailAttributeKey(Map<String, Object> attributes);
    /**
     * Retrieves the email address of a user based on the given attributes.
     * The attribute map may vary depending on the specific OAuth2 provider.
     *
     * @param attributes a map containing user attributes retrieved from the OAuth2 provider
     * @return the email address of the user, or null if the email attribute is not present
     */
    String getEmail(Map<String, Object> attributes);

    /**
     * Retrieves the key used to identify the name attribute within the provided attributes map.
     *
     * @param attributes a map containing user attributes, typically derived from an OAuth2 response
     * @return the key used to locate the name attribute in the attribute map
     */
    String getNameAttributeKey(Map<String, Object> attributes);
    /**
     * Retrieves the name information from the provided attributes map.
     *
     * @param attributes a map containing user attributes, where keys are attribute names and values are their corresponding data
     * @return the name extracted from the attributes map, or null if the name is not present
     */
    String getName(Map<String, Object> attributes);

    /**
     * Retrieves the key used to identify the username attribute within the provided attributes map.
     *
     * @param attributes a map containing user attributes, typically derived from an OAuth2 response
     * @return the key used to locate the username attribute in the attribute map
     */
    String getUsernameAttributeKey(Map<String, Object> attributes);
    /**
     * Retrieves the username from the given attributes.
     *
     * @param attributes a map containing attributes, generally extracted from
     *                   an OAuth2 authentication response.
     * @return the username as a string, or null if the username does not exist
     *         in the provided attributes.
     */
    String getUsername(Map<String, Object> attributes);

    /**
     * Retrieves the key used to identify the avatar URL attribute within the provided attributes map.
     *
     * @param attributes a map containing user attributes, typically derived from an OAuth2 response
     * @return the key used to locate the avatar URL attribute in the attribute map
     */
    String getAvatarUrlAttributeKey(Map<String, Object> attributes);
    /**
     * Retrieves the avatar URL of a user from the provided attributes map.
     * The specific key for the avatar URL may vary depending on the OAuth2 provider.
     *
     * @param attributes a map containing user attributes, which are typically provided
     *                   as part of the OAuth2 authentication response
     * @return the avatar URL as a string, or null if the avatar URL is not present
     *         in the provided attributes
     */
    String getAvatarUrl(Map<String, Object> attributes);

    /**
     * Retrieves the AuthProvider associated with the OAuth2 strategy.
     *
     * @return the AuthProvider enum value representing the provider this strategy corresponds to
     */
    AuthProvider getAuthProvider();
}
