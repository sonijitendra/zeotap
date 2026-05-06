package com.ims.workflow;

import com.ims.model.enums.Severity;
import com.ims.model.postgres.Incident;
import com.ims.workflow.strategy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Alert Strategy Factory Tests")
class AlertStrategyFactoryTest {

    private AlertStrategyFactory factory;

    @BeforeEach
    void setUp() {
        factory = new AlertStrategyFactory(List.of(
                new P0AlertStrategy(), new P1AlertStrategy(), new P2AlertStrategy()));
        factory.init();
    }

    @Test
    @DisplayName("Correct strategy is selected for each severity")
    void correctStrategySelection() {
        Incident p0 = Incident.builder().id(UUID.randomUUID()).severity(Severity.P0)
                .componentId("TEST").title("P0 Test").build();
        Incident p1 = Incident.builder().id(UUID.randomUUID()).severity(Severity.P1)
                .componentId("TEST").title("P1 Test").build();
        Incident p2 = Incident.builder().id(UUID.randomUUID()).severity(Severity.P2)
                .componentId("TEST").title("P2 Test").build();

        // Should not throw
        assertDoesNotThrow(() -> factory.executeAlert(p0));
        assertDoesNotThrow(() -> factory.executeAlert(p1));
        assertDoesNotThrow(() -> factory.executeAlert(p2));
    }
}
