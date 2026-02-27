package com.paula.pokerplanning_server.web.dto;

import com.paula.pokerplanning_server.domain.model.DeckType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String displayName,
        DeckType deckType,
        Boolean allowObservers
) {}
