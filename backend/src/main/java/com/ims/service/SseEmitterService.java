package com.ims.service;

import com.ims.model.postgres.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-Sent Events service for real-time incident updates to frontend.
 */
@Service
public class SseEmitterService {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterService.class);
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        log.debug("SSE client connected. Total: {}", emitters.size());
        return emitter;
    }

    public void sendIncidentUpdate(Incident incident) {
        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("incident-update")
                        .data(java.util.Map.of(
                                "id", incident.getId().toString(),
                                "componentId", incident.getComponentId(),
                                "severity", incident.getSeverity().name(),
                                "state", incident.getState().name(),
                                "title", incident.getTitle(),
                                "signalCount", incident.getSignalCount()
                        )));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }
}
