package com.bbmovie.auth.unit.service.auth.password;

import com.bbmovie.auth.dto.request.ChangePasswordRequest;
import com.bbmovie.auth.dto.request.ResetPasswordRequest;
import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.entity.enumerate.AuthProvider;
import com.bbmovie.auth.entity.enumerate.Role;
import com.bbmovie.auth.exception.AuthenticationException;
import com.bbmovie.auth.exception.InvalidPasswordException;
import com.bbmovie.auth.repository.UserRepository;
import com.bbmovie.auth.service.auth.password.PasswordServiceImpl;
import com.bbmovie.auth.service.auth.session.SessionService;
import com.bbmovie.auth.service.auth.verify.magiclink.ChangePasswordTokenService;
import com.bbmovie.auth.service.nats.EmailEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PasswordServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ChangePasswordTokenService changePasswordTokenService;

    @Mock
    private EmailEventProducer emailEventProducer;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SessionService sessionService;

    private PasswordServiceImpl passwordService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordServiceImpl(
                userRepository,
                passwordEncoder,
                emailEventProducer,
                authenticationManager,
                changePasswordTokenService,
                sessionService
        );
    }

    @Test
    void sendForgotPasswordEmail_WhenUserExists_ShouldGenerateAndSendToken() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .build();
        user.setId(UUID.randomUUID()); // Set ID separately since BaseEntity doesn't have builder method

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(changePasswordTokenService.generateChangePasswordToken(any(User.class))).thenReturn("reset-token");

        // When
        passwordService.sendForgotPasswordEmail("test@example.com");

        // Then
        verify(userRepository).findByEmail("test@example.com");
        verify(changePasswordTokenService).generateChangePasswordToken(user);
        verify(emailEventProducer).sendMagicLinkOnForgotPassword("test@example.com", "reset-token");
    }

    @Test
    void sendForgotPasswordEmail_WhenUserDoesNotExist_ShouldNotThrowException() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When - should not throw any exception
        assertDoesNotThrow(() -> {
            passwordService.sendForgotPasswordEmail("nonexistent@example.com");
        });

        // Then
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(changePasswordTokenService, never()).generateChangePasswordToken(any(User.class));
        verify(emailEventProducer, never()).sendMagicLinkOnForgotPassword(anyString(), anyString());
    }

    @Test
    void changePassword_WhenOldPasswordMatches_ShouldChangePassword() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .displayedUsername("testuser")
                .password("{bcrypt}oldEncodedPassword")
                .build();
        user.setId(UUID.randomUUID()); // Set ID separately since BaseEntity doesn't have builder method

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("oldPassword")
                .newPassword("newPassword123")
                .confirmNewPassword("newPassword123")
                .build();

        when(userRepository.findByDisplayedUsername("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "{bcrypt}oldEncodedPassword")).thenReturn(true);
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));

        // When
        passwordService.changePassword("test@example.com", request);

        // Then
        verify(userRepository).findByDisplayedUsername("test@example.com");
        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("oldPassword", "{bcrypt}oldEncodedPassword");
        verify(passwordEncoder).encode("newPassword123");
        verify(sessionService).logoutFromAllDevices("test@example.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertNotEquals("{bcrypt}oldEncodedPassword", updatedUser.getPassword());
    }

    @Test
    void changePassword_WhenOldPasswordDoesNotMatch_ShouldThrowAuthenticationException() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .displayedUsername("testuser")
                .password("{bcrypt}oldEncodedPassword")
                .build();
        user.setId(UUID.randomUUID()); // Set ID separately since BaseEntity doesn't have builder method

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("wrongOldPassword")
                .newPassword("newPassword123")
                .confirmNewPassword("newPassword123")
                .build();

        when(userRepository.findByDisplayedUsername("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongOldPassword", "{bcrypt}oldEncodedPassword")).thenReturn(false);

        // When & Then
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            passwordService.changePassword("test@example.com", request);
        });

        assertEquals("Current password is incorrect", exception.getMessage());
        verify(userRepository).findByDisplayedUsername("test@example.com");
        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("wrongOldPassword", "{bcrypt}oldEncodedPassword");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_WhenNewPasswordsDoNotMatch_ShouldThrowAuthenticationException() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .displayedUsername("testuser")
                .password("{bcrypt}encodedPassword")
                .build();
        user.setId(UUID.randomUUID()); // Set ID separately since BaseEntity doesn't have builder method

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("oldPassword")
                .newPassword("newPassword123")
                .confirmNewPassword("differentNewPassword") // Different from new password
                .build();

        when(userRepository.findByDisplayedUsername("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "{bcrypt}encodedPassword")).thenReturn(true);
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));

        // When & Then
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            passwordService.changePassword("test@example.com", request);
        });

        assertEquals("New password and confirm password do not match. Please try again.", exception.getMessage());
    }

    @Test
    void resetPassword_WhenTokenIsValidAndPasswordsMatch_ShouldResetPassword() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .password("{bcrypt}oldPassword")
                .build();
        user.setId(UUID.randomUUID()); // Set ID separately since BaseEntity doesn't have builder method

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .newPassword("newPassword123")
                .confirmNewPassword("newPassword123")
                .build();

        when(changePasswordTokenService.getEmailForToken(anyString())).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword123")).thenReturn("{bcrypt}newEncodedPassword");

        // When
        passwordService.resetPassword("valid-token", request);

        // Then
        verify(changePasswordTokenService).getEmailForToken("valid-token");
        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).encode("newPassword123");
        verify(changePasswordTokenService).deleteToken("valid-token");
        verify(sessionService).logoutFromAllDevices("test@example.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertEquals("{bcrypt}newEncodedPassword", updatedUser.getPassword());
    }

    @Test
    void resetPassword_WhenTokenIsInvalid_ShouldThrowAuthenticationException() {
        // Given
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .newPassword("newPassword123")
                .confirmNewPassword("newPassword123")
                .build();

        when(changePasswordTokenService.getEmailForToken("invalid-token")).thenReturn(null);

        // When & Then
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            passwordService.resetPassword("invalid-token", request);
        });

        assertEquals("Invalid or expired token. Please try again.", exception.getMessage());
        verify(userRepository, never()).findByEmail(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_WhenNewPasswordsDoNotMatch_ShouldThrowAuthenticationException() {
        // Given
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .newPassword("newPassword123")
                .confirmNewPassword("differentNewPassword") // Different from new password
                .build();

        when(changePasswordTokenService.getEmailForToken("valid-token")).thenReturn("test@example.com");
        User user = User.builder()
                .email("test@example.com")
                .password("{bcrypt}oldPassword")
                .build();
        user.setId(UUID.randomUUID()); // Set ID separately since BaseEntity doesn't have builder method
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // When & Then
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            passwordService.resetPassword("valid-token", request);
        });

        assertEquals("New password and confirm password do not match. Please try again.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class)); // Should not save if passwords don't match
    }
}