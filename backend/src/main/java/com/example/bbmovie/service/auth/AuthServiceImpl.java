package com.example.bbmovie.service.auth;

import com.example.bbmovie.dto.request.LoginRequest;
import com.example.bbmovie.dto.request.ChangePasswordRequest;
import com.example.bbmovie.dto.request.RegisterRequest;
import com.example.bbmovie.dto.request.ResetPasswordRequest;
import com.example.bbmovie.dto.response.AuthResponse;
import com.example.bbmovie.dto.response.LoginResponse;
import com.example.bbmovie.dto.response.UserAgentResponse;
import com.example.bbmovie.dto.response.UserResponse;
import com.example.bbmovie.entity.enumerate.AuthProvider;
import com.example.bbmovie.entity.enumerate.Role;
import com.example.bbmovie.exception.*;
import com.example.bbmovie.entity.User;
import com.example.bbmovie.exception.TokenVerificationException;
import com.example.bbmovie.repository.UserRepository;
import com.example.bbmovie.security.jwt.asymmetric.JwtTokenPairedKeyProvider;
import com.example.bbmovie.service.auth.verify.otp.OtpService;
import com.example.bbmovie.service.auth.verify.token.ChangePasswordTokenService;
import com.example.bbmovie.service.auth.verify.token.EmailVerifyTokenService;
import com.example.bbmovie.service.email.EmailService;
import com.example.bbmovie.utils.IpAddressUtils;
import com.example.bbmovie.utils.UserAgentAnalyzerUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import nl.basjes.parse.useragent.UserAgent;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.WebUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Log4j2
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenPairedKeyProvider jwtTokenPairedKeyProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final EmailVerifyTokenService emailVerifyTokenService;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final ChangePasswordTokenService changePasswordTokenService;
    private final OtpService otpService;
    private final UserAgentAnalyzerUtils userAgentAnalyzer;

    @Override
    public LoginResponse login(LoginRequest loginRequest, HttpServletRequest request) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Invalid username/email or password"));

        String userAgentString = request.getHeader("User-Agent");
        UserAgent agent = userAgentAnalyzer.parse(userAgentString);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        boolean isUserEnabled = user.isEnabled();
        if (!isUserEnabled) {
            throw new AccountNotEnabledException("Account is not enabled. Please verify your email first.");
        }

        boolean correctPassword = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());
        if (!correctPassword) {
            throw new InvalidCredentialsException("Invalid username/email or password");
        }

        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        String deviceName = agent.getValue("DeviceName");
        String deviceIpAddress = IpAddressUtils.getClientIp(request);
        String deviceOsName = agent.getValue("OperatingSystemName");
        String browser = agent.getValue("AgentName");
        String browserVersion = agent.getValue("AgentVersion");

        String accessToken = jwtTokenPairedKeyProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenPairedKeyProvider.generateRefreshToken(authentication);

        refreshTokenService.saveRefreshToken(
                refreshToken, user.getEmail(), deviceIpAddress, deviceName, deviceOsName, browser, browserVersion
        );

        jwtTokenPairedKeyProvider.removeJwtBlockAccessTokenOfEmailAndDevice(user.getEmail(), deviceName);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .role(user.getRole())
                .build();
        UserResponse userResponse = UserResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .build();
        UserAgentResponse userAgentResponse = UserAgentResponse.builder()
                .deviceName(deviceName)
                .deviceIpAddress(deviceIpAddress)
                .deviceOs(deviceOsName)
                .browser(browser)
                .browserVersion(browserVersion)
                .build();

        return LoginResponse.fromUserAndAuthAndUserAgentResponse(
                userResponse, authResponse, userAgentResponse
        );
    }

    @Override
    public LoginResponse getLoginResponseFromOAuth2Login(UserDetails userDetails, HttpServletRequest request) {
        String userAgentString = request.getHeader("User-Agent");
        UserAgent agent = userAgentAnalyzer.parse(userAgentString);

        String deviceName = agent.getValue("DeviceName");
        String deviceIpAddress = IpAddressUtils.getClientIp(request);
        String deviceOs = agent.getValue("OperatingSystemName");
        String browser = agent.getValue("AgentName");
        String browserVersion = agent.getValue("AgentVersion");

        UserAgentResponse userAgentResponse = UserAgentResponse.builder()
                .deviceName(deviceName)
                .deviceIpAddress(deviceIpAddress)
                .deviceOs(deviceOs)
                .browser(browser)
                .browserVersion(browserVersion)
                .build();

        Cookie accessTokenCookie = WebUtils.getCookie(request, "accessToken");
        if (accessTokenCookie == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token cookie is missing");
        }

        UserResponse userResponse = loadAuthenticatedUserInformation(userDetails.getUsername());

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessTokenCookie.getValue())
                .refreshToken(null)
                .email(userResponse.getEmail())
                .role(Role.USER)
                .build();

        return LoginResponse.fromUserAndAuthAndUserAgentResponse(
                userResponse, authResponse, userAgentResponse
        );
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        User user = registerUser(request);
        return AuthResponse.builder()
                .email(user.getEmail())
                .build();
    }

    private User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(Role.USER);
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setIsEnabled(false);

        User savedUser = userRepository.save(user);

        String verificationToken = emailVerifyTokenService.generateVerificationToken(savedUser);

        CompletableFuture.runAsync(() -> {
            try {
                emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);
            } catch (Exception ex) {
                log.error("Async email sending failed: {}", ex.getMessage());
            }
        });

        return savedUser;
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
            return "Account verification failed. Please try again.";
        }
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found for email: " + email));
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
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found for email: " + email));

        if (user.isEnabled()) {
            throw new EmailAlreadyVerifiedException("Email is already verified");
        }
        try {
            String token = emailVerifyTokenService.generateVerificationToken(user);
            emailService.sendVerificationEmail(email, token);
        } catch (Exception e) {
            throw new TokenVerificationException("Failed to send verification email: " + e.getMessage());
        }
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
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found for email: " + email));
        if (user.isEnabled()) {
            log.info("Account {} already verified", email);
            return;
        }
        user.setIsEnabled(true);
        userRepository.save(user);
        otpService.deleteOtpToken(otp);
    }

    @Override
    public void sendOtp(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found for email: " + email));

        if (user.isEnabled()) {
            throw new EmailAlreadyVerifiedException("Email is already verified");
        }
        try {
            String token = emailVerifyTokenService.generateVerificationToken(user);
            emailService.sendVerificationEmail(email, token);
        } catch (Exception e) {
            throw new TokenVerificationException("Failed to send verification email: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void logoutFromAllDevices(String email) {
        revokeAllTokensFromAllDevicesByEmail(email);
    }

    @Override
    @Transactional
    public void logoutFromCurrentDevice(String accessToken, String deviceName) {
        revokeAccessTokenAndRefreshTokenFromCurrentDevice(accessToken, deviceName);
    }

    @Override
    @Transactional
    public void logoutFromOneDevice(String username, String deviceName) {
        revokeAccessTokenAndRefreshTokenFromOneDevice(username, deviceName);
    }

    @Override
    public List<String> getAllLoggedInDevices(String email, HttpServletRequest request) {
        String userAgentString = request.getHeader("User-Agent");
        UserAgent agent = userAgentAnalyzer.parse(userAgentString);
        String deviceName = agent.getValue("DeviceName");

        List<String> allLoggedInDevices = getAllLoggedInDevices(email);
        for (int i = 0; i < allLoggedInDevices.size(); i++) {
            String device = allLoggedInDevices.get(i);
            if (device.equals(deviceName)) {
                String highLightCurrentDevice = "(Current device) " + device;
                allLoggedInDevices.set(i, highLightCurrentDevice);
                break;
            }
        }
        return allLoggedInDevices;
    }

    private void revokeAccessTokenAndRefreshTokenFromCurrentDevice(String accessToken, String deviceName) {
        String email = jwtTokenPairedKeyProvider.getUsernameFromToken(accessToken);
        refreshTokenService.deleteByEmailAndDeviceName(email, deviceName);
        jwtTokenPairedKeyProvider.invalidateAccessTokenByEmailAndDevice(email, deviceName);
    }

    private void revokeAccessTokenAndRefreshTokenFromOneDevice(String email, String deviceName) {
        refreshTokenService.deleteByEmailAndDeviceName(email, deviceName);
        jwtTokenPairedKeyProvider.invalidateAccessTokenByEmailAndDevice(email, deviceName);
    }

    private void revokeAllTokensFromAllDevicesByEmail(String email) {
        List<String> allDevicesName = getAllLoggedInDevices(email);
        refreshTokenService.deleteAllRefreshTokenByEmail(email);
        for (String device : allDevicesName) {
            jwtTokenPairedKeyProvider.invalidateAccessTokenByEmailAndDevice(email, device);
        }
    }

    private List<String> getAllLoggedInDevices(String email) {
        return refreshTokenService.getAllDeviceNameByEmail(email);
    }

    @Override
    public UserResponse loadAuthenticatedUserInformation(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found for email: " + email));
        return UserResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .build();
    }

    @Override
    @Transactional(noRollbackFor = CustomEmailException.class)
    public void changePassword(String requestEmail, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(requestEmail)
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

        emailService.notifyChangedPassword(user.getEmail());

        revokeAllTokensFromAllDevicesByEmail(user.getEmail());
    }

    @Override
    public void sendForgotPasswordEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found for email: " + email));
        String token = changePasswordTokenService.generateChangePasswordToken(user);
        emailService.sendForgotPasswordEmail(email, token);
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
        emailService.notifyChangedPassword(user.getEmail());
        log.info("Password reset for user {} successful", email);

        revokeAllTokensFromAllDevicesByEmail(email);
    }

    @Override
    public void revokeCookies(HttpServletResponse response) {
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
}