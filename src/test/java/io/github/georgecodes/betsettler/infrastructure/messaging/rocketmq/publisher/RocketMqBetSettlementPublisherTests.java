package io.github.georgecodes.betsettler.infrastructure.messaging.rocketmq.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.georgecodes.betsettler.domain.model.Bet;
import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.domain.model.SettlementResult;
import io.github.georgecodes.betsettler.infrastructure.config.BetSettlerMessagingProperties;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.dto.BetSettlementMessage;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.mapper.BetSettlementMessageMapper;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.payload.BetSettlementPayloadCodec;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.payload.BetSettlementPayloadSnapshotFactory;
import java.math.BigDecimal;
import java.time.Instant;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RocketMqBetSettlementPublisherTests {

  @Mock private RocketMQTemplate rocketMqTemplate;

  private final BetSettlementMessageMapper betSettlementMessageMapper =
      new BetSettlementMessageMapper();

  @Test
  void publishSendsMappedSettlementMessageToConfiguredTopic() {
    RocketMqBetSettlementPublisher publisher =
        new RocketMqBetSettlementPublisher(
            rocketMqTemplate,
            createPayloadSnapshotFactory(
                new BetSettlerMessagingProperties(
                    null,
                    new BetSettlerMessagingProperties.RocketMq(
                        "bet-settlements-test",
                        BetSettlerMessagingProperties.PublisherMode.ROCKETMQ))),
            createPayloadCodec());
    BetSettlement settlement = createSettlement();

    publisher.publish(settlement);

    verify(rocketMqTemplate)
        .convertAndSend(
            "bet-settlements-test",
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
  void payloadSnapshotSerializesTheMappedSettlementMessage() {
    RocketMqBetSettlementPublisher publisher =
        new RocketMqBetSettlementPublisher(
            rocketMqTemplate,
            createPayloadSnapshotFactory(
                new BetSettlerMessagingProperties(
                    null,
                    new BetSettlerMessagingProperties.RocketMq(
                        "bet-settlements-test",
                        BetSettlerMessagingProperties.PublisherMode.ROCKETMQ))),
            createPayloadCodec());

    String payloadSnapshot = publisher.payloadSnapshot(createSettlement());

    assertThat(payloadSnapshot).contains("\"betId\":\"BET-1001\"");
    assertThat(payloadSnapshot).contains("\"settlementResult\":\"WIN\"");
    assertThat(publisher.destinationTopic()).isEqualTo("bet-settlements-test");
  }

  @Test
  void replayPayloadSnapshotReplaysTheSerializedSettlementMessage() {
    RocketMqBetSettlementPublisher publisher =
        new RocketMqBetSettlementPublisher(
            rocketMqTemplate,
            createPayloadSnapshotFactory(
                new BetSettlerMessagingProperties(
                    null,
                    new BetSettlerMessagingProperties.RocketMq(
                        "bet-settlements-test",
                        BetSettlerMessagingProperties.PublisherMode.ROCKETMQ))),
            createPayloadCodec());

    publisher.replayPayloadSnapshot(
        "bet-settlements-retry",
        """
                {"betId":"BET-1001","userId":"USER-1","eventId":"EVT-1001","eventName":"Team A vs Team B","eventMarketId":"MARKET-1","predictedWinnerId":"TEAM-A","actualWinnerId":"TEAM-A","betAmount":25.00,"settlementResult":"WIN"}
        """);

    verify(rocketMqTemplate)
        .convertAndSend(
            "bet-settlements-retry",
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
  void replayPayloadSnapshotRejectsMalformedPayloadSnapshots() {
    RocketMqBetSettlementPublisher publisher =
        new RocketMqBetSettlementPublisher(
            rocketMqTemplate,
            createPayloadSnapshotFactory(
                new BetSettlerMessagingProperties(
                    null,
                    new BetSettlerMessagingProperties.RocketMq(
                        "bet-settlements-test",
                        BetSettlerMessagingProperties.PublisherMode.ROCKETMQ))),
            createPayloadCodec());

    assertThatThrownBy(() -> publisher.replayPayloadSnapshot("bet-settlements-retry", "{not-json}"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Failed to deserialize bet settlement payload snapshot for retry.");
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
