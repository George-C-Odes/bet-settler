package io.github.georgecodes.betsettler.application.exception;

/** Raised when an event outcome cannot be handed off to Kafka within the API publish contract. */
public class EventOutcomePublishFailedException extends IllegalStateException {

  /**
   * Creates a new publish-failed exception.
   *
   * @param message human-readable failure reason
   * @param cause underlying publish failure
   */
  public EventOutcomePublishFailedException(String message, Throwable cause) {
    super(message, cause);
  }
}
