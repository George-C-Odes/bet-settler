package io.github.georgecodes.betsettler.application.service;

import io.github.georgecodes.betsettler.application.dto.DemoResetSummary;
import io.github.georgecodes.betsettler.application.port.in.ResetDemoStateUseCase;
import io.github.georgecodes.betsettler.application.port.out.DemoStateResetPort;
import io.github.georgecodes.betsettler.application.port.out.SettlementFlowMetricsPort;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service that resets transient demo state for local and test profiles. */
@Slf4j
@Service
@Transactional
public class ResetDemoStateService implements ResetDemoStateUseCase {

  /** Port used to clear transient demo state. */
  private final DemoStateResetPort demoStateResetPort;

  /** Metrics port used to expose demo reset activity. */
  private final SettlementFlowMetricsPort settlementFlowMetricsPort;

  /**
   * Creates a new reset demo state application service.
   *
   * @param demoStateResetPort port used to clear transient demo state
   * @param settlementFlowMetricsPort metrics port used to expose demo reset activity
   */
  public ResetDemoStateService(
      DemoStateResetPort demoStateResetPort, SettlementFlowMetricsPort settlementFlowMetricsPort) {
    this.demoStateResetPort =
        Objects.requireNonNull(demoStateResetPort, "demoStateResetPort must not be null");
    this.settlementFlowMetricsPort =
        Objects.requireNonNull(
            settlementFlowMetricsPort, "settlementFlowMetricsPort must not be null");
  }

  @Override
  public DemoResetSummary resetDemoState() {
    DemoResetSummary demoResetSummary = demoStateResetPort.resetDemoState();
    settlementFlowMetricsPort.recordDemoReset(demoResetSummary);
    log.info(
        "Reset demo state processedEventOutcomesCleared={} settlementAuditsCleared={} seededBetsPreserved={} totalClearedRows={}",
        demoResetSummary.processedEventOutcomesCleared(),
        demoResetSummary.settlementAuditsCleared(),
        demoResetSummary.seededBetsPreserved(),
        demoResetSummary.totalClearedRows());
    return demoResetSummary;
  }
}
