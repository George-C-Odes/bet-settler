package io.github.georgecodes.betsettler.infrastructure.rest.mapper;

import io.github.georgecodes.betsettler.application.dto.PublishEventOutcomeCommand;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.EventOutcomeRequest;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.PublishEventOutcomeResponse;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Maps event-outcome REST payloads to and from the publish use case boundary. */
@Component
public class EventOutcomeRestMapper {

  /** Creates a new event-outcome REST mapper. */
  public EventOutcomeRestMapper() {}

  /**
   * Converts a REST request body into an application publish command.
   *
   * @param request request body received from the API
   * @return application command built from the request
   */
  public PublishEventOutcomeCommand toCommand(EventOutcomeRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    return new PublishEventOutcomeCommand(
        request.eventId(), request.eventName(), request.eventWinnerId());
  }

  /**
   * Creates the accepted-response payload for a publish request.
   *
   * @param command accepted publish command
   * @return accepted-response payload
   */
  public PublishEventOutcomeResponse toPublishEventOutcomeResponse(
      PublishEventOutcomeCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    return new PublishEventOutcomeResponse(
        command.eventId(),
        "ACCEPTED",
        "Event outcome accepted and published to Kafka for asynchronous processing.");
  }
}
