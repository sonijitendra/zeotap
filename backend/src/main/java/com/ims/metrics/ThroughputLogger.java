package com.ims.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Logs signal throughput metrics every 5 seconds as required.
 * Tracks total signals ingested/processed and calculates rate.
 */
@Component
public class ThroughputLogger {

    private static final Logger log = LoggerFactory.getLogger(ThroughputLogger.class);

    private final MeterRegistry meterRegistry;
    private final AtomicLong lastIngestedCount = new AtomicLong(0);
    private final AtomicLong lastProcessedCount = new AtomicLong(0);

    public ThroughputLogger(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedRate = 5000)
    public void logThroughput() {
        double ingestedTotal = getCounterValue("ims.signals.ingested");
        double processedTotal = getCounterValue("ims.signals.processed");
        double failedTotal = getCounterValue("ims.signals.failed");
        double producedTotal = getCounterValue("ims.signals.produced");

        long ingDelta = (long) ingestedTotal - lastIngestedCount.getAndSet((long) ingestedTotal);
        long procDelta = (long) processedTotal - lastProcessedCount.getAndSet((long) processedTotal);

        double ingestRate = ingDelta / 5.0;
        double processRate = procDelta / 5.0;

        log.info("📊 THROUGHPUT [5s] | Ingested: {}/s ({} total) | Processed: {}/s ({} total) | Failed: {} | Produced: {}",
                String.format("%.1f", ingestRate), (long) ingestedTotal,
                String.format("%.1f", processRate), (long) processedTotal,
                (long) failedTotal, (long) producedTotal);
    }

    private double getCounterValue(String name) {
        Counter counter = meterRegistry.find(name).counter();
        return counter != null ? counter.count() : 0;
    }
}
