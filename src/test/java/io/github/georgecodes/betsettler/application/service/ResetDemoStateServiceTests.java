package io.github.georgecodes.betsettler.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.georgecodes.betsettler.application.dto.DemoResetSummary;
import io.github.georgecodes.betsettler.application.port.out.DemoStateResetPort;
import io.github.georgecodes.betsettler.application.port.out.SettlementFlowMetricsPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResetDemoStateServiceTests {

  @Mock private DemoStateResetPort demoStateResetPort;

  @Mock private SettlementFlowMetricsPort settlementFlowMetricsPort;

  @InjectMocks private ResetDemoStateService resetDemoStateService;

  @Test
  void resetDemoStateReturnsSummaryAndRecordsMetrics() {
    DemoResetSummary summary = new DemoResetSummary(2L, 3L, 4L);
    when(demoStateResetPort.resetDemoState()).thenReturn(summary);

    DemoResetSummary response = resetDemoStateService.resetDemoState();

    assertThat(response).isEqualTo(summary);
    verify(settlementFlowMetricsPort).recordDemoReset(summary);
  }

  @Test
  void constructorRejectsNullDemoStateResetPort() {
    assertThatThrownBy(() -> new ResetDemoStateService(null, settlementFlowMetricsPort))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("demoStateResetPort must not be null");
  }

  @Test
  void constructorRejectsNullSettlementFlowMetricsPort() {
    assertThatThrownBy(() -> new ResetDemoStateService(demoStateResetPort, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("settlementFlowMetricsPort must not be null");
  }
}
