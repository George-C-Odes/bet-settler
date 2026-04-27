package io.github.georgecodes.betsettler.infrastructure.observability;

import io.github.georgecodes.betsettler.application.dto.DemoResetSummary;
import io.github.georgecodes.betsettler.application.port.out.SettlementFlowMetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Micrometer-backed metrics adapter for the event-outcome settlement flow. */
@Component
public class MicrometerSettlementFlowMetrics implements SettlementFlowMetricsPort {

  /** Tag name used for high-level event processing results. */
  private static final String RESULT_TAG = "result";

  /** Meter registry used to register counters and distribution summaries. */
  private final MeterRegistry meterRegistry;

  /**
   * Creates a new Micrometer-backed settlement-flow metrics adapter.
   *
   * @param meterRegistry meter registry used to publish counters and summaries
   */
  public MicrometerSettlementFlowMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
  }

  @Override
  public void recordAcceptedEventOutcome() {
    meterRegistry.counter("betsettler.event.outcomes.accepted").increment();
  }

  @Override
  public void recordProcessedEventOutcome(EventOutcomeProcessingResult processingResult) {
    Objects.requireNonNull(processingResult, "processingResult must not be null");
    meterRegistry
        .counter(
            "betsettler.event.outcomes.processed",
            RESULT_TAG,
            processingResult.name().toLowerCase(Locale.ROOT))
        .increment();
  }

  @Override
  public void recordPreparedSettlements(int settlementCount) {
    if (settlementCount < 0) {
      throw new IllegalArgumentException("settlementCount must not be negative");
    }
    meterRegistry.summary("betsettler.settlements.prepared").record(settlementCount);
  }

  @Override
  public void recordSettlementDispatchCounts(int sentCount, int failedCount) {
    recordSettlementDispatchCount("sent", sentCount);
    recordSettlementDispatchCount("failed", failedCount);
  }

  @Override
  public void recordDemoReset(DemoResetSummary demoResetSummary) {
    DemoResetSummary validatedSummary =
        Objects.requireNonNull(demoResetSummary, "demoResetSummary must not be null");
    meterRegistry.counter("betsettler.demo.reset.invocations").increment();
    meterRegistry
        .summary("betsettler.demo.reset.rows.cleared", "table", "processed_event_outcome")
        .record(validatedSummary.processedEventOutcomesCleared());
    meterRegistry
        .summary("betsettler.demo.reset.rows.cleared", "table", "settlement_audit")
        .record(validatedSummary.settlementAuditsCleared());
  }

  private void recordSettlementDispatchCount(String result, int count) {
    if (count < 0) {
      throw new IllegalArgumentException("count must not be negative");
    }
    if (count == 0) {
      return;
    }
    meterRegistry.counter("betsettler.settlements.dispatched", RESULT_TAG, result).increment(count);
  }
}
