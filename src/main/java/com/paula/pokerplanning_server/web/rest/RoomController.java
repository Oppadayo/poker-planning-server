package com.paula.pokerplanning_server.web.rest;

import com.paula.pokerplanning_server.domain.model.Participant;
import com.paula.pokerplanning_server.domain.model.Room;
import com.paula.pokerplanning_server.domain.model.Story;
import com.paula.pokerplanning_server.security.ActorContext;
import com.paula.pokerplanning_server.service.ActorService;
import com.paula.pokerplanning_server.service.RoomService;
import com.paula.pokerplanning_server.service.RoundService;
import com.paula.pokerplanning_server.service.StoryService;
import com.paula.pokerplanning_server.web.dto.*;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "Rooms", description = "Criação, entrada e gerenciamento de salas de Planning Poker.")
@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final StoryService storyService;
    private final RoundService roundService;
    private final ActorService actorService;

    // ─── Criar sala ───────────────────────────────────────────────────────────

    @Operation(
            summary = "Criar sala",
            description = """
                    Cria uma nova sala e torna o criador HOST automaticamente.

                    **Guest:** envie `X-Guest-Id` com um UUID gerado no frontend.
                    A resposta incluirá `guestToken` — guarde-o para operações de host.

                    **Usuário logado:** use `Authorization: Bearer <jwt>` (sem `X-Guest-Id`).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sala criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JoinRoomResponse createRoom(
            @RequestBody @Valid RoomCreateRequest request,
            @Parameter(description = "UUID do guest (ex: crypto.randomUUID()). Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            Authentication auth) {
        ActorContext actor = actorService.resolve(auth, guestId);
        return roomService.createRoom(
                actor, request.displayName(), request.deckType(),
                request.allowObservers() != null && request.allowObservers(), request.name()
        );
    }

    // ─── Obter sala ───────────────────────────────────────────────────────────

    @Operation(summary = "Buscar sala por ID", description = "Retorna dados básicos da sala (sem participantes nem rodada).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sala encontrada"),
            @ApiResponse(responseCode = "404", description = "Sala não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{roomId}")
    public RoomResponse getRoom(@PathVariable UUID roomId) {
        return RoomResponse.from(roomService.getRoom(roomId));
    }

    // ─── Estado completo ──────────────────────────────────────────────────────

    @Operation(
            summary = "Estado completo da sala",
            description = """
                    Retorna o estado snapshot da sala: room, me, participantes, histórias e rodada ativa.
                    Votos são ocultados (`value: null`) enquanto o status da rodada for `VOTING`.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado retornado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Não é participante da sala",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{roomId}/state")
    public RoomStateResponse getRoomState(
            @PathVariable UUID roomId,
            @Parameter(description = "UUID do guest. Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            Authentication auth) {
        Room room = roomService.getRoom(roomId);
        ActorContext actor = actorService.resolve(auth, guestId);
        Participant me = roomService.getParticipant(roomId, actor);
        List<Participant> participants = roomService.getParticipants(roomId);
        List<Story> stories = storyService.getStoriesByRoom(roomId);
        RoundResponse round = roundService.getActiveRoundResponse(roomId);
        return new RoomStateResponse(
                RoomResponse.from(room), ParticipantResponse.from(me),
                participants.stream().map(ParticipantResponse::from).toList(),
                stories.stream().map(StoryResponse::from).toList(),
                room.getCurrentStoryId(), round
        );
    }

    // ─── Entrar por roomId ────────────────────────────────────────────────────

    @Operation(
            summary = "Entrar na sala (por ID)",
            description = "Entra em uma sala. Role padrão: `PARTICIPANT`. Para guests, retorna `guestToken`."
    )
    @PostMapping("/{roomId}/join")
    public JoinRoomResponse joinRoom(
            @PathVariable UUID roomId,
            @RequestBody @Valid RoomJoinRequest request,
            @Parameter(description = "UUID do guest. Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            Authentication auth) {
        ActorContext actor = actorService.resolve(auth, guestId);
        return roomService.joinRoom(roomId, actor, request.displayName(), request.role());
    }

    // ─── Entrar por código ────────────────────────────────────────────────────

    @Operation(summary = "Entrar na sala (por código)", description = "Entra usando o código curto de 6 caracteres exibido no lobby.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrou na sala com sucesso"),
            @ApiResponse(responseCode = "404", description = "Código inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/join-by-code/{code}")
    public JoinRoomResponse joinByCode(
            @PathVariable String code,
            @RequestBody @Valid RoomJoinRequest request,
            @Parameter(description = "UUID do guest. Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            Authentication auth) {
        ActorContext actor = actorService.resolve(auth, guestId);
        return roomService.joinByCode(code, actor, request.displayName(), request.role());
    }

    // ─── Sair ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Sair da sala", description = "Marca o participante como offline e emite evento `PARTICIPANT_LEFT`.")
    @PostMapping("/{roomId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveRoom(
            @PathVariable UUID roomId,
            @Parameter(description = "UUID do guest. Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            Authentication auth) {
        actorService.resolve(auth, guestId);
        roomService.leaveRoom(roomId, actorService.resolve(auth, guestId));
    }

    // ─── [HOST] Expulsar participante ─────────────────────────────────────────

    @Operation(
            summary = "[HOST] Expulsar participante",
            description = "Remove um participante da sala. Requer `X-Guest-Token` (guest host) ou JWT com role HOST."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Participante removido"),
            @ApiResponse(responseCode = "403", description = "Somente o host pode executar",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{roomId}/participants/{participantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void kickParticipant(
            @PathVariable UUID roomId,
            @PathVariable UUID participantId,
            @Parameter(description = "Token assinado do host guest. Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            Authentication auth) {
        ActorContext actor = actorService.resolveHost(auth, guestToken, roomId);
        roomService.kickParticipant(roomId, participantId, actor);
    }

    // ─── [HOST] Transferir host ───────────────────────────────────────────────

    @Operation(summary = "[HOST] Transferir host", description = "Transfere a role HOST para outro participante. O host atual vira PARTICIPANT.")
    @PostMapping("/{roomId}/transfer-host/{newHostParticipantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transferHost(
            @PathVariable UUID roomId,
            @PathVariable UUID newHostParticipantId,
            @Parameter(description = "Token assinado do host guest. Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            Authentication auth) {
        ActorContext actor = actorService.resolveHost(auth, guestToken, roomId);
        roomService.transferHost(roomId, newHostParticipantId, actor);
    }

    // ─── [HOST] Fechar sala ───────────────────────────────────────────────────

    @Operation(summary = "[HOST] Fechar sala", description = "Marca a sala como CLOSED e emite `ROOM_CLOSED`. Sem mais ações possíveis.")
    @PostMapping("/{roomId}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void closeRoom(
            @PathVariable UUID roomId,
            @Parameter(description = "Token assinado do host guest. Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            Authentication auth) {
        ActorContext actor = actorService.resolveHost(auth, guestToken, roomId);
        roomService.closeRoom(roomId, actor);
    }
}
