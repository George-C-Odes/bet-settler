package io.github.georgecodes.betsettler.testsupport.rest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;

//noinspection unused
public abstract class HttpRestIntegrationTestSupport {

  //noinspection unused
  protected static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
  //noinspection unused
  protected static final String EVENT_OUTCOMES_PATH = "/api/v1/event-outcomes";
  //noinspection unused
  protected static final String BETS_PATH = "/api/v1/bets";
  //noinspection unused
  protected static final String DEMO_RESET_PATH = "/api/v1/internal/demo/reset";
  //noinspection unused
  protected static final String DEMO_RETRY_SETTLEMENTS_PATH =
      "/api/v1/internal/demo/retry-settlements";
  //noinspection unused
  protected static final String ACTUATOR_INFO_PATH = "/actuator/info";
  //noinspection unused
  protected static final String ACTUATOR_METRICS_PATH = "/actuator/metrics";
  //noinspection unused
  protected static final String OPEN_API_DOCS_PATH = "/v3/api-docs";
  //noinspection unused
  protected static final String SWAGGER_UI_PATH = "/swagger-ui/index.html";

  @LocalServerPort protected int port;

  //noinspection unused
  protected HttpResponse<String> sendEventOutcomePost(String body) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(apiUri(EVENT_OUTCOMES_PATH))
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
    return HTTP_CLIENT.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  //noinspection unused
  protected HttpResponse<String> sendDemoResetPost() throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(apiUri(DEMO_RESET_PATH))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
    return HTTP_CLIENT.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  //noinspection unused
  protected HttpResponse<String> sendRetrySettlementsPost() throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(apiUri(DEMO_RETRY_SETTLEMENTS_PATH))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
    return HTTP_CLIENT.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  //noinspection unused
  protected HttpResponse<String> sendGet(String path) throws Exception {
    HttpRequest request = HttpRequest.newBuilder(apiUri(path)).GET().build();
    return HTTP_CLIENT.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  protected URI apiUri(String path) {
    return URI.create("http://localhost:" + port + path);
  }
}
