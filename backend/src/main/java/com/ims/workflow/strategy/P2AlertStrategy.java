package com.ims.workflow.strategy;

import com.ims.model.postgres.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * P2 (Medium) alert strategy — standard notification.
 * In production: Slack message, dashboard highlight, email digest.
 */
@Component
public class P2AlertStrategy implements AlertStrategy {

    private static final Logger log = LoggerFactory.getLogger(P2AlertStrategy.class);

    @Override
    public void alert(Incident incident) {
        log.info("📋 P2 MEDIUM ALERT — Incident: {} | Component: {} | Title: {}",
                incident.getId(), incident.getComponentId(), incident.getTitle());
        log.info("   ➤ Posting to #incidents-low Slack channel");
        log.info("   ➤ Adding to daily incident digest");
        // In production: Slack, email digest
    }

    @Override
    public String getSeverity() {
        return "P2";
    }
}
