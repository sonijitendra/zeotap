package com.ims.workflow.strategy;

import com.ims.model.postgres.Incident;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory that selects the appropriate AlertStrategy based on severity.
 * Strategies are auto-registered via Spring DI — no if-else chains.
 */
@Component
public class AlertStrategyFactory {

    private static final Logger log = LoggerFactory.getLogger(AlertStrategyFactory.class);

    private final List<AlertStrategy> strategies;
    private final Map<String, AlertStrategy> strategyMap = new HashMap<>();

    public AlertStrategyFactory(List<AlertStrategy> strategies) {
        this.strategies = strategies;
    }

    @PostConstruct
    void init() {
        strategies.forEach(s -> strategyMap.put(s.getSeverity(), s));
        log.info("Registered {} alert strategies: {}", strategyMap.size(), strategyMap.keySet());
    }

    /**
     * Executes the appropriate alerting strategy for the incident's severity.
     * Falls back to P2 strategy for unknown severities.
     */
    public void executeAlert(Incident incident) {
        String severity = incident.getSeverity().name();
        AlertStrategy strategy = strategyMap.getOrDefault(severity,
                strategyMap.get("P2")); // Default fallback
        if (strategy != null) {
            strategy.alert(incident);
        } else {
            log.warn("No alert strategy found for severity: {}", severity);
        }
    }
}
