package com.paula.pokerplanning_server.web.rest;

import com.paula.pokerplanning_server.domain.model.Round;
import com.paula.pokerplanning_server.security.ActorContext;
import com.paula.pokerplanning_server.service.ActorService;
import com.paula.pokerplanning_server.service.RoundService;
import com.paula.pokerplanning_server.web.dto.ErrorResponse;
import com.paula.pokerplanning_server.web.dto.FinalizeRoundRequest;
import com.paula.pokerplanning_server.web.dto.RoundResponse;
import com.paula.pokerplanning_server.web.dto.VoteRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Rounds", description = """
        Controle do ciclo de vida de uma rodada de votação.

        Fluxo: **start → vote(s) → reveal → [reset | finalize]**

        Votos são ocultados (`value: null`) até o `reveal`.
        Apenas HOST pode iniciar, revelar, resetar e finalizar.
        Qualquer PARTICIPANT/HOST pode votar via REST ou WebSocket.
        """)
@RestController
@RequestMapping("/rooms/{roomId}/rounds")
@RequiredArgsConstructor
public class RoundController {

    private final RoundService roundService;
    private final ActorService actorService;

    @Operation(
            summary = "[HOST] Iniciar rodada",
            description = "Abre a votação para a história selecionada (`currentStoryId`). Emite `ROUND_STARTED`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rodada iniciada"),
            @ApiResponse(responseCode = "400", description = "Nenhuma história selecionada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Já existe rodada ativa",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public RoundResponse startRound(
            @PathVariable UUID roomId,
            @Parameter(description = "Token assinado do host guest. Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            Authentication auth) {
        ActorContext actor = actorService.resolveHost(auth, guestToken, roomId);
        Round round = roundService.startRound(roomId, actor);
        return roundService.toResponse(round);
    }

    @Operation(
            summary = "Votar",
            description = """
                    Registra ou atualiza o voto do participante na rodada ativa.
                    Emite evento `VOTE_CAST` sem revelar o valor (apenas `hasVoted: true`).
                    Também pode ser feito via WebSocket: envie para `/app/rooms/{roomId}/vote`.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Voto registrado"),
            @ApiResponse(responseCode = "400", description = "Rodada não está em votação",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Observers não podem votar",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/vote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void castVote(
            @PathVariable UUID roomId,
            @RequestBody @Valid VoteRequest request,
            @Parameter(description = "UUID do guest. Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            Authentication auth) {
        ActorContext actor = actorService.resolve(auth, guestId);
        roundService.castVote(roomId, actor, request.value());
    }

    @Operation(
            summary = "[HOST] Revelar votos",
            description = "Muda status para `REVEALED` e emite `ROUND_REVEALED` com todos os valores."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Votos revelados"),
            @ApiResponse(responseCode = "400", description = "Rodada não está em VOTING",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/reveal")
    public RoundResponse revealVotes(
            @PathVariable UUID roomId,
            @Parameter(description = "Token assinado do host guest. Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            Authentication auth) {
        ActorContext actor = actorService.resolveHost(auth, guestToken, roomId);
        Round round = roundService.revealVotes(roomId, actor);
        return roundService.toResponse(round);
    }

    @Operation(
            summary = "[HOST] Resetar rodada",
            description = "Apaga todos os votos e volta ao status `VOTING`. Emite `ROUND_RESET`."
    )
    @ApiResponse(responseCode = "200", description = "Rodada resetada")
    @PostMapping("/reset")
    public RoundResponse resetRound(
            @PathVariable UUID roomId,
            @Parameter(description = "Token assinado do host guest. Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            Authentication auth) {
        ActorContext actor = actorService.resolveHost(auth, guestToken, roomId);
        Round round = roundService.resetRound(roomId, actor);
        return roundService.toResponse(round);
    }

    @Operation(
            summary = "[HOST] Finalizar rodada",
            description = """
                    Encerra a rodada com uma estimativa final, marca a história como `ESTIMATED`
                    e emite `ROUND_FINALIZED`. A rodada deve estar em status `REVEALED`.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rodada finalizada"),
            @ApiResponse(responseCode = "400", description = "Rodada não está em REVEALED",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/finalize")
    public RoundResponse finalizeRound(
            @PathVariable UUID roomId,
            @RequestBody @Valid FinalizeRoundRequest request,
            @Parameter(description = "Token assinado do host guest. Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            Authentication auth) {
        ActorContext actor = actorService.resolveHost(auth, guestToken, roomId);
        Round round = roundService.finalizeRound(roomId, actor, request.finalEstimate());
        return roundService.toResponse(round);
    }
}
