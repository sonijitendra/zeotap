package com.ims.service;

import com.ims.dto.request.SignalRequest;
import com.ims.model.enums.IncidentState;
import com.ims.model.enums.Severity;
import com.ims.model.mongo.RawSignal;
import com.ims.model.postgres.Incident;
import com.ims.model.postgres.IncidentSignal;
import com.ims.repository.mongo.RawSignalRepository;
import com.ims.repository.postgres.IncidentRepository;
import com.ims.repository.postgres.IncidentSignalRepository;
import com.ims.workflow.strategy.AlertStrategyFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Core signal processing service — called by the Kafka consumer.
 * Handles:
 * 1. Duplicate signal detection (idempotency)
 * 2. Raw signal persistence to MongoDB
 * 3. Debounce check via DebounceService
 * 4. Incident creation or signal linking
 * 5. Alert strategy execution
 */
@Service
public class SignalProcessingService {

    private static final Logger log = LoggerFactory.getLogger(SignalProcessingService.class);

    private final RawSignalRepository rawSignalRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentSignalRepository incidentSignalRepository;
    private final DebounceService debounceService;
    private final AlertStrategyFactory alertStrategyFactory;
    private final SseEmitterService sseEmitterService;
    private final AuditService auditService;
    private final Counter signalProcessedCounter;
    private final Counter incidentCreatedCounter;

    public SignalProcessingService(RawSignalRepository rawSignalRepository,
                                    IncidentRepository incidentRepository,
                                    IncidentSignalRepository incidentSignalRepository,
                                    DebounceService debounceService,
                                    AlertStrategyFactory alertStrategyFactory,
                                    SseEmitterService sseEmitterService,
                                    AuditService auditService,
                                    MeterRegistry meterRegistry) {
        this.rawSignalRepository = rawSignalRepository;
        this.incidentRepository = incidentRepository;
        this.incidentSignalRepository = incidentSignalRepository;
        this.debounceService = debounceService;
        this.alertStrategyFactory = alertStrategyFactory;
        this.sseEmitterService = sseEmitterService;
        this.auditService = auditService;
        this.signalProcessedCounter = Counter.builder("ims.signals.processed")
                .description("Signals successfully processed")
                .register(meterRegistry);
        this.incidentCreatedCounter = Counter.builder("ims.incidents.created")
                .description("Incidents created from signals")
                .register(meterRegistry);
    }

    /**
     * Process a single signal:
     * 1. Check for duplicate (idempotency key = signalId)
     * 2. Persist raw signal to MongoDB
     * 3. Debounce check — create incident or link to existing
     * 4. Execute alerting strategy
     *
     * Retries up to 3 times with exponential backoff on transient failures.
     */
    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0, maxDelay = 5000),
            retryFor = { org.springframework.dao.TransientDataAccessException.class }
    )
    public void processSignal(SignalRequest signal) {
        // 1. Idempotency check — skip if already processed
        if (rawSignalRepository.existsBySignalId(signal.getSignalId())) {
            log.debug("Duplicate signal {} — skipping", signal.getSignalId());
            return;
        }

        // 2. Persist raw signal to MongoDB
        RawSignal rawSignal = RawSignal.builder()
                .signalId(signal.getSignalId())
                .componentId(signal.getComponentId())
                .severity(signal.getSeverity())
                .timestamp(signal.getTimestamp())
                .message(signal.getMessage())
                .metadata(signal.getMetadata())
                .processed(true)
                .processedAt(Instant.now())
                .build();

        rawSignalRepository.save(rawSignal);

        // 3. Debounce logic — returns existing incident ID or creates new one
        DebounceService.DebounceResult result = debounceService.debounce(signal);

        Incident incident;
        if (result.isNewIncident()) {
            // Create new incident
            incident = createIncident(signal);
            rawSignal.setIncidentId(incident.getId().toString());
            rawSignalRepository.save(rawSignal);

            // Update Redis debounce cache with the real DB-generated incident ID
            debounceService.updateIncidentId(signal.getComponentId(), incident.getId());

            // Execute alerting strategy
            alertStrategyFactory.executeAlert(incident);
            incidentCreatedCounter.increment();

            // Notify SSE subscribers
            sseEmitterService.sendIncidentUpdate(incident);

            auditService.logAsync("INCIDENT_CREATED", "Incident", incident.getId().toString(), signal);
        } else {
            // Link signal to existing incident
            UUID incidentId = result.getIncidentId();
            incident = incidentRepository.findById(incidentId).orElse(null);
            if (incident != null) {
                linkSignalToIncident(signal, incident);
                rawSignal.setIncidentId(incidentId.toString());
                rawSignalRepository.save(rawSignal);
                sseEmitterService.sendIncidentUpdate(incident);
            }
        }

        signalProcessedCounter.increment();
        log.debug("Signal {} processed successfully", signal.getSignalId());
    }

    @Transactional
    protected Incident createIncident(SignalRequest signal) {
        Severity severity;
        try {
            severity = Severity.valueOf(signal.getSeverity());
        } catch (IllegalArgumentException e) {
            severity = Severity.P2; // Default severity
        }

        Incident incident = Incident.builder()
                .componentId(signal.getComponentId())
                .severity(severity)
                .state(IncidentState.OPEN)
                .title(String.format("[%s] %s — %s", signal.getSeverity(),
                        signal.getComponentId(), signal.getMessage()))
                .description(signal.getMessage())
                .signalCount(1)
                .firstSignalAt(signal.getTimestamp())
                .build();

        incident = incidentRepository.save(incident);

        // Link the first signal
        IncidentSignal link = IncidentSignal.builder()
                .incidentId(incident.getId())
                .signalId(signal.getSignalId())
                .build();
        incidentSignalRepository.save(link);

        log.info("Created incident {} for component {} with severity {}",
                incident.getId(), signal.getComponentId(), severity);
        return incident;
    }

    @Transactional
    protected void linkSignalToIncident(SignalRequest signal, Incident incident) {
        // Idempotency check for linking
        if (!incidentSignalRepository.existsBySignalId(signal.getSignalId())) {
            IncidentSignal link = IncidentSignal.builder()
                    .incidentId(incident.getId())
                    .signalId(signal.getSignalId())
                    .build();
            incidentSignalRepository.save(link);
            incident.setSignalCount(incident.getSignalCount() + 1);

            // Escalate severity if new signal is higher
            try {
                Severity signalSeverity = Severity.valueOf(signal.getSeverity());
                if (signalSeverity.getPriority() < incident.getSeverity().getPriority()) {
                    log.info("Escalating incident {} severity from {} to {}",
                            incident.getId(), incident.getSeverity(), signalSeverity);
                    incident.setSeverity(signalSeverity);
                }
            } catch (IllegalArgumentException ignored) {}

            incidentRepository.save(incident);
            log.debug("Linked signal {} to incident {} (count: {})",
                    signal.getSignalId(), incident.getId(), incident.getSignalCount());
        }
    }
}
