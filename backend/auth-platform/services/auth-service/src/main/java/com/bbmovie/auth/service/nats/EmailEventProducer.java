package com.bbmovie.auth.service.nats;

import com.bbmovie.auth.exception.ChangedPasswordNotificationEventException;
import com.bbmovie.auth.exception.MagicLinkEventException;
import com.bbmovie.auth.exception.OtpEventException;
import io.nats.client.JetStream;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Map;

@Log4j2
@Service
public class EmailEventProducer extends AbstractNatsJetStreamService {

    private static final String EMAIL_KEY = "email";
    private static final String PHONE_NUMBER_KEY = "phoneNumber"; // Corrected key
    private static final String OTP_KEY = "otp";
    private static final String TOKEN_FOR_MAGIC_LINK_KEY = "token";
    private static final String TIME_CHANGE_PASSWORD_KEY = "timeChangedPassword";

    public void sendOtp(String phoneNumber, String otp) {
        Map<String, String> event = Map.of(PHONE_NUMBER_KEY, phoneNumber, OTP_KEY, otp);
        publish("auth.otp", event, new OtpEventException("OTP send failed, please try again."));
    }

    public void sendMagicLinkOnRegistration(String email, String tokenToCreateLink) {
        Map<String, String> event = Map.of(EMAIL_KEY, email, TOKEN_FOR_MAGIC_LINK_KEY, tokenToCreateLink);
        publish("auth.registration", event, new MagicLinkEventException("Magic link send failed, please try again."));
    }

    public void sendMagicLinkOnForgotPassword(String email, String tokenToCreateLink) {
        Map<String, String> event = Map.of(EMAIL_KEY, email, TOKEN_FOR_MAGIC_LINK_KEY, tokenToCreateLink);
        publish("auth.forgot_password", event, new MagicLinkEventException("Magic link send failed, please try again."));
    }

    public void sendNotificationOnChangingPassword(String email, ZonedDateTime timeChangedPassword) {
        Map<String, String> event = Map.of(EMAIL_KEY, email, TIME_CHANGE_PASSWORD_KEY, timeChangedPassword.toString());
        publish("auth.changed_password", event, new ChangedPasswordNotificationEventException("Failed to send notification, please try again."));
    }

    private void publish(String subject, Map<String, String> event, RuntimeException exception) {
        JetStream jetStream = getJetStream();
        if (jetStream == null) {
            log.warn("NATS JetStream not available. Skipping event publication to subject: {}", subject);
            // Don't throw exception, just log and continue - email delivery should not break registration
            return;
        }
        try {
            byte[] data = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(event);
            jetStream.publish(subject, data);
            log.info("Sent event to subject: {} for user: {}", subject, event.get(EMAIL_KEY) != null ? event.get(EMAIL_KEY) : event.get(PHONE_NUMBER_KEY));
        } catch (Exception e) {
            log.error("Failed to send event to subject {}: {}", subject, e.getMessage());
            // Don't throw exception, just log and continue - email delivery should not break registration
            // The user registration should still succeed even if email notification fails
        }
    }
}
