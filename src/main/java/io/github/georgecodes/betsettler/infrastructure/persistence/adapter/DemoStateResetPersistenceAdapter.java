package io.github.georgecodes.betsettler.infrastructure.persistence.adapter;

import io.github.georgecodes.betsettler.application.dto.DemoResetSummary;
import io.github.georgecodes.betsettler.application.port.out.DemoStateResetPort;
import io.github.georgecodes.betsettler.infrastructure.persistence.repository.SpringDataBetRepository;
import io.github.georgecodes.betsettler.infrastructure.persistence.repository.SpringDataProcessedEventOutcomeRepository;
import io.github.georgecodes.betsettler.infrastructure.persistence.repository.SpringDataSettlementAuditRepository;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Persistence-backed implementation of the demo state reset port. */
@Component
public class DemoStateResetPersistenceAdapter implements DemoStateResetPort {

  /** Repository used to clear processed event outcome state. */
  private final SpringDataProcessedEventOutcomeRepository processedEventOutcomeRepository;

  /** Repository used to clear settlement audit state. */
  private final SpringDataSettlementAuditRepository settlementAuditRepository;

  /** Repository used to confirm seeded bet rows remain available after reset. */
  private final SpringDataBetRepository betRepository;

  /**
   * Creates a new persistence-backed demo state reset adapter.
   *
   * @param processedEventOutcomeRepository repository used to clear processed outcome state
   * @param settlementAuditRepository repository used to clear settlement audit state
   * @param betRepository repository used to confirm seeded bet state remains available
   */
  public DemoStateResetPersistenceAdapter(
      SpringDataProcessedEventOutcomeRepository processedEventOutcomeRepository,
      SpringDataSettlementAuditRepository settlementAuditRepository,
      SpringDataBetRepository betRepository) {
    this.processedEventOutcomeRepository =
        Objects.requireNonNull(
            processedEventOutcomeRepository, "processedEventOutcomeRepository must not be null");
    this.settlementAuditRepository =
        Objects.requireNonNull(
            settlementAuditRepository, "settlementAuditRepository must not be null");
    this.betRepository = Objects.requireNonNull(betRepository, "betRepository must not be null");
  }

  @Override
  public DemoResetSummary resetDemoState() {
    long processedEventOutcomesCleared = processedEventOutcomeRepository.count();
    long settlementAuditsCleared = settlementAuditRepository.count();
    settlementAuditRepository.deleteAllInBatch();
    processedEventOutcomeRepository.deleteAllInBatch();
    return new DemoResetSummary(
        processedEventOutcomesCleared, settlementAuditsCleared, betRepository.count());
  }
}
