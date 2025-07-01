package com.example.bbmovie.config;

import com.example.common.dtos.kafka.VideoMetadata;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {

    // --- Producer Configuration ---

    /**
     * Defines the ProducerFactory for producing VideoMetadata messages.
     * This factory configures how Kafka Producers are created.
     *
     * @return The configured ProducerFactory.
     */
    @Bean
    public ProducerFactory<String, VideoMetadata> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        // Replace with your actual Kafka broker address(es)
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Ensure all messages are acknowledged by the Kafka leader and followers
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        // Number of times to retry sending a message if it fails
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Defines the KafkaTemplate for sending VideoMetadata messages.
     * This is the primary interface for producing messages to Kafka topics.
     *
     * @return The configured KafkaTemplate.
     */
    @Bean("videoMetadataKafkaTemplate") // Renamed for clarity and to avoid conflicts
    public KafkaTemplate<String, VideoMetadata> videoMetadataKafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // --- Consumer Configuration ---

    /**
     * Defines a KafkaTemplate specifically for the Dead Letter Topic (DLT)
     * publishing recoverer. This ensures that failed messages can be sent
     * to a DLT for later inspection or reprocessing.
     *
     * @return The KafkaTemplate for DLT.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplateForDLT() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
            Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                // Using JsonSerializer for the DLT messages as well
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
            )
        ));
    }

    /**
     * Defines the ConsumerFactory for consuming VideoMetadata messages.
     * This factory configures how Kafka Consumers are created for your listeners.
     * IMPORTANT: All necessary properties (bootstrap servers, deserializers, group ID)
     * must be set here if you manually define this bean.
     *
     * @return The configured ConsumerFactory.
     */
    @Bean
    public ConsumerFactory<String, VideoMetadata> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        // Replace with your actual Kafka broker address(es)
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        // Define a unique group ID for your consumer application
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "core-service-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        // Important for JsonDeserializer: allows deserialization of types not explicitly in trusted packages.
        // For production, consider specifying the exact packages, e.g., "com.yourcompany.kafka.model.*"
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        // If you are deserializing to a specific class (e.g., VideoMetadata), you can also set:
        // props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, VideoMetadata.class.getName());

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Configures the ConcurrentKafkaListenerContainerFactory, which is used
     * to create containers for @KafkaListener annotated methods.
     * This bean integrates the consumer factory and the error handler.
     *
     * @param consumerFactory The ConsumerFactory to use.
     * @param errorHandler The DefaultErrorHandler to apply for message processing failures.
     * @return The configured ConcurrentKafkaListenerContainerFactory.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, VideoMetadata> kafkaListenerContainerFactory(
            ConsumerFactory<String, VideoMetadata> consumerFactory,
            DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, VideoMetadata> factory = new ConcurrentKafkaListenerContainerFactory<>();
        // Correctly set the fully configured consumer factory
        factory.setConsumerFactory(consumerFactory);
        // Apply the common error handler for all listeners using this factory
        factory.setCommonErrorHandler(errorHandler);
        // If you need manual acknowledgement, uncomment the line below:
        // factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    /**
     * Defines a DefaultErrorHandler with DeadLetterPublishingRecoverer.
     * This error handler will send messages that fail after a certain number of retries
     * to a Dead Letter Topic.
     *
     * @param kafkaTemplateForDLT The KafkaTemplate used to publish messages to the DLT.
     * @return The configured DefaultErrorHandler.
     */
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplateForDLT) { // Corrected type to String, Object
        // The DeadLetterPublishingRecoverer publishes the failed record to a DLT.
        // By default, it appends ".DLT" to the original topic name.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplateForDLT);
        // FixedBackOff: first parameter is initial interval (0L for no delay on first retry),
        // second parameter is max number of retries.
        // So, 0L, 3L means 3 retries after the initial attempt (total 4 attempts).
        FixedBackOff backOff = new FixedBackOff(0L, 3L);
        return new DefaultErrorHandler(recoverer, backOff);
    }
}