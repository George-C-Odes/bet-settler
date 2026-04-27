package io.github.georgecodes.betsettler.infrastructure.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.BetResponse;
import io.github.georgecodes.betsettler.testsupport.rest.HttpRestIntegrationTestSupport;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BetQueryControllerIntegrationTests extends HttpRestIntegrationTestSupport {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

  @Test
  void getBetsReturnsAllSeededBetsWhenFilterIsOmitted() throws Exception {
    HttpResponse<String> response = sendGet(BETS_PATH);
    BetResponse[] responseBody = OBJECT_MAPPER.readValue(response.body(), BetResponse[].class);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(
            contentType -> assertThat(contentType).contains(MediaType.APPLICATION_JSON_VALUE));
    assertThat(responseBody)
        .extracting(BetResponse::betId)
        .containsExactly("BET-1001", "BET-1002", "BET-2001", "BET-3001");
  }

  @Test
  void getBetsFiltersByEventId() throws Exception {
    HttpResponse<String> response = sendGet(BETS_PATH + "?eventId=%20EVT-1001%20");
    BetResponse[] responseBody = OBJECT_MAPPER.readValue(response.body(), BetResponse[].class);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(responseBody).extracting(BetResponse::betId).containsExactly("BET-1001", "BET-1002");
  }

  @Test
  void getBetsTreatsBlankEventIdAsNoFilter() throws Exception {
    HttpResponse<String> response = sendGet(BETS_PATH + "?eventId=%20%20%20");
    BetResponse[] responseBody = OBJECT_MAPPER.readValue(response.body(), BetResponse[].class);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(responseBody)
        .extracting(BetResponse::betId)
        .containsExactly("BET-1001", "BET-1002", "BET-2001", "BET-3001");
  }
}
