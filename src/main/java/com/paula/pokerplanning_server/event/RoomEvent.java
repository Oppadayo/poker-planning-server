package com.paula.pokerplanning_server.event;

import com.paula.pokerplanning_server.domain.model.EventType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RoomEvent(
        String eventId,
        EventType type,
        UUID roomId,
        Instant timestamp,
        Map<String, Object> payload
) {
    public static RoomEvent of(EventType type, UUID roomId, Map<String, Object> payload) {
        return new RoomEvent(
                UUID.randomUUID().toString(),
                type,
                roomId,
                Instant.now(),
                payload
        );
    }
}
