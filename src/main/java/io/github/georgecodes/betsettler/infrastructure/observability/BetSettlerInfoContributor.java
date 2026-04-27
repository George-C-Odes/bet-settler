package io.github.georgecodes.betsettler.infrastructure.observability;

import io.github.georgecodes.betsettler.infrastructure.config.BetSettlerMessagingProperties;
import io.github.georgecodes.betsettler.infrastructure.config.DemoProfiles;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Contributes service-specific runtime details to the Actuator info endpoint. */
@Component
public class BetSettlerInfoContributor implements InfoContributor {

  /** Spring environment used to resolve active profiles. */
  private final Environment environment;

  /** Externalized messaging properties exposed through the info endpoint. */
  private final BetSettlerMessagingProperties messagingProperties;

  /**
   * Creates a new bet-settler Actuator info contributor.
   *
   * @param environment Spring environment used to resolve active profiles
   * @param messagingProperties externalized messaging properties
   */
  public BetSettlerInfoContributor(
      Environment environment, BetSettlerMessagingProperties messagingProperties) {
    this.environment = Objects.requireNonNull(environment, "environment must not be null");
    this.messagingProperties =
        Objects.requireNonNull(messagingProperties, "messagingProperties must not be null");
  }

  @Override
  public void contribute(Info.Builder builder) {
    builder.withDetail(
        "betSettler",
        Map.of(
            "description",
            "Sports betting settlement trigger and dispatch service",
            "activeProfiles",
            activeProfiles(),
            "messaging",
            Map.of(
                "eventOutcomesTopic", messagingProperties.kafka().eventOutcomesTopic(),
                "kafkaPublishAckTimeout",
                    messagingProperties.kafka().publishAckTimeout().toString(),
                "betSettlementsTopic", messagingProperties.rocketmq().betSettlementsTopic(),
                "publisherMode",
                    messagingProperties.rocketmq().publisherMode().name().toLowerCase(Locale.ROOT)),
            "demoResetEndpointProfiles",
            DemoProfiles.DEMO_RESET_ENDPOINT_PROFILES));
  }

  private List<String> activeProfiles() {
    String[] activeProfiles = environment.getActiveProfiles();
    if (activeProfiles.length == 0) {
      return List.of("default");
    }
    return Arrays.stream(activeProfiles).sorted().toList();
  }
}
