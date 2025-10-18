package com.bbmovie.auth.service.auth;

import com.bbmovie.auth.dto.DeviceInfo;
import com.bbmovie.auth.dto.request.ChangePasswordRequest;
import com.bbmovie.auth.dto.request.LoginRequest;
import com.bbmovie.auth.dto.request.RegisterRequest;
import com.bbmovie.auth.dto.request.ResetPasswordRequest;
import com.bbmovie.auth.dto.response.*;
import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.entity.enumerate.AuthProvider;
import com.bbmovie.auth.entity.enumerate.Role;
import com.bbmovie.auth.entity.jose.RefreshToken;
import com.bbmovie.auth.exception.*;
import com.bbmovie.auth.repository.UserRepository;
import com.bbmovie.auth.security.jose.dto.TokenPair;
import com.bbmovie.auth.security.jose.provider.JoseProvider;
import com.bbmovie.auth.service.auth.verify.otp.OtpService;
import com.bbmovie.auth.service.auth.verify.magiclink.ChangePasswordTokenService;
import com.bbmovie.auth.service.auth.verify.magiclink.EmailVerifyTokenService;
import com.bbmovie.auth.service.nats.ABACEventProducer;
import com.bbmovie.auth.service.nats.EmailEventProducer;
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
import org.springframework.beans.factory.annotation.Autowired;
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
import java.time.ZonedDateTime;
import java.util.*;

import static com.bbmovie.auth.constant.AuthErrorMessages.EMAIL_ALREADY_EXISTS;
import static com.bbmovie.auth.constant.UserErrorMessages.USER_NOT_FOUND_BY_EMAIL;
import static com.example.common.entity.JoseConstraint.JWT_LOGOUT_BLACKLIST_PREFIX;
import static com.example.common.entity.JoseConstraint.JosePayload.SID;
import static com.example.common.entity.JoseConstraint.JosePayload.SUB;

