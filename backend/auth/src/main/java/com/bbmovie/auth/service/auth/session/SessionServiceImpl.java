package com.bbmovie.auth.service.auth.session;

import com.bbmovie.auth.dto.DeviceInfo;
import com.bbmovie.auth.dto.request.LoginRequest;
import com.bbmovie.auth.dto.response.*;
import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.entity.enumerate.Role;
import com.bbmovie.auth.entity.jose.RefreshToken;
import com.bbmovie.auth.exception.AccountNotEnabledException;
import com.bbmovie.auth.exception.AuthenticationException;
import com.bbmovie.auth.exception.BadLoginException;
import com.bbmovie.auth.exception.UserNotFoundException;
import com.bbmovie.auth.repository.UserRepository;
import com.bbmovie.auth.security.jose.dto.TokenPair;
import com.bbmovie.auth.security.jose.provider.JoseProvider;
import com.bbmovie.auth.service.auth.RefreshTokenService;
import com.bbmovie.auth.service.nats.ABACEventProducer;
import com.bbmovie.auth.service.nats.LogoutEventProducer;
import com.bbmovie.auth.utils.DeviceInfoUtils;
import com.bbmovie.auth.utils.IpAddressUtils;
import com.bbmovie.auth.utils.UserAgentAnalyzerUtils;
import com.example.common.entity.JoseConstraint;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import nl.basjes.parse.useragent.UserAgent;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static com.bbmovie.auth.constant.UserErrorMessages.USER_NOT_FOUND_BY_EMAIL;
import static com.bbmovie.auth.security.SecurityConfig.isDeprecatedHash;
import static com.example.common.entity.JoseConstraint.JWT_LOGOUT_BLACKLIST_PREFIX;
import static com.example.common.entity.JoseConstraint.JosePayload.SID;
import static com.example.common.entity.JoseConstraint.JosePayload.SUB;

@Log4j2
@Service
public class SessionServiceImpl implements SessionService {

    private final JoseProvider joseProvider;
    private final LogoutEventProducer logoutEventProducer;
    private final UserAgentAnalyzerUtils userAgentAnalyzer;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final DeviceInfoUtils deviceInfoUtils;
    private final ABACEventProducer abacEventProducer;

    public SessionServiceImpl(
            JoseProvider joseProvider, LogoutEventProducer logoutEventProducer, UserAgentAnalyzerUtils userAgentAnalyzer,
            RefreshTokenService refreshTokenService, UserRepository userRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, DeviceInfoUtils deviceInfoUtils, ABACEventProducer abacEventProducer) {
        this.joseProvider = joseProvider;
        this.logoutEventProducer = logoutEventProducer;
        this.userAgentAnalyzer = userAgentAnalyzer;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.deviceInfoUtils = deviceInfoUtils;
        this.abacEventProducer = abacEventProducer;
    }

