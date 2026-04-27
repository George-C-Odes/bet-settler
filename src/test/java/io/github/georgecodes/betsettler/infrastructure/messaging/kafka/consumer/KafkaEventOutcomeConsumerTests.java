package io.github.georgecodes.betsettler.infrastructure.messaging.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import io.github.georgecodes.betsettler.application.port.in.ProcessEventOutcomeUseCase;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.infrastructure.messaging.kafka.dto.EventOutcomeMessage;
import io.github.georgecodes.betsettler.infrastructure.messaging.kafka.mapper.EventOutcomeMessageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KafkaEventOutcomeConsumerTests {

  @Mock private ProcessEventOutcomeUseCase processEventOutcomeUseCase;

  private KafkaEventOutcomeConsumer kafkaEventOutcomeConsumer;

  @BeforeEach
  void setUp() {
    kafkaEventOutcomeConsumer =
        new KafkaEventOutcomeConsumer(processEventOutcomeUseCase, new EventOutcomeMessageMapper());
  }

  @Test
  void consumeMapsMessageAndDelegatesToProcessUseCase() {
    EventOutcomeMessage eventOutcomeMessage =
        new EventOutcomeMessage("EVT-1001", "Team A vs Team B", "TEAM-A");

    kafkaEventOutcomeConsumer.consume(eventOutcomeMessage, "EVT-1001", "event-outcomes-test");

    verify(processEventOutcomeUseCase)
        .process(new EventOutcome("EVT-1001", "Team A vs Team B", "TEAM-A"));
  }

  @Test
  void consumeRejectsNullMessage() {
    assertThatThrownBy(
            () -> kafkaEventOutcomeConsumer.consume(null, "EVT-1001", "event-outcomes-test"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("eventOutcomeMessage must not be null");
  }
}
