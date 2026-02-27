package com.paula.pokerplanning_server.web.rest;

import com.paula.pokerplanning_server.domain.model.Story;
import com.paula.pokerplanning_server.security.ActorContext;
import com.paula.pokerplanning_server.service.ActorService;
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

@Tag(name = "Stories", description = "Gerenciamento do backlog de histórias da sala. Operações de escrita exigem role HOST.")
@RestController
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;
    private final ActorService actorService;

    @Operation(summary = "Listar histórias da sala", description = "Retorna todas as histórias ordenadas por `orderIndex`.")
    @GetMapping("/rooms/{roomId}/stories")
    public List<StoryResponse> getStories(@PathVariable UUID roomId) {
        return storyService.getStoriesByRoom(roomId).stream().map(StoryResponse::from).toList();
    }

    @Operation(
            summary = "[HOST] Criar história",
            description = "Adiciona uma história ao backlog da sala. Requer `X-Guest-Token` (guest) ou JWT com role HOST."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "História criada"),
            @ApiResponse(responseCode = "403", description = "Somente o host pode criar histórias",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/rooms/{roomId}/stories")
    @ResponseStatus(HttpStatus.CREATED)
    public StoryResponse createStory(
            @PathVariable UUID roomId,
            @RequestBody @Valid StoryCreateRequest request,
            @Parameter(description = "Token assinado do host guest. Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            Authentication auth) {
        ActorContext actor = actorService.resolveHost(auth, guestToken, roomId);
        Story story = storyService.createStory(roomId, actor,
                request.title(), request.description(), request.externalRef());
        return StoryResponse.from(story);
    }

    @Operation(
            summary = "[HOST] Atualizar história",
            description = "Atualiza título, descrição ou referência externa de uma história."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "História atualizada"),
            @ApiResponse(responseCode = "404", description = "História não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/stories/{storyId}")
    public StoryResponse updateStory(
            @PathVariable UUID storyId,
            @RequestBody @Valid StoryUpdateRequest request,
            @Parameter(description = "Token assinado do host guest. Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            Authentication auth) {
        Story existing = storyService.getStory(storyId);
        ActorContext actor = actorService.resolveHost(auth, guestToken, existing.getRoomId());
        Story story = storyService.updateStory(storyId, actor,
                request.title(), request.description(), request.externalRef());
        return StoryResponse.from(story);
    }

    @Operation(summary = "[HOST] Excluir história", description = "Remove uma história do backlog.")
    @ApiResponse(responseCode = "204", description = "História removida")
    @DeleteMapping("/stories/{storyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStory(
            @PathVariable UUID storyId,
            @Parameter(description = "Token assinado do host guest. Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            Authentication auth) {
        Story existing = storyService.getStory(storyId);
        ActorContext actor = actorService.resolveHost(auth, guestToken, existing.getRoomId());
        storyService.deleteStory(storyId, actor);
    }

    @Operation(
            summary = "[HOST] Reordenar histórias",
            description = "Redefine a ordem do backlog. Envie a lista de IDs na ordem desejada."
    )
    @PostMapping("/rooms/{roomId}/stories/reorder")
    public List<StoryResponse> reorderStories(
            @PathVariable UUID roomId,
            @RequestBody @Valid StoryReorderRequest request,
            @Parameter(description = "Token assinado do host guest. Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            Authentication auth) {
        ActorContext actor = actorService.resolveHost(auth, guestToken, roomId);
        return storyService.reorderStories(roomId, request.storyIds(), actor)
                .stream().map(StoryResponse::from).toList();
    }

    @Operation(
            summary = "[HOST] Selecionar história atual",
            description = "Define a história que será votada na próxima rodada. Emite evento `STORY_SELECTED`."
    )
    @ApiResponse(responseCode = "204", description = "História selecionada")
    @PostMapping("/rooms/{roomId}/stories/{storyId}/select")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void selectStory(
            @PathVariable UUID roomId,
            @PathVariable UUID storyId,
            @Parameter(description = "Token assinado do host guest. Omitir ao usar JWT.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            Authentication auth) {
        ActorContext actor = actorService.resolveHost(auth, guestToken, roomId);
        storyService.selectCurrentStory(roomId, storyId, actor);
    }
}
