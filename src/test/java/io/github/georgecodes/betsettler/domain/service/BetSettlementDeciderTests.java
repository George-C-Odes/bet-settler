package io.github.georgecodes.betsettler.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.georgecodes.betsettler.domain.model.Bet;
import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.domain.model.SettlementResult;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BetSettlementDeciderTests {

  private final BetSettlementDecider decider = new BetSettlementDecider();

  @Test
  void decideReturnsWinWhenWinnerMatches() {
    SettlementResult result = decider.decide(validBet("TEAM-A"), validEventOutcome("TEAM-A"));

    assertThat(result).isEqualTo(SettlementResult.WIN);
  }

  @Test
  void decideReturnsLoseWhenWinnerDiffers() {
    SettlementResult result = decider.decide(validBet("TEAM-B"), validEventOutcome("TEAM-A"));

    assertThat(result).isEqualTo(SettlementResult.LOSE);
  }

  @Test
  void settleCreatesSettlementWithDecidedResult() {
    Bet bet = validBet("TEAM-A");
    EventOutcome eventOutcome = validEventOutcome("TEAM-A");

    BetSettlement settlement = decider.settle(bet, eventOutcome);

    assertThat(settlement.bet()).isEqualTo(bet);
    assertThat(settlement.eventOutcome()).isEqualTo(eventOutcome);
    assertThat(settlement.settlementResult()).isEqualTo(SettlementResult.WIN);
  }

  @Test
  void decideRejectsNullBet() {
    assertThatNullPointerException()
        .isThrownBy(() -> decider.decide(null, validEventOutcome("TEAM-A")))
        .withMessage("bet must not be null");
  }

  @Test
  void decideRejectsNullEventOutcome() {
    assertThatNullPointerException()
        .isThrownBy(() -> decider.decide(validBet("TEAM-A"), null))
        .withMessage("eventOutcome must not be null");
  }

  @Test
  void decideRejectsMismatchedEventIdentifiers() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> decider.decide(validBet("TEAM-A"), eventOutcomeForEvent("EVT-9999", "TEAM-A")))
        .withMessage("bet.eventId and eventOutcome.eventId must match");
  }

  private Bet validBet(String eventWinnerId) {
    return betForEvent("EVT-1001", eventWinnerId);
  }

  private Bet betForEvent(String eventId, String eventWinnerId) {
    return new Bet(
        "BET-1001",
        "USER-1",
        eventId,
        "MARKET-1",
        eventWinnerId,
        new BigDecimal("12.50"),
        Instant.parse("2026-04-22T10:15:30Z"));
  }

  private EventOutcome validEventOutcome(String eventWinnerId) {
    return eventOutcomeForEvent("EVT-1001", eventWinnerId);
  }

  private EventOutcome eventOutcomeForEvent(String eventId, String eventWinnerId) {
    return new EventOutcome(eventId, "Team A vs Team B", eventWinnerId);
  }
}
