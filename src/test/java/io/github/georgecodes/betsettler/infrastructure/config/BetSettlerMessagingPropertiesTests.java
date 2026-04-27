package io.github.georgecodes.betsettler.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class BetSettlerMessagingPropertiesTests {

  @Test
  void constructorDefaultsKafkaPropertiesWhenMissing() {
    BetSettlerMessagingProperties properties = new BetSettlerMessagingProperties(null, null);

    assertThat(properties.kafka().eventOutcomesTopic()).isEqualTo("event-outcomes");
    assertThat(properties.kafka().publishAckTimeout()).isEqualTo(Duration.ofSeconds(3));
    assertThat(properties.rocketmq().betSettlementsTopic()).isEqualTo("bet-settlements");
    assertThat(properties.rocketmq().publisherMode())
        .isEqualTo(BetSettlerMessagingProperties.PublisherMode.LOGGING);
  }

  @Test
  void kafkaPropertiesDefaultTopicWhenNullValueIsProvided() {
    BetSettlerMessagingProperties.Kafka kafkaProperties =
        new BetSettlerMessagingProperties.Kafka(null, null);

    assertThat(kafkaProperties.eventOutcomesTopic()).isEqualTo("event-outcomes");
    assertThat(kafkaProperties.publishAckTimeout()).isEqualTo(Duration.ofSeconds(3));
  }

  @Test
  void kafkaPropertiesRejectBlankTopic() {
    assertThatThrownBy(() -> new BetSettlerMessagingProperties.Kafka("   ", Duration.ofSeconds(3)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("kafka.eventOutcomesTopic must not be blank");
  }

  @Test
  void kafkaPropertiesRejectNonPositiveAcknowledgementTimeout() {
    assertThatThrownBy(
            () -> new BetSettlerMessagingProperties.Kafka("event-outcomes", Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("kafka.publishAckTimeout must be positive");
  }

  @Test
  void rocketMqPropertiesDefaultTopicAndPublisherModeWhenNullValuesAreProvided() {
    BetSettlerMessagingProperties.RocketMq rocketMqProperties =
        new BetSettlerMessagingProperties.RocketMq(null, null);

    assertThat(rocketMqProperties.betSettlementsTopic()).isEqualTo("bet-settlements");
    assertThat(rocketMqProperties.publisherMode())
        .isEqualTo(BetSettlerMessagingProperties.PublisherMode.LOGGING);
  }

  @Test
  void rocketMqPropertiesRejectBlankTopic() {
    assertThatThrownBy(
            () ->
                new BetSettlerMessagingProperties.RocketMq(
                    "   ", BetSettlerMessagingProperties.PublisherMode.ROCKETMQ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("rocketmq.betSettlementsTopic must not be blank");
  }
}
