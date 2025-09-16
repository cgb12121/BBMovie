package com.bbmovie.auth.service.nats;

import com.bbmovie.auth.exception.ChangedPasswordNotificationEventException;
import com.bbmovie.auth.exception.MagicLinkEventException;
import com.bbmovie.auth.exception.OtpEventException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.nats.client.Connection;
import io.nats.client.JetStream;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * This class serves as a Kafka producer to send various types of email-related and OTP-related events to Kafka topics.
 * It provides methods to handle user notifications such as sending OTPs, magic links,
 * and notifications related to changes in user credentials. Any failures during the event
 * production process are handled by throwing respective custom exceptions.
 */
@Log4j2
@Service
public class EmailEventProducer {

    private final JetStream jetStream;

    private static final String EMAIL_KEY = "email";
    private static final String PHONE_NUMBER_KEY = "password";
    private static final String OTP_KEY = "otp";
    private static final String TOKEN_FOR_MAGIC_LINK_KEY = "token";
    private static final String TIME_CHANGE_PASSWORD_KEY = "timeChangedPassword";

    @Autowired
    public EmailEventProducer(Connection nats) throws java.io.IOException {
        this.jetStream = nats.jetStream();
    }

    /**
     * Sends a one-time password (OTP) to the specified phone number via Kafka.
     *
     * @param phoneNumber the phone number to which the OTP should be sent
     * @param otp the one-time password to be sent
     * @throws OtpEventException if an error occurs while sending the OTP
     */
    public void sendOtp(String phoneNumber, String otp) {
        try {
            Map<String, String> event = Map.of(PHONE_NUMBER_KEY, phoneNumber, OTP_KEY, otp);
            byte[] data = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(event);
            jetStream.publish("auth.otp", data);
            log.info("Sent OTP for user: {}", phoneNumber);
        } catch (Exception e) {
            log.error("Failed to send OTP: {}", e.getMessage());
            throw new OtpEventException("OTP send failed, please try again.");
        }
    }

    /**
     * Sends a magic link during user registration to the specified email address via Kafka.
     *
     * @param email the email address to which the magic link should be sent
     * @param tokenToCreateLink the token used to generate the magic link
     * @throws MagicLinkEventException if an error occurs while sending the magic link
     */
    public void sendMagicLinkOnRegistration(String email, String tokenToCreateLink) {
        try {
            Map<String, String> event = Map.of(EMAIL_KEY, email, TOKEN_FOR_MAGIC_LINK_KEY, tokenToCreateLink);
            byte[] data = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(event);
            jetStream.publish("auth.registration", data);
            log.info("Sent magic link for user: {}", email);
        } catch (Exception e) {
            log.error("Failed to send magic link: {}", e.getMessage());
            throw new MagicLinkEventException("Magic link send failed, please try again.");
        }
    }

    /**
     * Sends a magic link to the specified email address via Kafka when a user initiates the forgot password process.
     *
     * @param email the email address to which the magic link should be sent
     * @param tokenToCreateLink the token used to generate the magic link
     * @throws MagicLinkEventException if an error occurs while sending the magic link
     */
    public void sendMagicLinkOnForgotPassword(String email, String tokenToCreateLink) {
        try {
            Map<String, String> event = Map.of(EMAIL_KEY, email, TOKEN_FOR_MAGIC_LINK_KEY, tokenToCreateLink);
            byte[] data = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(event);
            jetStream.publish("auth.forgot_password", data);
            log.info("Sent magic link for user on forgot password: {}", email);
        } catch (Exception e) {
            log.error("Failed to send magic link on forgot password: {}", e.getMessage());
            throw new MagicLinkEventException("Magic link send failed, please try again.");
        }
    }

    /**
     * Sends a notification to the specified email address via Kafka when a user's password has been changed.
     *
     * @param email the email address to which the notification should be sent
     * @param timeChangedPassword the timestamp indicating when the password was changed
     * @throws ChangedPasswordNotificationEventException if an error occurs while sending the notification
     */
    public void sendNotificationOnChangingPassword(String email, ZonedDateTime timeChangedPassword) {
        try {
            Map<String, String> event = Map.of(EMAIL_KEY, email, TIME_CHANGE_PASSWORD_KEY, timeChangedPassword.toString());
            byte[] data = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(event);
            jetStream.publish("auth.changed_password", data);
            log.info("Sent notification for user: {}", email);
        } catch (Exception e) {
            log.error("Failed to send notification for user: {}", e.getMessage());
            throw new ChangedPasswordNotificationEventException("Failed to send notification, please try again.");
        }
    }
}
