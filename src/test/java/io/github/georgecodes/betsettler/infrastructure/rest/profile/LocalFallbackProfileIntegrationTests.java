package io.github.georgecodes.betsettler.infrastructure.rest.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.georgecodes.betsettler.application.port.out.EventOutcomePublisherPort;
import io.github.georgecodes.betsettler.infrastructure.config.DemoProfiles;
import io.github.georgecodes.betsettler.testsupport.rest.HttpRestIntegrationTestSupport;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.kafka.listener.auto-startup=false")
@Import(LocalFallbackProfileIntegrationTests.LocalFallbackTestConfiguration.class)
@ActiveProfiles("local-fallback")
class LocalFallbackProfileIntegrationTests extends HttpRestIntegrationTestSupport {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
  private static final int H2_TCP_PORT = findAvailablePort();

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("betsettler.h2.tcp.port", () -> H2_TCP_PORT);
  }

  @Test
  void postDemoResetIsAvailableInLocalFallbackProfile() throws Exception {
    HttpResponse<String> response = sendDemoResetPost();

    assertThat(response.statusCode()).isEqualTo(200);
  }

  @Test
  void actuatorInfoReflectsLocalFallbackMessagingConfiguration() throws Exception {
    HttpResponse<String> response = sendGet(ACTUATOR_INFO_PATH);
    JsonNode responseBody = OBJECT_MAPPER.readTree(response.body());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(responseBody.path("betSettler").path("activeProfiles"))
        .extracting(JsonNode::asText)
        .containsExactly("local-fallback");
    assertThat(responseBody.path("betSettler").path("messaging").path("publisherMode").asText())
        .isEqualTo("logging");
    assertThat(responseBody.path("betSettler").path("demoResetEndpointProfiles"))
        .extracting(JsonNode::asText)
        .containsExactlyElementsOf(DemoProfiles.DEMO_RESET_ENDPOINT_PROFILES);
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class LocalFallbackTestConfiguration {

    @Bean
    @Primary
    EventOutcomePublisherPort eventOutcomePublisherPort() {
      return ignoredEventOutcome -> {};
    }
  }

  private static int findAvailablePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Failed to allocate an available H2 TCP port for tests", exception);
    }
  }
}
