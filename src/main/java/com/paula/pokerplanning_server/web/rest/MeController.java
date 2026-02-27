package com.paula.pokerplanning_server.web.rest;

import com.paula.pokerplanning_server.domain.model.Room;
import com.paula.pokerplanning_server.security.UserPrincipal;
import com.paula.pokerplanning_server.service.UserService;
import com.paula.pokerplanning_server.web.dto.ClaimRequest;
import com.paula.pokerplanning_server.web.dto.ErrorResponse;
import com.paula.pokerplanning_server.web.dto.RoomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Me", description = "Operações do usuário autenticado. Requer JWT (bearerAuth).")
@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MeController {

    private final UserService userService;

    @Operation(
            summary = "Listar minhas salas",
            description = "Retorna as salas ativas criadas pelo usuário autenticado."
    )
    @ApiResponse(responseCode = "200", description = "Lista de salas")
    @GetMapping("/sessions")
    public List<RoomResponse> getSessions(@AuthenticationPrincipal UserPrincipal principal) {
        List<Room> rooms = userService.getSessionsByUser(principal.getUserId());
        return rooms.stream().map(RoomResponse::from).toList();
    }

    @Operation(
            summary = "Vincular sessões de convidado",
            description = """
                    Vincula salas e participações criadas como guest à conta do usuário logado.
                    Útil quando o usuário jogou como convidado e depois fez login/cadastro.
                    Requer o `guestId` e o `guestToken` obtidos ao criar/entrar na sala como guest.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessões vinculadas com sucesso"),
            @ApiResponse(responseCode = "403", description = "guestToken inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/claim")
    public ResponseEntity<Void> claimSessions(
            @RequestBody @Valid ClaimRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        userService.claimSessions(principal.getUserId(), request.guestId(), request.guestToken());
        return ResponseEntity.ok().build();
    }
}
