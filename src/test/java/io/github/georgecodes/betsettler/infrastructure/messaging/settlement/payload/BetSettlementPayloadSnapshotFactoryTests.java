package io.github.georgecodes.betsettler.infrastructure.messaging.settlement.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.georgecodes.betsettler.domain.model.Bet;
import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.domain.model.SettlementResult;
import io.github.georgecodes.betsettler.infrastructure.config.BetSettlerMessagingProperties;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.mapper.BetSettlementMessageMapper;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BetSettlementPayloadSnapshotFactoryTests {

  private final BetSettlementPayloadSnapshotFactory betSettlementPayloadSnapshotFactory =
      new BetSettlementPayloadSnapshotFactory(
          new BetSettlerMessagingProperties(
              null,
              new BetSettlerMessagingProperties.RocketMq(
                  "bet-settlements-test", BetSettlerMessagingProperties.PublisherMode.LOGGING)),
          new BetSettlementPayloadCodec(
              new BetSettlementMessageMapper(), new ObjectMapper().findAndRegisterModules()));

  @Test
  void destinationTopicReturnsConfiguredTopic() {
    assertThat(betSettlementPayloadSnapshotFactory.destinationTopic())
        .isEqualTo("bet-settlements-test");
  }

  @Test
  void payloadSnapshotSerializesTheMappedSettlementMessage() {
    String payloadSnapshot =
        betSettlementPayloadSnapshotFactory.payloadSnapshot(createSettlement());

    assertThat(payloadSnapshot).contains("\"betId\":\"BET-1001\"");
    assertThat(payloadSnapshot).contains("\"settlementResult\":\"WIN\"");
  }

  @Test
  void payloadSnapshotRejectsSerializationFailures() throws JsonProcessingException {
    ObjectMapper failingObjectMapper = mock(ObjectMapper.class);
    doThrow(new JsonProcessingException("boom") {})
        .when(failingObjectMapper)
        .writeValueAsString(any());
    BetSettlementPayloadSnapshotFactory failingBetSettlementPayloadSnapshotFactory =
        new BetSettlementPayloadSnapshotFactory(
            new BetSettlerMessagingProperties(null, null),
            new BetSettlementPayloadCodec(new BetSettlementMessageMapper(), failingObjectMapper));

    assertThatThrownBy(
            () -> failingBetSettlementPayloadSnapshotFactory.payloadSnapshot(createSettlement()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Failed to serialize bet settlement payload for audit.");
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
