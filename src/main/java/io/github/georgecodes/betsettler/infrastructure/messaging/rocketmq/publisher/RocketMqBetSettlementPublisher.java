package io.github.georgecodes.betsettler.infrastructure.messaging.rocketmq.publisher;

import static io.github.georgecodes.betsettler.domain.validation.ValidationSupport.requireNonBlank;

import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.dto.BetSettlementMessage;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.payload.BetSettlementPayloadCodec;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.payload.BetSettlementPayloadSnapshotFactory;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.publisher.AbstractBetSettlementPublisher;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;

/** Publishes settlement messages to RocketMQ. */
@Slf4j
public class RocketMqBetSettlementPublisher extends AbstractBetSettlementPublisher {

  /** RocketMQ template used to publish settlement payloads. */
  private final RocketMQTemplate rocketMqTemplate;

  /**
   * Creates a new RocketMQ-backed settlement publisher.
   *
   * @param rocketMqTemplate RocketMQ template used to publish settlement payloads
   * @param betSettlementPayloadSnapshotFactory shared settlement payload snapshot factory
   * @param betSettlementPayloadCodec shared settlement payload codec
   */
  public RocketMqBetSettlementPublisher(
      RocketMQTemplate rocketMqTemplate,
      BetSettlementPayloadSnapshotFactory betSettlementPayloadSnapshotFactory,
      BetSettlementPayloadCodec betSettlementPayloadCodec) {
    super(betSettlementPayloadSnapshotFactory, betSettlementPayloadCodec);
    this.rocketMqTemplate =
        Objects.requireNonNull(rocketMqTemplate, "rocketMqTemplate must not be null");
  }

  @Override
  public void publish(BetSettlement settlement) {
    Objects.requireNonNull(settlement, "settlement must not be null");
    BetSettlementMessage message = toMessage(settlement);
    log.info(
        "Publishing settlement via mode=rocketmq destinationTopic={} eventId={} betId={} userId={}",
        destinationTopic(),
        settlement.eventOutcome().eventId(),
        settlement.bet().betId(),
        settlement.bet().userId());
    rocketMqTemplate.convertAndSend(destinationTopic(), message);
  }

  @Override
  public void replayPayloadSnapshot(String destinationTopic, String payloadSnapshot) {
    String normalizedDestinationTopic = requireNonBlank(destinationTopic, "destinationTopic");
    BetSettlementMessage message = messageFromPayloadSnapshot(payloadSnapshot);
    log.info(
        "Replaying settlement payload via mode=rocketmq destinationTopic={} betId={} eventId={} userId={}",
        normalizedDestinationTopic,
        message.betId(),
        message.eventId(),
        message.userId());
    rocketMqTemplate.convertAndSend(normalizedDestinationTopic, message);
  }
}
