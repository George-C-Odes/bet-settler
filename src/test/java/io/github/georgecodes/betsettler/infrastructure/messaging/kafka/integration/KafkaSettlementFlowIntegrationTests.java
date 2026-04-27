package io.github.georgecodes.betsettler.infrastructure.messaging.kafka.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.github.georgecodes.betsettler.application.port.out.BetSettlementDispatchPort;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.publisher.LoggingBetSettlementPublisher;
import io.github.georgecodes.betsettler.infrastructure.persistence.entity.SettlementAuditEntity;
import io.github.georgecodes.betsettler.infrastructure.persistence.entity.SettlementPublishStatus;
import io.github.georgecodes.betsettler.infrastructure.persistence.repository.SpringDataProcessedEventOutcomeRepository;
import io.github.georgecodes.betsettler.infrastructure.persistence.repository.SpringDataSettlementAuditRepository;
import io.github.georgecodes.betsettler.testsupport.KafkaTestcontainersConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.kafka.listener.auto-startup=true")
@Import(KafkaTestcontainersConfiguration.class)
@ActiveProfiles("test")
class KafkaSettlementFlowIntegrationTests {

  private static final Duration POLL_INTERVAL = Duration.ofMillis(200);
  private static final Duration PROCESSING_TIMEOUT = Duration.ofSeconds(15);
  private static final Duration STABLE_COUNT_WINDOW = Duration.ofSeconds(2);
  private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

  @LocalServerPort private int port;

  @Autowired private BetSettlementDispatchPort betSettlementDispatchPort;

  @Autowired private SpringDataProcessedEventOutcomeRepository processedEventOutcomeRepository;

  @Autowired private SpringDataSettlementAuditRepository settlementAuditRepository;

  @AfterEach
  void cleanTransientState() {
    settlementAuditRepository.deleteAllInBatch();
    processedEventOutcomeRepository.deleteAllInBatch();
  }

  @Test
  void postEventOutcomeTriggersSettlementProcessingAndMarksAuditsSent() throws Exception {
    HttpResponse<String> response =
        sendEventOutcomePost(
            """
            {
              "eventId": "EVT-1001",
              "eventName": "Team A vs Team B",
              "eventWinnerId": "TEAM-A"
            }
            """);

    awaitCondition(
        "Expected settlement audits for EVT-1001 to reach count 2.",
        () ->
            processedEventOutcomeRepository.findById("EVT-1001").isPresent()
                && settlementAuditRepository.findAllByEventIdOrderByBetIdAsc("EVT-1001").size()
                    == 2);
    List<SettlementAuditEntity> audits =
        settlementAuditRepository.findAllByEventIdOrderByBetIdAsc("EVT-1001");

    assertThat(response.statusCode()).isEqualTo(202);
    assertThat(betSettlementDispatchPort).isInstanceOf(LoggingBetSettlementPublisher.class);
    assertThat(processedEventOutcomeRepository.findById("EVT-1001")).isPresent();
    assertThat(audits)
        .extracting(
            SettlementAuditEntity::getBetId,
            SettlementAuditEntity::getPublishStatus,
            SettlementAuditEntity::getFailureReason)
        .containsExactly(
            Tuple.tuple("BET-1001", SettlementPublishStatus.SENT, null),
            Tuple.tuple("BET-1002", SettlementPublishStatus.SENT, null));
  }

  @Test
  void duplicateEventOutcomesAreOnlySettledOnce() throws Exception {
    String requestBody =
        """
        {
          "eventId": "EVT-3001",
          "eventName": "Team E vs Team F",
          "eventWinnerId": "TEAM-D"
        }
        """;

    HttpResponse<String> firstResponse = sendEventOutcomePost(requestBody);
    HttpResponse<String> secondResponse = sendEventOutcomePost(requestBody);

    awaitStableCondition(
        () ->
            processedEventOutcomeRepository.findById("EVT-3001").isPresent()
                && settlementAuditRepository.findAllByEventIdOrderByBetIdAsc("EVT-3001").size()
                    == 1);
    List<SettlementAuditEntity> audits =
        settlementAuditRepository.findAllByEventIdOrderByBetIdAsc("EVT-3001");

    assertThat(firstResponse.statusCode()).isEqualTo(202);
    assertThat(secondResponse.statusCode()).isEqualTo(202);
    assertThat(processedEventOutcomeRepository.findById("EVT-3001")).isPresent();
    assertThat(audits).hasSize(1);
    assertThat(audits.getFirst().getBetId()).isEqualTo("BET-3001");
    assertThat(audits.getFirst().getPublishStatus()).isEqualTo(SettlementPublishStatus.SENT);
  }

  @Test
  void eventOutcomesWithoutMatchingBetsRecordProcessedStateWithoutAudits() throws Exception {
    HttpResponse<String> response =
        sendEventOutcomePost(
            """
            {
              "eventId": "EVT-9999",
              "eventName": "Team Unknown vs Team Missing",
              "eventWinnerId": "TEAM-Z"
            }
            """);

    awaitCondition(
        "Expected processed outcome for EVT-9999 with no settlement audits.",
        () ->
            processedEventOutcomeRepository.findById("EVT-9999").isPresent()
                && settlementAuditRepository.findAllByEventIdOrderByBetIdAsc("EVT-9999").isEmpty());

    assertThat(response.statusCode()).isEqualTo(202);
    assertThat(processedEventOutcomeRepository.findById("EVT-9999")).isPresent();
    assertThat(settlementAuditRepository.findAllByEventIdOrderByBetIdAsc("EVT-9999")).isEmpty();
  }

  private void awaitCondition(String failureMessage, BooleanSupplier condition) {
    long deadline = System.nanoTime() + PROCESSING_TIMEOUT.toNanos();
    while (System.nanoTime() < deadline) {
      if (condition.getAsBoolean()) {
        return;
      }
      sleep();
    }
    fail(failureMessage);
  }

  private void awaitStableCondition(BooleanSupplier condition) {
    long deadline = System.nanoTime() + PROCESSING_TIMEOUT.toNanos();
    long stableSince = -1L;
    while (System.nanoTime() < deadline) {
      if (condition.getAsBoolean()) {
        if (stableSince < 0L) {
          stableSince = System.nanoTime();
        }
        if (System.nanoTime() - stableSince >= STABLE_COUNT_WINDOW.toNanos()) {
          return;
        }
      } else {
        stableSince = -1L;
      }
      sleep();
    }
    fail("Expected settlement audits for EVT-3001 to stabilise at count 1.");
  }

  private static void sleep() {
    try {
      Thread.sleep(POLL_INTERVAL);
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
      fail(
          "Interrupted while waiting for asynchronous Kafka settlement processing.",
          interruptedException);
    }
  }

  private HttpResponse<String> sendEventOutcomePost(String body) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/event-outcomes"))
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
    return HTTP_CLIENT.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
  }
}
