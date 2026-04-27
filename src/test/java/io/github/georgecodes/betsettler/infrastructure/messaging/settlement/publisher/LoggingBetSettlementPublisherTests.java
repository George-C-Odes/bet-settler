package io.github.georgecodes.betsettler.infrastructure.messaging.settlement.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.georgecodes.betsettler.domain.model.Bet;
import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.domain.model.SettlementResult;
import io.github.georgecodes.betsettler.infrastructure.config.BetSettlerMessagingProperties;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.mapper.BetSettlementMessageMapper;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.payload.BetSettlementPayloadCodec;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.payload.BetSettlementPayloadSnapshotFactory;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LoggingBetSettlementPublisherTests {

  private final BetSettlementMessageMapper betSettlementMessageMapper =
      new BetSettlementMessageMapper();

  @Test
  void payloadSnapshotSerializesTheMappedSettlementMessage() {
    LoggingBetSettlementPublisher publisher =
        new LoggingBetSettlementPublisher(
            createPayloadSnapshotFactory(
                new BetSettlerMessagingProperties(
                    null,
                    new BetSettlerMessagingProperties.RocketMq(
                        "bet-settlements-test",
                        BetSettlerMessagingProperties.PublisherMode.LOGGING))),
            createPayloadCodec());

    String payloadSnapshot = publisher.payloadSnapshot(createSettlement());

    assertThat(payloadSnapshot).contains("\"betId\":\"BET-1001\"");
    assertThat(payloadSnapshot).contains("\"settlementResult\":\"WIN\"");
    assertThat(publisher.destinationTopic()).isEqualTo("bet-settlements-test");
  }

  @Test
  void publishLogsWithoutThrowing() {
    LoggingBetSettlementPublisher publisher =
        new LoggingBetSettlementPublisher(
            createPayloadSnapshotFactory(new BetSettlerMessagingProperties(null, null)),
            createPayloadCodec());

    assertThatCode(() -> publisher.publish(createSettlement())).doesNotThrowAnyException();
  }

  @Test
  void replayPayloadSnapshotLogsWithoutThrowing() {
    LoggingBetSettlementPublisher publisher =
        new LoggingBetSettlementPublisher(
            createPayloadSnapshotFactory(new BetSettlerMessagingProperties(null, null)),
            createPayloadCodec());

    assertThatCode(
            () ->
                publisher.replayPayloadSnapshot(
                    "bet-settlements-retry", "{\"betId\":\"BET-1001\"}"))
        .doesNotThrowAnyException();
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

  private BetSettlementPayloadSnapshotFactory createPayloadSnapshotFactory(
      BetSettlerMessagingProperties messagingProperties) {
    return new BetSettlementPayloadSnapshotFactory(messagingProperties, createPayloadCodec());
  }

  private BetSettlementPayloadCodec createPayloadCodec() {
    return new BetSettlementPayloadCodec(
        betSettlementMessageMapper, new ObjectMapper().findAndRegisterModules());
  }
}
