package io.github.georgecodes.betsettler.infrastructure.persistence.mapper;

import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.infrastructure.persistence.entity.SettlementAuditEntity;
import io.github.georgecodes.betsettler.infrastructure.persistence.entity.SettlementPublishStatus;
import java.time.Instant;
import java.util.Objects;

/** Maps domain settlement decisions into persistence audit entities. */
@SuppressWarnings("unused")
public final class SettlementAuditEntityMapper {

  /** Prevents instantiation of the mapper utility type. */
  private SettlementAuditEntityMapper() {}

  /**
   * Creates a pending audit entity from a settlement decision.
   *
   * @param settlement settlement decision to audit
   * @param destinationTopic destination topic or channel name
   * @param payloadSnapshot serialized payload snapshot
   * @param createdAt creation timestamp
   * @return a pending settlement audit entity
   */
  public static SettlementAuditEntity toPendingEntity(
      BetSettlement settlement,
      String destinationTopic,
      String payloadSnapshot,
      Instant createdAt) {
    Objects.requireNonNull(settlement, "settlement must not be null");
    Objects.requireNonNull(destinationTopic, "destinationTopic must not be null");
    Objects.requireNonNull(payloadSnapshot, "payloadSnapshot must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");

    return new SettlementAuditEntity(
        settlement.eventOutcome().eventId(),
        settlement.bet().betId(),
        settlement.bet().userId(),
        settlement.settlementResult().name(),
        destinationTopic,
        payloadSnapshot,
        SettlementPublishStatus.PENDING,
        createdAt);
  }
}
