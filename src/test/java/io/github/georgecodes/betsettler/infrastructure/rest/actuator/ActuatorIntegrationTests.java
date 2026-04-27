package io.github.georgecodes.betsettler.infrastructure.rest.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.georgecodes.betsettler.testsupport.rest.ConfigurableEventOutcomePublisherTestConfiguration;
import io.github.georgecodes.betsettler.testsupport.rest.HttpRestIntegrationTestSupport;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ConfigurableEventOutcomePublisherTestConfiguration.class)
@ActiveProfiles("test")
class ActuatorIntegrationTests extends HttpRestIntegrationTestSupport {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

  @Test
  void actuatorInfoReturnsMessagingAndProfileDetails() throws Exception {
    HttpResponse<String> response = sendGet(ACTUATOR_INFO_PATH);
    JsonNode responseBody = OBJECT_MAPPER.readTree(response.body());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(responseBody.path("betSettler").path("description").asText())
        .isEqualTo("Sports betting settlement trigger and dispatch service");
    assertThat(responseBody.path("betSettler").path("activeProfiles"))
        .extracting(JsonNode::asText)
        .containsExactly("test");
    assertThat(
            responseBody.path("betSettler").path("messaging").path("eventOutcomesTopic").asText())
        .isEqualTo("event-outcomes-test");
    assertThat(
            responseBody
                .path("betSettler")
                .path("messaging")
                .path("kafkaPublishAckTimeout")
                .asText())
        .isEqualTo("PT3S");
    assertThat(
            responseBody.path("betSettler").path("messaging").path("betSettlementsTopic").asText())
        .isEqualTo("bet-settlements-test");
    assertThat(responseBody.path("betSettler").path("messaging").path("publisherMode").asText())
        .isEqualTo("logging");
  }

  @Test
  void actuatorMetricsExposeCustomSettlementFlowMeters() throws Exception {
    double acceptedBefore = metricValue("betsettler.event.outcomes.accepted");
    double resetInvocationsBefore = metricValue("betsettler.demo.reset.invocations");

    sendEventOutcomePost(
        """
        {
          "eventId": "EVT-1001",
          "eventName": "Team A vs Team B",
          "eventWinnerId": "TEAM-A"
        }
        """);
    sendDemoResetPost();

    HttpResponse<String> metricsIndexResponse = sendGet(ACTUATOR_METRICS_PATH);
    JsonNode metricsIndex = OBJECT_MAPPER.readTree(metricsIndexResponse.body());
    HttpResponse<String> acceptedMetricResponse =
        sendGet(ACTUATOR_METRICS_PATH + "/betsettler.event.outcomes.accepted");
    HttpResponse<String> resetMetricResponse =
        sendGet(ACTUATOR_METRICS_PATH + "/betsettler.demo.reset.invocations");
    JsonNode acceptedMetric = OBJECT_MAPPER.readTree(acceptedMetricResponse.body());
    JsonNode resetMetric = OBJECT_MAPPER.readTree(resetMetricResponse.body());

    assertThat(metricsIndexResponse.statusCode()).isEqualTo(200);
    assertThat(metricsIndex.path("names"))
        .extracting(JsonNode::asText)
        .contains(
            "betsettler.event.outcomes.accepted",
            "betsettler.demo.reset.invocations",
            "betsettler.demo.reset.rows.cleared");
    assertThat(acceptedMetricResponse.statusCode()).isEqualTo(200);
    assertThat(acceptedMetric.path("measurements").get(0).path("value").asDouble())
        .isEqualTo(acceptedBefore + 1.0d);
    assertThat(resetMetricResponse.statusCode()).isEqualTo(200);
    assertThat(resetMetric.path("measurements").get(0).path("value").asDouble())
        .isEqualTo(resetInvocationsBefore + 1.0d);
  }

  private double metricValue(String metricName) throws Exception {
    HttpResponse<String> response = sendGet(ACTUATOR_METRICS_PATH + "/" + metricName);
    if (response.statusCode() == 404) {
      return 0.0d;
    }
    JsonNode responseBody = OBJECT_MAPPER.readTree(response.body());
    if (!responseBody.path("measurements").isArray()
        || responseBody.path("measurements").isEmpty()) {
      return 0.0d;
    }
    return responseBody.path("measurements").get(0).path("value").asDouble();
  }
}
