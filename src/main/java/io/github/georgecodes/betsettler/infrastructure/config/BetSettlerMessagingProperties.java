package io.github.georgecodes.betsettler.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Externalized messaging configuration for the bet settler service.
 *
 * @param kafka Kafka messaging properties
 * @param rocketmq RocketMQ messaging properties
 */
@Validated
@ConfigurationProperties(prefix = "betsettler.messaging")
public record BetSettlerMessagingProperties(@Valid Kafka kafka, @Valid RocketMq rocketmq) {

  /**
   * Creates messaging properties with sensible defaults for local development.
   *
   * @param kafka Kafka messaging properties
   * @param rocketmq RocketMQ messaging properties
   */
  public BetSettlerMessagingProperties {
    kafka = kafka == null ? new Kafka(null, null) : kafka;
    rocketmq = rocketmq == null ? new RocketMq(null, null) : rocketmq;
  }

  /**
   * Kafka messaging properties.
   *
   * @param eventOutcomesTopic topic used for published sports event outcomes
   * @param publishAckTimeout maximum time to wait for the Kafka broker acknowledgement before the
   *     API publish request is failed
   */
  public record Kafka(@NotBlank String eventOutcomesTopic, Duration publishAckTimeout) {

    /** Default topic used for event outcomes. */
    public static final String DEFAULT_EVENT_OUTCOMES_TOPIC = "event-outcomes";

    /** Default maximum time to wait for a Kafka broker acknowledgement. */
    public static final Duration DEFAULT_PUBLISH_ACK_TIMEOUT = Duration.ofSeconds(3);

    /**
     * Creates Kafka messaging properties with a default event outcomes topic.
     *
     * @param eventOutcomesTopic topic used for published sports event outcomes
     * @param publishAckTimeout maximum time to wait for the Kafka broker acknowledgement before the
     *     API publish request is failed
     */
    public Kafka {
      eventOutcomesTopic = normalizeTopic(eventOutcomesTopic);
      publishAckTimeout = normalizePublishAckTimeout(publishAckTimeout);
    }

    private static String normalizeTopic(String eventOutcomesTopic) {
      if (eventOutcomesTopic == null) {
        return DEFAULT_EVENT_OUTCOMES_TOPIC;
      }
      String trimmedTopic = eventOutcomesTopic.trim();
      if (trimmedTopic.isEmpty()) {
        throw new IllegalArgumentException("kafka.eventOutcomesTopic must not be blank");
      }
      return trimmedTopic;
    }

    private static Duration normalizePublishAckTimeout(Duration publishAckTimeout) {
      if (publishAckTimeout == null) {
        return DEFAULT_PUBLISH_ACK_TIMEOUT;
      }
      if (publishAckTimeout.isZero() || publishAckTimeout.isNegative()) {
        throw new IllegalArgumentException("kafka.publishAckTimeout must be positive");
      }
      return publishAckTimeout;
    }
  }

  /**
   * RocketMQ messaging properties.
   *
   * @param betSettlementsTopic topic used for published bet settlements
   * @param publisherMode settlement publisher implementation mode; explicit RocketMQ mode requires
   *     RocketMQ transport wiring to be available at startup
   */
  public record RocketMq(@NotBlank String betSettlementsTopic, PublisherMode publisherMode) {

    /** Default topic used for bet settlement publications. */
    public static final String DEFAULT_BET_SETTLEMENTS_TOPIC = "bet-settlements";

    /** Default publisher mode used when RocketMQ is not explicitly enabled. */
    public static final PublisherMode DEFAULT_PUBLISHER_MODE = PublisherMode.LOGGING;

    /**
     * Creates RocketMQ messaging properties with sensible defaults.
     *
     * @param betSettlementsTopic topic used for published bet settlements
     * @param publisherMode settlement publisher implementation mode
     */
    public RocketMq {
      betSettlementsTopic = normalizeTopic(betSettlementsTopic);
      publisherMode = publisherMode == null ? DEFAULT_PUBLISHER_MODE : publisherMode;
    }

    private static String normalizeTopic(String betSettlementsTopic) {
      if (betSettlementsTopic == null) {
        return DEFAULT_BET_SETTLEMENTS_TOPIC;
      }
      String trimmedTopic = betSettlementsTopic.trim();
      if (trimmedTopic.isEmpty()) {
        throw new IllegalArgumentException("rocketmq.betSettlementsTopic must not be blank");
      }
      return trimmedTopic;
    }
  }

  /** Supported settlement publisher modes. */
  public enum PublisherMode {
    /**
     * Publishes settlement messages to RocketMQ and fails startup if the transport is unavailable.
     */
    ROCKETMQ,

    /** Logs settlement payloads instead of sending to RocketMQ. */
    LOGGING
  }
}
