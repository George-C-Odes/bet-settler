package io.github.georgecodes.betsettler.infrastructure.rest.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import io.github.georgecodes.betsettler.application.exception.EventOutcomePublishFailedException;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.ErrorResponse;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.EventOutcomeRequest;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.ValidationErrorResponse;
import jakarta.validation.Valid;
import java.lang.reflect.Method;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class ApiExceptionHandlerTests {

  private final ApiExceptionHandler apiExceptionHandler = new ApiExceptionHandler();

  @Test
  void handleMethodArgumentNotValidReturnsSortedValidationErrors() throws Exception {
    Method submitMethod =
        RequestTarget.class.getDeclaredMethod("submit", EventOutcomeRequest.class);
    MethodParameter methodParameter = new MethodParameter(submitMethod, 0);
    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(
            new EventOutcomeRequest("EVT-1001", "Team A vs Team B", "TEAM-A"),
            "eventOutcomeRequest");
    bindingResult.addError(
        new FieldError("eventOutcomeRequest", "eventWinnerId", null, false, null, null, null));
    bindingResult.addError(
        new FieldError(
            "eventOutcomeRequest",
            "eventId",
            null,
            false,
            null,
            null,
            "eventId must not be blank"));
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/v1/event-outcomes");

    ResponseEntity<ErrorResponse> response =
        apiExceptionHandler.handleMethodArgumentNotValid(
            new MethodArgumentNotValidException(methodParameter, bindingResult), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("Request validation failed.");
    assertThat(response.getBody().path()).isEqualTo("/api/v1/event-outcomes");
    assertThat(response.getBody().validationErrors())
        .extracting(ValidationErrorResponse::field, ValidationErrorResponse::message)
        .containsExactly(
            tuple("eventId", "eventId must not be blank"),
            tuple("eventWinnerId", "Validation failed."));
  }

  @Test
  void handleHttpMessageNotReadableReturnsBadRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/v1/event-outcomes");

    ResponseEntity<ErrorResponse> response =
        apiExceptionHandler.handleHttpMessageNotReadable(
            new HttpMessageNotReadableException(
                "Malformed JSON body.", new MockHttpInputMessage(new byte[0])),
            request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("Request body is invalid or malformed.");
    assertThat(response.getBody().validationErrors()).isEmpty();
  }

  @Test
  void handleIllegalArgumentExceptionReturnsBadRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/v1/event-outcomes");

    ResponseEntity<ErrorResponse> response =
        apiExceptionHandler.handleIllegalArgumentException(
            new IllegalArgumentException("eventId must not be blank"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("eventId must not be blank");
  }

  @Test
  void handleEventOutcomePublishFailedExceptionReturnsServiceUnavailable() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/v1/event-outcomes");

    ResponseEntity<ErrorResponse> response =
        apiExceptionHandler.handleEventOutcomePublishFailedException(
            new EventOutcomePublishFailedException(
                "Kafka handoff timed out.", new RuntimeException()),
            request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("Kafka handoff timed out.");
  }

  @Test
  void handleIllegalStateExceptionReturnsConflict() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/v1/event-outcomes");

    ResponseEntity<ErrorResponse> response =
        apiExceptionHandler.handleIllegalStateException(
            new IllegalStateException("Settlement audit already exists."), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("Settlement audit already exists.");
  }

  static final class RequestTarget {

    void submit(@Valid EventOutcomeRequest request) {
      Objects.requireNonNull(request);
    }
  }
}
