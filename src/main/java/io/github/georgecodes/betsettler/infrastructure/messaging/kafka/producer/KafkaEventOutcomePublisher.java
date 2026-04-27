package io.github.georgecodes.betsettler.infrastructure.messaging.kafka.producer;

import io.github.georgecodes.betsettler.application.exception.EventOutcomePublishFailedException;
import io.github.georgecodes.betsettler.application.port.out.EventOutcomePublisherPort;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.infrastructure.config.BetSettlerMessagingProperties;
import io.github.georgecodes.betsettler.infrastructure.messaging.kafka.dto.EventOutcomeMessage;
import io.github.georgecodes.betsettler.infrastructure.messaging.kafka.mapper.EventOutcomeMessageMapper;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/** Publishes event outcomes to Kafka. */
@Slf4j
@Component
public class KafkaEventOutcomePublisher implements EventOutcomePublisherPort {

  /** Kafka template used to publish event outcome messages. */
  private final KafkaTemplate<String, EventOutcomeMessage> kafkaTemplate;

  /** Externalized messaging properties. */
  private final BetSettlerMessagingProperties messagingProperties;

  /** Mapper from domain event outcomes to Kafka payloads. */
  private final EventOutcomeMessageMapper eventOutcomeMessageMapper;

  /**
   * Creates a new Kafka-backed event outcome publisher.
   *
   * @param kafkaTemplate Kafka template used to publish event outcome messages
   * @param messagingProperties externalized messaging properties
   * @param eventOutcomeMessageMapper mapper from domain event outcomes to Kafka payloads
   */
  public KafkaEventOutcomePublisher(
      KafkaTemplate<String, EventOutcomeMessage> kafkaTemplate,
      BetSettlerMessagingProperties messagingProperties,
      EventOutcomeMessageMapper eventOutcomeMessageMapper) {
    this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate must not be null");
    this.messagingProperties =
        Objects.requireNonNull(messagingProperties, "messagingProperties must not be null");
    this.eventOutcomeMessageMapper =
        Objects.requireNonNull(
            eventOutcomeMessageMapper, "eventOutcomeMessageMapper must not be null");
  }

  @Override
  public void publish(EventOutcome eventOutcome) {
    Objects.requireNonNull(eventOutcome, "eventOutcome must not be null");
    String topic = messagingProperties.kafka().eventOutcomesTopic();
    long publishAckTimeoutMillis = messagingProperties.kafka().publishAckTimeout().toMillis();
    String eventId = eventOutcome.eventId();
    EventOutcomeMessage message = eventOutcomeMessageMapper.toMessage(eventOutcome);
    log.info(
        "Publishing event outcome to Kafka topic={} eventId={} eventWinnerId={} messageKey={} ackTimeoutMs={}",
        topic,
        eventId,
        eventOutcome.eventWinnerId(),
        eventId,
        publishAckTimeoutMillis);
    try {
      SendResult<String, EventOutcomeMessage> sendResult =
          kafkaTemplate
              .send(topic, eventId, message)
              .get(publishAckTimeoutMillis, TimeUnit.MILLISECONDS);
      log.info(
          "Published event outcome to Kafka topic={} partition={} offset={} eventId={} messageKey={}",
          sendResult.getRecordMetadata().topic(),
          sendResult.getRecordMetadata().partition(),
          sendResult.getRecordMetadata().offset(),
          eventId,
          eventId);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw publishFailedException(topic, eventId, "Kafka publish was interrupted.", exception);
    } catch (TimeoutException exception) {
      throw publishFailedException(
          topic,
          eventId,
          "Timed out waiting for Kafka broker acknowledgement after "
              + publishAckTimeoutMillis
              + " ms.",
          exception);
    } catch (ExecutionException exception) {
      Throwable cause = exception.getCause() == null ? exception : exception.getCause();
      throw publishFailedException(
          topic, eventId, "Kafka publish failed before broker acknowledgement.", cause);
    }
  }

  private EventOutcomePublishFailedException publishFailedException(
      String topic, String eventId, String detail, Throwable throwable) {
    log.error(
        "Failed to publish event outcome to Kafka topic={} eventId={} messageKey={} reason={}",
        topic,
        eventId,
        eventId,
        detail,
        throwable);
    return new EventOutcomePublishFailedException(
        "Failed to publish event outcome to Kafka topic="
            + topic
            + " eventId="
            + eventId
            + ". "
            + detail,
        throwable);
  }
}
