package com.paula.pokerplanning_server.web.dto;

import com.paula.pokerplanning_server.domain.model.Invite;
import com.paula.pokerplanning_server.domain.model.ParticipantRole;

import java.time.Instant;
import java.util.UUID;

public record InviteResponse(
        UUID id,
        UUID roomId,
        ParticipantRole role,
        Instant expiresAt,
        Integer maxUses,
        int uses,
        boolean revoked,
        Instant createdAt,
        String token  // raw token, only returned on creation
) {
    public static InviteResponse from(Invite invite, String rawToken) {
        return new InviteResponse(
                invite.getId(),
                invite.getRoomId(),
                invite.getRole(),
                invite.getExpiresAt(),
                invite.getMaxUses(),
                invite.getUses(),
                invite.isRevoked(),
                invite.getCreatedAt(),
                rawToken
        );
    }

    public static InviteResponse from(Invite invite) {
        return from(invite, null);
    }
}