    /**
     * Authenticates a user based on login credentials provided in the request.
     * Validates the user's email and password, checks if the user's account is enabled,
     * updates the last login time, generates token pairs, detects new login sessions,
     * and handles multifactor authentication if enabled.
     *
     * @param loginRequest The login request containing the user's email and password.
     * @param request The HTTP servlet request to retrieve additional request details.
     * @return A {@code LoginResponse} object containing user and authentication details.
     * @throws UserNotFoundException if the user is not found based on the provided email.
     * @throws AuthenticationException if the password does not match or is invalid.
     * @throws AccountNotEnabledException if the user's account is not verified or enabled.
     */
    @Override
    public LoginResponse login(LoginRequest loginRequest, HttpServletRequest request) {
        log.info("Login request received for email: {}, {}", loginRequest, request);

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(BadLoginException::new);

        boolean correctPassword = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());
        if (!correctPassword) {
            throw new BadLoginException();
        }
        boolean isUserEnabled = user.isEnabled();
        if (!isUserEnabled) {
            throw new AccountNotEnabledException("Account is not enabled. Please verify your email first.");
        }
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword(), user.getAuthorities())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        boolean isPasswordDeprecated = isDeprecatedHash(user.getPassword());
        if (isPasswordDeprecated) {
            String newPassword = passwordEncoder.encode(loginRequest.getPassword());
            user.setPassword(newPassword);
        }

        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        String sid = UUID.randomUUID().toString();
        TokenPair tokenPair = joseProvider.generateTokenPair(authentication, user);

        boolean isNewSession = refreshTokenService.findAllValidByEmail(user.getEmail())
                .stream()
                .noneMatch(token -> token.getSid().equals(sid));
        if (isNewSession) {
            log.info("New login detected for user {}", user.getEmail());
        } else {
            log.info("Existing login detected for user {}", user.getEmail());
        }
        boolean isMfaEnabled = user.isMfaEnabled();
        if (isNewSession && isMfaEnabled) {
            log.info("Detected login from different device.");
        }

        if (isNewSession && !isMfaEnabled) {
            log.info("Notify user about unknown device login.");
        }

        UserAgentResponse userAgentResponse = getUserDeviceInformation(request);
        saveRefreshToken(tokenPair, user, userAgentResponse);

        AuthResponse authResponse = createAuthResponse(tokenPair, user);
        UserResponse userResponse = createUserResponseFromUser(user);

        return LoginResponse.fromUserAndAuthResponse(userResponse, authResponse);
    }

    /**
     * Retrieves the login response for a user authenticated via OAuth2 login.
     *
     * @param userDetails the details of the authenticated user
     * @param request the HTTP servlet request containing authentication-related information
     * @return the login response containing user and authentication details
     * @throws ResponseStatusException if the access token cookie is missing
     */
    @Override
    public LoginResponse getLoginResponseFromOAuth2Login(UserDetails userDetails, HttpServletRequest request) {
        UserResponse userResponse = loadAuthenticatedUserInformation(userDetails.getUsername());
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(null)
                .email(userResponse.getEmail())
                .role(Role.USER)
                .build();
        return LoginResponse.fromUserAndAuthResponse(userResponse, authResponse);
    }

    /**
     * Updates user refresh tokens and associated session identifiers in response to changes
     * in Attribute-Based Access Control (ABAC) policies. This method retrieves all valid refresh
     * tokens for the specified user, generates new tokens with updated session identifiers,
     * blacklists the old tokens in the ABAC system, and queues an event to propagate the change.
     *
     * @param email the email address of the user whose tokens are to be updated.
     *              This is used to locate the user in the repository and fetch associated tokens.
     */
    //need to check again
    @Transactional
    @Override
    public void updateUserTokensOnAbacChange(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        List<RefreshToken> refreshTokens = refreshTokenService.findAllValidByEmail(email);

        for (RefreshToken refreshToken : refreshTokens) {
            String oldSid = refreshToken.getSid();
            joseProvider.addTokenToABACBlacklist(oldSid);
            abacEventProducer.send(JoseConstraint.JWT_ABAC_BLACKLIST_PREFIX + oldSid);

            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(email, "", authorities);
            Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, "", authorities);

            String newSid = UUID.randomUUID().toString();
            //need to pass time to make sure new-issued token expired same with old token
            String newRefreshToken = joseProvider.generateRefreshToken(authentication, newSid, user);

            refreshToken.setToken(newRefreshToken);
            refreshToken.setSid(newSid);
            refreshToken.setJti(UUID.randomUUID().toString());
            refreshToken.setExpiryDate(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000));
            refreshToken.setRevoked(false);
            refreshTokenService.saveRefreshToken(newSid, newRefreshToken);
        }
    }

    /**
     * Loads and retrieves the authenticated user information based on the provided email.
     *
     * @param email the email address of the user whose information is to be retrieved
     * @return the user response object containing the details of the authenticated user
     * @throws UserNotFoundException if no user is found with the given email address
     */
    @Override
    public UserResponse loadAuthenticatedUserInformation(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(String.format(USER_NOT_FOUND_BY_EMAIL, email)));
        return createUserResponseFromUser(user);
    }

    /**
     * Retrieves a list of all logged-in devices for a user based on the provided JWT token.
     * The method identifies the current device and marks it in the response list.
     *
     * @param accessToken the JSON Web Token representing the user's session and identity
     * @param request the HTTP servlet request containing details such as headers for identifying the current device and IP address
     * @return a list of {@code LoggedInDeviceResponse} objects representing the devices associated with the user,
     * including the current device and other previously logged-in devices
     */
    @Override
    public List<LoggedInDeviceResponse> getAllLoggedInDevices(String accessToken, HttpServletRequest request) {
        String userAgentString = request.getHeader("User-Agent");
        UserAgent currentAgent = userAgentAnalyzer.parse(userAgentString);
        String currentDeviceName = currentAgent.getValue("DeviceName");
        String currentIp = IpAddressUtils.getClientIp(request);

        String email = joseProvider.getUsernameFromToken(accessToken);

        List<DeviceInfo> allDevices = getAllLoggedInDevicesRaw(email);
        List<LoggedInDeviceResponse> result = new ArrayList<>();

        for (DeviceInfo device : allDevices) {
            boolean isCurrent = device.deviceName().equals(currentDeviceName) && device.ipAddress().equals(currentIp);
            result.add(new LoggedInDeviceResponse(device.deviceName(), device.ipAddress(), isCurrent));
        }

        return result;
    }

    /**
     * Revokes authentication cookies by creating cookies with the same names as the
     * authentication cookies but with null or expired values and adding them to the response.
     *
     * @param response the HTTP response object to which the revoked cookies will be added
     */
    @Override
    public void revokeAuthCookies(HttpServletResponse response) {
        Cookie revokeAccessTokenCookie = revokeCookie("accessToken");
        response.addCookie(revokeAccessTokenCookie);

        Cookie revokeJSESSIONID = revokeCookie("JSESSIONID");
        response.addCookie(revokeJSESSIONID);
    }

    /**
     * Logs out the user from the current device by revoking the access token.
     *
     * @param accessToken the access token of the current session to be revoked
     */
    @Transactional
    @Override
    public void logoutFromCurrentDevice(String accessToken) {
        revokeTokensFromCurrentDevice(accessToken);
    }

    /**
     * Logs out a user from a specific device by revoking the associated tokens.
     *
     * @param accessToken The access token of the user initiating the logout request.
     * @param targetSid The unique identifier of the device to be logged out.
     */
    @Transactional
    @Override
    public void logoutFromOneDevice(String accessToken, String targetSid) {
        revokeTokensFromSpecificDevice(accessToken, targetSid);
    }

    /**
     * Saves the refresh token along with user and device-specific information.
     *
     * @param tokenPair the pair of tokens containing the refresh token to be saved
     * @param user the user associated with the refresh token
     * @param userAgentResponse the response object containing information about the user's device and browser
     */
    private void saveRefreshToken(TokenPair tokenPair, User user, UserAgentResponse userAgentResponse) {
        refreshTokenService.saveRefreshToken(
                tokenPair.refreshToken(), user.getEmail(),
                userAgentResponse.getDeviceIpAddress(),
                userAgentResponse.getDeviceName(),
                userAgentResponse.getDeviceOs(),
                userAgentResponse.getBrowser(),
                userAgentResponse.getBrowserVersion()
        );
    }

    /**
     * Converts a User entity into a UserResponse DTO.
     *
     * @param user the User object containing user information
     * @return a UserResponse object constructed using the specified User's data
     */
    public static UserResponse createUserResponseFromUser(User user) {
        return UserResponse.builder()
                .username(user.getDisplayedUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .build();
    }

    /**
     * Creates an authentication response containing the access token,
     * refresh token, user's email, and user's role.
     *
     * @param tokenPair the pair of tokens including access token and refresh token
     * @param user the user object containing email and role information
     * @return an AuthResponse object containing authentication information
     */
    private AuthResponse createAuthResponse(TokenPair tokenPair, User user) {
        return AuthResponse.builder()
                .accessToken(tokenPair.accessToken())
                .refreshToken(tokenPair.refreshToken())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    /**
     * Revokes tokens associated with a specific device based on the provided target session identifier (SID).
     * The method ensures that the target SID is not the same as the current session SID, preventing self-revocation.
     * It interacts with the token service to delete associated refresh tokens, adds the token to a logout blacklist,
     * and triggers a logout event for the target SID.
     *
     * @param accessToken The access token of the currently authenticated user, used to extract claims.
     * @param targetSid The session identifier (SID) of the specific device whose tokens are to be revoked.
     *                  Must not be the same as the current session's SID.
     * @throws IllegalArgumentException Thrown if the target SID matches the current session SID.
     */
    private void revokeTokensFromSpecificDevice(String accessToken, String targetSid) {
        Map<String, Object> claims = joseProvider.getClaimsFromToken(accessToken);
        String sid = claims.get(SID).toString();
        String email = claims.get(SUB).toString();

        if (Objects.equals(targetSid, sid)) {
            throw new IllegalArgumentException("Target SID cannot be the same with your current session.");
        }

        refreshTokenService.deleteByEmailAndSid(email, targetSid);
        joseProvider.addTokenToLogoutBlacklist(targetSid);
        logoutEventProducer.send(JWT_LOGOUT_BLACKLIST_PREFIX + targetSid);
    }


    /**
     * <b>WARNING: </b> Potential for incorrect session revocation or security risks (e.g., revoking another user’s session).
     * <p>
     * Revokes all active tokens associated with a specific user's email address across all devices.
     * This process includes deleting all refresh tokens for the user and blacklisting associated session tokens.
     * Additionally, an event is produced to notify that the tokens have been invalidated.
     *
     * @param email the email address of the user whose tokens need to be revoked
     */
    private void revokeAllTokensFromAllDevices(String email) {
        List<String> allSessions = refreshTokenService.getAllSessionsByEmail(email);
        refreshTokenService.deleteAllRefreshTokenByEmail(email);
        for (String sid : allSessions) {
            joseProvider.addTokenToLogoutBlacklist(sid);
            logoutEventProducer.send(JWT_LOGOUT_BLACKLIST_PREFIX + sid);
        }
    }

    /**
     * Logs out a user from all devices by revoking all active tokens associated with the given email.
     *
     * @param email the email address of the user to be logged out from all devices
     */
    @Override
    public void logoutFromAllDevices(String email) {
        revokeAllTokensFromAllDevices(email);
    }

    /**
     * Retrieves a list of all devices where a user is logged in, based on their email.
     *
     * @param email The email address of the user whose logged-in devices are to be retrieved.
     * @return A list of {@code DeviceInfo} objects representing the user's logged-in devices.
     */
    private List<DeviceInfo> getAllLoggedInDevicesRaw(String email) {
        List<RefreshToken> tokens = refreshTokenService.findAllValidByEmail(email);
        return tokens.stream()
                .map(token -> new DeviceInfo(token.getDeviceName(), token.getDeviceIpAddress()))
                .distinct()
                .toList();
    }

    /**
     * Revokes tokens associated with the current device by blacklisting the session ID (SID)
     * extracted from the provided access token. This effectively logs out the device by
     * ensuring its tokens can no longer be used.
     *
     * @param accessToken the access token associated with the current device,
     *                    used to extract the session ID (SID) for revocation.
     */
    private void revokeTokensFromCurrentDevice(String accessToken) {
        Map<String, Object> claims = joseProvider.getClaimsFromToken(accessToken);
        String sid = claims.get(SID).toString();
        refreshTokenService.deleteRefreshToken(sid);
        joseProvider.addTokenToLogoutBlacklist(sid);

        logoutEventProducer.send(JWT_LOGOUT_BLACKLIST_PREFIX + sid);
    }

    /**
     * Retrieves information about the user's device based on the user agent data
     * present in the HTTP request.
     *
     * @param request the HttpServletRequest object containing the user agent and
     *                other request-related data
     * @return a UserAgentResponse object that contains the device information
     *         extracted from the user agent string
     */
    @Override
    public UserAgentResponse getUserDeviceInformation(HttpServletRequest request) {
        return deviceInfoUtils.extractUserAgentInfo(request);
    }

    /**
     * Revokes a cookie by setting its value to null, its max age to 0, and marking it as HTTP only.
     * The cookie path is set to "/".
     *
     * @param cookieName the name of the cookie to be revoked
     * @return a Cookie object representing the revoked cookie
     */
    private Cookie revokeCookie(String cookieName) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        return cookie;
    }
}
