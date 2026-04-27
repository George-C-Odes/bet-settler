package io.github.georgecodes.betsettler.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body used to publish an event outcome into the settlement workflow.
 *
 * @param eventId unique event identifier
 * @param eventName descriptive event name
 * @param eventWinnerId winning participant identifier for the event
 */
@Schema(description = "Request body used to publish an event outcome into the settlement workflow.")
public record EventOutcomeRequest(
    @Schema(description = "Unique sports event identifier.", example = "EVT-1001")
        @NotBlank(message = "eventId must not be blank")
        String eventId,
    @Schema(description = "Human-readable event name.", example = "Team A vs Team B")
        @NotBlank(message = "eventName must not be blank")
        String eventName,
    @Schema(description = "Winning participant identifier for the event.", example = "TEAM-A")
        @NotBlank(message = "eventWinnerId must not be blank")
        String eventWinnerId) {}
