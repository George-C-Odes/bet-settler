package io.github.georgecodes.betsettler.infrastructure.rest.dto;

import io.github.georgecodes.betsettler.domain.validation.ValidationSupport;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response returned when an event outcome publish request is accepted.
 *
 * @param eventId accepted event identifier
 * @param status transport-level request status
 * @param message human-readable confirmation message
 */
@Schema(description = "Response returned when an event outcome publish request is accepted.")
public record PublishEventOutcomeResponse(String eventId, String status, String message) {

  /**
   * Creates a validated publish response.
   *
   * @param eventId accepted event identifier
   * @param status transport-level request status
   * @param message human-readable confirmation message
   */
  public PublishEventOutcomeResponse {
    eventId = requireNonBlank(eventId, "eventId");
    status = requireNonBlank(status, "status");
    message = requireNonBlank(message, "message");
  }

  private static String requireNonBlank(String value, String fieldName) {
    return ValidationSupport.requireNonBlank(value, fieldName);
  }
}
