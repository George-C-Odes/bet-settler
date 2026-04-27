package io.github.georgecodes.betsettler.application.port.out;

import io.github.georgecodes.betsettler.domain.model.BetSettlement;

/** Provides destination metadata and payload snapshots for prepared settlements. */
public interface BetSettlementPayloadPort {

  /**
   * Returns the configured logical destination used for settlement publications.
   *
   * @return configured logical destination used for settlement publications
   */
  String destinationTopic();

  /**
   * Returns the serialized payload snapshot that should be stored in the settlement audit.
   *
   * @param settlement settlement decision to serialize
   * @return serialized payload snapshot
   */
  String payloadSnapshot(BetSettlement settlement);
}
