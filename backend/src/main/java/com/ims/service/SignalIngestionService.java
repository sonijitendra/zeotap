package com.ims.service;

import com.ims.dto.request.SignalRequest;
import com.ims.kafka.SignalProducer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Entry point for signal ingestion — receives signals from the API layer
 * and publishes them to Kafka for async processing.
 * This decouples HTTP request handling from signal processing,
 * providing backpressure and resilience when downstream services are slow.
 */
@Service
public class SignalIngestionService {

    private static final Logger log = LoggerFactory.getLogger(SignalIngestionService.class);

    private final SignalProducer signalProducer;
    private final Counter signalIngestedCounter;

    public SignalIngestionService(SignalProducer signalProducer, MeterRegistry meterRegistry) {
        this.signalProducer = signalProducer;
        this.signalIngestedCounter = Counter.builder("ims.signals.ingested")
                .description("Total signals received at the API layer")
                .register(meterRegistry);
    }

    /**
     * Accepts a single signal and publishes it to Kafka.
     * Returns immediately — processing is fully async.
     */
    public CompletableFuture<Void> ingest(SignalRequest signal) {
        signalIngestedCounter.increment();
        log.debug("Ingesting signal {} for component {}", signal.getSignalId(), signal.getComponentId());
        return signalProducer.publish(signal).thenAccept(result -> {});
    }

    /**
     * Batch signal ingestion for high-throughput producers.
     */
    public CompletableFuture<Void> ingestBatch(List<SignalRequest> signals) {
        log.info("Batch ingesting {} signals", signals.size());
        CompletableFuture<?>[] futures = signals.stream()
                .map(this::ingest)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }
}
