package com.example.bbmovie.config;

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
import org.springframework.kafka.core.KafkaTemplate; // Still needed for DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
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

    // You need a KafkaTemplate for the DeadLetterPublishingRecoverer
    // This KafkaTemplate doesn't necessarily need to be used for publishing in this service,
    // but the Recoverer needs it to send to the DLT.
    // If this service truly *only* listens, and you don't want to define a full producer
    // setup here, you can define a minimal KafkaTemplate just for the DLT.
    // However, it's often simpler to let Spring Boot auto-configure a default one if
    // producer properties are present (even if not explicitly used for direct sending).
    // Or, you can explicitly define a producer factory and template for DLT only.
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplateForDLT() {
        // This is a simplified KafkaTemplate for DLT. In a real app, you might have
        // more specific producer configurations, but for the DLT it just needs to send bytes.
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                // Minimal producer properties for DLT:
                // This assumes your DLT Kafka cluster is the same as your consumer's
                // If not, you'd need separate bootstrap servers etc.
                // Spring Boot's auto-configuration will often provide these from application.yml
                // under spring.kafka.producer.*
                 Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
                )
        ));
    }


    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        // Spring Boot will auto-configure a DefaultKafkaConsumerFactory based on
        // spring.kafka.consumer properties in application.yml
        return new DefaultKafkaConsumerFactory<>(new HashMap<>()); // Empty map, Spring Boot fills it
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
        factory.setCommonErrorHandler(errorHandler); // Apply the error handler here
        // If you need manual acknowledgement:
        // factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplateForDLT) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplateForDLT);
        FixedBackOff backOff = new FixedBackOff(0L, 3L); // 3 retries after initial attempt
        return new DefaultErrorHandler(recoverer, backOff);
    }
}