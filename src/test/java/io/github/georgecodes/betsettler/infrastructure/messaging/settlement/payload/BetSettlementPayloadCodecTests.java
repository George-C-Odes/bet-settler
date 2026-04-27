package io.github.georgecodes.betsettler.infrastructure.messaging.settlement.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.georgecodes.betsettler.domain.model.Bet;
import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.domain.model.SettlementResult;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.dto.BetSettlementMessage;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.mapper.BetSettlementMessageMapper;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BetSettlementPayloadCodecTests {

  private final BetSettlementPayloadCodec betSettlementPayloadCodec =
      new BetSettlementPayloadCodec(
          new BetSettlementMessageMapper(), new ObjectMapper().findAndRegisterModules());

  @Test
  void toMessageMapsTheSettlementToTheSharedTransportContract() {
    assertThat(betSettlementPayloadCodec.toMessage(createSettlement()))
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
  void messageFromPayloadSnapshotRestoresTheSerializedSettlementMessage() {
    BetSettlementMessage message =
        betSettlementPayloadCodec.messageFromPayloadSnapshot(
            """
            {"betId":"BET-1001","userId":"USER-1","eventId":"EVT-1001","eventName":"Team A vs Team B","eventMarketId":"MARKET-1","predictedWinnerId":"TEAM-A","actualWinnerId":"TEAM-A","betAmount":25.00,"settlementResult":"WIN"}
            """);

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
  void messageFromPayloadSnapshotRejectsMalformedPayloadSnapshots() {
    assertThatThrownBy(() -> betSettlementPayloadCodec.messageFromPayloadSnapshot("{not-json}"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Failed to deserialize bet settlement payload snapshot for retry.");
  }

  @Test
  void messageFromPayloadSnapshotRejectsBlankValues() {
    assertThatThrownBy(() -> betSettlementPayloadCodec.messageFromPayloadSnapshot("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("payloadSnapshot must not be blank");
  }

  private BetSettlement createSettlement() {
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
    return new BetSettlement(bet, eventOutcome, SettlementResult.WIN);
  }
}
