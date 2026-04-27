package io.github.georgecodes.betsettler.infrastructure.messaging.settlement.payload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.georgecodes.betsettler.domain.model.BetSettlement;
import io.github.georgecodes.betsettler.domain.validation.ValidationSupport;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.dto.BetSettlementMessage;
import io.github.georgecodes.betsettler.infrastructure.messaging.settlement.mapper.BetSettlementMessageMapper;
import java.util.Objects;

/** Maps shared settlement messages to and from stored JSON payload snapshots. */
public class BetSettlementPayloadCodec {

  /** Mapper used to create shared settlement transport payloads. */
  private final BetSettlementMessageMapper betSettlementMessageMapper;

  /** Shared JSON mapper used for audit payload snapshots and retry payload replay. */
  private final ObjectMapper objectMapper;

  /**
   * Creates a new settlement payload codec.
   *
   * @param betSettlementMessageMapper mapper used to create shared settlement transport payloads
   * @param objectMapper shared application JSON mapper
   */
  public BetSettlementPayloadCodec(
      BetSettlementMessageMapper betSettlementMessageMapper, ObjectMapper objectMapper) {
    this.betSettlementMessageMapper =
        Objects.requireNonNull(
            betSettlementMessageMapper, "betSettlementMessageMapper must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  /**
   * Maps the supplied settlement into the shared settlement transport payload.
   *
   * @param settlement settlement decision to map
   * @return mapped transport payload
   */
  public BetSettlementMessage toMessage(BetSettlement settlement) {
    Objects.requireNonNull(settlement, "settlement must not be null");
    return betSettlementMessageMapper.toMessage(settlement);
  }

  /**
   * Serializes the supplied settlement into the payload snapshot stored in the settlement audit.
   *
   * @param settlement settlement decision to serialize
   * @return serialized payload snapshot
   */
  public String payloadSnapshot(BetSettlement settlement) {
    return serialize(toMessage(settlement));
  }

  /**
   * Restores a stored settlement payload snapshot into the shared settlement transport DTO.
   *
   * @param payloadSnapshot serialized payload snapshot stored with the settlement audit
   * @return deserialized transport payload
   */
  public BetSettlementMessage messageFromPayloadSnapshot(String payloadSnapshot) {
    try {
      return objectMapper.readValue(
          ValidationSupport.requireNonBlank(payloadSnapshot, "payloadSnapshot"),
          BetSettlementMessage.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException(
          "Failed to deserialize bet settlement payload snapshot for retry.", exception);
    }
  }

  private String serialize(BetSettlementMessage message) {
    try {
      return objectMapper.writeValueAsString(message);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException(
          "Failed to serialize bet settlement payload for audit.", exception);
    }
  }
}
