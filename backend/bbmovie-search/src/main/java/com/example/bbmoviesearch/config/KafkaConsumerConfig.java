package com.example.bbmoviesearch.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public KafkaTemplate<Object, Object> kafkaTemplate() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
        )));
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplateForDLT() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                 Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
                )
        ));
    }


    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(new HashMap<>());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
                ConsumerConfig.GROUP_ID_CONFIG, "core-service",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
                JsonDeserializer.TRUSTED_PACKAGES, "*"
        )));
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
         factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplateForDLT) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplateForDLT);
        FixedBackOff backOff = new FixedBackOff(0L, 3L);
        return new DefaultErrorHandler(recoverer, backOff);
    }
}