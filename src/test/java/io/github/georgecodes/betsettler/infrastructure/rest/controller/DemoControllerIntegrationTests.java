package io.github.georgecodes.betsettler.infrastructure.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.DemoResetResponse;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.SettlementDispatchRetryResponse;
import io.github.georgecodes.betsettler.testsupport.rest.SettlementAuditRestIntegrationTestSupport;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DemoControllerIntegrationTests extends SettlementAuditRestIntegrationTestSupport {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

  @Test
  void postDemoResetClearsTransientStateAndPreservesSeededBets() throws Exception {
    EventOutcome processedOutcome = new EventOutcome("EVT-4001", "Team E vs Team F", "TEAM-E");
    assertThat(processedEventOutcomePort.recordProcessedIfAbsent(processedOutcome)).isTrue();
    settlementAuditPort.savePendingSettlements(
        List.of(
            createPendingSettlementAudit(
                createWinningSettlementForSeededBet(), "{\"betId\":\"BET-1001\"}")));

    assertThat(processedEventOutcomeRepository.count()).isEqualTo(1L);
    assertThat(settlementAuditRepository.count()).isEqualTo(1L);

    HttpResponse<String> response = sendDemoResetPost();
    DemoResetResponse responseBody =
        OBJECT_MAPPER.readValue(response.body(), DemoResetResponse.class);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(
            contentType -> assertThat(contentType).contains(MediaType.APPLICATION_JSON_VALUE));
    assertThat(responseBody.status()).isEqualTo("RESET");
    assertThat(responseBody.message())
        .isEqualTo("Demo settlement state has been reset while preserving seeded bets.");
    assertThat(responseBody.processedEventOutcomesCleared()).isEqualTo(1L);
    assertThat(responseBody.settlementAuditsCleared()).isEqualTo(1L);
    assertThat(responseBody.seededBetsPreserved()).isEqualTo(4L);

    assertThat(processedEventOutcomeRepository.count()).isZero();
    assertThat(settlementAuditRepository.count()).isZero();
    assertThat(betRepository.count()).isEqualTo(4L);
  }

  @Test
  void postRetrySettlementsReplaysPendingAndFailedSettlementAudits() throws Exception {
    settlementAuditPort.savePendingSettlements(
        List.of(
            createPendingSettlementAudit(
                createWinningSettlementForSeededBet(), "{\"betId\":\"BET-1001\"}"),
            createPendingSettlementAudit(
                createLosingSettlementForSeededBet(), "{\"betId\":\"BET-1002\"}")));
    settlementAuditPort.markFailed("EVT-1001", "BET-1002", "RocketMQ unavailable");

    HttpResponse<String> response = sendRetrySettlementsPost();
    SettlementDispatchRetryResponse responseBody =
        OBJECT_MAPPER.readValue(response.body(), SettlementDispatchRetryResponse.class);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(responseBody.status()).isEqualTo("RETRIED");
    assertThat(responseBody.retriedAuditCount()).isEqualTo(2L);
    assertThat(responseBody.sentCount()).isEqualTo(2L);
    assertThat(responseBody.failedCount()).isZero();
    assertThat(settlementAuditRepository.findAllByEventIdOrderByBetIdAsc("EVT-1001"))
        .extracting(audit -> audit.getPublishStatus().name())
        .containsExactly("SENT", "SENT");
  }
}
