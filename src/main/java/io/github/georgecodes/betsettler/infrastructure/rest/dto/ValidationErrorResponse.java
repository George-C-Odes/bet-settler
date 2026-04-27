package io.github.georgecodes.betsettler.infrastructure.rest.dto;

import io.github.georgecodes.betsettler.domain.validation.ValidationSupport;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Describes a single validation problem for an API request.
 *
 * @param field invalid field or parameter name
 * @param message human-readable validation message
 */
@Schema(description = "Describes a single validation problem for an API request.")
public record ValidationErrorResponse(String field, String message) {

  /**
   * Creates a validated validation-error response item.
   *
   * @param field invalid field or parameter name
   * @param message human-readable validation message
   */
  public ValidationErrorResponse {
    field = requireNonBlank(field, "field");
    message = requireNonBlank(message, "message");
  }

  private static String requireNonBlank(String value, String fieldName) {
    return ValidationSupport.requireNonBlank(value, fieldName);
  }
}
