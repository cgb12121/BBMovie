package com.bbmovie.auth.security.jose.dto;


import com.bbmovie.auth.security.jose.JoseProviderStrategy;
import com.bbmovie.auth.security.jose.filter.JoseAuthenticationFilter;

import java.util.List;


//TODO: this might need to be extended to handle new ABAC policies and JTI/SID
/**
 * Represents a validated security token created and managed by a {@link JoseProviderStrategy}.
 * This record encapsulates the necessary information about a token to facilitate
 * authentication and authorization processes within the system.
 * <p>
 * Attributes:
 * - provider: The strategy used to handle token operations, such as validation and claim extraction.
 * - sid: The unique session identifier associated with the token.
 * - username: The username associated with the authenticated entity.
 * - roles: A list of roles assigned to the authenticated entity, defining access control scopes.
 * - isEnabled: Indicates whether the token or the associated session is currently enabled or valid.
 * <p>
 * This record provides a structured way to handle post-validation token data, ensuring seamless
 * integration with security layers and business logic that rely on token-based access control.
 * <p>
 * This class is used by {@link JoseAuthenticationFilter} to validate old token after key rotation
 */
public record JoseValidatedToken(
        JoseProviderStrategy provider,
        String sid,
        String username,
        List<String> roles,
        boolean isEnabled
) {}