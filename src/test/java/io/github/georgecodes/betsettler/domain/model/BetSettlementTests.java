package io.github.georgecodes.betsettler.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BetSettlementTests {

  @Test
  void constructorStoresValidSettlement() {
    Bet bet = validBet("EVT-1001", "TEAM-A");
    EventOutcome eventOutcome = new EventOutcome("EVT-1001", "Team A vs Team B", "TEAM-A");

    BetSettlement settlement = new BetSettlement(bet, eventOutcome, SettlementResult.WIN);

    assertThat(settlement.bet()).isEqualTo(bet);
    assertThat(settlement.eventOutcome()).isEqualTo(eventOutcome);
    assertThat(settlement.settlementResult()).isEqualTo(SettlementResult.WIN);
  }

  @Test
  void constructorRejectsNullBet() {
    EventOutcome eventOutcome = new EventOutcome("EVT-1001", "Team A vs Team B", "TEAM-A");

    assertThatNullPointerException()
        .isThrownBy(() -> new BetSettlement(null, eventOutcome, SettlementResult.WIN))
        .withMessage("bet must not be null");
  }

  @Test
  void constructorRejectsNullEventOutcome() {
    assertThatNullPointerException()
        .isThrownBy(
            () -> new BetSettlement(validBet("EVT-1001", "TEAM-A"), null, SettlementResult.WIN))
        .withMessage("eventOutcome must not be null");
  }

  @Test
  void constructorRejectsNullSettlementResult() {
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new BetSettlement(
                    validBet("EVT-1001", "TEAM-A"),
                    new EventOutcome("EVT-1001", "Team A vs Team B", "TEAM-A"),
                    null))
        .withMessage("settlementResult must not be null");
  }

  @Test
  void constructorRejectsMismatchedEventIdentifiers() {
    Bet bet = validBet("EVT-1001", "TEAM-A");
    EventOutcome eventOutcome = new EventOutcome("EVT-2002", "Team A vs Team B", "TEAM-A");

    assertThatIllegalArgumentException()
        .isThrownBy(() -> new BetSettlement(bet, eventOutcome, SettlementResult.WIN))
        .withMessage("bet.eventId and eventOutcome.eventId must match");
  }

  private Bet validBet(String eventId, String eventWinnerId) {
    return new Bet(
        "BET-1001",
        "USER-1",
        eventId,
        "MARKET-1",
        eventWinnerId,
        new BigDecimal("12.50"),
        Instant.parse("2026-04-22T10:15:30Z"));
  }
}
