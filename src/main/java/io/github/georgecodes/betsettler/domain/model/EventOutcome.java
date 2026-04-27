package io.github.georgecodes.betsettler.domain.model;

import io.github.georgecodes.betsettler.domain.validation.ValidationSupport;

/**
 * Represents the outcome of a sporting event.
 *
 * @param eventId unique event identifier
 * @param eventName descriptive event name
 * @param eventWinnerId winning participant identifier for the event
 */
public record EventOutcome(String eventId, String eventName, String eventWinnerId) {

  /**
   * Creates a validated event outcome.
   *
   * @param eventId unique event identifier
   * @param eventName descriptive event name
   * @param eventWinnerId winning participant identifier for the event
   */
  public EventOutcome {
    eventId = requireNonBlank(eventId, "eventId");
    eventName = requireNonBlank(eventName, "eventName");
    eventWinnerId = requireNonBlank(eventWinnerId, "eventWinnerId");
  }

  private static String requireNonBlank(String value, String fieldName) {
    return ValidationSupport.requireNonBlank(value, fieldName);
  }
}
