package io.github.georgecodes.betsettler.infrastructure.rest.controller;

import io.github.georgecodes.betsettler.application.port.in.GetBetsQueryUseCase;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.BetResponse;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.ErrorResponse;
import io.github.georgecodes.betsettler.infrastructure.rest.mapper.BetResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Objects;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes bet inspection endpoints for local and automated verification. */
@RestController
@RequestMapping("/api/v1/bets")
@Tag(name = "Bets", description = "Inspect seeded bets and event-linked bet data.")
public class BetQueryController {

  /** Use case used to load bets. */
  private final GetBetsQueryUseCase getBetsQueryUseCase;

  /** Mapper used to translate domain bets into REST payloads. */
  private final BetResponseMapper betResponseMapper;

  /**
   * Creates a new bet query controller.
   *
   * @param getBetsQueryUseCase use case used to load bets
   * @param betResponseMapper mapper used to translate domain bets into REST payloads
   */
  public BetQueryController(
      GetBetsQueryUseCase getBetsQueryUseCase, BetResponseMapper betResponseMapper) {
    this.getBetsQueryUseCase =
        Objects.requireNonNull(getBetsQueryUseCase, "getBetsQueryUseCase must not be null");
    this.betResponseMapper =
        Objects.requireNonNull(betResponseMapper, "betResponseMapper must not be null");
  }

  /**
   * Returns all bets or only the bets for the supplied event identifier.
   *
   * @param eventId optional event identifier filter
   * @return matching bets
   */
  @GetMapping
  @Operation(
      summary = "Get bets",
      description =
          "Returns all seeded bets or only the bets linked to a supplied event identifier.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Matching bets returned successfully.",
        content =
            @Content(array = @ArraySchema(schema = @Schema(implementation = BetResponse.class)))),
    @ApiResponse(
        responseCode = "400",
        description = "The supplied request parameters were invalid.",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public List<BetResponse> getBets(
      @Parameter(
              description = "Optional event identifier filter. Surrounding whitespace is ignored.",
              example = "EVT-1001")
          @RequestParam(required = false)
          String eventId) {
    String normalizedEventId = normalizeEventId(eventId);
    return betResponseMapper.toBetResponses(
        normalizedEventId == null
            ? getBetsQueryUseCase.getAllBets()
            : getBetsQueryUseCase.getBetsByEventId(normalizedEventId));
  }

  private static String normalizeEventId(String eventId) {
    if (eventId == null) {
      return null;
    }
    String normalizedEventId = eventId.trim();
    return normalizedEventId.isEmpty() ? null : normalizedEventId;
  }
}
