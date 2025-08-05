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
import com.bbmovie.auth.security.jose.JoseProviderStrategy;
import com.bbmovie.auth.security.jose.JoseProviderStrategyContext;
import com.bbmovie.auth.security.jose.config.TokenPair;
import com.bbmovie.auth.service.auth.verify.otp.OtpService;
import com.bbmovie.auth.service.auth.verify.magiclink.ChangePasswordTokenService;
import com.bbmovie.auth.service.auth.verify.magiclink.EmailVerifyTokenService;
import com.bbmovie.auth.service.kafka.ABACEventProducer;
import com.bbmovie.auth.service.kafka.EmailEventProducer;
import com.bbmovie.auth.service.kafka.LogoutEventProducer;
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
import org.springframework.http.HttpStatus;
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
import org.springframework.web.util.WebUtils;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

import static com.bbmovie.auth.constant.error.AuthErrorMessages.EMAIL_ALREADY_EXISTS;
import static com.bbmovie.auth.constant.error.UserErrorMessages.USER_NOT_FOUND_BY_EMAIL;

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
    private final JoseProviderStrategy joseProviderStrategy;

    @Autowired
    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            JoseProviderStrategyContext joseProviderStrategyContext,
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
        this.joseProviderStrategy = joseProviderStrategyContext.getActiveProvider();
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest, HttpServletRequest request) {
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
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        String sid = UUID.randomUUID().toString();
        TokenPair tokenPair = joseProviderStrategy.generateTokenPair(authentication, user);

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

    @Override
    public UserAgentResponse getUserDeviceInformation(HttpServletRequest request) {
        return deviceInfoUtils.extractUserAgentInfo(request);
    }

    @Override
    public LoginResponse getLoginResponseFromOAuth2Login(UserDetails userDetails, HttpServletRequest request) {
        Cookie accessTokenCookie = WebUtils.getCookie(request, "accessToken");
        if (accessTokenCookie == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token cookie is missing");
        }
        String accessToken = accessTokenCookie.getValue();
        UserResponse userResponse = loadAuthenticatedUserInformation(userDetails.getUsername());
        AuthResponse authResponse = createAuthResponse(
                new TokenPair(accessToken, null),
                User.builder()
                        .email(userResponse.getEmail())
                        .role(Role.USER)
                        .build()
        );
        return LoginResponse.fromUserAndAuthResponse(userResponse, authResponse);
    }

    @Transactional
    @Override
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(EMAIL_ALREADY_EXISTS);
        }

        log.info("password: {}, {}", request.getPassword(), request.getConfirmPassword());
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AuthenticationException("Password and confirm password do not match. Please try again.");
        }

        User user = createUserFromRegisterRequest(request);
        User savedUser = userRepository.save(user);

        String verificationToken = emailVerifyTokenService.generateVerificationToken(savedUser);
        emailEventProducer.sendMagicLinkOnRegistration(savedUser.getEmail(), verificationToken);
    }

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

    @Override
    public void sendOtp(User user) {
        if (user.getPhoneNumber().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }
        String otp = otpService.generateOtpToken(user);
        emailEventProducer.sendOtp(user.getPhoneNumber(), otp);
    }

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

    private void logoutFromAllDevices(String email) {
        revokeAllTokensFromAllDevices(email);
    }

    @Transactional
    @Override
    public void logoutFromCurrentDevice(String accessToken) {
        revokeTokensFromCurrentDevice(accessToken);
    }

    @Transactional
    @Override
    public void logoutFromOneDevice(String accessToken, String targetSid) {
        revokeTokensFromSpecificDevice(accessToken, targetSid);
    }

    @Override
    public List<LoggedInDeviceResponse> getAllLoggedInDevices(String jwtToken, HttpServletRequest request) {
        String userAgentString = request.getHeader("User-Agent");
        UserAgent currentAgent = userAgentAnalyzer.parse(userAgentString);
        String currentDeviceName = currentAgent.getValue("DeviceName");
        String currentIp = IpAddressUtils.getClientIp(request);

        String email = joseProviderStrategy.getUsernameFromToken(jwtToken);

        List<DeviceInfo> allDevices = getAllLoggedInDevicesRaw(email);
        List<LoggedInDeviceResponse> result = new ArrayList<>();

        for (DeviceInfo device : allDevices) {
            boolean isCurrent = device.deviceName().equals(currentDeviceName) && device.ipAddress().equals(currentIp);
            result.add(new LoggedInDeviceResponse(device.deviceName(), device.ipAddress(), isCurrent));
        }

        return result;
    }

    private List<DeviceInfo> getAllLoggedInDevicesRaw(String email) {
        List<RefreshToken> tokens = refreshTokenService.findAllValidByEmail(email);
        return tokens.stream()
                .map(token -> new DeviceInfo(token.getDeviceName(), token.getDeviceIpAddress()))
                .distinct()
                .toList();
    }

    //need to check again
    @Transactional
    @Override
    public void updateUserTokensOnAbacChange(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        List<RefreshToken> refreshTokens = refreshTokenService.findAllValidByEmail(email);

        for (RefreshToken refreshToken : refreshTokens) {
            String oldSid = refreshToken.getSid();
            joseProviderStrategy.addTokenToABACBlacklist(oldSid);
            abacEventProducer.send(JoseConstraint.JWT_ABAC_BLACKLIST_PREFIX + oldSid);

            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(email, "", authorities);
            Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, "", authorities);

            String newSid = UUID.randomUUID().toString();
            //need to pass time to make sure new-issued token expired same with old token
            String newRefreshToken = joseProviderStrategy.generateRefreshToken(authentication, newSid, user);

            refreshToken.setToken(newRefreshToken);
            refreshToken.setSid(newSid);
            refreshToken.setJti(UUID.randomUUID().toString());
            refreshToken.setExpiryDate(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000));
            refreshToken.setRevoked(false);
            refreshTokenService.saveRefreshToken(newSid, newRefreshToken);
        }
    }

    private void revokeTokensFromCurrentDevice(String accessToken) {
        Map<String, Object> claims = joseProviderStrategy.getClaimsFromToken(accessToken);
        String sid = claims.get(JoseConstraint.JosePayload.SID).toString();
        refreshTokenService.deleteRefreshToken(sid);
        joseProviderStrategy.addTokenToLogoutBlacklist(sid);

        logoutEventProducer.send(JoseConstraint.JWT_LOGOUT_BLACKLIST_PREFIX + sid);
    }

    private void revokeTokensFromSpecificDevice(String accessToken, String targetSid) {
        Map<String, Object> claims = joseProviderStrategy.getClaimsFromToken(accessToken);
        String sid = claims.get(JoseConstraint.JosePayload.SID).toString();
        String email = claims.get(JoseConstraint.JosePayload.SUB).toString();

        if (Objects.equals(targetSid, sid)) {
            throw new IllegalArgumentException("Target SID cannot be the same with your current session.");
        }

        refreshTokenService.deleteByEmailAndSid(email, targetSid);
        joseProviderStrategy.addTokenToLogoutBlacklist(targetSid);
        logoutEventProducer.send(JoseConstraint.JWT_LOGOUT_BLACKLIST_PREFIX + targetSid);
    }

    /**
     * Potential for incorrect session revocation or security risks (e.g., revoking another user’s session).
     *
     * @param email account's email to revoke access and refresh tokens
     */
    private void revokeAllTokensFromAllDevices(String email) {
        List<String> allSessions = refreshTokenService.getAllSessionsByEmail(email);
        refreshTokenService.deleteAllRefreshTokenByEmail(email);
        for (String sid : allSessions) {
            joseProviderStrategy.addTokenToLogoutBlacklist(sid);
            logoutEventProducer.send(JoseConstraint.JWT_LOGOUT_BLACKLIST_PREFIX + sid);
        }
    }

    @Override
    public UserResponse loadAuthenticatedUserInformation(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(String.format(USER_NOT_FOUND_BY_EMAIL, email)));
        return createUserResponseFromUser(user);
    }

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

    @Override
    public void sendForgotPasswordEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found for email: " + email));
        String token = changePasswordTokenService.generateChangePasswordToken(user);
        emailEventProducer.sendMagicLinkOnForgotPassword(email, token);
    }

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

    @Override
    public void revokeAuthCookies(HttpServletResponse response) {
        Cookie revokeAccessTokenCookie = revokeCookie("accessToken");
        response.addCookie(revokeAccessTokenCookie);

        Cookie revokeJSESSIONID = revokeCookie("JSESSIONID");
        response.addCookie(revokeJSESSIONID);
    }

    private Cookie revokeCookie(String cookieName) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        return cookie;
    }

    private static UserResponse createUserResponseFromUser(User user) {
        return UserResponse.builder()
                .username(user.getDisplayedUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .build();
    }

    private static AuthResponse createAuthResponse(TokenPair tokenPair, User user) {
        return AuthResponse.builder()
                .accessToken(tokenPair.accessToken())
                .refreshToken(tokenPair.refreshToken())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

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

    private User createUserFromRegisterRequest(RegisterRequest request) {
        return User.builder()
                .email(request.getEmail())
                .displayedUsername(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .age(request.getAge())
                .region(request.getRegion())
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .isEnabled(false)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();
    }
}