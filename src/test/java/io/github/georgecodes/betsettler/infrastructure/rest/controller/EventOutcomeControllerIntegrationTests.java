package io.github.georgecodes.betsettler.infrastructure.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.georgecodes.betsettler.application.exception.EventOutcomePublishFailedException;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.ErrorResponse;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.PublishEventOutcomeResponse;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.ValidationErrorResponse;
import io.github.georgecodes.betsettler.testsupport.rest.ConfigurableEventOutcomePublisherTestConfiguration;
import io.github.georgecodes.betsettler.testsupport.rest.ConfigurableEventOutcomePublisherTestConfiguration.ConfigurableEventOutcomePublisher;
import io.github.georgecodes.betsettler.testsupport.rest.HttpRestIntegrationTestSupport;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ConfigurableEventOutcomePublisherTestConfiguration.class)
@ActiveProfiles("test")
class EventOutcomeControllerIntegrationTests extends HttpRestIntegrationTestSupport {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

  @Autowired private ConfigurableEventOutcomePublisher configurableEventOutcomePublisher;

  @Test
  void postEventOutcomeReturnsAcceptedForValidRequest() throws Exception {
    configurableEventOutcomePublisher.succeed();
    HttpResponse<String> response =
        sendEventOutcomePost(
            """
            {
              "eventId": "EVT-1001",
              "eventName": "Team A vs Team B",
              "eventWinnerId": "TEAM-A"
            }
            """);

    PublishEventOutcomeResponse responseBody =
        OBJECT_MAPPER.readValue(response.body(), PublishEventOutcomeResponse.class);

    assertThat(response.statusCode()).isEqualTo(202);
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(
            contentType -> assertThat(contentType).contains(MediaType.APPLICATION_JSON_VALUE));
    assertThat(responseBody.eventId()).isEqualTo("EVT-1001");
    assertThat(responseBody.status()).isEqualTo("ACCEPTED");
    assertThat(responseBody.message())
        .isEqualTo("Event outcome accepted and published to Kafka for asynchronous processing.");
  }

  @Test
  void postEventOutcomeReturnsServiceUnavailableWhenKafkaPublishFails() throws Exception {
    configurableEventOutcomePublisher.failWith(
        new EventOutcomePublishFailedException(
            "Failed to publish event outcome to Kafka topic=event-outcomes-test eventId=EVT-1001. Kafka publish failed before broker acknowledgement.",
            new RuntimeException("Kafka unavailable")));

    HttpResponse<String> response =
        sendEventOutcomePost(
            """
            {
              "eventId": "EVT-1001",
              "eventName": "Team A vs Team B",
              "eventWinnerId": "TEAM-A"
            }
            """);

    ErrorResponse responseBody = OBJECT_MAPPER.readValue(response.body(), ErrorResponse.class);

    assertThat(response.statusCode()).isEqualTo(503);
    assertThat(responseBody.error()).isEqualTo("Service Unavailable");
    assertThat(responseBody.message())
        .isEqualTo(
            "Failed to publish event outcome to Kafka topic=event-outcomes-test eventId=EVT-1001. Kafka publish failed before broker acknowledgement.");
  }

  @Test
  void postEventOutcomeReturnsBadRequestForBlankFields() throws Exception {
    HttpResponse<String> response =
        sendEventOutcomePost(
            """
            {
              "eventId": "   ",
              "eventName": "",
              "eventWinnerId": "   "
            }
            """);

    ErrorResponse responseBody = OBJECT_MAPPER.readValue(response.body(), ErrorResponse.class);

    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(responseBody.status()).isEqualTo(400);
    assertThat(responseBody.error()).isEqualTo("Bad Request");
    assertThat(responseBody.message()).isEqualTo("Request validation failed.");
    assertThat(responseBody.validationErrors())
        .extracting(ValidationErrorResponse::field)
        .containsExactly("eventId", "eventName", "eventWinnerId");
  }

  @Test
  void postEventOutcomeReturnsBadRequestForMalformedJson() throws Exception {
    HttpResponse<String> response = sendEventOutcomePost("{\"eventId\":\"EVT-1001\",");

    ErrorResponse responseBody = OBJECT_MAPPER.readValue(response.body(), ErrorResponse.class);

    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(responseBody.message()).isEqualTo("Request body is invalid or malformed.");
  }
}
