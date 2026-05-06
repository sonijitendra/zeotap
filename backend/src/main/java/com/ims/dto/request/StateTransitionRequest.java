package com.ims.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request to transition an incident to a new state.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateTransitionRequest {

    @NotBlank(message = "targetState is required")
    private String targetState;

    private String changedBy;

    private String notes;
}
