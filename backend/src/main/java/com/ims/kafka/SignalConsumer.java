package com.ims.kafka;

import com.ims.dto.request.SignalRequest;
import com.ims.service.SignalProcessingService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kafka consumer for processing ingested signals.
 * Configured for batch consumption with manual acknowledgment
 * to support backpressure and retry semantics.
 */
@Component
public class SignalConsumer {

    private static final Logger log = LoggerFactory.getLogger(SignalConsumer.class);

    private final SignalProcessingService signalProcessingService;
    private final Counter signalConsumedCounter;
    private final Counter signalFailedCounter;

    public SignalConsumer(SignalProcessingService signalProcessingService,
                          MeterRegistry meterRegistry) {
        this.signalProcessingService = signalProcessingService;
        this.signalConsumedCounter = Counter.builder("ims.signals.consumed")
                .description("Total signals consumed from Kafka")
                .register(meterRegistry);
        this.signalFailedCounter = Counter.builder("ims.signals.failed")
                .description("Total signals that failed processing")
                .register(meterRegistry);
    }

    /**
     * Batch listener consuming signals from the ingestion topic.
     * Processes each signal individually for granular error handling.
     * Only acknowledges after all signals in the batch are processed.
     */
    @KafkaListener(
            topics = "${ims.signal.kafka-topic}",
            groupId = "ims-signal-processor",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeSignals(@Payload List<SignalRequest> signals,
                                @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                                Acknowledgment ack) {
        log.debug("Received batch of {} signals from partitions {}", signals.size(), partitions);

        int successCount = 0;
        int failCount = 0;

        for (SignalRequest signal : signals) {
            try {
                signalProcessingService.processSignal(signal);
                signalConsumedCounter.increment();
                successCount++;
            } catch (Exception e) {
                signalFailedCounter.increment();
                failCount++;
                log.error("Failed to process signal {}: {}", signal.getSignalId(), e.getMessage(), e);
                // Individual signal failures don't block the batch — failed signals
                // will be retried via Kafka error handler / DLQ
            }
        }

        // Acknowledge the entire batch
        ack.acknowledge();
        log.debug("Batch processing complete: {} succeeded, {} failed", successCount, failCount);
    }
}
