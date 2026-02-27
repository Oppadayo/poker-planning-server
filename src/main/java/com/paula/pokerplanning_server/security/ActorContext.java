package com.paula.pokerplanning_server.security;

import com.paula.pokerplanning_server.domain.model.ParticipantRole;
import lombok.Getter;

import java.util.UUID;

/**
 * Represents the caller identity for a given request.
 * Either userId (for JWT-authenticated users) or guestId (for guest users) will be non-null.
 */
@Getter
public class ActorContext {

    private final UUID userId;
    private final String guestId;

    // Populated when a valid guestToken is provided (for host-only operations)
    private final UUID guestParticipantId;
    private final ParticipantRole guestRole;

    private ActorContext(UUID userId, String guestId, UUID guestParticipantId, ParticipantRole guestRole) {
        this.userId = userId;
        this.guestId = guestId;
        this.guestParticipantId = guestParticipantId;
        this.guestRole = guestRole;
    }

    public static ActorContext forUser(UUID userId) {
        return new ActorContext(userId, null, null, null);
    }

    public static ActorContext forGuest(String guestId) {
        return new ActorContext(null, guestId, null, null);
    }

    public static ActorContext forGuestWithToken(String guestId, UUID participantId, ParticipantRole role) {
        return new ActorContext(null, guestId, participantId, role);
    }

    public boolean isUser() {
        return userId != null;
    }

    public boolean isGuest() {
        return guestId != null;
    }

    public boolean hasGuestToken() {
        return guestParticipantId != null;
    }
}
