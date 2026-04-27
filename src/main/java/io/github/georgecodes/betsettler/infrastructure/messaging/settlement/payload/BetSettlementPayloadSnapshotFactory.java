package io.github.georgecodes.betsettler.infrastructure.messaging.settlement.payload;

import io.github.georgecodes.betsettler.application.port.out.BetSettlementPayloadPort;
import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.infrastructure.config.BetSettlerMessagingProperties;
import java.util.Objects;

/** Creates destination metadata and serialized payload snapshots for prepared settlements. */
public class BetSettlementPayloadSnapshotFactory implements BetSettlementPayloadPort {

  /** Externalized messaging properties. */
  private final BetSettlerMessagingProperties messagingProperties;

  /** Codec used to serialize settlement payload snapshots. */
  private final BetSettlementPayloadCodec betSettlementPayloadCodec;

  /**
   * Creates a new settlement payload snapshot factory.
   *
   * @param messagingProperties externalized messaging properties
   * @param betSettlementPayloadCodec codec used to create serialized payload snapshots
   */
  public BetSettlementPayloadSnapshotFactory(
      BetSettlerMessagingProperties messagingProperties,
      BetSettlementPayloadCodec betSettlementPayloadCodec) {
    this.messagingProperties =
        Objects.requireNonNull(messagingProperties, "messagingProperties must not be null");
    this.betSettlementPayloadCodec =
        Objects.requireNonNull(
            betSettlementPayloadCodec, "betSettlementPayloadCodec must not be null");
  }

  @Override
  public String destinationTopic() {
    return messagingProperties.rocketmq().betSettlementsTopic();
  }

  @Override
  public String payloadSnapshot(BetSettlement settlement) {
    Objects.requireNonNull(settlement, "settlement must not be null");
    return betSettlementPayloadCodec.payloadSnapshot(settlement);
  }
}
