package com.ims.service;

import com.ims.dto.request.SignalRequest;
import lombok.Getter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Debounce engine using Redis for distributed coordination.
 *
 * RACE CONDITION PREVENTION STRATEGY:
 * ═══════════════════════════════════
 * 1. Redis SETNX (SET if Not eXists) is atomic — exactly one thread wins the
 *    "first signal in window" designation.
 *
 * 2. Redis INCR is atomic — concurrent signal counting is safe without locks.
 *
 * 3. When creating an incident (on first signal), we use a Redisson distributed
 *    lock ("lock:debounce:{componentId}") to prevent duplicate incident creation
 *    across multiple consumer instances.
 *
 * 4. Inside the lock, we perform a DOUBLE-CHECK: re-verify that no incident was
 *    created between our initial check and lock acquisition.
 *
 * 5. The debounce window has a TTL, so it auto-expires — no manual cleanup needed.
 *
 * 6. Each signal carries an idempotency key (signalId) checked upstream in
 *    SignalProcessingService to prevent duplicate processing.
 */
@Service
public class DebounceService {

    private static final Logger log = LoggerFactory.getLogger(DebounceService.class);

    private static final String DEBOUNCE_KEY_PREFIX = "debounce:";
    private static final String DEBOUNCE_COUNT_PREFIX = "debounce:count:";
    private static final String DEBOUNCE_INCIDENT_PREFIX = "debounce:incident:";
    private static final String LOCK_PREFIX = "lock:debounce:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final int windowSeconds;
    private final long lockWaitMs;
    private final long lockLeaseMs;

    public DebounceService(RedisTemplate<String, Object> redisTemplate,
                           RedissonClient redissonClient,
                           @Value("${ims.debounce.window-seconds}") int windowSeconds,
                           @Value("${ims.debounce.lock-wait-ms}") long lockWaitMs,
                           @Value("${ims.debounce.lock-lease-ms}") long lockLeaseMs) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.windowSeconds = windowSeconds;
        this.lockWaitMs = lockWaitMs;
        this.lockLeaseMs = lockLeaseMs;
    }

    /**
     * Performs debounce logic for an incoming signal.
     *
     * @return DebounceResult indicating whether a new incident should be created
     *         or if the signal should be linked to an existing incident.
     */
    public DebounceResult debounce(SignalRequest signal) {
        String componentId = signal.getComponentId();
        String windowKey = DEBOUNCE_KEY_PREFIX + componentId;
        String countKey = DEBOUNCE_COUNT_PREFIX + componentId;
        String incidentKey = DEBOUNCE_INCIDENT_PREFIX + componentId;

        // Step 1: Attempt to set the window marker (SETNX — atomic)
        Boolean isFirstInWindow = redisTemplate.opsForValue()
                .setIfAbsent(windowKey, "1", Duration.ofSeconds(windowSeconds));

        if (Boolean.TRUE.equals(isFirstInWindow)) {
            // This is the FIRST signal in the debounce window
            // Initialize counter
            redisTemplate.opsForValue().set(countKey, "1", Duration.ofSeconds(windowSeconds));

            // Acquire distributed lock to safely create the incident
            RLock lock = redissonClient.getLock(LOCK_PREFIX + componentId);
            try {
                if (lock.tryLock(lockWaitMs, lockLeaseMs, TimeUnit.MILLISECONDS)) {
                    try {
                        // Double-check: verify no incident was created by another consumer
                        Object existingIncidentId = redisTemplate.opsForValue().get(incidentKey);
                        if (existingIncidentId != null) {
                            // Another thread beat us — link to existing
                            log.debug("Double-check: incident already exists for component {}", componentId);
                            return DebounceResult.existing(UUID.fromString(existingIncidentId.toString()));
                        }

                        // Create new incident marker (actual incident creation happens in caller)
                        // Store a placeholder; caller will update with real incident ID
                        String placeholderId = UUID.randomUUID().toString();
                        redisTemplate.opsForValue().set(incidentKey, placeholderId,
                                Duration.ofSeconds(windowSeconds));

                        log.info("New debounce window for component {} — creating incident", componentId);
                        return DebounceResult.newIncident(UUID.fromString(placeholderId));
                    } finally {
                        lock.unlock();
                    }
                } else {
                    // Couldn't acquire lock — another thread is creating the incident
                    // Wait briefly and check for the incident
                    log.debug("Lock contention for component {} — checking for existing incident", componentId);
                    return waitForIncident(incidentKey, componentId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted during debounce lock acquisition for component {}", componentId);
                return waitForIncident(incidentKey, componentId);
            }
        } else {
            // NOT the first signal — increment counter and link to existing incident
            redisTemplate.opsForValue().increment(countKey);

            Object existingIncidentId = redisTemplate.opsForValue().get(incidentKey);
            if (existingIncidentId != null) {
                UUID incidentId = UUID.fromString(existingIncidentId.toString());
                log.debug("Debounce hit for component {} — linking to incident {}", componentId, incidentId);
                return DebounceResult.existing(incidentId);
            } else {
                // Incident hasn't been created yet — wait for it
                return waitForIncident(incidentKey, componentId);
            }
        }
    }

    /**
     * Updates the cached incident ID in the debounce window after actual creation.
     */
    public void updateIncidentId(String componentId, UUID incidentId) {
        String incidentKey = DEBOUNCE_INCIDENT_PREFIX + componentId;
        Long ttl = redisTemplate.getExpire(incidentKey, TimeUnit.SECONDS);
        if (ttl != null && ttl > 0) {
            redisTemplate.opsForValue().set(incidentKey, incidentId.toString(),
                    Duration.ofSeconds(ttl));
        }
    }

    /**
     * Returns the current signal count in the debounce window for a component.
     */
    public long getWindowCount(String componentId) {
        Object count = redisTemplate.opsForValue().get(DEBOUNCE_COUNT_PREFIX + componentId);
        return count != null ? Long.parseLong(count.toString()) : 0;
    }

    private DebounceResult waitForIncident(String incidentKey, String componentId) {
        // Brief spin-wait for the incident to be created
        for (int i = 0; i < 10; i++) {
            Object id = redisTemplate.opsForValue().get(incidentKey);
            if (id != null) {
                return DebounceResult.existing(UUID.fromString(id.toString()));
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // Fallback: create a new incident (safe because of idempotency at DB level)
        log.warn("Timeout waiting for incident creation for component {} — creating new", componentId);
        return DebounceResult.newIncident(UUID.randomUUID());
    }

    /**
     * Result of the debounce check.
     */
    @Getter
    public static class DebounceResult {
        private final boolean newIncident;
        private final UUID incidentId;

        private DebounceResult(boolean newIncident, UUID incidentId) {
            this.newIncident = newIncident;
            this.incidentId = incidentId;
        }

        public static DebounceResult newIncident(UUID id) {
            return new DebounceResult(true, id);
        }

        public static DebounceResult existing(UUID id) {
            return new DebounceResult(false, id);
        }
    }
}
