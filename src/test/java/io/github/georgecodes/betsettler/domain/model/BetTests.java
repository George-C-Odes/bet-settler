package io.github.georgecodes.betsettler.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BetTests {

  @Test
  void constructorTrimsAndStoresValidValues() {
    Bet bet =
        new Bet(
            " BET-1001 ",
            " USER-1 ",
            " EVT-1001 ",
            " MARKET-1 ",
            " TEAM-A ",
            new BigDecimal("12.50"),
            Instant.parse("2026-04-22T10:15:30Z"));

    assertThat(bet.betId()).isEqualTo("BET-1001");
    assertThat(bet.userId()).isEqualTo("USER-1");
    assertThat(bet.eventId()).isEqualTo("EVT-1001");
    assertThat(bet.eventMarketId()).isEqualTo("MARKET-1");
    assertThat(bet.eventWinnerId()).isEqualTo("TEAM-A");
    assertThat(bet.betAmount()).isEqualByComparingTo("12.50");
    assertThat(bet.createdAt()).isEqualTo(Instant.parse("2026-04-22T10:15:30Z"));
  }

  @Test
  void constructorRejectsNullBetId() {
    assertThatNullPointerException()
        .isThrownBy(() -> createBet(null, "USER-1", "EVT-1001", "MARKET-1", "TEAM-A"))
        .withMessage("betId must not be null");
  }

  @Test
  void constructorRejectsBlankUserId() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> createBet("BET-1001", "   ", "EVT-1001", "MARKET-1", "TEAM-A"))
        .withMessage("userId must not be blank");
  }

  @Test
  void constructorRejectsNonPositiveBetAmount() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new Bet(
                    "BET-1001",
                    "USER-1",
                    "EVT-1001",
                    "MARKET-1",
                    "TEAM-A",
                    BigDecimal.ZERO,
                    Instant.parse("2026-04-22T10:15:30Z")))
        .withMessage("betAmount must be greater than zero");
  }

  @Test
  void constructorRejectsNullCreationTimestamp() {
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new Bet(
                    "BET-1001",
                    "USER-1",
                    "EVT-1001",
                    "MARKET-1",
                    "TEAM-A",
                    new BigDecimal("12.50"),
                    null))
        .withMessage("createdAt must not be null");
  }

  private Bet createBet(
      String betId, String userId, String eventId, String eventMarketId, String eventWinnerId) {
    return new Bet(
        betId,
        userId,
        eventId,
        eventMarketId,
        eventWinnerId,
        new BigDecimal("12.50"),
        Instant.parse("2026-04-22T10:15:30Z"));
  }
}
