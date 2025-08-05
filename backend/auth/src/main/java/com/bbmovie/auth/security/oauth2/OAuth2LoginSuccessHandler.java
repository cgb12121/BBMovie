package com.bbmovie.auth.security.oauth2;

import com.bbmovie.auth.dto.response.UserAgentResponse;
import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.entity.enumerate.Role;
import com.bbmovie.auth.entity.jose.RefreshToken;
import com.bbmovie.auth.security.jose.JoseProviderStrategyContext;
import com.bbmovie.auth.security.oauth2.strategy.user.info.OAuth2UserInfoStrategy;
import com.bbmovie.auth.security.oauth2.strategy.user.info.OAuth2UserInfoStrategyFactory;
import com.bbmovie.auth.service.UserService;
import com.bbmovie.auth.service.auth.RefreshTokenService;
import com.bbmovie.auth.utils.DeviceInfoUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * This handler manages the success flow of an OAuth2 login process. It extends
 * {@link SavedRequestAwareAuthenticationSuccessHandler} and helps in handling OAuth2
 * authentication tokens, user creation or updating, token generation, and redirection
 * after successful authentication.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private final UserService userService;
    private final JoseProviderStrategyContext joseProviderStrategyContext;
    private final RefreshTokenService refreshTokenService;
    private final ObjectProvider<PasswordEncoder> passwordEncoderProvider;
    private final OAuth2UserInfoStrategyFactory strategyFactory;
    private final DeviceInfoUtils deviceInfoUtils;

    /**
     * Retrieves a {@link PasswordEncoder} instance from the provider.
     *
     * @return a {@link PasswordEncoder} instance if available, or null if not provided.
     */
    private PasswordEncoder getPasswordEncoder() {
        return passwordEncoderProvider.getIfAvailable();
    }

    /**
     * Handles the successful authentication event for OAuth2 login. This method processes
     * OAuth2 authentication tokens, retrieves user information from the associated attributes,
     * creates or updates users in the system, generates access tokens, and finalizes the
     * authentication process.
     *
     * @param request the HTTP request that triggered the authentication success
     * @param response the HTTP response to be sent back to the client
     * @param authentication the authentication object containing details about the successful authentication
     * @throws IOException if an input or output exception occurs
     * @throws ServletException if a servlet exception occurs during request processing
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2AuthenticationToken oAuth2Token = (OAuth2AuthenticationToken) authentication;
        String provider = oAuth2Token.getAuthorizedClientRegistrationId();
        OAuth2UserInfoStrategy strategy = strategyFactory.getStrategy(provider);

        DefaultOAuth2User principal = (DefaultOAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = principal.getAttributes();

        User user = createAndSavaUserFromOAuth2(strategy, attributes);
        updateAuthentication(authentication, attributes, strategy, user);

        UserAgentResponse userAgentInfo = deviceInfoUtils.extractUserAgentInfo(request);

        String accessToken = generateAccessTokenAndSaveRefreshTokenForOAuth2(
                authentication, user.getEmail(), userAgentInfo, user
        );

        addAccessTokenToCookie(response, accessToken);

        String redirectUrl = frontendUrl + "/login?status=success&message=" +
                URLEncoder.encode("login via oauth2 success", StandardCharsets.UTF_8);
        setAlwaysUseDefaultTargetUrl(true);
        setDefaultTargetUrl(redirectUrl);
        super.onAuthenticationSuccess(request, response, authentication);
    }

    /**
     * Creates and saves a new {@link User} based on OAuth2 user information attributes.
     * If a user with the provided email already exists, the existing user will be returned.
     * Otherwise, a new user will be created and saved using the provided strategy and attributes.
     *
     * @param strategy the {@link OAuth2UserInfoStrategy} to extract user information from the attributes
     * @param attributes a map containing user information attributes provided by the OAuth2 provider
     * @return the newly created or existing {@link User} entity
     */
    private User createAndSavaUserFromOAuth2(
            OAuth2UserInfoStrategy strategy,
            Map<String, Object> attributes
    ) {
        String name = Optional.ofNullable(strategy.getName(attributes))
                .orElse(generateRandomUsername());
        String email = Optional.ofNullable(strategy.getEmail(attributes))
                .orElse(generateRandomEmail());
        String avatarUrl = Optional.ofNullable(strategy.getAvatarUrl(attributes))
                .orElse(generateRandomAvatarUrl());

        return userService.findByEmail(email).orElseGet(() -> {
            String randomPasswordForOauth2 = generateRandomPasswordFoForOauth2();
            String encodedPassword = getPasswordEncoder().encode(randomPasswordForOauth2);
            String[] nameParts = name.split(" ");
            String firstName = nameParts.length > 0 ? nameParts[0] : "";
            StringBuilder lastNameBuilder = new StringBuilder();
            for (int i = 1; i < nameParts.length; i++) {
                lastNameBuilder.append(nameParts[i]);
                if (i < nameParts.length - 1) {
                    lastNameBuilder.append(" ");
                }
            }
            String lastName = lastNameBuilder.toString();

            User newUser = createUserForOauth2(
                    email, encodedPassword, avatarUrl, firstName, lastName, strategy
            );
            return userService.createUserFromOAuth2(newUser);
        });
    }

    /**
     * Generates an access token and saves the corresponding refresh token for an OAuth2 authentication process.
     *
     * @param authentication the authentication object containing details about the authenticated user
     * @param email the email address of the authenticated user
     * @param userAgentInfo details about the user's device and browser
     * @param oauth2User the user entity representing the authenticated user
     * @return the generated access token
     */
    private String generateAccessTokenAndSaveRefreshTokenForOAuth2(
            Authentication authentication, String email, UserAgentResponse userAgentInfo, User oauth2User
    ) {
        String sid = UUID.randomUUID().toString();
        String accessToken = joseProviderStrategyContext.getActiveProvider().generateAccessToken(authentication, sid, oauth2User);
        String refreshToken = joseProviderStrategyContext.getActiveProvider().generateRefreshToken(authentication, sid, oauth2User);
        Date expirationDate = joseProviderStrategyContext.getActiveProvider().getExpirationDateFromToken(refreshToken);

        RefreshToken refreshTokenToDb = RefreshToken.builder()
                .token(refreshToken)
                .email(authentication.getName())
                .deviceIpAddress(userAgentInfo.getDeviceIpAddress())
                .expiryDate(expirationDate)
                .revoked(false)
                .build();

        refreshTokenService.saveRefreshToken(
                refreshTokenToDb.getToken(), email,
                userAgentInfo.getDeviceIpAddress(), userAgentInfo.getDeviceName(), userAgentInfo.getDeviceOs(),
                userAgentInfo.getBrowser(), userAgentInfo.getBrowserVersion()
        );
        return accessToken;
    }

    /**
     * Adds an access token to the HTTP response as an HTTP-only cookie.
     * <p>
     * The cookie has the following attributes:
     * <p> - Name: "accessToken"
     * <p> - Value: The provided access token
     * <p> - HttpOnly: true
     * <p> - Secure: false (should be set to true in production when using HTTPS)
     * <p> - SameSite: Strict
     * <p> - Path: "/"
     * <p> - MaxAge: 15 minutes
     *
     * @param response the HTTP response to which the access token cookie will be added
     * @param accessToken the access token to be stored in the cookie
     */
    private void addAccessTokenToCookie(HttpServletResponse response, String accessToken) {
        response.addHeader("Set-Cookie",
                ResponseCookie.from("accessToken", accessToken)
                        .httpOnly(true)
                        .secure(false) //set to true when using https
                        .sameSite("Strict")
                        .path("/")
                        .maxAge(Duration.ofMinutes(15))
                        .build()
                        .toString()
        );
    }

    /**
     * Updates the current authentication object with new attributes and user information.
     * This method constructs a new OAuth2 authentication token based on the provided strategy,
     * user attributes, and the current authentication, and then sets it in the security context.
     *
     * @param authentication the current authentication object to be updated
     * @param attributes a map containing user information attributes provided by the OAuth2 provider
     * @param strategy the strategy used to retrieve key attributes (e.g., email) from the provided attributes
     * @param user the user entity representing the authenticated user
     */
    private void updateAuthentication(
            Authentication authentication, Map<String, Object> attributes,
            OAuth2UserInfoStrategy strategy, User user
    ) {
        String usernameAttributeKey  = strategy.getEmailAttributeKey(attributes);
        DefaultOAuth2User newUser = new DefaultOAuth2User(
                user.getAuthorities(), attributes, usernameAttributeKey
        );

        Authentication securityAuth = new OAuth2AuthenticationToken(
                newUser, user.getAuthorities(),
                ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId()
        );
        SecurityContextHolder.getContext().setAuthentication(securityAuth);
    }

    /**
     * Creates a new {@link User} instance configured for OAuth2 authentication.
     *
     * @param email the email address of the user
     * @param encodedPassword the encoded password for the user
     * @param avatarUrl the URL of the user's avatar or profile picture
     * @param firstName the first name of the user
     * @param lastName the last name of the user
     * @param strategy the {@link OAuth2UserInfoStrategy} used to determine the OAuth2 provider and user details
     * @return a newly created {@link User} instance representing the OAuth2 authenticated user
     */
    private static User createUserForOauth2(
            String email, String encodedPassword, String avatarUrl, String firstName, String lastName,
            OAuth2UserInfoStrategy strategy
    ) {
        return User.builder()
                .email(email)
                .displayedUsername(email)
                .password(encodedPassword)
                .profilePictureUrl(avatarUrl)
                .firstName(firstName)
                .lastName(lastName)
                .role(Role.USER)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .isEnabled(true)
                .lastLoginTime(LocalDateTime.now())
                .authProvider(strategy.getAuthProvider())
                .build();
    }

    private static final Random random = new Random();

    /**
     * Generates a random password for OAuth2 authentication.
     * The generated password contains 20 characters and includes both uppercase and lowercase letters.
     *
     * @return a randomly generated password for OAuth2 authentication
     */
    private static String generateRandomPasswordFoForOauth2() {
        return RandomStringUtils.random(20, 0, 0, true,true, null, random);
    }

    /**
     * Generates a random username consisting of 10 alphanumeric characters.
     * <p>
     * This method leverages the RandomStringUtils to produce a string with a
     * combination of uppercase, lowercase, and numeric characters.
     *
     * @return a randomly generated username as a string
     */
    private static String generateRandomUsername() {
        return RandomStringUtils.random(10, 0, 0, true,true, null, random);
    }

    /**
     * Generates a random email address.
     * <p>
     * The generated email address consists of a random, alphanumeric string of 10 characters
     * followed by the domain "@bbmovie.com".
     *
     * @return a randomly generated email address
     */
    private static String generateRandomEmail() {
        return RandomStringUtils.random(10, 0, 0, true,true, null, random) + "@bbmovie.com";
    }

    /**
     * Generates a random avatar URL using the Picsum Photos free API.
     * The generated URL corresponds to a random image with dimensions 200x200,
     * selected from an available range of 1084 images on the Picsum service.
     *
     * @return a randomly generated avatar URL as a {@link String}
     */
    private static String generateRandomAvatarUrl() {
        int from1to1084cuzFreeApiHas1084img = random.nextInt(1084) + 1;
        return "https://picsum.photos/id/" + from1to1084cuzFreeApiHas1084img + "/200/200";
    }
}