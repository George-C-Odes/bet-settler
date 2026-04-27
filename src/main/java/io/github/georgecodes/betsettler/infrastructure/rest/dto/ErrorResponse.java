package io.github.georgecodes.betsettler.infrastructure.rest.dto;

import io.github.georgecodes.betsettler.domain.validation.ValidationSupport;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Standard error payload returned by the REST API.
 *
 * @param timestamp timestamp when the error response was created
 * @param status HTTP status code
 * @param error HTTP reason phrase
 * @param message human-readable error message
 * @param path request path that produced the error
 * @param validationErrors field-level validation details, if any
 */
@Schema(description = "Standard error payload returned by the REST API.")
public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    List<ValidationErrorResponse> validationErrors) {

  /**
   * Creates a validated error response.
   *
   * @param timestamp timestamp when the error response was created
   * @param status HTTP status code
   * @param error HTTP reason phrase
   * @param message human-readable error message
   * @param path request path that produced the error
   * @param validationErrors field-level validation details, if any
   */
  public ErrorResponse {
    Objects.requireNonNull(timestamp, "timestamp must not be null");
    if (status < 100) {
      throw new IllegalArgumentException("status must be a valid HTTP status code");
    }
    error = requireNonBlank(error, "error");
    message = requireNonBlank(message, "message");
    path = requireNonBlank(path, "path");
    validationErrors =
        List.copyOf(Objects.requireNonNull(validationErrors, "validationErrors must not be null"));
  }

  private static String requireNonBlank(String value, String fieldName) {
    return ValidationSupport.requireNonBlank(value, fieldName);
  }
}
