package io.github.georgecodes.betsettler.infrastructure.messaging.settlement.publisher;

import io.github.georgecodes.betsettler.application.port.out.BetSettlementDispatchPort;
import io.github.georgecodes.betsettler.application.port.out.BetSettlementReplayPort;
import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.dto.BetSettlementMessage;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.payload.BetSettlementPayloadCodec;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.payload.BetSettlementPayloadSnapshotFactory;
import java.util.Objects;

/** Provides shared destination and payload snapshot behavior for settlement publishers. */
public abstract class AbstractBetSettlementPublisher
    implements BetSettlementDispatchPort, BetSettlementReplayPort {

  /** Shared settlement payload snapshot factory. */
  private final BetSettlementPayloadSnapshotFactory betSettlementPayloadSnapshotFactory;

  /** Shared settlement payload codec. */
  private final BetSettlementPayloadCodec betSettlementPayloadCodec;

  /**
   * Creates a new shared settlement publisher base.
   *
   * @param betSettlementPayloadSnapshotFactory shared settlement payload snapshot factory
   * @param betSettlementPayloadCodec shared settlement payload codec
   */
  protected AbstractBetSettlementPublisher(
      BetSettlementPayloadSnapshotFactory betSettlementPayloadSnapshotFactory,
      BetSettlementPayloadCodec betSettlementPayloadCodec) {
    this.betSettlementPayloadSnapshotFactory =
        Objects.requireNonNull(
            betSettlementPayloadSnapshotFactory,
            "betSettlementPayloadSnapshotFactory must not be null");
    this.betSettlementPayloadCodec =
        Objects.requireNonNull(
            betSettlementPayloadCodec, "betSettlementPayloadCodec must not be null");
  }

  /**
   * Returns the configured logical destination used for settlement publications.
   *
   * @return configured logical destination used for settlement publications
   */
  public final String destinationTopic() {
    return betSettlementPayloadSnapshotFactory.destinationTopic();
  }

  /**
   * Returns the serialized payload snapshot that should be stored in the settlement audit.
   *
   * @param settlement settlement decision to serialize
   * @return serialized payload snapshot
   */
  public final String payloadSnapshot(BetSettlement settlement) {
    return betSettlementPayloadSnapshotFactory.payloadSnapshot(settlement);
  }

  /**
   * Maps the supplied settlement into the shared settlement transport payload.
   *
   * @param settlement settlement decision to map
   * @return mapped transport payload
   */
  protected final BetSettlementMessage toMessage(BetSettlement settlement) {
    return betSettlementPayloadCodec.toMessage(settlement);
  }

  /**
   * Restores a stored settlement payload snapshot into the shared settlement transport DTO.
   *
   * @param payloadSnapshot serialized payload snapshot stored with the settlement audit
   * @return deserialized transport payload
   */
  protected final BetSettlementMessage messageFromPayloadSnapshot(String payloadSnapshot) {
    return betSettlementPayloadCodec.messageFromPayloadSnapshot(payloadSnapshot);
  }
}
