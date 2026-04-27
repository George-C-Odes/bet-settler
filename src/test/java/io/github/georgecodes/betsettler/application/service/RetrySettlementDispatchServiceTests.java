package io.github.georgecodes.betsettler.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.georgecodes.betsettler.application.dto.SettlementDispatchRetrySummary;
import io.github.georgecodes.betsettler.application.model.audit.RetryableSettlementAudit;
import io.github.georgecodes.betsettler.application.port.out.BetSettlementReplayPort;
import io.github.georgecodes.betsettler.application.port.out.SettlementAuditPort;
import io.github.georgecodes.betsettler.application.support.SettlementFailureReasonFormatter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RetrySettlementDispatchServiceTests {

  @Mock private SettlementAuditPort settlementAuditPort;

  @Mock private BetSettlementReplayPort betSettlementReplayPort;

  private RetrySettlementDispatchService retrySettlementDispatchService;

  private final SettlementFailureReasonFormatter settlementFailureReasonFormatter =
      new SettlementFailureReasonFormatter();

  @BeforeEach
  void setUp() {
    retrySettlementDispatchService =
        new RetrySettlementDispatchService(
            settlementAuditPort, betSettlementReplayPort, settlementFailureReasonFormatter);
  }

  @Test
  void retrySettlementsReturnsZeroSummaryWhenNothingNeedsReplay() {
    when(settlementAuditPort.findRetryableSettlements()).thenReturn(List.of());

    SettlementDispatchRetrySummary summary = retrySettlementDispatchService.retrySettlements();

    assertThat(summary).isEqualTo(new SettlementDispatchRetrySummary(0, 0, 0));
    verify(betSettlementReplayPort, never()).replayPayloadSnapshot(anyString(), anyString());
  }

  @Test
  void retrySettlementsReplaysStoredDestinationTopicAndPayloadSnapshotUnchanged() {
    RetryableSettlementAudit pendingAudit =
        new RetryableSettlementAudit(
            "EVT-1001",
            "BET-1001",
            "USER-1",
            "PENDING",
            "bet-settlements-retry-snapshot",
            "{\"betId\":\"BET-1001\",\"source\":\"stored-audit\"}");
    when(settlementAuditPort.findRetryableSettlements()).thenReturn(List.of(pendingAudit));

    SettlementDispatchRetrySummary summary = retrySettlementDispatchService.retrySettlements();

    assertThat(summary).isEqualTo(new SettlementDispatchRetrySummary(1, 1, 0));
    verify(betSettlementReplayPort)
        .replayPayloadSnapshot(
            "bet-settlements-retry-snapshot",
            "{\"betId\":\"BET-1001\",\"source\":\"stored-audit\"}");
    verify(settlementAuditPort).markSent("EVT-1001", "BET-1001");
    verify(settlementAuditPort, never()).markFailed(anyString(), anyString(), anyString());
  }

  @Test
  void retrySettlementsReplaysPendingAndFailedAuditsReturnedByTheAuditPort() {
    RetryableSettlementAudit pendingAudit =
        new RetryableSettlementAudit(
            "EVT-1001",
            "BET-1001",
            "USER-1",
            "PENDING",
            "bet-settlements",
            "{\"betId\":\"BET-1001\"}");
    RetryableSettlementAudit failedAudit =
        new RetryableSettlementAudit(
            "EVT-2001",
            "BET-2001",
            "USER-2",
            "FAILED",
            "bet-settlements",
            "{\"betId\":\"BET-2001\"}");
    when(settlementAuditPort.findRetryableSettlements())
        .thenReturn(List.of(pendingAudit, failedAudit));

    SettlementDispatchRetrySummary summary = retrySettlementDispatchService.retrySettlements();

    assertThat(summary).isEqualTo(new SettlementDispatchRetrySummary(2, 2, 0));
    verify(betSettlementReplayPort)
        .replayPayloadSnapshot("bet-settlements", "{\"betId\":\"BET-1001\"}");
    verify(betSettlementReplayPort)
        .replayPayloadSnapshot("bet-settlements", "{\"betId\":\"BET-2001\"}");
    verify(settlementAuditPort).markSent("EVT-1001", "BET-1001");
    verify(settlementAuditPort).markSent("EVT-2001", "BET-2001");
    verify(betSettlementReplayPort, times(2)).replayPayloadSnapshot(anyString(), anyString());
  }

  @Test
  void retrySettlementsMarksReplayFailuresAsFailed() {
    RetryableSettlementAudit failedAudit =
        new RetryableSettlementAudit(
            "EVT-2001",
            "BET-2001",
            "USER-2",
            "FAILED",
            "bet-settlements",
            "{\"betId\":\"BET-2001\"}");
    when(settlementAuditPort.findRetryableSettlements()).thenReturn(List.of(failedAudit));
    doThrow(new IllegalStateException("RocketMQ unavailable"))
        .when(betSettlementReplayPort)
        .replayPayloadSnapshot("bet-settlements", "{\"betId\":\"BET-2001\"}");

    SettlementDispatchRetrySummary summary = retrySettlementDispatchService.retrySettlements();

    assertThat(summary).isEqualTo(new SettlementDispatchRetrySummary(1, 0, 1));
    verify(settlementAuditPort)
        .markFailed("EVT-2001", "BET-2001", "IllegalStateException: RocketMQ unavailable");
    verify(settlementAuditPort, never()).markSent("EVT-2001", "BET-2001");
  }
}
