package io.github.georgecodes.betsettler.infrastructure.messaging.kafka.consumer;

import io.github.georgecodes.betsettler.application.port.in.ProcessEventOutcomeUseCase;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.infrastructure.messaging.kafka.dto.EventOutcomeMessage;
import io.github.georgecodes.betsettler.infrastructure.messaging.kafka.mapper.EventOutcomeMessageMapper;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/** Consumes event outcomes from Kafka and hands them to the processing use case. */
@Slf4j
@Component
public class KafkaEventOutcomeConsumer {

  /** Application use case that processes consumed event outcomes. */
  private final ProcessEventOutcomeUseCase processEventOutcomeUseCase;

  /** Mapper used to convert Kafka payloads into the domain model. */
  private final EventOutcomeMessageMapper eventOutcomeMessageMapper;

  /**
   * Creates a new Kafka event outcome consumer.
   *
   * @param processEventOutcomeUseCase application use case that processes consumed outcomes
   * @param eventOutcomeMessageMapper mapper used to convert Kafka payloads into the domain model
   */
  public KafkaEventOutcomeConsumer(
      ProcessEventOutcomeUseCase processEventOutcomeUseCase,
      EventOutcomeMessageMapper eventOutcomeMessageMapper) {
    this.processEventOutcomeUseCase =
        Objects.requireNonNull(
            processEventOutcomeUseCase, "processEventOutcomeUseCase must not be null");
    this.eventOutcomeMessageMapper =
        Objects.requireNonNull(
            eventOutcomeMessageMapper, "eventOutcomeMessageMapper must not be null");
  }

  /**
   * Consumes a Kafka event outcome payload and delegates it to the processing use case.
   *
   * @param eventOutcomeMessage consumed Kafka payload
   * @param key consumed Kafka record key, when present
   * @param topic consumed Kafka topic name
   */
  @KafkaListener(topics = "${betsettler.messaging.kafka.event-outcomes-topic}")
  public void consume(
      @Payload EventOutcomeMessage eventOutcomeMessage,
      @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
    EventOutcomeMessage validatedMessage =
        Objects.requireNonNull(eventOutcomeMessage, "eventOutcomeMessage must not be null");
    EventOutcome eventOutcome = eventOutcomeMessageMapper.toDomain(validatedMessage);
    log.info(
        "Consumed event outcome from Kafka topic={} key={} eventId={} eventWinnerId={}",
        topic,
        key,
        eventOutcome.eventId(),
        eventOutcome.eventWinnerId());
    processEventOutcomeUseCase.process(eventOutcome);
  }
}
