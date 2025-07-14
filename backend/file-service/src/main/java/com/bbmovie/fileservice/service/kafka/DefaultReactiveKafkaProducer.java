package com.bbmovie.fileservice.service.kafka;

import com.example.common.dtos.kafka.FileUploadEvent;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Log4j2
@Service
public class DefaultReactiveKafkaProducer implements ReactiveKafkaProducer {

    private final KafkaTemplate<String, FileUploadEvent> kafkaTemplate;

    @Autowired
    public DefaultReactiveKafkaProducer(KafkaTemplate<String, FileUploadEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public Mono<Void> send(String topic, String aggregateId, FileUploadEvent event) {
        return Mono.fromCallable(() -> {
                    kafkaTemplate.send(topic, aggregateId, event)
                            .whenComplete((result, throwable) ->
                                    {
                                        if (throwable == null) {
                                            log.info("Published event: {}", result);
                                        } else {
                                            log.error("Failed to publish event", throwable);
                                        }
                                    }
                            );
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}