@Service
@Log4j2
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final EmailVerifyTokenService emailVerifyTokenService;
    private final EmailEventProducer emailEventProducer;
    private final LogoutEventProducer logoutEventProducer;
    private final ABACEventProducer abacEventProducer;
    private final RefreshTokenService refreshTokenService;
    private final ChangePasswordTokenService changePasswordTokenService;
    private final OtpService otpService;
    private final DeviceInfoUtils deviceInfoUtils;
    private final UserAgentAnalyzerUtils userAgentAnalyzer;
    private final JoseProvider joseProvider;

    @Autowired
    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            JoseProvider joseProvider,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            EmailVerifyTokenService emailVerifyTokenService,
            EmailEventProducer emailEventProducer,
            LogoutEventProducer logoutEventProducer,
            ABACEventProducer abacEventProducer,
            RefreshTokenService refreshTokenService,
            ChangePasswordTokenService changePasswordTokenService,
            OtpService otpService,
            DeviceInfoUtils deviceInfoUtils,
            UserAgentAnalyzerUtils userAgentAnalyzer
    ) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.emailVerifyTokenService = emailVerifyTokenService;
        this.emailEventProducer = emailEventProducer;
        this.logoutEventProducer = logoutEventProducer;
        this.abacEventProducer = abacEventProducer;
        this.refreshTokenService = refreshTokenService;
        this.changePasswordTokenService = changePasswordTokenService;
        this.otpService = otpService;
        this.deviceInfoUtils = deviceInfoUtils;
        this.userAgentAnalyzer = userAgentAnalyzer;
        this.joseProvider = joseProvider;
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
                .orElseThrow(() -> new UserNotFoundException("Invalid username/email or password"));

        boolean correctPassword = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());
        if (!correctPassword) {
            throw new AuthenticationException("Invalid username/email or password");
        }
        boolean isUserEnabled = user.isEnabled();
        if (!isUserEnabled) {
            throw new AccountNotEnabledException("Account is not enabled. Please verify your email first.");
        }
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword(), user.getAuthorities())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

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
     * Registers a new user account with the provided registration details.
     * Ensures email uniqueness, validates passwords, and generates a verification token.
     *
     * @param request the registration details containing the user's email, password,
     *                and confirmation password
     * @throws EmailAlreadyExistsException if the email address already exists in the system
     * @throws AuthenticationException if the password and confirmation password does not match
     */
    @Transactional
    @Override
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(EMAIL_ALREADY_EXISTS);
        }

        log.info("password: {}, {}", request.getPassword(), request.getConfirmPassword());
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AuthenticationException("Password and confirm password does not match. Please try again.");
        }

        User user = createUserFromRegisterRequest(request);
        User savedUser = userRepository.save(user);

        String verificationToken = emailVerifyTokenService.generateVerificationToken(savedUser);
        emailEventProducer.sendMagicLinkOnRegistration(savedUser.getEmail(), verificationToken);
    }

    /**
     * Verifies the user account using the provided email verification token.
     * If the token is valid and the user's account is not yet enabled,
     * the user's account is activated, and the token is deleted.
     * Otherwise, appropriate exceptions are thrown for invalid tokens or other errors.
     *
     * @param token the email verification token used to verify the user account
     * @return a success message indicating that the account verification is completed,
     *         or that the account was already verified
     * @throws TokenVerificationException if the token is null or empty
     * @throws AuthenticationException if the token is invalid or the user cannot be verified
     */
    @Transactional
    @Override
    public String verifyAccountByEmail(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new TokenVerificationException("Verification token cannot be null or empty");
        }
        String email = emailVerifyTokenService.getEmailForToken(token);
        if (email == null) {
            log.warn("Token {} was already used or is invalid", token);
            throw new AuthenticationException("Account verification failed. Please try again.");
        }

        log.info("Trying to verify: {}", email);
        User user = userRepository.findByEmail(email).orElseThrow(() -> new AuthenticationException("Unable to verify user"));
        if (user.isEnabled()) {
            log.info("Email {} already verified", email);
            return "Account already verified.";
        }
        user.setIsEnabled(true);
        userRepository.save(user);
        emailVerifyTokenService.deleteToken(token);

        return "Account verification successful. Please login to continue.";
    }

    /**
     * Sends a verification email to a user with the specified email address. This method generates
     * a verification token and sends it to the user's email. If the email is already verified or
     * the user does not exist, an appropriate exception is thrown.
     *
     * @param email the email address to which the verification email will be sent
     *              (must not be null or empty)
     * @throws IllegalArgumentException if the email is null or empty
     * @throws UserNotFoundException if no user exists with the given email address
     * @throws EmailAlreadyVerifiedException if the email is already verified
     * @throws TokenVerificationException if an error occurs during the token generation or sending process
     */
    @Override
    public void sendVerificationEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(
                        String.format(USER_NOT_FOUND_BY_EMAIL, email)
                ));

        if (user.isEnabled()) {
            throw new EmailAlreadyVerifiedException("Email is already verified");
        }
        try {
            String token = emailVerifyTokenService.generateVerificationToken(user);
            emailEventProducer.sendMagicLinkOnRegistration(email, token);
        } catch (Exception e) {
            throw new TokenVerificationException("Failed to send verification email: " + e.getMessage());
        }
    }

    /**
     * Sends a one-time password (OTP) to the user's phone number.
     * This method generates an OTP token for the specified user and sends it
     * using an email event producer.
     *
     * @param user the user for whom the OTP will be generated and sent. The user must have a valid phone number.
     * @throws IllegalArgumentException if the user's phone number is null or empty.
     */
    @Override
    public void sendOtp(User user) {
        if (user.getPhoneNumber().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }
        String otp = otpService.generateOtpToken(user);
        emailEventProducer.sendOtp(user.getPhoneNumber(), otp);
    }

    /**
     * Verifies a user's account using the provided OTP (One-Time Password).
     * This method enables the user's account if the OTP is valid, not previously used,
     * and associated with a registered email. If the OTP is null, empty, or invalid,
     * an appropriate exception or log entry is generated.
     *
     * @param otp the One-Time Password provided by the user for account verification
     * @throws TokenVerificationException if the OTP is null or empty
     * @throws UserNotFoundException if no user is found for the email linked to the OTP
     */
    @Transactional
    @Override
    public void verifyAccountByOtp(String otp) {
        if (otp == null || otp.trim().isEmpty()) {
            throw new TokenVerificationException("Verification token cannot be null or empty");
        }
        String email = otpService.getEmailForOtpToken(otp);
        if (email == null) {
            log.warn("Otp {} was already used or is invalid", otp);
            return;
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(
                        String.format(USER_NOT_FOUND_BY_EMAIL, email)
                ));
        if (user.isEnabled()) {
            log.info("Account {} already verified", email);
            return;
        }
        user.setIsEnabled(true);
        userRepository.save(user);
        otpService.deleteOtpToken(otp);
    }

    /**
     * Logs out a user from all devices by revoking all active tokens associated with the given email.
     *
     * @param email the email address of the user to be logged out from all devices
     */
    private void logoutFromAllDevices(String email) {
        revokeAllTokensFromAllDevices(email);
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
     * Changes the password for an existing user. The method performs the necessary
     * validations to ensure that the current password is correct, the new password
     * is different from the current password, and the new password matches the
     * confirmation password. Upon a successful password change, an email notification
     * is sent, and the user is logged out of all devices.
     *
     * @param requestEmail the email or username of the user requesting the password change
     * @param request the request object containing the current password, new password, and confirmation of the new password
     *
     * @throws UserNotFoundException if the user associated with the given email or username does not exist
     * @throws AuthenticationException if the current password is incorrect, the new password matches the current password,
     *                                  or the new password and confirmation password does not match
     */
    @Override
    @Transactional(noRollbackFor = CustomEmailException.class)
    public void changePassword(String requestEmail, ChangePasswordRequest request) {
        User user = userRepository.findByDisplayedUsername(requestEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found for username: " + requestEmail));
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(requestEmail, request.getCurrentPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        boolean correctPassword = passwordEncoder.matches(request.getCurrentPassword(), user.getPassword());
        if (!correctPassword) {
            throw new AuthenticationException("Current password is incorrect");
        }
        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw new AuthenticationException("New password can not be the same as the current password. Please try again.");
        }
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new AuthenticationException("New password and confirm password do not match. Please try again.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        emailEventProducer.sendNotificationOnChangingPassword(user.getEmail(), ZonedDateTime.now());

        logoutFromAllDevices(user.getEmail());
    }

    /**
     * Sends a "Forgot Password" email to the user associated with the provided email address.
     * This method generates a password reset token and triggers an email containing a magic link
     * for the user to reset their password.
     *
     * @param email the email address of the user requesting a password reset
     * @throws UserNotFoundException if no user is found for the provided email address
     */
    @Override
    public void sendForgotPasswordEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found for email: " + email));
        String token = changePasswordTokenService.generateChangePasswordToken(user);
        emailEventProducer.sendMagicLinkOnForgotPassword(email, token);
    }

    /**
     * Resets the password of a user using a provided reset token and new password details.
     * Validates the token, checks if the new password matches the confirmation password,
     * updates the user's password, and invalidates the token upon successful reset.
     * Sends a notification email to the user and logs out from all devices.
     *
     * @param token the reset password token used to identify the user and validate the request
     * @param request the request object containing the new password and confirmation for the password change
     */
    @Override
    public void resetPassword(String token, ResetPasswordRequest request) {
        String email = changePasswordTokenService.getEmailForToken(token);
        if (email == null) {
            log.warn("Reset password token {} was already used or is invalid", token);
            return;
        }
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new UserNotFoundException("User not found for email: " + email)
        );

        if (request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new AuthenticationException("New password and confirm password do not match. Please try again.");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        changePasswordTokenService.deleteToken(token);
        emailEventProducer.sendNotificationOnChangingPassword(user.getEmail(), ZonedDateTime.now());
        log.info("Password reset for user {} successful", email);

        logoutFromAllDevices(email);
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

    /**
     * Converts a User entity into a UserResponse DTO.
     *
     * @param user the User object containing user information
     * @return a UserResponse object constructed using the specified User's data
     */
    private static UserResponse createUserResponseFromUser(User user) {
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
    private static AuthResponse createAuthResponse(TokenPair tokenPair, User user) {
        return AuthResponse.builder()
                .accessToken(tokenPair.accessToken())
                .refreshToken(tokenPair.refreshToken())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
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
     * Creates a new User object based on the provided RegisterRequest.
     *
     * @param request the register request containing the user details such as email, username, password, phone number, first name, last name, age, and region
     * @return a newly created User instance with the provided details and default settings for roles, authentication provider, and account status
     */
    private User createUserFromRegisterRequest(RegisterRequest request) {
        return User.builder()
                .email(request.getEmail())
                .displayedUsername(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
//                .phoneNumber(request.getPhoneNumber())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
//                .age(request.getAge())
//                .region(request.getRegion())
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .isEnabled(false)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();
    }
}