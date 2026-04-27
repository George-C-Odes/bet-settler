package io.github.georgecodes.betsettler.application.model.dispatch;

import static io.github.georgecodes.betsettler.domain.validation.ValidationSupport.requireNonBlank;

import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import java.util.List;
import java.util.Objects;

/** Immutable result of the transactional settlement-preparation stage. */
public sealed interface PreparedDispatchPlan
    permits PreparedDispatchPlan.DispatchRequired,
        PreparedDispatchPlan.Duplicate,
        PreparedDispatchPlan.NoMatch {

  /**
   * Returns the processed event identifier.
   *
   * @return processed event identifier
   */
  String eventId();

  /**
   * Returns the prepared outcome status.
   *
   * @return prepared outcome status
   */
  Status status();

  /**
   * Returns the prepared settlements to dispatch.
   *
   * @return immutable prepared settlement decisions
   */
  List<BetSettlement> settlements();

  /**
   * Creates a duplicate dispatch plan for an event that has already been processed.
   *
   * @param eventId duplicated event identifier
   * @return duplicate dispatch plan with no settlements to publish
   */
  static PreparedDispatchPlan duplicate(String eventId) {
    return new Duplicate(eventId);
  }

  /**
   * Creates a no-match dispatch plan for an event that has no matching bets.
   *
   * @param eventId processed event identifier
   * @param destinationTopic configured logical publication destination
   * @return no-match dispatch plan with no settlements to publish
   */
  static PreparedDispatchPlan noMatch(String eventId, String destinationTopic) {
    return new NoMatch(eventId, destinationTopic);
  }

  /**
   * Creates a dispatch plan for prepared settlements that must be published.
   *
   * @param eventId processed event identifier
   * @param destinationTopic configured logical publication destination
   * @param settlements prepared settlement decisions to dispatch
   * @return dispatchable plan containing prepared settlements
   */
  static PreparedDispatchPlan dispatch(
      String eventId, String destinationTopic, List<BetSettlement> settlements) {
    return new DispatchRequired(eventId, destinationTopic, settlements);
  }

  /**
   * Returns whether the prepared outcome is a duplicate that should be skipped.
   *
   * @return {@code true} when the event outcome was already processed
   */
  default boolean isDuplicate() {
    return status() == Status.DUPLICATE;
  }

  /**
   * Returns whether the prepared outcome includes settlements that must be dispatched.
   *
   * @return {@code true} when the plan contains settlements to publish
   */
  default boolean requiresDispatch() {
    return status() == Status.DISPATCH_REQUIRED;
  }

  /**
   * Returns whether the prepared outcome carries a destination topic.
   *
   * @return {@code true} when a logical publication destination is available
   */
  @SuppressWarnings("unused")
  default boolean hasDestinationTopic() {
    return false;
  }

  /**
   * Returns the required destination topic for non-duplicate outcomes.
   *
   * @return required logical publication destination
   */
  default String requiredDestinationTopic() {
    throw new IllegalStateException("destinationTopic is unavailable for status=" + status());
  }

  /**
   * Duplicate outcome that should be skipped.
   *
   * @param eventId duplicated event identifier
   */
  record Duplicate(String eventId) implements PreparedDispatchPlan {

    /**
     * Creates a duplicate prepared dispatch plan.
     *
     * @param eventId duplicated event identifier
     */
    public Duplicate {
      eventId = requireNonBlank(eventId, "eventId");
    }

    @Override
    public Status status() {
      return Status.DUPLICATE;
    }

    @Override
    public List<BetSettlement> settlements() {
      return List.of();
    }
  }

  /**
   * Accepted outcome with no matching bets.
   *
   * @param eventId processed event identifier
   * @param destinationTopic configured logical publication destination
   */
  record NoMatch(String eventId, String destinationTopic) implements PreparedDispatchPlan {

    /**
     * Creates a prepared dispatch plan for a no-match outcome.
     *
     * @param eventId processed event identifier
     * @param destinationTopic configured logical publication destination
     */
    public NoMatch {
      eventId = requireNonBlank(eventId, "eventId");
      destinationTopic = requireNonBlank(destinationTopic, "destinationTopic");
    }

    @Override
    public Status status() {
      return Status.NO_MATCH;
    }

    @Override
    public List<BetSettlement> settlements() {
      return List.of();
    }

    @Override
    public boolean hasDestinationTopic() {
      return true;
    }

    @Override
    public String requiredDestinationTopic() {
      return destinationTopic;
    }
  }

  /**
   * Accepted outcome with prepared settlements that must be dispatched.
   *
   * @param eventId processed event identifier
   * @param destinationTopic configured logical publication destination
   * @param settlements prepared settlement decisions to dispatch
   */
  record DispatchRequired(String eventId, String destinationTopic, List<BetSettlement> settlements)
      implements PreparedDispatchPlan {

    /**
     * Creates a prepared dispatch plan for dispatchable settlements.
     *
     * @param eventId processed event identifier
     * @param destinationTopic configured logical publication destination
     * @param settlements prepared settlement decisions to dispatch
     */
    public DispatchRequired {
      eventId = requireNonBlank(eventId, "eventId");
      destinationTopic = requireNonBlank(destinationTopic, "destinationTopic");
      settlements =
          List.copyOf(Objects.requireNonNull(settlements, "settlements must not be null"));
      if (settlements.isEmpty()) {
        throw new IllegalArgumentException(
            "settlements must not be empty when status=DISPATCH_REQUIRED");
      }
    }

    @Override
    public Status status() {
      return Status.DISPATCH_REQUIRED;
    }

    @Override
    public boolean hasDestinationTopic() {
      return true;
    }

    @Override
    public String requiredDestinationTopic() {
      return destinationTopic;
    }
  }

  /** Supported prepared dispatch outcomes. */
  enum Status {
    /** The consumed event outcome was already processed and must be skipped. */
    DUPLICATE,

    /** The event outcome was accepted but no matching bets were found. */
    NO_MATCH,

    /** The event outcome produced settlements that must be dispatched. */
    DISPATCH_REQUIRED
  }
}
