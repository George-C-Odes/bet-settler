package io.github.georgecodes.betsettler.infrastructure.rest.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RestDtoValidationTests {

  @Test
  void publishEventOutcomeResponseTrimsAcceptedPayloadFields() {
    PublishEventOutcomeResponse response =
        new PublishEventOutcomeResponse(
            " EVT-1001 ",
            " ACCEPTED ",
            " Event outcome accepted and published to Kafka for asynchronous processing. ");

    assertThat(response.eventId()).isEqualTo("EVT-1001");
    assertThat(response.status()).isEqualTo("ACCEPTED");
    assertThat(response.message())
        .isEqualTo("Event outcome accepted and published to Kafka for asynchronous processing.");
  }

  @Test
  void settlementDispatchRetryResponseRejectsInvalidCounts() {
    assertThatThrownBy(() -> new SettlementDispatchRetryResponse("RETRIED", "Done", -1, 0, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("retriedAuditCount must not be negative");
    assertThatThrownBy(() -> new SettlementDispatchRetryResponse("RETRIED", "Done", 1, 2, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("sentCount plus failedCount must not exceed retriedAuditCount");
  }

  @Test
  void errorResponseNormalizesFieldsAndCopiesValidationErrors() {
    List<ValidationErrorResponse> validationErrors = new ArrayList<>();
    validationErrors.add(new ValidationErrorResponse(" eventId ", " must not be blank "));

    ErrorResponse response =
        new ErrorResponse(
            Instant.parse("2026-04-23T10:15:30Z"),
            400,
            " Bad Request ",
            " Request validation failed. ",
            " /api/v1/event-outcomes ",
            validationErrors);
    validationErrors.add(new ValidationErrorResponse("eventName", "must not be blank"));

    assertThat(response.error()).isEqualTo("Bad Request");
    assertThat(response.message()).isEqualTo("Request validation failed.");
    assertThat(response.path()).isEqualTo("/api/v1/event-outcomes");
    assertThat(response.validationErrors())
        .singleElement()
        .satisfies(
            error -> {
              assertThat(error.field()).isEqualTo("eventId");
              assertThat(error.message()).isEqualTo("must not be blank");
            });
  }

  @Test
  void errorResponseRejectsInvalidStatusCodes() {
    assertThatThrownBy(
            () ->
                new ErrorResponse(
                    Instant.parse("2026-04-23T10:15:30Z"),
                    99,
                    "Bad Request",
                    "Request validation failed.",
                    "/api/v1/event-outcomes",
                    List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("status must be a valid HTTP status code");
    assertThatThrownBy(
            () ->
                new ErrorResponse(
                    Instant.parse("2026-04-23T10:15:30Z"),
                    600,
                    "Bad Request",
                    "Request validation failed.",
                    "/api/v1/event-outcomes",
                    List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("status must be a valid HTTP status code");
  }

  @Test
  void demoResetResponseRejectsNegativeCounts() {
    assertThatThrownBy(() -> new DemoResetResponse("RESET", "Done", -1, 0, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("processedEventOutcomesCleared must not be negative");
    assertThatThrownBy(() -> new DemoResetResponse("RESET", "Done", 0, -1, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("settlementAuditsCleared must not be negative");
    assertThatThrownBy(() -> new DemoResetResponse("RESET", "Done", 0, 0, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("seededBetsPreserved must not be negative");
  }

  @Test
  void betResponseRejectsBlankIdentifiersAndNullValues() {
    Instant createdAt = Instant.parse("2026-04-23T10:15:30Z");

    assertThatThrownBy(
            () ->
                new BetResponse(
                    "   ", "USER-1", "EVT-1001", "MARKET-1", "TEAM-A", BigDecimal.TEN, createdAt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("betId must not be blank");
    assertThatThrownBy(
            () ->
                new BetResponse(
                    "BET-1001", "USER-1", "EVT-1001", "MARKET-1", "TEAM-A", null, createdAt))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("betAmount must not be null");
    assertThatThrownBy(
            () ->
                new BetResponse(
                    "BET-1001", "USER-1", "EVT-1001", "MARKET-1", "TEAM-A", BigDecimal.TEN, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("createdAt must not be null");
  }

  @Test
  void validationErrorResponseRejectsBlankMessage() {
    ValidationErrorResponse response =
        new ValidationErrorResponse(" eventName ", " must not be blank ");

    assertThat(response.field()).isEqualTo("eventName");
    assertThat(response.message()).isEqualTo("must not be blank");
    assertThatThrownBy(() -> new ValidationErrorResponse("eventName", "   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("message must not be blank");
  }
}
