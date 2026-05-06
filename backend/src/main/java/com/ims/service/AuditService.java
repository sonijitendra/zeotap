package com.ims.service;

import com.ims.model.mongo.AuditLog;
import com.ims.repository.mongo.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Async("signalProcessorExecutor")
    public void logAsync(String action, String entityType, String entityId, Object payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payloadMap = objectMapper.convertValue(payload, Map.class);
            AuditLog auditLog = AuditLog.builder()
                    .action(action).entityType(entityType).entityId(entityId)
                    .payload(payloadMap).actor("system").timestamp(Instant.now()).build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }
}
