package io.github.georgecodes.betsettler.infrastructure.rest.controller;

import io.github.georgecodes.betsettler.application.dto.DemoResetSummary;
import io.github.georgecodes.betsettler.application.dto.SettlementDispatchRetrySummary;
import io.github.georgecodes.betsettler.application.port.in.ResetDemoStateUseCase;
import io.github.georgecodes.betsettler.application.port.in.RetrySettlementDispatchUseCase;
import io.github.georgecodes.betsettler.infrastructure.config.DemoProfiles;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.DemoResetResponse;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.ErrorResponse;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.SettlementDispatchRetryResponse;
import io.github.georgecodes.betsettler.infrastructure.rest.mapper.DemoResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes supported local and test-only helper endpoints for demo-state management. */
@RestController
@Profile({DemoProfiles.LOCAL_DOCKER, DemoProfiles.LOCAL_FALLBACK, DemoProfiles.TEST})
@RequestMapping("/api/v1/internal/demo")
@Tag(
    name = "Demo helpers",
    description =
        "Helper endpoints for resetting transient demo state in the supported local and test profiles.")
public class DemoController {

  /** Use case used to reset transient demo state. */
  private final ResetDemoStateUseCase resetDemoStateUseCase;

  /** Use case used to replay pending or failed settlement dispatches. */
  private final RetrySettlementDispatchUseCase retrySettlementDispatchUseCase;

  /** Mapper used to translate reset responses. */
  private final DemoResponseMapper demoResponseMapper;

  /**
   * Creates a new demo controller.
   *
   * @param resetDemoStateUseCase use case used to reset transient demo state
   * @param retrySettlementDispatchUseCase use case used to replay retryable settlements
   * @param demoResponseMapper mapper used to translate demo-helper responses
   */
  public DemoController(
      ResetDemoStateUseCase resetDemoStateUseCase,
      RetrySettlementDispatchUseCase retrySettlementDispatchUseCase,
      DemoResponseMapper demoResponseMapper) {
    this.resetDemoStateUseCase =
        Objects.requireNonNull(resetDemoStateUseCase, "resetDemoStateUseCase must not be null");
    this.retrySettlementDispatchUseCase =
        Objects.requireNonNull(
            retrySettlementDispatchUseCase, "retrySettlementDispatchUseCase must not be null");
    this.demoResponseMapper =
        Objects.requireNonNull(demoResponseMapper, "demoResponseMapper must not be null");
  }

  /**
   * Resets transient demo state for the supported local or test execution profiles.
   *
   * @return success-response payload
   */
  @PostMapping("/reset")
  @Operation(
      summary = "Reset transient demo state",
      description =
          "Clears processed event outcomes and settlement audit rows while preserving Flyway-seeded demo bets. This endpoint is only available in the supported local profiles and the test profile.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Transient demo state cleared successfully.",
        content = @Content(schema = @Schema(implementation = DemoResetResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description =
            "The reset operation could not be completed because of an application state conflict.",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<DemoResetResponse> resetDemoState() {
    DemoResetSummary demoResetSummary = resetDemoStateUseCase.resetDemoState();
    return ResponseEntity.ok(demoResponseMapper.toDemoResetResponse(demoResetSummary));
  }

  /**
   * Retries settlement audit rows that remain pending or previously failed.
   *
   * @return retry summary payload
   */
  @PostMapping("/retry-settlements")
  @Operation(
      summary = "Retry pending or failed settlement dispatches",
      description =
          "Replays settlement audit rows that are still PENDING or FAILED by resending each row's stored destination topic and stored payload snapshot through the currently configured settlement transport. This endpoint is only available in the supported local profiles and the test profile.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Retryable settlement dispatches were replayed.",
        content =
            @Content(schema = @Schema(implementation = SettlementDispatchRetryResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description =
            "The retry operation could not be completed because of an application state conflict.",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<SettlementDispatchRetryResponse> retrySettlements() {
    SettlementDispatchRetrySummary retrySummary = retrySettlementDispatchUseCase.retrySettlements();
    return ResponseEntity.ok(demoResponseMapper.toSettlementDispatchRetryResponse(retrySummary));
  }
}
