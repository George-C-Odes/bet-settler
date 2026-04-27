package io.github.georgecodes.betsettler.infrastructure.messaging.settlement.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.georgecodes.betsettler.domain.model.Bet;
import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.domain.model.SettlementResult;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.dto.BetSettlementMessage;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BetSettlementMessageMapperTests {

  private final BetSettlementMessageMapper betSettlementMessageMapper =
      new BetSettlementMessageMapper();

  @Test
  void toMessageMapsAllSettlementFields() {
    Bet bet =
        new Bet(
            "BET-1001",
            "USER-1",
            "EVT-1001",
            "MARKET-1",
            "TEAM-A",
            new BigDecimal("25.00"),
            Instant.parse("2026-04-23T10:15:30Z"));
    EventOutcome eventOutcome = new EventOutcome("EVT-1001", "Team A vs Team B", "TEAM-A");
    BetSettlement settlement = new BetSettlement(bet, eventOutcome, SettlementResult.WIN);

    BetSettlementMessage message = betSettlementMessageMapper.toMessage(settlement);

    assertThat(message)
        .isEqualTo(
            new BetSettlementMessage(
                "BET-1001",
                "USER-1",
                "EVT-1001",
                "Team A vs Team B",
                "MARKET-1",
                "TEAM-A",
                "TEAM-A",
                new BigDecimal("25.00"),
                "WIN"));
  }

  @Test
  void toMessageRejectsNullSettlement() {
    assertThatThrownBy(() -> betSettlementMessageMapper.toMessage(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("settlement must not be null");
  }
}
