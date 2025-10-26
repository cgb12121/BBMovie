package com.bbmovie.auth.service.auth.password;

import com.bbmovie.auth.dto.request.ChangePasswordRequest;
import com.bbmovie.auth.dto.request.ResetPasswordRequest;
import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.exception.AuthenticationException;
import com.bbmovie.auth.exception.CustomEmailException;
import com.bbmovie.auth.exception.UserNotFoundException;
import com.bbmovie.auth.repository.UserRepository;
import com.bbmovie.auth.service.auth.session.SessionService;
import com.bbmovie.auth.service.auth.verify.magiclink.ChangePasswordTokenService;
import com.bbmovie.auth.service.nats.EmailEventProducer;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Log4j2
@Service
public class PasswordServiceImpl implements PasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailEventProducer emailEventProducer;
    private final AuthenticationManager authenticationManager;
    private final ChangePasswordTokenService changePasswordTokenService;
    private final SessionService sessionService;

    @Autowired
    public PasswordServiceImpl(
            UserRepository userRepository, PasswordEncoder passwordEncoder, EmailEventProducer emailEventProducer,
            AuthenticationManager authenticationManager, ChangePasswordTokenService changePasswordTokenService,
            SessionService sessionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailEventProducer = emailEventProducer;
        this.authenticationManager = authenticationManager;
        this.changePasswordTokenService = changePasswordTokenService;
        this.sessionService = sessionService;
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
    @Transactional(noRollbackFor = CustomEmailException.class)
    @Override
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

        sessionService.logoutFromAllDevices(user.getEmail());
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

        sessionService.logoutFromAllDevices(email);
    }
}
