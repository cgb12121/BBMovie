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
public class KafkaEmailEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public KafkaEmailEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    public void sendOtp(String phoneNumber, String otp) {
        try {
            Map<String, String> event = Map.of("phoneNumber", phoneNumber, "otp", otp);
            kafkaTemplate.send(KafkaTopicConfig.OTP_SMS_TOPIC, phoneNumber, event);
            log.info("Sent OTP for user: {}", phoneNumber);
        } catch (Exception e) {
            log.error("Failed to send OTP: {}", e.getMessage());
            throw new OtpEventException("OTP send failed, please try again.");
        }
    }

    public void sendMagicLinkOnRegistration(String email, String tokenToCreateLink) {
        try {
            Map<String, String> event = Map.of("email", email, "token", tokenToCreateLink);
            kafkaTemplate.send(KafkaTopicConfig.REGISTER_EMAIL_TOPIC, email, event);
            log.info("Sent magic link for user: {}", email);
        } catch (Exception e) {
            log.error("Failed to send magic link: {}", e.getMessage());
            throw new MagicLinkEventException("Magic link send failed, please try again.");
        }
    }

    public void sendMagicLinkOnForgotPassword(String email, String tokenToCreateLink) {
        try {
            Map<String, String> event = Map.of("email", email, "token", tokenToCreateLink);
            kafkaTemplate.send(KafkaTopicConfig.FORGOT_PASSWORD_TOPIC, email, event);
            log.info("Sent magic link for user: {}", email);
        } catch (Exception e) {
            log.error("Failed to send magic link: {}", e.getMessage());
            throw new MagicLinkEventException("Magic link send failed, please try again.");
        }
    }
    
    public void sendNotificationOnChangingPassword(String email, ZonedDateTime timeChangedPassword) {
        try {
            Map<String, String> event = Map.of("email", email, "timeChangedPassword", timeChangedPassword.toString());
            kafkaTemplate.send(KafkaTopicConfig.REGISTER_EMAIL_TOPIC, email, event);
            log.info("Sent notification for user: {}", email);
        } catch (Exception e) {
            log.error("Failed to send notification for user: {}", e.getMessage());
            throw new ChangedPasswordNotificationEventException("Failed to send notification, please try again.");
        }
    }
}
