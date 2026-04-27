package io.github.georgecodes.betsettler.application.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import org.junit.jupiter.api.Test;

class PublishEventOutcomeCommandTests {

  @Test
  void constructorTrimsAndStoresValidValues() {
    PublishEventOutcomeCommand command =
        new PublishEventOutcomeCommand(" EVT-1001 ", " Team A vs Team B ", " TEAM-A ");

    assertThat(command.eventId()).isEqualTo("EVT-1001");
    assertThat(command.eventName()).isEqualTo("Team A vs Team B");
    assertThat(command.eventWinnerId()).isEqualTo("TEAM-A");
  }

  @Test
  void toEventOutcomeCreatesDomainEventOutcome() {
    PublishEventOutcomeCommand command =
        new PublishEventOutcomeCommand("EVT-1001", "Team A vs Team B", "TEAM-A");

    EventOutcome eventOutcome = command.toEventOutcome();

    assertThat(eventOutcome.eventId()).isEqualTo("EVT-1001");
    assertThat(eventOutcome.eventName()).isEqualTo("Team A vs Team B");
    assertThat(eventOutcome.eventWinnerId()).isEqualTo("TEAM-A");
  }

  @Test
  void constructorRejectsNullEventName() {
    assertThatNullPointerException()
        .isThrownBy(() -> new PublishEventOutcomeCommand("EVT-1001", null, "TEAM-A"))
        .withMessage("eventName must not be null");
  }

  @Test
  void constructorRejectsBlankEventWinnerId() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new PublishEventOutcomeCommand("EVT-1001", "Team A vs Team B", "   "))
        .withMessage("eventWinnerId must not be blank");
  }
}
