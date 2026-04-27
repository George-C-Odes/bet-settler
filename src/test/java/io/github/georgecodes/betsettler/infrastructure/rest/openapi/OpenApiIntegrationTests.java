package io.github.georgecodes.betsettler.infrastructure.rest.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.georgecodes.betsettler.testsupport.rest.HttpRestIntegrationTestSupport;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OpenApiIntegrationTests extends HttpRestIntegrationTestSupport {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

  @Test
  void openApiDocsExposeConfiguredMetadataAndRestPaths() throws Exception {
    HttpResponse<String> response = sendGet(OPEN_API_DOCS_PATH);
    JsonNode responseBody = OBJECT_MAPPER.readTree(response.body());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(contentType -> assertThat(contentType).contains("application/json"));
    assertThat(responseBody.path("info").path("title").asText()).isEqualTo("bet-settler API");
    assertThat(responseBody.path("paths").has(EVENT_OUTCOMES_PATH)).isTrue();
    assertThat(responseBody.path("paths").has(BETS_PATH)).isTrue();
    assertThat(responseBody.path("paths").has(DEMO_RESET_PATH)).isTrue();
    assertThat(responseBody.path("paths").has(DEMO_RETRY_SETTLEMENTS_PATH)).isTrue();
  }

  @Test
  void swaggerUiIndexIsServed() throws Exception {
    HttpResponse<String> response = sendGet(SWAGGER_UI_PATH);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(
            contentType -> assertThat(contentType).contains(MediaType.TEXT_HTML_VALUE));
    assertThat(response.body()).contains("Swagger UI");
  }
}
