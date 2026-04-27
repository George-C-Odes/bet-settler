package io.github.georgecodes.betsettler.infrastructure.messaging.settlement.mapper;

import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.dto.BetSettlementMessage;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Maps domain settlement decisions to shared settlement transport payloads. */
@Component
public class BetSettlementMessageMapper {

  /** Creates a new bet settlement message mapper. */
  public BetSettlementMessageMapper() {}

  /**
   * Maps the supplied domain settlement decision into a settlement transport payload.
   *
   * @param settlement domain settlement decision to map
   * @return mapped settlement transport payload
   */
  public BetSettlementMessage toMessage(BetSettlement settlement) {
    Objects.requireNonNull(settlement, "settlement must not be null");
    return new BetSettlementMessage(
        settlement.bet().betId(),
        settlement.bet().userId(),
        settlement.eventOutcome().eventId(),
        settlement.eventOutcome().eventName(),
        settlement.bet().eventMarketId(),
        settlement.bet().eventWinnerId(),
        settlement.eventOutcome().eventWinnerId(),
        settlement.bet().betAmount(),
        settlement.settlementResult().name());
  }
}
