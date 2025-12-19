package com.bbmovie.auth.unit.service.auth.session;

import com.bbmovie.auth.dto.request.LoginRequest;
import com.bbmovie.auth.dto.response.UserAgentResponse;
import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.entity.enumerate.AuthProvider;
import com.bbmovie.auth.entity.enumerate.Role;
import com.bbmovie.auth.exception.AccountNotEnabledException;
import com.bbmovie.auth.exception.BadLoginException;
import com.bbmovie.auth.repository.UserRepository;
import com.bbmovie.auth.security.jose.provider.JoseProvider;
import com.bbmovie.auth.security.jose.dto.TokenPair;
import com.bbmovie.auth.service.auth.RefreshTokenService;
import com.bbmovie.auth.service.auth.session.SessionServiceImpl;
import com.bbmovie.auth.service.nats.ABACEventProducer;
import com.bbmovie.auth.service.nats.LogoutEventProducer;
import com.bbmovie.auth.utils.DeviceInfoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private JoseProvider joseProvider;

    @Mock
    private LogoutEventProducer logoutEventProducer;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private DeviceInfoUtils deviceInfoUtils;

    @Mock
    private ABACEventProducer abacEventProducer;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private SessionServiceImpl sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionServiceImpl(
                joseProvider,
                logoutEventProducer,
                null, // UserAgentAnalyzerUtils - mock this or create separately
                refreshTokenService,
                userRepository,
                passwordEncoder,
                authenticationManager,
                deviceInfoUtils,
                abacEventProducer
        );
    }

    @Test
    void login_WhenUserNotFound_ShouldThrowBadLoginException() {
        // Given
        LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "password123");
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        BadLoginException exception = assertThrows(BadLoginException.class, () -> {
            sessionService.login(loginRequest, request);
        });

        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_WhenPasswordDoesNotMatch_ShouldThrowBadLoginException() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .password("{bcrypt}encodedPassword")
                .isEnabled(true)
                .build();
        user.setId(UUID.randomUUID()); // Set ID separately since BaseEntity doesn't have builder method

        LoginRequest loginRequest = new LoginRequest("test@example.com", "wrongPassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "{bcrypt}encodedPassword")).thenReturn(false);

        // When & Then
        BadLoginException exception = assertThrows(BadLoginException.class, () -> {
            sessionService.login(loginRequest, request);
        });

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("wrongPassword", "{bcrypt}encodedPassword");
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_WhenAccountIsNotEnabled_ShouldThrowAccountNotEnabledException() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .password("{bcrypt}encodedPassword")
                .isEnabled(false) // Not enabled
                .build();
        user.setId(UUID.randomUUID()); // Set ID separately since BaseEntity doesn't have builder method

        LoginRequest loginRequest = new LoginRequest("test@example.com", "correctPassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correctPassword", "{bcrypt}encodedPassword")).thenReturn(true);

        // When & Then
        AccountNotEnabledException exception = assertThrows(AccountNotEnabledException.class, () -> {
            sessionService.login(loginRequest, request);
        });

        assertEquals("Account is not enabled. Please verify your email first.", exception.getMessage());
        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("correctPassword", "{bcrypt}encodedPassword");
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_WhenValidCredentials_ShouldReturnLoginResponse() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .password("{bcrypt}encodedPassword")
                .role(Role.USER)
                .isEnabled(true)
                .build();
        user.setId(UUID.randomUUID()); // Set ID separately since BaseEntity doesn't have builder method

        LoginRequest loginRequest = new LoginRequest("test@example.com", "correctPassword");

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user,
                "correctPassword",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        TokenPair tokenPair = new TokenPair("accessToken", "refreshToken");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correctPassword", "{bcrypt}encodedPassword")).thenReturn(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(joseProvider.generateTokenPair(any(Authentication.class), any(User.class))).thenReturn(tokenPair);
        when(deviceInfoUtils.extractUserAgentInfo(any(HttpServletRequest.class)))
            .thenReturn(new UserAgentResponse("192.168.1.1", "Chrome", "Windows", "Windows 10", "95.0"));

        // When
        var result = sessionService.login(loginRequest, request);

        // Then
        assertNotNull(result);
        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("correctPassword", "{bcrypt}encodedPassword");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(joseProvider).generateTokenPair(any(Authentication.class), eq(user));
        verify(deviceInfoUtils).extractUserAgentInfo(any(HttpServletRequest.class));

        // Verify that user's last login time was updated
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertNotNull(updatedUser.getLastLoginTime());
    }

    @Test
    void logoutFromCurrentDevice_ShouldRevokeTokens() {
        // Given
        String accessToken = "valid.access.token";

        // Mock the token parsing
        when(joseProvider.getClaimsFromToken("valid.access.token")).thenReturn(
                java.util.Map.of("sid", "session-id", "sub", "test@example.com")
        );

        // Mock the device info response
        when(deviceInfoUtils.extractUserAgentInfo(any(HttpServletRequest.class)))
            .thenReturn(new UserAgentResponse("192.168.1.1", "Chrome", "Windows", "Windows 10", "95.0"));

        // When
        sessionService.logoutFromCurrentDevice(accessToken);

        // Then
        verify(refreshTokenService).deleteRefreshToken("session-id");
        verify(joseProvider).addTokenToLogoutBlacklist("session-id");
        verify(logoutEventProducer).send("logout-blacklist:session-id");
    }

    @Test
    void loadAuthenticatedUserInformation_WhenUserExists_ShouldReturnUserInfo() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .displayedUsername("testuser")
                .firstName("John")
                .lastName("Doe")
                .role(Role.USER)
                .build();
        user.setId(UUID.randomUUID()); // Set ID separately since BaseEntity doesn't have builder method

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // When
        var result = sessionService.loadAuthenticatedUserInformation("test@example.com");

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        verify(userRepository).findByEmail("test@example.com");
    }
}