package com.ims.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration with DLQ support and error handling.
 */
@Configuration
public class KafkaConfig {

    @Value("${ims.signal.kafka-topic}")
    private String signalTopic;

    @Value("${ims.signal.dlq-topic}")
    private String dlqTopic;

    @Value("${ims.signal.incident-topic}")
    private String incidentTopic;

    @Bean
    public NewTopic signalsTopic() {
        return TopicBuilder.name(signalTopic)
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic deadLetterTopic() {
        return TopicBuilder.name(dlqTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic incidentEventsTopic() {
        return TopicBuilder.name(incidentTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Error handler with DLQ routing: after 3 retries with 1s backoff,
     * failed messages are published to the dead letter topic.
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
    }
}
