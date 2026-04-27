package io.github.georgecodes.betsettler.application.dto;

import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.domain.validation.ValidationSupport;

/**
 * Command used to publish an event outcome into the settlement flow.
 *
 * @param eventId unique event identifier
 * @param eventName descriptive event name
 * @param eventWinnerId winning participant identifier for the event
 */
public record PublishEventOutcomeCommand(String eventId, String eventName, String eventWinnerId) {

  /**
   * Creates a validated publish command.
   *
   * @param eventId unique event identifier
   * @param eventName descriptive event name
   * @param eventWinnerId winning participant identifier for the event
   */
  public PublishEventOutcomeCommand {
    eventId = requireNonBlank(eventId, "eventId");
    eventName = requireNonBlank(eventName, "eventName");
    eventWinnerId = requireNonBlank(eventWinnerId, "eventWinnerId");
  }

  /**
   * Converts the command into the core domain event outcome.
   *
   * @return a domain event outcome
   */
  public EventOutcome toEventOutcome() {
    return new EventOutcome(eventId, eventName, eventWinnerId);
  }

  private static String requireNonBlank(String value, String fieldName) {
    return ValidationSupport.requireNonBlank(value, fieldName);
  }
}
