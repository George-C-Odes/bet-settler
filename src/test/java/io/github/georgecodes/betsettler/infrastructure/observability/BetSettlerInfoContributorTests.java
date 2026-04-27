package io.github.georgecodes.betsettler.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.georgecodes.betsettler.infrastructure.config.BetSettlerMessagingProperties;
import io.github.georgecodes.betsettler.infrastructure.config.DemoProfiles;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;
import org.springframework.mock.env.MockEnvironment;

class BetSettlerInfoContributorTests {

  @Test
  void contributeUsesDefaultProfileWhenNoProfilesAreActive() {
    Info.Builder builder = getBuilder1();

    @SuppressWarnings("unchecked")
    Map<String, Object> details =
        (Map<String, Object>) builder.build().getDetails().get("betSettler");
    assertThat(details.get("description"))
        .isEqualTo("Sports betting settlement trigger and dispatch service");
    assertThat(details.get("activeProfiles")).isEqualTo(List.of("default"));
    assertThat(details.get("demoResetEndpointProfiles"))
        .isEqualTo(DemoProfiles.DEMO_RESET_ENDPOINT_PROFILES);
    @SuppressWarnings("unchecked")
    Map<String, Object> messaging = (Map<String, Object>) details.get("messaging");
    assertThat(messaging)
        .containsEntry("eventOutcomesTopic", "event-outcomes-local")
        .containsEntry("kafkaPublishAckTimeout", "PT5S")
        .containsEntry("betSettlementsTopic", "bet-settlements-local")
        .containsEntry("publisherMode", "logging");
  }

  private static Info.@NonNull Builder getBuilder1() {
    BetSettlerInfoContributor contributor =
        new BetSettlerInfoContributor(
            new MockEnvironment(),
            new BetSettlerMessagingProperties(
                new BetSettlerMessagingProperties.Kafka(
                    "event-outcomes-local", Duration.ofSeconds(5)),
                new BetSettlerMessagingProperties.RocketMq(
                    "bet-settlements-local", BetSettlerMessagingProperties.PublisherMode.LOGGING)));
    Info.Builder builder = new Info.Builder();

    contributor.contribute(builder);
    return builder;
  }

  @Test
  void contributeSortsActiveProfilesAndLowercasesPublisherMode() {
    Info.Builder builder = getBuilder();

    @SuppressWarnings("unchecked")
    Map<String, Object> details =
        (Map<String, Object>) builder.build().getDetails().get("betSettler");
    @SuppressWarnings("unchecked")
    Map<String, Object> messaging = (Map<String, Object>) details.get("messaging");
    assertThat(details.get("activeProfiles")).isEqualTo(List.of("local-fallback", "test"));
    assertThat(messaging.get("eventOutcomesTopic")).isEqualTo("event-outcomes");
    assertThat(messaging.get("kafkaPublishAckTimeout")).isEqualTo("PT3S");
    assertThat(messaging.get("betSettlementsTopic")).isEqualTo("bet-settlements-docker");
    assertThat(messaging.get("publisherMode")).isEqualTo("rocketmq");
  }

  private static Info.@NonNull Builder getBuilder() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("test", "local-fallback");
    BetSettlerInfoContributor contributor =
        new BetSettlerInfoContributor(
            environment,
            new BetSettlerMessagingProperties(
                null,
                new BetSettlerMessagingProperties.RocketMq(
                    "bet-settlements-docker",
                    BetSettlerMessagingProperties.PublisherMode.ROCKETMQ)));
    Info.Builder builder = new Info.Builder();

    contributor.contribute(builder);
    return builder;
  }

  @Test
  void constructorRejectsNullDependencies() {
    BetSettlerMessagingProperties properties = new BetSettlerMessagingProperties(null, null);

    assertThatThrownBy(() -> new BetSettlerInfoContributor(null, properties))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("environment must not be null");
    assertThatThrownBy(() -> new BetSettlerInfoContributor(new MockEnvironment(), null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("messagingProperties must not be null");
  }
}
