package io.github.georgecodes.betsettler.application.port.out;

/**
 * Replays previously stored settlement payload snapshots to the configured outbound destination.
 */
public interface BetSettlementReplayPort {

  /**
   * Replays a previously stored settlement payload snapshot to a destination.
   *
   * @param destinationTopic destination topic or channel name to use for the replay
   * @param payloadSnapshot serialized payload snapshot stored in the settlement audit
   */
  void replayPayloadSnapshot(String destinationTopic, String payloadSnapshot);
}
