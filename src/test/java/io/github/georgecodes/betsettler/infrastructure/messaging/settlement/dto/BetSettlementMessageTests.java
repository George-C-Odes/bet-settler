package io.github.georgecodes.betsettler.infrastructure.messaging.settlement.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BetSettlementMessageTests {

  @Test
  void constructorTrimsAndStoresAllFields() {
    BetSettlementMessage message =
        new BetSettlementMessage(
            " BET-1001 ",
            " USER-1 ",
            " EVT-1001 ",
            " Team A vs Team B ",
            " MARKET-1 ",
            " TEAM-A ",
            " TEAM-A ",
            new BigDecimal("25.00"),
            " WIN ");

    assertThat(message.betId()).isEqualTo("BET-1001");
    assertThat(message.userId()).isEqualTo("USER-1");
    assertThat(message.eventId()).isEqualTo("EVT-1001");
    assertThat(message.eventName()).isEqualTo("Team A vs Team B");
    assertThat(message.eventMarketId()).isEqualTo("MARKET-1");
    assertThat(message.predictedWinnerId()).isEqualTo("TEAM-A");
    assertThat(message.actualWinnerId()).isEqualTo("TEAM-A");
    assertThat(message.betAmount()).isEqualByComparingTo("25.00");
    assertThat(message.settlementResult()).isEqualTo("WIN");
  }

  @Test
  void constructorRejectsNonPositiveBetAmount() {
    assertThatThrownBy(
            () ->
                new BetSettlementMessage(
                    "BET-1001",
                    "USER-1",
                    "EVT-1001",
                    "Team A vs Team B",
                    "MARKET-1",
                    "TEAM-A",
                    "TEAM-A",
                    BigDecimal.ZERO,
                    "WIN"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("betAmount must be greater than zero");
  }
}
