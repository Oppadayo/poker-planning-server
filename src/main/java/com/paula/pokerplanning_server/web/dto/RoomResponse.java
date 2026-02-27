package com.paula.pokerplanning_server.web.dto;

import com.paula.pokerplanning_server.domain.model.DeckType;
import com.paula.pokerplanning_server.domain.model.Room;
import com.paula.pokerplanning_server.domain.model.RoomStatus;

import java.time.Instant;
import java.util.UUID;

public record RoomResponse(
        UUID id,
        String name,
        String code,
        DeckType deckType,
        boolean allowObservers,
        RoomStatus status,
        UUID currentStoryId,
        Instant createdAt
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getCode(),
                room.getSettings().getDeckType(),
                room.getSettings().isAllowObservers(),
                room.getStatus(),
                room.getCurrentStoryId(),
                room.getCreatedAt()
        );
    }
}
