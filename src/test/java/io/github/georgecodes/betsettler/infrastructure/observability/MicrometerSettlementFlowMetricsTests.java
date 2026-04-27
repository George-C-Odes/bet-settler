package io.github.georgecodes.betsettler.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.georgecodes.betsettler.application.dto.DemoResetSummary;
import io.github.georgecodes.betsettler.application.port.out.SettlementFlowMetricsPort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MicrometerSettlementFlowMetricsTests {

  private SimpleMeterRegistry meterRegistry;

  private MicrometerSettlementFlowMetrics metrics;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    metrics = new MicrometerSettlementFlowMetrics(meterRegistry);
  }

  @Test
  void recordsAllSupportedSettlementFlowMetrics() {
    metrics.recordAcceptedEventOutcome();
    metrics.recordProcessedEventOutcome(
        SettlementFlowMetricsPort.EventOutcomeProcessingResult.NO_MATCH);
    metrics.recordPreparedSettlements(3);
    metrics.recordSettlementDispatchCounts(2, 1);
    metrics.recordDemoReset(new DemoResetSummary(4, 5, 4));

    assertThat(meterRegistry.get("betsettler.event.outcomes.accepted").counter().count())
        .isEqualTo(1.0d);
    assertThat(
            meterRegistry
                .get("betsettler.event.outcomes.processed")
                .tag("result", "no_match")
                .counter()
                .count())
        .isEqualTo(1.0d);
    assertThat(meterRegistry.get("betsettler.settlements.prepared").summary().totalAmount())
        .isEqualTo(3.0d);
    assertThat(
            meterRegistry
                .get("betsettler.settlements.dispatched")
                .tag("result", "sent")
                .counter()
                .count())
        .isEqualTo(2.0d);
    assertThat(
            meterRegistry
                .get("betsettler.settlements.dispatched")
                .tag("result", "failed")
                .counter()
                .count())
        .isEqualTo(1.0d);
    assertThat(meterRegistry.get("betsettler.demo.reset.invocations").counter().count())
        .isEqualTo(1.0d);
    assertThat(
            meterRegistry
                .get("betsettler.demo.reset.rows.cleared")
                .tag("table", "processed_event_outcome")
                .summary()
                .totalAmount())
        .isEqualTo(4.0d);
    assertThat(
            meterRegistry
                .get("betsettler.demo.reset.rows.cleared")
                .tag("table", "settlement_audit")
                .summary()
                .totalAmount())
        .isEqualTo(5.0d);
  }

  @Test
  void settlementDispatchMetricsIgnoreZeroCounts() {
    metrics.recordSettlementDispatchCounts(0, 0);

    assertThat(meterRegistry.find("betsettler.settlements.dispatched").meters()).isEmpty();
  }

  @Test
  void constructorAndMetricValidationRejectInvalidInputs() {
    assertThatThrownBy(() -> new MicrometerSettlementFlowMetrics(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("meterRegistry must not be null");
    assertThatThrownBy(() -> metrics.recordProcessedEventOutcome(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("processingResult must not be null");
    assertThatThrownBy(() -> metrics.recordPreparedSettlements(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("settlementCount must not be negative");
    assertThatThrownBy(() -> metrics.recordSettlementDispatchCounts(-1, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("count must not be negative");
    assertThatThrownBy(() -> metrics.recordSettlementDispatchCounts(0, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("count must not be negative");
    assertThatThrownBy(() -> metrics.recordDemoReset(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("demoResetSummary must not be null");
  }
}
