package com.ims.service;

import com.ims.dto.request.SignalRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Debounce Service Tests")
class DebounceServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private RedissonClient redissonClient;
    @Mock private ValueOperations<String, Object> valueOps;
    @Mock private RLock rLock;

    private DebounceService debounceService;

    @BeforeEach
    void setUp() {
        debounceService = new DebounceService(redisTemplate, redissonClient, 10, 500, 3000);
    }

    private SignalRequest createSignal(String componentId) {
        return SignalRequest.builder()
                .signalId(UUID.randomUUID().toString())
                .componentId(componentId)
                .severity("P1")
                .timestamp(Instant.now())
                .message("Test signal")
                .build();
    }

    @Test
    @DisplayName("First signal in window creates new incident")
    void firstSignalCreatesNewIncident() throws Exception {
        SignalRequest signal = createSignal("CACHE_01");

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("debounce:CACHE_01"), any(), any(Duration.class)))
                .thenReturn(true);
        when(redissonClient.getLock("lock:debounce:CACHE_01")).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(valueOps.get("debounce:incident:CACHE_01")).thenReturn(null);

        DebounceService.DebounceResult result = debounceService.debounce(signal);

        assertTrue(result.isNewIncident());
        assertNotNull(result.getIncidentId());
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("Subsequent signal links to existing incident")
    void subsequentSignalLinksToExisting() {
        SignalRequest signal = createSignal("CACHE_01");
        UUID existingId = UUID.randomUUID();

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("debounce:CACHE_01"), any(), any(Duration.class)))
                .thenReturn(false);
        when(valueOps.get("debounce:incident:CACHE_01")).thenReturn(existingId.toString());

        DebounceService.DebounceResult result = debounceService.debounce(signal);

        assertFalse(result.isNewIncident());
        assertEquals(existingId, result.getIncidentId());
    }

    @Test
    @DisplayName("Double-check prevents duplicate incident creation")
    void doubleCheckPreventsDuplicate() throws Exception {
        SignalRequest signal = createSignal("CACHE_01");
        UUID existingId = UUID.randomUUID();

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("debounce:CACHE_01"), any(), any(Duration.class)))
                .thenReturn(true);
        when(redissonClient.getLock("lock:debounce:CACHE_01")).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        // Another thread created the incident between SETNX and lock acquisition
        when(valueOps.get("debounce:incident:CACHE_01")).thenReturn(existingId.toString());

        DebounceService.DebounceResult result = debounceService.debounce(signal);

        assertFalse(result.isNewIncident());
        assertEquals(existingId, result.getIncidentId());
    }

    @Test
    @DisplayName("Window count tracking works")
    void windowCountTracking() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("debounce:count:CACHE_01")).thenReturn("42");

        long count = debounceService.getWindowCount("CACHE_01");

        assertEquals(42, count);
    }

    @Test
    @DisplayName("Update incident ID preserves TTL")
    void updateIncidentIdPreservesTtl() {
        UUID incidentId = UUID.randomUUID();
        when(redisTemplate.getExpire("debounce:incident:CACHE_01", TimeUnit.SECONDS)).thenReturn(7L);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        debounceService.updateIncidentId("CACHE_01", incidentId);

        verify(valueOps).set(eq("debounce:incident:CACHE_01"),
                eq(incidentId.toString()), eq(Duration.ofSeconds(7)));
    }

    @Test
    @DisplayName("DebounceResult factory methods work correctly")
    void debounceResultFactoryMethods() {
        UUID id = UUID.randomUUID();

        DebounceService.DebounceResult newResult = DebounceService.DebounceResult.newIncident(id);
        assertTrue(newResult.isNewIncident());
        assertEquals(id, newResult.getIncidentId());

        DebounceService.DebounceResult existingResult = DebounceService.DebounceResult.existing(id);
        assertFalse(existingResult.isNewIncident());
        assertEquals(id, existingResult.getIncidentId());
    }
}
