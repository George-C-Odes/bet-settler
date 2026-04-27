package io.github.georgecodes.betsettler.infrastructure.messaging.kafka.mapper;

import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.infrastructure.messaging.kafka.dto.EventOutcomeMessage;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Maps domain event outcomes to Kafka transport payloads. */
@Component
public class EventOutcomeMessageMapper {

  /** Creates a new event outcome message mapper. */
  public EventOutcomeMessageMapper() {}

  /**
   * Maps the supplied domain event outcome to a Kafka payload.
   *
   * @param eventOutcome domain event outcome to map
   * @return mapped Kafka payload
   */
  public EventOutcomeMessage toMessage(EventOutcome eventOutcome) {
    Objects.requireNonNull(eventOutcome, "eventOutcome must not be null");
    return new EventOutcomeMessage(
        eventOutcome.eventId(), eventOutcome.eventName(), eventOutcome.eventWinnerId());
  }

  /**
   * Maps the supplied Kafka payload into the domain event outcome.
   *
   * @param eventOutcomeMessage Kafka payload to map
   * @return mapped domain event outcome
   */
  public EventOutcome toDomain(EventOutcomeMessage eventOutcomeMessage) {
    Objects.requireNonNull(eventOutcomeMessage, "eventOutcomeMessage must not be null");
    return new EventOutcome(
        eventOutcomeMessage.eventId(),
        eventOutcomeMessage.eventName(),
        eventOutcomeMessage.eventWinnerId());
  }
}
