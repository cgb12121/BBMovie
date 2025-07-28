package com.bbmovie.auth.service.kafka;

import com.bbmovie.auth.config.KafkaTopicConfig;
import com.bbmovie.auth.exception.ChangedPasswordNotificationEventException;
import com.bbmovie.auth.exception.MagicLinkEventException;
import com.bbmovie.auth.exception.OtpEventException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Map;

@Log4j2
@Service
public class EmailEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String EMAIL_KEY = "email";
    private static final String PHONE_NUMBER_KEY = "password";
    private static final String OTP_KEY = "otp";
    private static final String TOKEN_FOR_MAGIC_LINK_KEY = "token";
    private static final String TIME_CHANGE_PASSWORD_KEY = "timeChangedPassword";

    @Autowired
    public EmailEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOtp(String phoneNumber, String otp) {
        try {
            Map<String, String> event = Map.of(PHONE_NUMBER_KEY, phoneNumber, OTP_KEY, otp);
            kafkaTemplate.send(KafkaTopicConfig.OTP_SMS_TOPIC, phoneNumber, event);
            log.info("Sent OTP for user: {}", phoneNumber);
        } catch (Exception e) {
            log.error("Failed to send OTP: {}", e.getMessage());
            throw new OtpEventException("OTP send failed, please try again.");
        }
    }

    public void sendMagicLinkOnRegistration(String email, String tokenToCreateLink) {
        try {
            Map<String, String> event = Map.of(EMAIL_KEY, email, TOKEN_FOR_MAGIC_LINK_KEY, tokenToCreateLink);
            kafkaTemplate.send(KafkaTopicConfig.REGISTER_EMAIL_TOPIC, email, event);
            log.info("Sent magic link for user: {}", email);
        } catch (Exception e) {
            log.error("Failed to send magic link: {}", e.getMessage());
            throw new MagicLinkEventException("Magic link send failed, please try again.");
        }
    }

    public void sendMagicLinkOnForgotPassword(String email, String tokenToCreateLink) {
        try {
            Map<String, String> event = Map.of(EMAIL_KEY, email, TOKEN_FOR_MAGIC_LINK_KEY, tokenToCreateLink);
            kafkaTemplate.send(KafkaTopicConfig.FORGOT_PASSWORD_TOPIC, email, event);
            log.info("Sent magic link for user on forgot password: {}", email);
        } catch (Exception e) {
            log.error("Failed to send magic link on forgot password: {}", e.getMessage());
            throw new MagicLinkEventException("Magic link send failed, please try again.");
        }
    }

    public void sendNotificationOnChangingPassword(String email, ZonedDateTime timeChangedPassword) {
        try {
            Map<String, String> event = Map.of(EMAIL_KEY, email, TIME_CHANGE_PASSWORD_KEY, timeChangedPassword.toString());
            kafkaTemplate.send(KafkaTopicConfig.CHANGE_PASSWORD_EMAIL_TOPIC, email, event);
            log.info("Sent notification for user: {}", email);
        } catch (Exception e) {
            log.error("Failed to send notification for user: {}", e.getMessage());
            throw new ChangedPasswordNotificationEventException("Failed to send notification, please try again.");
        }
    }
}
