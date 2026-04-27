package io.github.georgecodes.betsettler.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.georgecodes.betsettler.application.dto.PublishEventOutcomeCommand;
import io.github.georgecodes.betsettler.application.exception.EventOutcomePublishFailedException;
import io.github.georgecodes.betsettler.application.port.out.EventOutcomePublisherPort;
import io.github.georgecodes.betsettler.application.port.out.SettlementFlowMetricsPort;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublishEventOutcomeServiceTests {

  @Mock private EventOutcomePublisherPort eventOutcomePublisherPort;

  @Mock private SettlementFlowMetricsPort settlementFlowMetricsPort;

  @InjectMocks private PublishEventOutcomeService publishEventOutcomeService;

  @Test
  void publishDelegatesMappedEventOutcomeToPublisherPort() {
    PublishEventOutcomeCommand command =
        new PublishEventOutcomeCommand("EVT-1001", "Team A vs Team B", "TEAM-A");

    publishEventOutcomeService.publish(command);

    InOrder inOrder = inOrder(eventOutcomePublisherPort, settlementFlowMetricsPort);

    inOrder
        .verify(eventOutcomePublisherPort)
        .publish(new EventOutcome("EVT-1001", "Team A vs Team B", "TEAM-A"));
    inOrder.verify(settlementFlowMetricsPort).recordAcceptedEventOutcome();
  }

  @Test
  void publishDoesNotRecordAcceptedMetricWhenPublisherFails() {
    PublishEventOutcomeCommand command =
        new PublishEventOutcomeCommand("EVT-1002", "Team C vs Team D", "TEAM-C");
    doThrow(
            new EventOutcomePublishFailedException(
                "Kafka unavailable", new RuntimeException("boom")))
        .when(eventOutcomePublisherPort)
        .publish(new EventOutcome("EVT-1002", "Team C vs Team D", "TEAM-C"));

    assertThatThrownBy(() -> publishEventOutcomeService.publish(command))
        .isInstanceOf(EventOutcomePublishFailedException.class)
        .hasMessage("Kafka unavailable");

    verify(settlementFlowMetricsPort, never()).recordAcceptedEventOutcome();
  }

  @Test
  void publishRejectsNullCommand() {
    assertThatThrownBy(() -> publishEventOutcomeService.publish(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("command must not be null");
  }
}
