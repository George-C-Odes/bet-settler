package io.github.georgecodes.betsettler.application.service;

import static io.github.georgecodes.betsettler.domain.validation.ValidationSupport.requireNonBlank;

import io.github.georgecodes.betsettler.application.model.audit.PendingSettlementAudit;
import io.github.georgecodes.betsettler.application.model.dispatch.PreparedDispatchPlan;
import io.github.georgecodes.betsettler.application.port.out.BetQueryPort;
import io.github.georgecodes.betsettler.application.port.out.BetSettlementPayloadPort;
import io.github.georgecodes.betsettler.application.port.out.ProcessedEventOutcomePort;
import io.github.georgecodes.betsettler.application.port.out.SettlementAuditPort;
import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.domain.service.BetSettlementDecider;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Prepares deduplicated settlement dispatch plans inside the transactional application stage. */
@Service
public class SettlementDispatchPreparationService {

  /** Port used to load matching bets for the processed event. */
  private final BetQueryPort betQueryPort;

  /** Port used to deduplicate processed event outcomes. */
  private final ProcessedEventOutcomePort processedEventOutcomePort;

  /** Port used to persist pending settlement-audit rows. */
  private final SettlementAuditPort settlementAuditPort;

  /** Port used to derive destination metadata and payload snapshots for prepared settlements. */
  private final BetSettlementPayloadPort betSettlementPayloadPort;

  /** Stateless domain service used to decide settlement outcomes. */
  private final BetSettlementDecider betSettlementDecider;

  /**
   * Creates a new settlement-dispatch preparation service.
   *
   * @param betQueryPort port used to load matching bets
   * @param processedEventOutcomePort port used to deduplicate processed outcomes
   * @param settlementAuditPort port used to persist pending settlement audits
   * @param betSettlementPayloadPort port used to resolve destination metadata and payloads
   * @param betSettlementDecider domain service used to decide settlement outcomes
   */
  public SettlementDispatchPreparationService(
      BetQueryPort betQueryPort,
      ProcessedEventOutcomePort processedEventOutcomePort,
      SettlementAuditPort settlementAuditPort,
      BetSettlementPayloadPort betSettlementPayloadPort,
      BetSettlementDecider betSettlementDecider) {
    this.betQueryPort = Objects.requireNonNull(betQueryPort, "betQueryPort must not be null");
    this.processedEventOutcomePort =
        Objects.requireNonNull(
            processedEventOutcomePort, "processedEventOutcomePort must not be null");
    this.settlementAuditPort =
        Objects.requireNonNull(settlementAuditPort, "settlementAuditPort must not be null");
    this.betSettlementPayloadPort =
        Objects.requireNonNull(
            betSettlementPayloadPort, "betSettlementPayloadPort must not be null");
    this.betSettlementDecider =
        Objects.requireNonNull(betSettlementDecider, "betSettlementDecider must not be null");
  }

  /**
   * Prepares the deduplicated settlement dispatch plan for a consumed event outcome.
   *
   * @param eventOutcome consumed event outcome to prepare
   * @return immutable plan describing prepared settlements and duplicate state
   */
  public PreparedDispatchPlan prepare(EventOutcome eventOutcome) {
    Objects.requireNonNull(eventOutcome, "eventOutcome must not be null");
    String eventId = eventOutcome.eventId();
    if (!processedEventOutcomePort.recordProcessedIfAbsent(eventOutcome)) {
      return PreparedDispatchPlan.duplicate(eventId);
    }

    String destinationTopic =
        requireNonBlank(betSettlementPayloadPort.destinationTopic(), "destinationTopic");
    List<BetSettlement> settlements =
        betQueryPort.getBetsByEventId(eventId).stream()
            .map(bet -> betSettlementDecider.settle(bet, eventOutcome))
            .toList();

    if (settlements.isEmpty()) {
      return PreparedDispatchPlan.noMatch(eventId, destinationTopic);
    }

    settlementAuditPort.savePendingSettlements(
        settlements.stream()
            .map(
                settlement ->
                    new PendingSettlementAudit(
                        settlement,
                        destinationTopic,
                        betSettlementPayloadPort.payloadSnapshot(settlement)))
            .toList());

    return PreparedDispatchPlan.dispatch(eventId, destinationTopic, settlements);
  }
}
