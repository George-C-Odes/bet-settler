package io.github.georgecodes.betsettler.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class EventOutcomeTests {

  @Test
  void constructorTrimsAndStoresValidValues() {
    EventOutcome eventOutcome = new EventOutcome(" EVT-1001 ", " Team A vs Team B ", " TEAM-A ");

    assertThat(eventOutcome.eventId()).isEqualTo("EVT-1001");
    assertThat(eventOutcome.eventName()).isEqualTo("Team A vs Team B");
    assertThat(eventOutcome.eventWinnerId()).isEqualTo("TEAM-A");
  }

  @Test
  void constructorRejectsNullEventId() {
    assertThatNullPointerException()
        .isThrownBy(() -> new EventOutcome(null, "Team A vs Team B", "TEAM-A"))
        .withMessage("eventId must not be null");
  }

  @Test
  void constructorRejectsBlankEventWinnerId() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new EventOutcome("EVT-1001", "Team A vs Team B", "   "))
        .withMessage("eventWinnerId must not be blank");
  }
}
