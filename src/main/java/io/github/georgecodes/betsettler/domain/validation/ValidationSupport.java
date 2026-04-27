package io.github.georgecodes.betsettler.domain.validation;

import java.math.BigDecimal;
import java.util.Objects;

/** Provides reusable validation helpers for immutable value objects. */
public final class ValidationSupport {

  /** Prevents instantiation of the validation helper type. */
  private ValidationSupport() {}

  /**
   * Requires a non-null, non-blank string value.
   *
   * @param value string value to validate
   * @param fieldName logical field name used in validation messages
   * @return trimmed non-blank string value
   */
  public static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    String trimmedValue = value.trim();
    if (trimmedValue.isEmpty()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return trimmedValue;
  }

  /**
   * Requires a non-null positive monetary or numeric value.
   *
   * @param value numeric value to validate
   * @param fieldName logical field name used in validation messages
   * @return positive numeric value
   */
  public static BigDecimal requirePositive(BigDecimal value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    if (value.signum() <= 0) {
      throw new IllegalArgumentException(fieldName + " must be greater than zero");
    }
    return value;
  }
}
