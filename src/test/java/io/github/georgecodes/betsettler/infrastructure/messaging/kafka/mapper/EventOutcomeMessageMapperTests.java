package io.github.georgecodes.betsettler.infrastructure.messaging.kafka.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.infrastructure.messaging.kafka.dto.EventOutcomeMessage;
import org.junit.jupiter.api.Test;

class EventOutcomeMessageMapperTests {

  private final EventOutcomeMessageMapper eventOutcomeMessageMapper =
      new EventOutcomeMessageMapper();

  @Test
  void toMessageMapsAllEventOutcomeFields() {
    EventOutcome eventOutcome = new EventOutcome("EVT-1001", "Team A vs Team B", "TEAM-A");

    EventOutcomeMessage message = eventOutcomeMessageMapper.toMessage(eventOutcome);

    assertThat(message)
        .isEqualTo(new EventOutcomeMessage("EVT-1001", "Team A vs Team B", "TEAM-A"));
  }

  @Test
  void toMessageRejectsNullEventOutcome() {
    assertThatThrownBy(() -> eventOutcomeMessageMapper.toMessage(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("eventOutcome must not be null");
  }

  @Test
  void toDomainMapsAllMessageFields() {
    EventOutcomeMessage message = new EventOutcomeMessage("EVT-1001", "Team A vs Team B", "TEAM-A");

    EventOutcome eventOutcome = eventOutcomeMessageMapper.toDomain(message);

    assertThat(eventOutcome).isEqualTo(new EventOutcome("EVT-1001", "Team A vs Team B", "TEAM-A"));
  }

  @Test
  void toDomainRejectsNullMessage() {
    assertThatThrownBy(() -> eventOutcomeMessageMapper.toDomain(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("eventOutcomeMessage must not be null");
  }
}
