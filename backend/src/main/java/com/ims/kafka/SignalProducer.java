package com.ims.kafka;

import com.ims.dto.request.SignalRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for signal ingestion pipeline.
 * Partitions messages by componentId to ensure ordering per component.
 * Uses idempotent producer config for exactly-once delivery guarantees.
 */
@Component
public class SignalProducer {

    private static final Logger log = LoggerFactory.getLogger(SignalProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topicName;
    private final Counter signalProducedCounter;

    public SignalProducer(KafkaTemplate<String, Object> kafkaTemplate,
                          @Value("${ims.signal.kafka-topic}") String topicName,
                          MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
        this.signalProducedCounter = Counter.builder("ims.signals.produced")
                .description("Total signals published to Kafka")
                .register(meterRegistry);
    }

    /**
     * Publishes a signal to Kafka asynchronously.
     * Uses componentId as the partition key to co-locate signals from the same component.
     */
    public CompletableFuture<SendResult<String, Object>> publish(SignalRequest signal) {
        return kafkaTemplate.send(topicName, signal.getComponentId(), signal)
                .thenApply(result -> {
                    signalProducedCounter.increment();
                    log.debug("Signal {} published to partition {} offset {}",
                            signal.getSignalId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    return result;
                })
                .exceptionally(ex -> {
                    log.error("Failed to publish signal {} to Kafka: {}",
                            signal.getSignalId(), ex.getMessage(), ex);
                    return null;
                });
    }
}
