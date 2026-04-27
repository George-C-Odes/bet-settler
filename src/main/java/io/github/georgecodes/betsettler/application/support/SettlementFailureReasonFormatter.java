package io.github.georgecodes.betsettler.application.support;

import java.util.Objects;
import org.springframework.stereotype.Component;

/** Formats settlement publication failures for persistence in the settlement audit. */
@Component
public class SettlementFailureReasonFormatter {

  /** Maximum length supported by the settlement audit failure reason column. */
  public static final int MAX_FAILURE_REASON_LENGTH = 512;

  /** Creates a new settlement failure reason formatter. */
  public SettlementFailureReasonFormatter() {}

  /**
   * Formats the supplied throwable into a bounded audit-friendly failure reason.
   *
   * @param throwable throwable to format
   * @return formatted failure reason
   */
  public String format(Throwable throwable) {
    Objects.requireNonNull(throwable, "throwable must not be null");
    String exceptionType = throwable.getClass().getSimpleName();
    String message = throwable.getMessage();
    String failureReason =
        message == null || message.isBlank()
            ? exceptionType
            : exceptionType + ": " + message.trim();
    if (failureReason.length() <= MAX_FAILURE_REASON_LENGTH) {
      return failureReason;
    }
    return failureReason.substring(0, MAX_FAILURE_REASON_LENGTH);
  }
}
