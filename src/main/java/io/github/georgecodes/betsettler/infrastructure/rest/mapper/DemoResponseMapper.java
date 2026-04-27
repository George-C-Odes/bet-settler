package io.github.georgecodes.betsettler.infrastructure.rest.mapper;

import io.github.georgecodes.betsettler.application.dto.DemoResetSummary;
import io.github.georgecodes.betsettler.application.dto.SettlementDispatchRetrySummary;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.DemoResetResponse;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.SettlementDispatchRetryResponse;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Maps demo-helper use-case summaries into REST response payloads. */
@Component
public class DemoResponseMapper {

  /** Creates a new demo-response mapper. */
  public DemoResponseMapper() {}

  /**
   * Creates the success-response payload for a demo reset request.
   *
   * @param demoResetSummary summary returned by the reset use case
   * @return success-response payload
   */
  public DemoResetResponse toDemoResetResponse(DemoResetSummary demoResetSummary) {
    Objects.requireNonNull(demoResetSummary, "demoResetSummary must not be null");
    return new DemoResetResponse(
        "RESET",
        "Demo settlement state has been reset while preserving seeded bets.",
        demoResetSummary.processedEventOutcomesCleared(),
        demoResetSummary.settlementAuditsCleared(),
        demoResetSummary.seededBetsPreserved());
  }

  /**
   * Creates the success-response payload for a settlement-dispatch retry request.
   *
   * @param retrySummary summary returned by the retry use case
   * @return success-response payload
   */
  public SettlementDispatchRetryResponse toSettlementDispatchRetryResponse(
      SettlementDispatchRetrySummary retrySummary) {
    Objects.requireNonNull(retrySummary, "retrySummary must not be null");
    return new SettlementDispatchRetryResponse(
        retrySummary.hasFailures() ? "PARTIAL" : "RETRIED",
        retrySummary.hasFailures()
            ? "Retried settlement dispatches with one or more remaining failures."
            : "Retried settlement dispatches successfully.",
        retrySummary.retriedAuditCount(),
        retrySummary.sentCount(),
        retrySummary.failedCount());
  }
}
