package com.ims.model.enums;

/**
 * Signal severity levels driving the alerting strategy selection.
 * Lower number = higher severity.
 */
public enum Severity {
    P0("Critical - Total service outage", 0),
    P1("High - Major feature degradation", 1),
    P2("Medium - Minor feature impact", 2),
    P3("Low - Cosmetic or informational", 3);

    private final String description;
    private final int priority;

    Severity(String description, int priority) {
        this.description = description;
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * Returns the higher (more critical) severity between two.
     */
    public static Severity higher(Severity a, Severity b) {
        return a.priority <= b.priority ? a : b;
    }
}
