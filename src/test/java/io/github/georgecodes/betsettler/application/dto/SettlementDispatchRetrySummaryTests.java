package io.github.georgecodes.betsettler.application.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SettlementDispatchRetrySummaryTests {

  @Test
  void hasFailuresReflectsFailedRetryCount() {
    assertThat(new SettlementDispatchRetrySummary(2, 2, 0).hasFailures()).isFalse();
    assertThat(new SettlementDispatchRetrySummary(2, 1, 1).hasFailures()).isTrue();
  }

  @Test
  void constructorRejectsInvalidCounts() {
    assertThatThrownBy(() -> new SettlementDispatchRetrySummary(-1, 0, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("retriedAuditCount must not be negative");
    assertThatThrownBy(() -> new SettlementDispatchRetrySummary(1, -1, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("sentCount must not be negative");
    assertThatThrownBy(() -> new SettlementDispatchRetrySummary(1, 0, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("failedCount must not be negative");
    assertThatThrownBy(() -> new SettlementDispatchRetrySummary(1, 1, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("sentCount plus failedCount must not exceed retriedAuditCount");
  }
}
