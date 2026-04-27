package io.github.georgecodes.betsettler.testsupport.rest;

import io.github.georgecodes.betsettler.application.model.audit.PendingSettlementAudit;
import io.github.georgecodes.betsettler.application.port.out.BetQueryPort;
import io.github.georgecodes.betsettler.application.port.out.ProcessedEventOutcomePort;
import io.github.georgecodes.betsettler.application.port.out.SettlementAuditPort;
import io.github.georgecodes.betsettler.domain.model.Bet;
import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.domain.model.EventOutcome;
import io.github.georgecodes.betsettler.domain.model.SettlementResult;
import io.github.georgecodes.betsettler.infrastructure.persistence.repository.SpringDataBetRepository;
import io.github.georgecodes.betsettler.infrastructure.persistence.repository.SpringDataProcessedEventOutcomeRepository;
import io.github.georgecodes.betsettler.infrastructure.persistence.repository.SpringDataSettlementAuditRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class SettlementAuditRestIntegrationTestSupport
    extends HttpRestIntegrationTestSupport {

  @Autowired protected BetQueryPort betQueryPort;

  @Autowired protected ProcessedEventOutcomePort processedEventOutcomePort;

  @Autowired protected SettlementAuditPort settlementAuditPort;

  @Autowired protected SpringDataBetRepository betRepository;

  @Autowired protected SpringDataProcessedEventOutcomeRepository processedEventOutcomeRepository;

  @Autowired protected SpringDataSettlementAuditRepository settlementAuditRepository;

  @AfterEach
  protected void cleanTransientState() {
    settlementAuditRepository.deleteAllInBatch();
    processedEventOutcomeRepository.deleteAllInBatch();
  }

  protected BetSettlement createWinningSettlementForSeededBet() {
    return createSettlementForSeededBet("BET-1001", SettlementResult.WIN);
  }

  protected BetSettlement createLosingSettlementForSeededBet() {
    return createSettlementForSeededBet("BET-1002", SettlementResult.LOSE);
  }

  protected PendingSettlementAudit createPendingSettlementAudit(
      BetSettlement settlement, String payloadSnapshot) {
    return new PendingSettlementAudit(settlement, "bet-settlements", payloadSnapshot);
  }

  private BetSettlement createSettlementForSeededBet(
      String betId, SettlementResult settlementResult) {
    Bet bet =
        betQueryPort.getAllBets().stream()
            .filter(candidateBet -> candidateBet.betId().equals(betId))
            .findFirst()
            .orElseThrow();
    EventOutcome eventOutcome = new EventOutcome(bet.eventId(), "Team A vs Team B", "TEAM-A");
    return new BetSettlement(bet, eventOutcome, settlementResult);
  }
}
