package com.bbmovie.auth.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String REGISTER_EMAIL_TOPIC = "register-email-topic";
    public static final String CHANGE_PASSWORD_EMAIL_TOPIC = "change-password-email-topic";
    public static final String OTP_SMS_TOPIC = "otp-sms-topic";
    public static final String FORGOT_PASSWORD_TOPIC = "forgot-password-topic";

    public static final String LOGOUT_TOPIC = "logout-topic";
    public static final String ABAC_TOPIC = "abac-topic";

    @Bean("logoutTopic")
    public NewTopic logoutTopic() {
        return TopicBuilder.name(LOGOUT_TOPIC).build();
    }

    @Bean("abacTopic")
    public NewTopic abacTopic() {
        return TopicBuilder.name(ABAC_TOPIC).build();
    }

    @Bean("registerEmailTopic")
    public NewTopic registerEmailTopic() {
        return buildShortLivedTopic(REGISTER_EMAIL_TOPIC);
    }

    @Bean("changePasswordEmailTopic")
    public NewTopic changePasswordEmailTopic() {
        return buildShortLivedTopic(CHANGE_PASSWORD_EMAIL_TOPIC);
    }

    @Bean("otpSmsTopic")
    public NewTopic otpSmsTopic() {
        return buildShortLivedTopic(OTP_SMS_TOPIC);
    }

    @Bean("forgotPasswordEmail")
    public NewTopic forgotPasswordEmailTopic() {
        return buildShortLivedTopic(FORGOT_PASSWORD_TOPIC);
    }

    private NewTopic buildShortLivedTopic(String name) {
        return TopicBuilder.name(name)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "1000")
                .config("delete.retention.ms", "1000")
                .build();
    }
}