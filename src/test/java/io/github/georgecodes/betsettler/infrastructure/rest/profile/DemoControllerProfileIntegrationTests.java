package io.github.georgecodes.betsettler.infrastructure.rest.profile;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.georgecodes.betsettler.testsupport.rest.HttpRestIntegrationTestSupport;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoControllerProfileIntegrationTests extends HttpRestIntegrationTestSupport {

  @Test
  void postDemoResetIsUnavailableWhenLocalAndTestProfilesAreInactive() throws Exception {
    HttpResponse<String> response = sendDemoResetPost();

    assertThat(response.statusCode()).isEqualTo(404);
  }
}
