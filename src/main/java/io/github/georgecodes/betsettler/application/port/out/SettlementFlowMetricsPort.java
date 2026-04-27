package io.github.georgecodes.betsettler.application.port.out;

import io.github.georgecodes.betsettler.application.dto.DemoResetSummary;

/** Records lightweight metrics for the event-outcome and settlement flow. */
@SuppressWarnings("unused")
public interface SettlementFlowMetricsPort {

  /** Supported high-level processing outcomes for a consumed event outcome. */
  enum EventOutcomeProcessingResult {
    /** The event outcome was skipped because it had already been processed. */
    DUPLICATE,

    /** The event outcome was processed, but no matching bets were found. */
    NO_MATCH,

    /** The event outcome was processed and produced one or more settlement dispatch attempts. */
    DISPATCHED
  }

  /** Records that the API accepted an event outcome for asynchronous publication. */
  void recordAcceptedEventOutcome();

  /**
   * Records the high-level result of processing a consumed event outcome.
   *
   * @param processingResult high-level processing result to record
   */
  void recordProcessedEventOutcome(EventOutcomeProcessingResult processingResult);

  /**
   * Records how many settlements were prepared for one consumed event outcome.
   *
   * @param settlementCount number of prepared settlements
   */
  void recordPreparedSettlements(int settlementCount);

  /**
   * Records the aggregate dispatch outcome for one consumed event outcome.
   *
   * @param sentCount number of successfully published settlement messages
   * @param failedCount number of settlement publications that failed
   */
  void recordSettlementDispatchCounts(int sentCount, int failedCount);

  /**
   * Records that demo-state reset was invoked and captures the reset summary.
   *
   * @param demoResetSummary summary of cleared and preserved demo-state rows
   */
  void recordDemoReset(DemoResetSummary demoResetSummary);
}
