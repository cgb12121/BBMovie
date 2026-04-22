package com.bbmovie.notificationservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String NOTIFY_TRIGGERS = "notifications.trigger";
    public static final String NOTIFY_WEB = "notify.web";
    public static final String NOTIFY_PUSH = "notify.push";
    public static final String NOTIFY_EMAIL = "notify.email";
    public static final String EMAIL_NEWS_EVENTS = "notifications.email.news";

    @Bean
    public NewTopic triggersTopic() {
        return TopicBuilder.name(NOTIFY_TRIGGERS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic webTopic() {
        return TopicBuilder.name(NOTIFY_WEB).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic pushTopic() {
        return TopicBuilder.name(NOTIFY_PUSH).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic emailTopic() {
        return TopicBuilder.name(NOTIFY_EMAIL).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic emailNewsEventsTopic() {
        return TopicBuilder.name(EMAIL_NEWS_EVENTS).partitions(3).replicas(1).build();
    }
}
