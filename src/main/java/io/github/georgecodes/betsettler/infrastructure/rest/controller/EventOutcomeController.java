package io.github.georgecodes.betsettler.infrastructure.rest.controller;

import io.github.georgecodes.betsettler.application.dto.PublishEventOutcomeCommand;
import io.github.georgecodes.betsettler.application.port.in.PublishEventOutcomeUseCase;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.ErrorResponse;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.EventOutcomeRequest;
import io.github.georgecodes.betsettler.infrastructure.rest.dto.PublishEventOutcomeResponse;
import io.github.georgecodes.betsettler.infrastructure.rest.mapper.EventOutcomeRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Handles publish requests for sports event outcomes. */
@RestController
@RequestMapping("/api/v1/event-outcomes")
@Tag(
    name = "Event outcomes",
    description = "Publish sports event outcomes into the settlement workflow.")
public class EventOutcomeController {

  /** Use case used to accept and publish event outcomes. */
  private final PublishEventOutcomeUseCase publishEventOutcomeUseCase;

  /** Mapper used to translate REST payloads. */
  private final EventOutcomeRestMapper eventOutcomeRestMapper;

  /**
   * Creates a new event outcome controller.
   *
   * @param publishEventOutcomeUseCase use case used to accept and publish event outcomes
   * @param eventOutcomeRestMapper mapper used to translate event-outcome REST payloads
   */
  public EventOutcomeController(
      PublishEventOutcomeUseCase publishEventOutcomeUseCase,
      EventOutcomeRestMapper eventOutcomeRestMapper) {
    this.publishEventOutcomeUseCase =
        Objects.requireNonNull(
            publishEventOutcomeUseCase, "publishEventOutcomeUseCase must not be null");
    this.eventOutcomeRestMapper =
        Objects.requireNonNull(eventOutcomeRestMapper, "eventOutcomeRestMapper must not be null");
  }

  /**
   * Accepts an event outcome for asynchronous publication and processing.
   *
   * @param request validated event outcome request body
   * @return accepted-response payload
   */
  @PostMapping
  @Operation(
      summary = "Publish an event outcome",
      description =
          "Accepts a validated sports event outcome, waits briefly for Kafka broker acknowledgement, and then returns 202 for asynchronous downstream processing.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "202",
        description = "Event outcome accepted after Kafka acknowledged the publish request.",
        content = @Content(schema = @Schema(implementation = PublishEventOutcomeResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Request validation failed or the request body was malformed.",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "503",
        description =
            "The event outcome could not be handed off to Kafka within the publish acknowledgement window.",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description =
            "The request could not be processed because of an application state conflict.",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<PublishEventOutcomeResponse> publishEventOutcome(
      @Valid @RequestBody EventOutcomeRequest request) {
    PublishEventOutcomeCommand command = eventOutcomeRestMapper.toCommand(request);
    publishEventOutcomeUseCase.publish(command);
    return ResponseEntity.accepted()
        .body(eventOutcomeRestMapper.toPublishEventOutcomeResponse(command));
  }
}
