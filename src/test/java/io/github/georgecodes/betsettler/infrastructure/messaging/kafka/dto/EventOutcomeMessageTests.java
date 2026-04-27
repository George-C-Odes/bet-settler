package io.github.georgecodes.betsettler.infrastructure.messaging.kafka.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EventOutcomeMessageTests {

  @Test
  void constructorTrimsAndStoresAllFields() {
    EventOutcomeMessage message =
        new EventOutcomeMessage(" EVT-1001 ", " Team A vs Team B ", " TEAM-A ");

    assertThat(message.eventId()).isEqualTo("EVT-1001");
    assertThat(message.eventName()).isEqualTo("Team A vs Team B");
    assertThat(message.eventWinnerId()).isEqualTo("TEAM-A");
  }

  @Test
  void constructorRejectsBlankWinnerId() {
    assertThatThrownBy(() -> new EventOutcomeMessage("EVT-1001", "Team A vs Team B", "   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("eventWinnerId must not be blank");
  }
}
