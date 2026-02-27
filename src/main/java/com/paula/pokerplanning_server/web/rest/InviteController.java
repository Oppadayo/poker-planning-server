package com.paula.pokerplanning_server.web.rest;

import com.paula.pokerplanning_server.domain.model.Invite;
import com.paula.pokerplanning_server.security.ActorContext;
import com.paula.pokerplanning_server.service.ActorService;
import com.paula.pokerplanning_server.service.InviteService;
import com.paula.pokerplanning_server.web.dto.ErrorResponse;
import com.paula.pokerplanning_server.web.dto.InviteCreateRequest;
import com.paula.pokerplanning_server.web.dto.InviteResponse;
import com.paula.pokerplanning_server.web.dto.JoinRoomResponse;
import com.paula.pokerplanning_server.web.dto.RoomJoinRequest;
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

@Tag(name = "Invites", description = """
        Links de convite para entrar em salas.
        O token é gerado como UUID, armazenado como SHA-256 e retornado **uma única vez** na criação.
        """)
@RestController
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;
    private final ActorService actorService;

    @Operation(
            summary = "[HOST] Criar convite",
            description = """
                    Gera um link de convite para a sala. O campo `token` da resposta é retornado
                    **apenas nesta chamada** — guarde-o. Pode configurar role, expiração e limite de usos.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Convite criado (token incluso na resposta)"),
            @ApiResponse(responseCode = "403", description = "Somente o host pode criar convites",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/rooms/{roomId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteResponse createInvite(
            @PathVariable UUID roomId,
            @RequestBody @Valid InviteCreateRequest request,
            @Parameter(description = "Token assinado do host guest. Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            Authentication auth) {
        ActorContext actor = actorService.resolveHost(auth, guestToken, roomId);
        String rawToken = inviteService.createInvite(roomId, actor,
                request.role(), request.expiresAt(), request.maxUses());
        Invite invite = inviteService.getInviteByToken(rawToken);
        return InviteResponse.from(invite, rawToken);
    }

    @Operation(
            summary = "[HOST] Listar convites da sala",
            description = "Retorna todos os convites criados para a sala (tokens ocultos na listagem)."
    )
    @GetMapping("/rooms/{roomId}/invites")
    public List<InviteResponse> listInvites(
            @PathVariable UUID roomId,
            @Parameter(description = "Token assinado do host guest. Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            Authentication auth) {
        ActorContext actor = actorService.resolveHost(auth, guestToken, roomId);
        return inviteService.listInvites(roomId, actor)
                .stream().map(InviteResponse::from).toList();
    }

    @Operation(
            summary = "[HOST] Revogar convite",
            description = "Invalida o convite imediatamente. Usos futuros serão rejeitados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Convite revogado"),
            @ApiResponse(responseCode = "404", description = "Convite não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/invites/{inviteId}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeInvite(
            @PathVariable UUID inviteId,
            @Parameter(description = "Token assinado do host guest. Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @Parameter(description = "ID da sala (necessário para validar autorização do host).")
            @RequestHeader(value = "X-Room-Id") UUID roomId,
            Authentication auth) {
        ActorContext actor = actorService.resolveHost(auth, guestToken, roomId);
        inviteService.revokeInvite(inviteId, actor);
    }

    @Operation(
            summary = "Visualizar convite",
            description = "Retorna dados do convite (sem o token) para exibir informações antes de entrar na sala."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convite encontrado"),
            @ApiResponse(responseCode = "404", description = "Token inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/invites/{token}")
    public InviteResponse getInvite(@PathVariable String token) {
        Invite invite = inviteService.getInviteByToken(token);
        return InviteResponse.from(invite);
    }

    @Operation(
            summary = "Entrar pela sala via convite",
            description = """
                    Entra na sala usando o token de convite. A role do participante é determinada pelo convite.
                    Para guests, retorna `guestToken` na resposta.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrou na sala com sucesso"),
            @ApiResponse(responseCode = "400", description = "Convite expirado, revogado ou com uso máximo atingido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/invites/{token}/join")
    public JoinRoomResponse joinByInvite(
            @PathVariable String token,
            @RequestBody @Valid RoomJoinRequest request,
            @Parameter(description = "UUID do guest. Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            Authentication auth) {
        ActorContext actor = actorService.resolve(auth, guestId);
        return inviteService.joinByInvite(token, actor, request.displayName());
    }
}
