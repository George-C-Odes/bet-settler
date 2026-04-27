package io.github.georgecodes.betsettler.application.model.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RetryableSettlementAuditTests {

  @Test
  void constructorNormalizesFields() {
    RetryableSettlementAudit audit =
        new RetryableSettlementAudit(
            " EVT-1001 ",
            " BET-1001 ",
            " USER-1 ",
            " FAILED ",
            " bet-settlements ",
            " {\"betId\":\"BET-1001\"} ");

    assertThat(audit.eventId()).isEqualTo("EVT-1001");
    assertThat(audit.betId()).isEqualTo("BET-1001");
    assertThat(audit.userId()).isEqualTo("USER-1");
    assertThat(audit.publishStatus()).isEqualTo("FAILED");
    assertThat(audit.destinationTopic()).isEqualTo("bet-settlements");
    assertThat(audit.payloadSnapshot()).isEqualTo("{\"betId\":\"BET-1001\"}");
  }

  @Test
  void constructorRejectsBlankRequiredFields() {
    assertThatThrownBy(
            () ->
                new RetryableSettlementAudit(
                    " ", "BET-1001", "USER-1", "FAILED", "bet-settlements", "{}"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("eventId must not be blank");
  }
}
