package io.github.georgecodes.betsettler.application.model.audit;

import static io.github.georgecodes.betsettler.domain.validation.ValidationSupport.requireNonBlank;

import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import java.util.Objects;

/**
 * Carries the data required to persist a pending settlement audit row.
 *
 * @param settlement prepared settlement decision to audit
 * @param destinationTopic destination topic or channel name
 * @param payloadSnapshot serialized payload snapshot for audit purposes
 */
@SuppressWarnings("unused")
public record PendingSettlementAudit(
    BetSettlement settlement, String destinationTopic, String payloadSnapshot) {

  /**
   * Creates a validated pending settlement audit descriptor.
   *
   * @param settlement prepared settlement decision to audit
   * @param destinationTopic destination topic or channel name
   * @param payloadSnapshot serialized payload snapshot for audit purposes
   */
  @SuppressWarnings("unused")
  public PendingSettlementAudit {
    Objects.requireNonNull(settlement, "settlement must not be null");
    destinationTopic = requireNonBlank(destinationTopic, "destinationTopic");
    payloadSnapshot = requireNonBlank(payloadSnapshot, "payloadSnapshot");
  }
}
