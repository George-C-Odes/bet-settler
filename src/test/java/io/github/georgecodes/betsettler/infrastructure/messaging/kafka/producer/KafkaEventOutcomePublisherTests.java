package io.github.georgecodes.betsettler.infrastructure.messaging.kafka.producer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.georgecodes.betsettler.application.exception.EventOutcomePublishFailedException;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.infrastructure.config.BetSettlerMessagingProperties;
import io.github.georgecodes.betsettler.infrastructure.messaging.kafka.dto.EventOutcomeMessage;
import io.github.georgecodes.betsettler.infrastructure.messaging.kafka.mapper.EventOutcomeMessageMapper;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class KafkaEventOutcomePublisherTests {

  @Mock private KafkaTemplate<String, EventOutcomeMessage> kafkaTemplate;

  @Mock private SendResult<String, EventOutcomeMessage> sendResult;

  @Mock private RecordMetadata recordMetadata;

  private final EventOutcomeMessageMapper eventOutcomeMessageMapper =
      new EventOutcomeMessageMapper();

  @Test
  void publishSendsMappedMessageToConfiguredTopic() {
    BetSettlerMessagingProperties messagingProperties =
        new BetSettlerMessagingProperties(
            new BetSettlerMessagingProperties.Kafka("event-outcomes-test", Duration.ofSeconds(3)),
            null);
    KafkaEventOutcomePublisher publisher =
        new KafkaEventOutcomePublisher(
            kafkaTemplate, messagingProperties, eventOutcomeMessageMapper);
    EventOutcome eventOutcome = new EventOutcome("EVT-1001", "Team A vs Team B", "TEAM-A");
    EventOutcomeMessage expectedMessage =
        new EventOutcomeMessage("EVT-1001", "Team A vs Team B", "TEAM-A");

    when(recordMetadata.topic()).thenReturn("event-outcomes-test");
    when(recordMetadata.partition()).thenReturn(0);
    when(recordMetadata.offset()).thenReturn(42L);
    when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);
    when(kafkaTemplate.send(eq("event-outcomes-test"), eq("EVT-1001"), eq(expectedMessage)))
        .thenReturn(CompletableFuture.completedFuture(sendResult));

    publisher.publish(eventOutcome);

    verify(kafkaTemplate).send("event-outcomes-test", "EVT-1001", expectedMessage);
  }

  @Test
  void publishRaisesDedicatedExceptionWhenKafkaSendFails() {
    BetSettlerMessagingProperties messagingProperties =
        new BetSettlerMessagingProperties(
            new BetSettlerMessagingProperties.Kafka("event-outcomes-test", Duration.ofSeconds(3)),
            null);
    KafkaEventOutcomePublisher publisher =
        new KafkaEventOutcomePublisher(
            kafkaTemplate, messagingProperties, eventOutcomeMessageMapper);
    EventOutcome eventOutcome = new EventOutcome("EVT-1002", "Team C vs Team D", "TEAM-C");
    EventOutcomeMessage expectedMessage =
        new EventOutcomeMessage("EVT-1002", "Team C vs Team D", "TEAM-C");

    when(kafkaTemplate.send(eq("event-outcomes-test"), eq("EVT-1002"), eq(expectedMessage)))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("boom")));

    assertThatThrownBy(() -> publisher.publish(eventOutcome))
        .isInstanceOf(EventOutcomePublishFailedException.class)
        .hasMessage(
            "Failed to publish event outcome to Kafka topic=event-outcomes-test eventId=EVT-1002. Kafka publish failed before broker acknowledgement.")
        .hasCauseInstanceOf(RuntimeException.class);

    verify(kafkaTemplate).send("event-outcomes-test", "EVT-1002", expectedMessage);
  }

  @Test
  void publishRaisesDedicatedExceptionWhenKafkaAcknowledgementTimesOut() {
    BetSettlerMessagingProperties messagingProperties =
        new BetSettlerMessagingProperties(
            new BetSettlerMessagingProperties.Kafka("event-outcomes-test", Duration.ofMillis(25)),
            null);
    KafkaEventOutcomePublisher publisher =
        new KafkaEventOutcomePublisher(
            kafkaTemplate, messagingProperties, eventOutcomeMessageMapper);
    EventOutcome eventOutcome = new EventOutcome("EVT-1003", "Team E vs Team F", "TEAM-E");
    EventOutcomeMessage expectedMessage =
        new EventOutcomeMessage("EVT-1003", "Team E vs Team F", "TEAM-E");

    when(kafkaTemplate.send(eq("event-outcomes-test"), eq("EVT-1003"), eq(expectedMessage)))
        .thenReturn(new TimeoutSendFuture());

    assertThatThrownBy(() -> publisher.publish(eventOutcome))
        .isInstanceOf(EventOutcomePublishFailedException.class)
        .hasMessageContaining("Timed out waiting for Kafka broker acknowledgement after 25 ms.");

    verify(kafkaTemplate).send("event-outcomes-test", "EVT-1003", expectedMessage);
  }

  @Test
  void publishRestoresInterruptFlagWhenKafkaSendIsInterrupted() {
    BetSettlerMessagingProperties messagingProperties =
        new BetSettlerMessagingProperties(
            new BetSettlerMessagingProperties.Kafka("event-outcomes-test", Duration.ofMillis(25)),
            null);
    KafkaEventOutcomePublisher publisher =
        new KafkaEventOutcomePublisher(
            kafkaTemplate, messagingProperties, eventOutcomeMessageMapper);
    EventOutcome eventOutcome = new EventOutcome("EVT-1004", "Team G vs Team H", "TEAM-G");
    EventOutcomeMessage expectedMessage =
        new EventOutcomeMessage("EVT-1004", "Team G vs Team H", "TEAM-G");

    when(kafkaTemplate.send(eq("event-outcomes-test"), eq("EVT-1004"), eq(expectedMessage)))
        .thenReturn(new InterruptedSendFuture());

    assertThatThrownBy(() -> publisher.publish(eventOutcome))
        .isInstanceOf(EventOutcomePublishFailedException.class)
        .hasMessageContaining("Kafka publish was interrupted.");
    assertThat(Thread.interrupted()).isTrue();

    verify(kafkaTemplate).send("event-outcomes-test", "EVT-1004", expectedMessage);
  }

  @Test
  void publishUsesExecutionExceptionWhenKafkaSendFailureHasNoCause() {
    BetSettlerMessagingProperties messagingProperties =
        new BetSettlerMessagingProperties(
            new BetSettlerMessagingProperties.Kafka("event-outcomes-test", Duration.ofMillis(25)),
            null);
    KafkaEventOutcomePublisher publisher =
        new KafkaEventOutcomePublisher(
            kafkaTemplate, messagingProperties, eventOutcomeMessageMapper);
    EventOutcome eventOutcome = new EventOutcome("EVT-1005", "Team I vs Team J", "TEAM-I");
    EventOutcomeMessage expectedMessage =
        new EventOutcomeMessage("EVT-1005", "Team I vs Team J", "TEAM-I");

    when(kafkaTemplate.send(eq("event-outcomes-test"), eq("EVT-1005"), eq(expectedMessage)))
        .thenReturn(new ExecutionFailureWithoutCauseFuture());

    assertThatThrownBy(() -> publisher.publish(eventOutcome))
        .isInstanceOf(EventOutcomePublishFailedException.class)
        .hasMessageContaining("Kafka publish failed before broker acknowledgement.")
        .hasCauseInstanceOf(ExecutionException.class);

    verify(kafkaTemplate).send("event-outcomes-test", "EVT-1005", expectedMessage);
  }

  private static final class TimeoutSendFuture
      extends CompletableFuture<SendResult<String, EventOutcomeMessage>> {

    @Override
    public SendResult<String, EventOutcomeMessage> get(long timeout, TimeUnit unit)
        throws TimeoutException {
      throw new TimeoutException("timed out");
    }
  }

  private static final class InterruptedSendFuture
      extends CompletableFuture<SendResult<String, EventOutcomeMessage>> {

    @Override
    public SendResult<String, EventOutcomeMessage> get(long timeout, TimeUnit unit)
        throws InterruptedException {
      throw new InterruptedException("interrupted");
    }
  }

  private static final class ExecutionFailureWithoutCauseFuture
      extends CompletableFuture<SendResult<String, EventOutcomeMessage>> {

    @Override
    public SendResult<String, EventOutcomeMessage> get(long timeout, TimeUnit unit)
        throws ExecutionException {
      throw new ExecutionException("execution failed", null);
    }
  }
}
