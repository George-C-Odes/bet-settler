package io.github.georgecodes.betsettler.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers OpenAPI metadata for the bet-settler REST API. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

  /** Creates a new OpenAPI configuration. */
  public OpenApiConfiguration() {}

  /**
   * Creates the OpenAPI model used by Swagger UI and `/v3/api-docs`.
   *
   * @return configured OpenAPI metadata
   */
  @Bean
  public OpenAPI betSettlerOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("bet-settler API")
                .version("v1")
                .description(
                    "Sports betting settlement trigger and dispatch API backed by Kafka, H2, and RocketMQ-compatible publication modes."))
        .addTagsItem(
            new Tag()
                .name("Event outcomes")
                .description("Publish sports event outcomes into the settlement workflow."))
        .addTagsItem(
            new Tag().name("Bets").description("Inspect seeded bets and event-linked bet data."))
        .addTagsItem(
            new Tag()
                .name("Demo helpers")
                .description(
                    "Local and test-only helper endpoints for resetting transient demo state."));
  }
}
