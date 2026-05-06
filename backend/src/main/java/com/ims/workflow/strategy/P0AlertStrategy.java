package com.ims.workflow.strategy;

import com.ims.model.postgres.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * P0 (Critical) alert strategy — immediate multi-channel notification.
 * In production: pages on-call, sends Slack/PagerDuty, triggers war room.
 */
@Component
public class P0AlertStrategy implements AlertStrategy {

    private static final Logger log = LoggerFactory.getLogger(P0AlertStrategy.class);

    @Override
    public void alert(Incident incident) {
        log.error("🚨 P0 CRITICAL ALERT — Incident: {} | Component: {} | Title: {}",
                incident.getId(), incident.getComponentId(), incident.getTitle());
        log.error("   ➤ Paging on-call engineer immediately");
        log.error("   ➤ Opening war room channel");
        log.error("   ➤ Notifying VP of Engineering");
        log.error("   ➤ Sending PagerDuty high-urgency notification");
        // In production: integrate with PagerDuty, Slack, OpsGenie APIs
    }

    @Override
    public String getSeverity() {
        return "P0";
    }
}
