package io.github.georgecodes.betsettler.infrastructure.messaging.kafka.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.georgecodes.betsettler.infrastructure.config.BetSettlerMessagingProperties;
import io.github.georgecodes.betsettler.testsupport.KafkaTestcontainersConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.kafka.KafkaContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(KafkaTestcontainersConfiguration.class)
@ActiveProfiles("test")
class KafkaEventOutcomePublishingIntegrationTests {

  private static final Duration KAFKA_POLL_TIMEOUT = Duration.ofMillis(250);
  private static final Duration RECORD_AWAIT_TIMEOUT = Duration.ofSeconds(10);
  private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

  @LocalServerPort private int port;

  @Autowired private KafkaContainer kafkaContainer;

  @Autowired private BetSettlerMessagingProperties messagingProperties;

  @Test
  void postEventOutcomePublishesExpectedMessageToConfiguredKafkaTopic() throws Exception {
    try (Consumer<String, String> consumer = createConsumer()) {
      String topic = messagingProperties.kafka().eventOutcomesTopic();
      consumer.subscribe(List.of(topic));
      consumer.poll(KAFKA_POLL_TIMEOUT);

      HttpResponse<String> response = sendEventOutcomePost();

      ConsumerRecord<String, String> record = awaitSingleRecord(consumer);
      String payloadValue = record.value();
      if (payloadValue == null) {
        throw new AssertionError("Expected the published Kafka payload to be non-null.");
      }
      JsonNode payload = OBJECT_MAPPER.readTree(payloadValue);

      assertThat(response.statusCode()).isEqualTo(202);
      assertThat(record.topic()).isEqualTo(topic);
      assertThat(record.key()).isEqualTo("EVT-5001");
      assertThat(payload.path("eventId").asText()).isEqualTo("EVT-5001");
      assertThat(payload.path("eventName").asText()).isEqualTo("Team G vs Team H");
      assertThat(payload.path("eventWinnerId").asText()).isEqualTo("TEAM-G");
    }
  }

  private Consumer<String, String> createConsumer() {
    Properties properties = new Properties();
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, "phase4-kafka-publish-" + UUID.randomUUID());
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, Boolean.FALSE.toString());
    properties.put(
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    properties.put(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    return new KafkaConsumer<>(properties);
  }

  private ConsumerRecord<String, String> awaitSingleRecord(Consumer<String, String> consumer) {
    long deadline = System.nanoTime() + RECORD_AWAIT_TIMEOUT.toNanos();
    while (System.nanoTime() < deadline) {
      ConsumerRecords<String, String> records = consumer.poll(KAFKA_POLL_TIMEOUT);
      if (!records.isEmpty()) {
        return records.iterator().next();
      }
    }
    throw new AssertionError("Expected an event outcome message to be published to Kafka.");
  }

  private HttpResponse<String> sendEventOutcomePost() throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(eventOutcomesUri())
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    """
                    {
                      "eventId": "EVT-5001",
                      "eventName": "Team G vs Team H",
                      "eventWinnerId": "TEAM-G"
                    }
                    """,
                    StandardCharsets.UTF_8))
            .build();
    return HTTP_CLIENT.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  private URI eventOutcomesUri() {
    return URI.create("http://localhost:" + port + "/api/v1/event-outcomes");
  }
}
