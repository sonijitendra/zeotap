package com.ims.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.Map;

/**
 * Inbound signal payload from monitoring agents.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalRequest {

    @NotBlank(message = "signalId is required")
    private String signalId;

    @NotBlank(message = "componentId is required")
    private String componentId;

    @NotBlank(message = "severity is required")
    private String severity;

    @NotNull(message = "timestamp is required")
    private Instant timestamp;

    @NotBlank(message = "message is required")
    private String message;

    private Map<String, Object> metadata;
}
