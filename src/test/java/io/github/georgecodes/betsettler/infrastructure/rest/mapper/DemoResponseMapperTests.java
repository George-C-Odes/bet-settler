package io.github.georgecodes.betsettler.infrastructure.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.georgecodes.betsettler.application.dto.DemoResetSummary;
import io.github.georgecodes.betsettler.application.dto.SettlementDispatchRetrySummary;
import org.junit.jupiter.api.Test;

class DemoResponseMapperTests {

  private final DemoResponseMapper demoResponseMapper = new DemoResponseMapper();

  @Test
  void toDemoResetResponseBuildsTheExpectedPayload() {
    assertThat(demoResponseMapper.toDemoResetResponse(new DemoResetSummary(2, 3, 4)))
        .extracting(
            "status",
            "message",
            "processedEventOutcomesCleared",
            "settlementAuditsCleared",
            "seededBetsPreserved")
        .containsExactly(
            "RESET",
            "Demo settlement state has been reset while preserving seeded bets.",
            2L,
            3L,
            4L);
  }

  @Test
  void toSettlementDispatchRetryResponseReturnsRetriedStatusWhenNoFailuresRemain() {
    assertThat(
            demoResponseMapper.toSettlementDispatchRetryResponse(
                new SettlementDispatchRetrySummary(2, 2, 0)))
        .extracting("status", "message", "retriedAuditCount", "sentCount", "failedCount")
        .containsExactly("RETRIED", "Retried settlement dispatches successfully.", 2L, 2L, 0L);
  }

  @Test
  void toSettlementDispatchRetryResponseReturnsPartialStatusWhenFailuresRemain() {
    assertThat(
            demoResponseMapper.toSettlementDispatchRetryResponse(
                new SettlementDispatchRetrySummary(2, 1, 1)))
        .extracting("status", "message", "retriedAuditCount", "sentCount", "failedCount")
        .containsExactly(
            "PARTIAL",
            "Retried settlement dispatches with one or more remaining failures.",
            2L,
            1L,
            1L);
  }

  @Test
  void toDemoResetResponseRejectsNullSummary() {
    assertThatThrownBy(() -> demoResponseMapper.toDemoResetResponse(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("demoResetSummary must not be null");
  }

  @Test
  void toSettlementDispatchRetryResponseRejectsNullSummary() {
    assertThatThrownBy(() -> demoResponseMapper.toSettlementDispatchRetryResponse(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("retrySummary must not be null");
  }
}
