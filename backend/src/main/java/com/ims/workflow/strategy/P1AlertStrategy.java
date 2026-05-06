package com.ims.workflow.strategy;

import com.ims.model.postgres.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * P1 (High) alert strategy — urgent team notification.
 * In production: Slack channel alert, PagerDuty low-urgency, email to team lead.
 */
@Component
public class P1AlertStrategy implements AlertStrategy {

    private static final Logger log = LoggerFactory.getLogger(P1AlertStrategy.class);

    @Override
    public void alert(Incident incident) {
        log.warn("⚠️  P1 HIGH ALERT — Incident: {} | Component: {} | Title: {}",
                incident.getId(), incident.getComponentId(), incident.getTitle());
        log.warn("   ➤ Sending Slack notification to #incidents channel");
        log.warn("   ➤ Paging primary on-call (low urgency)");
        log.warn("   ➤ Emailing team lead");
        // In production: integrate with Slack, PagerDuty low-urgency
    }

    @Override
    public String getSeverity() {
        return "P1";
    }
}
