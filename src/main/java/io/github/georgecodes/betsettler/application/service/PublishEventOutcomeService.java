package io.github.georgecodes.betsettler.application.service;

import io.github.georgecodes.betsettler.application.dto.PublishEventOutcomeCommand;
import io.github.georgecodes.betsettler.application.port.in.PublishEventOutcomeUseCase;
import io.github.georgecodes.betsettler.application.port.out.EventOutcomePublisherPort;
import io.github.georgecodes.betsettler.application.port.out.SettlementFlowMetricsPort;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Default publish use case implementation for the REST API entry point. */
@Slf4j
@Service
public class PublishEventOutcomeService implements PublishEventOutcomeUseCase {

  /** Outbound port used to publish event outcomes to Kafka. */
  private final EventOutcomePublisherPort eventOutcomePublisherPort;

  /** Metrics port used to expose lightweight publish-flow metrics. */
  private final SettlementFlowMetricsPort settlementFlowMetricsPort;

  /**
   * Creates a new publish event outcome application service.
   *
   * @param eventOutcomePublisherPort outbound port used to publish event outcomes
   * @param settlementFlowMetricsPort metrics port used to expose publish-flow metrics
   */
  public PublishEventOutcomeService(
      EventOutcomePublisherPort eventOutcomePublisherPort,
      SettlementFlowMetricsPort settlementFlowMetricsPort) {
    this.eventOutcomePublisherPort =
        Objects.requireNonNull(
            eventOutcomePublisherPort, "eventOutcomePublisherPort must not be null");
    this.settlementFlowMetricsPort =
        Objects.requireNonNull(
            settlementFlowMetricsPort, "settlementFlowMetricsPort must not be null");
  }

  @Override
  public void publish(PublishEventOutcomeCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    EventOutcome eventOutcome = command.toEventOutcome();
    log.info(
        "Received event outcome publish request for eventId={}, eventName={}, eventWinnerId={}",
        eventOutcome.eventId(),
        eventOutcome.eventName(),
        eventOutcome.eventWinnerId());
    eventOutcomePublisherPort.publish(eventOutcome);
    settlementFlowMetricsPort.recordAcceptedEventOutcome();
    log.info(
        "Accepted event outcome publish request after Kafka acknowledgement for eventId={}, eventName={}, eventWinnerId={}",
        eventOutcome.eventId(),
        eventOutcome.eventName(),
        eventOutcome.eventWinnerId());
  }
}
