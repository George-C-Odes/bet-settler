package io.github.georgecodes.betsettler.infrastructure.messaging.kafka.dto;

import io.github.georgecodes.betsettler.domain.validation.ValidationSupport;

/**
 * Kafka payload representing an event outcome publication.
 *
 * @param eventId unique event identifier
 * @param eventName descriptive event name
 * @param eventWinnerId winning participant identifier for the event
 */
public record EventOutcomeMessage(String eventId, String eventName, String eventWinnerId) {

  /**
   * Creates a validated Kafka event outcome message.
   *
   * @param eventId unique event identifier
   * @param eventName descriptive event name
   * @param eventWinnerId winning participant identifier for the event
   */
  public EventOutcomeMessage {
    eventId = requireNonBlank(eventId, "eventId");
    eventName = requireNonBlank(eventName, "eventName");
    eventWinnerId = requireNonBlank(eventWinnerId, "eventWinnerId");
  }

  private static String requireNonBlank(String value, String fieldName) {
    return ValidationSupport.requireNonBlank(value, fieldName);
  }
}
