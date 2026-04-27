package io.github.georgecodes.betsettler.infrastructure.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.georgecodes.betsettler.application.dto.PublishEventOutcomeCommand;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.EventOutcomeRequest;
import org.junit.jupiter.api.Test;

class EventOutcomeRestMapperTests {

  private final EventOutcomeRestMapper eventOutcomeRestMapper = new EventOutcomeRestMapper();

  @Test
  void toCommandMapsTheEventOutcomeRequest() {
    PublishEventOutcomeCommand command =
        eventOutcomeRestMapper.toCommand(
            new EventOutcomeRequest("EVT-1001", "Team A vs Team B", "TEAM-A"));

    assertThat(command)
        .isEqualTo(new PublishEventOutcomeCommand("EVT-1001", "Team A vs Team B", "TEAM-A"));
  }

  @Test
  void toPublishEventOutcomeResponseBuildsTheAcceptedPayload() {
    assertThat(
            eventOutcomeRestMapper.toPublishEventOutcomeResponse(
                new PublishEventOutcomeCommand("EVT-1001", "Team A vs Team B", "TEAM-A")))
        .extracting("eventId", "status", "message")
        .containsExactly(
            "EVT-1001",
            "ACCEPTED",
            "Event outcome accepted and published to Kafka for asynchronous processing.");
  }

  @Test
  void toCommandRejectsNullRequest() {
    assertThatThrownBy(() -> eventOutcomeRestMapper.toCommand(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("request must not be null");
  }

  @Test
  void toPublishEventOutcomeResponseRejectsNullCommand() {
    assertThatThrownBy(() -> eventOutcomeRestMapper.toPublishEventOutcomeResponse(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("command must not be null");
  }
}
