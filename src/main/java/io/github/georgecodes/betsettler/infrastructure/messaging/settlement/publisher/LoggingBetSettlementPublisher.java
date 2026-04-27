package io.github.georgecodes.betsettler.infrastructure.messaging.settlement.publisher;

import static io.github.georgecodes.betsettler.domain.validation.ValidationSupport.requireNonBlank;

import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.payload.BetSettlementPayloadCodec;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.payload.BetSettlementPayloadSnapshotFactory;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/** Logs settlement payloads when RocketMQ publication is not available. */
@Slf4j
public class LoggingBetSettlementPublisher extends AbstractBetSettlementPublisher {

  /**
   * Creates a new logging settlement publisher.
   *
   * @param betSettlementPayloadSnapshotFactory shared settlement payload snapshot factory
   * @param betSettlementPayloadCodec shared settlement payload codec
   */
  public LoggingBetSettlementPublisher(
      BetSettlementPayloadSnapshotFactory betSettlementPayloadSnapshotFactory,
      BetSettlementPayloadCodec betSettlementPayloadCodec) {
    super(betSettlementPayloadSnapshotFactory, betSettlementPayloadCodec);
  }

  @Override
  public void publish(BetSettlement settlement) {
    Objects.requireNonNull(settlement, "settlement must not be null");
    log.info(
        "Publishing settlement via fallback mode=logging destinationTopic={} eventId={} betId={} userId={} payload={}",
        destinationTopic(),
        settlement.eventOutcome().eventId(),
        settlement.bet().betId(),
        settlement.bet().userId(),
        payloadSnapshot(settlement));
  }

  @Override
  public void replayPayloadSnapshot(String destinationTopic, String payloadSnapshot) {
    log.info(
        "Replaying settlement payload via fallback mode=logging destinationTopic={} payload={}",
        requireNonBlank(destinationTopic, "destinationTopic"),
        requireNonBlank(payloadSnapshot, "payloadSnapshot"));
  }
}
