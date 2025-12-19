package com.bbmovie.auth.unit.service.auth.registration;

import com.bbmovie.auth.dto.request.RegisterRequest;
import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.entity.enumerate.AuthProvider;
import com.bbmovie.auth.entity.enumerate.Role;
import com.bbmovie.auth.exception.AuthenticationException;
import com.bbmovie.auth.exception.EmailAlreadyExistsException;
import com.bbmovie.auth.exception.TokenVerificationException;
import com.bbmovie.auth.exception.UserNotFoundException;
import com.bbmovie.auth.repository.UserRepository;
import com.bbmovie.auth.service.auth.registration.RegistrationServiceImpl;
import com.bbmovie.auth.service.auth.verify.magiclink.EmailVerifyTokenService;
import com.bbmovie.auth.service.nats.EmailEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerifyTokenService emailVerifyTokenService;

    @Mock
    private EmailEventProducer emailEventProducer;

    private RegistrationServiceImpl registrationService;

    private RegisterRequest validRegisterRequest;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationServiceImpl(userRepository, emailVerifyTokenService, emailEventProducer, null);
        validRegisterRequest = RegisterRequest.builder()
                .email("test@example.com")
                .username("testuser")
                .password("password123")
                .confirmPassword("password123")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    @Test
    void register_WhenEmailAlreadyExists_ShouldThrowEmailAlreadyExistsException() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When & Then
        EmailAlreadyExistsException exception = assertThrows(EmailAlreadyExistsException.class, () -> {
            registrationService.register(validRegisterRequest);
        });

        assertEquals("This email already used, please try another one.", exception.getMessage());
        verify(userRepository, never()).save(any());
        verify(emailVerifyTokenService, never()).generateVerificationToken(any());
        verify(emailEventProducer, never()).sendMagicLinkOnRegistration(anyString(), anyString());
    }

    @Test
    void register_WhenPasswordsDoNotMatch_ShouldThrowAuthenticationException() {
        // Given
        RegisterRequest requestWithMismatchedPasswords = RegisterRequest.builder()
                .email("test@example.com")
                .username("testuser")
                .password("password123")
                .confirmPassword("differentPassword")
                .firstName("John")
                .lastName("Doe")
                .build();

        // When & Then
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            registrationService.register(requestWithMismatchedPasswords);
        });

        assertEquals("Password and confirm password does not match. Please try again.", exception.getMessage());
        verify(userRepository, never()).save(any());
        verify(emailVerifyTokenService, never()).generateVerificationToken(any());
        verify(emailEventProducer, never()).sendMagicLinkOnRegistration(anyString(), anyString());
    }

    @Test
    void register_WhenValidRequest_ShouldSaveUserAndSendVerificationEmail() {
        // Given
        User savedUser = User.builder()
                .email("test@example.com")
                .displayedUsername("testuser")
                .password("{bcrypt}encodedPassword")
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .isEnabled(false)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();
        savedUser.setId(UUID.randomUUID()); // Set ID separately since BaseEntity doesn't have builder method

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(emailVerifyTokenService.generateVerificationToken(any(User.class))).thenReturn("verification-token");

        // When
        registrationService.register(validRegisterRequest);

        // Then
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailVerifyTokenService, times(1)).generateVerificationToken(any(User.class));
        verify(emailEventProducer, times(1)).sendMagicLinkOnRegistration(eq("test@example.com"), eq("verification-token"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        assertEquals("test@example.com", capturedUser.getEmail());
        assertEquals("testuser", capturedUser.getDisplayedUsername());
        assertEquals(Role.USER, capturedUser.getRole());
        assertFalse(capturedUser.isEnabled());
    }

    @Test
    void verifyAccountByEmail_WhenTokenIsNull_ShouldThrowTokenVerificationException() {
        // When & Then
        TokenVerificationException exception = assertThrows(TokenVerificationException.class, () -> {
            registrationService.verifyAccountByEmail(null);
        });

        assertEquals("Verification token cannot be null or empty", exception.getMessage());
    }

    @Test
    void verifyAccountByEmail_WhenTokenIsEmpty_ShouldThrowTokenVerificationException() {
        // When & Then
        TokenVerificationException exception = assertThrows(TokenVerificationException.class, () -> {
            registrationService.verifyAccountByEmail("");
        });

        assertEquals("Verification token cannot be null or empty", exception.getMessage());
    }

    @Test
    void sendVerificationEmail_WhenEmailExistsAndNotVerified_ShouldSendEmail() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .isEnabled(false)
                .build();
        user.setId(UUID.randomUUID());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(emailVerifyTokenService.generateVerificationToken(any(User.class))).thenReturn("new-token");

        // When
        registrationService.sendVerificationEmail("test@example.com");

        // Then
        verify(emailVerifyTokenService).generateVerificationToken(user);
        verify(emailEventProducer).sendMagicLinkOnRegistration("test@example.com", "new-token");
    }

    @Test
    void sendVerificationEmail_WhenEmailDoesNotExist_ShouldThrowUserNotFoundException() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            registrationService.sendVerificationEmail("nonexistent@example.com");
        });

        assertTrue(exception.getMessage().contains("nonexistent@example.com"));
    }
}