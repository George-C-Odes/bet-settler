package io.github.georgecodes.betsettler.infrastructure.rest.error;

import io.github.georgecodes.betsettler.application.exception.EventOutcomePublishFailedException;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.ErrorResponse;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.ValidationErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Centralized translation of common API exceptions into stable REST error payloads. */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

  /** Creates a new API exception handler. */
  public ApiExceptionHandler() {}

  /**
   * Handles Bean Validation failures for request bodies.
   *
   * @param exception validation exception raised by Spring MVC
   * @param request current HTTP request
   * @return structured error payload
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    List<ValidationErrorResponse> validationErrors =
        exception.getBindingResult().getFieldErrors().stream()
            .sorted(Comparator.comparing(FieldError::getField))
            .map(this::toValidationErrorResponse)
            .toList();
    return buildResponse(
        HttpStatus.BAD_REQUEST,
        "Request validation failed.",
        request.getRequestURI(),
        validationErrors);
  }

  /**
   * Handles malformed JSON or incompatible request bodies.
   *
   * @param exception request body parsing exception
   * @param request current HTTP request
   * @return structured error payload
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    log.debug("Malformed request body for path={}", request.getRequestURI(), exception);
    return buildResponse(
        HttpStatus.BAD_REQUEST,
        "Request body is invalid or malformed.",
        request.getRequestURI(),
        List.of());
  }

  /**
   * Handles invalid user input that passed transport-level parsing.
   *
   * @param exception application or domain validation exception
   * @param request current HTTP request
   * @return structured error payload
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
      IllegalArgumentException exception, HttpServletRequest request) {
    return buildResponse(
        HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI(), List.of());
  }

  /**
   * Handles broker handoff failures for event-outcome publication.
   *
   * @param exception publish failure exception
   * @param request current HTTP request
   * @return structured error payload
   */
  @ExceptionHandler(EventOutcomePublishFailedException.class)
  public ResponseEntity<ErrorResponse> handleEventOutcomePublishFailedException(
      EventOutcomePublishFailedException exception, HttpServletRequest request) {
    return buildResponse(
        HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request.getRequestURI(), List.of());
  }

  /**
   * Handles expected application state conflicts.
   *
   * @param exception application state exception
   * @param request current HTTP request
   * @return structured error payload
   */
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalStateException(
      IllegalStateException exception, HttpServletRequest request) {
    return buildResponse(
        HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI(), List.of());
  }

  private ValidationErrorResponse toValidationErrorResponse(FieldError fieldError) {
    return new ValidationErrorResponse(
        fieldError.getField(),
        fieldError.getDefaultMessage() == null
            ? "Validation failed."
            : fieldError.getDefaultMessage());
  }

  private ResponseEntity<ErrorResponse> buildResponse(
      HttpStatus status,
      String message,
      String path,
      List<ValidationErrorResponse> validationErrors) {
    return ResponseEntity.status(status)
        .body(
            new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                validationErrors));
  }
}
