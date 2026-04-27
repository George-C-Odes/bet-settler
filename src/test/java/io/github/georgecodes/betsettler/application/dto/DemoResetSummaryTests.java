package io.github.georgecodes.betsettler.application.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DemoResetSummaryTests {

  @Test
  void totalClearedRowsReturnsCombinedTransientRowCount() {
    DemoResetSummary summary = new DemoResetSummary(2, 3, 4);

    assertThat(summary.totalClearedRows()).isEqualTo(5L);
  }

  @Test
  void constructorRejectsNegativeCounts() {
    assertThatThrownBy(() -> new DemoResetSummary(-1, 0, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("processedEventOutcomesCleared must not be negative");
    assertThatThrownBy(() -> new DemoResetSummary(0, -1, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("settlementAuditsCleared must not be negative");
    assertThatThrownBy(() -> new DemoResetSummary(0, 0, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("seededBetsPreserved must not be negative");
  }
}
